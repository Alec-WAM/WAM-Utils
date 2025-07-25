package alec_wam.wam_utils.client.render.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import alec_wam.wam_utils.client.ModClientInit;
import alec_wam.wam_utils.client.model.WorkerModel;
import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.BeeStingerLayer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.SpinAttackEffectLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WorkerEntityRenderer extends LivingEntityRenderer<WorkerEntity, PlayerRenderState, PlayerModel> {
    
    public WorkerEntityRenderer(EntityRendererProvider.Context context, boolean useSlimModel) {
        super(context, new WorkerModel(context.bakeLayer(useSlimModel ? ModClientInit.WORKER_SLIM_MODEL : ModClientInit.WORKER_MODEL), useSlimModel), 0.25F);
        this.addLayer(
            new HumanoidArmorLayer<>(
                this,
                new HumanoidArmorModel<>(context.bakeLayer(useSlimModel ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(useSlimModel ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getEquipmentRenderer()
            )
        );
        this.addLayer(new PlayerItemInHandLayer<>(this));
        this.addLayer(new ArrowLayer<>(this, context));
//        this.addLayer(new Deadmau5EarsLayer(this, context.getModelSet()));
        this.addLayer(new CapeLayer(this, context.getModelSet(), context.getEquipmentAssets()));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet()));
        this.addLayer(new WingsLayer<>(this, context.getModelSet(), context.getEquipmentRenderer()));
        this.addLayer(new ParrotOnShoulderLayer(this, context.getModelSet()));
        this.addLayer(new SpinAttackEffectLayer(this, context.getModelSet()));
        this.addLayer(new BeeStingerLayer<>(this, context));
    }
	   
	public static void register() {
		EntityRenderers.register(ModInit.WORKER_ENTITY.get(), (context) -> new WorkerEntityRenderer(context, false));
	}

	@Override
    public Vec3 getRenderOffset(PlayerRenderState p_360756_) {
        Vec3 vec3 = super.getRenderOffset(p_360756_);
        return p_360756_.isCrouching ? vec3.add(0.0, p_360756_.scale * -2.0F / 16.0, 0.0) : vec3;
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
	public PlayerRenderState createRenderState() {
		return new PlayerRenderState();
	}

	@Override
	public ResourceLocation getTextureLocation(PlayerRenderState renderState) {
		return renderState.skin.texture();
	}
	
	@Override
	protected void renderNameTag(PlayerRenderState p_363185_, Component p_117809_, PoseStack p_117810_, MultiBufferSource p_117811_, int p_117812_) {
		
	}

	@Override
    public void extractRenderState(WorkerEntity p_361478_, PlayerRenderState p_360583_, float p_364121_) {
        super.extractRenderState(p_361478_, p_360583_, p_364121_);
        HumanoidMobRenderer.extractHumanoidRenderState(p_361478_, p_360583_, p_364121_, this.itemModelResolver);
        p_360583_.isBaby = true;
        p_360583_.leftArmPose = getArmPose(p_361478_, HumanoidArm.LEFT);
        p_360583_.rightArmPose = getArmPose(p_361478_, HumanoidArm.RIGHT);
        p_360583_.skin = p_361478_.getSkin();
        p_360583_.arrowCount = p_361478_.getArrowCount();
        p_360583_.stingerCount = p_361478_.getStingerCount();
        p_360583_.useItemRemainingTicks = p_361478_.getUseItemRemainingTicks();
        p_360583_.swinging = p_361478_.swinging;
        p_360583_.isSpectator = p_361478_.isSpectator();
        p_360583_.showHat = p_361478_.isModelPartShown(PlayerModelPart.HAT);
        p_360583_.showJacket = p_361478_.isModelPartShown(PlayerModelPart.JACKET);
        p_360583_.showLeftPants = p_361478_.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
        p_360583_.showRightPants = p_361478_.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
        p_360583_.showLeftSleeve = p_361478_.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
        p_360583_.showRightSleeve = p_361478_.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
        p_360583_.showCape = p_361478_.isModelPartShown(PlayerModelPart.CAPE);
        extractFlightData(p_361478_, p_360583_, p_364121_);
        extractCapeState(p_361478_, p_360583_, p_364121_);
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
            p_360583_.scoreText = null;
//        }

        p_360583_.parrotOnLeftShoulder = null;
        p_360583_.parrotOnRightShoulder = null;
        p_360583_.id = p_361478_.getId();
        if(p_361478_.hasCustomName()) {
        	p_360583_.name = p_361478_.getName().getString();
        }
        else {
        	p_360583_.name = null;
        }
        p_360583_.heldOnHead.clear();
        if (p_360583_.isUsingItem) {
            ItemStack itemstack = p_361478_.getItemInHand(p_360583_.useItemHand);
            if (itemstack.is(Items.SPYGLASS)) {
                this.itemModelResolver.updateForLiving(p_360583_.heldOnHead, itemstack, ItemDisplayContext.HEAD, p_361478_);
            }
        }
    }

    private static void extractFlightData(WorkerEntity worker, PlayerRenderState renderState, float partialTick) {
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

    private static void extractCapeState(WorkerEntity worker, PlayerRenderState renderState, float partialTick) {
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
    protected void setupRotations(PlayerRenderState p_363355_, PoseStack p_117803_, float p_117804_, float p_117805_) {
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

}
