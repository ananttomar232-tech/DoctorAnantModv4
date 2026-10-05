package com.dranant.city;

import com.dranant.clinic.Blueprint;
import com.dranant.clinic.BuildHelper;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.dranant.city.CityDecor.*;

/**
 * "ANANT MOTORS" supercar showroom (city south band, street segment 5..55). 49 x 23 glass-and-quartz hall with a
 * floor-to-ceiling glass front, a wide drive-out portal, three lit podiums (the centre one is a slowly turning turntable)
 * and the three supercars already parked on them. Pedestals in front of each car sell your own copy.
 * Lot-local coordinates: x -24..24, z -12 (back) .. 11 (front facing the ring road), y 0 = street level.
 */
public final class CityShowroom {
    public static final int CENTER_ALONG = 30;   // centre of the 5..55 street segment
    public static final int HALF_W = 23;
    private static final int Z_BACK = -11, Z_FRONT = 9, TOP = 9;
    private static final String TAG = "dranant_showroom";
    private static final int[][] PODIUMS = {{-14, -3}, {0, -4}, {14, -3}};
    private static final SupercarVariant[] CARS = {SupercarVariant.VELOCE, SupercarVariant.PHANTOM, SupercarVariant.GT};

    /** The showroom's frame inside a founded city. */
    public static Frame frame(Frame city) {
        return city.child(CENTER_ALONG, 0, 127, net.minecraft.world.level.block.Rotation.CLOCKWISE_180);
    }

    private static BlockState st(Block b) {
        return b.defaultBlockState();
    }

    public static void design(Blueprint bp, RandomSource r, int number) {
        bp.clearLocal(-24, 1, -12, 24, TOP + 6, 13);
        BlockState quartz = st(Blocks.SMOOTH_QUARTZ), pillar = st(Blocks.QUARTZ_PILLAR), black = st(Blocks.BLACK_CONCRETE),
                glass = st(Blocks.LIGHT_GRAY_STAINED_GLASS), front = st(Blocks.GLASS), lantern = st(Blocks.SEA_LANTERN),
                grid = st(Blocks.GRAY_CONCRETE), white = st(Blocks.WHITE_CONCRETE);

        // ---------------- polished floor: white tiles with a dark grout grid, glowing strip lights
        for (int x = -HALF_W; x <= HALF_W; x++)
            for (int z = Z_BACK; z <= Z_FRONT; z++) {
                boolean edge = Math.abs(x) == HALF_W || z == Z_BACK || z == Z_FRONT;
                boolean line = Math.floorMod(x, 6) == 0 || Math.floorMod(z, 6) == 0;
                bp.set(x, 0, z, edge ? black : line ? grid : white);
            }
        // ---------------- walls
        for (int y = 1; y < TOP; y++) {
            for (int x = -HALF_W; x <= HALF_W; x++) {
                boolean pil = Math.floorMod(x + HALF_W, 8) == 0;
                bp.set(x, y, Z_BACK, pil ? pillar : y <= 2 || y == TOP - 1 ? black : st(Blocks.BLACK_STAINED_GLASS));
                // front: floor-to-ceiling glass, drive-out portal 11 wide x 4 high
                boolean portal = Math.abs(x) <= 5 && y <= 4;
                bp.set(x, y, Z_FRONT, portal ? st(Blocks.AIR) : pil && !(Math.abs(x) <= 5) ? pillar : front);
            }
            for (int z = Z_BACK; z <= Z_FRONT; z++) {
                boolean pil = Math.floorMod(z - Z_BACK, 5) == 0;
                for (int sx : new int[]{-HALF_W, HALF_W}) bp.set(sx, y, z, pil ? pillar : y <= 2 ? black : glass);
            }
        }
        // portal frame
        for (int x = -6; x <= 6; x++) bp.set(x, 5, Z_FRONT, quartz);
        for (int y = 1; y <= 5; y++) {
            bp.set(-6, y, Z_FRONT, pillar);
            bp.set(6, y, Z_FRONT, pillar);
        }
        // ---------------- roof: black slab with a sea-lantern light grid, quartz parapet and facade band
        for (int x = -HALF_W; x <= HALF_W; x++)
            for (int z = Z_BACK; z <= Z_FRONT; z++) {
                boolean light = Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 1 && Math.abs(x) < HALF_W && z > Z_BACK && z < Z_FRONT;
                bp.set(x, TOP, z, light ? lantern : black);
                if (Math.abs(x) == HALF_W || z == Z_BACK || z == Z_FRONT) bp.set(x, TOP + 1, z, quartz);
            }
        for (int x = -HALF_W; x <= HALF_W; x++) {
            bp.set(x, TOP + 2, Z_FRONT, quartz);
            bp.set(x, TOP + 3, Z_FRONT, Math.floorMod(x, 2) == 0 ? lantern : quartz);
        }
        // ---------------- podiums: quartz slab discs ringed with light
        for (int[] p : PODIUMS) {
            for (int dx = -5; dx <= 5; dx++)
                for (int dz = -5; dz <= 5; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d <= 4.3) bp.set(p[0] + dx, 1, p[1] + dz, st(Blocks.SMOOTH_QUARTZ_SLAB).setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                    else if (d <= 5.0) bp.set(p[0] + dx, 0, p[1] + dz, lantern);
                }
        }
        // red carpet from the portal to the centre car
        for (int z = 2; z < Z_FRONT; z++) for (int x = -1; x <= 1; x++) bp.set(x, 1, z, st(Blocks.RED_CARPET));
        // ---------------- sales pedestals in front of each car (buy your own)
        for (int[] p : PODIUMS) {
            bp.set(p[0] + 4, 0, p[1] + 6, st(Blocks.GOLD_BLOCK));
            bp.set(p[0] + 4, 1, p[1] + 6, ModBlocks.ITEM_DISPLAY_PEDESTAL.get().defaultBlockState());
        }
        // ---------------- reception desk + lounge
        for (int x = 15; x <= 21; x++) bp.set(x, 1, -9, st(Blocks.POLISHED_BLACKSTONE));
        bp.set(15, 1, -10, st(Blocks.POLISHED_BLACKSTONE));
        bp.set(21, 1, -10, st(Blocks.POLISHED_BLACKSTONE));
        bp.set(18, 2, -9, ModBlocks.DIAMOND_CASH_REGISTER.get().defaultBlockState()
                .setValue(com.dranant.block.DiamondCashRegisterBlock.FACING, Direction.SOUTH));
        bp.set(16, 2, -9, st(Blocks.POTTED_WHITE_TULIP));
        bench(bp, -20, 1, -9, Direction.SOUTH, 4);
        bp.set(-21, 1, -7, st(Blocks.BLACK_CARPET));
        for (int[] c : new int[][]{{-22, -10}, {22, -10}, {-22, 8}, {22, 8}, {-7, 8}, {7, 8}}) {
            bp.set(c[0], 1, c[1], st(Blocks.POTTED_BAMBOO));
        }
        for (int[] c : new int[][]{{-20, 6}, {20, 6}}) {
            bp.set(c[0], 1, c[1], st(Blocks.FLOWERING_AZALEA));
        }
        // wall banners of the three models
        for (int i = 0; i < 3; i++) {
            int x = PODIUMS[i][0];
            DyeColor dye = i == 0 ? DyeColor.RED : i == 1 ? DyeColor.BLACK : DyeColor.BLUE;
            Block banner = switch (dye) {
                case RED -> Blocks.RED_WALL_BANNER;
                case BLACK -> Blocks.BLACK_WALL_BANNER;
                default -> Blocks.BLUE_WALL_BANNER;
            };
            for (int bx : new int[]{x - 3, x + 3}) {
                bp.set(bx, 5, Z_BACK + 1, banner.defaultBlockState().setValue(net.minecraft.world.level.block.WallBannerBlock.FACING, Direction.SOUTH));
            }
        }
        // ---------------- outside: driveway, lamps, flower beds, building number
        for (int x = -6; x <= 6; x++)
            for (int z = Z_FRONT + 1; z <= 13; z++) bp.set(x, 0, z, Math.abs(x) == 6 ? st(Blocks.WHITE_CONCRETE) : st(Blocks.SMOOTH_STONE));
        for (int x = -24; x <= 24; x++) {
            if (Math.abs(x) <= 6) continue;
            bp.set(x, 0, 12, st(Blocks.SMOOTH_STONE));
            bp.set(x, 0, 13, st(Blocks.SMOOTH_STONE));
        }
        flowerBed(bp, -22, 10, -8, 11, 1, r);
        flowerBed(bp, 8, 10, 22, 11, 1, r);
        lamp(bp, -7, 1, 12);
        lamp(bp, 7, 1, 12);
        lamp(bp, -24, 1, 12);
        lamp(bp, 24, 1, 12);
        sign(bp, 9, 2, Z_FRONT + 1, Direction.SOUTH, DyeColor.YELLOW, "Building No. " + number, "ANANT MOTORS", "Supercar Showroom", "Free test drive!");
        hologram(bp, 0, TOP + 5, Z_FRONT + 1, json("ANANT MOTORS", "gold", true), 4.0F, TAG);
        hologram(bp, 0, TOP + 4, Z_FRONT + 1, json("Supercar Showroom - Building No. " + number, "white", false), 1.6F, TAG);

        // ---------------- cars + price tags, after the hall is built
        Frame f = bp.frame();
        for (int i = 0; i < 3; i++) {
            final SupercarVariant v = CARS[i];
            final int[] p = PODIUMS[i];
            final boolean turntable = i == 1;
            hologram(bp, p[0], 5, p[1], json(v.displayName(), String.format("#%06X", v.color()), true), 1.4F, TAG);
            hologram(bp, p[0], 4, p[1], json(v.horsepower() + " HP  |  0-100 " + v.zeroToHundred() + "  |  " + v.topSpeedKmh() + " km/h", "white", false), 1.0F, TAG);
            final BlockPos pedestal = f.at(p[0] + 4, 1, p[1] + 6);
            bp.after(level -> BuildHelper.stockPedestal(level, pedestal, new ItemStack(SupercarEntity.itemFor(v)), v.price(), true));
            final BlockPos carPos = f.at(p[0], 1, p[1]);
            final float yaw = f.yaw(Direction.SOUTH) + (i == 0 ? -28.0F : i == 2 ? 28.0F : 0.0F);
            bp.after(level -> spawnCar(level, carPos, v, yaw, turntable));
        }
    }

    /** Parks a showroom car (removing any old copy standing on that podium first). */
    public static void spawnCar(ServerLevel level, BlockPos pos, SupercarVariant variant, float yaw, boolean turntable) {
        Vec3 at = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        for (SupercarEntity old : level.getEntitiesOfClass(SupercarEntity.class, new AABB(at, at).inflate(2.5D), SupercarEntity::isShowroomCar)) {
            if (old.getPassengers().isEmpty()) old.discard();
        }
        SupercarEntity car = ModEntities.SUPERCAR.get().create(level);
        if (car == null) return;
        car.setVariant(variant);
        car.setShowroom(true, turntable);
        car.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        level.addFreshEntity(car);
    }

    /** /city showroom: (re)builds the showroom in an already founded city (e.g. a v3 city). */
    public static boolean rebuild(ServerLevel level, Frame city, int number) {
        if (ConstructionManager.isBusy()) return false;
        Frame f = frame(city);
        CityDecor.removeHolograms(level, Vec3.atCenterOf(f.at(0, 5, 0)), 30, TAG);
        Blueprint bp = new Blueprint(f);
        design(bp, RandomSource.create(f.origin().asLong()), number);
        ConstructionManager.add(ConstructionManager.blueprintTask(level, bp, f.origin(), true, 4000));
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("Building ANANT MOTORS supercar showroom...")
                .withStyle(ChatFormatting.GOLD), false);
        return true;
    }

    private CityShowroom() {}
}
