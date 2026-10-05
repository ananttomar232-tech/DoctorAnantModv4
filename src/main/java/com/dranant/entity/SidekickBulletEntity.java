package com.dranant.entity;

import com.dranant.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Fast, gravity-free blaster round. Damage to the Mutant Blood Beast is capped so it never drops below 50% HP. */
public class SidekickBulletEntity extends ThrowableItemProjectile {
    private static final float DAMAGE = 2.0F;

    public SidekickBulletEntity(EntityType<? extends SidekickBulletEntity> type, Level level) {
        super(type, level);
    }

    public SidekickBulletEntity(Level level, LivingEntity shooter) {
        super(ModEntities.SIDEKICK_BULLET.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.IRON_NUGGET;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0D;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.CRIT, getX(), getY(), getZ(), 0, 0, 0);
        } else if (tickCount > 60) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (target == owner || target instanceof Player || target instanceof SidekickEntity) return;
        if (target instanceof MutantBloodBeastEntity beast) {
            float allowed = beast.getHealth() - beast.getMaxHealth() * 0.5F;
            if (allowed <= 0.0F) return;
            beast.invulnerableTime = 0;
            beast.hurt(damageSources().mobProjectile(this, owner instanceof LivingEntity le ? le : null), Math.min(DAMAGE, allowed));
            beast.invulnerableTime = 0;
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.2);
            discard();
        }
    }
}
