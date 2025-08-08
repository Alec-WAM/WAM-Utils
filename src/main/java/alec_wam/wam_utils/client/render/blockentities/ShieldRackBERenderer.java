package alec_wam.wam_utils.client.render.blockentities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import alec_wam.wam_utils.common.blocks.shieldrack.ShieldRackBE;
import alec_wam.wam_utils.common.blocks.shieldrack.ShieldRackBlock;
import alec_wam.wam_utils.common.helpers.ItemHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

public class ShieldRackBERenderer implements BlockEntityRenderer<ShieldRackBE> {
	
	private final ItemModelResolver itemModelResolver;
    private final ItemStackRenderState itemRenderState = new ItemStackRenderState();

    public ShieldRackBERenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.getItemModelResolver();
    }
	
	@Override
	public void render(ShieldRackBE blockEntity, float partialTick, PoseStack poseStack,
			MultiBufferSource bufferSource, int packedLight, int packedOverlay, Vec3 cameraPos) {
		IItemHandler handler = blockEntity.getExternalItemHandler(null);        
		if(handler !=null) {
			// poseStack.pushPose();			
			// this.itemRenderState.render(poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
			// poseStack.popPose();

            poseStack.pushPose();	
            Direction rot = blockEntity.getBlockState().getValue(ShieldRackBlock.FACING);
            if(rot == Direction.NORTH){
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.translate(-1, 0, -1);
            }
            
            if(rot == Direction.WEST){
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F + 180.0F));
                poseStack.translate(0, 0, -1);
            }
            
            if(rot == Direction.EAST){
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.translate(-1, 0, 0);
            }
            boolean hasLeftStack = !blockEntity.getLeftStack().isEmpty();
            boolean hasRightStack = !blockEntity.getRightStack().isEmpty();
           
            // Shield
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, (hasLeftStack && hasRightStack) ? 0.05 : 0);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            float scale = 1.8F;
            poseStack.scale(scale, scale, scale);
            this.itemModelResolver.updateForTopItem(itemRenderState, blockEntity.getShieldStack(), ItemDisplayContext.FIXED, blockEntity.getLevel(), null, 0);
            this.itemRenderState.render(poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();

            float weaponScale = 1.0f;
            // Left weapon
            poseStack.pushPose();
            boolean isCrossbow = blockEntity.getLeftStack().getItem() instanceof net.minecraft.world.item.CrossbowItem;
            boolean rotateToHandle = ItemHelper.isSword(blockEntity.getLeftStack()) || ItemHelper.isRangedWeapon(blockEntity.getLeftStack());
            poseStack.translate(rotateToHandle ? 0.35 : 0.25, 0.5, hasRightStack ? 0.15 : 0.1);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            
            poseStack.mulPose(Axis.ZP.rotationDegrees(isCrossbow ? 0.0F : rotateToHandle ? 90.0F : -90.0F));
            poseStack.scale(weaponScale, weaponScale, weaponScale);
            this.itemModelResolver.updateForTopItem(itemRenderState, blockEntity.getLeftStack(), ItemDisplayContext.FIXED, blockEntity.getLevel(), null, 0);
            this.itemRenderState.render(poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();

            // Right weapon
            poseStack.pushPose();
            isCrossbow = blockEntity.getRightStack().getItem() instanceof net.minecraft.world.item.CrossbowItem;
            rotateToHandle = ItemHelper.isSword(blockEntity.getRightStack()) || ItemHelper.isRangedWeapon(blockEntity.getRightStack());
            poseStack.translate(rotateToHandle ? 0.65 : 0.75, 0.5, 0.1);
            
            poseStack.mulPose(Axis.ZP.rotationDegrees(isCrossbow ? 0.0F : rotateToHandle ? 90.0F : -90.0F));
            poseStack.scale(weaponScale, weaponScale, weaponScale);
            this.itemModelResolver.updateForTopItem(itemRenderState, blockEntity.getRightStack(), ItemDisplayContext.FIXED, blockEntity.getLevel(), null, 0);
            this.itemRenderState.render(poseStack, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            
            poseStack.popPose();
		}
	}

}
