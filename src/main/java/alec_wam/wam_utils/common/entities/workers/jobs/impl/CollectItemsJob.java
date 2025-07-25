package alec_wam.wam_utils.common.entities.workers.jobs.impl;

import java.util.Comparator;
import java.util.List;

import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.jobs.AreaWorkerJob;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager.JobType;
import alec_wam.wam_utils.common.entities.workers.jobs.WorkerJob;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class CollectItemsJob extends AreaWorkerJob {
	private static final Vec3i ITEM_PICKUP_REACH = new Vec3i(1, 0, 1);

	private int idleTimer = 0;
	private int scanDelay = 0;
	private ItemEntity pickupEntity;
	
	public CollectItemsJob(WorkerEntity worker, ResourceKey<Level> dimension, List<BlockPos> blockPosList) {
		super(worker, dimension, blockPosList);
	}
	
	public CollectItemsJob(WorkerEntity worker, ResourceKey<Level> dimension, BlockPos posA, BlockPos posB) {
		super(worker, dimension, posA, posB);
	}
	
	@Override
	public void save(ValueOutput valueOutput) {
		super.save(valueOutput);
		valueOutput.putInt("IdleTimer", this.idleTimer);
		valueOutput.putInt("ScanDelay", this.scanDelay);
	}
	
	@Override
	public void load(ValueInput valueInput) {
		super.load(valueInput);
		this.idleTimer = valueInput.getIntOr("IdleTimer", 0);
		this.scanDelay = valueInput.getIntOr("ScanDelay", 0);
	}

	public static CollectItemsJob createJob(WorkerEntity worker, ItemStack stack, Player player) {
		return JobManager.createJobFromSelection(
            worker, stack, player,
            posList -> new CollectItemsJob(worker, player.level().dimension(), posList),
            (pos1, pos2) -> new CollectItemsJob(worker, player.level().dimension(), pos1, pos2)
        );
	}
	
	@Override
	public boolean needsItem(ItemStack stack) {
		return super.needsItem(stack);
	}
	
	@Override
	public boolean blockEating() {
		//Eat at 3 seconds
		return this.idleTimer < 3 * 20 || this.pickupEntity !=null;
	}
	
	@Override
	public void run() {		
		Level level = worker.level();
		if(!level.isClientSide) {
			pickUpItems();
		}
		
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
	
    public boolean canPickUpItem(ItemEntity itemEntity) {
        return itemEntity != null && !itemEntity.isRemoved() && !itemEntity.getItem().isEmpty() && !itemEntity.hasPickUpDelay()
                && this.wantsToPickUp(itemEntity.getItem());
    }

	public void pickUpItems() {
		Level level = worker.level();
		
		pickupNearbyItems();
		
		if(this.pickupEntity == null) {
			this.idleTimer++;
			if(scanDelay > 0) {
				scanDelay--;				
				return;
			}
			
			for(AABB aabb : this.getBoundingBoxes()) {
				List<ItemEntity> itemEntities = level.getEntitiesOfClass(ItemEntity.class, aabb).stream()
                .filter(this::canPickUpItem)
                .sorted(new Comparator<ItemEntity>() {
	
					//Sort Closest First
					@Override
					public int compare(ItemEntity o1, ItemEntity o2) {
						double distance1 = o1.position().distanceToSqr(worker.position());
						double distance2 = o2.position().distanceToSqr(worker.position());
						return Double.compare(distance1, distance2);
					}
				
				}).toList();
				for (ItemEntity itementity : itemEntities) {
					if (this.canPickUpItem(itementity)) {
						this.pickupEntity = itementity;
						this.idleTimer = 0;
						break;
					}
				}
			}
			
			if(this.pickupEntity == null) {
				scanDelay = 5 * 20; //Wait 5 seconds to scan again
			}
		}
		else {
			if(!this.canPickUpItem(pickupEntity)) {
				this.pickupEntity = null;
				return;
			}		

			worker.getLookControl().setLookAt(pickupEntity);
			
			double distance = this.pickupEntity.position().distanceToSqr(worker.position());;
			double maxDistance = worker.getBbWidth() + 1.0D;
			if(distance > maxDistance && worker.getNavigation().isDone()) {
				Path path = worker.getNavigation().createPath(this.pickupEntity, 0);
				if(path !=null) {
					worker.getNavigation().moveTo(path, 1.0D);
				}
			}
		}
	}
	
	public boolean wantsToPickUp(ItemStack stack) {
		//TODO Add Filter
		//Allow at least one of them and let the pickup logic fit in the actual stack
		return this.worker.canAddToInventory(stack.copyWithCount(1));
	}
	
	private void pickupNearbyItems() {
		Level level = worker.level();
		Vec3i vec3i = ITEM_PICKUP_REACH;
		for (ItemEntity itementity : level.getEntitiesOfClass(ItemEntity.class,
				worker.getBoundingBox().inflate((double) vec3i.getX(), (double) vec3i.getY(), (double) vec3i.getZ()))) {
			if (this.canPickUpItem(itementity)) {
				if(level instanceof ServerLevel serverLevel) {
                    worker.pickUpItem(serverLevel, itementity);
                }
			}
		}	
	}
	
	@Override
	public void stop() {
		
	}

	@Override
	public boolean isSame(WorkerJob job) {
		return job instanceof CollectItemsJob && super.isSame(job);
	}

	@Override
	public JobType getJobType() {
		return JobType.COLLECT;
	}

}
