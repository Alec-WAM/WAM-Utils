package alec_wam.wam_utils.common.blocks.conveyor.splitter;

import javax.annotation.Nullable;

import alec_wam.wam_utils.common.blocks.BaseEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

public class ConveyorSplitterBlock extends BaseEntityBlock {

	public ConveyorSplitterBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ConveyorSplitterBE(pos, state);
	}

	@Override
	public InteractionResult playerInteract(Level level, Player player, BlockPos blockPos, BlockHitResult hitResult, @Nullable ItemStack stack, @Nullable InteractionHand hand) {
		
		BlockEntity blockEntity = level.getBlockEntity(blockPos);
		
		if(blockEntity !=null && blockEntity instanceof ConveyorSplitterBE splitter) {
			return splitter.playerInteract(player, hitResult, stack, hand);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isValidBE(BlockEntity blockEntity) {
		return blockEntity instanceof ConveyorSplitterBE;
	}
	
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
		
		BlockEntity blockEntity = level.getBlockEntity(pos);		
		if(blockEntity !=null && blockEntity instanceof ConveyorSplitterBE splitter) {
			splitter.clearRouteCache();
		}
    }


}
