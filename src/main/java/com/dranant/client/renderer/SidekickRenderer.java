package com.dranant.client.renderer;

import com.dranant.config.DrAnantConfig;
import com.dranant.entity.SidekickEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * The sidekick uses the real Minecraft player model with his owner's player skin (Steve if unavailable or disabled in the
 * config) and wears an HD ninja outfit on top. He aims his blaster when shooting.
 */
public class SidekickRenderer extends HumanoidMobRenderer<SidekickEntity, PlayerModel<SidekickEntity>> {
    private static final ResourceLocation STEVE = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    public SidekickRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        addLayer(new NinjaOutfitLayer(this, context.getModelSet()));
    }

    @Override
    public void render(SidekickEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
        PlayerModel<SidekickEntity> model = getModel();
        model.rightArmPose = entity.isShooting() ? HumanoidModel.ArmPose.BOW_AND_ARROW : HumanoidModel.ArmPose.ITEM;
        model.leftArmPose = entity.isShooting() ? HumanoidModel.ArmPose.BOW_AND_ARROW : HumanoidModel.ArmPose.EMPTY;
        super.render(entity, yaw, partialTick, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SidekickEntity entity) {
        if (DrAnantConfig.SIDEKICK_OWNER_SKIN.get() && entity.getOwnerId().isPresent() && Minecraft.getInstance().getConnection() != null) {
            PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(entity.getOwnerId().get());
            if (info != null) return info.getSkin().texture();
        }
        return STEVE;
    }
}
