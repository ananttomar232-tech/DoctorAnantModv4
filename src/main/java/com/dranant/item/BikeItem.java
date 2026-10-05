package com.dranant.item;

import com.dranant.entity.BikeEntity;
import com.dranant.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** Places the Dr. Anant Bike. */
public class BikeItem extends Item {
    public BikeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
            BikeEntity bike = ModEntities.DR_ANANT_BIKE.get().create(level);
            if (bike == null) return InteractionResult.FAIL;
            Player player = context.getPlayer();
            bike.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player != null ? player.getYRot() : 0.0F, 0.0F);
            level.addFreshEntity(bike);
            if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("2 seats - your sidekick rides with you!").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("W/S throttle, A/D steer, punch to pick up").withStyle(ChatFormatting.GRAY));
    }
}
