package com.dranant.client.renderer;

import com.dranant.DoctorAnantMod;
import com.dranant.client.model.BikeModel;
import com.dranant.entity.BikeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class BikeRenderer extends EntityRenderer<BikeEntity> {
    private static final ResourceLocation TEXTURE = DoctorAnantMod.id("textures/entity/dr_anant_bike.png");
    private final BikeModel model;

    public BikeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new BikeModel(context.bakeLayer(BikeModel.LAYER));
        this.shadowRadius = 0.6F;
    }

    @Override
    public void render(BikeEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getViewYRot(partialTick)));
        float wheelie = Mth.lerp(partialTick, entity.wheelieO, entity.wheelie);
        if (wheelie > 0.001F) {   // lift the front wheel, pivoting on the rear axle
            poseStack.translate(0.0F, 0.375F, 0.69F);
            poseStack.mulPose(Axis.XP.rotation(wheelie));
            poseStack.translate(0.0F, -0.375F, -0.69F);
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        model.setupAnim(entity, Mth.lerp(partialTick, entity.wheelRotO, entity.wheelRot), entity.lean, entity.tickCount + partialTick, 0.0F, 0.0F);
        model.renderToBuffer(poseStack, buffer.getBuffer(model.renderType(TEXTURE)), light, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(BikeEntity entity) {
        return TEXTURE;
    }
}
