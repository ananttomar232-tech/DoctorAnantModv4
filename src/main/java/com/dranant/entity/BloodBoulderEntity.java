package com.dranant.entity;

import com.dranant.registry.ModEntities;
import com.dranant.registry.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Long-range attack: a huge chunk of blood-soaked rock hurled in an arc. Explodes on impact (no block damage). */
public class BloodBoulderEntity extends ThrowableItemProjectile {
    public BloodBoulderEntity(EntityType<? extends BloodBoulderEntity> type, Level level) {
        super(type, level);
    }

    public BloodBoulderEntity(Level level, LivingEntity thrower) {
        super(ModEntities.BLOOD_BOULDER.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.REDSTONE_BLOCK;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05D;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(DustParticleOptions.REDSTONE, getX(), getY(), getZ(), 0, 0, 0);
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0.02, 0);
        } else if (tickCount > 200) {
            discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel level)) return;
        Vec3 p = position();
        level.playSound(null, blockPosition(), ModSounds.GROUND_SLAM.get(), SoundSource.HOSTILE, 2.0F, 1.1F);
        level.sendParticles(ParticleTypes.EXPLOSION, p.x, p.y, p.z, 3, 0.8, 0.4, 0.8, 0);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()), p.x, p.y, p.z, 60, 1.2, 0.6, 1.2, 0.3);
        Entity owner = getOwner();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.5D), x -> x != owner && x.isAlive())) {
            double d = e.distanceToSqr(this);
            if (d > 3.5 * 3.5) continue;
            if (e.hurt(damageSources().mobProjectile(this, owner instanceof LivingEntity le ? le : null), 12.0F * (float) (1.0 - Math.sqrt(d) / 5.0))) {
                Vec3 push = e.position().subtract(p).normalize();
                e.push(push.x * 1.2, 0.5, push.z * 1.2);
                e.hurtMarked = true;
            }
        }
        discard();
    }
}
