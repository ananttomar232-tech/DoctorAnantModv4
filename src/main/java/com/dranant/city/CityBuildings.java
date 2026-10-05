package com.dranant.city;

import com.dranant.clinic.Blueprint;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;

import static com.dranant.city.CityDecor.*;

/**
 * City building designs. Every design is written in lot-local coordinates: x across the lot, z from back (-12) to the
 * road-facing front (+11), y 0 = street level. Each building gets a glowing "Building No. N" sign.
 */
public final class CityBuildings {
    public static final int Z_BACK = -12;
    public static final int Z_FRONT = 11;

    private static final Block[] WALLS = {Blocks.WHITE_TERRACOTTA, Blocks.BIRCH_PLANKS, Blocks.OAK_PLANKS, Blocks.BRICKS,
            Blocks.SMOOTH_SANDSTONE, Blocks.CALCITE, Blocks.MUD_BRICKS};
    private static final Block[][] ROOFS = {{Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB}, {Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB},
            {Blocks.BRICK_STAIRS, Blocks.BRICK_SLAB}, {Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB},
            {Blocks.MANGROVE_STAIRS, Blocks.MANGROVE_SLAB}, {Blocks.CHERRY_STAIRS, Blocks.CHERRY_SLAB}};
    private static final Block[] PILLARS = {Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.STRIPPED_BIRCH_LOG};
    private static final Block[] CONCRETE = {Blocks.WHITE_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE,
            Blocks.PINK_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.LIME_CONCRETE};
    private static final String[] FAMILIES = {"Sharma Niwas", "Verma Villa", "Gupta House", "Patel Bhawan", "Singh Sadan", "Jain Kutir",
            "Yadav Ghar", "Mishra Nivas", "Khan Manzil", "Reddy Home", "Iyer House", "Das Kothi"};

    private static <T> T pick(T[] arr, RandomSource r) {
        return arr[r.nextInt(arr.length)];
    }

    private static BlockState st(Block b) {
        return b.defaultBlockState();
    }

    // =================================================================== 1. cottage (w 13)
    public static void cottage(Blueprint bp, RandomSource r, int number) {
        Block wall = pick(WALLS, r);
        Block[] roof = pick(ROOFS, r);
        Block pillar = pick(PILLARS, r);
        // front yard: picket fence, path, flower beds
        for (int x = -6; x <= 6; x++) if (Math.abs(x) > 1) bp.set(x, 1, Z_FRONT, st(Blocks.OAK_FENCE));
        bp.set(-6, 1, Z_FRONT - 1, st(Blocks.OAK_FENCE));
        bp.set(6, 1, Z_FRONT - 1, st(Blocks.OAK_FENCE));
        path(bp, 0, 4, 0, Z_FRONT, st(Blocks.STONE_BRICKS));
        flowerBed(bp, -5, 6, -2, 9, 1, r);
        flowerBed(bp, 2, 6, 5, 9, 1, r);
        // shell
        for (int y = 1; y <= 8; y++)
            for (int x = -5; x <= 5; x++)
                for (int z = -6; z <= 3; z++) {
                    boolean ex = Math.abs(x) == 5, ez = z == -6 || z == 3;
                    if (!ex && !ez) continue;
                    BlockState s = ex && ez ? st(pillar) : y == 1 ? st(Blocks.STONE_BRICKS)
                            : y == 5 ? st(pillar).setValue(RotatedPillarBlock.AXIS, ez ? Direction.Axis.X : Direction.Axis.Z) : st(wall);
                    bp.set(x, y, z, s);
                }
        // windows
        for (int y : new int[]{2, 3, 6, 7}) {
            for (int x : new int[]{-3, -2, 2, 3}) {
                bp.set(x, y, 3, st(Blocks.GLASS_PANE));
                bp.set(x, y, -6, st(Blocks.GLASS_PANE));
            }
            for (int z : new int[]{-4, -3, -1, 0}) {
                bp.set(5, y, z, st(Blocks.GLASS_PANE));
                bp.set(-5, y, z, st(Blocks.GLASS_PANE));
            }
        }
        // floors + interior
        bp.fill(-4, 0, -5, 4, 0, 2, st(Blocks.OAK_PLANKS));
        bp.fill(-4, 5, -5, 4, 5, 2, st(Blocks.SPRUCE_PLANKS));
        bp.fill(-4, 1, -5, 4, 4, 2, st(Blocks.AIR));
        bp.fill(-4, 6, -5, 4, 8, 2, st(Blocks.AIR));
        for (int y = 1; y <= 5; y++) bp.set(4, y, -5, st(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
        bp.set(-4, 1, -5, st(Blocks.CRAFTING_TABLE));
        bp.set(-3, 1, -5, st(Blocks.FURNACE).setValue(AbstractFurnaceBlock.FACING, Direction.SOUTH));
        bp.set(-4, 1, 2, st(Blocks.BOOKSHELF));
        bp.set(-4, 2, 2, st(Blocks.POTTED_RED_TULIP));
        bp.set(3, 1, 2, st(Blocks.CHEST));
        bp.fill(-1, 1, -3, 1, 1, -1, st(Blocks.RED_CARPET));
        hangingLamp(bp, 0, 4, -1);
        bed(bp, -3, 6, -2, Blocks.RED_BED, Direction.NORTH);
        bed(bp, 2, 6, -2, Blocks.LIGHT_BLUE_BED, Direction.NORTH);
        bp.fill(-1, 6, -4, 1, 6, -2, st(Blocks.WHITE_CARPET));
        hangingLamp(bp, 0, 8, -1);
        // door + porch
        door(bp, 0, 1, 3, Blocks.OAK_DOOR, Direction.NORTH, false);
        bp.fill(-2, 4, 4, 2, 4, 5, slab(roof[1], false));
        bp.set(-2, 1, 5, st(Blocks.OAK_FENCE));
        bp.set(-2, 2, 5, st(Blocks.OAK_FENCE));
        bp.set(-2, 3, 5, st(Blocks.OAK_FENCE));
        bp.set(2, 1, 5, st(Blocks.OAK_FENCE));
        bp.set(2, 2, 5, st(Blocks.OAK_FENCE));
        bp.set(2, 3, 5, st(Blocks.OAK_FENCE));
        hangingLamp(bp, 0, 3, 4);
        sign(bp, -1, 2, 4, Direction.SOUTH, DyeColor.WHITE, "Building No. " + number, pick(FAMILIES, r), "Cottage", "Swagat hai!");
        // gable roof (ridge along x)
        for (int k = 0; k <= 5; k++) {
            int y = 9 + k;
            for (int x = -6; x <= 6; x++) {
                bp.set(x, y, 4 - k, stair(roof[0], Direction.NORTH));
                bp.set(x, y, -7 + k, stair(roof[0], Direction.SOUTH));
            }
            for (int z = -7 + k + 1; z <= 4 - k - 1; z++) {
                bp.set(-5, y, z, st(wall));
                bp.set(5, y, z, st(wall));
            }
        }
        bp.set(-5, 10, -2, st(Blocks.GLASS_PANE));
        bp.set(5, 10, -2, st(Blocks.GLASS_PANE));
        // chimney with smoke
        for (int y = 1; y <= 14; y++) bp.set(3, y, -7, st(Blocks.BRICKS));
        campfire(bp, 3, 15, -7);
        // backyard
        bp.set(-4, 1, -10, st(Blocks.HAY_BLOCK));
        bp.set(-3, 1, -10, st(Blocks.COMPOSTER));
        bp.set(-4, 1, -9, st(Blocks.BARREL));
        tree(bp, 3, 1, -10, r.nextBoolean() ? TreeType.CHERRY : TreeType.OAK, r);
    }

    // =================================================================== 2. apartment (w 15)
    public static void apartment(Blueprint bp, RandomSource r, int number) {
        Block conc = pick(CONCRETE, r);
        int floors = 4 + r.nextInt(3);
        int fh = 4;
        int top = floors * fh;
        for (int y = 1; y <= top; y++)
            for (int x = -6; x <= 6; x++)
                for (int z = -8; z <= 4; z++) {
                    boolean ex = Math.abs(x) == 6, ez = z == -8 || z == 4;
                    if (!ex && !ez) continue;
                    int ly = y % fh;
                    BlockState s;
                    if (ex && ez) s = st(Blocks.QUARTZ_PILLAR);
                    else if (ly == 0) s = st(Blocks.SMOOTH_QUARTZ);
                    else if ((ly == 2 || ly == 3) && (ez ? Math.abs(x) <= 4 : (z % 2 == 0 && z > -8 && z < 4))) s = st(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE);
                    else s = st(conc);
                    bp.set(x, y, z, s);
                }
        bp.fill(-5, 0, -7, 5, 0, 3, st(Blocks.POLISHED_ANDESITE));
        for (int k = 1; k < floors; k++) {
            int y = k * fh;
            bp.fill(-5, y, -7, 5, y, 3, st(Blocks.SMOOTH_STONE));
            bp.set(0, y, -2, st(Blocks.SEA_LANTERN));
            // balcony
            bp.fill(-4, y, 5, 4, y, 5, slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
            for (int x = -4; x <= 4; x++) bp.set(x, y + 1, 6, st(Blocks.IRON_BARS));
            bp.set(-5, y + 1, 5, st(Blocks.IRON_BARS));
            bp.set(5, y + 1, 5, st(Blocks.IRON_BARS));
            if (r.nextBoolean()) bp.set(-3, y + 1, 5, st(Blocks.POTTED_FERN));
            if (r.nextBoolean()) bp.set(3, y + 1, 5, st(Blocks.POTTED_AZALEA));
            bp.fill(-5, y + 1, -7, 5, y + fh - 1, 3, st(Blocks.AIR));
        }
        bp.fill(-5, 1, -7, 5, fh - 1, 3, st(Blocks.AIR));
        // roof + rooftop garden
        bp.fill(-5, top, -7, 5, top, 3, st(Blocks.SMOOTH_STONE));
        bp.fill(-5, top + 1, -7, 5, top + 1, 3, st(Blocks.GRASS_BLOCK));
        for (int x = -6; x <= 6; x++) {
            bp.set(x, top + 1, -8, st(Blocks.SMOOTH_QUARTZ));
            bp.set(x, top + 1, 4, st(Blocks.SMOOTH_QUARTZ));
            bp.set(x, top + 2, -8, st(Blocks.OAK_FENCE));
            bp.set(x, top + 2, 4, st(Blocks.OAK_FENCE));
        }
        for (int z = -8; z <= 4; z++) {
            bp.set(-6, top + 1, z, st(Blocks.SMOOTH_QUARTZ));
            bp.set(6, top + 1, z, st(Blocks.SMOOTH_QUARTZ));
            bp.set(-6, top + 2, z, st(Blocks.OAK_FENCE));
            bp.set(6, top + 2, z, st(Blocks.OAK_FENCE));
        }
        for (int i = 0; i < 14; i++) bp.set(-5 + r.nextInt(11), top + 2, -7 + r.nextInt(11), FLOWERS[r.nextInt(FLOWERS.length)].defaultBlockState());
        bp.set(-4, top + 2, -6, st(Blocks.FLOWERING_AZALEA));
        bp.set(4, top + 2, 2, st(Blocks.FLOWERING_AZALEA));
        tree(bp, 0, top + 2, -3, TreeType.AZALEA, r);
        lamp(bp, -5, top + 2, 3);
        lamp(bp, 5, top + 2, -7);
        // lobby entrance
        door(bp, -1, 1, 4, Blocks.BIRCH_DOOR, Direction.NORTH, false);
        door(bp, 0, 1, 4, Blocks.BIRCH_DOOR, Direction.NORTH, true);
        bp.fill(-3, fh, 5, 2, fh, 7, slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
        hangingLamp(bp, -1, fh - 1, 6);
        hangingLamp(bp, 0, fh - 1, 6);
        sign(bp, 2, 2, 5, Direction.SOUTH, DyeColor.WHITE, "Building No. " + number, "Anant Apartments", floors + " floors", "Flats available");
        hologram(bp, 0, top + 5, -2, json("ANANT APARTMENTS  #" + number, "aqua", true), 1.6F, "dranant_bld_" + number);
        // yard
        path(bp, -1, 5, 0, Z_FRONT, st(Blocks.SMOOTH_STONE));
        tree(bp, -5, 1, 9, randomTree(r), r);
        tree(bp, 5, 1, 9, randomTree(r), r);
        bench(bp, -4, 1, Z_FRONT - 4, Direction.EAST, 1);
        lamp(bp, 2, 1, Z_FRONT);
        flowerBed(bp, -6, -12, 6, -10, 1, r);
    }

    // =================================================================== 3. villa with pool (w 19)
    public static void villa(Blueprint bp, RandomSource r, int number) {
        // house
        for (int y = 1; y <= 8; y++)
            for (int x = -2; x <= 8; x++)
                for (int z = -10; z <= -1; z++) {
                    boolean ex = x == -2 || x == 8, ez = z == -10 || z == -1;
                    if (!ex && !ez) continue;
                    BlockState s = (ex && ez) ? st(Blocks.QUARTZ_PILLAR) : (y == 4 || y == 8) ? st(Blocks.SMOOTH_QUARTZ)
                            : ((y == 2 || y == 3 || y == 6 || y == 7) && !(ex && ez) && ((ez && x % 3 != 0) || (ex && z % 3 != 0)))
                            ? st(Blocks.GLASS_PANE) : st(Blocks.WHITE_CONCRETE);
                    bp.set(x, y, z, s);
                }
        bp.fill(-1, 0, -9, 7, 0, -2, st(Blocks.POLISHED_DIORITE));
        bp.fill(-1, 4, -9, 7, 4, -2, st(Blocks.SMOOTH_QUARTZ));
        bp.fill(-1, 1, -9, 7, 3, -2, st(Blocks.AIR));
        bp.fill(-1, 5, -9, 7, 7, -2, st(Blocks.AIR));
        for (int y = 1; y <= 4; y++) bp.set(7, y, -9, st(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
        bp.fill(-2, 9, -10, 8, 9, -1, st(Blocks.SMOOTH_QUARTZ));
        for (int x = -2; x <= 8; x++) {
            bp.set(x, 10, -10, st(Blocks.WHITE_STAINED_GLASS_PANE));
            bp.set(x, 10, -1, st(Blocks.WHITE_STAINED_GLASS_PANE));
        }
        for (int z = -10; z <= -1; z++) {
            bp.set(-2, 10, z, st(Blocks.WHITE_STAINED_GLASS_PANE));
            bp.set(8, 10, z, st(Blocks.WHITE_STAINED_GLASS_PANE));
        }
        bp.set(0, 10, -8, st(Blocks.POTTED_BAMBOO));
        bp.set(6, 10, -3, st(Blocks.POTTED_CACTUS));
        lamp(bp, 6, 10, -8);
        bed(bp, 0, 5, -5, Blocks.WHITE_BED, Direction.NORTH);
        bp.set(5, 1, -8, st(Blocks.BOOKSHELF));
        bp.set(4, 1, -8, st(Blocks.ENCHANTING_TABLE));
        bp.fill(1, 1, -6, 5, 1, -4, st(Blocks.LIGHT_BLUE_CARPET));
        hangingLamp(bp, 3, 3, -5);
        door(bp, 3, 1, -1, Blocks.DARK_OAK_DOOR, Direction.NORTH, false);
        sign(bp, 5, 2, 0, Direction.SOUTH, DyeColor.WHITE, "Building No. " + number, pick(FAMILIES, r), "Luxury Villa", "");
        // pool
        for (int x = -8; x <= -3; x++)
            for (int z = -9; z <= -2; z++) {
                boolean rim = x == -8 || x == -3 || z == -9 || z == -2;
                bp.set(x, 0, z, rim ? st(Blocks.SMOOTH_QUARTZ) : st(Blocks.WATER));
                if (!rim) bp.set(x, -1, z, st(Blocks.PRISMARINE_BRICKS));
            }
        bp.set(-5, -1, -5, st(Blocks.SEA_LANTERN));
        for (int z = 0; z <= 2; z += 2) {
            bp.set(-7, 1, z, slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
            bp.set(-5, 1, z, slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
        }
        tree(bp, -8, 1, 4, TreeType.JUNGLE_PALM, r);
        tree(bp, -2, 1, -11, TreeType.JUNGLE_PALM, r);
        // garden, hedges, driveway
        hedge(bp, -9, Z_BACK, -9, Z_FRONT - 1, 1, 2);
        hedge(bp, 9, Z_BACK, 9, Z_FRONT - 1, 1, 2);
        for (int x = -9; x <= 9; x++) if (Math.abs(x - 3) > 1) hedge(bp, x, Z_FRONT, x, Z_FRONT, 1, 2);
        path(bp, 2, 0, 4, Z_FRONT, st(Blocks.SMOOTH_STONE));
        lamp(bp, 1, 1, Z_FRONT - 1);
        lamp(bp, 5, 1, Z_FRONT - 1);
        flowerBed(bp, 6, 2, 8, 9, 1, r);
        tree(bp, -5, 1, 8, TreeType.CHERRY, r);
    }

    // =================================================================== 4. market row (w 17)
    public static void market(Blueprint bp, RandomSource r, int number) {
        // back shophouse
        for (int y = 1; y <= 8; y++)
            for (int x = -8; x <= 8; x++)
                for (int z = -12; z <= -6; z++) {
                    boolean ex = Math.abs(x) == 8, ez = z == -12 || z == -6;
                    if (!ex && !ez) continue;
                    BlockState s = (ex && ez) ? st(Blocks.STONE_BRICKS) : y == 4 ? st(Blocks.STONE_BRICKS)
                            : (ez && (y == 6 || y == 7) && x % 4 != 0) ? st(Blocks.GLASS_PANE) : st(Blocks.BRICKS);
                    bp.set(x, y, z, s);
                }
        bp.fill(-7, 0, -11, 7, 0, -7, st(Blocks.SPRUCE_PLANKS));
        bp.fill(-7, 4, -11, 7, 4, -7, st(Blocks.SPRUCE_PLANKS));
        bp.fill(-7, 1, -11, 7, 3, -7, st(Blocks.AIR));
        bp.fill(-7, 5, -11, 7, 7, -7, st(Blocks.AIR));
        bp.fill(-8, 9, -12, 8, 9, -6, st(Blocks.STONE_BRICK_SLAB));
        for (int x = -8; x <= 8; x += 2) bp.set(x, 9, -6, st(Blocks.STONE_BRICK_WALL));
        // plaza
        for (int x = -8; x <= 8; x++)
            for (int z = -5; z <= Z_FRONT; z++) bp.set(x, 0, z, ((x + z) & 1) == 0 ? st(Blocks.POLISHED_ANDESITE) : st(Blocks.STONE_BRICKS));
        String[] names = {"Sabzi Mandi", "Mithai Shop", "Kirana Store", "Chai Corner", "Kapda Bazaar", "Phool Wala"};
        Block[][] goods = {{Blocks.MELON, Blocks.PUMPKIN, Blocks.HAY_BLOCK}, {Blocks.CAKE, Blocks.HONEY_BLOCK, Blocks.CAKE},
                {Blocks.BARREL, Blocks.CHEST, Blocks.BARREL}, {Blocks.CAULDRON, Blocks.BREWING_STAND, Blocks.FLOWER_POT},
                {Blocks.WHITE_WOOL, Blocks.RED_WOOL, Blocks.BLUE_WOOL}, {Blocks.POTTED_POPPY, Blocks.POTTED_BLUE_ORCHID, Blocks.POTTED_ALLIUM}};
        Block[] awnings = {Blocks.RED_WOOL, Blocks.BLUE_WOOL, Blocks.GREEN_WOOL, Blocks.ORANGE_WOOL, Blocks.PURPLE_WOOL};
        for (int i = 0; i < 3; i++) {
            int c = -5 + i * 5;
            int type = r.nextInt(names.length);
            for (int[] post : new int[][]{{c - 2, -3}, {c + 2, -3}, {c - 2, 3}, {c + 2, 3}})
                for (int y = 1; y <= 3; y++) bp.set(post[0], y, post[1], st(Blocks.OAK_FENCE));
            Block aw = pick(awnings, r);
            for (int x = c - 2; x <= c + 2; x++)
                for (int z = -3; z <= 3; z++) bp.set(x, 4, z, ((x + z) & 1) == 0 ? st(aw) : st(Blocks.WHITE_WOOL));
            for (int x = c - 1; x <= c + 1; x++) bp.set(x, 1, 2, st(Blocks.BARREL));
            for (int k = 0; k < 3; k++) bp.set(c - 1 + k, 1, -2, st(goods[type][k]));
            sign(bp, c, 3, 4, Direction.SOUTH, DyeColor.YELLOW, "Building No. " + number, names[type], "Fresh & Sasta", "");
            if (i == 0) hologram(bp, 0, 7, 0, json("BAZAAR  #" + number, "gold", true), 1.4F, "dranant_bld_" + number);
        }
        lamp(bp, -8, 1, Z_FRONT);
        lamp(bp, 8, 1, Z_FRONT);
        bench(bp, -3, 1, 8, Direction.SOUTH, 2);
        bench(bp, 2, 1, 8, Direction.SOUTH, 2);
        door(bp, -4, 1, -6, Blocks.SPRUCE_DOOR, Direction.NORTH, false);
        door(bp, 4, 1, -6, Blocks.SPRUCE_DOOR, Direction.NORTH, false);
    }

    // =================================================================== 5. garden (w 13)
    public static void garden(Blueprint bp, RandomSource r, int number) {
        for (int x = -6; x <= 6; x++) {
            if (Math.abs(x) > 1) hedge(bp, x, Z_FRONT, x, Z_FRONT, 1, 1);
            hedge(bp, x, Z_BACK, x, Z_BACK, 1, 1);
        }
        hedge(bp, -6, Z_BACK, -6, Z_FRONT, 1, 1);
        hedge(bp, 6, Z_BACK, 6, Z_FRONT, 1, 1);
        path(bp, 0, Z_BACK + 1, 0, Z_FRONT, st(Blocks.MOSSY_STONE_BRICKS));
        path(bp, -5, 0, 5, 0, st(Blocks.MOSSY_STONE_BRICKS));
        // pond
        for (int x = 2; x <= 5; x++)
            for (int z = 2; z <= 7; z++) {
                boolean rim = x == 2 || x == 5 || z == 2 || z == 7;
                bp.set(x, 0, z, rim ? st(Blocks.MOSSY_COBBLESTONE) : st(Blocks.WATER));
                if (!rim) {
                    bp.set(x, -1, z, st(Blocks.CLAY));
                    if (r.nextInt(3) == 0) bp.set(x, 1, z, st(Blocks.LILY_PAD));
                }
            }
        tree(bp, -3, 1, -5, TreeType.CHERRY, r);
        tree(bp, 3, 1, -6, TreeType.CHERRY, r);
        flowerBed(bp, -5, 2, -2, 7, 1, r);
        flowerBed(bp, -5, -10, -2, -2, 1, r);
        bench(bp, -1, 1, 4, Direction.WEST, 1);
        bench(bp, 1, 1, -3, Direction.EAST, 1);
        lamp(bp, -1, 1, 9);
        lamp(bp, 1, 1, -10);
        sign(bp, -2, 1, Z_FRONT, Direction.SOUTH, DyeColor.LIME, "Building No. " + number, "Sakura Garden", "Phool mat todo!", "");
    }

    // =================================================================== 6. office tower (w 15)
    public static void tower(Blueprint bp, RandomSource r, int number) {
        int floors = 7 + r.nextInt(3);
        int fh = 4;
        int top = floors * fh;
        Block glass = r.nextBoolean() ? Blocks.LIGHT_BLUE_STAINED_GLASS : Blocks.CYAN_STAINED_GLASS;
        for (int y = 1; y <= top; y++)
            for (int x = -6; x <= 6; x++)
                for (int z = -8; z <= 4; z++) {
                    boolean ex = Math.abs(x) == 6, ez = z == -8 || z == 4;
                    if (!ex && !ez) continue;
                    boolean pillar = (ex && ez) || (ez && (x == 0 || Math.abs(x) == 3)) || (ex && (z == -2));
                    BlockState s = pillar ? st(Blocks.QUARTZ_PILLAR) : (y % fh == 0) ? st(Blocks.SMOOTH_QUARTZ) : st(glass);
                    bp.set(x, y, z, s);
                }
        bp.fill(-5, 0, -7, 5, 0, 3, st(Blocks.POLISHED_DIORITE));
        for (int k = 1; k < floors; k++) {
            bp.fill(-5, k * fh, -7, 5, k * fh, 3, st(Blocks.SMOOTH_STONE));
            bp.set(-3, k * fh, -2, st(Blocks.SEA_LANTERN));
            bp.set(3, k * fh, -2, st(Blocks.SEA_LANTERN));
        }
        bp.fill(-5, 1, -7, 5, fh - 1, 3, st(Blocks.AIR));
        for (int k = 1; k < floors; k++) bp.fill(-5, k * fh + 1, -7, 5, k * fh + fh - 1, 3, st(Blocks.AIR));
        bp.fill(-6, top, -8, 6, top, 4, st(Blocks.SMOOTH_QUARTZ));
        for (int x = -6; x <= 6; x++) {
            bp.set(x, top + 1, -8, st(Blocks.SEA_LANTERN));
            bp.set(x, top + 1, 4, st(Blocks.SEA_LANTERN));
        }
        bp.set(0, top + 1, -2, st(Blocks.IRON_BLOCK));
        bp.set(0, top + 2, -2, st(Blocks.IRON_BARS));
        bp.set(0, top + 3, -2, st(Blocks.IRON_BARS));
        bp.set(0, top + 4, -2, st(Blocks.LIGHTNING_ROD));
        door(bp, -1, 1, 4, Blocks.BIRCH_DOOR, Direction.NORTH, false);
        door(bp, 1, 1, 4, Blocks.BIRCH_DOOR, Direction.NORTH, true);
        bp.set(0, 1, 4, st(Blocks.GOLD_BLOCK));
        bp.set(0, 2, 4, st(Blocks.GOLD_BLOCK));
        bp.set(-4, 1, 3, st(Blocks.POTTED_BAMBOO));
        bp.set(4, 1, 3, st(Blocks.POTTED_BAMBOO));
        for (int x = -6; x <= 6; x++)
            for (int z = 5; z <= Z_FRONT; z++) bp.set(x, 0, z, ((x + z) & 1) == 0 ? st(Blocks.SMOOTH_STONE) : st(Blocks.POLISHED_ANDESITE));
        lamp(bp, -5, 1, Z_FRONT - 1);
        lamp(bp, 5, 1, Z_FRONT - 1);
        bp.set(-3, 1, 8, st(Blocks.POTTED_AZALEA));
        bp.set(3, 1, 8, st(Blocks.POTTED_FLOWERING_AZALEA));
        sign(bp, 2, 2, 5, Direction.SOUTH, DyeColor.WHITE, "Building No. " + number, "Anant Corp Tower", floors + " floors", "Offices");
        hologram(bp, 0, top + 6, -2, json("ANANT TOWER  #" + number, "light_purple", true), 1.8F, "dranant_bld_" + number);
        tree(bp, -4, 1, -11, TreeType.BIRCH, r);
        tree(bp, 4, 1, -11, TreeType.BIRCH, r);
    }

    // =================================================================== landmarks (28 x 28 corners, x/z in [-13,14])
    public static void townHall(Blueprint bp, RandomSource r, int number) {
        for (int x = -13; x <= 14; x++)
            for (int z = -13; z <= 14; z++) bp.set(x, 0, z, ((x + z) % 3 == 0) ? st(Blocks.POLISHED_ANDESITE) : st(Blocks.SMOOTH_STONE));
        for (int y = 1; y <= 9; y++)
            for (int x = -10; x <= 10; x++)
                for (int z = -11; z <= 3; z++) {
                    boolean ex = Math.abs(x) == 10, ez = z == -11 || z == 3;
                    if (!ex && !ez) continue;
                    BlockState s = (ex && ez) ? st(Blocks.CHISELED_STONE_BRICKS) : y == 9 ? st(Blocks.SMOOTH_QUARTZ)
                            : ((y >= 3 && y <= 6) && (x % 3 == 0 || z % 3 == 0) && !(ex && ez)) ? st(Blocks.GLASS_PANE) : st(Blocks.STONE_BRICKS);
                    bp.set(x, y, z, s);
                }
        bp.fill(-9, 0, -10, 9, 0, 2, st(Blocks.POLISHED_DEEPSLATE));
        bp.fill(-9, 1, -10, 9, 8, 2, st(Blocks.AIR));
        bp.fill(-10, 10, -11, 10, 10, 3, st(Blocks.SMOOTH_QUARTZ_SLAB));
        for (int z = -9; z <= 1; z++) bp.set(0, 1, z, st(Blocks.RED_CARPET));
        for (int x = -9; x <= 9; x += 3) {
            bp.set(x, 1, -10, st(Blocks.BOOKSHELF));
            bp.set(x, 2, -10, st(Blocks.BOOKSHELF));
        }
        hangingLamp(bp, -5, 8, -4);
        hangingLamp(bp, 5, 8, -4);
        hangingLamp(bp, 0, 8, -6);
        // colonnade portico
        for (int x = -9; x <= 9; x += 3)
            for (int y = 1; y <= 8; y++) bp.set(x, y, 6, st(Blocks.QUARTZ_PILLAR));
        bp.fill(-10, 9, 4, 10, 9, 7, st(Blocks.SMOOTH_QUARTZ));
        for (int x = -10; x <= 10; x++) bp.set(x, 10, 7, stair(Blocks.QUARTZ_STAIRS, Direction.NORTH));
        bp.fill(-10, 0, 4, 10, 0, 7, st(Blocks.QUARTZ_BRICKS));
        for (int x = -10; x <= 10; x++) bp.set(x, 0, 8, stair(Blocks.QUARTZ_STAIRS, Direction.NORTH));
        door(bp, -1, 1, 3, Blocks.DARK_OAK_DOOR, Direction.NORTH, false);
        door(bp, 0, 1, 3, Blocks.DARK_OAK_DOOR, Direction.NORTH, true);
        for (int x : new int[]{-6, -3, 3, 6}) bp.set(x, 7, 4, st(Blocks.RED_WALL_BANNER).setValue(WallBannerBlock.FACING, Direction.SOUTH));
        // clock tower
        for (int y = 10; y <= 22; y++)
            for (int x = -2; x <= 2; x++)
                for (int z = -7; z <= -3; z++) {
                    boolean ex = Math.abs(x) == 2, ez = z == -7 || z == -3;
                    if (ex || ez) bp.set(x, y, z, (ex && ez) ? st(Blocks.CHISELED_STONE_BRICKS) : st(Blocks.STONE_BRICKS));
                }
        for (int[] face : new int[][]{{0, -3, 1}, {0, -7, -1}}) {
            bp.set(face[0], 18, face[1], st(Blocks.GOLD_BLOCK));
            bp.set(face[0] - 1, 18, face[1], st(Blocks.WHITE_CONCRETE));
            bp.set(face[0] + 1, 18, face[1], st(Blocks.WHITE_CONCRETE));
            bp.set(face[0], 19, face[1], st(Blocks.WHITE_CONCRETE));
            bp.set(face[0], 17, face[1], st(Blocks.WHITE_CONCRETE));
        }
        bp.set(0, 21, -5, st(Blocks.BELL));
        for (int k = 0; k <= 3; k++) {
            int y = 23 + k;
            int rad = 3 - k;
            for (int x = -rad; x <= rad; x++) {
                bp.set(x, y, -5 - rad, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.SOUTH));
                bp.set(x, y, -5 + rad, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.NORTH));
            }
            for (int z = -5 - rad; z <= -5 + rad; z++) {
                bp.set(-rad, y, z, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.EAST));
                bp.set(rad, y, z, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.WEST));
            }
        }
        bp.set(0, 27, -5, st(Blocks.GOLD_BLOCK));
        bp.set(0, 28, -5, st(Blocks.LIGHTNING_ROD));
        flowerBed(bp, -12, 9, -4, 13, 1, r);
        flowerBed(bp, 4, 9, 12, 13, 1, r);
        tree(bp, -12, 1, -12, TreeType.CHERRY, r);
        tree(bp, 13, 1, -12, TreeType.CHERRY, r);
        lamp(bp, -2, 1, 13);
        lamp(bp, 2, 1, 13);
        sign(bp, 2, 2, 4, Direction.SOUTH, DyeColor.YELLOW, "Building No. " + number, "TOWN HALL", "Dr. Anant City", "Est. 2026");
        hologram(bp, 0, 31, -5, json("TOWN HALL", "gold", true), 2.5F, "dranant_bld_" + number);
    }

    public static void statuePlaza(Blueprint bp, RandomSource r, int number) {
        for (int x = -13; x <= 14; x++)
            for (int z = -13; z <= 14; z++) {
                boolean cross = Math.abs(x) <= 1 || Math.abs(z - 4) <= 1;
                bp.set(x, 0, z, cross ? st(Blocks.QUARTZ_BRICKS) : ((x * 7 + z * 3) % 5 == 0 ? st(Blocks.POLISHED_ANDESITE) : st(Blocks.SMOOTH_STONE)));
            }
        int w = StatueData.ROWS[0].length();
        int h = StatueData.ROWS.length;
        int x0 = -w / 2;
        // pedestal
        for (int y = 1; y <= 3; y++)
            for (int x = x0 - 2; x <= x0 + w + 1; x++)
                for (int z = -4; z <= 2; z++) {
                    boolean edge = x == x0 - 2 || x == x0 + w + 1 || z == -4 || z == 2;
                    bp.set(x, y, z, y == 3 && edge ? st(Blocks.GOLD_BLOCK) : st(Blocks.QUARTZ_BRICKS));
                }
        java.util.Map<Character, BlockState> palette = new java.util.HashMap<>();
        for (String[] e : StatueData.PALETTE) {
            Block b = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse(e[1]));
            palette.put(e[0].charAt(0), b.defaultBlockState());
        }
        for (int row = 0; row < h; row++) {
            String line = StatueData.ROWS[row];
            int y = 4 + (h - 1 - row);
            for (int col = 0; col < line.length(); col++) {
                BlockState s = palette.get(line.charAt(col));
                if (s == null) continue;
                bp.set(x0 + col, y, -1, s);
                bp.set(x0 + col, y, -2, s);
            }
        }
        sign(bp, 0, 2, 3, Direction.SOUTH, DyeColor.YELLOW, "Building No. " + number, "DR. ANANT", "Anant Doctor karege", "sabke dukho ka ilaaj");
        hologram(bp, 0, h + 7, -1, json("DR. ANANT - Anant Doctor karege aapke sabhi dukho ka ilaaj", "gold", true), 1.8F, "dranant_bld_" + number);
        for (int[] c : new int[][]{{-11, -11}, {12, -11}, {-11, 12}, {12, 12}}) tree(bp, c[0], 1, c[1], TreeType.CHERRY, r);
        flowerBed(bp, -12, -4, -5, 1, 1, r);
        flowerBed(bp, 6, -4, 13, 1, 1, r);
        bench(bp, -6, 1, 9, Direction.NORTH, 4);
        bench(bp, 3, 1, 9, Direction.NORTH, 4);
        lamp(bp, -3, 1, 12);
        lamp(bp, 3, 1, 12);
        lamp(bp, -12, 1, 6);
        lamp(bp, 13, 1, 6);
    }

    public static void fountainPark(Blueprint bp, RandomSource r, int number) {
        int cx = 0, cz = 0;
        for (int x = -13; x <= 14; x++)
            for (int z = -13; z <= 14; z++) {
                double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
                if (d < 6.5) {
                    bp.set(x, 0, z, st(Blocks.WATER));
                    bp.set(x, -1, z, st(Blocks.PRISMARINE_BRICKS));
                } else if (d < 7.5) {
                    bp.set(x, 0, z, st(Blocks.QUARTZ_BRICKS));
                    bp.set(x, 1, z, slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
                } else if (d < 9.5 || Math.abs(x) <= 1 || Math.abs(z) <= 1) {
                    bp.set(x, 0, z, st(Blocks.STONE_BRICKS));
                }
            }
        bp.set(0, -1, 0, st(Blocks.SEA_LANTERN));
        for (int y = 0; y <= 4; y++) bp.set(0, y, 0, st(Blocks.QUARTZ_PILLAR));
        bp.set(0, 5, 0, st(Blocks.WATER));
        for (Direction d : Direction.Plane.HORIZONTAL) bp.set(d.getStepX(), 4, d.getStepZ(), st(Blocks.WATER));
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8 + Math.PI / 8;
            tree(bp, (int) Math.round(Math.cos(a) * 11.5), 1, (int) Math.round(Math.sin(a) * 11.5), i % 2 == 0 ? TreeType.CHERRY : TreeType.AZALEA, r);
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            Direction side = d.getClockWise();
            int bx = d.getStepX() * 8 + side.getStepX() * 2, bz = d.getStepZ() * 8 + side.getStepZ() * 2;
            bench(bp, bx, 1, bz, d.getOpposite(), 1);
            lamp(bp, d.getStepX() * 10 + side.getStepX() * 2, 1, d.getStepZ() * 10 + side.getStepZ() * 2);
        }
        sign(bp, 2, 1, 13, Direction.SOUTH, DyeColor.CYAN, "Building No. " + number, "Fountain Park", "Paani bachao!", "");
        hologram(bp, 0, 9, 0, json("FOUNTAIN PARK", "aqua", true), 2.0F, "dranant_bld_" + number);
    }

    public static void bazaarPlaza(Blueprint bp, RandomSource r, int number) {
        for (int x = -13; x <= 14; x++)
            for (int z = -13; z <= 14; z++) bp.set(x, 0, z, ((x + z) & 1) == 0 ? st(Blocks.POLISHED_GRANITE) : st(Blocks.SMOOTH_SANDSTONE));
        // well
        for (int x = -2; x <= 2; x++)
            for (int z = -2; z <= 2; z++) {
                boolean rim = Math.abs(x) == 2 || Math.abs(z) == 2;
                if (rim) bp.set(x, 1, z, st(Blocks.COBBLESTONE));
                else {
                    bp.set(x, 0, z, st(Blocks.WATER));
                    bp.set(x, -1, z, st(Blocks.WATER));
                }
            }
        for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            bp.set(c[0], 2, c[1], st(Blocks.OAK_FENCE));
            bp.set(c[0], 3, c[1], st(Blocks.OAK_FENCE));
        }
        bp.fill(-2, 4, -2, 2, 4, 2, slab(Blocks.SPRUCE_SLAB, false));
        hangingLamp(bp, 0, 3, 0);
        Block[] awnings = {Blocks.RED_WOOL, Blocks.YELLOW_WOOL, Blocks.LIME_WOOL, Blocks.MAGENTA_WOOL};
        int[][] stalls = {{-9, -9}, {9, -9}, {-9, 9}, {9, 9}};
        for (int i = 0; i < 4; i++) {
            int sx = stalls[i][0], sz = stalls[i][1];
            for (int[] p : new int[][]{{sx - 2, sz - 2}, {sx + 2, sz - 2}, {sx - 2, sz + 2}, {sx + 2, sz + 2}})
                for (int y = 1; y <= 3; y++) bp.set(p[0], y, p[1], st(Blocks.SPRUCE_FENCE));
            for (int x = sx - 2; x <= sx + 2; x++)
                for (int z = sz - 2; z <= sz + 2; z++) bp.set(x, 4, z, ((x + z) & 1) == 0 ? st(awnings[i]) : st(Blocks.WHITE_WOOL));
            bp.set(sx - 1, 1, sz, st(Blocks.BARREL));
            bp.set(sx, 1, sz, st(Blocks.MELON));
            bp.set(sx + 1, 1, sz, st(Blocks.PUMPKIN));
        }
        tree(bp, -12, 1, 0, TreeType.OAK, r);
        tree(bp, 13, 1, 0, TreeType.OAK, r);
        lamp(bp, 0, 1, -12);
        lamp(bp, 0, 1, 13);
        bench(bp, -5, 1, 0, Direction.EAST, 2);
        bench(bp, 5, 1, 1, Direction.WEST, 2);
        sign(bp, -1, 1, 13, Direction.SOUTH, DyeColor.ORANGE, "Building No. " + number, "Chandni Bazaar", "Sab kuch milega", "");
        hologram(bp, 0, 7, 0, json("CHANDNI BAZAAR", "gold", true), 2.0F, "dranant_bld_" + number);
    }

    public static void bell(Blueprint bp, int x, int y, int z) {
        bp.set(x, y, z, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, Direction.NORTH));
    }

    private CityBuildings() {}
}
