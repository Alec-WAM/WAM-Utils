package alec_wam.wam_utils.blocks.entity_pod.piglin;

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
import net.minecraft.world.entity.monster.piglin.Piglin;

public class PiglinTradingPodRenderer implements BlockEntityRenderer<PiglinTradingPodBE> {
	private final EntityRenderDispatcher entityRenderer;
	
    public PiglinTradingPodRenderer(BlockEntityRendererProvider.Context context) {
    	this.entityRenderer = context.getEntityRenderer();
    }

    @Override
    public void render(PiglinTradingPodBE pod, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {

         // Always remember to push the current transformation so that you can restore it later
        poseStack.pushPose();

        poseStack.translate(0.5D, 0.05D, 0.5D);
        Piglin entity = pod.getTradeEntity();
        if (entity != null) {        	
        	Direction dir = pod.getBlockState().getValue(AbstractEntityPodBlock.FACING);
        	float f = -dir.toYRot();
        	poseStack.mulPose(Vector3f.YP.rotationDegrees(f));
        	
        	double offset = 0.15D;
        	poseStack.translate(0, 0, -offset);
        	float width = 0.8F;
        	float height = 0.8F;
        	poseStack.scale(width, height, width);        	
        	this.entityRenderer.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, poseStack, bufferSource, combinedLight);
        }
        poseStack.popPose();
    }

    public static void register() {
        BlockEntityRenderers.register(BlockInit.PIGLIN_TRADING_POD_BE.get(), PiglinTradingPodRenderer::new);
    }

}

