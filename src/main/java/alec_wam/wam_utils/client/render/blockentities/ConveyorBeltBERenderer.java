package alec_wam.wam_utils.client.render.blockentities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import alec_wam.wam_utils.common.blocks.conveyor.ConveyorBeltBE;
import alec_wam.wam_utils.common.blocks.conveyor.ConveyorBeltBE.MovingItem;
import alec_wam.wam_utils.common.blocks.conveyor.ConveyorBeltBE.MovingItem.Phase;
import alec_wam.wam_utils.common.blocks.conveyor.ConveyorBeltBlock;
import alec_wam.wam_utils.common.blocks.conveyor.ConveyorBeltBlock.BeltSlope;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

public class ConveyorBeltBERenderer implements BlockEntityRenderer<ConveyorBeltBE> {
	
	private final ItemModelResolver itemModelResolver;
    private final RandomSource random = RandomSource.create();
    private final ItemClusterRenderState itemRenderState = new ItemClusterRenderState();

    public ConveyorBeltBERenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.getItemModelResolver();
		this.itemRenderState.shouldSpread = true;
    }
	
	@Override
	public void render(ConveyorBeltBE blockEntity, float partialTick, PoseStack poseStack,
			MultiBufferSource bufferSource, int packedLight, int packedOverlay, Vec3 cameraPos) {
		IItemHandler handler = blockEntity.getItemHandler(null);        
		if(handler !=null) {
			poseStack.pushPose();			

			ItemStack stack = blockEntity.clientTransfer ? blockEntity.ghostItem : handler.getStackInSlot(0);
			this.itemModelResolver.updateForTopItem(itemRenderState.item, stack, ItemDisplayContext.GROUND, blockEntity.getLevel(), null, 0);
			this.itemRenderState.count = ItemClusterRenderState.getRenderedAmount(stack.getCount());
            this.itemRenderState.seed = ItemClusterRenderState.getSeedForItemStack(stack);
	        
            MovingItem movingItem = blockEntity.movingItem;
	        float prevProgress = movingItem !=null ? movingItem.prevProgress : 0.0F;
	        float progress = movingItem !=null ? movingItem.progress : 0.0F;
	        float calcProgress = Mth.lerp(partialTick, prevProgress, progress);
	        Direction dir = blockEntity.getBlockState().getValueOrElse(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
	        BeltSlope beltSlope = blockEntity.getBlockState().getValueOrElse(ConveyorBeltBlock.SLOPE, BeltSlope.UP);
	        Vec3 offset = Vec3.ZERO;
	        
	        Vec3 center = new Vec3(0.5, 0.1, 0.5);
	        if(beltSlope != BeltSlope.FLAT) {
	        	center = center.add(0, 0.5, 0);
	    	}
	        
	        if(blockEntity.movingItem !=null) {
		        if (movingItem.phase == MovingItem.Phase.MOVING_IN) {
		        	Vec3 start = getOffsetForDirection(movingItem.from);
		        	
		        	if(beltSlope == BeltSlope.DOWN) {
		        		start = start.add(0, 1, 0);
		        	}
		        	
		        	offset = interpolateBetween(start, center, calcProgress);
		        } else {
		        	Vec3 end = getOffsetForDirection(movingItem.to);
		        	if(beltSlope == BeltSlope.UP) {
		        		end = end.add(0, 1, 0);
		        	}
		        	offset = interpolateBetween(center, end, calcProgress);
		        }
	        }
	        else if(blockEntity.clientTransfer) {
	        	offset = getOffsetForDirection(dir);
	        	if(beltSlope == BeltSlope.UP) {
	        		offset = offset.add(0, 1, 0);
	        	}
	        }
	        
	        poseStack.translate(offset.x, offset.y, offset.z);
	        
	        //Rotate items from previous direction to new direction
	        if(movingItem !=null && movingItem.phase == Phase.MOVING_IN && movingItem.from != movingItem.to.getOpposite()) {
	        	//Use opposite for correct directions
	        	float fromYaw = -movingItem.from.getOpposite().toYRot();
	        	float toYaw = -movingItem.to.toYRot();

	        	float angleDiff = Mth.wrapDegrees(toYaw - fromYaw);
	        	float currentYaw = fromYaw + angleDiff * (progress);
	        	
	        	poseStack.mulPose(Axis.YP.rotationDegrees(currentYaw));
	        }
	        else {	        
	        	poseStack.mulPose(Axis.YP.rotationDegrees(-dir.toYRot()));
	        }
	        
	        if(beltSlope == BeltSlope.UP) {
	        	poseStack.mulPose(Axis.XP.rotationDegrees(-45));
	    	}
	        else if(beltSlope == BeltSlope.DOWN) {
	        	poseStack.mulPose(Axis.XP.rotationDegrees(45));
	    	}
	        
			ItemEntityRenderer.renderMultipleFromCount(poseStack, bufferSource, 15728880, this.itemRenderState, this.random);
			poseStack.popPose();
		}
	}
	
	private Vec3 getOffsetForDirection(Direction dir) {
	    return switch (dir) {
        	case NORTH -> new Vec3(0.5, 0.1, 0.0);
		    case SOUTH -> new Vec3(0.5, 0.1, 1.0);
	        case EAST  -> new Vec3(1.0, 0.1, 0.5);
	        case WEST  -> new Vec3(0.0, 0.1, 0.5);
	        default    -> new Vec3(0.5, 0.1, 0.5);
	    };
	}
	
	private Vec3 interpolateBetween(Vec3 start, Vec3 end, float progress) {
	    return start.lerp(end, progress);
	}

}
