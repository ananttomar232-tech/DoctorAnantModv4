package com.dranant.city;

import com.dranant.clinic.Blueprint;
import com.dranant.clinic.ConstructionManager;
import com.dranant.clinic.Frame;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.dranant.city.CityDecor.*;
import static com.dranant.city.CityLayout.*;

/**
 * Builds Dr. Anant City (299 x 297) in front of the player, gate facing them, as a streamed construction:
 * terrain levelling -> clinic district -> ring road + avenues -> ~50 numbered buildings -> 4 landmark plazas ->
 * green belt -> city wall with towers and 4 gates -> citizens.
 */
public final class CityBuilder {
    private static final int BUDGET = 5000; // blocks per tick

    private record Lot(int type, int number, Frame frame, boolean hedge, int alongSign) {}

    public static boolean found(ServerLevel level, Player player) {
        CityData data = CityData.get(level.getServer());
        if (ConstructionManager.isBusy()) {
            player.sendSystemMessage(Component.literal("Wait - something is still being built.").withStyle(ChatFormatting.RED));
            return false;
        }
        Direction look = player.getDirection();
        int groundY = player.blockPosition().getY();
        BlockPos center = player.blockPosition().relative(look, FOUND_DISTANCE + HZ);
        Rotation rotation = com.dranant.clinic.Frame.rotationFacing(look.getOpposite());
        Frame city = new Frame(new BlockPos(center.getX(), groundY - 1, center.getZ()), rotation);
        data.setCity(city.origin(), rotation, level.dimension());
        long seed = city.origin().asLong() ^ 0x5DEECE66DL;

        List<Supplier<ConstructionManager.Task>> stages = new ArrayList<>();
        BoundingBox area = BoundingBox.fromCorners(city.at(-HX, 0, -HZ), city.at(HX, 0, HZ));
        stages.add(() -> ConstructionManager.terrainTask(level, area, city.origin().getY(), 64));

        // ---------------- district
        stages.add(piece(level, city, bp -> district(bp, RandomSource.create(seed + 1))));
        for (CityLayout.Plot plot : PLOTS) stages.add(piece(level, city, bp -> plotMarker(bp, plot)));
        // ---------------- roads
        stages.add(piece(level, city, CityBuilder::ringRoad));
        stages.add(piece(level, city, CityBuilder::avenues));

        // ---------------- building lots, all numbered
        List<Lot> lots = new ArrayList<>();
        int[] counter = {0};
        for (int[] l : lotLayout(seed)) {
            lots.add(new Lot(l[2], l[3], lotFrame(city, l[0], l[1]), l[4] == 1, (l[0] == 0 || l[0] == 3) ? -1 : 1));
            counter[0] = Math.max(counter[0], l[3]);
        }
        for (Lot lot : lots) {
            stages.add(() -> ConstructionManager.blueprintTask(level, lotBlueprint(lot, seed), lot.frame().origin(), false, BUDGET));
        }
        // ---------------- landmarks in the 4 corners
        int n = counter[0];
        final int showroomNo = ++n, villaNo = ++n;
        stages.add(piece(level, CityHouse.frame(city), bp -> CityHouse.design(bp, RandomSource.create(seed + 22), villaNo)));
        stages.add(piece(level, CityShowroom.frame(city), bp -> CityShowroom.design(bp, RandomSource.create(seed + 21), showroomNo)));
        final int townHallNo = ++n, statueNo = ++n, fountainNo = ++n, bazaarNo = ++n;
        stages.add(piece(level, city.child(-129, 0, -128, Rotation.NONE), bp -> CityBuildings.townHall(bp, RandomSource.create(seed + 11), townHallNo)));
        stages.add(piece(level, city.child(128, 0, -128, Rotation.NONE), bp -> CityBuildings.statuePlaza(bp, RandomSource.create(seed + 12), statueNo)));
        stages.add(piece(level, city.child(-128, 0, 128, Rotation.CLOCKWISE_180), bp -> CityBuildings.fountainPark(bp, RandomSource.create(seed + 13), fountainNo)));
        stages.add(piece(level, city.child(129, 0, 128, Rotation.CLOCKWISE_180), bp -> CityBuildings.bazaarPlaza(bp, RandomSource.create(seed + 14), bazaarNo)));
        final int totalBuildings = n;
        // ---------------- green belt + wall
        stages.add(piece(level, city, bp -> greenBelt(bp, RandomSource.create(seed + 2))));
        for (Direction side : Direction.Plane.HORIZONTAL) stages.add(piece(level, city, bp -> wall(bp, side, RandomSource.create(seed + side.ordinal()))));
        stages.add(piece(level, city, CityBuilder::gatesAndTowers));

        player.sendSystemMessage(Component.literal("Founding Dr. Anant City (299 x 297)... watch it rise!").withStyle(ChatFormatting.GOLD));
        ConstructionManager.stream(level, stages,
                progress -> level.players().forEach(p -> p.displayClientMessage(Component.literal(String.format("Dr. Anant City construction: %d%%", (int) (progress * 100)))
                        .withStyle(ChatFormatting.YELLOW), true)),
                l -> finish(l, city, totalBuildings, seed));
        return true;
    }

    private static Supplier<ConstructionManager.Task> piece(ServerLevel level, Frame frame, java.util.function.Consumer<Blueprint> design) {
        return () -> {
            Blueprint bp = new Blueprint(frame);
            design.accept(bp);
            return ConstructionManager.blueprintTask(level, bp, frame.origin(), false, BUDGET);
        };
    }

    private static void finish(ServerLevel level, Frame city, int buildings, long seed) {
        CityData.get(level.getServer()).setCityComplete(buildings);
        RandomSource r = RandomSource.create(seed + 99);
        // citizens: villagers along the sidewalks, golems at the gates, cats in the parks
        for (int i = 0; i < 18; i++) {
            Villager v = EntityType.VILLAGER.create(level);
            if (v == null) continue;
            int lx = r.nextInt(2 * ZONE_X0) - ZONE_X0;
            int lz = (r.nextBoolean() ? 1 : -1) * (ZONE_Z0 + 1);
            if (r.nextBoolean()) {
                lz = r.nextInt(2 * ZONE_Z0) - ZONE_Z0;
                lx = (r.nextBoolean() ? 1 : -1) * (ZONE_X0 + 1);
            }
            BlockPos p = city.at(lx, 1, lz);
            v.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, r.nextFloat() * 360, 0);
            VillagerProfession[] profs = {VillagerProfession.FARMER, VillagerProfession.LIBRARIAN, VillagerProfession.CLERIC,
                    VillagerProfession.BUTCHER, VillagerProfession.MASON, VillagerProfession.NONE};
            v.setVillagerData(v.getVillagerData().setProfession(profs[r.nextInt(profs.length)]));
            level.addFreshEntity(v);
        }
        for (int[] g : new int[][]{{0, 140}, {0, -140}, {140, 0}, {-140, 0}}) {
            IronGolem golem = EntityType.IRON_GOLEM.create(level);
            if (golem == null) continue;
            BlockPos p = city.at(g[0], 1, g[1]);
            golem.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
            golem.setPlayerCreated(true);
            level.addFreshEntity(golem);
        }
        for (int i = 0; i < 5; i++) {
            Cat cat = EntityType.CAT.create(level);
            if (cat == null) continue;
            BlockPos p = city.at(-128 + r.nextInt(10) - 5, 1, 128 + r.nextInt(10) - 5);
            cat.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
            level.addFreshEntity(cat);
        }
        BlockPos gate = city.at(0, 1, HZ + 2);
        ConstructionManager.finale(level, gate);
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                "Dr. Anant City is complete! " + buildings + " buildings (incl. ANANT MOTORS showroom + Dr. Anant's Villa) + Clinic District with 7 plots. Use /build clinic <1-6> or /build shop.")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
    }

    // =================================================================== lots
    private static final int[] TYPE_WIDTH = {13, 15, 19, 17, 13, 15}; // cottage, apartment, villa, market, garden, tower
    private static final int[] TYPE_WEIGHT = {30, 20, 12, 13, 10, 15};

    private static int pickType(RandomSource r) {
        int total = 0;
        for (int w : TYPE_WEIGHT) total += w;
        int roll = r.nextInt(total);
        for (int i = 0; i < TYPE_WEIGHT.length; i++) {
            roll -= TYPE_WEIGHT[i];
            if (roll < 0) return i;
        }
        return 0;
    }

    /** Walks the four building bands and fills each street segment with lots facing the ring road. */
    public static List<int[]> lotLayout(long seed) {
        RandomSource r = RandomSource.create(seed);
        List<int[]> out = new ArrayList<>(); // {side, alongCenter, type, number, hedgeAfter}
        int number = 0;
        int[][] segmentsNS = {{-114, -61}, {-55, -5}, {5, 55}, {61, 114}};
        int[][] segmentsEW = {{-113, -61}, {-55, -5}, {5, 55}, {61, 113}};
        for (int side = 0; side < 4; side++) { // 0 south, 1 north, 2 east, 3 west
            int[][] segs = side < 2 ? segmentsNS : segmentsEW;
            for (int[] seg : segs) {
                if (side == 0 && (seg[0] == 5 || seg[0] == -55)) continue; // reserved: ANANT MOTORS showroom (5..55) + Dr. Anant's Villa (-55..-5)
                int cursor = seg[0];
                while (true) {
                    int type = pickType(r);
                    int w = TYPE_WIDTH[type];
                    if (cursor + w - 1 > seg[1]) {
                        type = 0;
                        w = TYPE_WIDTH[0];
                        if (cursor + w - 1 > seg[1]) break;
                    }
                    out.add(new int[]{side, cursor + w / 2, type, ++number, cursor + w + 1 <= seg[1] ? 1 : 0});
                    cursor += w + 2;
                }
            }
        }
        return out;
    }

    private static Frame lotFrame(Frame city, int side, int along) {
        return switch (side) {
            case 0 -> city.child(along, 0, 127, Rotation.CLOCKWISE_180);       // south band, faces north
            case 1 -> city.child(along, 0, -127, Rotation.NONE);               // north band, faces south
            case 2 -> city.child(128, 0, along, Frame.rotationFacing(Direction.WEST));
            default -> city.child(-128, 0, along, Frame.rotationFacing(Direction.EAST));
        };
    }

    private static Blueprint lotBlueprint(Lot lot, long seed) {
        Blueprint bp = new Blueprint(lot.frame());
        RandomSource r = RandomSource.create(seed * 31 + lot.number());
        switch (lot.type()) {
            case 1 -> CityBuildings.apartment(bp, r, lot.number());
            case 2 -> CityBuildings.villa(bp, r, lot.number());
            case 3 -> CityBuildings.market(bp, r, lot.number());
            case 4 -> CityBuildings.garden(bp, r, lot.number());
            case 5 -> CityBuildings.tower(bp, r, lot.number());
            default -> CityBuildings.cottage(bp, r, lot.number());
        }
        // front sidewalk + street lamp for every lot
        int hw = TYPE_WIDTH[lot.type()] / 2;
        for (int x = -hw; x <= hw; x++) {
            bp.set(x, 0, 12, Blocks.SMOOTH_STONE.defaultBlockState());
            bp.set(x, 0, 13, Blocks.SMOOTH_STONE.defaultBlockState());
        }
        if (lot.hedge()) {
            int hx = lot.alongSign() * (hw + 1); // the gap toward the next lot along the street
            hedge(bp, hx, -12, hx, 9, 1, 1);
            lamp(bp, hx, 1, 11);
        }
        return bp;
    }

    // =================================================================== district + plots
    private static void district(Blueprint bp, RandomSource r) {
        // internal service road between plot rows
        for (int x = -DIST_HX; x <= DIST_HX; x++)
            for (int z = SERVICE_ROAD_Z0; z <= SERVICE_ROAD_Z1; z++) {
                boolean walk = z == SERVICE_ROAD_Z0 || z == SERVICE_ROAD_Z1;
                boolean line = z == (SERVICE_ROAD_Z0 + SERVICE_ROAD_Z1) / 2 && Math.floorMod(x, 4) < 2;
                bp.set(x, 0, z, walk ? Blocks.SMOOTH_STONE.defaultBlockState() : line ? Blocks.YELLOW_CONCRETE.defaultBlockState() : Blocks.GRAY_CONCRETE.defaultBlockState());
            }
        for (int x = -DIST_HX + 4; x <= DIST_HX - 4; x += 12) {
            lamp(bp, x, 1, SERVICE_ROAD_Z0);
            lamp(bp, x + 6, 1, SERVICE_ROAD_Z1);
        }
        // paths from the ring road to every front-row plot + benches and lamps on the front lawn
        for (CityLayout.Plot p : PLOTS) {
            int frontZ = p.frontZ();
            int pathEnd = p.tier() == 6 ? SERVICE_ROAD_Z0 - 1 : DIST_HZ;
            for (int z = frontZ + 1; z <= pathEnd; z++)
                for (int x = p.cx() - 1; x <= p.cx() + 1; x++) bp.set(x, 0, z, Blocks.SMOOTH_STONE.defaultBlockState());
        }
        // trees + flowers in every free spot of the district (never inside a plot)
        for (int x = -DIST_HX + 3; x <= DIST_HX - 3; x += 7)
            for (int z = -DIST_HZ + 3; z <= DIST_HZ - 3; z += 7) {
                if (nearPlotOrRoad(x, z, 4)) continue;
                tree(bp, x + r.nextInt(3) - 1, 1, z + r.nextInt(3) - 1, randomTree(r), r);
            }
        for (int i = 0; i < 900; i++) {
            int x = r.nextInt(2 * DIST_HX - 2) - DIST_HX + 1, z = r.nextInt(2 * DIST_HZ - 2) - DIST_HZ + 1;
            if (!nearPlotOrRoad(x, z, 1)) bp.set(x, 1, z, FLOWERS[r.nextInt(FLOWERS.length)].defaultBlockState());
        }
        // healing gardens beside the Mega Hospital plot: ponds with lilies and benches
        for (int side = -1; side <= 1; side += 2) {
            int pcx = side * 85, pcz = -45;
            for (int x = pcx - 6; x <= pcx + 6; x++)
                for (int z = pcz - 6; z <= pcz + 6; z++) {
                    double d = Math.hypot(x - pcx, z - pcz);
                    if (d < 5.5) {
                        bp.set(x, 0, z, Blocks.WATER.defaultBlockState());
                        bp.set(x, -1, z, Blocks.CLAY.defaultBlockState());
                        bp.set(x, 1, z, r.nextInt(5) == 0 ? Blocks.LILY_PAD.defaultBlockState() : Blocks.AIR.defaultBlockState());
                    } else if (d < 6.5) {
                        bp.set(x, 0, z, Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
                        bp.set(x, 1, z, Blocks.AIR.defaultBlockState());
                    }
                }
            bench(bp, pcx - 2, 1, pcz + 8, Direction.NORTH, 4);
            lamp(bp, pcx + 4, 1, pcz + 8);
            lamp(bp, pcx - 4, 1, pcz - 8);
            sign(bp, pcx, 1, pcz + 7, Direction.SOUTH, DyeColor.LIME, "Healing Garden", "Relax karo,", "jaldi theek", "ho jaoge!");
        }
        // CLINIC DISTRICT gateway arch on the front lawn
        for (int y = 1; y <= 8; y++) {
            bp.set(-24, y, DIST_HZ - 1, Blocks.QUARTZ_PILLAR.defaultBlockState());
            bp.set(-20, y, DIST_HZ - 1, Blocks.QUARTZ_PILLAR.defaultBlockState());
        }
        for (int x = -25; x <= -19; x++) bp.set(x, 9, DIST_HZ - 1, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
        hologram(bp, -22, 11, DIST_HZ - 1, json("CLINIC DISTRICT", "aqua", true), 3.0F, "dranant_district");
    }

    private static boolean nearPlotOrRoad(int x, int z, int margin) {
        if (z >= SERVICE_ROAD_Z0 - margin && z <= SERVICE_ROAD_Z1 + margin) return true;
        if (z >= DIST_HZ - 2 - margin) return true;
        if (x >= -26 && x <= -18 && z >= DIST_HZ - 3) return true;
        for (CityLayout.Plot p : PLOTS) {
            if (x >= p.minX() - margin && x <= p.maxX() + margin && z >= p.minZ() - margin && z <= p.maxZ() + margin + 4) return true;
        }
        for (int side = -1; side <= 1; side += 2) if (Math.hypot(x - side * 85, z + 45) < 9 + margin) return true;
        return false;
    }

    private static void plotMarker(Blueprint bp, CityLayout.Plot p) {
        BlockState curb = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = p.minX(); x <= p.maxX(); x++) {
            bp.set(x, 0, p.minZ(), curb);
            if (Math.abs(x - p.cx()) > 1) bp.set(x, 0, p.maxZ(), curb);
        }
        for (int z = p.minZ(); z <= p.maxZ(); z++) {
            bp.set(p.minX(), 0, z, curb);
            bp.set(p.maxX(), 0, z, curb);
        }
        lamp(bp, p.minX(), 1, p.maxZ());
        lamp(bp, p.maxX(), 1, p.maxZ());
        bp.set(p.cx() + 3, 1, p.maxZ() + 1, Blocks.DARK_OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 0));
        final BlockPos signPos = bp.frame().at(p.cx() + 3, 1, p.maxZ() + 1);
        String[] lines = {"PLOT " + p.id(), p.tier() == 0 ? "Central Shop" : "Level " + p.tier() + " Clinic",
                (p.w() - 2) + " x " + (p.d() - 2) + " blocks", p.tier() == 0 ? "/build shop" : "/build clinic " + p.tier()};
        bp.after(level -> writeSign(level, signPos, DyeColor.WHITE, lines));
        hologram(bp, p.cx(), 7, p.maxZ(), PlotManager.plotHologramJson(p, false, 0), 1.3F, PlotManager.plotTag(p));
    }

    // =================================================================== roads
    private static void ringRoad(Blueprint bp) {
        for (int x = -(ZONE_X0 - 1); x <= ZONE_X0 - 1; x++)
            for (int z = -(ZONE_Z0 - 1); z <= ZONE_Z0 - 1; z++) {
                int k = Math.max(Math.abs(x) - DIST_HX, Math.abs(z) - DIST_HZ);
                if (k < 1 || k > ROAD) continue;
                BlockState s;
                if (k == 1 || k == ROAD) s = Blocks.SMOOTH_STONE.defaultBlockState();
                else if (k == 5) {
                    int along = Math.abs(x) - DIST_HX >= Math.abs(z) - DIST_HZ ? z : x;
                    s = Math.floorMod(along, 4) < 2 ? Blocks.YELLOW_CONCRETE.defaultBlockState() : Blocks.GRAY_CONCRETE.defaultBlockState();
                } else s = Blocks.GRAY_CONCRETE.defaultBlockState();
                bp.set(x, 0, z, s);
                if ((k == 1 || k == ROAD) && Math.floorMod(x + z * 3, 14) == 0) lamp(bp, x, 1, z);
            }
    }

    private static void avenues(Blueprint bp) {
        // four 9-wide avenues from the ring road to the four gates
        for (int a = ZONE_Z0; a <= WALL_Z0 + WALL - 1; a++)
            for (int w = -4; w <= 4; w++) {
                for (int sgn = -1; sgn <= 1; sgn += 2) bp.set(w, 0, a * sgn, roadState(w, a));
            }
        for (int a = ZONE_X0; a <= WALL_X0 + WALL - 1; a++)
            for (int w = -4; w <= 4; w++) {
                for (int sgn = -1; sgn <= 1; sgn += 2) bp.set(a * sgn, 0, w, roadState(w, a));
            }
        for (int a = ZONE_Z0 + 3; a <= GREEN_Z0 + 2; a += 9) {
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                lamp(bp, -5, 1, a * sgn);
                lamp(bp, 5, 1, a * sgn);
            }
        }
        for (int a = ZONE_X0 + 3; a <= GREEN_X0 + 2; a += 9) {
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                lamp(bp, a * sgn, 1, -5);
                lamp(bp, a * sgn, 1, 5);
            }
        }
        // side streets (5 wide) splitting the building bands into blocks
        for (int s : new int[]{-58, 58}) {
            for (int a = ZONE_Z0; a <= GREEN_Z0 + GREEN - 1; a++)
                for (int w = -2; w <= 2; w++)
                    for (int sgn = -1; sgn <= 1; sgn += 2)
                        bp.set(s + w, 0, a * sgn, Math.abs(w) == 2 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState());
            for (int a = ZONE_X0; a <= GREEN_X0 + GREEN - 1; a++)
                for (int w = -2; w <= 2; w++)
                    for (int sgn = -1; sgn <= 1; sgn += 2)
                        bp.set(a * sgn, 0, s + w, Math.abs(w) == 2 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState());
        }
    }

    private static BlockState roadState(int w, int along) {
        if (Math.abs(w) == 4) return Blocks.SMOOTH_STONE.defaultBlockState();
        if (w == 0 && Math.floorMod(along, 4) < 2) return Blocks.YELLOW_CONCRETE.defaultBlockState();
        return Blocks.GRAY_CONCRETE.defaultBlockState();
    }

    private static boolean isStreet(int along) {
        return Math.abs(along) <= 5 || (Math.abs(along) >= 55 && Math.abs(along) <= 61);
    }

    // =================================================================== green belt + wall
    private static void greenBelt(Blueprint bp, RandomSource r) {
        // south / north belts
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            for (int x = -GREEN_X0 + 1; x <= GREEN_X0 - 1; x++) {
                if (isStreet(x)) continue;
                bp.set(x, 1, GREEN_Z0 * sgn, leaves(Blocks.AZALEA_LEAVES));
                if (r.nextInt(4) == 0) bp.set(x, 1, (GREEN_Z0 + 2) * sgn, FLOWERS[r.nextInt(FLOWERS.length)].defaultBlockState());
            }
            for (int x = -GREEN_X0 + 3; x <= GREEN_X0 - 3; x += 7) {
                if (isStreet(x) || isStreet(x + 2) || isStreet(x - 2)) continue;
                tree(bp, x, 1, (GREEN_Z0 + 2) * sgn, randomTree(r), r);
            }
        }
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            for (int z = -GREEN_Z0 + 1; z <= GREEN_Z0 - 1; z++) {
                if (isStreet(z)) continue;
                bp.set(GREEN_X0 * sgn, 1, z, leaves(Blocks.AZALEA_LEAVES));
                if (r.nextInt(4) == 0) bp.set((GREEN_X0 + 2) * sgn, 1, z, FLOWERS[r.nextInt(FLOWERS.length)].defaultBlockState());
            }
            for (int z = -GREEN_Z0 + 3; z <= GREEN_Z0 - 3; z += 7) {
                if (isStreet(z) || isStreet(z + 2) || isStreet(z - 2)) continue;
                tree(bp, (GREEN_X0 + 2) * sgn, 1, z, randomTree(r), r);
            }
        }
    }

    private static void wall(Blueprint bp, Direction side, RandomSource r) {
        boolean zSide = side.getAxis() == Direction.Axis.Z;
        int sgn = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
        int from = zSide ? -HX : -HZ + WALL;
        int to = zSide ? HX : HZ - WALL;
        int base = zSide ? WALL_Z0 : WALL_X0;
        for (int a = from; a <= to; a++) {
            boolean gate = Math.abs(a) <= 4;
            for (int t = 0; t < WALL; t++) {
                int c = (base + t) * sgn;
                int x = zSide ? a : c, z = zSide ? c : a;
                for (int y = 1; y <= WALL_HEIGHT; y++) {
                    if (gate && y <= 7) {
                        bp.set(x, y, z, Blocks.AIR.defaultBlockState());
                        continue;
                    }
                    Block b = y == WALL_HEIGHT ? Blocks.SMOOTH_STONE
                            : r.nextInt(9) == 0 ? Blocks.MOSSY_STONE_BRICKS : r.nextInt(12) == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS;
                    if (gate && y == 8) b = Blocks.CHISELED_QUARTZ_BLOCK;
                    bp.set(x, y, z, b.defaultBlockState());
                }
                if (t == WALL - 1 && Math.floorMod(a, 2) == 0) bp.set(x, WALL_HEIGHT + 1, z, Blocks.STONE_BRICK_WALL.defaultBlockState());
                if (t == 0) bp.set(x, WALL_HEIGHT + 1, z, Blocks.STONE_BRICK_WALL.defaultBlockState());
                if (t == 1 && Math.floorMod(a, 10) == 0) bp.set(x, WALL_HEIGHT + 1, z, Blocks.LANTERN.defaultBlockState());
            }
        }
    }

    private static void gatesAndTowers(Blueprint bp) {
        int[][] towers = {
                {-HX + 2, -HZ + 2}, {HX - 2, -HZ + 2}, {-HX + 2, HZ - 2}, {HX - 2, HZ - 2},
                {-7, HZ - 2}, {7, HZ - 2}, {-7, -HZ + 2}, {7, -HZ + 2},
                {HX - 2, -7}, {HX - 2, 7}, {-HX + 2, -7}, {-HX + 2, 7}};
        for (int[] t : towers) tower(bp, t[0], t[1]);
        // banners + city name above the main (south) gate
        for (int x : new int[]{-7, 7}) {
            bp.set(x, 11, HZ + 1, Blocks.RED_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING, Direction.SOUTH));
        }
        hologram(bp, 0, 13, HZ + 1, json("DR. ANANT CITY", "gold", true), 4.0F, "dranant_city_name");
        hologram(bp, 0, 11, HZ + 1, json("Anant Doctor karege aapke sabhi dukho ka ilaaj", "yellow", false), 1.6F, "dranant_city_slogan");
        hologram(bp, 0, 12, -HZ - 1, json("DR. ANANT CITY - North Gate", "gold", true), 2.5F, "dranant_city_n");
        hologram(bp, HX + 1, 12, 0, json("DR. ANANT CITY - East Gate", "gold", true), 2.5F, "dranant_city_e");
        hologram(bp, -HX - 1, 12, 0, json("DR. ANANT CITY - West Gate", "gold", true), 2.5F, "dranant_city_w");
    }

    private static void tower(Blueprint bp, int cx, int cz) {
        int top = WALL_HEIGHT + 5;
        for (int y = 1; y <= top; y++)
            for (int x = cx - 2; x <= cx + 2; x++)
                for (int z = cz - 2; z <= cz + 2; z++) {
                    boolean edge = Math.abs(x - cx) == 2 || Math.abs(z - cz) == 2;
                    if (edge) bp.set(x, y, z, (y % 5 == 0) ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState());
                    else bp.set(x, y, z, y == top ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
        for (int k = 0; k <= 2; k++) {
            int y = top + 1 + k;
            int rad = 3 - k;
            for (int x = cx - rad; x <= cx + rad; x++) {
                bp.set(x, y, cz - rad, stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH));
                bp.set(x, y, cz + rad, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
            }
            for (int z = cz - rad; z <= cz + rad; z++) {
                bp.set(cx - rad, y, z, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST));
                bp.set(cx + rad, y, z, stair(Blocks.DARK_OAK_STAIRS, Direction.WEST));
            }
        }
        bp.set(cx, top + 4, cz, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        bp.set(cx, top + 5, cz, Blocks.LANTERN.defaultBlockState());
        bp.set(cx, top - 3, cz + 2, Blocks.GLASS_PANE.defaultBlockState());
        bp.set(cx, top - 3, cz - 2, Blocks.GLASS_PANE.defaultBlockState());
    }

    public static Vec3 gateLocation(Frame city) {
        return Vec3.atBottomCenterOf(city.at(0, 1, HZ + 3));
    }

    private CityBuilder() {}
}
