package com.dranant.item;

import com.dranant.entity.MutantBloodBeastEntity;
import com.dranant.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Defibrillator Hammer: a crushing 17-damage hammer. Right-click = CLEAR! shockwave: 12 damage in a 5-block radius
 * and the Mutant Blood Beast is electro-stunned for 1.5 s (15 s cooldown). Every hit on the beast crackles with sparks.
 */
public class DefibrillatorHammerItem extends SwordItem {
    public DefibrillatorHammerItem(Properties properties) {
        super(Tiers.NETHERITE, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 16, 0.4, 0.5, 0.4, 0.2);
            Vec3 push = target.position().subtract(attacker.position()).multiply(1, 0, 1).normalize();
            if (!(target instanceof MutantBloodBeastEntity)) target.push(push.x * 0.9, 0.3, push.z * 0.9);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.getCooldowns().addCooldown(this, 300);
        player.swing(hand, true);
        if (level instanceof ServerLevel server) {
            Vec3 c = player.position();
            server.playSound(null, player.blockPosition(), ModSounds.DEFIB_SHOCK.get(), SoundSource.PLAYERS, 1.6F, 1.0F);
            server.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0F, 1.6F);
            for (int ring = 1; ring <= 5; ring++) {
                for (int i = 0; i < ring * 10; i++) {
                    double a = Math.PI * 2 * i / (ring * 10);
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x + Math.cos(a) * ring, c.y + 0.2, c.z + Math.sin(a) * ring, 1, 0, 0.15, 0, 0.02);
                }
            }
            server.sendParticles(ParticleTypes.FLASH, c.x, c.y + 1, c.z, 1, 0, 0, 0, 0);
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5.0D, 2.5D, 5.0D),
                    x -> x != player && x.isAlive() && !(x instanceof com.dranant.entity.SidekickEntity) && x.distanceTo(player) <= 5.5F)) {
                e.invulnerableTime = 0;
                e.hurt(player.damageSources().playerAttack(player), 12.0F);
                if (e instanceof MutantBloodBeastEntity beast && !beast.isHandcuffed() && !beast.isBeingCured()) {
                    beast.stun(30);
                    player.displayClientMessage(Component.literal("CLEAR! The beast is electro-stunned!").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), true);
                } else {
                    Vec3 push = e.position().subtract(c).multiply(1, 0, 1).normalize();
                    e.push(push.x * 1.2, 0.5, push.z * 1.2);
                    e.hurtMarked = true;
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click: CLEAR! shockwave (5 blocks, 12 dmg)").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("Stuns the Mutant Blood Beast - 15 s cooldown").withStyle(ChatFormatting.GRAY));
    }
}
