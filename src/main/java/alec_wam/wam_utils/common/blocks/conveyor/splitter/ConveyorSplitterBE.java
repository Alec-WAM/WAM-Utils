package alec_wam.wam_utils.common.blocks.conveyor.splitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.jetbrains.annotations.UnknownNullability;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.blocks.BaseBE;
import alec_wam.wam_utils.common.blocks.conveyor.ConveyorBeltBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class ConveyorSplitterBE extends BaseBE {

	public static class ItemFilter implements INBTSerializable<CompoundTag> {

		public static enum FilterType {
			ITEM, TAG;
		}
		
		public FilterType type;
		public ItemStack stack;
		public String itemTag;
		
		public ItemFilter() {}
		
		public ItemFilter setItemStack(ItemStack stack) {
			this.type = FilterType.ITEM;
			this.stack = stack;
			return this;
		}
		
		public ItemFilter setItemTag(String tag) {
			this.type = FilterType.TAG;
			this.itemTag = tag;
			return this;
		}
		
		public boolean itemMatches(ItemStack stack) {
			if(type == FilterType.ITEM) {
				//TODO Make this able to be "fuzzy" instead of exact
				return ItemStack.isSameItemSameComponents(this.stack, stack);
			}
			else if(type == FilterType.TAG) {
				return stack.getTags().anyMatch(this::tagMatches);
			}
			return false;
		}
		
		private boolean tagMatches(TagKey<Item> tagKey) {
			if(this.itemTag == null) {
				return false;
			}
			String otherTag = tagKey.location().toString().toLowerCase(Locale.ROOT);
			return this.itemTag.equalsIgnoreCase(otherTag);
		}
		
		@Override
		public @UnknownNullability CompoundTag serializeNBT(Provider provider) {
			CompoundTag tag = new CompoundTag();
			if(this.type !=null) {
				tag.putByte("type", (byte)this.type.ordinal());
				if(type == FilterType.ITEM) {
					tag.put("itemstack", this.stack.save(provider));
				}
				else if(type == FilterType.TAG) {
					tag.putString("itemtag", this.itemTag);
				}
			}
			return tag;
		}

		@Override
		public void deserializeNBT(Provider provider, CompoundTag tag) {
	        if(tag.contains("type")) {
	        	this.type = FilterType.values()[tag.getByteOr("type", (byte)0)];
	        	
	        	if(this.type == FilterType.ITEM) {
	        		this.stack = ItemStack.parse(provider, tag.getCompoundOrEmpty("itemstack")).orElse(ItemStack.EMPTY);
	        	}
	        	else if(this.type == FilterType.TAG) {
	        		this.itemTag = tag.getStringOr("itemtag", itemTag);
	        	}
	        }
		}
		
	}
	
	public static class ItemFilterList implements INBTSerializable<CompoundTag> {

		private List<ItemFilter> filters;
		
		public ItemFilterList() {
			this.filters = new ArrayList<>();
		}
		
		public ItemFilterList(List<ItemFilter> filters) {
			this.filters = filters;
		}
		
		public boolean itemMatches(ItemStack stack) {
			return this.filters.stream().anyMatch(filter -> filter.itemMatches(stack));
		}
		
		public ItemFilter addTagFilter(String string) {
			ItemFilter tagFilter = new ItemFilter().setItemTag(string);
			this.addFilter(tagFilter);
			return tagFilter;
		}
		
		public ItemFilter addItemStackFilter(ItemStack stack) {
			ItemFilter tagFilter = new ItemFilter().setItemStack(stack);
			this.addFilter(tagFilter);
			return tagFilter;
		}
		
		public void addFilter(ItemFilter filter) {
			this.filters.add(filter);
		}
		
		public void removeFilter(int index) {
			this.filters.remove(index);
		}
		
		@Override
		public @UnknownNullability CompoundTag serializeNBT(Provider provider) {
			CompoundTag tag = new CompoundTag();
			if(this.filters !=null) {
				ListTag filterTagList = new ListTag();
				for(ItemFilter filter : this.filters) {
					filterTagList.add(filter.serializeNBT(provider));
				}
				tag.put("filters", filterTagList);
			}
			return tag;
		}

		@Override
		public void deserializeNBT(Provider provider, CompoundTag tag) {
			if(this.filters !=null) {
				this.filters.clear();
			}
			else {
				this.filters = new ArrayList<>();
			}
			
			if(tag.contains("filters")) {
				ListTag filterTagList = tag.getListOrEmpty("filters");
				for(int i = 0; i < filterTagList.size(); i++) {
					CompoundTag filterTag = filterTagList.getCompoundOrEmpty(i);
					ItemFilter filter = new ItemFilter();
					filter.deserializeNBT(provider, filterTag);
					this.filters.add(filter);
				}
			}
		}
		
	}
	
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
	
	public ConveyorSplitterBE(BlockPos pos, BlockState blockState) {
		super(ModInit.CONVEYOR_SPLITTER_BLOCK_ENTITY.get(), pos, blockState);
	}	
	
	@Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if(!itemFilters.isEmpty()) {
        	ListTag tagList = new ListTag();
        	for(Entry<Direction, ItemFilter> entry : this.itemFilters.entrySet()) {
        		CompoundTag filterTag = new CompoundTag();
        		filterTag.putString("direction", entry.getKey().getName());
        		filterTag.put("filter", entry.getValue().serializeNBT(provider));
        		tagList.add(filterTag);
        	}
        	tag.put("filters", tagList);
        }
    }
    
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
    	this.itemFilters.clear();
    	if(tag.contains("filters")) {
    		ListTag tagList = tag.getListOrEmpty("filters");
    		for(int i = 0; i < tagList.size(); i++) {
    			CompoundTag filterTag = tagList.getCompoundOrEmpty(i);
    			if(!filterTag.isEmpty()) {
    				Direction dir = Direction.byName(filterTag.getStringOr("direction", "north"));
    				ItemFilter filter = new ItemFilter();
    				filter.deserializeNBT(provider, filterTag.getCompoundOrEmpty("filter"));
    				if(filter.type !=null) {
    					this.itemFilters.put(dir, filter);
    				}
    			}
    		}
    	}
    	
    	super.loadAdditional(tag, provider);
    }
	
	//TODO Change this to an ItemHandler that only accepts allowed items
	@Override
	public IItemHandler getItemHandler() {
		return getData(ModInit.ITEM_HANDLER_ATTACHMENT);
	}
	
	@Override
	public int getInventorySize() {
		return 1;
	}

	public InteractionResult playerInteract(Player player, BlockHitResult hitResult, @Nullable ItemStack stack, @Nullable InteractionHand hand) {
		Direction dir = hitResult.getDirection();
		if(player.isCrouching()) {
			System.out.println("Crouching");
			if(stack == null || stack.isEmpty()) {
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
		}
		else {
			if(stack == null || stack.isEmpty()) {
				ItemFilter filter = this.itemFilters.get(dir);
				Component message = null;
				if(filter == null) {
					message = Component.literal(dir.getName() + ": No Filter");
				}
				else {
					String valueStr = filter.type == ItemFilter.FilterType.ITEM ? filter.stack.getDisplayName().getString() : filter.itemTag;
					message = Component.literal(dir.getName() + "(" + filter.type.name() + "): " + valueStr);				
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
			        	ItemFilter filter = new ItemFilter().setItemTag(component.getString().toLowerCase());
			        	this.itemFilters.put(dir, filter);
						this.clearRouteCache();
			        	this.markDirtyClient();
			        	return InteractionResult.SUCCESS_SERVER;
			        }
				}
				else {
					ItemStack copyStack = stack.copyWithCount(1);
					ItemFilter filter = new ItemFilter().setItemStack(copyStack);
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
							if(!filter.itemMatches(stack)) {
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
