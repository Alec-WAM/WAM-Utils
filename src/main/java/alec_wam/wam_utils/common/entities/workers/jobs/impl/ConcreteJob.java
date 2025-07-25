package alec_wam.wam_utils.common.entities.workers.jobs.impl;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.jobs.BreakBlockWorkerJob;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager.JobType;
import alec_wam.wam_utils.common.entities.workers.jobs.WorkerJob;
import alec_wam.wam_utils.common.helpers.BlockHelper;
import alec_wam.wam_utils.common.helpers.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;

public class ConcreteJob extends BreakBlockWorkerJob {
    private static final Vec3i ITEM_PICKUP_REACH = new Vec3i(1, 0, 1);

    private static final Predicate<ItemStack> IS_CONCREATE_POWDER = (stack) -> {
		return stack.is(Tags.Items.CONCRETE_POWDERS);
	};
    private static final Predicate<ItemStack> IS_SOLID_CONCREATE = (stack) -> {
		return stack.is(Tags.Items.CONCRETES);
	};
    private static final Predicate<BlockState> IS_SOLID_CONCREATE_BLOCK = (state) -> {
        return state.is(Tags.Blocks.CONCRETES);
    };
    
	private int itemScanDelay = 0;
	private ItemEntity pickupEntity;
	
	public ConcreteJob(WorkerEntity worker, ResourceKey<Level> dimension, List<BlockPos> blockPosList) {
		super(worker, dimension, blockPosList);
	}
	
	public ConcreteJob(WorkerEntity worker, ResourceKey<Level> dimension, BlockPos posA, BlockPos posB) {
		super(worker, dimension, posA, posB);
	}
	
	@Override
	public void save(ValueOutput valueOutput) {
		super.save(valueOutput);
        valueOutput.putInt("ItemScanDelay", this.itemScanDelay);
	}
	
	@Override
	public void load(ValueInput valueInput) {
		super.load(valueInput);
        this.itemScanDelay = valueInput.getIntOr("ItemScanDelay", 0);
	}

	public static ConcreteJob createJob(WorkerEntity worker, ItemStack stack, Player player) {
		return JobManager.createJobFromSelection(
            worker, stack, player,
            posList -> new ConcreteJob(worker, player.level().dimension(), posList),
            (pos1, pos2) -> new ConcreteJob(worker, player.level().dimension(), pos1, pos2)
        );
	}
	
	@Override
	public boolean needsItem(ItemStack stack) {
		return IS_CONCREATE_POWDER.test(stack) || super.needsItem(stack);
	}
	
	@Override
	public void run() {
		super.run();
		Level level = worker.level();
		if(!level.isClientSide) {
			pickUpItems();
		}
		placeConcreate();

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
		return 2.0D;
	}

	@Override
	public boolean canInteractWithBlock(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		FluidState fluidState = level.getFluidState(pos);
		
		if(fluidState.is(FluidTags.WATER)) {
			if(state.canBeReplaced()) {
				if(!ItemHelper.findItem(worker, IS_CONCREATE_POWDER).isEmpty()) {
					return true;
				}
			}
		}
		
		if(BlockHelper.shouldSolidify(level, pos, state, fluidState)) {
			if(!ItemHelper.findItem(worker, IS_CONCREATE_POWDER).isEmpty()) {
				return true;
			}
		}
		
		if(IS_SOLID_CONCREATE_BLOCK.test(state)) {
			//TODO Cache this
			if(!ItemHelper.findBestTool(worker, state).isEmpty()) {
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public void tickBlockBreak() {
		if(this.getWorkingPos() !=null) {
			BlockState state = worker.level().getBlockState(getWorkingPos());
			if(!IS_SOLID_CONCREATE_BLOCK.test(state)) {
				return;
			}
		}
        if(this.pickupEntity !=null){
            return; // Don't break blocks while picking up items
        }

//		System.out.println("Break Block");
		super.tickBlockBreak();
	}
	
	public void placeConcreate() {
		Level level = worker.level();
		if (!level.isClientSide) {
			worker.requireItem(IS_CONCREATE_POWDER);
			
			if(this.getWorkingPos() == null)return;
			BlockPos pos = this.getWorkingPos();
			BlockState state = level.getBlockState(pos);
			
			if(IS_SOLID_CONCREATE_BLOCK.test(state)) {
				return;
			}
			

//			System.out.println("Place Concrete");
			
			if (worker.swapToItem(IS_CONCREATE_POWDER)) {
				return;
			}
			
			ItemStack handItem = worker.getMainHandItem();
			
			if(handItem.isEmpty() || (!IS_CONCREATE_POWDER.test(handItem))) {
				return;
			}
			
			if (this.isCloseToBlockPos()) {				
				if(BlockHelper.placeBlock(worker.level(), pos, handItem, worker, InteractionHand.MAIN_HAND, Direction.UP, true, true)) {
					worker.swing(InteractionHand.MAIN_HAND);
				}
				this.finishWorking();
			}
		}
	}
	
    @Override
    public boolean shouldScanForBlocks(){
        return this.pickupEntity == null;
    }

    public boolean canPickUpItem(ItemEntity itemEntity) {
        return itemEntity != null && !itemEntity.isRemoved() && !itemEntity.getItem().isEmpty() && !itemEntity.hasPickUpDelay()
                && this.wantsToPickUp(itemEntity.getItem());
    }
	
	public boolean wantsToPickUp(ItemStack stack) {
		if(!IS_SOLID_CONCREATE.test(stack)) return false;
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

    public void pickUpItems() {
        Level level = worker.level();
        pickupNearbyItems();

        if(ItemHelper.findItem(worker, IS_CONCREATE_POWDER).isEmpty()) {
            // No concrete powder, so pick up items
            if(this.pickupEntity == null) {
                this.idleTimer++;
                if(this.itemScanDelay > 0) {
                    this.itemScanDelay--;				
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
                    this.itemScanDelay = 5 * 20; //Wait 5 seconds to scan again
                }
            }
            else {
                if(!this.canPickUpItem(this.pickupEntity)) {
                    this.pickupEntity = null;
                    return;
                }		

                worker.getLookControl().setLookAt(this.pickupEntity);
                
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
    }
	
	@Override
	public void stop() {
		super.stop();
	}

	@Override
	public boolean isSame(WorkerJob job) {
		return job instanceof ConcreteJob && super.isSame(job);
	}

	@Override
	public JobType getJobType() {
		return JobType.CONCRETE;
	}

}
