package alec_wam.wam_utils.blocks.shield_rack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;

import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.utils.ItemUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;

public class ShieldRackBERenderer implements BlockEntityRenderer<ShieldRackBE> {
	private final ItemRenderer itemRenderer;
	public ShieldRackBERenderer(BlockEntityRendererProvider.Context context) {
		itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ShieldRackBE rack, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {

         // Always remember to push the current transformation so that you can restore it later
        poseStack.pushPose();	
        Direction rot = rack.getBlockState().getValue(ShieldRackBlock.FACING);
        if(rot == Direction.NORTH){
			poseStack.mulPose(Vector3f.YP.rotationDegrees(180.0F));
			poseStack.translate(-1, 0, -1);
		}
		
		if(rot == Direction.WEST){
			poseStack.mulPose(Vector3f.YP.rotationDegrees(90.0F + 180.0F));
			poseStack.translate(0, 0, -1);
		}
		
		if(rot == Direction.EAST){
			poseStack.mulPose(Vector3f.YP.rotationDegrees(90.0F));
			poseStack.translate(-1, 0, 0);
		}
        boolean hasLeftStack = !rack.getLeftStack().isEmpty();
        boolean hasRightStack = !rack.getRightStack().isEmpty();
        if(!rack.getShieldStack().isEmpty()) {
        	poseStack.pushPose();
        	poseStack.translate(0.5, 0.5, (hasLeftStack && hasRightStack) ? 0.05 : 0);
            poseStack.mulPose(Vector3f.YP.rotationDegrees(180.0F));
        	float scale = 1.8F;
        	poseStack.scale(scale, scale, scale);
        	this.itemRenderer.renderStatic(rack.getShieldStack(), ItemTransforms.TransformType.FIXED, combinedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, 0);
        	poseStack.popPose();
        }
        float weaponScale = 1.0f;
        if(hasLeftStack) {
        	poseStack.pushPose();
        	boolean rotateToHandle = ItemUtils.isSword(rack.getLeftStack());
        	poseStack.translate(rotateToHandle ? 0.35 : 0.25, 0.5, hasRightStack ? 0.15 : 0.1);
            poseStack.mulPose(Vector3f.YP.rotationDegrees(180.0F));
        	
        	poseStack.mulPose(Vector3f.ZP.rotationDegrees(rotateToHandle ? 90.0F : -90.0F));
        	poseStack.scale(weaponScale, weaponScale, weaponScale);
        	this.itemRenderer.renderStatic(rack.getLeftStack(), ItemTransforms.TransformType.FIXED, combinedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, 0);
        	poseStack.popPose();
        }
        if(hasRightStack) {
        	poseStack.pushPose();
        	boolean rotateToHandle = ItemUtils.isSword(rack.getRightStack());
        	poseStack.translate(rotateToHandle ? 0.65 : 0.75, 0.5, 0.1);
        	
        	poseStack.mulPose(Vector3f.ZP.rotationDegrees(rotateToHandle ? 90.0F : -90.0F));
        	poseStack.scale(weaponScale, weaponScale, weaponScale);
        	this.itemRenderer.renderStatic(rack.getRightStack(), ItemTransforms.TransformType.FIXED, combinedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, 0);
        	poseStack.popPose();
        }
        
        poseStack.popPose();
    }

    public static void register() {
        BlockEntityRenderers.register(BlockInit.SHIELD_RACK_BE.get(), ShieldRackBERenderer::new);
    }

}

