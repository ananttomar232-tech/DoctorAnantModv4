package com.dranant.entity;

import com.dranant.dialogue.Speech;
import com.dranant.registry.ModItems;
import com.dranant.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dr. Anant's sidekick: an NPC with a Minecraft player skin. Follows his owner everywhere, hops on whatever the owner
 * rides (the Dr. Anant Bike, boats, camels, horses, minecarts...) and, when he spots the Mutant Blood Beast, opens fire
 * from wherever he is sitting - but stops at 50% HP so the doctor can handcuff and cure it.
 */
public class SidekickEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(SidekickEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_SHOOTING = SynchedEntityData.defineId(SidekickEntity.class, EntityDataSerializers.BOOLEAN);
    private static final double SIGHT_RANGE = 40.0D;

    private int mountCooldown;
    private boolean announcedHalf;

    public SidekickEntity(EntityType<? extends SidekickEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.SIDEKICK_BLASTER.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_SHOOTING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ShootBeastGoal(this));
        goalSelector.addGoal(2, new FollowOwnerGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    // ------------------------------------------------------------------ owner
    public Optional<UUID> getOwnerId() {
        return entityData.get(DATA_OWNER);
    }

    public void setOwner(@Nullable Player player) {
        entityData.set(DATA_OWNER, player == null ? Optional.empty() : Optional.of(player.getUUID()));
    }

    @Nullable
    public Player getOwner() {
        return getOwnerId().map(id -> level().getPlayerByUUID(id)).orElse(null);
    }

    public boolean isShooting() {
        return entityData.get(DATA_SHOOTING);
    }

    // ------------------------------------------------------------------ riding along
    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        Player owner = getOwner();
        if (owner == null) return;
        if (--mountCooldown > 0) return;
        mountCooldown = 10;
        Entity vehicle = owner.getVehicle();
        if (vehicle != null && getVehicle() != vehicle && !(vehicle instanceof MutantBloodBeastEntity)) {
            if (distanceTo(owner) > 12.0D) teleportTo(owner.getX(), owner.getY(), owner.getZ());
            if (getVehicle() != null) stopRiding();
            if (startRiding(vehicle, true)) {
                Speech.say(this, pickRideLine(), Speech.STYLE_FRIENDLY, 50);
            }
        } else if (vehicle == null && getVehicle() != null && !(getVehicle() instanceof Player)) {
            stopRiding();
            Vec3 behind = owner.position().subtract(owner.getLookAngle().multiply(1.5, 0, 1.5));
            teleportTo(behind.x, owner.getY(), behind.z);
        }
    }

    private String pickRideLine() {
        String[] lines = {"Chalo bhai, ride pe chalte hain!", "Main peeche baith gaya, chalao!", "Full speed, Doctor saab!", "Helmet pehna? Chalo!"};
        return lines[random.nextInt(lines.length)];
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide || hand != InteractionHand.MAIN_HAND) return InteractionResult.sidedSuccess(level().isClientSide);
        if (getOwnerId().isEmpty()) {
            setOwner(player);
            Speech.say(this, "Ab main aapka sidekick hoon, Doctor saab!", Speech.STYLE_FRIENDLY);
            player.sendSystemMessage(Component.literal(getName().getString() + " is now your sidekick!").withStyle(ChatFormatting.GREEN));
        } else if (getOwnerId().get().equals(player.getUUID())) {
            Speech.say(this, "Ready hoon! Beast dikhe to bata dena... ya main khud dekh lunga!", Speech.STYLE_FRIENDLY);
        } else {
            Speech.say(this, "Main sirf apne boss ki baat sunta hoon.", Speech.STYLE_RUDE);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        getOwnerId().ifPresent(id -> tag.putUUID("Owner", id));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) entityData.set(DATA_OWNER, Optional.of(tag.getUUID("Owner")));
        setInvulnerable(true);
    }

    // ------------------------------------------------------------------ goals
    /** Walks after the owner on foot; teleports if left far behind. */
    static class FollowOwnerGoal extends Goal {
        private final SidekickEntity mob;
        private int repath;

        FollowOwnerGoal(SidekickEntity mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Player owner = mob.getOwner();
            return owner != null && !mob.isPassenger() && owner.getVehicle() == null && mob.distanceToSqr(owner) > 9.0D;
        }

        @Override
        public boolean canContinueToUse() {
            Player owner = mob.getOwner();
            return owner != null && !mob.isPassenger() && mob.distanceToSqr(owner) > 4.0D;
        }

        @Override
        public void tick() {
            Player owner = mob.getOwner();
            if (owner == null) return;
            mob.getLookControl().setLookAt(owner, 10.0F, mob.getMaxHeadXRot());
            if (mob.distanceToSqr(owner) > 24 * 24) {
                mob.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                return;
            }
            if (--repath <= 0) {
                repath = 10;
                mob.getNavigation().moveTo(owner, mob.distanceToSqr(owner) > 64 ? 1.4D : 1.0D);
            }
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }
    }

    /** Fires at the Mutant Blood Beast (even while riding) until it is down to 50% health. */
    static class ShootBeastGoal extends Goal {
        private final SidekickEntity mob;
        @Nullable
        private MutantBloodBeastEntity target;
        private int cooldown;
        private int burst;
        private int talk;

        ShootBeastGoal(SidekickEntity mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.LOOK));
        }

        private boolean valid(MutantBloodBeastEntity beast) {
            return beast.isAlive() && !beast.isHandcuffed() && !beast.isBeingCured()
                    && beast.getHealth() > beast.getMaxHealth() * 0.5F && mob.hasLineOfSight(beast);
        }

        @Override
        public boolean canUse() {
            List<MutantBloodBeastEntity> beasts = mob.level().getEntitiesOfClass(MutantBloodBeastEntity.class,
                    new AABB(mob.blockPosition()).inflate(SIGHT_RANGE), this::valid);
            if (beasts.isEmpty()) {
                checkHalf();
                return false;
            }
            beasts.sort((a, b) -> Double.compare(a.distanceToSqr(mob), b.distanceToSqr(mob)));
            target = beasts.get(0);
            return true;
        }

        private void checkHalf() {
            if (mob.announcedHalf || mob.level().isClientSide) return;
            List<MutantBloodBeastEntity> near = mob.level().getEntitiesOfClass(MutantBloodBeastEntity.class, new AABB(mob.blockPosition()).inflate(SIGHT_RANGE),
                    b -> b.isAlive() && b.getHealth() <= b.getMaxHealth() * 0.5F);
            if (!near.isEmpty()) {
                mob.announcedHalf = true;
                Speech.say(mob, "50% ho gaya! Doctor saab, ab handcuff lagao!", Speech.STYLE_FRIENDLY, 80);
                Player owner = mob.getOwner();
                if (owner != null) owner.displayClientMessage(Component.literal("Sidekick: Beast is at 50% - HANDCUFF IT NOW!").withStyle(ChatFormatting.GOLD), true);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && valid(target) && mob.distanceTo(target) < SIGHT_RANGE + 8;
        }

        @Override
        public void start() {
            mob.announcedHalf = false;
            mob.entityData.set(DATA_SHOOTING, true);
            Speech.say(mob, "BEAST! Fire fire fire!", Speech.STYLE_RUDE, 50);
            cooldown = 10;
        }

        @Override
        public void stop() {
            mob.entityData.set(DATA_SHOOTING, false);
            target = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (target == null || !(mob.level() instanceof ServerLevel level)) return;
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (mob.isPassenger()) {
                double dx = target.getX() - mob.getX(), dz = target.getZ() - mob.getZ();
                float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
                mob.setYHeadRot(yaw);
            }
            if (--cooldown > 0) return;
            // short 3-round bursts: ~1.5 damage/sec on his own - he softens the beast, the doctor does the real work
            burst++;
            cooldown = burst % 3 == 0 ? 70 : 4;
            Vec3 eye = mob.getEyePosition();
            Vec3 aim = target.position().add(0, target.getBbHeight() * 0.6D, 0).subtract(eye).normalize();
            SidekickBulletEntity bullet = new SidekickBulletEntity(level, mob);
            bullet.setPos(eye.x + aim.x * 0.6, eye.y - 0.2 + aim.y * 0.6, eye.z + aim.z * 0.6);
            bullet.shoot(aim.x, aim.y, aim.z, 3.2F, 1.5F);
            level.addFreshEntity(bullet);
            mob.swing(InteractionHand.MAIN_HAND);
            level.playSound(null, mob.blockPosition(), ModSounds.SIDEKICK_SHOT.get(), SoundSource.NEUTRAL, 1.0F, 0.9F + mob.random.nextFloat() * 0.3F);
            level.sendParticles(ParticleTypes.SMOKE, eye.x + aim.x, eye.y - 0.2 + aim.y, eye.z + aim.z, 3, 0.05, 0.05, 0.05, 0.01);
            level.sendParticles(ParticleTypes.FLAME, eye.x + aim.x, eye.y - 0.2 + aim.y, eye.z + aim.z, 2, 0.02, 0.02, 0.02, 0.01);
            if (++talk % 12 == 0) {
                String[] lines = {"Le goli kha!", "Doctor saab, main cover deta hoon!", "Aur paas mat aana, monster!", "Reload... aur fire!"};
                Speech.say(mob, lines[mob.random.nextInt(lines.length)], Speech.STYLE_RUDE, 40);
            }
        }
    }
}
