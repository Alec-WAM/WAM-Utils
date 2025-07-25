package alec_wam.wam_utils.common.entities.workers.jobs;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.helpers.BlockHelper;
import alec_wam.wam_utils.common.helpers.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BreakBlockWorkerJob extends MultiBlockPosWorkerJob {

	public static final AtomicInteger NEXT_BREAKER_ID = new AtomicInteger();	
	
	protected int ticksUntilNextProgress;
	protected int destroyProgress;
	protected int breakerId = -NEXT_BREAKER_ID.incrementAndGet();
	
	public BreakBlockWorkerJob(WorkerEntity worker, ResourceKey<Level> dimension, List<BlockPos> blockPosList) {
		super(worker, dimension, blockPosList);
	}
	
	public BreakBlockWorkerJob(WorkerEntity worker, ResourceKey<Level> dimension, BlockPos posA, BlockPos posB) {
		super(worker, dimension, posA, posB);
	}
	
	@Override
	public boolean needsItem(ItemStack stack) {
		return ItemHelper.isTool(stack) || super.needsItem(stack);
	}
	
	@Override
	public void run() {
		super.run();
		lookAtBlock();
		moveToBlock(1);
		tickBlockBreak();
	}
	
	@Override
	public void stop() {
		if(this.getWorkingPos() !=null) {
			if (destroyProgress != 0) {
				BlockPos breakingPos = this.getWorkingPos();
				destroyProgress = 0;
				worker.level().destroyBlockProgress(breakerId, breakingPos, -1);
			}
		}
	}
	
	@Override
	public void finishWorking() {
		super.finishWorking();
		this.ticksUntilNextProgress = 0;
	}
	
	public boolean allowEmptyHandBreaking() {
		return true;
	}
	
	public void tickBlockBreak() {
		if(this.getWorkingPos() == null)return;
		Level level = worker.level();
		if(!level.isClientSide) {			
			BlockPos breakingPos = getWorkingPos();

			if (ticksUntilNextProgress < 0)
				return;
			if (ticksUntilNextProgress > 0 && ticksUntilNextProgress-- > 0)
				return;			
			
			if(!this.isCloseToBlockPos()) {
				return;
			}
			
			BlockState stateToBreak = level.getBlockState(breakingPos);
			float blockHardness = stateToBreak.getDestroySpeed(level, breakingPos);
			
			if (!BlockHelper.canBreak(stateToBreak, blockHardness) || !this.canInteractWithBlock(level, breakingPos)) {
				if (destroyProgress != 0) {
					destroyProgress = 0;
					level.destroyBlockProgress(breakerId, breakingPos, -1);
				}
				return;
			}		
			
			if(worker.swapToBestTool(this.getWorkingPos(), !this.allowEmptyHandBreaking())) {
				//Grabbing tool so stop trying to mine
				return;
			}
			lookAtBlock();
			
			float breakSpeed = getBreakSpeed(stateToBreak, blockHardness);
			destroyProgress += Mth.clamp((int) ((breakSpeed / blockHardness)), 1, 10 - destroyProgress);
			if(destroyProgress % 4 == 0) {
				level.playSound(null, worker.blockPosition(), stateToBreak.getSoundType(level, breakingPos, worker)
					.getHitSound(), SoundSource.BLOCKS, .25f, 1);
			}

			if (destroyProgress >= 10) {
				onBlockBroken(breakingPos, stateToBreak);
				destroyProgress = 0;
				ticksUntilNextProgress = 0;
				level.destroyBlockProgress(breakerId, breakingPos, -1);
				return;
			}

			ticksUntilNextProgress = (int) ((blockHardness / breakSpeed) / 11);
			level.destroyBlockProgress(breakerId, breakingPos, (int) destroyProgress);
			worker.swing(InteractionHand.MAIN_HAND);
		}
	}
	
	public float getBreakSpeed(BlockState state, float blockHardness) {
		ItemStack bestTool = worker.getMainHandItem();		
		int i = bestTool.isCorrectToolForDrops(state) ? 30 : 100;
        return BlockHelper.getBasicDigSpeed(worker, bestTool, state) / blockHardness / (float)i;
	}
	
	public void onBlockBroken(BlockPos breakingPos, BlockState state) {
		ItemStack heldItem = worker.getMainHandItem();	
		BlockHelper.destroyAndPickUpBlock(worker.level(), breakingPos, 1.0F, heldItem, worker, (stack, dropPos) -> worker.dropItemFromBrokenBlock(stack, dropPos, true, true));
		this.finishWorking();
	}

}
