package com.dranant.city;

import com.dranant.clinic.ClinicTiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dr. Anant City geometry (city-local coordinates, x = east, z = south, gate on the south side, y 0 = street level).
 *
 * <pre>
 *   wall (3) | green belt (4) | building zone (28) | ring road (9) | CLINIC DISTRICT 211 x 209 | ...
 * </pre>
 * The district holds 7 reserved plots, sized from the real building footprints (+ clear margin + curb):
 * Row 1 (front, line by line): Central Shop, Level 1, 2, 3, 4, 5. Row 2 (back): Level 6 Mega Hospital (125 x 128).
 */
public final class CityLayout {
    public static final int DIST_HX = 105;
    public static final int DIST_HZ = 104;
    public static final int ROAD = 9;
    public static final int ZONE = 28;
    public static final int GREEN = 4;
    public static final int WALL = 3;
    public static final int WALL_HEIGHT = 9;

    public static final int ROAD_X0 = DIST_HX + 1;            // 106
    public static final int ROAD_Z0 = DIST_HZ + 1;            // 105
    public static final int ZONE_X0 = ROAD_X0 + ROAD;         // 115
    public static final int ZONE_Z0 = ROAD_Z0 + ROAD;         // 114
    public static final int ZONE_X1 = ZONE_X0 + ZONE - 1;     // 142
    public static final int ZONE_Z1 = ZONE_Z0 + ZONE - 1;     // 141
    public static final int GREEN_X0 = ZONE_X1 + 1;           // 143
    public static final int GREEN_Z0 = ZONE_Z1 + 1;           // 142
    public static final int WALL_X0 = GREEN_X0 + GREEN;       // 147
    public static final int WALL_Z0 = GREEN_Z0 + GREEN;       // 146
    public static final int HX = WALL_X0 + WALL - 1;          // 149  -> city is 299 wide
    public static final int HZ = WALL_Z0 + WALL - 1;          // 148  -> city is 297 deep

    /** Distance from the player to the city's south wall when it is founded. */
    public static final int FOUND_DISTANCE = 12;

    /** Internal district road between the two plot rows. */
    public static final int SERVICE_ROAD_Z0 = 29;
    public static final int SERVICE_ROAD_Z1 = 35;

    /** A reserved building plot. tier 0 = Central Shop. (cx, cz) = centre, w/d = reserved size including curb. */
    public record Plot(int id, int tier, int cx, int cz, int w, int d) {
        public String title() {
            return tier == 0 ? "Central Shop" : "Level " + tier + " - " + ClinicTiers.name(tier);
        }

        public int minX() { return cx - w / 2; }
        public int minZ() { return cz - d / 2; }
        public int maxX() { return minX() + w - 1; }
        public int maxZ() { return minZ() + d - 1; }
        public int frontZ() { return maxZ(); }
    }

    public static final List<Plot> PLOTS;

    static {
        List<Plot> list = new ArrayList<>();
        // tier, building core width, core depth
        int[][] row1 = {{0, 19, 19}, {1, 15, 13}, {2, 21, 17}, {3, 27, 21}, {4, 21, 36}, {5, 44, 61}};
        int x = -101;
        int front = DIST_HZ - 3;
        int id = 1;
        for (int[] r : row1) {
            int tier = r[0];
            boolean procedural = tier >= 1 && tier <= 3;
            int w = r[1] + (procedural ? 6 : 4);
            int d = r[2] + (procedural ? 6 : 4);
            list.add(new Plot(id++, tier, x + w / 2, front - d / 2, w, d));
            x += w + 5;
        }
        list.add(new Plot(id, 6, 0, -102 + 65, 129, 130)); // Level 6: 125 x 128 structure
        PLOTS = Collections.unmodifiableList(list);
    }

    public static Plot plotForTier(int tier) {
        for (Plot p : PLOTS) if (p.tier() == tier) return p;
        return null;
    }

    public static boolean insideCity(int lx, int lz) {
        return Math.abs(lx) <= HX && Math.abs(lz) <= HZ;
    }

    private CityLayout() {}
}
