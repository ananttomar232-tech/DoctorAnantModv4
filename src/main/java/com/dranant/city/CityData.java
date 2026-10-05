package com.dranant.city;

import com.dranant.clinic.Frame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * World-saved city + clinic registry: where Dr. Anant City is, which plots are built, how many buildings the city has,
 * and every clinic/shop ever built (so "/erase clinic N" can remove it, inside or outside the city).
 */
public class CityData extends SavedData {
    private static final String NAME = "dranant_city";

    private boolean hasCity;
    private boolean cityComplete;
    private BlockPos cityOrigin = BlockPos.ZERO;
    private Rotation cityRotation = Rotation.NONE;
    private ResourceKey<Level> cityDimension = Level.OVERWORLD;
    private int buildingCount;

    /** A built clinic (tier 1-6) or shop (tier 0). plotId 0 = built outside the city. */
    public record Built(int tier, int plotId, String dimension, BoundingBox box, int floorY, int number) {}

    private final List<Built> built = new ArrayList<>();
    private int clinicsEverBuilt;

    public static CityData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(CityData::new, CityData::load, null), NAME);
    }

    // ------------------------------------------------------------------ city
    public void setCity(BlockPos origin, Rotation rotation, ResourceKey<Level> dimension) {
        hasCity = true;
        cityComplete = false;
        cityOrigin = origin;
        cityRotation = rotation;
        cityDimension = dimension;
        buildingCount = 0;
        built.removeIf(b -> b.plotId() > 0);
        setDirty();
    }

    public void setCityComplete(int buildings) {
        cityComplete = true;
        buildingCount = buildings;
        setDirty();
    }

    public boolean hasCity() {
        return hasCity;
    }

    public boolean isCityComplete() {
        return cityComplete;
    }

    public Frame cityFrame() {
        return new Frame(cityOrigin, cityRotation);
    }

    public BlockPos getCityOrigin() {
        return cityOrigin;
    }

    public Rotation getCityRotation() {
        return cityRotation;
    }

    public ResourceKey<Level> getCityDimension() {
        return cityDimension;
    }

    public int getBuildingCount() {
        return buildingCount;
    }

    /** Converts a world position to city-local x/z (inverse of the city frame). */
    public int[] toLocal(BlockPos world) {
        int dx = world.getX() - cityOrigin.getX();
        int dz = world.getZ() - cityOrigin.getZ();
        return switch (cityRotation) {
            case CLOCKWISE_90 -> new int[]{dz, -dx};
            case CLOCKWISE_180 -> new int[]{-dx, -dz};
            case COUNTERCLOCKWISE_90 -> new int[]{-dz, dx};
            default -> new int[]{dx, dz};
        };
    }

    public boolean isInsideCity(Level level, BlockPos pos) {
        if (!hasCity || !level.dimension().equals(cityDimension)) return false;
        int[] l = toLocal(pos);
        return CityLayout.insideCity(l[0], l[1]);
    }

    // ------------------------------------------------------------------ clinics
    @Nullable
    public Built builtOnPlot(int plotId) {
        for (Built b : built) if (b.plotId() == plotId) return b;
        return null;
    }

    public int addBuilt(int tier, int plotId, ResourceKey<Level> dim, BoundingBox box, int floorY) {
        clinicsEverBuilt++;
        built.add(new Built(tier, plotId, dim.location().toString(), box, floorY, clinicsEverBuilt));
        setDirty();
        return clinicsEverBuilt;
    }

    /** Latest built entry of a tier (plot builds first). */
    @Nullable
    public Built latestOfTier(int tier) {
        Built best = null;
        for (Built b : built) {
            if (b.tier() != tier) continue;
            if (best == null || b.number() > best.number()) best = b;
        }
        return best;
    }

    public void remove(Built b) {
        built.remove(b);
        setDirty();
    }

    public List<Built> getBuilt() {
        return built;
    }

    // ------------------------------------------------------------------ save / load
    private static CityData load(CompoundTag tag, HolderLookup.Provider registries) {
        CityData d = new CityData();
        d.hasCity = tag.getBoolean("hasCity");
        d.cityComplete = tag.getBoolean("cityComplete");
        d.cityOrigin = BlockPos.of(tag.getLong("origin"));
        d.cityRotation = Rotation.values()[Math.max(0, Math.min(3, tag.getInt("rotation")))];
        if (tag.contains("dimension")) {
            d.cityDimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("dimension")));
        }
        d.buildingCount = tag.getInt("buildingCount");
        d.clinicsEverBuilt = tag.getInt("clinicsEverBuilt");
        ListTag list = tag.getList("built", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag b = list.getCompound(i);
            int[] box = b.getIntArray("box");
            if (box.length != 6) continue;
            d.built.add(new Built(b.getInt("tier"), b.getInt("plot"), b.getString("dim"),
                    new BoundingBox(box[0], box[1], box[2], box[3], box[4], box[5]), b.getInt("floorY"), b.getInt("number")));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("hasCity", hasCity);
        tag.putBoolean("cityComplete", cityComplete);
        tag.putLong("origin", cityOrigin.asLong());
        tag.putInt("rotation", cityRotation.ordinal());
        tag.putString("dimension", cityDimension.location().toString());
        tag.putInt("buildingCount", buildingCount);
        tag.putInt("clinicsEverBuilt", clinicsEverBuilt);
        ListTag list = new ListTag();
        for (Built b : built) {
            CompoundTag c = new CompoundTag();
            c.putInt("tier", b.tier());
            c.putInt("plot", b.plotId());
            c.putString("dim", b.dimension());
            BoundingBox x = b.box();
            c.putIntArray("box", new int[]{x.minX(), x.minY(), x.minZ(), x.maxX(), x.maxY(), x.maxZ()});
            c.putInt("floorY", b.floorY());
            c.putInt("number", b.number());
            list.add(c);
        }
        tag.put("built", list);
        return tag;
    }
}
