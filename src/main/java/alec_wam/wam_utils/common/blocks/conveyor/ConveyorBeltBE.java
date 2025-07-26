package alec_wam.wam_utils.common.blocks.conveyor;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.blocks.BaseBE;
import alec_wam.wam_utils.common.blocks.conveyor.ConveyorBeltBE.MovingItem.Phase;
import alec_wam.wam_utils.common.blocks.conveyor.ConveyorBeltBlock.BeltSlope;
import alec_wam.wam_utils.common.blocks.conveyor.splitter.ConveyorSplitterBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ConveyorBeltBE extends BaseBE {
	public static final AABB SUCK_AABB = Block.column(16.0, 2.0, 4.0).toAabbs().get(0);
	public static final AABB SUCK_AABB_SLOPE = Block.cube(16).toAabbs().get(0);
	public static final int SLOT_SIZE = 1;	
	public static float BELT_SPEED = 0.08F;
	
	public static class MovingItem {
	    public enum Phase { MOVING_IN, MOVING_OUT }

		public static final Codec<MovingItem> CODEC = RecordCodecBuilder.create(instance -> // Given an instance
			instance.group( // Define the fields within the instance
				Direction.CODEC.fieldOf("from").forGetter((MovingItem item) -> item.from),
				Direction.CODEC.fieldOf("to").forGetter((MovingItem item) -> item.to),
				Codec.STRING.fieldOf("phaseStr").forGetter((MovingItem item) -> item.phase.name()),
				Codec.FLOAT.fieldOf("progress").forGetter((MovingItem item) -> item.progress),
				Codec.FLOAT.fieldOf("prevProgress").forGetter((MovingItem item) -> item.prevProgress)
			).apply(instance, MovingItem::new) // Define how to create the object
		);

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

		public MovingItem(Direction from, Direction to, String phaseStr, float progress, float prevProgress) {
	        this.from = from;
	        this.to = to;
	        this.phase = phaseStr != null ? Phase.valueOf(phaseStr) : Phase.MOVING_IN;
	        this.progress = progress;
	        this.prevProgress = prevProgress;
	    }

	    public boolean isFinished() {
	        return phase == Phase.MOVING_OUT && progress >= 1f;
	    }
	}
	
	
	public MovingItem movingItem = null;
	public boolean clientTransfer = false;
	public ItemStack ghostItem = ItemStack.EMPTY;
	private int ghostDelay = 0;
	private boolean isRedstonePowered = false;
	private ItemStackHandler inventory = new ItemStackHandler(SLOT_SIZE);

	
	public ConveyorBeltBE(BlockPos pos, BlockState blockState) {
		super(ModInit.CONVEYOR_BELT_BLOCK_ENTITY.get(), pos, blockState);
	}

	@Override
	public ItemStackHandler getItemHandler(@Nullable Direction side) {
		return inventory;
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
	            	this.ghostItem = this.inventory.getStackInSlot(0).copy();
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
    public void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
		valueOutput.storeNullable("moveItem", MovingItem.CODEC, this.movingItem);
        valueOutput.putBoolean("redstonePowered", isRedstonePowered);
    }
    
    @Override
    public void loadAdditional(ValueInput valueInput) {
		this.movingItem = valueInput.read("moveItem", MovingItem.CODEC).orElse(null);
    	this.isRedstonePowered = valueInput.getBooleanOr("redstonePowered", false);
    	super.loadAdditional(valueInput);
    }
    
    public Direction getFacing() {
    	return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

	public InteractionResult playerInteract(Player player) {
		if(player.isCrouching()) {
			if(this.inventory !=null) {
				ItemStack beltStack = this.inventory.getStackInSlot(0);
				if(!beltStack.isEmpty()) {
					ItemStack giveStack = this.inventory.extractItem(0, beltStack.getCount(), false);
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
    	if(this.inventory == null || this.movingItem !=null)return;
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
            ItemStack leftover = ItemHandlerHelper.insertItemStacked(this.inventory, stack, false);
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
    	if(this.inventory == null || this.movingItem == null)return;
    	
    	ItemStack currentStack = this.inventory.getStackInSlot(0);
    	
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
    	if(this.inventory == null)return false;
    	
    	ItemStack currentStack = this.inventory.getStackInSlot(0);
    	
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
	    			final ItemStack fakeItem = this.inventory.extractItem(0, currentStack.getCount(), true);
	    			if(beltBE.canAcceptItem(fakeItem)) {

	    				ItemStack remainder = beltBE.transferItemFromOtherBelt(fakeItem, dir.getOpposite());
	    				int removeAmount = fakeItem.getCount() - remainder.getCount();
	    				
	    				if(removeAmount != 0) {
	    					this.inventory.extractItem(0, removeAmount, false);
	    				}
	    				
	    				return this.inventory.getStackInSlot(0).isEmpty();
	    			}
	    			return false;
	    		}
    		}
    		
    		final int oldCount = currentStack.getCount();
    		ItemStack dropStack = this.inventory.extractItem(0, oldCount, true);    
    		BlockState otherState = getLevel().getBlockState(blockPos);	
    		
    		
    		//Don't allow inventory transfer unless the belt is flat    		
    		if(beltSlope == BeltSlope.FLAT) {
	    		BlockEntity blockEntity = getLevel().getBlockEntity(blockPos);
	    		
	    		if(blockEntity != null && blockEntity instanceof ConveyorSplitterBE splitter) {
	    			if(splitter.canAcceptItem(dropStack, dir.getOpposite())) {
	    				dropStack = splitter.transferFromBelt(dropStack, dir.getOpposite());
	    	    		
	    	    		if(dropStack.getCount() != oldCount) {
	    	    			this.inventory.extractItem(0, oldCount - dropStack.getCount(), false);
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
                this.inventory.extractItem(0, oldCount, false);
                return true;
    		}
    		
    		if(dropStack.getCount() != oldCount) {
    			this.inventory.extractItem(0, oldCount - dropStack.getCount(), false);
    		}
    		return this.inventory.getStackInSlot(0).isEmpty();
    	}
    	return false;
    }
    
    public boolean canAcceptItem(ItemStack stack) {
    	//FIXME Disable merging because it resets the progress of the belt instead of showing a 2nd progress
//    	if(this.movingItem !=null) {
//    		return ItemStack.isSameItemSameComponents(this.getItemHandler().getStackInSlot(0), stack);
//    	}
    	return this.movingItem == null && this.inventory.getStackInSlot(0).isEmpty();
    }
    
    public ItemStack transferItemFromOtherBelt(ItemStack stack, Direction from) {
    	ItemStack remainder = stack;
    	Direction to = getFacing();
        if (canAcceptItem(stack)) {
        	remainder = this.inventory.insertItem(0, stack, false);
            movingItem = new MovingItem(from, to);
            this.markDirtyClient();
        }
        return remainder;
    }

}
