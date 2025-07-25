package alec_wam.wam_utils.common.entities.workers.jobs.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.jobs.AreaWorkerJob;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager.JobType;
import alec_wam.wam_utils.common.entities.workers.jobs.WorkerJob;
import alec_wam.wam_utils.common.helpers.EntityHelper;
import alec_wam.wam_utils.common.helpers.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent.LivingTargetType;

public class ButcherAnimalsJob extends AreaWorkerJob {
    public static final int MIN_ANIMALS = 4; // Minimum number of animals of same type allowed to live in an area
    private int idleTimer = 0;
    private int scanDelay = 0;
    private int attackDelay = 0;
    private Animal killTarget;

    public ButcherAnimalsJob(WorkerEntity worker, ResourceKey<Level> dimension, List<BlockPos> blockPosList) {
        super(worker, dimension, blockPosList);
    }

    public ButcherAnimalsJob(WorkerEntity worker, ResourceKey<Level> dimension, BlockPos posA, BlockPos posB) {
        super(worker, dimension, posA, posB);
    }

    public static ButcherAnimalsJob createJob(WorkerEntity worker, ItemStack stack, Player player) {
        return JobManager.createJobFromSelection(
                worker, stack, player,
                posList -> new ButcherAnimalsJob(worker, player.level().dimension(), posList),
                (pos1, pos2) -> new ButcherAnimalsJob(worker, player.level().dimension(), pos1, pos2));
    }

    @Override
    public void saveToTag(CompoundTag tag) {
        super.saveToTag(tag);
        tag.putInt("IdleTimer", this.idleTimer);
        tag.putInt("ScanDelay", this.scanDelay);
    }

    @Override
    public void loadAdditionalData(CompoundTag tag) {
        super.loadAdditionalData(tag);
        this.idleTimer = tag.getIntOr("IdleTimer", 0);
        this.scanDelay = tag.getIntOr("ScanDelay", 0);
    }

    @Override
    public boolean needsItem(ItemStack stack) {
        return ItemHelper.isMeleeWeapon(stack) || super.needsItem(stack);
    }

    @Override
    public boolean blockEating() {
        // Eat at 3 seconds
        return this.idleTimer < 3 * 20 || this.killTarget != null;
    }

    @Override
    public void run() {
        Level level = worker.level();
        if (!level.isClientSide) {
            killAnimals();
        }

        // Unload Inventory if idling for 5 seconds
        if (this.idleTimer >= 5 * 20) {
            if (worker.getExternalInventorySettings() != null) {
                // Only check every whole second to prevent unneeded inventory scans
                if ((this.idleTimer % 20 == 0) && worker.getExternalInventorySettings().hasInputFace()
                        && worker.hasItemsToUnload()) {
                    this.idleTimer = 0;
                    worker.startUnloadingInventory(false);
                }
            }
        }
    }

    public boolean validKillTarget(Entity entity) {
        if (entity == null || !(entity instanceof Animal)) {
            return false;
        }
        Animal animal = (Animal) entity;
        if (animal == null || !animal.isAlive() || animal.isInLove() || animal.isBaby()) {
            return false;
        }
        return animal.isAttackable();
    }

    public void killAnimals() {
        Level level = worker.level();
        if (this.killTarget == null) {
            this.idleTimer++;
            if (scanDelay > 0) {
                scanDelay--;
                return;
            }

            for (AABB aabb : this.getBoundingBoxes()) {
                List<Animal> allAnimalsInArea = level.getEntities(worker, aabb, this::validKillTarget)
                        .stream().map(entity -> (Animal) entity).toList();

                // Group all animals by type (not just breedable ones)
                Map<Class<? extends Animal>, List<Animal>> totalGroups = allAnimalsInArea.stream()
                        .collect(Collectors.groupingBy(Animal::getClass));

                for (Map.Entry<Class<? extends Animal>, List<Animal>> entry : totalGroups.entrySet()) {
                    List<Animal> totalGroup = entry.getValue();

                    if (totalGroup.size() <= MIN_ANIMALS) {
                        continue; // Skip groups that are too small
                    }
                    Animal target = totalGroup.stream().sorted(EntityHelper.getEntityDistanceComparator(worker)).findFirst().orElse(null);
                    if(target !=null){
                        
                        LivingChangeTargetEvent changeTargetEvent = CommonHooks.onLivingChangeTarget(worker, target, LivingTargetType.MOB_TARGET);
                        if(changeTargetEvent.isCanceled() || changeTargetEvent.getNewAboutToBeSetTarget() == null) {
                            continue; // Skip if the target change was canceled
                        }
                        
                        if(this.validKillTarget(changeTargetEvent.getNewAboutToBeSetTarget())) {
                            target = (Animal)changeTargetEvent.getNewAboutToBeSetTarget();
                        }
                        else {
                            continue; // Skip if the target is not valid
                        }
                        
                        this.killTarget = target;
                        this.idleTimer = 0;
                        break;
                    }
                }
            }

            if (this.killTarget == null) {
                scanDelay = 10 * 20; // Wait 10 seconds to scan again
            }
        }
        else {
            if (worker.swapToBestMelee()) {
                return;
            }

            if (!this.validKillTarget(this.killTarget)) {
                this.killTarget = null;
                return;
            }

            worker.getLookControl().setLookAt(this.killTarget);

            if(this.attackDelay > 0) {
				this.attackDelay--;
				return;
			}

            double distance = this.killTarget.position().distanceToSqr(worker.position());
            double maxDistance = worker.getBbWidth() + 1.0D;
            if (distance > maxDistance) {
                if (worker.getNavigation().isDone()) {
                    Path path = worker.getNavigation().createPath(this.killTarget, 0);
                    if (path != null) {
                        worker.getNavigation().moveTo(path, 1.0D);
                    }
                }
            } else {
                swingSword(this.killTarget);
            }
        }
    }

    public void swingSword(Entity attackEntity) {
        Level level = worker.level();
        if(level instanceof ServerLevel serverLevel){
            if (attackEntity.isAttackable()) {
                worker.swing(InteractionHand.MAIN_HAND);
                if(EntityHelper.attackEntity(serverLevel, worker, attackEntity)) {
                    this.attackDelay = 20;
                }
            }
        }
	}

    @Override
    public void stop() {
        this.killTarget = null;
        worker.getNavigation().stop();
    }

    @Override
    public boolean isSame(WorkerJob job) {
        return job instanceof ButcherAnimalsJob && super.isSame(job);
    }

    @Override
    public JobType getJobType() {
        return JobType.BUTCHER;
    }

}
