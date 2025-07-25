package alec_wam.wam_utils.common.entities.workers.jobs.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mojang.datafixers.util.Pair;

import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.jobs.AreaWorkerJob;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager.JobType;
import alec_wam.wam_utils.common.entities.workers.jobs.WorkerJob;
import alec_wam.wam_utils.common.helpers.EntityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class BreedAnimalsJob extends AreaWorkerJob {
    public static final int MAX_ANIMALS = 8; // Maximum number of animals of same type allowed to live in an area
	private int idleTimer = 0;
	private int scanDelay = 0;
    private Animal currentBreedTarget;
	private Pair<Animal, Animal> breedTargets;
	
	public BreedAnimalsJob(WorkerEntity worker, ResourceKey<Level> dimension, List<BlockPos> blockPosList) {
		super(worker, dimension, blockPosList);
	}
	
	public BreedAnimalsJob(WorkerEntity worker, ResourceKey<Level> dimension, BlockPos posA, BlockPos posB) {
		super(worker, dimension, posA, posB);
	}

	public static BreedAnimalsJob createJob(WorkerEntity worker, ItemStack stack, Player player) {
		return JobManager.createJobFromSelection(
            worker, stack, player,
            posList -> new BreedAnimalsJob(worker, player.level().dimension(), posList),
            (pos1, pos2) -> new BreedAnimalsJob(worker, player.level().dimension(), pos1, pos2)
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
		return super.needsItem(stack);
	}
	
	@Override
	public boolean blockEating() {
		//Eat at 3 seconds
		return this.idleTimer < 3 * 20 || this.breedTargets !=null;
	}
	
	@Override
	public void run() {		
		Level level = worker.level();
		if(!level.isClientSide) {
			breedAnimals();
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
	
    public boolean validBreedTarget(Entity entity) {
        if (entity == null || !(entity instanceof Animal)) {
            return false;
        }
		Animal animal = (Animal) entity;
        if (animal == null || !animal.isAlive() || animal.isInLove() || animal.isBaby()) {
            return false;
        }
        return animal.canFallInLove() && animal.getAge() == 0;
    }

    public boolean isCurrentBreedTargetFood(ItemStack stack) {
        if (this.currentBreedTarget == null) return false;
        return this.currentBreedTarget.isFood(stack);
    }

	public void breedAnimals() {
		Level level = worker.level();		
		if(this.breedTargets == null) {
			this.idleTimer++;
			if(scanDelay > 0) {
				scanDelay--;				
				return;
			}
			
			for(AABB aabb : this.getBoundingBoxes()) {
				List<Animal> allAnimalsInArea = level.getEntities(worker, aabb, entity -> entity instanceof Animal)
					.stream().map(entity -> (Animal) entity).toList();

				// Group all animals by type (not just breedable ones)
				Map<Class<? extends Animal>, List<Animal>> totalGroups = allAnimalsInArea.stream()
					.collect(Collectors.groupingBy(Animal::getClass));

				// Now filter only valid breed targets from all animals
				List<Animal> breedableEntities = allAnimalsInArea.stream()
					.filter(this::validBreedTarget).toList();

				Map<Class<? extends Animal>, List<Animal>> breedableGroups = breedableEntities.stream()
					.collect(Collectors.groupingBy(Animal::getClass));

                for (Map.Entry<Class<? extends Animal>, List<Animal>> entry : totalGroups.entrySet()) {
                    Class<? extends Animal> type = entry.getKey();
    				List<Animal> totalGroup = entry.getValue();
                
                    if (totalGroup.size() >= MAX_ANIMALS) {
						//Too many animals of this type, skip
						continue;
					}

					List<Animal> breedables = breedableGroups.getOrDefault(type, List.of());
					if (breedables.size() < 2) {
						continue; // not enough to breed
					}

                    boolean hasBreedingItem = worker.checkForItem((stack) -> {
                        return breedables.get(0).isFood(stack);
                    });
                    if (hasBreedingItem) {
                        Pair<Animal, Animal> foundTargets = EntityHelper.findClosestPair(breedables);
						this.breedTargets = foundTargets;
                        this.currentBreedTarget = null;
                        this.idleTimer = 0;
						break;
                    }
                }
			}
			
			if(this.breedTargets == null) {
				scanDelay = 10 * 20; //Wait 10 seconds to scan again
			}
		}
		else {
            Animal breedEntity = this.currentBreedTarget;

            if(breedEntity == null) {
                // If no specific breed target, try to find one from the pair
                if(this.breedTargets.getFirst() != null && this.validBreedTarget(this.breedTargets.getFirst())) {
					breedEntity = this.breedTargets.getFirst();
                    this.currentBreedTarget = breedEntity;
                } else if(this.breedTargets.getSecond() != null && this.validBreedTarget(this.breedTargets.getSecond())) {
					breedEntity = this.breedTargets.getSecond();
                    this.currentBreedTarget = breedEntity;
                }
                else {
					// Both targets are invalid or already in love, reset targets
                    this.breedTargets = null;
                    this.currentBreedTarget = null;
                    return;
                }
            }

            if(worker.swapToItem(this::isCurrentBreedTargetFood, Optional.of(2))){
                return;
            }

			if(!this.validBreedTarget(breedEntity)) {
				this.currentBreedTarget = null;
				return;
			}		

			if(!isCurrentBreedTargetFood(worker.getMainHandItem())){
				return;
			}

			worker.getLookControl().setLookAt(breedEntity);
			
			double distance = breedEntity.position().distanceToSqr(worker.position());;
			double maxDistance = worker.getBbWidth() + 1.0D;
			if(distance > maxDistance) {
				if(worker.getNavigation().isDone()) {
					Path path = worker.getNavigation().createPath(breedEntity, 0);
					if(path !=null) {
						worker.getNavigation().moveTo(path, 1.0D);
					}
				}
			}
			else {
				setBreedingTargetInLove();
			}
		}
	}

    public boolean setBreedingTargetInLove() {
        if (!this.validBreedTarget(this.currentBreedTarget)) {
            this.currentBreedTarget = null;
            return false;
        }

        worker.swing(InteractionHand.MAIN_HAND);
        worker.getMainHandItem().shrink(1);
        this.currentBreedTarget.setInLove(null);
		// Reset the breeding targets after initiating breeding
		this.currentBreedTarget = null;

        return true; // Successfully initiated breeding
    }
	
	@Override
	public void stop() {
		this.breedTargets = null;
		this.currentBreedTarget = null;
		worker.getNavigation().stop();
	}

	@Override
	public boolean isSame(WorkerJob job) {
		return job instanceof BreedAnimalsJob && super.isSame(job);
	}

	@Override
	public JobType getJobType() {
		return JobType.BREED;
	}

}
