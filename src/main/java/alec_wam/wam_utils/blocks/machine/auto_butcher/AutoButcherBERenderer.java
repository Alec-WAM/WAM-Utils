package alec_wam.wam_utils.blocks.machine.auto_butcher;

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

public class AutoButcherBERenderer implements BlockEntityRenderer<AutoButcherBE> {
	public AutoButcherBERenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AutoButcherBE butcher, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
    	if(butcher.showBoundingBox()) {
	    	poseStack.pushPose();	
	        BlockPos pos = butcher.getBlockPos();
	        poseStack.translate(-pos.getX(), -pos.getY(), -pos.getZ());
	        
	        AABB aabb = butcher.getRangeBB();
	        LevelRenderer.renderLineBox(poseStack, bufferSource.getBuffer(RenderType.lines()), aabb, 1.0F, 0.0F, 0.0F, 1.0F); //Red
	        poseStack.popPose();
    	}
    }

    public static void register() {
        BlockEntityRenderers.register(BlockInit.AUTO_BUTCHER_BE.get(), AutoButcherBERenderer::new);
    }

}

