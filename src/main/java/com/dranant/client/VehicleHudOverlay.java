package com.dranant.client;

import com.dranant.DoctorAnantMod;
import com.dranant.entity.BikeEntity;
import com.dranant.entity.SupercarEntity;
import com.dranant.entity.SupercarVariant;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Driving HUD (bottom-right): HD analogue speedometer + rev counter with smoothly swinging needles, digital speed,
 * gear indicator (R/N/1-7), nitro/turbo gauge, drift meter and status lamps (lights, doors, bonnet, boot).
 */
public class VehicleHudOverlay implements LayeredDraw.Layer {
    private static final ResourceLocation SPEEDO_CAR = DoctorAnantMod.id("textures/gui/speedo_320.png");
    private static final ResourceLocation SPEEDO_BIKE = DoctorAnantMod.id("textures/gui/speedo_200.png");
    private static final ResourceLocation TACH = DoctorAnantMod.id("textures/gui/tach.png");

    private float shownSpeed;
    private float shownRpm;
    private float driftScore;
    private long lastMillis;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        Entity vehicle = mc.player.getVehicle();
        boolean car = vehicle instanceof SupercarEntity;
        boolean bike = vehicle instanceof BikeEntity;
        if (!car && !bike) {
            shownSpeed = 0;
            shownRpm = 0;
            return;
        }
        boolean driver = vehicle.getControllingPassenger() == mc.player;
        Font font = mc.font;
        long now = Util.getMillis();
        float dt = lastMillis == 0 ? 0.016F : Math.min(0.1F, (now - lastMillis) / 1000.0F);
        lastMillis = now;

        int kmh, gear;
        float rpm;
        double tank;
        boolean boosting;
        float slip;
        float maxKmh;
        if (car) {
            SupercarEntity c = (SupercarEntity) vehicle;
            kmh = driver ? c.getSpeedKmh() : estimateKmh(vehicle);
            gear = c.getGear();
            rpm = driver ? c.getRpm() : 900.0F + kmh * 25.0F;
            tank = c.getNitro();
            boosting = c.isNitroActive() || c.isNitroVisible();
            slip = c.getDrift();
            maxKmh = 320.0F;
        } else {
            BikeEntity b = (BikeEntity) vehicle;
            kmh = driver ? b.getSpeedKmh() : estimateKmh(vehicle);
            gear = b.getGear();
            rpm = driver ? b.getRpm() : 1100.0F + kmh * 60.0F;
            tank = b.getTurbo();
            boosting = b.isBoosting();
            slip = b.getSlide();
            maxKmh = 200.0F;
        }
        float k = 1.0F - (float) Math.exp(-dt * 9.0F);
        shownSpeed += (kmh - shownSpeed) * k;
        shownRpm += (rpm - shownRpm) * k;

        int W = g.guiWidth(), H = g.guiHeight();
        int big = 96, small = 70;
        int sx = W - big - 8, sy = H - big - 8;
        int tx = sx - small - 4, ty = H - small - 10;

        // panel backdrop
        g.fill(tx - 6, sy - 18, W - 4, H - 4, 0x70000000);

        g.blit(car ? SPEEDO_CAR : SPEEDO_BIKE, sx, sy, big, big, 0, 0, 256, 256, 256, 256);
        g.blit(TACH, tx, ty, small, small, 0, 0, 256, 256, 256, 256);
        needle(g, sx + big / 2.0F, sy + big / 2.0F, big * 0.40F, Mth.clamp(shownSpeed / maxKmh, 0.0F, 1.0F), 0xFFFF3B30);
        needle(g, tx + small / 2.0F, ty + small / 2.0F, small * 0.40F, Mth.clamp(shownRpm / 9000.0F, 0.0F, 1.0F), 0xFFFF8A00);

        // digital speed
        String sp = String.valueOf(Math.round(shownSpeed));
        g.pose().pushPose();
        g.pose().translate(sx + big / 2.0F, sy + big * 0.66F, 50);
        g.pose().scale(1.5F, 1.5F, 1.0F);
        g.drawString(font, sp, -font.width(sp) / 2, -4, 0xFFFFFFFF, true);
        g.pose().popPose();

        // gear
        String gs = gear < 0 ? "R" : gear == 0 ? "N" : String.valueOf(gear);
        int gx = tx + small / 2, gy = ty + (int) (small * 0.66F);
        g.fill(gx - 7, gy - 6, gx + 7, gy + 7, 0xC0000000);
        g.drawString(font, gs, gx - font.width(gs) / 2, gy - 3, gear < 0 ? 0xFFFF5555 : shownRpm > 7600 ? 0xFFFF3030 : 0xFF55FFFF, true);
        if (shownRpm > 7600 && (now / 120) % 2 == 0) {
            g.drawString(font, "SHIFT", gx - font.width("SHIFT") / 2, ty - 9, 0xFFFF4040, true);
        }

        // title line
        String title = car ? ((SupercarEntity) vehicle).getVariant().displayName() : "Dr. Anant Moto";
        int titleColor = car ? 0xFF000000 | ((SupercarEntity) vehicle).getVariant().color() : 0xFFFFAA30;
        g.drawString(font, title, W - 8 - font.width(title), sy - 14, titleColor, true);

        // nitro / turbo bar
        int barX = tx - 2, barY = sy - 4, barW = (W - 8) - barX - big - 6;
        if (barW < 30) barW = 30;
        String nlabel = car ? "NITRO" : "TURBO";
        g.drawString(font, nlabel, barX, barY - 9, boosting ? 0xFF7FD4FF : 0xFFAAAAAA, true);
        g.fill(barX, barY, barX + barW, barY + 5, 0xFF202020);
        int fillW = (int) (barW * tank);
        int nCol = boosting ? ((now / 80) % 2 == 0 ? 0xFF55DDFF : 0xFFFFFFFF) : 0xFF2A8CFF;
        g.fill(barX, barY, barX + fillW, barY + 5, nCol);

        // drift meter
        if (driver && slip > 0.22F && shownSpeed > 25) driftScore += dt * shownSpeed * slip * 2.0F;
        else driftScore = Math.max(0, driftScore - dt * 400);
        if (driftScore > 5) {
            String d = "DRIFT  " + (int) driftScore;
            g.pose().pushPose();
            g.pose().translate(W / 2.0F, H * 0.30F, 0);
            float s = 1.4F + Mth.sin(now / 90.0F) * 0.06F;
            g.pose().scale(s, s, 1);
            g.drawString(font, d, -font.width(d) / 2, 0, 0xFFFFD040, true);
            g.pose().popPose();
        }

        // status lamps
        if (car) {
            SupercarEntity c = (SupercarEntity) vehicle;
            int lx = tx - 4, ly = H - 12;
            lx = lamp(g, font, lx, ly, "LIGHTS", c.lightsOn(), 0xFF66CCFF);
            boolean doors = c.isPartOpen(SupercarEntity.PART_DOOR_LEFT) || c.isPartOpen(SupercarEntity.PART_DOOR_RIGHT);
            lamp(g, font, lx, ly, doors ? "DOOR OPEN" : c.isPartOpen(SupercarEntity.PART_HOOD) ? "BONNET" : c.isPartOpen(SupercarEntity.PART_TRUNK) ? "BOOT" : "",
                    true, 0xFFFFAA00);
            if (c.isBraking() && driver) g.drawString(font, "BRAKE", tx + small / 2 - font.width("BRAKE") / 2, ty + small - 2, 0xFFFF3030, true);
        }
    }

    private static int lamp(GuiGraphics g, Font font, int rightX, int y, String label, boolean on, int color) {
        if (label.isEmpty()) return rightX;
        int w = font.width(label);
        int x = rightX - w;
        if (x < 4) return rightX;
        g.drawString(font, label, x, y, on ? color : 0xFF555555, true);
        return x - 8;
    }

    private static int estimateKmh(Entity e) {
        double dx = e.getX() - e.xo, dz = e.getZ() - e.zo;
        return (int) Math.round(Math.sqrt(dx * dx + dz * dz) * 72.0D);
    }

    /** Needle swinging over a 270 degree dial; value 0..1, starting bottom-left, clockwise. */
    private static void needle(GuiGraphics g, float cx, float cy, float len, float value, int color) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 100);
        g.pose().mulPose(Axis.ZP.rotationDegrees(-135.0F + 270.0F * value));
        g.pose().scale(0.5F, 0.5F, 1.0F);
        int l = (int) (len * 2);
        g.fill(-2, -l, 2, 6, color);
        g.fill(-1, -l, 1, -l + 8, 0xFFFFFFFF);
        g.pose().popPose();
        g.fill((int) cx - 3, (int) cy - 3, (int) cx + 3, (int) cy + 3, 0xFF303238);
    }
}
