package com.dranant.client.renderer;

import com.dranant.DoctorAnantMod;
import com.dranant.entity.SidekickEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.resources.ResourceLocation;

/**
 * HD ninja outfit (4x texture) worn over the sidekick's player skin: hood with red headband and eye slit, red-trimmed gi,
 * sash with kunai, katana across the back, bandage-wrapped forearms and shins, tabi boots.
 */
public class NinjaOutfitLayer extends RenderLayer<SidekickEntity, PlayerModel<SidekickEntity>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(DoctorAnantMod.id("ninja_outfit"), "main");
    private static final ResourceLocation TEXTURE = DoctorAnantMod.id("textures/entity/ninja_outfit.png");
    private final HumanoidModel<SidekickEntity> outfit;

    public NinjaOutfitLayer(RenderLayerParent<SidekickEntity, PlayerModel<SidekickEntity>> parent, EntityModelSet models) {
        super(parent);
        this.outfit = new HumanoidModel<>(models.bakeLayer(LAYER));
    }

    public static LayerDefinition createLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.27F), 0.0F), 64, 32);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, SidekickEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;
        getParentModel().copyPropertiesTo(outfit);
        outfit.setAllVisible(true);
        renderColoredCutoutModel(outfit, TEXTURE, poseStack, buffer, light, entity, -1);
    }
}
