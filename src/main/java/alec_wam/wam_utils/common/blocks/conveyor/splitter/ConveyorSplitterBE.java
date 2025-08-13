package alec_wam.wam_utils.common.blocks.conveyor.splitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.blocks.BaseBE;
import alec_wam.wam_utils.common.blocks.conveyor.ConveyorBeltBE;
import alec_wam.wam_utils.common.blocks.conveyor.ItemFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ConveyorSplitterBE extends BaseBE {
	
	
	public static class ItemSplitterEntry {
		private List<Direction> validDirections;
		private int rrIndex = 0;
		
		public ItemSplitterEntry(List<Direction> directions) {
			this.validDirections = directions;
		}
		
		public List<Direction> getValidDirections() {
			return validDirections;
		}
		
		public Direction getNextDirection() {
			if(this.validDirections.size() == 1) {
				return this.validDirections.get(0);
			}
			else if(this.validDirections.size() >= 2) {
				this.incrementRoundRobin();
				return this.validDirections.get(rrIndex);
			}
			return null;
		}
		
		public Direction getCurrentDirection() {
			if(this.validDirections.size() == 1) {
				return this.validDirections.get(0);
			}
			else if(this.validDirections.size() >= 2) {
				int index = this.rrIndex <= 0 ? 0 : this.rrIndex;
				return this.validDirections.get(index);
			}
			return null;
		}
		
		public void incrementRoundRobin() {
			if(this.validDirections.size() == 1) {
				this.rrIndex = 0;
			}
			else {
				this.rrIndex++;
				this.rrIndex %= this.validDirections.size();
			}
		}
	}
	
	public static final int SLOT_SIZE = 1;
	
	public Map<Direction, ItemFilter> itemFilters = new HashMap<>();
	public Map<ItemStackKey, ItemSplitterEntry> itemRouteCache = new HashMap<>();

	private ItemStackHandler inventory = new ItemStackHandler(SLOT_SIZE);
	
	public ConveyorSplitterBE(BlockPos pos, BlockState blockState) {
		super(ModInit.CONVEYOR_SPLITTER_BLOCK_ENTITY.get(), pos, blockState);
	}	
	
	//TODO Change this to an ItemHandler that only accepts allowed items
	

    @Override
    public ItemStackHandler getInternalInventory() {
        return this.inventory;
    }

	@Override
	public IItemHandler getExternalItemHandler(@Nullable Direction side) {
		return inventory;
	}

	@Override
    public void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        if(!itemFilters.isEmpty()) {
			ValueOutput.ValueOutputList listValue = valueOutput.childrenList("filters");
			for(Entry<Direction, ItemFilter> entry : this.itemFilters.entrySet()) {
				ValueOutput childOutput = listValue.addChild();
				childOutput.putString("direction", entry.getKey().getName());
				childOutput.store("filter", ItemFilter.CODEC, entry.getValue());
			}
        }
    }
    
    @Override
    public void loadAdditional(ValueInput valueInput) {
    	this.itemFilters.clear();
		ValueInput.ValueInputList listValue = valueInput.childrenListOrEmpty("filters");
		for(ValueInput childInput : listValue) {
			Direction dir = Direction.byName(childInput.getStringOr("direction", "north"));
			ItemFilter filter = childInput.read("filter", ItemFilter.CODEC).orElse(null);
			if(filter !=null) {
				this.itemFilters.put(dir, filter);
			}
		}
    	// if(tag.contains("filters")) {
    	// 	ListTag tagList = tag.getListOrEmpty("filters");
    	// 	for(int i = 0; i < tagList.size(); i++) {
    	// 		CompoundTag filterTag = tagList.getCompoundOrEmpty(i);
    	// 		if(!filterTag.isEmpty()) {
    	// 			Direction dir = Direction.byName(filterTag.getStringOr("direction", "north"));
    	// 			ItemFilter filter = new ItemFilter();
    	// 			filter.deserializeNBT(provider, filterTag.getCompoundOrEmpty("filter"));
    	// 			if(filter.type !=null) {
    	// 				this.itemFilters.put(dir, filter);
    	// 			}
    	// 		}
    	// 	}
    	// }

    	super.loadAdditional(valueInput);
    }

	public InteractionResult playerInteract(Player player, BlockHitResult hitResult, @Nullable ItemStack stack, @Nullable InteractionHand hand) {
		Direction dir = hitResult.getDirection();
		if(player.isCrouching()) {
			if(stack == null || stack.isEmpty()) {
				IItemHandler handler = this.inventory;
				if(handler !=null) {
					ItemStack beltStack = handler.getStackInSlot(0);
					if(!beltStack.isEmpty()) {
						ItemStack giveStack = handler.extractItem(0, beltStack.getCount(), false);
						ItemHandlerHelper.giveItemToPlayer(player, giveStack);
						return InteractionResult.SUCCESS_SERVER;
					}
				}
			}
		}
		else {
			if(stack == null || stack.isEmpty()) {
				ItemFilter filter = this.itemFilters.get(dir);
				Component message = null;
				if(filter == null) {
					message = Component.literal(dir.getName() + ": No Filter");
				}
				else {
					String valueStr = filter.getType() == ItemFilter.FilterType.ITEM ? (filter.getStack().isPresent() ? filter.getStack().get().getDisplayName().getString() : "Empty") : filter.getItemTag().orElse("No Tag");
					message = Component.literal(dir.getName() + "(" + filter.getType().name() + "): " + valueStr);
				}
				player.displayClientMessage(message, true);
	        	return InteractionResult.SUCCESS_SERVER;
			}
			else {
				if(stack.is(Items.STICK)) {
					if(this.itemFilters.containsKey(dir)) {
						this.itemFilters.remove(dir);
						this.clearRouteCache();
						return InteractionResult.SUCCESS_SERVER;
					}
				}
				else if(stack.is(Items.NAME_TAG)) {
					Component component = stack.get(DataComponents.CUSTOM_NAME);
			        if (component != null) {
			        	ItemFilter filter = ItemFilter.itemTagFilter(component.getString().toLowerCase(), true);
			        	this.itemFilters.put(dir, filter);
						this.clearRouteCache();
			        	this.markDirtyClient();
			        	return InteractionResult.SUCCESS_SERVER;
			        }
				}
				else {
					ItemStack copyStack = stack.copyWithCount(1);
					ItemFilter filter = ItemFilter.itemStackFilter(copyStack, true);
		        	this.itemFilters.put(dir, filter);
					this.clearRouteCache();
		        	this.markDirtyClient();
		        	return InteractionResult.SUCCESS_SERVER;
				}
			}
		}
		return InteractionResult.PASS;
	}
	
	public boolean canAcceptItem(ItemStack stack, Direction from) {
		//TODO Only accept items that are able to go to a valid location
		return true;
	}
	
	public void clearRouteCache() {
		this.itemRouteCache.clear();
	}
	
	public ItemSplitterEntry getSplitterEntry(ItemStack stack) {
		ItemStackKey itemKey = new ItemStackKey(stack, true);
		//TODO Handle Splitting and random not just round robin
		if(this.itemRouteCache.containsKey(itemKey)) {			
			return this.itemRouteCache.get(itemKey);
		}
		else {
			List<Direction> validDirections = new ArrayList<>();
			boolean filtersOnly = false;
			for(Direction dir : Direction.Plane.HORIZONTAL) {
				BlockPos pos = this.getBlockPos().relative(dir);
				BlockEntity be = getLevel().getBlockEntity(pos);
				if(be instanceof ConveyorBeltBE) {
					Direction facing = getLevel().getBlockState(pos).getValue(BlockStateProperties.HORIZONTAL_FACING);
					//Going "out" not in
					if(facing != dir.getOpposite()) {
						ItemFilter filter = this.itemFilters.get(dir);
						if(filter !=null) {
							if(!filter.passesFilter(stack)) {
								continue;
							}
							filtersOnly = true;
						}
						validDirections.add(dir);
					}
				}
			}
			
			if(filtersOnly) {
				validDirections = validDirections.stream().filter(this.itemFilters::containsKey).toList();
			}
			
			if(!validDirections.isEmpty()) {
				ItemSplitterEntry cache = new ItemSplitterEntry(validDirections);
				this.itemRouteCache.put(itemKey, cache);
				return cache;
			}
		}		
		return null;
	}
	
	public ItemStack transferFromBelt(ItemStack stack, Direction from) {
		ItemStack remainder = stack;
//		for(Direction dir : Direction.Plane.HORIZONTAL) {
//			//TODO Prioritize filtered locations
//			if(dir == from) {
//				continue;
//			}
//			
//			ItemFilter filter = this.itemFilters.get(dir);
//			if(filter !=null) {
//				if(!filter.itemMatches(stack)) {
//					continue;
//				}
//			}
//			
//			BlockPos pos = this.getBlockPos().relative(dir);
//			BlockEntity be = getLevel().getBlockEntity(pos);
//			
//			if(be != null && be instanceof ConveyorBeltBE belt) {
//				if(belt.canAcceptItem(remainder)) {
//					remainder = belt.transferItemFromOtherBelt(remainder, dir.getOpposite());
//				}
//			}
//			
//			if(remainder.isEmpty()) {
//				break;
//			}
//		}
		ItemSplitterEntry splitterEntry = this.getSplitterEntry(stack);
		if(splitterEntry !=null) {
			Direction dir = splitterEntry.getCurrentDirection();
			if(dir !=null) {
				BlockPos pos = this.getBlockPos().relative(dir);
				BlockEntity be = getLevel().getBlockEntity(pos);
				
				//TODO Add a "non-strict" round robin mode to allow the splitter to move to the next valid location
				if(be != null && be instanceof ConveyorBeltBE belt) {
					if(belt.canAcceptItem(remainder)) {
						remainder = belt.transferItemFromOtherBelt(remainder, dir.getOpposite());
						splitterEntry.incrementRoundRobin();
					}
				}
			}
		}
		return remainder;
	}

}
