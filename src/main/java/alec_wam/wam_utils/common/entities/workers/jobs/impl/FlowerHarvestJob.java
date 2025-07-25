package alec_wam.wam_utils.common.entities.workers.jobs.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager.JobType;
import alec_wam.wam_utils.common.entities.workers.jobs.MultiBlockPosWorkerJob;
import alec_wam.wam_utils.common.entities.workers.jobs.WorkerJob;
import alec_wam.wam_utils.common.helpers.BlockHelper;
import alec_wam.wam_utils.common.helpers.EntityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class FlowerHarvestJob extends MultiBlockPosWorkerJob {
	private static final Predicate<ItemStack> IS_BONE_MEAL = (stack) -> {
		return stack.is(Items.BONE_MEAL);
	};

    private boolean hasGrownFlowers = false;
	
	public FlowerHarvestJob(WorkerEntity worker, ResourceKey<Level> dimension, List<BlockPos> blockPosList) {
		super(worker, dimension, blockPosList);
	}
	
	public FlowerHarvestJob(WorkerEntity worker, ResourceKey<Level> dimension, BlockPos posA, BlockPos posB) {
		super(worker, dimension, posA, posB);
	}
	
	@Override
	public void save(ValueOutput valueOutput) {
		super.save(valueOutput);
	}
	
	@Override
	public void load(ValueInput valueInput) {
		super.load(valueInput);
	}

	public static FlowerHarvestJob createJob(WorkerEntity worker, ItemStack stack, Player player) {
		return JobManager.createJobFromSelection(
            worker, stack, player,
            posList -> new FlowerHarvestJob(worker, player.level().dimension(), posList),
            (pos1, pos2) -> new FlowerHarvestJob(worker, player.level().dimension(), pos1, pos2)
        );
	}
	
	@Override
	public boolean needsItem(ItemStack stack) {
		return IS_BONE_MEAL.test(stack) || super.needsItem(stack);
	}
	
	@Override
	public void run() {
		super.run();
		if(!worker.level().isClientSide) {
			growAndHarvest();
		}
		
		//Unload Inventory if idling for 5 seconds
		if (this.idleTimer >= 5 * 20 && !this.hasGrownFlowers) {
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
    public int scanSpeed(){
        // Scan way faster when harvesting flowers
        return this.hasGrownFlowers ? 5 : super.scanSpeed();
    }

	@Override	
	public double getInteractionRange(BlockState state) {
		return 2.25D;
	}

	@Override
	public boolean canInteractWithBlock(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
        BlockState belowState = level.getBlockState(pos.below());
		
        if(hasGrownFlowers){
            //TODO Make this flower blocks and grasses
            return state.is(BlockTags.SMALL_FLOWERS) 
                || state.is(Blocks.TALL_GRASS) 
                || state.is(BlockTags.EDIBLE_FOR_SHEEP)
                || state.is(Blocks.AZALEA)
                || state.is(Blocks.FLOWERING_AZALEA)
                || state.is(Blocks.MOSS_CARPET)
                || state.is(Blocks.PALE_MOSS_CARPET);
        }
        //TODO Cache if has bonemeal
        if(this.worker.checkForItem(IS_BONE_MEAL)){
            if(belowState.is(Blocks.GRASS_BLOCK) || belowState.is(Blocks.MOSS_BLOCK) || belowState.is(Blocks.PALE_MOSS_BLOCK)){
                if(belowState.getBlock() instanceof BonemealableBlock bonemealableBlock){
                    return bonemealableBlock.isValidBonemealTarget(level, pos.below(), belowState);
                }
            }
        }
        return false;
	}
	
    @Override
    public void onNoValidBlocksFound() {
        super.onNoValidBlocksFound();
        // Once all of the grown flowers have been harvested, reset to use bonemeal
        if(this.hasGrownFlowers){
            this.hasGrownFlowers = false;
        }
    }

	public void growAndHarvest() {				
		Level level = worker.level();
		if (!level.isClientSide) {				
			if(!this.hasGrownFlowers) {
				if(worker.swapToItem(IS_BONE_MEAL)) {
					return;
				}
			}

		    if(this.getWorkingPos() == null)return;

			lookAtBlock();
			moveToBlock(1);
			
			ItemStack handItem = worker.getMainHandItem();
			if(!this.hasGrownFlowers && !IS_BONE_MEAL.test(handItem)) {
				return;
			}
			
			if (this.isCloseToBlockPos()) {
				BlockPos pos = getWorkingPos();
                // TODO Handle if the entity is inside block and ignore angle
				if(EntityHelper.isLookingAtHorizontally(worker, pos, 20)){
					if(canInteractWithBlock(level, pos)) {						
                        if(!this.hasGrownFlowers){
                            //Grow Flowers
                            BlockPos realPos = getWorkingPos().below();
                            if(BoneMealItem.applyBonemeal(handItem, level, realPos, null)){
                                worker.swing(InteractionHand.MAIN_HAND);
                                this.hasGrownFlowers = true;
                                finishWorking();
                                return;
                            }
                            else {
                                this.invalidateWorkingPos();
                                return;
                            }
                        }
                        else {
                            //Harvest block
                            worker.swing(InteractionHand.MAIN_HAND);
                            List<BlockPos> harvestPosList = new ArrayList<>();
                            harvestPosList.add(pos);
                            BlockState blockState = level.getBlockState(pos);
                            if(blockState.getBlock() instanceof DoublePlantBlock){
                                harvestPosList.add(pos.above());
                            }
                            harvestPosList.forEach(harvestPos -> {                                
                                BlockHelper.destroyBlockAs(level, harvestPos, worker, worker.getMainHandItem(), 1.0F, stack -> {
                                    worker.dropItemFromBrokenBlock(harvestPos, stack, true, true);
                                });
                            });
                            this.finishWorking();
                        }
					}
                    else {
                        this.invalidateWorkingPos();
                        return;
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
		return job instanceof FlowerHarvestJob && super.isSame(job);
	}

	@Override
	public JobType getJobType() {
		return JobType.FLOWERS;
	}

}
