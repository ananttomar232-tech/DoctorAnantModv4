package com.dranant.city;

import com.dranant.clinic.Blueprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Reusable decoration pieces for the city: trees, lamps, benches, flower beds, hedges, doors, signs, holograms... */
public final class CityDecor {
    public enum TreeType { OAK, BIRCH, CHERRY, SPRUCE, JUNGLE_PALM, AZALEA }

    public static final Block[] FLOWERS = {Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.ALLIUM, Blocks.AZURE_BLUET,
            Blocks.OXEYE_DAISY, Blocks.PINK_TULIP, Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP, Blocks.LILY_OF_THE_VALLEY, Blocks.BLUE_ORCHID};

    public static BlockState leaves(Block block) {
        BlockState s = block.defaultBlockState();
        return s.hasProperty(LeavesBlock.PERSISTENT) ? s.setValue(LeavesBlock.PERSISTENT, true) : s;
    }

    public static BlockState stair(Block block, Direction facing) {
        return block.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    public static BlockState upsideStair(Block block, Direction facing) {
        return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, Half.TOP);
    }

    public static BlockState slab(Block block, boolean top) {
        return block.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
    }

    // ------------------------------------------------------------------ trees
    public static void tree(Blueprint bp, int x, int y, int z, TreeType type, RandomSource r) {
        switch (type) {
            case SPRUCE -> {
                int h = 6 + r.nextInt(3);
                for (int i = 0; i < h; i++) bp.set(x, y + i, z, Blocks.SPRUCE_LOG.defaultBlockState());
                BlockState lv = leaves(Blocks.SPRUCE_LEAVES);
                for (int layer = 0; layer < h - 1; layer++) {
                    int ly = y + 2 + layer;
                    int rad = (h - layer) % 2 == 0 ? 2 : 1;
                    if (layer > h - 4) rad = 1;
                    for (int dx = -rad; dx <= rad; dx++)
                        for (int dz = -rad; dz <= rad; dz++)
                            if ((dx != 0 || dz != 0) && Math.abs(dx) + Math.abs(dz) <= rad + 1) bp.set(x + dx, ly, z + dz, lv);
                }
                bp.set(x, y + h, z, lv);
                bp.set(x, y + h + 1, z, lv);
            }
            case JUNGLE_PALM -> {
                int h = 6 + r.nextInt(2);
                for (int i = 0; i < h; i++) bp.set(x, y + i, z, Blocks.JUNGLE_LOG.defaultBlockState());
                BlockState lv = leaves(Blocks.JUNGLE_LEAVES);
                int top = y + h;
                bp.set(x, top, z, lv);
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    for (int i = 1; i <= 3; i++) bp.set(x + d.getStepX() * i, top - (i == 3 ? 1 : 0), z + d.getStepZ() * i, lv);
                }
                bp.set(x + 1, top, z + 1, lv);
                bp.set(x - 1, top, z - 1, lv);
                bp.set(x + 1, top, z - 1, lv);
                bp.set(x - 1, top, z + 1, lv);
            }
            case AZALEA -> {
                bp.set(x, y, z, Blocks.OAK_LOG.defaultBlockState());
                bp.set(x, y + 1, z, Blocks.OAK_LOG.defaultBlockState());
                for (int dx = -2; dx <= 2; dx++)
                    for (int dz = -2; dz <= 2; dz++)
                        for (int dy = 2; dy <= 3; dy++)
                            if (Math.abs(dx) + Math.abs(dz) + (dy - 2) <= 3)
                                bp.set(x + dx, y + dy, z + dz, leaves(r.nextBoolean() ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.AZALEA_LEAVES));
            }
            default -> {
                Block log = type == TreeType.BIRCH ? Blocks.BIRCH_LOG : type == TreeType.CHERRY ? Blocks.CHERRY_LOG : Blocks.OAK_LOG;
                Block leaf = type == TreeType.BIRCH ? Blocks.BIRCH_LEAVES : type == TreeType.CHERRY ? Blocks.CHERRY_LEAVES : Blocks.OAK_LEAVES;
                int h = 4 + r.nextInt(2);
                BlockState lv = leaves(leaf);
                for (int dy = h - 2; dy <= h + 1; dy++) {
                    int rad = dy >= h + 1 ? 1 : 2;
                    for (int dx = -rad; dx <= rad; dx++)
                        for (int dz = -rad; dz <= rad; dz++) {
                            if (rad == 2 && Math.abs(dx) == 2 && Math.abs(dz) == 2 && r.nextInt(2) == 0) continue;
                            bp.set(x + dx, y + dy, z + dz, lv);
                        }
                }
                for (int i = 0; i < h; i++) bp.set(x, y + i, z, log.defaultBlockState());
                if (type == TreeType.CHERRY) {
                    for (int i = 0; i < 4; i++) bp.set(x + r.nextInt(5) - 2, y, z + r.nextInt(5) - 2, Blocks.PINK_PETALS.defaultBlockState());
                }
            }
        }
    }

    public static TreeType randomTree(RandomSource r) {
        TreeType[] pool = {TreeType.OAK, TreeType.BIRCH, TreeType.CHERRY, TreeType.CHERRY, TreeType.AZALEA, TreeType.SPRUCE};
        return pool[r.nextInt(pool.length)];
    }

    // ------------------------------------------------------------------ street furniture
    public static void lamp(Blueprint bp, int x, int y, int z) {
        bp.set(x, y, z, Blocks.STONE_BRICK_WALL.defaultBlockState());
        bp.set(x, y + 1, z, Blocks.DARK_OAK_FENCE.defaultBlockState());
        bp.set(x, y + 2, z, Blocks.DARK_OAK_FENCE.defaultBlockState());
        bp.set(x, y + 3, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
    }

    public static void hangingLamp(Blueprint bp, int x, int y, int z) {
        bp.set(x, y, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    /** Bench facing {@code facing} (the direction a sitter looks). */
    public static void bench(Blueprint bp, int x, int y, int z, Direction facing, int length) {
        Direction along = facing.getClockWise();
        for (int i = 0; i < length; i++) {
            bp.set(x + along.getStepX() * i, y, z + along.getStepZ() * i, stair(Blocks.SPRUCE_STAIRS, facing.getOpposite()));
        }
    }

    public static void flowerBed(Blueprint bp, int x0, int z0, int x1, int z1, int y, RandomSource r) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                boolean edge = x == Math.min(x0, x1) || x == Math.max(x0, x1) || z == Math.min(z0, z1) || z == Math.max(z0, z1);
                if (edge) {
                    bp.set(x, y - 1, z, Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
                } else {
                    bp.set(x, y - 1, z, Blocks.GRASS_BLOCK.defaultBlockState());
                    bp.set(x, y, z, FLOWERS[r.nextInt(FLOWERS.length)].defaultBlockState());
                }
            }
    }

    public static void hedge(Blueprint bp, int x0, int z0, int x1, int z1, int y, int height) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
                for (int h = 0; h < height; h++) bp.set(x, y + h, z, leaves(Blocks.AZALEA_LEAVES));
    }

    public static void path(Blueprint bp, int x0, int z0, int x1, int z1, BlockState state) {
        bp.fill(x0, 0, z0, x1, 0, z1, state);
    }

    /** Two-block door; {@code facing} = direction someone walking IN faces. */
    public static void door(Blueprint bp, int x, int y, int z, Block door, Direction facing, boolean rightHinge) {
        BlockState s = door.defaultBlockState().setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HINGE, rightHinge ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
        bp.set(x, y, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        bp.set(x, y + 1, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** Bed whose head points {@code headDirection}; (x,z) is the foot. */
    public static void bed(Blueprint bp, int x, int y, int z, Block bed, Direction headDirection) {
        BlockState s = bed.defaultBlockState().setValue(BedBlock.FACING, headDirection);
        bp.set(x, y, z, s.setValue(BedBlock.PART, BedPart.FOOT));
        bp.set(x + headDirection.getStepX(), y, z + headDirection.getStepZ(), s.setValue(BedBlock.PART, BedPart.HEAD));
    }

    public static void campfire(Blueprint bp, int x, int y, int z) {
        bp.set(x, y, z, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true).setValue(CampfireBlock.SIGNAL_FIRE, false));
    }

    // ------------------------------------------------------------------ signs + holograms
    /** Glowing wall sign; {@code facing} = the direction its text faces. */
    public static void sign(Blueprint bp, int x, int y, int z, Direction facing, DyeColor color, String... lines) {
        bp.set(x, y, z, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing));
        final BlockPos world = bp.frame().at(x, y, z);
        bp.after(level -> writeSign(level, world, color, lines));
    }

    public static void writeSign(ServerLevel level, BlockPos pos, DyeColor color, String... lines) {
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            SignText text = sign.getFrontText().setColor(color).setHasGlowingText(true);
            for (int i = 0; i < Math.min(4, lines.length); i++) text = text.setMessage(i, Component.literal(lines[i]));
            sign.setText(text, true);
            sign.setChanged();
            BlockState s = level.getBlockState(pos);
            level.sendBlockUpdated(pos, s, s, Block.UPDATE_ALL);
        }
    }

    /** Floating text-display hologram (spawned after the piece is built). {@code json} is a text component JSON string. */
    public static void hologram(Blueprint bp, int x, int y, int z, String json, float scale, String tag) {
        final BlockPos world = bp.frame().at(x, y, z);
        bp.after(level -> spawnHologram(level, Vec3.atBottomCenterOf(world), json, scale, tag));
    }

    public static void spawnHologram(ServerLevel level, Vec3 pos, String json, float scale, String tag) {
        CompoundTag t = new CompoundTag();
        t.putString("id", "minecraft:text_display");
        ListTag p = new ListTag();
        p.add(DoubleTag.valueOf(pos.x));
        p.add(DoubleTag.valueOf(pos.y));
        p.add(DoubleTag.valueOf(pos.z));
        t.put("Pos", p);
        t.putString("text", json);
        t.putString("billboard", "center");
        t.putInt("background", 0x90000000);
        t.putBoolean("shadow", true);
        t.putInt("line_width", 400);
        CompoundTag tr = new CompoundTag();
        tr.put("left_rotation", floats(0, 0, 0, 1));
        tr.put("right_rotation", floats(0, 0, 0, 1));
        tr.put("translation", floats(0, 0, 0));
        tr.put("scale", floats(scale, scale, scale));
        t.put("transformation", tr);
        ListTag tags = new ListTag();
        tags.add(StringTag.valueOf("dranant_holo"));
        tags.add(StringTag.valueOf(tag));
        t.put("Tags", tags);
        EntityType.create(t, level).ifPresent(e -> {
            e.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
            level.addFreshEntity(e);
        });
    }

    public static void removeHolograms(ServerLevel level, Vec3 around, double radius, String tag) {
        for (Entity e : level.getEntitiesOfClass(Display.TextDisplay.class, new AABB(around, around).inflate(radius), d -> d.getTags().contains(tag))) {
            e.discard();
        }
    }

    private static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float f : v) l.add(FloatTag.valueOf(f));
        return l;
    }

    public static String json(String text, String color, boolean bold) {
        return "{\"text\":\"" + text.replace("\"", "'") + "\",\"color\":\"" + color + "\",\"bold\":" + bold + "}";
    }

    private CityDecor() {}
}
