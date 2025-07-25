package alec_wam.wam_utils.common.entities.workers.jobs.impl;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;

import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager.JobType;
import alec_wam.wam_utils.common.entities.workers.jobs.MultiBlockPosWorkerJob;
import alec_wam.wam_utils.common.entities.workers.jobs.WorkerJob;
import alec_wam.wam_utils.common.helpers.BlockHelper;
import alec_wam.wam_utils.common.helpers.ItemHelper;
import alec_wam.wam_utils.common.helpers.TreeCutter;
import alec_wam.wam_utils.common.helpers.TreeCutter.Tree;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;

public class HarvestCropJob extends MultiBlockPosWorkerJob {

	public HarvestCropJob(WorkerEntity worker, ResourceKey<Level> dimension, List<BlockPos> blockPosList) {
		super(worker, dimension, blockPosList);
	}
	
	public HarvestCropJob(WorkerEntity worker, ResourceKey<Level> dimension, BlockPos posA, BlockPos posB) {
		super(worker, dimension, posA, posB);
	}

	public static HarvestCropJob createJob(WorkerEntity worker, ItemStack stack, Player player) {
		return JobManager.createJobFromSelection(
            worker, stack, player,
            posList -> new HarvestCropJob(worker, player.level().dimension(), posList),
            (pos1, pos2) -> new HarvestCropJob(worker, player.level().dimension(), pos1, pos2)
        );
	}
	
	@Override
	public boolean needsItem(ItemStack stack) {
		return ItemHelper.isTool(stack) || super.needsItem(stack);
	}
	
	@Override
	public void run() {
		super.run();
		lookAtBlock();
		moveToBlock(0);
		harvestBlock();
		
		//Unload Inventory if idling for 5 seconds
		if(this.idleTimer >= 5 * 20) {
			if(worker.getExternalInventorySettings() != null) {
				//Only check every whole second to prevent unneeded inventory scans
				if((this.idleTimer % 20 == 0) && worker.getExternalInventorySettings().hasInputFace() && worker.hasItemsToUnload()) {
					this.idleTimer = 0;
					worker.startUnloadingInventory(false);
				}
			}
		}
	}
	
	@Override	
	public double getInteractionRange(BlockState state) {
		if(state.getBlock() instanceof CactusBlock) {
			return 2.0D;
		}
		return 1.5D;
	}

	@Override
	public boolean canInteractWithBlock(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if(TreeCutter.isVerticalPlant(state)) {
			BlockState stateAbove = level.getBlockState(pos.above());
			return TreeCutter.isVerticalPlant(stateAbove);
		}
		return BlockHelper.isValidCrop(level, pos, state, true) || BlockHelper.isValidOtherCrop(level, pos, state);
	}
	
	public void harvestBlock() {
		if(this.getWorkingPos() == null)return;
		Level world = worker.level();
		if (!world.isClientSide) {
			lookAtBlock();
			if(worker.swapToBestTool(this.getWorkingPos(), false)) {
				return;
			}
			if (this.isCloseToBlockPos()) {
				BlockPos pos = getWorkingPos();
				BlockState state = world.getBlockState(pos);
				boolean notCropButCanHarvest = false;

				boolean replant = true;
				if (!BlockHelper.isValidCrop(world, pos, state, replant)) {
					if (BlockHelper.isValidOtherCrop(world, pos, state))
						notCropButCanHarvest = true;
					else
						return;
				}

				MutableBoolean seedSubtracted = new MutableBoolean(notCropButCanHarvest);
				BlockState harvestState = state;

				worker.swing(InteractionHand.MAIN_HAND, true);

				if (TreeCutter.isVerticalPlant(state)) {
					Tree tree = TreeCutter.findTree(world, pos);
					if (tree != null) {
						tree.destroyBlocksEffect(worker.level(), (worldState) -> worker.getMainHandItem(), worker, 1.0F, 0.0F,
								(stack, dropPos) -> worker.dropItemFromBrokenBlock(stack, dropPos, true, true));
						this.finishWorking();
					}
					else {
						this.finishWorking();
					}
				} else {
					BlockHelper.destroyBlockAs(world, pos, worker, worker.getMainHandItem(), 1.0F, stack -> {
						if (replant && !seedSubtracted.getValue()
								&& ItemStack.isSameItem(stack, new ItemStack(harvestState.getBlock()))) {
							stack.shrink(1);
							seedSubtracted.setTrue();
						}
						worker.dropItemFromBrokenBlock(pos, stack, true, true);
						// hasHarvested = true;
					});

					BlockState cutCrop = BlockHelper.harvestCrop(world, pos, state, replant);
					world.setBlockAndUpdate(pos,
							cutCrop.canSurvive(world, pos) ? cutCrop : Blocks.AIR.defaultBlockState());
					this.finishWorking();
				}
			}
		}
	}
	
	@Override
	public void stop() {
		
	}

	@Override
	public boolean isSame(WorkerJob job) {
		return job instanceof HarvestCropJob && super.isSame(job);
	}

	@Override
	public JobType getJobType() {
		return JobType.CROP;
	}

}
