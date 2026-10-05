package com.dranant.client;

import com.dranant.DoctorAnantMod;
import com.dranant.client.model.MutantBloodBeastModel;
import com.dranant.client.renderer.AssistantRenderer;
import com.dranant.client.renderer.ItemDisplayPedestalRenderer;
import com.dranant.client.renderer.MutantBloodBeastRenderer;
import com.dranant.client.renderer.PatientVillagerRenderer;
import com.dranant.client.renderer.BikeRenderer;
import com.dranant.client.renderer.SidekickRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import com.dranant.registry.ModBlockEntities;
import com.dranant.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Client-only registration: entity + block-entity renderers, model layers and the Diamond Counter HUD. */
@EventBusSubscriber(modid = DoctorAnantMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MUTANT_BLOOD_BEAST.get(), MutantBloodBeastRenderer::new);
        event.registerEntityRenderer(ModEntities.ASSISTANT_PHARMACIST.get(),
                ctx -> new AssistantRenderer<>(ctx, DoctorAnantMod.id("textures/entity/assistant_pharmacist.png")));
        event.registerEntityRenderer(ModEntities.ASSISTANT_SCIENTIST.get(),
                ctx -> new AssistantRenderer<>(ctx, DoctorAnantMod.id("textures/entity/assistant_scientist.png")));
        event.registerEntityRenderer(ModEntities.SHOPKEEPER.get(),
                ctx -> new AssistantRenderer<>(ctx, DoctorAnantMod.id("textures/entity/shopkeeper.png")));
        event.registerEntityRenderer(ModEntities.PATIENT_VILLAGER.get(), PatientVillagerRenderer::new);
        event.registerEntityRenderer(ModEntities.SIDEKICK.get(), SidekickRenderer::new);
        event.registerEntityRenderer(ModEntities.SIDEKICK_BULLET.get(), ctx -> new ThrownItemRenderer<>(ctx, 0.6F, true));
        event.registerEntityRenderer(ModEntities.DR_ANANT_BIKE.get(), BikeRenderer::new);
        event.registerEntityRenderer(ModEntities.BLOOD_BOULDER.get(), ctx -> new ThrownItemRenderer<>(ctx, 3.0F, true));
        event.registerEntityRenderer(ModEntities.BLOOD_ORB.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.2F, true));
        event.registerEntityRenderer(ModEntities.SEAT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.SUPERCAR.get(), com.dranant.client.renderer.SupercarRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ITEM_DISPLAY_PEDESTAL.get(), ItemDisplayPedestalRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MutantBloodBeastModel.LAYER, MutantBloodBeastModel::createBodyLayer);
        event.registerLayerDefinition(com.dranant.client.model.BikeModel.LAYER, com.dranant.client.model.BikeModel::createLayer);
        event.registerLayerDefinition(com.dranant.client.model.SupercarModel.LAYER, com.dranant.client.model.SupercarModel::createLayer);
        event.registerLayerDefinition(com.dranant.client.renderer.NinjaOutfitLayer.LAYER, com.dranant.client.renderer.NinjaOutfitLayer::createLayer);
    }

    @SubscribeEvent
    public static void registerKeys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        VehicleKeys.ALL.forEach(event::register);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(DoctorAnantMod.id("diamond_tracker"), new DiamondTrackerOverlay());
        event.registerAboveAll(DoctorAnantMod.id("vehicle_hud"), new VehicleHudOverlay());
    }

    private ClientModEvents() {}
}
