package alec_wam.wam_utils.blocks.enchantment_indexer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.blocks.bookshelf.EnchantmentBookshelfBE;
import alec_wam.wam_utils.blocks.bookshelf.EnchantmentBookshelfBlock;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ItemInit;
import alec_wam.wam_utils.utils.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class EnchantmentIndexerBE extends WAMUtilsBlockEntity {

	public static final int BOOK_SLOTS = 2;
	protected final ItemStackHandler bookItems = createBookItemHandler();
	public final LazyOptional<IItemHandler> bookItemHandler = LazyOptional.of(() -> bookItems);	
	private final List<BlockPos> shelves = new ArrayList<BlockPos>();	
	private final List<ShelfItem> itemList = new ArrayList<ShelfItem>();	
	private int scanDelay = 0;
	
	public static class ShelfItem {
		private ItemStack stack;
		private BlockPos pos;
		
		public ShelfItem(ItemStack stack, BlockPos pos) {
			this.stack = stack;
			this.pos = pos;
		}
		
		public ItemStack getStack() {
			return this.stack;
		}
		
		public BlockPos getPos() {
			return this.pos;
		}
		
		public ItemStack extractStack(Level level, boolean simulate) {
			if(level !=null) {
				if(level.isLoaded(pos)) {
					BlockEntity be = level.getBlockEntity(pos);
					if(be !=null && be instanceof EnchantmentBookshelfBE shelf) {
						IItemHandler bookHandler = shelf.bookItemHandler.orElse(null);
						if(bookHandler != null) {
							for(int i = 0; i < bookHandler.getSlots(); i++) {
								ItemStack otherBook = bookHandler.getStackInSlot(i);
								if(InventoryUtils.areItemsEqualIgnoreCount(stack, otherBook, false)) {
									return bookHandler.extractItem(i, 1, simulate);
								}
							}
						}
					}
				}
			}
			
			return ItemStack.EMPTY;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj == null)return false;
			if(!(obj instanceof ShelfItem))return false;
			ShelfItem otherItem = (ShelfItem)obj;
			return InventoryUtils.areItemsEqualIgnoreCount(stack, otherItem.getStack(), false) && pos.equals(otherItem.getPos());
		}
	}
	
	public EnchantmentIndexerBE(BlockPos p_155229_, BlockState p_155230_) {
		super(BlockInit.ENCHANTMENT_INDEXER_BE.get(), p_155229_, p_155230_);
	}
	
	@Override
	public void receiveMessageFromClient(CompoundTag nbt) {
		/*if(nbt.contains("Filter")) {
			this.filter = EnchantmentCategoryBookshelfFilter.getMode(nbt.getInt("Filter"));
			this.setChanged();
		}*/
	}

	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		if (nbt.contains("Inventory.Books")) {
			bookItems.deserializeNBT(nbt.getCompound("Inventory.Books"));
		}	
	}

	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		nbt.put("Inventory.Books", bookItems.serializeNBT());
	}
	
	public void tickServer() {
		if(this.scanDelay > 0) {
			scanDelay--;
		}
		
		if(scanDelay < 0) {
			this.buildShelfList();
			this.scanDelay = 20 * 60; //1 Min Delay
			return;
		}
		
		ItemStack inputBook = this.bookItems.getStackInSlot(0);
		if(!inputBook.isEmpty()) {
			importBook();
		}
	}
	
	public void importBook() {
		ItemStack inputBook = this.bookItems.getStackInSlot(0);
		if(!inputBook.isEmpty()) {
			//WAMUtilsMod.LOGGER.debug("Trying to Import : " + this.shelves.size());
			for(BlockPos pos : this.shelves) {
				if(!level.isLoaded(pos))continue;
				BlockEntity be = level.getBlockEntity(pos);
				if(be !=null && be instanceof EnchantmentBookshelfBE shelf) {
					IItemHandler bookHandler = shelf.bookItemHandler.orElse(null);
					if(bookHandler != null) {
						//WAMUtilsMod.LOGGER.debug("Trying to Import : " +bookHandler);
						ItemStack insert = InventoryUtils.putStackInInventoryAllSlots(bookHandler, inputBook);
						if(insert.isEmpty()) {
							this.bookItems.setStackInSlot(0, ItemStack.EMPTY);
							break;
						}
					}
				}
			}
		}
	}
	
	public void buildItemList() {
		if(!this.shelves.isEmpty()) {
			this.itemList.clear();
			for(BlockPos pos : this.shelves) {
				if(!level.isLoaded(pos))continue;
				BlockEntity be = level.getBlockEntity(pos);
				if(be !=null && be instanceof EnchantmentBookshelfBE shelf) {
					IItemHandler bookHandler = shelf.bookItemHandler.orElse(null);
					if(bookHandler != null) {
						for(int i = 0; i < bookHandler.getSlots(); i++) {
							ItemStack book = bookHandler.getStackInSlot(i);
							if(!book.isEmpty() && (book.is(Items.ENCHANTED_BOOK) || book.is(ItemInit.SINGLE_ENCHANTMENT_ITEM.get()))) {
								itemList.add(new ShelfItem(book, pos));
							}
						}
					}
				}
			}
		}
	}
	
	public void buildShelfList() {
		if(this.level == null) {
			return;
		}
		
		//WAMUtilsMod.LOGGER.debug("Building Shelf List");
		
		shelves.clear();
		
		final int range = 32;
		
		final Queue<BlockPos> searchQueue = new LinkedList<>();
	    final Set<BlockPos> searchDiscovered = new HashSet<>();
	    
	    searchQueue.add(worldPosition);
        searchDiscovered.add(worldPosition);
        
        while(!searchQueue.isEmpty()) {
        	BlockPos pos = searchQueue.remove();
        	//WAMUtilsMod.LOGGER.debug("Scanning : " + pos);
        	int distance = Math.max(Math.max(Math.abs(pos.getX() - worldPosition.getX()), Math.abs(pos.getY() - worldPosition.getY())), Math.abs(pos.getZ() - worldPosition.getZ()));
            if (distance > range) {
            	//WAMUtilsMod.LOGGER.debug("Range issue");
                continue;
            }
            
            if(!level.isLoaded(pos)) {
            	//WAMUtilsMod.LOGGER.debug("Loaded issue");
            	continue;
            }
            
            BlockState state = level.getBlockState(pos);
            if(pos != worldPosition && !(state.getBlock() instanceof EnchantmentBookshelfBlock)) {
            	//WAMUtilsMod.LOGGER.debug("Not Shelf issue");
            	continue;
            }
            
            if(pos != worldPosition) {
            	shelves.add(pos);
            }
            
            for(Direction dir : Direction.values()) {
            	BlockPos otherPos = pos.relative(dir);
            	if(!searchDiscovered.contains(otherPos)) {
            		searchQueue.add(otherPos);
            		searchDiscovered.add(otherPos);
            	}
            }
        }
        
        this.shelves.sort(new BlockDistanceComparator(worldPosition));
        //WAMUtilsMod.LOGGER.debug("Built List : " + this.shelves.size());
        
        buildItemList();
	}

	
	public static class BlockDistanceComparator implements Comparator<BlockPos> {					
		
		private final BlockPos origin;
		
		public BlockDistanceComparator(@Nullable BlockPos origin) {
			this.origin = origin;
		}	
		
		@Override
		public int compare(BlockPos o1, BlockPos o2) {
			return o1.distManhattan(origin) - o2.distManhattan(origin);
		}
		
	};
	
	public void tickClient() {
		
	}
	
	@Override
	public void setRemoved() {
		super.setRemoved();
		bookItemHandler.invalidate();
	}

	public boolean isValidBookForSlot(ItemStack stack, int slot) {
		if(!stack.isEmpty()) {
			if(stack.is(Items.ENCHANTED_BOOK) || stack.is(ItemInit.SINGLE_ENCHANTMENT_ITEM.get())) {
				return true;
			}
		}
		return false;
	}
	
	@Nonnull
	private ItemStackHandler createBookItemHandler() {
		return new ItemStackHandler(BOOK_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				setChanged();
				markBlockForUpdate(null);
			}

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return isValidBookForSlot(stack, slot);
			}

			@Nonnull
			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				if(slot == 0) {
					return ItemStack.EMPTY;
				}
				return super.extractItem(BOOK_SLOTS, BOOK_SLOTS, remove);
			}
			
			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				if(slot == 1) {
					return stack;
				}
				if (!isValidBookForSlot(stack, slot)) {
					return stack;
				}
				return super.insertItem(slot, stack, simulate);
			}
		};
	}
	
	@NotNull
	@Override
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) {
			return bookItemHandler.cast();
		} 
		return super.getCapability(cap, side);
	}

}
