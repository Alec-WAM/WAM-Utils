package alec_wam.wam_utils.common.blocks.conveyor;

import java.util.List;

import org.jetbrains.annotations.UnknownNullability;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.blocks.BaseBE;
import alec_wam.wam_utils.common.blocks.conveyor.ConveyorBeltBE.MovingItem.Phase;
import alec_wam.wam_utils.common.blocks.conveyor.ConveyorBeltBlock.BeltSlope;
import alec_wam.wam_utils.common.blocks.conveyor.splitter.ConveyorSplitterBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class ConveyorBeltBE extends BaseBE {
	public static final AABB SUCK_AABB = Block.column(16.0, 2.0, 4.0).toAabbs().get(0);
	public static final AABB SUCK_AABB_SLOPE = Block.cube(16).toAabbs().get(0);
	public static final int SLOT_SIZE = 1;	
	public static float BELT_SPEED = 0.08F;
	
	public class MovingItem implements INBTSerializable<CompoundTag> {
	    public enum Phase { MOVING_IN, MOVING_OUT }

	    public Direction from;
	    public Direction to;
	    public Phase phase;
	    public float progress;
	    public float prevProgress;

	    public MovingItem() {
	    	//Only for NBT
	    }
	    
	    public MovingItem(Direction from, Direction to) {
	        this.from = from;
	        this.to = to;
	        this.phase = Phase.MOVING_IN;
	        this.progress = 0f;
	        this.prevProgress = 0f;
	    }

	    public boolean isFinished() {
	        return phase == Phase.MOVING_OUT && progress >= 1f;
	    }

		@Override
		public @UnknownNullability CompoundTag serializeNBT(Provider provider) {
			CompoundTag tag = new CompoundTag();
			tag.putInt("from", from.ordinal());
			tag.putInt("to", to.ordinal());
			tag.putFloat("progress", progress);
			tag.putInt("phase", phase.ordinal());
			return tag;
		}

		@Override
		public void deserializeNBT(Provider provider, CompoundTag tag) {
	        this.from = Direction.values()[tag.getIntOr("from", 0)];
	        this.to = Direction.values()[tag.getIntOr("to", 0)];
	        this.progress = tag.getFloatOr("progress", 0.0F);
	        this.phase = Phase.values()[tag.getIntOr("phase", 0)];
		}
	}
	
	
	public MovingItem movingItem = null;
	public boolean clientTransfer = false;
	public ItemStack ghostItem = ItemStack.EMPTY;
	private int ghostDelay = 0;
	private boolean isRedstonePowered = false;

	
	public ConveyorBeltBE(BlockPos pos, BlockState blockState) {
		super(ModInit.CONVEYOR_BELT_BLOCK_ENTITY.get(), pos, blockState);
	}
	
	@Override
	public IItemHandler getItemHandler() {
		return getData(ModInit.ITEM_HANDLER_ATTACHMENT);
	}
	
	@Override
	public int getInventorySize() {
		return SLOT_SIZE;
	}

    @Override
    public void tickClient() {
    	
    	if(this.isRedstonePowered) {
    		if(this.movingItem != null) {
    			movingItem.prevProgress = movingItem.progress;
    		}
    		
    		return;
    	}
    	
		if(this.clientTransfer && this.movingItem == null) {
			if(this.ghostDelay > 0) {
				this.ghostDelay--;
			}
			if(this.ghostDelay <= 0) {
				this.ghostItem = ItemStack.EMPTY;
				this.clientTransfer = false;
			}
		}
    	if(this.movingItem != null) {
    		if (movingItem.progress < 1.0f) {	
    			movingItem.prevProgress = movingItem.progress;
    			movingItem.progress += BELT_SPEED;
    		}
    		else {
    			movingItem.prevProgress = movingItem.progress = 1.0F;
    		}
    		
    		
	        if (movingItem.progress >= 1.0f) {
	        	
	        	if (movingItem.phase == MovingItem.Phase.MOVING_IN) {
	                // Switch to MOVING_OUT phase
	                movingItem.phase = MovingItem.Phase.MOVING_OUT;
	                movingItem.progress = 0f;
	                movingItem.prevProgress = 0f;
	            } else if (movingItem.phase == MovingItem.Phase.MOVING_OUT) {
	            	this.ghostItem = this.getItemHandler().getStackInSlot(0).copy();
		        	this.clientTransfer = true;
		        	this.ghostDelay = 1;
	            }
	        }
    	}
    }

    @Override
    public void tickServer() {
        super.tickServer();
        
        boolean redstonePowered = getLevel().hasNeighborSignal(getBlockPos());
        if(this.isRedstonePowered != redstonePowered) {
        	this.isRedstonePowered = redstonePowered;
        	this.markDirtyClient();
        }
        
        if(!this.isRedstonePowered) {
        	this.suckInItems();
        	this.moveItems();
        }
    }
    
    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if(this.movingItem != null) {
        	tag.put("moveItem", this.movingItem.serializeNBT(provider));
        }
        tag.putBoolean("redstonePowered", isRedstonePowered);
    }
    
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
    	if(tag.contains("moveItem")) {
    		this.movingItem = new MovingItem();
    		this.movingItem.deserializeNBT(provider, tag.getCompoundOrEmpty("moveItem"));
    	}
    	else {
    		this.movingItem = null;
    	}
    	this.isRedstonePowered = tag.getBooleanOr("redstonePowered", false);
    	super.loadAdditional(tag, provider);
    }
    
    public Direction getFacing() {
    	return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

	public InteractionResult playerInteract(Player player) {
		if(player.isCrouching()) {
			IItemHandler handler = this.getItemHandler();
			if(handler !=null) {
				ItemStack beltStack = handler.getStackInSlot(0);
				if(!beltStack.isEmpty()) {
					ItemStack giveStack = handler.extractItem(0, beltStack.getCount(), false);
					ItemHandlerHelper.giveItemToPlayer(player, giveStack);
					return InteractionResult.SUCCESS_SERVER;
				}
			}
		}
		return InteractionResult.PASS;
	}
    
    public boolean isStackValid(ItemStack stack) {
    	return true;
    }
    
    public void suckInItems() {
    	BlockPos pos = this.getBlockPos();
    	IItemHandler handler = this.getItemHandler();
    	if(handler == null || this.movingItem !=null)return;
    	AABB aabb = null;    	
    	BeltSlope beltSlope = this.getBlockState().getValueOrElse(ConveyorBeltBlock.SLOPE, BeltSlope.FLAT);
    	
    	if(beltSlope == BeltSlope.FLAT) {
        	aabb = SUCK_AABB.move(pos.getX(), pos.getY(), pos.getZ());    	
    	}
    	else {
    		aabb = SUCK_AABB_SLOPE.move(pos.getX(), pos.getY(), pos.getZ());
    	}
    	
    	List<ItemEntity> items = getLevel().getEntitiesOfClass(ItemEntity.class, aabb, EntitySelector.ENTITY_STILL_ALIVE);
    	for (ItemEntity itemEntity : items) {
//    		if (itemEntity.hasPickUpDelay())
//                continue;
    		ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty() || !isStackValid(stack)) continue;
            ItemStack leftover = ItemHandlerHelper.insertItemStacked(handler, stack, false);
            if (leftover.isEmpty()) {
                itemEntity.remove(RemovalReason.DISCARDED);
            } else {
                // Otherwise, update the ItemEntity with the modified stack
                itemEntity.setItem(leftover);
            }
            
            this.movingItem = new MovingItem(getFacing().getOpposite(), getFacing());
            //Start from the middle
            this.movingItem.phase = Phase.MOVING_OUT;
            this.markDirtyClient();
            return;
        }
    }
    
    public void moveItems() {
    	IItemHandler handler = this.getItemHandler();
    	if(handler == null || this.movingItem == null)return;
    	
    	ItemStack currentStack = handler.getStackInSlot(0);
    	
    	final boolean hasMoveItem = this.movingItem != null;
    	
    	if(currentStack.isEmpty()) {
    		this.movingItem = null;
    	}
    	else {
//    		Prevent growing while "idle"
    		if (movingItem.progress < 1.0f) {	
    			movingItem.prevProgress = movingItem.progress;
    			movingItem.progress += BELT_SPEED;
    		}
    		
    		
	        if (movingItem.progress >= 1.0f) {
	        	
	        	if (movingItem.phase == MovingItem.Phase.MOVING_IN) {
	                // Switch to MOVING_OUT phase
	                movingItem.phase = MovingItem.Phase.MOVING_OUT;
	                movingItem.progress = 0f;
	                movingItem.prevProgress = 0f;
	            } else if (movingItem.phase == MovingItem.Phase.MOVING_OUT) {
	                // Done: transfer to next belt or inventory
		        	if(this.transferItemToNextLocation()) {
		        		movingItem = null;
		        	}
	            }
	        }
    	}
    	
    	boolean newHasMoveItem = this.movingItem !=null;
    	
    	if(newHasMoveItem != hasMoveItem) {
    		this.markDirtyClient();
    	}
    }
    
    public boolean transferItemToNextLocation() {
    	IItemHandler handler = this.getItemHandler();
    	if(handler == null)return false;
    	
    	ItemStack currentStack = handler.getStackInSlot(0);
    	
    	Direction dir = getFacing();
    	BlockPos blockPos = getBlockPos().relative(dir);
    	
    	BeltSlope beltSlope = this.getBlockState().getValueOrElse(ConveyorBeltBlock.SLOPE, BeltSlope.FLAT);
    	
    	if(beltSlope == BeltSlope.UP) {
    		blockPos = blockPos.above();
    	}
    	else if(beltSlope == BeltSlope.DOWN) {
    		blockPos = blockPos.below();
    	}
    	
    	if(blockPos !=null) {
    		
    		BlockPos[] beltCheckList = new BlockPos[] {blockPos};
    		
    		if(beltSlope == BeltSlope.FLAT) {
    			//Check both in front and below
    			//Transfer to below belt if it is "down" slope and the current belt is flat
    			beltCheckList = new BlockPos[] {blockPos, blockPos.below()};
    		}
    		else if(beltSlope == BeltSlope.DOWN) {
    			//Check both in front and below
    			//Transfer to above belt if it is "flat" slope and the current belt is down
    			beltCheckList = new BlockPos[] {blockPos, blockPos.above()};
    		}
    		
    		for(int i = 0; i < beltCheckList.length; i++) {
    			BlockPos currentBeltPos = beltCheckList[i];
	    		BlockEntity blockEntity = getLevel().getBlockEntity(currentBeltPos);    		
	    		
	    		if(blockEntity instanceof ConveyorBeltBE beltBE) {
	    			final ItemStack fakeItem = handler.extractItem(0, currentStack.getCount(), true);
	    			if(beltBE.canAcceptItem(fakeItem)) {

	    				ItemStack remainder = beltBE.transferItemFromOtherBelt(fakeItem, dir.getOpposite());
	    				int removeAmount = fakeItem.getCount() - remainder.getCount();
	    				
	    				if(removeAmount != 0) {
	    					handler.extractItem(0, removeAmount, false);
	    				}
	    				
	    				return handler.getStackInSlot(0).isEmpty();
	    			}
	    			return false;
	    		}
    		}
    		
    		final int oldCount = currentStack.getCount();
    		ItemStack dropStack = handler.extractItem(0, oldCount, true);    
    		BlockState otherState = getLevel().getBlockState(blockPos);	
    		
    		
    		//Don't allow inventory transfer unless the belt is flat    		
    		if(beltSlope == BeltSlope.FLAT) {
	    		BlockEntity blockEntity = getLevel().getBlockEntity(blockPos);
	    		
	    		if(blockEntity != null && blockEntity instanceof ConveyorSplitterBE splitter) {
	    			if(splitter.canAcceptItem(dropStack, dir.getOpposite())) {
	    				dropStack = splitter.transferFromBelt(dropStack, dir.getOpposite());
	    	    		
	    	    		if(dropStack.getCount() != oldCount) {
	    	    			handler.extractItem(0, oldCount - dropStack.getCount(), false);
	    	    		}
	    				
	    				return dropStack.isEmpty();
	    			}
	    			return false;
	    		}
	    		
    			IItemHandler otherHandler = getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, blockPos, otherState, null, dir.getOpposite());
	    		if(!dropStack.isEmpty() && otherHandler !=null) {
	    			dropStack = ItemHandlerHelper.insertItemStacked(otherHandler, dropStack, false);
	    		}
    		}
    		
    		//TODO Maybe drop in front of belt if below is not air and it is air in front of downward belt
//    		Only drop if empty block
    		if(!dropStack.isEmpty() && otherState.isAir()) {
        		float speed = 0.15f;
        		ItemEntity itementity = new ItemEntity(level, blockPos.getX() + 0.5, blockPos.getY() + 0.1, blockPos.getZ() + 0.5, dropStack);
                itementity.setDeltaMovement(
                        (double) dir.getStepX() * speed,
                        (double) dir.getStepY() * speed,
                        (double) dir.getStepZ() * speed
                );
                itementity.setDefaultPickUpDelay();
                level.addFreshEntity(itementity);
                handler.extractItem(0, oldCount, false);
                return true;
    		}
    		
    		if(dropStack.getCount() != oldCount) {
    			handler.extractItem(0, oldCount - dropStack.getCount(), false);
    		}
    		return handler.getStackInSlot(0).isEmpty();
    	}
    	return false;
    }
    
    public boolean canAcceptItem(ItemStack stack) {
    	//FIXME Disable merging because it resets the progress of the belt instead of showing a 2nd progress
//    	if(this.movingItem !=null) {
//    		return ItemStack.isSameItemSameComponents(this.getItemHandler().getStackInSlot(0), stack);
//    	}
    	return this.movingItem == null && this.getItemHandler().getStackInSlot(0).isEmpty();
    }
    
    public ItemStack transferItemFromOtherBelt(ItemStack stack, Direction from) {
    	ItemStack remainder = stack;
    	Direction to = getFacing();
        if (canAcceptItem(stack)) {
        	remainder = this.getItemHandler().insertItem(0, stack, false);
            movingItem = new MovingItem(from, to);
            this.markDirtyClient();
        }
        return remainder;
    }

}
