package com.dranant.client;

import com.dranant.entity.BikeEntity;
import com.dranant.entity.SupercarEntity;
import com.dranant.network.CarControlPayload;
import com.dranant.vehicle.VehicleInput;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Vehicle key bindings (rebindable under Controls > "Dr. Anant Vehicles") and the per-tick driver input:
 * SPACE = handbrake / drift, SPRINT = nitro / turbo, H horn, J headlights, U doors, N bonnet, M boot.
 */
@EventBusSubscriber(modid = com.dranant.DoctorAnantMod.MODID, value = Dist.CLIENT)
public final class VehicleKeys {
    private static final String CATEGORY = "key.categories.dranant.vehicles";
    public static final KeyMapping HORN = key("key.dranant.horn", GLFW.GLFW_KEY_H);
    public static final KeyMapping LIGHTS = key("key.dranant.lights", GLFW.GLFW_KEY_J);
    public static final KeyMapping DOORS = key("key.dranant.doors", GLFW.GLFW_KEY_U);
    public static final KeyMapping HOOD = key("key.dranant.hood", GLFW.GLFW_KEY_N);
    public static final KeyMapping TRUNK = key("key.dranant.trunk", GLFW.GLFW_KEY_M);
    public static final List<KeyMapping> ALL = List.of(HORN, LIGHTS, DOORS, HOOD, TRUNK);

    private static KeyMapping key(String name, int code) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            VehicleInput.brake = false;
            VehicleInput.boost = false;
            return;
        }
        Entity vehicle = mc.player.getVehicle();
        boolean driving = (vehicle instanceof SupercarEntity || vehicle instanceof BikeEntity) && vehicle.getControllingPassenger() == mc.player;
        VehicleInput.brake = driving && mc.screen == null && mc.options.keyJump.isDown();
        VehicleInput.boost = driving && mc.screen == null && mc.options.keySprint.isDown();
        while (HORN.consumeClick()) if (vehicle != null) CarControlPayload.send(SupercarEntity.CONTROL_HORN);
        while (LIGHTS.consumeClick()) if (vehicle instanceof SupercarEntity) CarControlPayload.send(SupercarEntity.CONTROL_LIGHTS);
        while (DOORS.consumeClick()) if (vehicle instanceof SupercarEntity) CarControlPayload.send(SupercarEntity.CONTROL_DOORS);
        while (HOOD.consumeClick()) if (vehicle instanceof SupercarEntity) CarControlPayload.send(SupercarEntity.CONTROL_HOOD);
        while (TRUNK.consumeClick()) if (vehicle instanceof SupercarEntity) CarControlPayload.send(SupercarEntity.CONTROL_TRUNK);
    }

    private VehicleKeys() {}
}
