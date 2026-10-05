package com.dranant.client.renderer;

import com.dranant.DoctorAnantMod;
import com.dranant.client.model.SupercarModel;
import com.dranant.entity.SupercarEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Supercar renderer: 4x HD paint (cutout), tinted see-through glass (translucent pass), always-on emissive details
 * (LED daytime running lights, dashboard gauges, ambient strips, tail-light bar) and the headlight layer when the lights are on.
 * The body rolls in corners and pitches under acceleration / braking; crashes shake the car.
 */
public class SupercarRenderer extends EntityRenderer<SupercarEntity> {
    private static final ResourceLocation GLASS = DoctorAnantMod.id("textures/entity/supercar/glass.png");
    private static final ResourceLocation GLOW = DoctorAnantMod.id("textures/entity/supercar/glow.png");
    private static final ResourceLocation LIGHTS = DoctorAnantMod.id("textures/entity/supercar/lights.png");
    private final SupercarModel model;

    public SupercarRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new SupercarModel(context.bakeLayer(SupercarModel.LAYER));
        this.shadowRadius = 1.3F;
        this.shadowStrength = 0.9F;
    }

    @Override
    public void render(SupercarEntity car, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - car.getViewYRot(partialTick)));
        int shake = car.getCrashShake();
        if (shake > 0) {
            float s = shake / 18.0F * 0.06F;
            poseStack.translate(Mth.sin((car.tickCount + partialTick) * 3.1F) * s, Math.abs(Mth.cos((car.tickCount + partialTick) * 2.3F)) * s, 0.0F);
        }
        poseStack.translate(0.0F, 0.35F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotation(car.getBodyRoll(partialTick)));
        poseStack.mulPose(Axis.XP.rotation(car.getBodyPitch(partialTick)));
        poseStack.translate(0.0F, -0.35F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        model.pose(car, partialTick);
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(car.getVariant().texture())), light, OverlayTexture.NO_OVERLAY, -1);
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityTranslucent(GLASS)), light, OverlayTexture.NO_OVERLAY, -1);
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(GLOW)), 0xF000F0, OverlayTexture.NO_OVERLAY, -1);
        if (car.lightsOn()) {
            model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(LIGHTS)), 0xF000F0, OverlayTexture.NO_OVERLAY, -1);
        }
        poseStack.popPose();
        super.render(car, yaw, partialTick, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SupercarEntity car) {
        return car.getVariant().texture();
    }
}
