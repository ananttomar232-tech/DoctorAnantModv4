package com.dranant.item;

import com.dranant.clinic.ClinicTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * RIGHT-click on the ground: places the block and the whole clinic is built ON THAT SPOT, entrance facing you.
 * LEFT-click on a block: places just the summoner block (handled in ModGameEvents#onLeftClickBlock); right-click it later to build there.
 */
public class ClinicSummonerItem extends BlockItem {
    private final int tier;

    public ClinicSummonerItem(Block block, int tier, Properties properties) {
        super(block, properties);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Level " + tier + " - " + ClinicTiers.name(tier)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Place it: the clinic is built right on that spot").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("Left-click: place only the block").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(String.format("Shop price: %,d Diamonds", ClinicTiers.price(tier))).withStyle(ChatFormatting.GOLD));
    }
}
