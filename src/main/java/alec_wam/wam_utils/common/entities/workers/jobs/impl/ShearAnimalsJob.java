package alec_wam.wam_utils.common.entities.workers.jobs.impl;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.jobs.AreaWorkerJob;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager.JobType;
import alec_wam.wam_utils.common.entities.workers.jobs.WorkerJob;
import alec_wam.wam_utils.common.helpers.EntityHelper;
import alec_wam.wam_utils.common.helpers.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.IShearable;

public class ShearAnimalsJob extends AreaWorkerJob {
	private static final Predicate<ItemStack> IS_SHEARS = (stack) -> {
		return ItemHelper.isShears(stack);
	};
	
	private int idleTimer = 0;
	private int scanDelay = 0;
	private Entity shearEntity;
	
	public ShearAnimalsJob(WorkerEntity worker, ResourceKey<Level> dimension, List<BlockPos> blockPosList) {
		super(worker, dimension, blockPosList);
	}
	
	public ShearAnimalsJob(WorkerEntity worker, ResourceKey<Level> dimension, BlockPos posA, BlockPos posB) {
		super(worker, dimension, posA, posB);
	}

	public static ShearAnimalsJob createJob(WorkerEntity worker, ItemStack stack, Player player) {
		return JobManager.createJobFromSelection(
            worker, stack, player,
            posList -> new ShearAnimalsJob(worker, player.level().dimension(), posList),
            (pos1, pos2) -> new ShearAnimalsJob(worker, player.level().dimension(), pos1, pos2)
        );
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
	
	@Override
	public boolean needsItem(ItemStack stack) {
		return IS_SHEARS.test(stack) || super.needsItem(stack);
	}
	
	@Override
	public boolean blockEating() {
		//Eat at 3 seconds
		return this.idleTimer < 3 * 20 || this.shearEntity !=null;
	}
	
	@Override
	public void run() {		
		Level level = worker.level();
		if(!level.isClientSide) {
			//Don't try and shear if missing shears
			if(!worker.swapToItem(IS_SHEARS, ItemHelper.BEST_ITEM_SORTER)) {
				shearAnimals();
			}			
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
	
	public void shearAnimals() {
		Level level = worker.level();
		ItemStack handItem = worker.getMainHandItem();
		if(!IS_SHEARS.test(handItem))return;
		
		if(this.shearEntity == null) {
			this.idleTimer++;
			if(scanDelay > 0) {
				scanDelay--;				
				return;
			}
			
			for(AABB aabb : this.getBoundingBoxes()) {
				List<Entity> shearableEntities = level.getEntities(worker, aabb, (entity) -> entity instanceof IShearable).stream().sorted(new Comparator<Entity>() {
	
					//Sort Closest First
					@Override
					public int compare(Entity o1, Entity o2) {
						double distance1 = o1.position().distanceToSqr(worker.position());
						double distance2 = o2.position().distanceToSqr(worker.position());
						return Double.compare(distance1, distance2);
					}
				
				}).toList();
				for (Entity entity : shearableEntities) {
					if(!(entity instanceof IShearable))continue;					
					IShearable shearable = (IShearable)entity;
					BlockPos pos = BlockPos.containing(entity.position());
					if (!entity.isRemoved() && entity.isAlive() && shearable.isShearable(null, handItem, level, pos)) {
						this.shearEntity = entity;
						this.idleTimer = 0;
						break;
					}
				}
			}
			
			if(this.shearEntity == null) {
				scanDelay = 5 * 20; //Wait 5 seconds to scan again
			}
		}
		else {
			BlockPos pos = BlockPos.containing(this.shearEntity.position());
			IShearable shearable = (IShearable)this.shearEntity;
			if(this.shearEntity.isRemoved() || !this.shearEntity.isAlive() || !shearable.isShearable(null, handItem, level, pos)) {
				this.shearEntity = null;
				return;
			}		

			worker.getLookControl().setLookAt(shearEntity);
			
			double distance = this.shearEntity.position().distanceToSqr(worker.position());;
			double maxDistance = worker.getBbWidth() + 1.0D;
			if(distance > maxDistance) {
				if(worker.getNavigation().isDone()) {
					Path path = worker.getNavigation().createPath(this.shearEntity, 0);
					if(path !=null) {
						worker.getNavigation().moveTo(path, 1.0D);
					}
				}
			}
			else if(EntityHelper.isLookingAtHorizontally(worker, this.shearEntity, 20)){
				shearAnimal(shearable, pos);
			}
		}
	}
	
	public void shearAnimal(IShearable shearable, BlockPos pos) {
		Level level = worker.level();
		ItemStack handItem = worker.getMainHandItem();
		worker.swing(InteractionHand.MAIN_HAND);
		List<ItemStack> drops = shearable.onSheared(null, handItem, level, pos);
		drops.forEach((drop) -> {
			ItemStack remainder = worker.addToInventory(drop);			
			if(!remainder.isEmpty()) {
				if(worker.hasItemsToUnload()) {
					worker.startUnloadingInventory(false);
				}
				if(worker.level() instanceof ServerLevel serverLevel) {
					shearable.spawnShearedDrop(serverLevel, pos, remainder);
				}
			}
		});
		this.shearEntity.gameEvent(GameEvent.SHEAR, worker);
		handItem.hurtAndBreak(1, worker, EquipmentSlot.MAINHAND);
		this.shearEntity = null;	
		this.idleTimer = 0;
	}
	
	@Override
	public void stop() {
		this.shearEntity = null;
		worker.getNavigation().stop();
	}

	@Override
	public boolean isSame(WorkerJob job) {
		return job instanceof ShearAnimalsJob && super.isSame(job);
	}

	@Override
	public JobType getJobType() {
		return JobType.SHEAR;
	}

}
