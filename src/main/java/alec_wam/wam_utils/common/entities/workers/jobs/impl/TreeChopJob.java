package alec_wam.wam_utils.common.entities.workers.jobs.impl;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.jobs.BreakBlockWorkerJob;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager.JobType;
import alec_wam.wam_utils.common.entities.workers.jobs.WorkerJob;
import alec_wam.wam_utils.common.helpers.BlockHelper;
import alec_wam.wam_utils.common.helpers.ItemHelper;
import alec_wam.wam_utils.common.helpers.TreeCutter;
import alec_wam.wam_utils.common.helpers.TreeCutter.Tree;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

public class TreeChopJob extends BreakBlockWorkerJob {

	private static final Predicate<ItemStack> IS_SAPLING = (stack) -> {
		return stack.is(ItemTags.SAPLINGS) || stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() instanceof SaplingBlock;
	};

	public Tree tree;
	public boolean missingSaplingItems = false;
	
	public TreeChopJob(WorkerEntity worker, ResourceKey<Level> dimension, List<BlockPos> blockPosList) {
		super(worker, dimension, blockPosList);
	}
	
	public TreeChopJob(WorkerEntity worker, ResourceKey<Level> dimension, BlockPos posA, BlockPos posB) {
		super(worker, dimension, posA, posB);
	}

	public static TreeChopJob createJob(WorkerEntity worker, ItemStack stack, Player player) {
		return JobManager.createJobFromSelection(
            worker, stack, player,
            posList -> new TreeChopJob(worker, player.level().dimension(), posList),
            (pos1, pos2) -> new TreeChopJob(worker, player.level().dimension(), pos1, pos2)
        );
	}	
	
	@Override
	public void saveToTag(CompoundTag tag) {
		super.saveToTag(tag);
		tag.putBoolean("MissingSaplingItems", this.missingSaplingItems);
	}

	@Override
	public void loadAdditionalData(CompoundTag tag) {
		super.loadAdditionalData(tag);
		this.missingSaplingItems = tag.getBooleanOr("MissingSaplingItems", false);
	}

	@Override
	public void run() {
		if(tree == null && destroyProgress > 0 && this.getWorkingPos() != null) {
			tree = TreeCutter.findTree(worker.level(), this.getWorkingPos());
		}	
		super.run();

		plantSaplings();
		
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
	public void tickBlockBreak() {
		if(this.getWorkingPos() !=null) {
			BlockState state = worker.level().getBlockState(getWorkingPos());
			if(state.isAir() || state.canBeReplaced()) {
				return;
			}
		}
		super.tickBlockBreak();
	}

	public void plantSaplings() {
		Level level = worker.level();
		if (!level.isClientSide) {			
			if(this.getWorkingPos() == null)return;
			BlockPos pos = this.getWorkingPos();
			
			BlockState state = level.getBlockState(pos);
			if(TreeCutter.isLog(state)){
				return;
			}
			//TODO Make this more intelligent, like only plant saplings on valid soil
			Optional<Integer> saplingRequestSize = Optional.of(this.getBlockPosList().size());
			worker.requireItem(IS_SAPLING, saplingRequestSize);

			if (worker.swapToItem(IS_SAPLING, saplingRequestSize)) {
				return;
			}
			
			ItemStack handItem = worker.getMainHandItem();
			
			if(handItem.isEmpty() || (!IS_SAPLING.test(handItem))) {
				this.missingSaplingItems = true;
				this.invalidateWorkingPos();
				return;
			}

			BlockState placeState = BlockHelper.getBlockStateForPlacement(level, pos, handItem, worker, InteractionHand.MAIN_HAND, Direction.UP);
			
			if(placeState == null || !placeState.canSurvive(level, pos)) {
				this.invalidateWorkingPos();
				return;
			}

			if (this.isCloseToBlockPos()) {				
				if(BlockHelper.placeBlock(level, pos, handItem, worker, InteractionHand.MAIN_HAND, Direction.UP, true, true)) {
					worker.swing(InteractionHand.MAIN_HAND);
				}
				this.finishWorking();
			}
		}
	}

	public boolean canSaplingSurvive(@Nullable BlockState state, LevelReader level, BlockPos pos) {
		BlockState saplingState = state != null ? state : Blocks.OAK_SAPLING.defaultBlockState();
        BlockPos blockpos = pos.below();
        BlockState belowBlockState = level.getBlockState(blockpos);
        var soilDecision = belowBlockState.canSustainPlant(level, blockpos, Direction.UP, saplingState);
        if (!soilDecision.isDefault()) return soilDecision.isTrue();
        return belowBlockState.is(BlockTags.DIRT) || belowBlockState.getBlock() instanceof net.minecraft.world.level.block.FarmBlock;
    }
	
	@Override
	public boolean needsItem(ItemStack stack) {
		return ItemHelper.isAxe(stack) || super.needsItem(stack);
	}

	@Override
	public boolean allowEmptyHandBreaking() {
		return false;
	}
	
	@Override	
	public double getInteractionRange(BlockState state) {
		return 1.5D;
	}
	
	@Override
	public void stop() {
		super.stop();
		this.tree = null;
	}
	
	@Override
	public boolean canInteractWithBlock(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if(TreeCutter.isLog(state)){
			return true;
		}

		if(!this.missingSaplingItems) {
			if(this.canSaplingSurvive(null, level, pos)) {
				if (state.isAir() || state.canBeReplaced()) {
					if(!state.is(BlockTags.SAPLINGS) && !(state.getBlock() instanceof SaplingBlock)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public void onBlockBroken(BlockPos pos, BlockState state) {
		ItemStack tool = worker.getMainHandItem();
		if(tree !=null) {
			if(tree != TreeCutter.NO_TREE) {
				tree.destroyBlocks(worker.level(), (worldState) -> ItemHelper.findBestTool(worker, worldState), worker, (dropPos, stack) -> worker.dropItemFromBrokenBlock(dropPos, stack, true, true));
			}
			this.tree = null;
		}
		//Destroy "Base" block of tree
		BlockHelper.destroyAndPickUpBlock(worker.level(), pos, 1.0F, tool, worker, (stack, dropPos) -> worker.dropItemFromBrokenBlock(stack, dropPos, true, true));
		
		//Replant Sapling
		ItemStack saplingStack = ItemHelper.findFirstWithTag(worker.getInventory(), ItemTags.SAPLINGS);
		if(!saplingStack.isEmpty()) {			
			BlockHelper.placeBlock(worker.level(), pos, saplingStack, worker, InteractionHand.OFF_HAND, Direction.UP, true, true);
			worker.swing(InteractionHand.OFF_HAND);
		}
		this.missingSaplingItems = false;
		this.finishWorking();
	}

	@Override
	public boolean isSame(WorkerJob job) {
		return job instanceof TreeChopJob && super.isSame(job);
	}

	@Override
	public JobType getJobType() {
		return JobType.TREE;
	}

}
