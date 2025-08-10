package alec_wam.wam_utils.common.entities.workers.jobs.impl;

import java.util.ArrayList;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

public class MeleeAttackJob extends AreaWorkerJob {
	private static final Predicate<ItemStack> IS_MELEE_WEAPON = (stack) -> {
		return ItemHelper.isMeleeWeapon(stack);
	};
	
	private int idleTimer = 0;
	private int scanDelay = 0;
	private int attackDelay = 0;
	private Entity attackEntity;
	
	public MeleeAttackJob(WorkerEntity worker, ResourceKey<Level> dimension, List<BlockPos> blockPosList) {
		super(worker, dimension, blockPosList);
	}
	
	public MeleeAttackJob(WorkerEntity worker, ResourceKey<Level> dimension, BlockPos posA, BlockPos posB) {
		super(worker, dimension, posA, posB);
	}

	public static MeleeAttackJob createJob(WorkerEntity worker, ItemStack stack, Player player) {
		return JobManager.createJobFromSelection(
            worker, stack, player,
            posList -> new MeleeAttackJob(worker, player.level().dimension(), posList),
            (pos1, pos2) -> new MeleeAttackJob(worker, player.level().dimension(), pos1, pos2)
        );
	}
	
	@Override
	public void save(ValueOutput valueOutput) {
		super.save(valueOutput);
		valueOutput.putInt("IdleTimer", this.idleTimer);
		valueOutput.putInt("ScanDelay", this.scanDelay);
		valueOutput.putInt("AttackDelay", this.attackDelay);
	}
	
	@Override
	public void load(ValueInput valueInput) {
		super.load(valueInput);
		this.idleTimer = valueInput.getIntOr("IdleTimer", 0);
		this.scanDelay = valueInput.getIntOr("ScanDelay", 0);
		this.attackDelay = valueInput.getIntOr("AttackDelay", 0);
	}
	
	//TODO Track if stack is not the "best" sword to allow unneeded swords to be removed
	@Override
	public boolean needsItem(ItemStack stack) {
		//Keep Armor if we need that piece
		if(ItemHelper.isArmor(stack, worker)) {
			EquipmentSlot slot = worker.getEquipmentSlotForItem(stack);
			ItemStack workerArmorStack = worker.getItemBySlot(slot);
			if(workerArmorStack.isEmpty()) {
				return true;
			}
		}
		//Always keep food
		return IS_MELEE_WEAPON.test(stack) || stack.has(DataComponents.FOOD) && !ItemHelper.isBadFood(stack, worker);
	}
	
	@Override
	public boolean blockEating() {
		//Eat at 3 seconds
		return this.idleTimer < 3 * 20 || this.attackEntity != null;
	}
	
	@Override
	public void run() {		
		Level level = worker.level();
		if(!level.isClientSide) {
			if(!worker.swapToBestMelee()) {
				attackEntities();
			}			
		}
		
		//Eat or Unload Inventory if idling for 5 seconds
		boolean isIdle = this.idleTimer >= 5 * 20;
		
		if(isIdle) {			
			if(worker.getExternalInventorySettings() != null) {
				//Only check every whole second to prevent unneeded inventory scans
				if((this.idleTimer % 20 == 0) && worker.getExternalInventorySettings().hasInputFace() && worker.hasItemsToUnload()) {
					this.idleTimer = 0;
					worker.startUnloadingInventory(false);
					return;
				}
			}
			
			//Only check every whole second to prevent unneeded inventory scans
			if((this.idleTimer % 20 == 0) && worker.needsArmor()) {
				this.idleTimer = 0;
				worker.equipArmor();
				return;
			}
		}
	}
	
	public boolean isValidTarget(Entity entity) {
		if(entity.isRemoved() || !entity.isAlive()) {
			return false;
		}
//		if (!worker.getSensing().hasLineOfSight(entity)) {
//            return false;
//         }
		if(entity instanceof Player) {
			return false;
		}
		if(entity instanceof Creeper || entity instanceof Ghast) {
			return false;
		}
		if(entity instanceof LivingEntity) {
			LivingEntity living = (LivingEntity)entity;
			if(!living.canBeSeenByAnyone()) {
				return false;
			}
			if(!living.canBeSeenAsEnemy()) {
				return false;
			}
		}
		return entity instanceof Enemy;
	}
	
	public void attackEntities() {
		Level level = worker.level();
		ItemStack handItem = worker.getMainHandItem();
		if(!IS_MELEE_WEAPON.test(handItem))return;
		
		if(this.attackEntity == null) {
			this.idleTimer++;
			
			if(worker.getLastAttacker() !=null) {
				if(this.isValidTarget(worker.getLastAttacker())) {
					this.setTarget(worker.getLastAttacker());
					return;
				}
			}
			
			// Prioritize healing before finding new target
			if(this.worker.isHurt()){
				return;
			}
			
			if(scanDelay > 0) {
				scanDelay--;				
				return;
			}
			
			List<Entity> allEntities = new ArrayList<Entity>();
			this.getBoundingBoxes().forEach((aabb) -> {
				allEntities.addAll(level.getEntities(worker, aabb, this::isValidTarget));
			});
			
			double d0 = -1.0D;
			Entity closestEntity = null;
			for (Entity entity : allEntities) {	
				double d1 = entity.distanceToSqr(worker);
	            if (d0 == -1.0D || d1 < d0) {
	               d0 = d1;
	               closestEntity = entity;
	            }
			}
			
			if(closestEntity !=null) {
				setTarget(closestEntity);
			}
			
			if(this.attackEntity == null) {
				scanDelay = 5 * 20; //Wait 5 seconds to scan again
			}
		}
		else {
			if(!attackEntity.isAttackable() || !attackEntity.isAlive() || attackEntity.isRemoved()) {
				// System.out.println("Removed Target (Dead)");
				worker.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
				worker.getBrain().eraseMemory(MemoryModuleType.PATH);
				this.attackEntity = null;
				return;
			}
			
			
			worker.getLookControl().setLookAt(attackEntity);
			
			if(this.attackDelay > 0) {
				this.attackDelay--;
				return;
			}
			
				
			float multi = 4.0F;
			Vec3 range = new Vec3(worker.getBbWidth() * multi, worker.getBbHeight(), worker.getBbWidth() * multi);
			// TODO Create custom sensing to allow seeing feet
			boolean canSee = this.worker.hasLineOfSight(attackEntity, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attackEntity.getY());
			// System.out.println("Try and attack: " + canSee);
			if(!EntityHelper.isWithinMeleeAttackRange(worker, attackEntity, range) || !canSee) {			
				if(worker.getNavigation().isDone()) {
					// System.out.println("Building Path to Target");
					BlockPos attackPos = this.attackEntity.blockPosition();
					BlockPos validBlockPos = EntityHelper.findWalkableAtOrAdjacentPos(worker, attackPos);
					worker.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(validBlockPos, 1.0F, 0));
				}
			}
			else if(EntityHelper.isLookingAtHorizontally(worker, this.attackEntity, 20)){
				this.swingSword(attackEntity);
			}
		}
	}
	
	public void setTarget(Entity entity) {
		if(entity instanceof LivingEntity) {
			LivingChangeTargetEvent changeTargetEvent = CommonHooks.onLivingChangeTarget(worker, (LivingEntity)entity, LivingChangeTargetEvent.LivingTargetType.MOB_TARGET);
			if(!changeTargetEvent.isCanceled()) {
				if(this.isValidTarget(changeTargetEvent.getNewAboutToBeSetTarget())) {
					this.attackEntity = changeTargetEvent.getNewAboutToBeSetTarget();
				}
			}
		}
		else {
			this.attackEntity = entity;
		}
		
		if(this.attackEntity !=null) {
	        EntityTracker entityTracker = new EntityTracker(this.attackEntity, true);
	        worker.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, entityTracker);
			
			this.attackDelay = 0;
			this.idleTimer = 0;
		}
	}
	
	protected double getAttackReachSqr(Entity target) {
		return (double) (worker.getBbWidth() * 2.0F * worker.getBbWidth() * 2.0F + target.getBbWidth());
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
	public boolean pickupMobDrops(ItemStack stack) {
		return true;
	}
	
	@Override
	public void stop() {
		worker.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		worker.getBrain().eraseMemory(MemoryModuleType.PATH);
		this.attackEntity = null;
	}

	@Override
	public boolean isSame(WorkerJob job) {
		return job instanceof MeleeAttackJob && super.isSame(job);
	}

	@Override
	public JobType getJobType() {
		return JobType.DEFEND_MELEE;
	}

}
