package com.dranant.entity;

import com.dranant.DoctorAnantMod;
import net.minecraft.resources.ResourceLocation;

/** The three showroom supercars. Speeds are in blocks per tick (x72 = km/h). */
public enum SupercarVariant {
    VELOCE("veloce", "Anant Veloce V12", 450_000L, 2.60D, 0.036D, 0.30D, DoorStyle.SCISSOR, 0xFF2030, 790, "2.9 s"),
    GT("gt", "Anant Stradale GT", 300_000L, 2.30D, 0.040D, 0.38D, DoorStyle.CONVENTIONAL, 0x3C8CFF, 650, "3.2 s"),
    PHANTOM("phantom", "Anant Phantom Noir", 750_000L, 2.90D, 0.032D, 0.33D, DoorStyle.GULLWING, 0xD4AF37, 1020, "2.5 s");

    public enum DoorStyle { SCISSOR, CONVENTIONAL, GULLWING }

    private final String id;
    private final String displayName;
    private final long price;
    private final double topSpeed;
    private final double accel;
    private final double grip;
    private final DoorStyle doorStyle;
    private final int color;
    private final int horsepower;
    private final String zeroToHundred;
    private final ResourceLocation texture;

    SupercarVariant(String id, String displayName, long price, double topSpeed, double accel, double grip, DoorStyle doorStyle,
                    int color, int horsepower, String zeroToHundred) {
        this.id = id;
        this.displayName = displayName;
        this.price = price;
        this.topSpeed = topSpeed;
        this.accel = accel;
        this.grip = grip;
        this.doorStyle = doorStyle;
        this.color = color;
        this.horsepower = horsepower;
        this.zeroToHundred = zeroToHundred;
        this.texture = DoctorAnantMod.id("textures/entity/supercar/" + id + ".png");
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public long price() { return price; }
    public double topSpeed() { return topSpeed; }
    public double accel() { return accel; }
    public double grip() { return grip; }
    public DoorStyle doorStyle() { return doorStyle; }
    public int color() { return color; }
    public int horsepower() { return horsepower; }
    public String zeroToHundred() { return zeroToHundred; }
    public ResourceLocation texture() { return texture; }
    public int topSpeedKmh() { return (int) Math.round(topSpeed * 72.0D); }

    public static SupercarVariant byOrdinal(int i) {
        SupercarVariant[] v = values();
        return v[Math.floorMod(i, v.length)];
    }
}
