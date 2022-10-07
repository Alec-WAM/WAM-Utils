package alec_wam.wam_utils.blocks.xp_vacuum;

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

public class XPVacuumBERenderer implements BlockEntityRenderer<XPVacuumBE> {
	public XPVacuumBERenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(XPVacuumBE vacuum, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
    	if(vacuum.showBoundingBox()) {
	    	poseStack.pushPose();	
	        BlockPos pos = vacuum.getBlockPos();
	        poseStack.translate(-pos.getX(), -pos.getY(), -pos.getZ());
	        
	        AABB aabb = vacuum.getRangeBB();
	        LevelRenderer.renderLineBox(poseStack, bufferSource.getBuffer(RenderType.lines()), aabb, 0.0F, 1.0F, 0.0F, 1.0F);
	        poseStack.popPose();
    	}
    }

    public static void register() {
        BlockEntityRenderers.register(BlockInit.XP_VACUUM_BE.get(), XPVacuumBERenderer::new);
    }

}


