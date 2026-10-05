package com.dranant.entity;

import com.dranant.registry.ModItems;
import com.dranant.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Dr. Anant's motorbike: 2 seats (driver + sidekick). Realistic but OP handling, simulated on the rider's client:
 * W throttle (6-speed auto gearbox, 112 km/h), S strong brakes / reverse, A/D lean-steering that tightens at low speed,
 * SPACE rear brake power-slide (drift), SPRINT turbo boost (155 km/h, refills while cruising), wheelies on hard launches,
 * climbs full blocks, bounces off walls instead of breaking. Right-click to ride, punch to pick it back up.
 */
public class BikeEntity extends Entity {
    private static final double MAX_SPEED = 1.55D;
    private static final double BOOST_SPEED = 2.15D;
    private double speed;
    private float turnInput;
    private double velX, velZ;
    private double turbo = 1.0D;
    private boolean boosting;
    private int gear;
    private float rpm = 1000.0F;
    private float slide;
    // client visuals
    public float wheelRot;
    public float wheelRotO;
    public float lean;
    public float wheelie;
    public float wheelieO;
    // remote interpolation
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private float lerpYRot;

    public BikeEntity(EntityType<? extends BikeEntity> type, Level level) {
        super(type, level);
    }

    public double getSpeed() { return speed; }
    public int getSpeedKmh() { return (int) Math.round(Math.abs(speed) * 72.0D); }
    public int getGear() { return gear; }
    public float getRpm() { return rpm; }
    public double getTurbo() { return turbo; }
    public boolean isBoosting() { return boosting; }
    public float getSlide() { return slide; }

    public void honk() {
        level().playSound(null, blockPosition(), ModSounds.BIKE_HORN.get(), SoundSource.NEUTRAL, 1.5F, 1.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    public float maxUpStep() {
        return 1.0F;
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
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level().isClientSide) {
            if (player.startRiding(this)) {
                level().playSound(null, blockPosition(), ModSounds.BIKE_HORN.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) return true;
        if (source.getEntity() instanceof Player player) {
            if (!player.getAbilities().instabuild) spawnAtLocation(new ItemStack(ModItems.DR_ANANT_BIKE.get()));
            ejectPassengers();
            discard();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.DR_ANANT_BIKE.get());
    }

    // ------------------------------------------------------------------ driving
    @Override
    public void tick() {
        super.tick();
        wheelRotO = wheelRot;
        wheelieO = wheelie;
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
            drive();
        } else {
            tickLerp();
        }
        if (level().isClientSide) {
            double moved = Math.sqrt(Math.pow(getX() - xo, 2) + Math.pow(getZ() - zo, 2));
            wheelRot += (float) (moved * 2.2D);
            lean += ((-turnInput * 0.5F * (float) Math.min(1.0D, Math.abs(speed) * 1.6D)) - lean) * 0.2F;
            float wheelieTarget = isControlledByLocalInstance() && getControllingPassenger() != null && getControllingPassenger().zza > 0
                    && speed > 0.08D && speed < 0.55D ? (float) ((0.55D - speed) * 0.9D) : 0.0F;
            wheelie += (Math.min(0.42F, wheelieTarget) - wheelie) * 0.15F;
            Vec3 back = Vec3.directionFromRotation(0.0F, getYRot()).scale(-1.1D);
            if (moved > 0.05D && random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.SMOKE, getX() + back.x, getY() + 0.35D, getZ() + back.z, 0, 0.02, 0);
            }
            if (boosting) {
                level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX() + back.x, getY() + 0.4D, getZ() + back.z, back.x * 0.2, 0.01, back.z * 0.2);
            }
            if (slide > 0.25F && moved > 0.2D) {
                level().addParticle(ParticleTypes.CLOUD, getX() + back.x * 0.8, getY() + 0.1D, getZ() + back.z * 0.8, 0, 0.03, 0);
            }
        } else if (getControllingPassenger() != null && tickCount % 6 == 0) {
            double moved = Math.sqrt(Math.pow(getX() - xo, 2) + Math.pow(getZ() - zo, 2));
            if (moved > 0.05D) {
                level().playSound(null, blockPosition(), ModSounds.BIKE_ENGINE.get(), SoundSource.NEUTRAL, 0.5F, 0.7F + (float) Math.min(1.2D, moved * 0.8D));
            }
        }
    }

    private void drive() {
        LivingEntity driver = getControllingPassenger();
        boolean local = driver != null && level().isClientSide;
        float forward = driver != null ? driver.zza : 0.0F;
        turnInput = driver != null ? driver.xxa : 0.0F;
        boolean rearBrake = local && com.dranant.vehicle.VehicleInput.brake;
        boosting = local && com.dranant.vehicle.VehicleInput.boost && forward > 0 && turbo > 0.02D;
        turbo = boosting ? Math.max(0.0D, turbo - 0.008D) : Math.min(1.0D, turbo + 0.0025D);
        double top = boosting ? BOOST_SPEED : MAX_SPEED;
        if (forward > 0) {
            if (speed < -0.02D) speed = Math.min(0.0D, speed + 0.1D);
            else {
                double frac = Mth.clamp(speed / top, 0.0D, 1.0D);
                speed += (boosting ? 0.085D : 0.05D) * (1.1D - 0.6D * frac) * (1.0D - frac * frac);
            }
        } else if (forward < 0) {
            speed = speed > 0.02D ? Math.max(0.0D, speed - 0.095D) : Math.max(-0.3D, speed - 0.02D);
        } else {
            speed *= 0.985D;
            if (Math.abs(speed) < 0.004D) speed = 0.0D;
        }
        if (speed > top) speed = Math.max(top, speed - 0.03D);
        if (rearBrake) speed *= 0.965D;
        if (driver == null) speed *= 0.8D;
        if (isInWater()) speed *= 0.8D;

        double abs = Math.abs(speed);
        double turnRate = 7.5D * Math.min(1.0D, abs / 0.2D) / (1.0D + abs * 0.7D) * (rearBrake ? 1.8D : 1.0D);
        setYRot(getYRot() - (float) (turnInput * turnRate * Math.signum(speed)));
        Vec3 heading = Vec3.directionFromRotation(0.0F, getYRot()).scale(speed);
        double grip = rearBrake ? 0.1D : 0.55D;
        velX += (heading.x - velX) * grip;
        velZ += (heading.z - velZ) * grip;
        double velLen = Math.sqrt(velX * velX + velZ * velZ);
        slide = velLen > 0.05D && abs > 0.05D ? (float) Math.acos(Mth.clamp((velX * heading.x + velZ * heading.z) / (velLen * abs), -1.0D, 1.0D)) : 0.0F;

        double dy = getDeltaMovement().y;
        dy = onGround() ? -0.05D : Math.max(-2.5D, dy - 0.08D);
        setDeltaMovement(velX, dy, velZ);
        double before = velLen;
        move(MoverType.SELF, getDeltaMovement());
        if (horizontalCollision && before > 0.3D) {
            speed = -speed * 0.2D;
            velX *= -0.2D;
            velZ *= -0.2D;
        } else if (horizontalCollision) {
            speed *= 0.6D;
        }
        double frac = Mth.clamp(abs / MAX_SPEED, 0.0D, 1.4D);
        if (speed < -0.01D) { gear = -1; rpm = 2500.0F; }
        else if (abs < 0.01D) { gear = 0; rpm = forward > 0 ? 3000.0F : 1100.0F; }
        else {
            double g = Math.min(frac, 1.0D) * 6.0D;
            gear = Mth.clamp((int) g + 1, 1, 6);
            rpm = (float) (2500 + (g - Math.floor(g)) * 7500 + (frac > 1.0D ? 1500 : 0));
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpSteps = 10;
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

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (!hasPassenger(passenger)) return;
        int index = getPassengers().indexOf(passenger);
        double offset = index == 0 ? 0.1D : -0.55D;
        Vec3 dir = Vec3.directionFromRotation(0.0F, getYRot());
        callback.accept(passenger, getX() + dir.x * offset, getY() + 0.3D, getZ() + dir.z * offset);
        if (passenger instanceof LivingEntity living && !(passenger instanceof Player)) {
            living.setYBodyRot(getYRot());
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }
}
