package com.dranant.entity.ai;

import com.dranant.dialogue.Speech;
import com.dranant.entity.BloodBoulderEntity;
import com.dranant.entity.BloodOrbEntity;
import com.dranant.entity.MutantBloodBeastEntity;
import com.dranant.registry.ModEffects;
import com.dranant.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;

/** The beast's v4 move set: long range, short range, crowd control, grabs and agility. */
public final class BossAbilities {
    private static final EnumSet<Goal.Flag> ROOTED = EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP);

    // =================================================================== LONG RANGE: boulder throw
    public static class BoulderThrow extends BossAbilityGoal {
        public BoulderThrow(MutantBloodBeastEntity beast) {
            super(beast, "boulder", 34, 260, 170, MutantBloodBeastEntity.ACTION_THROW_WINDUP, 9.0D, 42.0D, ROOTED);
        }

        @Override
        protected boolean extraCondition(LivingEntity target) {
            return beast.hasLineOfSight(target);
        }

        @Override
        protected void onTick(ServerLevel level, LivingEntity target, int t) {
            if (t < 22) {
                BlockState ground = level.getBlockState(beast.blockPosition().below());
                if (ground.isAir()) ground = Blocks.STONE.defaultBlockState();
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), beast.getX(), beast.getY() + 0.2, beast.getZ(), 6, 0.8, 0.1, 0.8, 0.1);
                if (t == 4) level.playSound(null, beast.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
            }
            if (t == 22) {
                beast.setActionState(MutantBloodBeastEntity.ACTION_THROW);
                BloodBoulderEntity boulder = new BloodBoulderEntity(level, beast);
                Vec3 from = beast.position().add(0, 4.2, 0);
                boulder.setPos(from.x, from.y, from.z);
                Vec3 to = target.position().add(target.getDeltaMovement().scale(12)).subtract(from);
                double horiz = Math.sqrt(to.x * to.x + to.z * to.z);
                boulder.shoot(to.x, to.y + horiz * 0.28, to.z, (float) Mth.clamp(horiz / 16.0, 0.9, 2.4), 2.0F);
                level.addFreshEntity(boulder);
                level.playSound(null, beast.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 1.2F);
            }
        }
    }

    // =================================================================== LONG RANGE: blood spit volley
    public static class BloodSpit extends BossAbilityGoal {
        public BloodSpit(MutantBloodBeastEntity beast) {
            super(beast, "spit", 30, 160, 100, MutantBloodBeastEntity.ACTION_SPIT, 5.0D, 30.0D, EnumSet.of(Flag.LOOK));
        }

        @Override
        protected boolean extraCondition(LivingEntity target) {
            return beast.hasLineOfSight(target);
        }

        @Override
        protected void onTick(ServerLevel level, LivingEntity target, int t) {
            int shots = beast.isEnraged() ? 5 : 3;
            if (t >= 8 && (t - 8) % 5 == 0 && (t - 8) / 5 < shots) {
                BloodOrbEntity orb = new BloodOrbEntity(level, beast);
                Vec3 mouth = beast.position().add(beast.getLookAngle().scale(1.2)).add(0, 3.6, 0);
                orb.setPos(mouth.x, mouth.y, mouth.z);
                Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0).add(target.getDeltaMovement().scale(6)).subtract(mouth);
                orb.shoot(aim.x, aim.y, aim.z, 1.8F, 3.0F);
                level.addFreshEntity(orb);
                level.playSound(null, beast.blockPosition(), SoundEvents.LLAMA_SPIT, SoundSource.HOSTILE, 1.5F, 0.6F);
            }
        }
    }

    // =================================================================== SHORT RANGE: 3-hit claw combo
    public static class ClawCombo extends BossAbilityGoal {
        public ClawCombo(MutantBloodBeastEntity beast) {
            super(beast, "claw", 30, 90, 60, MutantBloodBeastEntity.ACTION_CLAW, 0.0D, 4.5D, EnumSet.of(Flag.LOOK, Flag.MOVE));
        }

        @Override
        protected void onTick(ServerLevel level, LivingEntity target, int t) {
            if (t == 7 || t == 15 || t == 23) {
                beast.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                Vec3 look = beast.getLookAngle().multiply(1, 0, 1).normalize();
                Vec3 front = beast.position().add(look.scale(2.0));
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, beast.getY() + 2.0, front.z, 3, 0.8, 0.4, 0.8, 0);
                level.playSound(null, beast.blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 1.5F, 0.8F + t * 0.01F);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, beast.getBoundingBox().inflate(4.5D), x -> x != beast && x.isAlive())) {
                    Vec3 to = e.position().subtract(beast.position()).multiply(1, 0, 1);
                    if (to.length() > 4.8 || (to.length() > 0.5 && to.normalize().dot(look) < 0.3)) continue;
                    if (e.hurt(beast.damageSources().mobAttack(beast), 7.0F)) {
                        Vec3 push = to.length() > 0.01 ? to.normalize() : look;
                        e.push(push.x * (t == 23 ? 1.8 : 0.6), t == 23 ? 0.6 : 0.2, push.z * (t == 23 ? 1.8 : 0.6));
                        e.hurtMarked = true;
                    }
                }
            }
            if (t < 23) beast.setDeltaMovement(beast.getLookAngle().multiply(0.12, 0, 0.12).add(0, beast.getDeltaMovement().y, 0));
        }
    }

    // =================================================================== CROWD CONTROL: stun slam
    public static class StunSlam extends BossAbilityGoal {
        private boolean airborne;

        public StunSlam(MutantBloodBeastEntity beast) {
            super(beast, "stunslam", 60, 380, 240, MutantBloodBeastEntity.ACTION_SLAM_WINDUP, 0.0D, 10.0D, ROOTED);
        }

        @Override
        protected void onStart() {
            airborne = false;
            Speech.say(beast, "STAND STILL!!", Speech.STYLE_BOSS, 40);
        }

        @Override
        protected void onTick(ServerLevel level, LivingEntity target, int t) {
            if (t == 10) {
                beast.setDeltaMovement(0, 1.1, 0);
                beast.hasImpulse = true;
                beast.setActionState(MutantBloodBeastEntity.ACTION_AIRBORNE);
                airborne = true;
            }
            if (airborne && t > 16 && beast.onGround()) {
                airborne = false;
                timer = 999;
                level.playSound(null, beast.blockPosition(), ModSounds.GROUND_SLAM.get(), SoundSource.HOSTILE, 3.0F, 1.3F);
                DustParticleOptions spark = new DustParticleOptions(new Vector3f(1.0F, 0.9F, 0.2F), 1.8F);
                for (int ring = 1; ring <= 8; ring++)
                    for (int i = 0; i < ring * 10; i++) {
                        double a = Math.PI * 2 * i / (ring * 10);
                        level.sendParticles(spark, beast.getX() + Math.cos(a) * ring, beast.getY() + 0.2, beast.getZ() + Math.sin(a) * ring, 1, 0, 0.1, 0, 0);
                    }
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, beast.getX(), beast.getY() + 0.5, beast.getZ(), 80, 4, 0.3, 4, 0.2);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, beast.getBoundingBox().inflate(8.0D, 3.0D, 8.0D), x -> x != beast && x.isAlive())) {
                    if (e.distanceTo(beast) > 8.5) continue;
                    e.hurt(beast.damageSources().mobAttack(beast), 8.0F);
                    e.addEffect(new MobEffectInstance(ModEffects.STUNNED, 50, 0, false, true, true));
                    if (e instanceof Player p) p.displayClientMessage(Component.literal("STUNNED!").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), true);
                }
            }
        }
    }

    // =================================================================== GRAB + THROW
    public static class GrabThrow extends BossAbilityGoal {
        private boolean holding;

        public GrabThrow(MutantBloodBeastEntity beast) {
            super(beast, "grab", 64, 420, 280, MutantBloodBeastEntity.ACTION_GRAB, 0.0D, 3.8D, ROOTED);
        }

        @Override
        protected boolean extraCondition(LivingEntity target) {
            return !target.isPassenger() && target.getBbHeight() < 2.5F;
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() || (holding && timer < 64);
        }

        @Override
        protected void onStart() {
            holding = false;
        }

        @Override
        protected void onStop() {
            if (holding) release(null);
        }

        private void release(Vec3 launch) {
            holding = false;
            LivingEntity t = target;
            if (t == null) return;
            if (t.getVehicle() == beast) t.stopRiding();
            beast.setGrabbed(null);
            if (launch != null) {
                t.setDeltaMovement(launch);
                t.hurtMarked = true;
            }
        }

        @Override
        protected void onTick(ServerLevel level, LivingEntity target, int t) {
            if (t == 10) {
                if (beast.distanceTo(target) < 4.5 && !target.isPassenger() && target.startRiding(beast, true)) {
                    holding = true;
                    beast.setGrabbed(target);
                    level.playSound(null, beast.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 1.4F);
                    Speech.say(beast, "GOT YOU!", Speech.STYLE_BOSS, 40);
                    if (target instanceof Player p) p.displayClientMessage(Component.literal("GRABBED! Mash SHIFT to break free!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
                } else {
                    timer = 999;
                }
            }
            if (holding) {
                if (target.getVehicle() != beast) {
                    // broke free
                    holding = false;
                    beast.setGrabbed(null);
                    timer = 999;
                    return;
                }
                if (t % 10 == 0) target.hurt(beast.damageSources().mobAttack(beast), 2.0F);
                level.sendParticles(DustParticleOptions.REDSTONE, target.getX(), target.getY() + 1, target.getZ(), 4, 0.3, 0.4, 0.3, 0);
                if (t >= 44) {
                    Vec3 look = beast.getLookAngle().multiply(1, 0, 1).normalize();
                    release(new Vec3(look.x * 2.4, 1.0, look.z * 2.4));
                    target.hurt(beast.damageSources().mobAttack(beast), 8.0F);
                    level.playSound(null, beast.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 2.0F, 0.5F);
                    beast.setActionState(MutantBloodBeastEntity.ACTION_THROW);
                    timer = 54;
                }
            }
        }
    }

    // =================================================================== AGILITY: pounce
    public static class Pounce extends BossAbilityGoal {
        private boolean inAir;

        public Pounce(MutantBloodBeastEntity beast) {
            super(beast, "pounce", 50, 200, 120, MutantBloodBeastEntity.ACTION_SLAM_WINDUP, 7.0D, 18.0D, ROOTED);
        }

        @Override
        protected void onTick(ServerLevel level, LivingEntity target, int t) {
            if (t == 6) {
                Vec3 to = target.position().subtract(beast.position());
                double d = Math.sqrt(to.x * to.x + to.z * to.z);
                double v = Mth.clamp(d / 9.0, 0.6, 2.0);
                beast.setDeltaMovement(to.x / d * v, 0.65, to.z / d * v);
                beast.hasImpulse = true;
                beast.setActionState(MutantBloodBeastEntity.ACTION_AIRBORNE);
                inAir = true;
                level.playSound(null, beast.blockPosition(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 2.0F, 0.6F);
            }
            if (inAir && t > 10 && beast.onGround()) {
                inAir = false;
                timer = 999;
                level.sendParticles(ParticleTypes.EXPLOSION, beast.getX(), beast.getY() + 0.3, beast.getZ(), 2, 1, 0.2, 1, 0);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, beast.getBoundingBox().inflate(3.0D), x -> x != beast && x.isAlive())) {
                    if (e.hurt(beast.damageSources().mobAttack(beast), 10.0F)) {
                        Vec3 push = e.position().subtract(beast.position()).multiply(1, 0, 1).normalize();
                        e.push(push.x * 1.3, 0.5, push.z * 1.3);
                        e.hurtMarked = true;
                    }
                }
            }
        }
    }

    private BossAbilities() {}
}
