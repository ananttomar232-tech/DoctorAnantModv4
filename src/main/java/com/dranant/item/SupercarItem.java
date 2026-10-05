package com.dranant.item;

import com.dranant.entity.SupercarEntity;
import com.dranant.entity.SupercarVariant;
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

/** Places one of the supercars, facing away from the player. */
public class SupercarItem extends Item {
    private final SupercarVariant variant;

    public SupercarItem(SupercarVariant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public SupercarVariant getVariant() {
        return variant;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
            SupercarEntity car = ModEntities.SUPERCAR.get().create(level);
            if (car == null) return InteractionResult.FAIL;
            Player player = context.getPlayer();
            car.setVariant(variant);
            car.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player != null ? player.getYRot() : 0.0F, 0.0F);
            if (!level.noCollision(car, car.getBoundingBox())) {
                if (player != null) player.displayClientMessage(Component.literal("Not enough space to park here!").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            level.addFreshEntity(car);
            if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(variant.horsepower() + " HP  |  0-100 km/h " + variant.zeroToHundred() + "  |  " + variant.topSpeedKmh() + " km/h")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("W/S throttle-brake, A/D steer, SPACE handbrake drift, SPRINT nitro").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("H horn, J lights, U doors, N bonnet, M boot").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Sneak+right-click door/bonnet/boot to open, sneak+punch to pick up").withStyle(ChatFormatting.DARK_GRAY));
    }
}
