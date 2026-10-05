package com.dranant.item;

import com.dranant.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Plasma Rifle: hitscan energy beam (48 blocks), 9 damage, 0.6 s between shots. Works on foot or while riding the bike/car.
 * Deals double damage to the Mutant Blood Beast's back. Never breaks blocks.
 */
public class PlasmaRifleItem extends Item {
    public static final double RANGE = 48.0D;
    public static final float DAMAGE = 9.0F;
    public static final int COOLDOWN = 12;

    public PlasmaRifleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.getCooldowns().addCooldown(this, COOLDOWN);
        if (level instanceof ServerLevel server) {
            fire(server, player);
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void fire(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(RANGE));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        AABB sweep = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end, sweep,
                e -> e instanceof LivingEntity && e.isAlive() && e != player.getVehicle() && !e.hasPassenger(player) && !e.isAlliedTo(player)
                        && !(e instanceof com.dranant.entity.SidekickEntity), 0.3F);
        if (hit != null) end = hit.getLocation();

        // beam visuals: bright cyan core with a magenta spiral
        double len = end.distanceTo(eye);
        Vec3 start = eye.add(look.scale(0.8)).add(0, -0.15, 0);
        DustParticleOptions core = new DustParticleOptions(new Vector3f(0.2F, 1.0F, 1.0F), 1.1F);
        DustParticleOptions halo = new DustParticleOptions(new Vector3f(1.0F, 0.2F, 0.9F), 0.7F);
        Vec3 dir = end.subtract(start).normalize();
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-4) side = new Vec3(1, 0, 0);
        side = side.normalize();
        Vec3 up = side.cross(dir).normalize();
        for (double d = 0; d < len; d += 0.4D) {
            Vec3 p = start.add(dir.scale(d));
            level.sendParticles(core, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            double a = d * 2.2D;
            Vec3 q = p.add(side.scale(Math.cos(a) * 0.18)).add(up.scale(Math.sin(a) * 0.18));
            level.sendParticles(halo, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 14, 0.2, 0.2, 0.2, 0.3);
        level.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0, 0, 0, 0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PLASMA_SHOT.get(), SoundSource.PLAYERS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);

        if (hit != null) {
            Entity target = hit.getEntity();
            float dmg = DAMAGE;
            if (target instanceof com.dranant.entity.MutantBloodBeastEntity beast) {
                Vec3 facing = Vec3.directionFromRotation(0, beast.yBodyRot);
                Vec3 toShooter = player.position().subtract(beast.position()).multiply(1, 0, 1).normalize();
                if (facing.dot(toShooter) < -0.3D) dmg *= 2.0F; // shot in the back
            }
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().playerAttack(player), dmg);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click: plasma beam (48 blocks, 9 dmg)").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Back-shots on the Blood Beast deal x2").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("Usable while riding").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}
