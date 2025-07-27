package alec_wam.wam_utils.client.render.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import alec_wam.wam_utils.client.ModClientInit;
import alec_wam.wam_utils.client.model.WorkerModel;
import alec_wam.wam_utils.client.util.RenderHelper;
import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.helpers.EntityHelper;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class WorkerEntityRenderer extends MobRenderer<WorkerEntity, WorkerRenderState, WorkerModel> {
    
    private static final ResourceLocation FISHING_BOBBER_TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/fishing_hook.png");
    private static final RenderType FISHING_BOBBER_RENDER_TYPE = RenderType.entityCutout(FISHING_BOBBER_TEXTURE_LOCATION);
    
    private final WorkerModel normalModel;
    private final WorkerModel slimModel;

    public WorkerEntityRenderer(EntityRendererProvider.Context context, WorkerModel normalModel, WorkerModel slimModel) {
        super(context, normalModel, 0.25F);
        this.normalModel = normalModel;
        this.slimModel = slimModel;
        this.addLayer(
            new HumanoidArmorLayer<>(
                this,
                new HumanoidArmorModel<>(context.bakeLayer(ModClientInit.WORKER_MODEL_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModClientInit.WORKER_MODEL_OUTER_ARMOR)),
                context.getEquipmentRenderer()
            )
        );
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), CustomHeadLayer.Transforms.DEFAULT));
        this.addLayer(new WingsLayer<>(this, context.getModelSet(), context.getEquipmentRenderer()));
        this.addLayer(new ItemInHandLayer<>(this));
        // TODO Fix some of these layers
        //this.addLayer(new ArrowLayer<>(this, context));
//        this.addLayer(new Deadmau5EarsLayer(this, context.getModelSet()));
        // this.addLayer(new CapeLayer(this, context.getModelSet(), context.getEquipmentAssets()));
        // this.addLayer(new SpinAttackEffectLayer(this, context.getModelSet()));
        // this.addLayer(new BeeStingerLayer<>(this, context));
    }
	   
	public static void register() {
		EntityRenderers.register(ModInit.WORKER_ENTITY.get(), (context) -> {
            WorkerModel normalModel = new WorkerModel(context.bakeLayer(ModClientInit.WORKER_MODEL), false);
            WorkerModel slimModel = new WorkerModel(context.bakeLayer(ModClientInit.WORKER_SLIM_MODEL), true);
            return new WorkerEntityRenderer(context, normalModel, slimModel);
        });
	}

	@Override
    public Vec3 getRenderOffset(WorkerRenderState p_360756_) {
        Vec3 vec3 = super.getRenderOffset(p_360756_);
        return p_360756_.isCrouching ? vec3.add(0.0, p_360756_.scale * -2.0F / 16.0, 0.0) : vec3;
    }

	@Override
	public WorkerRenderState createRenderState() {
		return new WorkerRenderState();
	}

	@Override
	public ResourceLocation getTextureLocation(WorkerRenderState renderState) {
		return renderState.skin.texture();
	}
	
	@Override
	protected void renderNameTag(WorkerRenderState p_363185_, Component p_117809_, PoseStack p_117810_, MultiBufferSource p_117811_, int p_117812_) {
		
	}

    private static HumanoidModel.ArmPose getArmPose(WorkerEntity worker, HumanoidArm arm) {
        ItemStack itemstack = worker.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack itemstack1 = worker.getItemInHand(InteractionHand.OFF_HAND);
        HumanoidModel.ArmPose humanoidmodel$armpose = getArmPose(worker, itemstack, InteractionHand.MAIN_HAND);
        HumanoidModel.ArmPose humanoidmodel$armpose1 = getArmPose(worker, itemstack1, InteractionHand.OFF_HAND);
        if (humanoidmodel$armpose.isTwoHanded()) {
            humanoidmodel$armpose1 = itemstack1.isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        }

        return worker.getMainArm() == arm ? humanoidmodel$armpose : humanoidmodel$armpose1;
    }

    private static HumanoidModel.ArmPose getArmPose(WorkerEntity worker, ItemStack stack, InteractionHand hand) {
        var extensions = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(stack);
        var armPose = extensions.getArmPose(worker, hand, stack);
        if (armPose != null) {
            return armPose;
        }
        if (stack.isEmpty()) {
            return HumanoidModel.ArmPose.EMPTY;
        } else if (!worker.swinging && stack.is(Items.CROSSBOW) && CrossbowItem.isCharged(stack)) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        } else {
            if (worker.getUsedItemHand() == hand && worker.getUseItemRemainingTicks() > 0) {
                ItemUseAnimation itemuseanimation = stack.getUseAnimation();
                if (itemuseanimation == ItemUseAnimation.BLOCK) {
                    return HumanoidModel.ArmPose.BLOCK;
                }

                if (itemuseanimation == ItemUseAnimation.BOW) {
                    return HumanoidModel.ArmPose.BOW_AND_ARROW;
                }

                if (itemuseanimation == ItemUseAnimation.SPEAR) {
                    return HumanoidModel.ArmPose.THROW_SPEAR;
                }

                if (itemuseanimation == ItemUseAnimation.CROSSBOW) {
                    return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
                }

                if (itemuseanimation == ItemUseAnimation.SPYGLASS) {
                    return HumanoidModel.ArmPose.SPYGLASS;
                }

                if (itemuseanimation == ItemUseAnimation.TOOT_HORN) {
                    return HumanoidModel.ArmPose.TOOT_HORN;
                }

                if (itemuseanimation == ItemUseAnimation.BRUSH) {
                    return HumanoidModel.ArmPose.BRUSH;
                }
            }

            return HumanoidModel.ArmPose.ITEM;
        }
    }

	@Override
    public void extractRenderState(WorkerEntity worker, WorkerRenderState renderState, float partialTick) {
        super.extractRenderState(worker, renderState, partialTick);
        
        HumanoidMobRenderer.extractHumanoidRenderState(worker, renderState, partialTick, this.itemModelResolver);
        renderState.leftArmPose = getArmPose(worker, HumanoidArm.LEFT);
        renderState.rightArmPose = getArmPose(worker, HumanoidArm.RIGHT);
        
        renderState.isBaby = true;
        renderState.ageScale = 0.5F;
        renderState.skin = worker.getSkin();
        renderState.arrowCount = worker.getArrowCount();
        renderState.stingerCount = worker.getStingerCount();
        renderState.useItemRemainingTicks = worker.getUseItemRemainingTicks();
        renderState.swinging = worker.swinging;
        renderState.isSpectator = worker.isSpectator();
        renderState.showHat = worker.isModelPartShown(PlayerModelPart.HAT);
        renderState.showJacket = worker.isModelPartShown(PlayerModelPart.JACKET);
        renderState.showLeftPants = worker.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
        renderState.showRightPants = worker.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
        renderState.showLeftSleeve = worker.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
        renderState.showRightSleeve = worker.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
        renderState.showCape = worker.isModelPartShown(PlayerModelPart.CAPE);
        extractFlightData(worker, renderState, partialTick);
        extractCapeState(worker, renderState, partialTick);
//        if (p_360583_.distanceToCameraSq < 100.0) {
//            Scoreboard scoreboard = p_361478_.getScoreboard();
//            Objective objective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
//            if (objective != null) {
//                ReadOnlyScoreInfo readonlyscoreinfo = scoreboard.getPlayerScoreInfo(p_361478_, objective);
//                Component component = ReadOnlyScoreInfo.safeFormatValue(readonlyscoreinfo, objective.numberFormatOrDefault(StyledFormat.NO_STYLE));
//                p_360583_.scoreText = Component.empty().append(component).append(CommonComponents.SPACE).append(objective.getDisplayName());
//            } else {
//                p_360583_.scoreText = null;
//            }
//        } else {
            renderState.scoreText = null;
//        }

        renderState.parrotOnLeftShoulder = null;
        renderState.parrotOnRightShoulder = null;
        renderState.id = worker.getId();
        if(worker.hasCustomName()) {
        	renderState.name = worker.getName().getString();
        }
        else {
        	renderState.name = null;
        }
        renderState.heldOnHead.clear();
        if (renderState.isUsingItem) {
            ItemStack itemstack = worker.getItemInHand(renderState.useItemHand);
            if (itemstack.is(Items.SPYGLASS)) {
                this.itemModelResolver.updateForLiving(renderState.heldOnHead, itemstack, ItemDisplayContext.HEAD, worker);
            }
        }

        renderState.isFishing = worker.isFishing();
        renderState.bobberPosition = worker.currentBobberPosition();
        renderState.fishingRodLocation = EntityHelper.getEntityThirdPersonHandPos(worker, worker.getMainArm(), 0.5F, partialTick);

        if(worker.isFishing()){
            Vec3 vec3 = renderState.fishingRodLocation;
            Vec3 vec31 = renderState.bobberPosition.add(0.0, 0.25, 0.0);
            renderState.fishingLineOriginOffset = vec3.subtract(vec31);
        }
        else {
            renderState.fishingLineOriginOffset = Vec3.ZERO;
        }
    }

    private static void extractFlightData(WorkerEntity worker, WorkerRenderState renderState, float partialTick) {
        renderState.fallFlyingTimeInTicks = worker.getFallFlyingTicks() + partialTick;
        Vec3 vec3 = worker.getViewVector(partialTick);
        Vec3 vec31 = worker.getDeltaMovementLerped(partialTick);
        if (vec31.horizontalDistanceSqr() > 1.0E-5F && vec3.horizontalDistanceSqr() > 1.0E-5F) {
            renderState.shouldApplyFlyingYRot = true;
            double d0 = vec31.horizontal().normalize().dot(vec3.horizontal().normalize());
            double d1 = vec31.x * vec3.z - vec31.z * vec3.x;
            renderState.flyingYRot = (float)(Math.signum(d1) * Math.acos(Math.min(1.0, Math.abs(d0))));
        } else {
            renderState.shouldApplyFlyingYRot = false;
            renderState.flyingYRot = 0.0F;
        }
    }

    private static void extractCapeState(WorkerEntity worker, WorkerRenderState renderState, float partialTick) {
//        double d0 = Mth.lerp((double)partialTick, worker.xCloakO, worker.xCloak) - Mth.lerp((double)partialTick, worker.xo, worker.getX());
//        double d1 = Mth.lerp((double)partialTick, worker.yCloakO, worker.yCloak) - Mth.lerp((double)partialTick, worker.yo, worker.getY());
//        double d2 = Mth.lerp((double)partialTick, worker.zCloakO, worker.zCloak) - Mth.lerp((double)partialTick, worker.zo, worker.getZ());
//        float f = Mth.rotLerp(partialTick, worker.yBodyRotO, worker.yBodyRot);
//        double d3 = Mth.sin(f * (float) (Math.PI / 180.0));
//        double d4 = -Mth.cos(f * (float) (Math.PI / 180.0));
//        renderState.capeFlap = (float)d1 * 10.0F;
//        renderState.capeFlap = Mth.clamp(renderState.capeFlap, -6.0F, 32.0F);
//        renderState.capeLean = (float)(d0 * d3 + d2 * d4) * 100.0F;
//        renderState.capeLean = renderState.capeLean * (1.0F - renderState.fallFlyingScale());
//        renderState.capeLean = Mth.clamp(renderState.capeLean, 0.0F, 150.0F);
//        renderState.capeLean2 = (float)(d0 * d4 - d2 * d3) * 100.0F;
//        renderState.capeLean2 = Mth.clamp(renderState.capeLean2, -20.0F, 20.0F);
//        float f1 = Mth.lerp(partialTick, worker.oBob, worker.bob);
//        float f2 = Mth.lerp(partialTick, worker.walkDistO, worker.walkDist);
//        renderState.capeFlap = renderState.capeFlap + Mth.sin(f2 * 6.0F) * 32.0F * f1;
    }

    @Override
    protected void setupRotations(WorkerRenderState p_363355_, PoseStack p_117803_, float p_117804_, float p_117805_) {
        float f = p_363355_.swimAmount;
        float f1 = p_363355_.xRot;
        if (p_363355_.isFallFlying) {
            super.setupRotations(p_363355_, p_117803_, p_117804_, p_117805_);
            float f2 = p_363355_.fallFlyingScale();
            if (!p_363355_.isAutoSpinAttack) {
                p_117803_.mulPose(Axis.XP.rotationDegrees(f2 * (-90.0F - f1)));
            }

            if (p_363355_.shouldApplyFlyingYRot) {
                p_117803_.mulPose(Axis.YP.rotation(p_363355_.flyingYRot));
            }
        } else if (f > 0.0F) {
            super.setupRotations(p_363355_, p_117803_, p_117804_, p_117805_);
            float f4 = p_363355_.isInWater ? -90.0F - f1 : -90.0F;
            float f3 = Mth.lerp(f, 0.0F, f4);
            p_117803_.mulPose(Axis.XP.rotationDegrees(f3));
            if (p_363355_.isVisuallySwimming) {
                p_117803_.translate(0.0F, -1.0F, 0.3F);
            }
        } else {
            super.setupRotations(p_363355_, p_117803_, p_117804_, p_117805_);
        }
    }

    @Override
    public void render(WorkerRenderState renderState, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        this.model = (renderState.skin == null || renderState.skin.model() == PlayerSkin.Model.WIDE) ? this.normalModel : this.slimModel;
        super.render(renderState, poseStack, bufferSource, packedLight);

        if(renderState.isFishing){
            Vec3 bobber = renderState.bobberPosition.subtract(renderState.x, renderState.y, renderState.z);

            poseStack.pushPose();
            poseStack.pushPose();
            poseStack.translate(bobber);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            PoseStack.Pose posestack$pose = poseStack.last();
            VertexConsumer vertexconsumer = bufferSource.getBuffer(FISHING_BOBBER_RENDER_TYPE);
            bobberVertex(vertexconsumer, posestack$pose, packedLight, 0.0F, 0, 0, 1);
            bobberVertex(vertexconsumer, posestack$pose, packedLight, 1.0F, 0, 1, 1);
            bobberVertex(vertexconsumer, posestack$pose, packedLight, 1.0F, 1, 1, 0);
            bobberVertex(vertexconsumer, posestack$pose, packedLight, 0.0F, 1, 0, 0);
            poseStack.popPose();
            
            poseStack.translate(bobber.subtract(0.0, 0.15, 0.0));
            
            float f = (float)renderState.fishingLineOriginOffset.x;
            float f1 = (float)renderState.fishingLineOriginOffset.y;
            float f2 = (float)renderState.fishingLineOriginOffset.z;
            VertexConsumer vertexconsumer1 = bufferSource.getBuffer(RenderType.lineStrip());
            PoseStack.Pose posestack$pose1 = poseStack.last();
            int segments = 16;

            for (int j = 0; j <= segments; j++) {
                RenderHelper.stringVertex(f, f1, f2, vertexconsumer1, posestack$pose1, (float)j / segments, (float)(j + 1) / segments);
            }

            poseStack.popPose();
        
        }
    }

    private static void bobberVertex(VertexConsumer consumer, PoseStack.Pose pose, int packedLight, float x, int y, int u, int v) {
        consumer.addVertex(pose, x - 0.5F, y - 0.5F, 0.0F)
            .setColor(-1)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

}
