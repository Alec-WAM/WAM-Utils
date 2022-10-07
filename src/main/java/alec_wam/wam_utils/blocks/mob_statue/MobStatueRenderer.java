package alec_wam.wam_utils.blocks.mob_statue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;

import alec_wam.wam_utils.blocks.entity_pod.AbstractEntityPodBlock;
import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;

public class MobStatueRenderer implements BlockEntityRenderer<MobStatueBE> {
	private final EntityRenderDispatcher entityRenderer;
	
    public MobStatueRenderer(BlockEntityRendererProvider.Context context) {
    	this.entityRenderer = context.getEntityRenderer();
    }

    @Override
    public void render(MobStatueBE statue, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {

         // Always remember to push the current transformation so that you can restore it later
        if(statue.getScale() > 0.0F) {
	    	poseStack.pushPose();
	
	        poseStack.translate(0.5D, 0.125D, 0.5D);
	        LivingEntity entity = statue.getDisplayEntity();
	        if (entity != null) {        	
	        	float f = -statue.getRotation() % 360;
	        	poseStack.mulPose(Vector3f.YP.rotationDegrees(f));
	        	float scale = statue.getScale();
	        	poseStack.scale(scale, scale, scale);   
	        	Boolean oldValue = this.entityRenderer.options.entityShadows().get();
	        	this.entityRenderer.options.entityShadows().set(Boolean.FALSE);
	        	this.entityRenderer.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, poseStack, bufferSource, combinedLight);
	        	this.entityRenderer.options.entityShadows().set(oldValue);
	        }
	        poseStack.popPose();
        }
    }

    public static void register() {
        BlockEntityRenderers.register(BlockInit.MOB_STATUE_BE.get(), MobStatueRenderer::new);
    }

}

