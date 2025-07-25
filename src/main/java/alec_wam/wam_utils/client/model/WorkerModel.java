package alec_wam.wam_utils.client.model;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.client.render.entities.WorkerRenderState;
import net.minecraft.Util;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;

public class WorkerModel extends HumanoidModel<WorkerRenderState> {
    private static final String LEFT_SLEEVE = "left_sleeve";
    private static final String RIGHT_SLEEVE = "right_sleeve";
    private static final String LEFT_PANTS = "left_pants";
    private static final String RIGHT_PANTS = "right_pants";
    private final List<ModelPart> bodyParts;
    public final ModelPart leftSleeve;
    public final ModelPart rightSleeve;
    public final ModelPart leftPants;
    public final ModelPart rightPants;
    public final ModelPart jacket;
    private final boolean slim;

    public WorkerModel(ModelPart root, boolean slim) {
        super(root, RenderType::entityTranslucent);
        this.slim = slim;
        this.leftSleeve = this.leftArm.getChild(LEFT_SLEEVE);
        this.rightSleeve = this.rightArm.getChild(RIGHT_SLEEVE);
        this.leftPants = this.leftLeg.getChild(LEFT_PANTS);
        this.rightPants = this.rightLeg.getChild(RIGHT_PANTS);
        this.jacket = this.body.getChild("jacket");
        this.bodyParts = List.of(this.head, this.body, this.leftArm, this.rightArm, this.leftLeg, this.rightLeg);
    }

    public static MeshDefinition createMesh(CubeDeformation cubeDeformation, boolean slim) {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(cubeDeformation, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();
        if (slim) {
            PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, cubeDeformation),
                PartPose.offset(5.0F, 2.0F, 0.0F)
            );
            PartDefinition partdefinition2 = partdefinition.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, cubeDeformation),
                PartPose.offset(-5.0F, 2.0F, 0.0F)
            );
            partdefinition1.addOrReplaceChild(
                LEFT_SLEEVE, CubeListBuilder.create().texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.ZERO
            );
            partdefinition2.addOrReplaceChild(
                RIGHT_SLEEVE, CubeListBuilder.create().texOffs(40, 32).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.ZERO
            );
        } else {
            PartDefinition partdefinition4 = partdefinition.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation),
                PartPose.offset(5.0F, 2.0F, 0.0F)
            );
            PartDefinition partdefinition6 = partdefinition.getChild("right_arm");
            partdefinition4.addOrReplaceChild(
                LEFT_SLEEVE, CubeListBuilder.create().texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.ZERO
            );
            partdefinition6.addOrReplaceChild(
                RIGHT_SLEEVE, CubeListBuilder.create().texOffs(40, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.ZERO
            );
        }

        PartDefinition partdefinition5 = partdefinition.addOrReplaceChild(
            "left_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation), PartPose.offset(1.9F, 12.0F, 0.0F)
        );
        PartDefinition partdefinition7 = partdefinition.getChild("right_leg");
        partdefinition5.addOrReplaceChild(
            LEFT_PANTS, CubeListBuilder.create().texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.ZERO
        );
        partdefinition7.addOrReplaceChild(
            RIGHT_PANTS, CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.ZERO
        );
        PartDefinition partdefinition3 = partdefinition.getChild("body");
        partdefinition3.addOrReplaceChild(
            "jacket", CubeListBuilder.create().texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.ZERO
        );
        return meshdefinition;
    }

    public void setupAnim(WorkerRenderState p_365286_) {
        boolean flag = !p_365286_.isSpectator;
        this.body.visible = flag;
        this.rightArm.visible = flag;
        this.leftArm.visible = flag;
        this.rightLeg.visible = flag;
        this.leftLeg.visible = flag;
        this.hat.visible = p_365286_.showHat;
        this.jacket.visible = p_365286_.showJacket;
        this.leftPants.visible = p_365286_.showLeftPants;
        this.rightPants.visible = p_365286_.showRightPants;
        this.leftSleeve.visible = p_365286_.showLeftSleeve;
        this.rightSleeve.visible = p_365286_.showRightSleeve;
        super.setupAnim(p_365286_);
    }

    @Override
    public void setAllVisible(boolean visible) {
        super.setAllVisible(visible);
        this.leftSleeve.visible = visible;
        this.rightSleeve.visible = visible;
        this.leftPants.visible = visible;
        this.rightPants.visible = visible;
        this.jacket.visible = visible;
    }

    @Override
    public void translateToHand(HumanoidArm side, PoseStack poseStack) {
        this.root().translateAndRotate(poseStack);
        ModelPart modelpart = this.getArm(side);
        if (this.slim) {
            float f = 0.5F * (side == HumanoidArm.RIGHT ? 1 : -1);
            modelpart.x += f;
            modelpart.translateAndRotate(poseStack);
            modelpart.x -= f;
        } else {
            modelpart.translateAndRotate(poseStack);
        }
    }

    public ModelPart getRandomBodyPart(RandomSource random) {
        return Util.getRandom(this.bodyParts, random);
    }
}
