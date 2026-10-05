package com.dranant.entity;

import com.dranant.block.FurnitureBlock;
import com.dranant.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Invisible seat used by sofas, chairs and the toilet; removed as soon as nobody sits on it. */
public class SeatEntity extends Entity {
    private Direction facing = Direction.NORTH;

    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static void sit(Level level, BlockPos pos, Player player, double height, Direction facing) {
        if (player.isPassenger() || !level.getEntitiesOfClass(SeatEntity.class, new AABB(pos)).isEmpty()) return;
        SeatEntity seat = ModEntities.SEAT.get().create(level);
        if (seat == null) return;
        seat.facing = facing;
        seat.moveTo(pos.getX() + 0.5D, pos.getY() + height, pos.getZ() + 0.5D, facing.toYRot(), 0.0F);
        level.addFreshEntity(seat);
        player.startRiding(seat);
        player.setYRot(facing.toYRot());
        player.setYHeadRot(facing.toYRot());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (getPassengers().isEmpty() || !(level().getBlockState(blockPosition()).getBlock() instanceof FurnitureBlock))) {
            ejectPassengers();
            discard();
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        BlockPos front = blockPosition().relative(facing);
        if (level().getBlockState(front).getCollisionShape(level(), front).isEmpty()) return Vec3.atBottomCenterOf(front);
        return new Vec3(getX(), blockPosition().getY() + 1.0D, getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
