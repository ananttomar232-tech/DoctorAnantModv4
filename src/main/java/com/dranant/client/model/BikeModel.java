package com.dranant.client.model;

import com.dranant.DoctorAnantMod;
import com.dranant.entity.BikeEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Box model of the Dr. Anant motorbike (front = -Z). Wheels spin with distance travelled, the whole bike leans into turns. */
public class BikeModel extends HierarchicalModel<BikeEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(DoctorAnantMod.id("dr_anant_bike"), "main");

    private final ModelPart root;
    private final ModelPart bike;
    private final ModelPart frontWheel;
    private final ModelPart rearWheel;

    public BikeModel(ModelPart root) {
        this.root = root;
        this.bike = root.getChild("bike");
        this.frontWheel = bike.getChild("front_wheel");
        this.rearWheel = bike.getChild("rear_wheel");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bike = root.addOrReplaceChild("bike", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-2.0F, -10.0F, -9.0F, 4.0F, 3.0F, 18.0F)   // frame
                .texOffs(48, 24).addBox(-3.0F, -9.0F, -4.0F, 6.0F, 5.0F, 8.0F)    // engine
                .texOffs(0, 48).addBox(-3.0F, -14.0F, -6.0F, 6.0F, 4.0F, 7.0F)    // tank
                .texOffs(32, 48).addBox(-3.0F, -13.0F, 1.0F, 6.0F, 2.0F, 9.0F)    // seat (2 riders)
                .texOffs(64, 48).addBox(-1.0F, -17.0F, -12.0F, 2.0F, 11.0F, 2.0F) // front fork
                .texOffs(56, 0).addBox(-6.0F, -18.0F, -12.0F, 12.0F, 1.0F, 1.0F)  // handlebar
                .texOffs(80, 48).addBox(-1.5F, -15.0F, -14.0F, 3.0F, 3.0F, 2.0F)  // headlight
                .texOffs(0, 64).addBox(-2.0F, -11.0F, 8.0F, 4.0F, 1.0F, 6.0F)     // rear fender
                .texOffs(24, 64).addBox(3.0F, -8.0F, 1.0F, 1.0F, 1.0F, 9.0F),     // exhaust
                PartPose.offset(0.0F, 24.0F, 0.0F));
        CubeListBuilder wheel = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.0F, -6.0F, -4.0F, 2.0F, 12.0F, 8.0F)
                .texOffs(24, 0).addBox(-1.0F, -4.0F, -6.0F, 2.0F, 8.0F, 12.0F);
        bike.addOrReplaceChild("front_wheel", wheel, PartPose.offset(0.0F, -6.0F, -11.0F));
        bike.addOrReplaceChild("rear_wheel", wheel, PartPose.offset(0.0F, -6.0F, 11.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(BikeEntity entity, float wheelAngle, float lean, float ageInTicks, float netHeadYaw, float headPitch) {
        frontWheel.xRot = -wheelAngle;
        rearWheel.xRot = -wheelAngle;
        bike.zRot = lean;
    }
}
