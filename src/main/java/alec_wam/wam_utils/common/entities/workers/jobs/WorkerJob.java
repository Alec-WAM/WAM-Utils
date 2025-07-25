package alec_wam.wam_utils.common.entities.workers.jobs;

import java.util.List;

import javax.annotation.Nullable;

import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.WorkerEntity.ExternalInventoryStatus;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager.JobType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public abstract class WorkerJob {

	public final WorkerEntity worker;
	
	public WorkerJob(WorkerEntity worker) {
		this.worker = worker;
	}
	
	public void saveToTag(CompoundTag tag) {
		
	}
	
	/**
	 * Load additional data that is not part of the constructor
	 * @param tag
	 */
	public void loadAdditionalData(CompoundTag tag) {
		
	}
	
	public abstract JobType getJobType();
	
	public boolean canKeepRunning() {
//		return worker.getExternalInventoryStatus() == ExternalInventoryStatus.NONE && !worker.isEating;
		return worker.getExternalInventoryStatus() == ExternalInventoryStatus.NONE;
	}
	
	public boolean needsItem(ItemStack stack) {
//		if(worker.foodData.needsFood() || worker.foodData.isHurt(worker)) {
//			if(!ItemUtil.isBadFood(stack, worker)) {
//				return true;
//			}
//		}
		return false;
	}
	
	public boolean blockEating() {
		return false;
	}
	
	public boolean pickupMobDrops() {
		return false;
	}
	
	/**
	 * Perform the actions of the job
	 * @param worker
	 */
	public abstract void run();
	
	public abstract void stop();
	
	/*
	 * Render bounding box
	 */
	@Nullable
	public List<AABB> getBoundingBoxes() {
		return null;
	}
	
	public abstract boolean isSame(WorkerJob job);
	
}
