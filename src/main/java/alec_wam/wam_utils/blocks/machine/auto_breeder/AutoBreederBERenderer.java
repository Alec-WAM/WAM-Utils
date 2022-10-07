package alec_wam.wam_utils.blocks.machine.auto_breeder;

import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class AutoBreederBERenderer implements BlockEntityRenderer<AutoBreederBE> {
	public AutoBreederBERenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AutoBreederBE breeder, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
    	if(breeder.showBoundingBox()) {
	    	poseStack.pushPose();	
	        BlockPos pos = breeder.getBlockPos();
	        poseStack.translate(-pos.getX(), -pos.getY(), -pos.getZ());
	        
	        AABB aabb = breeder.getRangeBB();
	        LevelRenderer.renderLineBox(poseStack, bufferSource.getBuffer(RenderType.lines()), aabb, 1.0F, 0.0F, 0.9F, 1.0F); //Pink
	        poseStack.popPose();
    	}
    }

    public static void register() {
        BlockEntityRenderers.register(BlockInit.AUTO_BREEDER_BE.get(), AutoBreederBERenderer::new);
    }

}

