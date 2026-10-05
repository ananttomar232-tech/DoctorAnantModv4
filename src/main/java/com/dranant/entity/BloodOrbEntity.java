package com.dranant.entity;

import com.dranant.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Long-range volley: fast globs of toxic blood (damage + poison). */
public class BloodOrbEntity extends ThrowableItemProjectile {
    public BloodOrbEntity(EntityType<? extends BloodOrbEntity> type, Level level) {
        super(type, level);
    }

    public BloodOrbEntity(Level level, LivingEntity thrower) {
        super(ModEntities.BLOOD_ORB.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.REDSTONE;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.01D;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(new DustParticleOptions(new org.joml.Vector3f(0.6F, 0.0F, 0.0F), 1.2F), getX(), getY(), getZ(), 0, 0, 0);
        } else if (tickCount > 80) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity owner = getOwner();
        if (result.getEntity() instanceof LivingEntity target && target != owner) {
            if (target.hurt(damageSources().mobProjectile(this, owner instanceof LivingEntity le ? le : null), 5.0F)) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(DustParticleOptions.REDSTONE, getX(), getY(), getZ(), 15, 0.3, 0.3, 0.3, 0);
            discard();
        }
    }
}
