package com.dranant.block;

import com.dranant.clinic.ClinicBuilder;
import com.dranant.clinic.ClinicTiers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Clinic Summoner block (tiers 1-6). PLACING it (normal right-click) builds that tier's clinic right on the spot,
 * centred on the block, entrance facing you. Placed with LEFT-click it stays as a decorative block until you
 * right-click it - then the clinic is built on that block's spot.
 */
public class ClinicSummonerBlock extends Block {
    public static final MapCodec<ClinicSummonerBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("tier").forGetter(ClinicSummonerBlock::getTier),
            propertiesCodec()
    ).apply(instance, ClinicSummonerBlock::new));

    private final int tier;

    public ClinicSummonerBlock(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    public String getClinicName() {
        return ClinicTiers.name(tier);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            ClinicBuilder.summonAt(serverLevel, player, tier, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel && placer instanceof Player player) {
            // build after this tick so the block placement fully completes first
            serverLevel.getServer().execute(() -> {
                if (serverLevel.getBlockState(pos).is(this)) ClinicBuilder.summonAt(serverLevel, player, tier, pos);
            });
        }
    }
}
