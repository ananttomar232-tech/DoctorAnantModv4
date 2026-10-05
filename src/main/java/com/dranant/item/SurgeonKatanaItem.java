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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Surgeon's Katana: 12 damage, fast swings. Right-click = Scalpel Dash - you blink ~8 blocks forward through enemies,
 * slicing everything you pass for 10 damage (3 s cooldown). Perfect for dodging the beast's slam.
 */
public class SurgeonKatanaItem extends SwordItem {
    public SurgeonKatanaItem(Properties properties) {
        super(Tiers.NETHERITE, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isPassenger()) return InteractionResultHolder.pass(stack);
        player.getCooldowns().addCooldown(this, 60);
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        Vec3 from = player.position();
        player.setDeltaMovement(flat.x * 2.6D, 0.25D, flat.z * 2.6D);
        player.hurtMarked = true;
        player.fallDistance = 0;
        if (level instanceof ServerLevel server) {
            Vec3 to = from.add(flat.scale(8.0D));
            AABB path = new AABB(from, to).inflate(1.6D, 1.5D, 1.6D);
            Set<LivingEntity> hit = new HashSet<>();
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, path, x -> x != player && x.isAlive() && !(x instanceof com.dranant.entity.SidekickEntity))) {
                if (hit.add(e)) {
                    e.invulnerableTime = 0;
                    e.hurt(player.damageSources().playerAttack(player), 10.0F);
                    server.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 2, 0.3, 0.3, 0.3, 0);
                }
            }
            DustParticleOptions trail = new DustParticleOptions(new Vector3f(0.75F, 0.95F, 1.0F), 1.3F);
            for (double d = 0; d < 8.0D; d += 0.35D) {
                Vec3 p = from.add(flat.scale(d));
                server.sendParticles(trail, p.x, p.y + 1.0, p.z, 2, 0.1, 0.35, 0.1, 0);
            }
            server.sendParticles(ParticleTypes.CLOUD, from.x, from.y + 0.2, from.z, 10, 0.3, 0.1, 0.3, 0.05);
            server.playSound(null, player.blockPosition(), ModSounds.KATANA_DASH.get(), SoundSource.PLAYERS, 1.2F, 1.1F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click: Scalpel Dash (8 blocks, 10 dmg)").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("3 s cooldown - dash out of the beast's slam!").withStyle(ChatFormatting.GRAY));
    }
}
