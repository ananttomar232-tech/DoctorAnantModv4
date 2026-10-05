package com.dranant.city;

import com.dranant.block.FurnitureBlock;
import com.dranant.block.FurnitureType;
import com.dranant.block.TallFurnitureBlock;
import com.dranant.clinic.Blueprint;
import com.dranant.clinic.ConstructionManager;
import com.dranant.clinic.Frame;
import com.dranant.entity.SupercarEntity;
import com.dranant.entity.SupercarVariant;
import com.dranant.registry.ModBlocks;
import com.dranant.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.dranant.city.CityDecor.*;

/**
 * "Dr. Anant's Villa" - a two-storey modern smart home on the city's south band (street segment -55..-5), fully furnished
 * with the HD furniture set: open-plan kitchen with island, 8-seat dining, living room with smart TV and sofas, staircase,
 * master bedroom with TV, home office, bathroom (vanity, toilet, rain shower, tub), roof terrace lounge, infinity pool and a
 * supercar in the driveway. Lot-local: x -24..24, z -12 (back) .. 11 (front, facing the ring road), y 0 = street level.
 */
public final class CityHouse {
    public static final int CENTER_ALONG = -30;
    private static final String TAG = "dranant_villa";

    public static Frame frame(Frame city) {
        return city.child(CENTER_ALONG, 0, 127, Rotation.CLOCKWISE_180);
    }

    private static BlockState st(Block b) {
        return b.defaultBlockState();
    }

    private static BlockState furn(FurnitureType t, Direction facing) {
        return ModBlocks.FURNITURE.get(t).get().defaultBlockState().setValue(FurnitureBlock.FACING, facing);
    }

    private static void put(Blueprint bp, int x, int y, int z, FurnitureType t, Direction facing) {
        if (t.tall()) {
            BlockState s = furn(t, facing);
            bp.set(x, y, z, s.setValue(TallFurnitureBlock.HALF, DoubleBlockHalf.LOWER));
            bp.set(x, y + 1, z, s.setValue(TallFurnitureBlock.HALF, DoubleBlockHalf.UPPER));
        } else {
            bp.set(x, y, z, furn(t, facing));
        }
    }

    public static void design(Blueprint bp, RandomSource r, int number) {
        bp.clearLocal(-24, 1, -12, 24, 16, 13);
        BlockState white = st(Blocks.WHITE_CONCRETE), black = st(Blocks.BLACK_CONCRETE), glass = st(Blocks.GLASS),
                quartz = st(Blocks.SMOOTH_QUARTZ), wood = st(Blocks.STRIPPED_DARK_OAK_WOOD), floor = st(Blocks.POLISHED_DIORITE),
                oakFloor = st(Blocks.STRIPPED_OAK_WOOD), lantern = st(Blocks.SEA_LANTERN), grass = st(Blocks.GRASS_BLOCK), air = st(Blocks.AIR);
        final Direction N = Direction.NORTH, S = Direction.SOUTH, E = Direction.EAST, W = Direction.WEST;

        // ================= garden ground
        bp.fill(-24, 0, -12, 24, 0, 11, grass);
        for (int x = -24; x <= 24; x++) {
            bp.set(x, 0, 12, st(Blocks.SMOOTH_STONE));
            bp.set(x, 0, 13, st(Blocks.SMOOTH_STONE));
        }

        // ================= ground floor shell  x -18..18, z -10..2, rooms y1..4, slab y5
        bp.fill(-18, 0, -10, 18, 0, 2, floor);
        bp.fill(0, 0, -9, 17, 0, 1, oakFloor);                      // warm wood floor in the living room
        for (int y = 1; y <= 4; y++) {
            for (int x = -18; x <= 18; x++) {
                bp.set(x, y, -10, (x % 6 == 0) ? white : (y == 2 || y == 3) ? glass : white);          // back wall with windows
                boolean mull = Math.floorMod(x, 5) == 0;
                bp.set(x, y, 2, x <= -13 ? wood : mull ? black : glass);                             // front: wood cladding + glass
            }
            for (int z = -10; z <= 2; z++) {
                bp.set(-18, y, z, (z % 4 == 0 || y == 1 || y == 4) ? white : glass);
                bp.set(18, y, z, (z % 4 == 0 || y == 1 || y == 4) ? white : glass);
            }
        }
        // ground-floor ceiling / first-floor slab with recessed lights
        bp.fill(-18, 5, -10, 18, 5, 2, white);
        bp.fill(-12, 5, 3, 18, 5, 4, white);                       // cantilevered upper floor over the porch
        for (int x : new int[]{-12, -6, 6, 12, 18}) for (int y = 1; y <= 4; y++) bp.set(x, y, 4, black);   // porch columns
        // front door (double, dark oak) at x -1..0
        door(bp, -1, 1, 2, Blocks.DARK_OAK_DOOR, N, false);
        door(bp, 0, 1, 2, Blocks.DARK_OAK_DOOR, N, true);
        bp.set(-1, 3, 2, black); bp.set(0, 3, 2, black);

        // ================= kitchen (x -17..-9)
        FurnitureType[] backRow = {FurnitureType.FRIDGE, FurnitureType.KITCHEN_COUNTER, FurnitureType.KITCHEN_SINK, FurnitureType.KITCHEN_COUNTER,
                FurnitureType.KITCHEN_STOVE, FurnitureType.KITCHEN_COUNTER, FurnitureType.KITCHEN_COUNTER, FurnitureType.KITCHEN_COUNTER};
        for (int i = 0; i < backRow.length; i++) put(bp, -17 + i, 1, -9, backRow[i], S);
        for (int x = -16; x <= -11; x++) bp.set(x, 4, -9, st(Blocks.DARK_OAK_PLANKS));   // upper cabinets
        bp.set(-13, 3, -9, st(Blocks.END_ROD));                                           // range hood light
        for (int x = -16; x <= -12; x++) put(bp, x, 1, -5, FurnitureType.KITCHEN_COUNTER, N);   // island
        for (int x = -15; x <= -13; x++) put(bp, x, 1, -4, FurnitureType.DINING_CHAIR, N);      // bar stools
        bp.set(-17, 1, 1, st(Blocks.POTTED_BAMBOO));

        // ================= dining (x -8..-2): 8-seat table
        for (int x = -7; x <= -3; x++) {
            put(bp, x, 1, -5, FurnitureType.DINING_TABLE, N);
            if (x != -5) {
                put(bp, x, 1, -6, FurnitureType.DINING_CHAIR, S);
                put(bp, x, 1, -4, FurnitureType.DINING_CHAIR, N);
            }
        }
        put(bp, -8, 1, -5, FurnitureType.DINING_CHAIR, E);
        put(bp, -2, 1, -5, FurnitureType.DINING_CHAIR, W);
        bp.set(-5, 4, -5, st(Blocks.LANTERN).setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));

        // ================= living room (x 4..17)
        put(bp, 10, 1, -9, FurnitureType.TV_STAND, S);
        put(bp, 8, 1, -9, FurnitureType.FLOOR_LAMP, S);
        put(bp, 12, 1, -9, FurnitureType.FLOOR_LAMP, S);
        for (int x = 8; x <= 12; x++) put(bp, x, 1, -3, FurnitureType.MODERN_SOFA, N);
        put(bp, 6, 1, -6, FurnitureType.MODERN_SOFA, E);
        put(bp, 6, 1, -5, FurnitureType.MODERN_SOFA, E);
        put(bp, 14, 1, -6, FurnitureType.MODERN_SOFA, W);
        put(bp, 14, 1, -5, FurnitureType.MODERN_SOFA, W);
        put(bp, 9, 1, -6, FurnitureType.COFFEE_TABLE, N);
        put(bp, 10, 1, -6, FurnitureType.COFFEE_TABLE, N);
        put(bp, 11, 1, -6, FurnitureType.COFFEE_TABLE, N);
        for (int z = -9; z <= -6; z++) {
            bp.set(17, 1, z, st(Blocks.CHISELED_BOOKSHELF).setValue(net.minecraft.world.level.block.ChiseledBookShelfBlock.FACING, W));
            bp.set(17, 2, z, st(Blocks.BOOKSHELF));
        }
        bp.set(17, 1, 1, st(Blocks.POTTED_FLOWERING_AZALEA_BUSH));
        bp.set(5, 1, 1, st(Blocks.POTTED_BAMBOO));

        // ================= staircase (x 2..3) climbing north to the first floor
        for (int i = 0; i < 4; i++) {
            int z = 1 - i;
            for (int x = 2; x <= 3; x++) {
                bp.set(x, 1 + i, z, st(Blocks.QUARTZ_STAIRS).setValue(StairBlock.FACING, N));
                for (int y = 1; y < 1 + i; y++) bp.set(x, y, z, quartz);
            }
        }
        for (int x = 2; x <= 3; x++) for (int z = -2; z <= 1; z++) bp.set(x, 5, z, air);   // stairwell opening
        for (int x = 2; x <= 3; x++) bp.set(x, 5, -3, quartz);                                // landing

        // ================= first floor  x -12..18, z -10..4, rooms y6..9, roof y10
        bp.fill(-12, 5, -10, 18, 5, 4, oakFloor);
        for (int x = -8; x <= 16; x += 4) for (int z = -8; z <= 0; z += 4) if (x < 1 || x > 4) bp.set(x, 5, z, lantern);   // recessed ceiling lights
        for (int x = 2; x <= 3; x++) for (int z = -2; z <= 1; z++) bp.set(x, 5, z, air);
        for (int y = 6; y <= 9; y++) {
            for (int x = -12; x <= 18; x++) {
                bp.set(x, y, -10, (y == 7 || y == 8) && x % 4 != 0 ? glass : white);
                bp.set(x, y, 4, Math.floorMod(x, 6) == 0 || y == 9 ? black : glass);
            }
            for (int z = -10; z <= 4; z++) {
                bp.set(-12, y, z, white);
                bp.set(18, y, z, (y == 7 || y == 8) && z % 3 != 0 ? glass : white);
            }
        }
        // stairwell glass railing
        for (int z = -2; z <= 1; z++) { bp.set(1, 6, z, st(Blocks.GLASS_PANE)); bp.set(4, 6, z, st(Blocks.GLASS_PANE)); }
        bp.set(2, 6, 2, st(Blocks.GLASS_PANE)); bp.set(3, 6, 2, st(Blocks.GLASS_PANE));
        // roof with solar panels and skylights
        bp.fill(-12, 10, -10, 18, 10, 4, white);
        for (int x = -12; x <= 18; x++) { bp.set(x, 11, -10, black); bp.set(x, 11, 4, black); }
        for (int z = -10; z <= 4; z++) { bp.set(-12, 11, z, black); bp.set(18, 11, z, black); }
        for (int x = -9; x <= 15; x += 2) for (int z = -8; z <= -5; z++) bp.set(x, 11, z, st(Blocks.DAYLIGHT_DETECTOR));
        for (int x = -8; x <= 16; x += 6) bp.set(x, 10, 0, lantern);

        // ---- master bedroom (x 6..17), partition at x 5 with a door
        for (int y = 6; y <= 9; y++) for (int z = -10; z <= 4; z++) bp.set(5, y, z, white);
        door(bp, 5, 6, 2, Blocks.BIRCH_DOOR, E, false);
        bed(bp, 11, 6, -8, Blocks.WHITE_BED, N);
        bed(bp, 12, 6, -8, Blocks.WHITE_BED, N);
        put(bp, 10, 6, -9, FurnitureType.FLOOR_LAMP, S);
        put(bp, 13, 6, -9, FurnitureType.FLOOR_LAMP, S);
        bp.fill(10, 6, -6, 13, 6, -5, st(Blocks.LIGHT_GRAY_CARPET));
        put(bp, 11, 6, 3, FurnitureType.TV_STAND, N);
        put(bp, 16, 6, 2, FurnitureType.MODERN_SOFA, W);
        for (int z = -9; z <= -6; z++) for (int y = 6; y <= 8; y++) bp.set(17, y, z, st(Blocks.DARK_OAK_PLANKS));   // wardrobe wall
        bp.set(16, 6, -9, st(Blocks.POTTED_WHITE_TULIP));

        // ---- bathroom (x -5..-1), walls x -6 / x 0 and front wall z 1 with door
        for (int y = 6; y <= 9; y++) {
            for (int z = -10; z <= 1; z++) { bp.set(-6, y, z, white); bp.set(0, y, z, white); }
            for (int x = -6; x <= 0; x++) bp.set(x, y, 1, white);
        }
        door(bp, -3, 6, 1, Blocks.BIRCH_DOOR, N, false);
        bp.fill(-5, 5, -9, -1, 5, 0, st(Blocks.WHITE_GLAZED_TERRACOTTA));
        put(bp, -5, 6, -9, FurnitureType.MODERN_TOILET, S);
        put(bp, -3, 6, -9, FurnitureType.BATHROOM_VANITY, S);
        bp.set(-1, 6, -9, st(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, 3));     // tub
        bp.set(-1, 6, -8, st(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, 3));
        for (int y = 6; y <= 8; y++) { bp.set(-4, y, -5, st(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE)); bp.set(-5, y, -5, st(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE)); }
        bp.set(-5, 9, -4, st(Blocks.END_ROD));                                                     // rain shower head
        bp.set(-5, 5, -4, st(Blocks.LIGHT_BLUE_CONCRETE));

        // ---- home office (x -11..-7), open to the landing; glass door to the roof terrace
        put(bp, -9, 6, -9, FurnitureType.OFFICE_DESK, S);
        put(bp, -8, 6, -9, FurnitureType.OFFICE_DESK, S);
        put(bp, -9, 6, -8, FurnitureType.DINING_CHAIR, N);
        for (int z = -9; z <= -6; z++) for (int y = 6; y <= 8; y++) bp.set(-11, y, z, st(Blocks.BOOKSHELF));
        put(bp, -11, 6, -3, FurnitureType.FLOOR_LAMP, S);
        put(bp, -8, 6, -2, FurnitureType.MODERN_SOFA, N);
        bp.set(-12, 6, 0, air); bp.set(-12, 7, 0, air);
        door(bp, -12, 6, 0, Blocks.BIRCH_DOOR, W, false);

        // ---- roof terrace over the kitchen (x -18..-13, y5 level) with glass railing and lounge
        for (int x = -18; x <= -13; x++) { bp.set(x, 6, -10, st(Blocks.GLASS_PANE)); bp.set(x, 6, 2, st(Blocks.GLASS_PANE)); }
        for (int z = -10; z <= 2; z++) bp.set(-18, 6, z, st(Blocks.GLASS_PANE));
        bp.fill(-17, 5, -9, -13, 5, 1, st(Blocks.STRIPPED_SPRUCE_WOOD));
        put(bp, -16, 6, -4, FurnitureType.MODERN_SOFA, E);
        put(bp, -16, 6, -3, FurnitureType.MODERN_SOFA, E);
        put(bp, -14, 6, -4, FurnitureType.COFFEE_TABLE, N);
        bp.set(-17, 6, -8, st(Blocks.POTTED_FLOWERING_AZALEA_BUSH));
        bp.set(-17, 6, 1, st(Blocks.POTTED_BAMBOO));

        // ================= infinity pool (x -17..-5, z 6..10) + loungers
        for (int x = -18; x <= -4; x++)
            for (int z = 5; z <= 11; z++) {
                boolean rim = x == -18 || x == -4 || z == 5 || z == 11;
                if (rim) { bp.set(x, 0, z, quartz); continue; }
                bp.set(x, 0, z, st(Blocks.WATER));
                bp.set(x, -1, z, st(Blocks.WATER));
                bp.set(x, -2, z, (x + z) % 4 == 0 ? lantern : st(Blocks.LIGHT_BLUE_CONCRETE));
            }
        put(bp, -15, 1, 4, FurnitureType.MODERN_SOFA, S);
        put(bp, -13, 1, 4, FurnitureType.MODERN_SOFA, S);
        put(bp, -9, 1, 4, FurnitureType.FLOOR_LAMP, S);

        // ================= driveway + path + landscaping
        bp.fill(6, 0, 5, 14, 0, 11, st(Blocks.SMOOTH_STONE));
        for (int z = 5; z <= 11; z++) { bp.set(6, 0, z, white); bp.set(14, 0, z, white); }
        bp.fill(-1, 0, 3, 0, 0, 11, st(Blocks.POLISHED_ANDESITE));
        hedge(bp, -24, -12, -24, 10, 1, 2);
        hedge(bp, 24, -12, 24, 10, 1, 2);
        hedge(bp, -23, -12, 23, -12, 1, 2);
        tree(bp, -21, 1, -8, TreeType.CHERRY, r);
        tree(bp, 21, 1, -8, TreeType.AZALEA, r);
        tree(bp, 21, 1, 6, TreeType.BIRCH, r);
        flowerBed(bp, 16, 6, 22, 10, 1, r);
        flowerBed(bp, 1, 6, 4, 10, 1, r);
        lamp(bp, -2, 1, 11);
        lamp(bp, 1, 1, 11);
        lamp(bp, 15, 1, 11);
        lamp(bp, -22, 1, 11);
        bp.set(3, 1, 11, quartz);
        bp.set(3, 2, 11, quartz);
        sign(bp, 3, 2, 12, Direction.SOUTH, DyeColor.WHITE, "Building No. " + number, "DR. ANANT'S VILLA", "Modern Smart Home", "Swagat hai!");
        hologram(bp, 3, 13, 4, json("Dr. Anant's Villa", "aqua", true), 2.5F, TAG);

        // a supercar parked in the driveway, facing the street
        final BlockPos carPos = bp.frame().at(10, 1, 8);
        final float yaw = bp.frame().yaw(Direction.SOUTH);
        bp.after(level -> parkCar(level, carPos, yaw));
    }

    private static void parkCar(ServerLevel level, BlockPos pos, float yaw) {
        Vec3 at = Vec3.atBottomCenterOf(pos);
        if (!level.getEntitiesOfClass(SupercarEntity.class, new AABB(at, at).inflate(3.0D)).isEmpty()) return;
        SupercarEntity car = ModEntities.SUPERCAR.get().create(level);
        if (car == null) return;
        car.setVariant(SupercarVariant.GT);
        car.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        level.addFreshEntity(car);
    }

    /** /city house: (re)builds the villa in an already founded city. */
    public static boolean rebuild(ServerLevel level, Frame city, int number) {
        if (ConstructionManager.isBusy()) return false;
        Frame f = frame(city);
        CityDecor.removeHolograms(level, Vec3.atCenterOf(f.at(0, 8, 0)), 30, TAG);
        Blueprint bp = new Blueprint(f);
        design(bp, RandomSource.create(f.origin().asLong()), number);
        ConstructionManager.add(ConstructionManager.blueprintTask(level, bp, f.origin(), true, 4000));
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("Building Dr. Anant's modern villa...")
                .withStyle(ChatFormatting.AQUA), false);
        return true;
    }

    private CityHouse() {}
}
