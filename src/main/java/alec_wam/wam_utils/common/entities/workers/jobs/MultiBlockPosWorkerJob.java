package alec_wam.wam_utils.common.entities.workers.jobs;

import java.util.List;

import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.helpers.EntityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public abstract class MultiBlockPosWorkerJob extends AreaWorkerJob {

	private BlockPos workingPos;
	private BlockPos validPathPos;
	private int blockScanIndex = 0;
	private int blockScanStartIndex = -1; // Reset to -1 when a valid block is found or reset
	protected int scanDelay = 0;
	public int idleTimer = 0;
	public MultiBlockPosWorkerJob(WorkerEntity worker, ResourceKey<Level> dimension, List<BlockPos> blockPosList) {
		super(worker, dimension, blockPosList);
	}
	
	public MultiBlockPosWorkerJob(WorkerEntity worker, ResourceKey<Level> dimension, BlockPos posA, BlockPos posB) {
		super(worker, dimension, posA, posB);
	}
	
	@Override
	public void save(ValueOutput valueOutput) {
		super.save(valueOutput);
		valueOutput.storeNullable("WorkingPos", BlockPos.CODEC, workingPos);
		valueOutput.putInt("BlockScanIndex", this.blockScanIndex);
		valueOutput.putInt("ScanDelay", this.scanDelay);
		valueOutput.putInt("IdleTimer", this.idleTimer);
	}
	
	@Override
	public void load(ValueInput valueInput) {
		super.load(valueInput);
		this.workingPos = valueInput.read("WorkingPos", BlockPos.CODEC).orElse(null);
		this.blockScanIndex = valueInput.getIntOr("BlockScanIndex", 0);
		this.scanDelay = valueInput.getIntOr("ScanDelay", 0);
		this.idleTimer = valueInput.getIntOr("IdleTimer", 0);
	}
	
	public abstract boolean canInteractWithBlock(Level level, BlockPos pos);

	public boolean isCloseToBlockPos() {
		if(this.getWorkingPos() == null)return false;
		Level level = worker.level();
		BlockState state = level.getBlockState(getWorkingPos());
		double range = getInteractionRange(state);
		return EntityHelper.isCloseToBlockPos(worker, workingPos, range);		
	}
	
	public double getInteractionRange(BlockState state) {
		return 3.0D;
	}
	
	public void moveToBlock(int minDistance) {
		if(this.workingPos == null)return;
		//TODO Handle stuck timer
		BlockPos pos = getWorkingPos();
//		EntityHelper.moveToBlock(worker, workingPos, 1.0F, range, minDistance);
		
		if(validPathPos == null) {
			this.validPathPos = minDistance == 0 ? pos : EntityHelper.findWalkableAdjacentPos(worker, pos);
		}

		if(this.validPathPos !=null) {
			BlockPosTracker blockpostrackerLook = new BlockPosTracker(pos);
			BlockPosTracker blockpostracker = new BlockPosTracker(this.validPathPos);
	        worker.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, blockpostrackerLook);
	        worker.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(blockpostracker, 1.0F, 0));
		}
	}
	
	public int scanSpeed() {
		return 20;
	}
	
	@Override
	public boolean blockEating() {
		//Eat at 3 seconds
		return this.idleTimer < 3 * 20 || this.getWorkingPos() !=null;
	}
	
	public boolean shouldScanForBlocks() {
		return true;
	}

	@Override
	public void run() {
		Level level = worker.level();
		if(this.shouldScanForBlocks()){
			if(this.workingPos == null) {			
				idleTimer++;
				if(this.scanDelay > 0) {
					this.scanDelay--;
					return;
				}
				BlockPos scanPos = blockPosList.get(blockScanIndex);
				//System.out.println(scanPos);
				//System.out.println(blockScanIndex);
				if(canInteractWithBlock(level, scanPos)) {
					idleTimer = 0;
					this.workingPos = scanPos;
					this.onSetWorkPos();
					blockScanIndex++;
					blockScanIndex %= blockPosList.size();
					blockScanStartIndex = -1; // reset loop tracking
				}
				else {
					if (blockScanStartIndex == -1) {
						blockScanStartIndex = blockScanIndex;
					}
					this.scanDelay = scanSpeed();
					blockScanIndex++;
					blockScanIndex %= blockPosList.size();

					if (blockScanIndex == blockScanStartIndex) {
						blockScanStartIndex = -1;
						this.onNoValidBlocksFound(); // 🟡 Called only after full loop
					}
				}
			}
			else {
				//Clear if invalid
				//TODO Make this random check not every tick
				if(!canInteractWithBlock(level, this.workingPos)) {
					this.invalidateWorkingPos();
				}
			}
		}
	}
	
	/**
	 * Called after full loop and none of the blocks were valid
	*/
	public void onNoValidBlocksFound() {
		
	}

	public void onSetWorkPos() {
		if(this.validPathPos !=null) {
			this.validPathPos = null;
		}
	}

	public BlockPos getWorkingPos() {
		return workingPos;
	}

	public void invalidateWorkingPos() {
		if(this.workingPos != null) {
			this.workingPos = null;
			this.scanDelay = 0;
			this.idleTimer = 0;
			worker.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
			worker.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		}
	}
	
	public void finishWorking() {
		this.idleTimer = 0;
		this.workingPos = null;
	}
	
	public void lookAtBlock() {
		if(getWorkingPos() == null) {
			return;
		}
		this.worker.getLookControl().setLookAt(Vec3.atCenterOf(getWorkingPos()));
	}

	@Override
	public boolean isSame(WorkerJob job) {
		if(job instanceof MultiBlockPosWorkerJob otherJob) {
			return this.getWorkingPos() != null && this.getWorkingPos().equals(otherJob.getWorkingPos()) && super.isSame(job);
		}
		return false;
	}

}
