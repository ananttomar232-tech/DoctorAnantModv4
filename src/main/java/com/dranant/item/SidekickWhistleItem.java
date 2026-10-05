package com.dranant.item;

import com.dranant.dialogue.Speech;
import com.dranant.entity.SidekickEntity;
import com.dranant.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Calls your sidekick to you (spawns him the first time). */
public class SidekickWhistleItem extends Item {
    public SidekickWhistleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sl) {
            SidekickEntity mine = null;
            for (SidekickEntity s : sl.getEntitiesOfClass(SidekickEntity.class, new AABB(player.blockPosition()).inflate(256.0D))) {
                if (s.getOwnerId().map(id -> id.equals(player.getUUID())).orElse(false)) {
                    mine = s;
                    break;
                }
            }
            if (mine == null) {
                mine = ModEntities.SIDEKICK.get().create(sl);
                if (mine == null) return InteractionResultHolder.fail(stack);
                mine.setOwner(player);
                mine.setCustomName(Component.literal("Sidekick Bunty").withStyle(ChatFormatting.YELLOW));
                mine.setCustomNameVisible(true);
                mine.moveTo(player.getX() + 1.0D, player.getY(), player.getZ() + 1.0D, player.getYRot(), 0.0F);
                sl.addFreshEntity(mine);
                Speech.say(mine, "Bunty haazir hai, Doctor saab!", Speech.STYLE_FRIENDLY, 60);
            } else {
                if (mine.isPassenger()) mine.stopRiding();
                mine.teleportTo(player.getX() + 1.0D, player.getY(), player.getZ() + 1.0D);
                Speech.say(mine, "Aa gaya boss!", Speech.STYLE_FRIENDLY, 40);
            }
            level.playSound(null, player.blockPosition(), com.dranant.registry.ModSounds.SIDEKICK_WHISTLE.get(), SoundSource.PLAYERS, 1.0F, 1.6F);
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Calls your sidekick (spawns him the first time)").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("He rides with you and shoots the Blood Beast down to 50%").withStyle(ChatFormatting.AQUA));
    }
}
