package alec_wam.wam_utils.common.entities.workers.jobs.impl;

import java.util.List;
import java.util.function.Predicate;

import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager.JobType;
import alec_wam.wam_utils.common.entities.workers.jobs.MultiBlockPosWorkerJob;
import alec_wam.wam_utils.common.entities.workers.jobs.WorkerJob;
import alec_wam.wam_utils.common.helpers.BlockHelper;
import alec_wam.wam_utils.common.helpers.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BeeHiveJob extends MultiBlockPosWorkerJob {
	private static final Predicate<ItemStack> IS_SHEARS = (stack) -> {
		return ItemHelper.isShears(stack);
	};
	private static final Predicate<ItemStack> IS_GLASS_BOTTLE = (stack) -> {
		return stack.is(Items.GLASS_BOTTLE);
	};	
	
	private byte harvestType = -1;
	
	public BeeHiveJob(WorkerEntity worker, ResourceKey<Level> dimension, List<BlockPos> blockPosList) {
		super(worker, dimension, blockPosList);
	}
	
	public BeeHiveJob(WorkerEntity worker, ResourceKey<Level> dimension, BlockPos posA, BlockPos posB) {
		super(worker, dimension, posA, posB);
	}
	
	@Override
	public void save(ValueOutput valueOutput) {
		super.save(valueOutput);
		valueOutput.putByte("HarvestType", this.harvestType);
	}
	
	@Override
	public void load(ValueInput valueInput) {
		super.load(valueInput);
		this.harvestType = valueInput.getByteOr("HarvestType", (byte)-1);
	}

	public static BeeHiveJob createJob(WorkerEntity worker, ItemStack stack, Player player) {
		return JobManager.createJobFromSelection(
            worker, stack, player,
            posList -> new BeeHiveJob(worker, player.level().dimension(), posList),
            (pos1, pos2) -> new BeeHiveJob(worker, player.level().dimension(), pos1, pos2)
        );
	}
	
	@Override
	public boolean needsItem(ItemStack stack) {
		// Prevent eating these items
		if(stack.is(Items.HONEY_BOTTLE) || stack.is(Items.HONEYCOMB)) {
			return false;
		}
		return IS_SHEARS.test(stack) || IS_GLASS_BOTTLE.test(stack) || super.needsItem(stack);
	}
	
	@Override
	public void run() {
		super.run();
		if(!worker.level().isClientSide) {
			harvestBeehive();
		}
		
		//Unload Inventory if idling for 5 seconds
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
	
	@Override	
	public double getInteractionRange(BlockState state) {
		return 2.25D;
	}

	@Override
	public boolean canInteractWithBlock(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return BlockHelper.IS_FULL_BEEHIVE.test(state);
	}
	
	public void pickHarvestType() {
		boolean hasShears = !ItemHelper.findItem(worker, IS_SHEARS).isEmpty();
		boolean hasBottle = !ItemHelper.findItem(worker, IS_GLASS_BOTTLE).isEmpty();
		
		if(hasShears && hasBottle) {
			this.harvestType = (byte)(worker.getRandom().nextBoolean() ? 1 : 0);
//			System.out.println("Picked Harvest Type: " + this.harvestType);
		}
		else if(hasShears) {
			this.harvestType = 1;
//			System.out.println("Picked Harvest Type: " + this.harvestType);
		}
		else if(hasBottle) {
			this.harvestType = 0;
//			System.out.println("Picked Harvest Type: " + this.harvestType);
		}
	}
	
	public void harvestBeehive() {				
		if(this.harvestType == -1) {			
			// Grab Items if possible
			worker.requireItem(IS_SHEARS, ItemHelper.BEST_ITEM_SORTER);
			worker.requireItem(IS_GLASS_BOTTLE);
			// TODO Wait a few seconds to check again
			pickHarvestType();
			return;
		}
		if(this.getWorkingPos() == null)return;
		Level level = worker.level();
		if (!level.isClientSide) {				
			if(harvestType == 1) {
				if(worker.swapToItem(IS_SHEARS, ItemHelper.BEST_ITEM_SORTER)) {
					this.harvestType = -1;
					return;
				}
			}
			else if(harvestType == 0) {
				if(worker.swapToItem(IS_GLASS_BOTTLE)) {
					this.harvestType = -1;
					return;
				}
			}
			
			ItemStack handItem = worker.getMainHandItem();
			if(handItem.isEmpty() || (!IS_SHEARS.test(handItem) && !IS_GLASS_BOTTLE.test(handItem))) {
				return;
			}
			lookAtBlock();
			moveToBlock(1);
			
			if (this.isCloseToBlockPos()) {
				BlockPos pos = getWorkingPos();
				if(canInteractWithBlock(level, pos)) {
					boolean harvested = false;
					if(IS_SHEARS.test(handItem)) {
						level.playSound(null, worker.getX(), worker.getY(), worker.getZ(), SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
			            worker.putInInventory(new ItemStack(Items.HONEYCOMB, 3));
			            handItem.hurtAndBreak(1, worker, EquipmentSlot.MAINHAND);
			            harvested = true;
			            level.gameEvent(worker, GameEvent.SHEAR, pos);
					}
					else if(IS_GLASS_BOTTLE.test(handItem)) {
						handItem.shrink(1);
						level.playSound(null, worker.getX(), worker.getY(), worker.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
			            if (handItem.isEmpty()) {
			               worker.holdInMainHand(new ItemStack(Items.HONEY_BOTTLE));
			            } else {
			               worker.putInInventory(new ItemStack(Items.HONEY_BOTTLE));
			            }

			            harvested = true;
			            level.gameEvent(worker, GameEvent.FLUID_PICKUP, pos);
					}
					
					if(harvested) {
						//TODO Anger Bees
						BlockState state = level.getBlockState(pos);
						if(state.hasProperty(BeehiveBlock.HONEY_LEVEL)) {
							level.setBlock(pos, state.setValue(BeehiveBlock.HONEY_LEVEL, Integer.valueOf(0)), 3);
						}
						this.harvestType = -1;
						this.finishWorking();
					}
				}
			}
		}
	}
	
	@Override
	public void stop() {
		
	}

	@Override
	public boolean isSame(WorkerJob job) {
		return job instanceof BeeHiveJob && super.isSame(job);
	}

	@Override
	public JobType getJobType() {
		return JobType.BEEHIVE;
	}

}
