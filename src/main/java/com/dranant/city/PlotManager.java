package com.dranant.city;

import com.dranant.clinic.ClinicBuilder;
import com.dranant.clinic.ClinicTiers;
import com.dranant.clinic.CentralShopBuilder;
import com.dranant.clinic.ConstructionManager;
import com.dranant.clinic.Frame;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Builds the shop and clinics 1-6 into their reserved Clinic District plots (line by line), keeps count of every
 * clinic built, and erases them again with "/erase clinic N" (animated, top-down) - inside or outside the city.
 */
public final class PlotManager {

    public static String plotTag(CityLayout.Plot p) {
        return "dranant_plot_" + p.id();
    }

    public static String plotHologramJson(CityLayout.Plot p, boolean built, int clinicNumber) {
        String size = (p.w() - 2) + "x" + (p.d() - 2);
        String text = "PLOT " + p.id() + " | " + p.title() + " | " + size + (built ? " | BUILT - Clinic #" + clinicNumber : " | EMPTY");
        return CityDecor.json(text, built ? "green" : "yellow", true);
    }

    private static Frame cityFrame(MinecraftServer server) {
        return CityData.get(server).cityFrame();
    }

    @Nullable
    private static ServerLevel cityLevel(MinecraftServer server) {
        return server.getLevel(CityData.get(server).getCityDimension());
    }

    /** World box of a plot's interior (inside the curb), from street level up to +64. */
    public static BoundingBox plotInterior(Frame city, CityLayout.Plot p) {
        return BoundingBox.fromCorners(city.at(p.minX() + 1, 1, p.minZ() + 1), city.at(p.maxX() - 1, 64, p.maxZ() - 1));
    }

    /** Builds tier 0 (shop) or 1-6 into its plot. @return true if construction started. */
    public static boolean buildInPlot(MinecraftServer server, int tier, @Nullable Player player) {
        CityData data = CityData.get(server);
        ServerLevel level = cityLevel(server);
        CityLayout.Plot plot = CityLayout.plotForTier(tier);
        if (!data.hasCity() || level == null || plot == null) {
            msg(player, "There is no Dr. Anant City yet. Use /city build first.", ChatFormatting.RED);
            return false;
        }
        CityData.Built existing = data.builtOnPlot(plot.id());
        if (existing != null) {
            msg(player, "Plot " + plot.id() + " already has " + plot.title() + " (Clinic #" + existing.number() + "). Use /erase "
                    + (tier == 0 ? "shop" : "clinic " + tier) + " first.", ChatFormatting.RED);
            return false;
        }
        Frame city = data.cityFrame();
        BlockPos center = city.at(plot.cx(), 1, plot.cz());
        if (tier == 0) {
            CentralShopBuilder.buildAt(level, center, city.rotation(), player);
        } else {
            ClinicBuilder.build(level, ClinicBuilder.planAt(level, tier, center, city.rotation()));
        }
        int number = data.addBuilt(tier, plot.id(), level.dimension(), plotInterior(city, plot), city.origin().getY());
        refreshHologram(level, city, plot, true, number);
        msg(player, "Building " + plot.title() + " on PLOT " + plot.id() + " - Clinic #" + number + " in the city!", ChatFormatting.GREEN);
        return true;
    }

    /** Records a clinic/shop built outside the city (so it can be erased later). */
    public static int recordFreeform(ServerLevel level, int tier, BoundingBox box, int floorY) {
        return CityData.get(level.getServer()).addBuilt(tier, 0, level.dimension(), box, floorY);
    }

    public static void refreshHologram(ServerLevel level, Frame city, CityLayout.Plot plot, boolean built, int number) {
        Vec3 pos = Vec3.atBottomCenterOf(city.at(plot.cx(), 7, plot.maxZ()));
        CityDecor.removeHolograms(level, pos, 3.0D, plotTag(plot));
        CityDecor.spawnHologram(level, pos, plotHologramJson(plot, built, number), 1.3F, plotTag(plot));
    }

    /** Erases the latest built clinic of {@code tier} (0 = shop). */
    public static boolean erase(MinecraftServer server, int tier, Consumer<Component> feedback) {
        CityData data = CityData.get(server);
        CityData.Built b = data.latestOfTier(tier);
        String name = tier == 0 ? "Central Shop" : "Level " + tier + " " + ClinicTiers.name(tier);
        if (b == null) {
            feedback.accept(Component.literal("No " + name + " has been built.").withStyle(ChatFormatting.RED));
            return false;
        }
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(b.dimension()));
        ServerLevel level = server.getLevel(dim);
        if (level == null) return false;
        data.remove(b);
        ConstructionManager.erase(level, b.box(), b.floorY(), Blocks.GRASS_BLOCK.defaultBlockState(), l -> {
            if (b.plotId() > 0) {
                for (CityLayout.Plot p : CityLayout.PLOTS) {
                    if (p.id() == b.plotId()) refreshHologram(l, data.cityFrame(), p, false, 0);
                }
            }
        });
        feedback.accept(Component.literal("Erasing " + name + " (Clinic #" + b.number() + ")" + (b.plotId() > 0 ? " from PLOT " + b.plotId() : "") + "...")
                .withStyle(ChatFormatting.YELLOW));
        return true;
    }

    public static int eraseAll(MinecraftServer server, Consumer<Component> feedback) {
        int n = 0;
        for (int tier = 0; tier <= ClinicTiers.MAX_TIER; tier++) {
            while (CityData.get(server).latestOfTier(tier) != null) {
                if (!erase(server, tier, feedback)) break;
                n++;
            }
        }
        return n;
    }

    public static Component info(MinecraftServer server) {
        CityData data = CityData.get(server);
        if (!data.hasCity()) return Component.literal("No city yet. Stand on flat land and use /city build.").withStyle(ChatFormatting.YELLOW);
        StringBuilder sb = new StringBuilder();
        sb.append("Dr. Anant City - ").append(data.isCityComplete() ? "complete" : "under construction")
                .append(" | Buildings: ").append(data.getBuildingCount()).append('\n');
        int built = 0;
        for (CityLayout.Plot p : CityLayout.PLOTS) {
            CityData.Built b = data.builtOnPlot(p.id());
            if (b != null) built++;
            sb.append(" PLOT ").append(p.id()).append(": ").append(p.title()).append(" (").append(p.w() - 2).append('x').append(p.d() - 2)
                    .append(") - ").append(b != null ? "BUILT, Clinic #" + b.number() : "empty").append('\n');
        }
        sb.append("Clinic District: ").append(built).append("/").append(CityLayout.PLOTS.size()).append(" plots built | Total clinics on record: ")
                .append(data.getBuilt().size());
        return Component.literal(sb.toString()).withStyle(ChatFormatting.AQUA);
    }

    private static void msg(@Nullable Player player, String text, ChatFormatting color) {
        if (player != null) player.sendSystemMessage(Component.literal(text).withStyle(color));
    }

    private PlotManager() {}
}
