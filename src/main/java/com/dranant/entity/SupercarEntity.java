package com.dranant.entity;

import com.dranant.registry.ModItems;
import com.dranant.registry.ModSounds;
import com.dranant.vehicle.VehicleInput;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Drivable supercar. Simulated on the driver's client (like boats) for lag-free, realistic handling:
 * <ul>
 *   <li>engine torque curve with 7 automatic gears + RPM, real top speeds (Veloce 187 km/h, GT 166, Phantom 209)</li>
 *   <li>strong brakes (S), reverse, handbrake drift (SPACE) with separate heading/velocity and tyre smoke</li>
 *   <li>nitro (SPRINT) +35% top speed with exhaust flames, refills while you cruise</li>
 *   <li>speed-sensitive steering, 1-block step climbing, crashes bounce the car back - it NEVER explodes</li>
 *   <li>two seats (driver + sidekick/friend), opening doors (scissor/conventional/gullwing), bonnet, boot, headlights, horn</li>
 * </ul>
 * Right-click: get in. Sneak + right-click a door / the front / the back: open or close that door, the bonnet or the boot.
 * Sneak + punch: pick the car up as an item (showroom cars only in creative).
 */
public class SupercarEntity extends Entity {
    public static final int PART_DOOR_LEFT = 0, PART_DOOR_RIGHT = 1, PART_HOOD = 2, PART_TRUNK = 3;
    public static final int CONTROL_HORN = 0, CONTROL_LIGHTS = 1, CONTROL_DOORS = 2, CONTROL_HOOD = 3, CONTROL_TRUNK = 4,
            CONTROL_NITRO_ON = 5, CONTROL_NITRO_OFF = 6;

    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(SupercarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_PARTS = SynchedEntityData.defineId(SupercarEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_LIGHTS = SynchedEntityData.defineId(SupercarEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SHOWROOM = SynchedEntityData.defineId(SupercarEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TURNTABLE = SynchedEntityData.defineId(SupercarEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_NITRO = SynchedEntityData.defineId(SupercarEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double WHEEL_RADIUS = 6.5D / 16.0D;

    // ---- driving state (meaningful on the simulating side)
    private double speed;            // signed, blocks/tick along the heading
    private double velX, velZ;       // actual horizontal velocity (differs from heading while drifting)
    private float steer;             // -1..1 smoothed steering input
    private double nitro = 1.0D;     // 0..1 tank
    private boolean nitroActive;
    private boolean braking;
    private int gear = 1;
    private float rpm = 900.0F;
    private float drift;             // slip angle (radians)
    private int crashShake;

    // ---- client visuals
    private final float[] partOpen = new float[4];
    private final float[] partOpenO = new float[4];
    private float wheelSpin, wheelSpinO;
    private float visualSteer, visualSteerO;
    private float wingLift, wingLiftO;
    private float bodyRoll, bodyRollO, bodyPitch, bodyPitchO;
    private double lastMoved;
    private float lastYaw;

    // ---- server bookkeeping
    private double serverSpeed;
    private int autoCloseDoor = -1;
    private int autoCloseTicks;

    // ---- remote interpolation
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private float lerpYRot;

    public SupercarEntity(EntityType<? extends SupercarEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_VARIANT, 0);
        builder.define(DATA_PARTS, (byte) 0);
        builder.define(DATA_LIGHTS, false);
        builder.define(DATA_SHOWROOM, false);
        builder.define(DATA_TURNTABLE, false);
        builder.define(DATA_NITRO, false);
    }

    // ------------------------------------------------------------------ data
    public SupercarVariant getVariant() {
        return SupercarVariant.byOrdinal(entityData.get(DATA_VARIANT));
    }

    public void setVariant(SupercarVariant variant) {
        entityData.set(DATA_VARIANT, variant.ordinal());
    }

    public boolean isPartOpen(int part) {
        return (entityData.get(DATA_PARTS) & (1 << part)) != 0;
    }

    public void setPartOpen(int part, boolean open) {
        byte b = entityData.get(DATA_PARTS);
        b = (byte) (open ? b | (1 << part) : b & ~(1 << part));
        entityData.set(DATA_PARTS, b);
    }

    public boolean lightsOn() {
        return entityData.get(DATA_LIGHTS);
    }

    public boolean isShowroomCar() {
        return entityData.get(DATA_SHOWROOM);
    }

    public void setShowroom(boolean showroom, boolean turntable) {
        entityData.set(DATA_SHOWROOM, showroom);
        entityData.set(DATA_TURNTABLE, turntable);
    }

    public boolean isNitroVisible() {
        return entityData.get(DATA_NITRO);
    }

    // HUD accessors (driver client)
    public double getSpeed() { return speed; }
    public int getSpeedKmh() { return (int) Math.round(Math.abs(speed) * 72.0D); }
    public int getGear() { return gear; }
    public float getRpm() { return rpm; }
    public double getNitro() { return nitro; }
    public boolean isNitroActive() { return nitroActive; }
    public boolean isBraking() { return braking; }
    public float getDrift() { return drift; }
    public int getCrashShake() { return crashShake; }

    // animation accessors
    public float getPartOpen(int part, float partialTick) {
        return Mth.lerp(partialTick, partOpenO[part], partOpen[part]);
    }

    public float getWheelSpin(float partialTick) {
        return Mth.lerp(partialTick, wheelSpinO, wheelSpin);
    }

    public float getVisualSteer(float partialTick) {
        return Mth.lerp(partialTick, visualSteerO, visualSteer);
    }

    public float getWingLift(float partialTick) {
        return Mth.lerp(partialTick, wingLiftO, wingLift);
    }

    /** Suspension body roll (radians) - the body leans out of corners. */
    public float getBodyRoll(float partialTick) {
        return Mth.lerp(partialTick, bodyRollO, bodyRoll);
    }

    /** Suspension pitch (radians) - squat under acceleration, dive under braking. */
    public float getBodyPitch(float partialTick) {
        return Mth.lerp(partialTick, bodyPitchO, bodyPitch);
    }

    // ------------------------------------------------------------------ entity basics
    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public float maxUpStep() {
        return 1.05F;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < 2;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(itemFor(getVariant()));
    }

    public static net.minecraft.world.item.Item itemFor(SupercarVariant v) {
        return switch (v) {
            case VELOCE -> ModItems.SUPERCAR_VELOCE.get();
            case GT -> ModItems.SUPERCAR_GT.get();
            case PHANTOM -> ModItems.SUPERCAR_PHANTOM.get();
        };
    }

    // ------------------------------------------------------------------ interaction
    @Override
    public InteractionResult interactAt(Player player, Vec3 hit, InteractionHand hand) {
        if (!player.isSecondaryUseActive()) return InteractionResult.PASS; // falls through to interact() -> get in
        if (level().isClientSide) return InteractionResult.SUCCESS;
        // local coordinates: forward / left relative to the car
        Vec3 fwd = Vec3.directionFromRotation(0.0F, getYRot());
        Vec3 left = new Vec3(fwd.z, 0, -fwd.x).scale(-1.0D);
        double f = hit.x * fwd.x + hit.z * fwd.z;
        double l = hit.x * left.x + hit.z * left.z;
        int part;
        if (Math.abs(l) > 0.62D && Math.abs(f) < 0.9D) part = l > 0 ? PART_DOOR_LEFT : PART_DOOR_RIGHT;
        else part = f > 0 ? PART_HOOD : PART_TRUNK;
        togglePart(part, player);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level().isClientSide) {
            if (!canAddPassenger(player)) {
                player.displayClientMessage(Component.literal("Both seats are taken!").withStyle(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
            }
            boolean driver = getPassengers().isEmpty();
            int door = driver ? PART_DOOR_LEFT : PART_DOOR_RIGHT;
            if (!isPartOpen(door)) {
                setPartOpen(door, true);
                autoCloseDoor = door;
                autoCloseTicks = 28;
                level().playSound(null, blockPosition(), ModSounds.CAR_DOOR.get(), SoundSource.NEUTRAL, 0.9F, 1.1F);
            }
            if (player.startRiding(this)) {
                entityData.set(DATA_TURNTABLE, false);
                SupercarVariant v = getVariant();
                if (driver) {
                    player.displayClientMessage(Component.literal(v.displayName() + "  |  " + v.horsepower() + " HP  |  0-100 " + v.zeroToHundred()
                            + "  |  Top " + v.topSpeedKmh() + " km/h").withStyle(ChatFormatting.GOLD), true);
                    level().playSound(null, blockPosition(), ModSounds.CAR_ENGINE.get(), SoundSource.NEUTRAL, 1.2F, 0.6F);
                }
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public void togglePart(int part, @Nullable Player by) {
        boolean open = !isPartOpen(part);
        setPartOpen(part, open);
        if (autoCloseDoor == part) autoCloseDoor = -1;
        level().playSound(null, blockPosition(), ModSounds.CAR_DOOR.get(), SoundSource.NEUTRAL, 1.0F, open ? 1.15F : 0.85F);
        if (by != null) {
            String name = switch (part) {
                case PART_DOOR_LEFT -> "Driver door";
                case PART_DOOR_RIGHT -> "Passenger door";
                case PART_HOOD -> "Bonnet";
                default -> "Boot";
            };
            by.displayClientMessage(Component.literal(name + (open ? " opened" : " closed")).withStyle(ChatFormatting.AQUA), true);
        }
    }

    /** Server side handler for the driver's key presses (horn, lights, doors, bonnet, boot, nitro flames). */
    public void handleControl(int action, Player player) {
        if (!hasPassenger(player)) return;
        switch (action) {
            case CONTROL_HORN -> level().playSound(null, blockPosition(), ModSounds.CAR_HORN.get(), SoundSource.NEUTRAL, 2.0F, 1.0F);
            case CONTROL_LIGHTS -> {
                entityData.set(DATA_LIGHTS, !lightsOn());
                level().playSound(null, blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.NEUTRAL, 0.6F, lightsOn() ? 1.2F : 0.9F);
            }
            case CONTROL_DOORS -> {
                boolean open = !(isPartOpen(PART_DOOR_LEFT) || isPartOpen(PART_DOOR_RIGHT));
                setPartOpen(PART_DOOR_LEFT, open);
                setPartOpen(PART_DOOR_RIGHT, open);
                autoCloseDoor = -1;
                level().playSound(null, blockPosition(), ModSounds.CAR_DOOR.get(), SoundSource.NEUTRAL, 1.0F, open ? 1.15F : 0.85F);
            }
            case CONTROL_HOOD -> togglePart(PART_HOOD, player);
            case CONTROL_TRUNK -> togglePart(PART_TRUNK, player);
            case CONTROL_NITRO_ON -> {
                if (!isNitroVisible()) level().playSound(null, blockPosition(), ModSounds.CAR_NITRO.get(), SoundSource.NEUTRAL, 1.5F, 1.0F);
                entityData.set(DATA_NITRO, true);
            }
            case CONTROL_NITRO_OFF -> entityData.set(DATA_NITRO, false);
            default -> {
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) return true;
        if (source.getEntity() instanceof Player player && source.getDirectEntity() == player) {
            if (player.isShiftKeyDown() && getPassengers().isEmpty()) {
                if (isShowroomCar() && !player.getAbilities().instabuild) {
                    player.displayClientMessage(Component.literal("Showroom display car - drive it, don't pocket it! Buy your own in the Central Shop.")
                            .withStyle(ChatFormatting.YELLOW), true);
                    return false;
                }
                if (!player.getAbilities().instabuild) spawnAtLocation(new ItemStack(itemFor(getVariant())));
                discard();
                return true;
            }
            player.displayClientMessage(Component.literal("Sneak + punch to pick the car up").withStyle(ChatFormatting.GRAY), true);
        }
        return false; // supercars are indestructible: no damage, no explosions
    }

    @Override
    public boolean ignoreExplosion(net.minecraft.world.level.Explosion explosion) {
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    // ------------------------------------------------------------------ ticking
    @Override
    public void tick() {
        super.tick();
        for (int i = 0; i < 4; i++) {
            partOpenO[i] = partOpen[i];
            float target = isPartOpen(i) ? 1.0F : 0.0F;
            partOpen[i] += Mth.clamp(target - partOpen[i], -0.07F, 0.07F);
        }
        wheelSpinO = wheelSpin;
        visualSteerO = visualSteer;
        wingLiftO = wingLift;
        bodyRollO = bodyRoll;
        bodyPitchO = bodyPitch;
        if (crashShake > 0) crashShake--;

        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
            drive();
        } else {
            tickLerp();
        }

        if (level().isClientSide) {
            clientVisuals();
        } else {
            serverTick((ServerLevel) level());
        }
    }

    private void drive() {
        SupercarVariant v = getVariant();
        LivingEntity driver = getControllingPassenger();
        boolean local = driver != null && level().isClientSide;
        float forward = driver != null ? driver.zza : 0.0F;
        float side = driver != null ? driver.xxa : 0.0F;
        boolean handbrake = local && VehicleInput.brake;
        boolean wantNitro = local && VehicleInput.boost && forward > 0 && nitro > 0.02D;

        // nitro tank
        boolean wasNitro = nitroActive;
        nitroActive = wantNitro;
        if (nitroActive) nitro = Math.max(0.0D, nitro - 0.0065D);
        else nitro = Math.min(1.0D, nitro + 0.0016D);
        if (local && wasNitro != nitroActive) com.dranant.network.CarControlPayload.send(nitroActive ? CONTROL_NITRO_ON : CONTROL_NITRO_OFF);

        double top = v.topSpeed() * (nitroActive ? 1.35D : 1.0D);
        braking = false;
        if (forward > 0) {
            if (speed < -0.02D) {
                speed = Math.min(0.0D, speed + 0.09D);
                braking = true;
            } else {
                double frac = Mth.clamp(speed / top, 0.0D, 1.0D);
                double torque = v.accel() * (1.15D - 0.65D * frac) * (1.0D - frac * frac);
                speed += torque * (nitroActive ? 2.2D : 1.0D);
            }
        } else if (forward < 0) {
            if (speed > 0.02D) {
                speed = Math.max(0.0D, speed - 0.085D);
                braking = true;
            } else {
                speed = Math.max(-0.42D, speed - 0.018D);
            }
        } else {
            speed *= 0.991D;
            speed -= Math.signum(speed) * 0.0025D;
            if (Math.abs(speed) < 0.004D) speed = 0.0D;
        }
        if (speed > top) speed = Math.max(top, speed - 0.02D);
        if (handbrake) speed *= 0.972D;
        if (driver == null) speed *= 0.86D;
        if (isInWater()) speed *= 0.85D;

        // steering: quick at parking speed, calm and precise at 200 km/h, sharper with the handbrake
        steer += (side - steer) * 0.22F;
        double abs = Math.abs(speed);
        double turnRate = 6.2D * Math.min(1.0D, abs / 0.28D) / (1.0D + abs * 0.85D) * (handbrake ? 1.7D : 1.0D);
        setYRot(getYRot() - (float) (steer * turnRate * Math.signum(speed)));

        // tyre grip: velocity chases the heading; low grip = drift
        Vec3 heading = Vec3.directionFromRotation(0.0F, getYRot()).scale(speed);
        double grip = handbrake ? 0.055D : v.grip() * (1.0D - 0.35D * Math.abs(steer) * Math.min(1.0D, abs / v.topSpeed()));
        velX += (heading.x - velX) * grip;
        velZ += (heading.z - velZ) * grip;
        double velLen = Math.sqrt(velX * velX + velZ * velZ);
        drift = velLen > 0.05D && abs > 0.05D ? (float) Math.acos(Mth.clamp((velX * heading.x + velZ * heading.z) / (velLen * abs), -1.0D, 1.0D)) : 0.0F;
        if (drift > 0.2F) speed *= 0.995D; // scrubbing speed while sliding

        double dy = getDeltaMovement().y;
        dy = onGround() ? -0.05D : Math.max(-2.5D, dy - 0.08D);
        setDeltaMovement(velX, dy, velZ);
        double before = velLen;
        move(MoverType.SELF, getDeltaMovement());

        // crash: bounce back, shake the camera - never explode
        if (horizontalCollision && before > 0.28D) {
            speed = -speed * 0.22D;
            velX *= -0.25D;
            velZ *= -0.25D;
            crashShake = (int) Math.min(18, 6 + before * 8);
            if (local) {
                level().playLocalSound(getX(), getY(), getZ(), ModSounds.CAR_CRASH.get(), SoundSource.NEUTRAL, (float) Math.min(1.5D, before), 0.9F, false);
            }
        } else if (horizontalCollision) {
            velX *= 0.5D;
            velZ *= 0.5D;
            speed *= 0.5D;
        }

        // automatic 7-speed gearbox + rev counter
        double frac = Mth.clamp(Math.abs(speed) / v.topSpeed(), 0.0D, 1.2D);
        if (speed < -0.01D) {
            gear = -1;
            rpm = (float) (900 + Math.abs(speed) / 0.42D * 4500);
        } else if (abs < 0.01D) {
            gear = 0;
            rpm = forward > 0 ? 2500.0F : 900.0F;
        } else {
            double g = frac * 7.0D;
            gear = Mth.clamp((int) g + 1, 1, 7);
            double inGear = g - Math.floor(g);
            rpm = (float) (1800 + inGear * 6200 + (gear == 7 ? frac * 600 : 0));
        }
    }

    private void clientVisuals() {
        Vec3 fwd = Vec3.directionFromRotation(0.0F, getYRot());
        double dx = getX() - xo, dz = getZ() - zo;
        double moved = Math.sqrt(dx * dx + dz * dz);
        double signed = (dx * fwd.x + dz * fwd.z) >= 0 ? moved : -moved;
        wheelSpin += (float) (signed / WHEEL_RADIUS);
        if (isControlledByLocalInstance() && getControllingPassenger() != null) {
            visualSteer = steer;
        } else {
            float yawDelta = Mth.wrapDegrees(getYRot() - lastYaw);
            float target = moved > 0.02D ? Mth.clamp(-yawDelta / (float) Math.max(1.5D, moved * 4.0D), -1.0F, 1.0F) : visualSteer * 0.9F;
            visualSteer += (target - visualSteer) * 0.3F;
        }
        lastYaw = getYRot();
        float rollTarget = -visualSteer * (float) Math.min(1.0D, moved / 1.2D) * 0.055F;
        float pitchTarget = Mth.clamp((float) (moved - lastMoved) * -0.9F, -0.045F, 0.045F);
        lastMoved = moved;
        bodyRoll += (rollTarget - bodyRoll) * 0.18F;
        bodyPitch += (pitchTarget - bodyPitch) * 0.15F;
        float wingTarget = (getVariant() == SupercarVariant.PHANTOM && (moved > 0.9D || (isControlledByLocalInstance() && braking && moved > 0.4D))) ? 1.0F : 0.0F;
        wingLift += Mth.clamp(wingTarget - wingLift, -0.05F, 0.05F);

        Vec3 left = new Vec3(fwd.z, 0, -fwd.x).scale(-1.0D);
        // tyre smoke while drifting / burnouts
        boolean smoke = (isControlledByLocalInstance() && (drift > 0.22F && moved > 0.25D || VehicleInput.brake && moved > 0.35D && getControllingPassenger() != null));
        if (smoke || (!isControlledByLocalInstance() && Math.abs(visualSteer) > 0.85F && moved > 0.8D)) {
            for (int s = -1; s <= 1; s += 2) {
                Vec3 p = position().add(fwd.scale(-1.3D)).add(left.scale(0.85D * s));
                level().addParticle(ParticleTypes.CLOUD, p.x, p.y + 0.15D, p.z, (random.nextDouble() - 0.5D) * 0.05D, 0.03D, (random.nextDouble() - 0.5D) * 0.05D);
            }
        }
        // exhaust: idle puffs, nitro flames
        Vec3 exhaust = position().add(fwd.scale(-2.15D)).add(0, 0.44D, 0);
        if (isNitroVisible()) {
            for (int s = -1; s <= 1; s += 2) {
                Vec3 p = exhaust.add(left.scale(0.28D * s));
                level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, -fwd.x * 0.25D, 0.0D, -fwd.z * 0.25D);
                level().addParticle(ParticleTypes.FLAME, p.x, p.y, p.z, -fwd.x * 0.18D, 0.01D, -fwd.z * 0.18D);
            }
        } else if (!getPassengers().isEmpty() && random.nextInt(4) == 0) {
            Vec3 p = exhaust.add(left.scale(0.28D * (random.nextBoolean() ? 1 : -1)));
            level().addParticle(ParticleTypes.SMOKE, p.x, p.y, p.z, -fwd.x * 0.04D, 0.01D, -fwd.z * 0.04D);
        }
        if (crashShake > 10 && random.nextInt(2) == 0) {
            level().addParticle(ParticleTypes.CRIT, getX() + fwd.x * 2, getY() + 0.6D, getZ() + fwd.z * 2, fwd.x * 0.2, 0.2, fwd.z * 0.2);
        }
    }

    private void serverTick(ServerLevel level) {
        double dx = getX() - xo, dz = getZ() - zo;
        double prev = serverSpeed;
        serverSpeed = Math.sqrt(dx * dx + dz * dz);
        boolean driven = getControllingPassenger() != null;

        if (autoCloseDoor >= 0 && --autoCloseTicks <= 0) {
            setPartOpen(autoCloseDoor, false);
            level.playSound(null, blockPosition(), ModSounds.CAR_DOOR.get(), SoundSource.NEUTRAL, 1.0F, 0.85F);
            autoCloseDoor = -1;
        }
        if (entityData.get(DATA_TURNTABLE) && !driven) {
            setYRot(getYRot() + 0.6F);
        }
        if (driven && tickCount % 5 == 0) {
            float pitch = 0.55F + (float) Math.min(1.3D, serverSpeed / getVariant().topSpeed() * 1.1D);
            level.playSound(null, blockPosition(), ModSounds.CAR_ENGINE.get(), SoundSource.NEUTRAL, 0.55F + (float) Math.min(0.6D, serverSpeed), pitch);
        }
        // crash heard by everyone (no explosion, just metal crunch + sparks)
        if (prev > 0.55D && serverSpeed < prev * 0.3D) {
            level.playSound(null, blockPosition(), ModSounds.CAR_CRASH.get(), SoundSource.NEUTRAL, (float) Math.min(2.0D, prev * 1.2D), 0.9F);
            level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.8D, getZ(), 25, 1.0D, 0.4D, 1.0D, 0.3D);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.8D, getZ(), 6, 0.8D, 0.3D, 0.8D, 0.02D);
        }
        // ramming mobs (the beast too!)
        if (driven && serverSpeed > 0.3D) {
            Vec3 dir = new Vec3(dx, 0, dz).normalize();
            AABB front = getBoundingBox().inflate(0.4D, 0.2D, 0.4D).move(dir.scale(1.4D));
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, front, x -> x.isAlive() && x.getRootVehicle() != this && !(x instanceof Player p && p.isSpectator()))) {
                if (e.hurt(damageSources().flyIntoWall(), (float) (serverSpeed * 9.0D))) {
                    e.push(dir.x * serverSpeed * 1.1D, 0.35D + serverSpeed * 0.2D, dir.z * serverSpeed * 1.1D);
                    e.hurtMarked = true;
                }
            }
        }
    }

    // ------------------------------------------------------------------ interpolation for non-simulating clients
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpSteps = 5;
    }

    @Override
    public double lerpTargetX() {
        return lerpSteps > 0 ? lerpX : getX();
    }

    @Override
    public double lerpTargetY() {
        return lerpSteps > 0 ? lerpY : getY();
    }

    @Override
    public double lerpTargetZ() {
        return lerpSteps > 0 ? lerpZ : getZ();
    }

    @Override
    public float lerpTargetYRot() {
        return lerpSteps > 0 ? lerpYRot : getYRot();
    }

    private void tickLerp() {
        if (lerpSteps <= 0) return;
        double nx = getX() + (lerpX - getX()) / lerpSteps;
        double ny = getY() + (lerpY - getY()) / lerpSteps;
        double nz = getZ() + (lerpZ - getZ()) / lerpSteps;
        setYRot(getYRot() + Mth.wrapDegrees(lerpYRot - getYRot()) / lerpSteps);
        lerpSteps--;
        setPos(nx, ny, nz);
    }

    // ------------------------------------------------------------------ seats
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (!hasPassenger(passenger)) return;
        int index = getPassengers().indexOf(passenger);
        Vec3 fwd = Vec3.directionFromRotation(0.0F, getYRot());
        Vec3 left = new Vec3(fwd.z, 0, -fwd.x).scale(-1.0D);
        double lateral = index == 0 ? 0.40D : -0.40D;
        double hipY = 0.05D;                     // low supercar seating position
        double attach = passenger.getVehicleAttachmentPoint(this).y;
        if (attach < 0.1D && passenger.getBbHeight() > 1.4F) attach = 0.6D;
        Vec3 seat = position().add(left.scale(lateral)).add(fwd.scale(-0.05D));
        callback.accept(passenger, seat.x, getY() + hipY - attach, seat.z);
        if (passenger instanceof LivingEntity living && !(passenger instanceof Player)) {
            living.setYBodyRot(getYRot());
        }
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (passenger instanceof Player p && !level().isClientSide && getPassengers().size() == 1) {
            p.setYRot(getYRot());
        }
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        clampRotation(passenger);
    }

    private void clampRotation(Entity passenger) {
        passenger.setYBodyRot(getYRot());
        float delta = Mth.wrapDegrees(passenger.getYRot() - getYRot());
        float clamped = Mth.clamp(delta, -130.0F, 130.0F);
        passenger.yRotO += clamped - delta;
        passenger.setYRot(passenger.getYRot() + clamped - delta);
        passenger.setYHeadRot(passenger.getYRot());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 fwd = Vec3.directionFromRotation(0.0F, getYRot());
        Vec3 left = new Vec3(fwd.z, 0, -fwd.x).scale(-1.0D);
        int index = getPassengers().indexOf(passenger);
        double first = index <= 0 ? 1.0D : -1.0D;
        for (double s : new double[]{first, -first}) {
            Vec3 p = position().add(left.scale(1.7D * s));
            AABB box = passenger.getDimensions(passenger.getPose()).makeBoundingBox(p);
            if (level().noCollision(passenger, box) && !level().getBlockState(net.minecraft.core.BlockPos.containing(p).below()).isAir()) return p;
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    // ------------------------------------------------------------------ save
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_VARIANT, tag.getInt("Variant"));
        entityData.set(DATA_PARTS, tag.getByte("Parts"));
        entityData.set(DATA_LIGHTS, tag.getBoolean("Lights"));
        entityData.set(DATA_SHOWROOM, tag.getBoolean("Showroom"));
        entityData.set(DATA_TURNTABLE, tag.getBoolean("Turntable"));
        if (tag.contains("Nitro")) nitro = tag.getDouble("Nitro");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Variant", entityData.get(DATA_VARIANT));
        tag.putByte("Parts", entityData.get(DATA_PARTS));
        tag.putBoolean("Lights", lightsOn());
        tag.putBoolean("Showroom", isShowroomCar());
        tag.putBoolean("Turntable", entityData.get(DATA_TURNTABLE));
        tag.putDouble("Nitro", nitro);
    }
}
