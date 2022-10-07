package alec_wam.wam_utils.blocks.machine.quarry;

import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;

public class QuarryBERenderer implements BlockEntityRenderer<QuarryBE> {
	public QuarryBERenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(QuarryBE quarry, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {

         // Always remember to push the current transformation so that you can restore it later
        poseStack.pushPose();	
        BlockPos pos = quarry.getBlockPos();
        poseStack.translate(-pos.getX(), -pos.getY(), -pos.getZ());
        
        Direction dir = quarry.getBlockState().getValue(QuarryBlock.FACING).getOpposite();
        Vec3i maxRangePos = quarry.getMaxRangeOffset();        
		BlockPos back = quarry.getBlockPos().relative(dir);
        AABB aabb = new AABB(back).expandTowards(maxRangePos.getX(), 0, maxRangePos.getZ());
        LevelRenderer.renderLineBox(poseStack, bufferSource.getBuffer(RenderType.lines()), aabb, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    public static void register() {
        BlockEntityRenderers.register(BlockInit.QUARRY_BE.get(), QuarryBERenderer::new);
    }

}

