package alec_wam.wam_utils.common.blocks.enchantment.indexer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.blocks.BaseBE;
import alec_wam.wam_utils.common.helpers.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class EnchantmentIndexerBE extends BaseBE {

    public static record ShelfItem(BlockPos pos, ItemStack stack, Optional<Direction> direction) {
        
        public static final Codec<ShelfItem> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
				BlockPos.CODEC.fieldOf("pos").forGetter(ShelfItem::pos),
				ItemStack.CODEC.fieldOf("itemstack").forGetter(ShelfItem::stack),
				Direction.CODEC.optionalFieldOf("direction").forGetter(ShelfItem::direction)
			).apply(instance, ShelfItem::new)
		);
        
        public ItemStack extractStack(Level level, boolean simulate) {
			if(level !=null) {
				if(level.isLoaded(pos)) {
                    IItemHandler invHandler = BlockHelper.getItemHandler(level, pos, direction.orElse(null)).orElse(null);
                    if(invHandler != null) {                        
                        for(int i = 0; i < invHandler.getSlots(); i++) {
                            ItemStack otherBook = invHandler.getStackInSlot(i);
                            if(ItemStack.isSameItemSameComponents(otherBook, this.stack)) {
                                return invHandler.extractItem(i, 1, simulate);
                            }
                        }
                    }
				}
			}
			
			return ItemStack.EMPTY;
		}
    }

    public static final int INVENTORY_SIZE = 2;

    public static class EnchantmentIndexerInventory extends ItemStackHandler {

        public EnchantmentIndexerInventory() {
            super(INVENTORY_SIZE);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            // if(slot != 0) {
            //     return stack;
            // }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // if(slot != 1) {
            //     return ItemStack.EMPTY;
            // }
            return super.extractItem(slot, amount, simulate);
        }
    }

    private final List<BlockPos> shelves = new ArrayList<BlockPos>();	
	private final List<ShelfItem> itemList = new ArrayList<ShelfItem>();	
	private int scanDelay = 0;
    private EnchantmentIndexerInventory inventory = new EnchantmentIndexerInventory();

    public EnchantmentIndexerBE(BlockPos pos, BlockState blockState) {
        super(ModInit.ENCHANTMENT_INDEXER_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public ItemStackHandler getItemHandler(@Nullable Direction side) {
        return this.inventory;
    }

    @Override
    public void tickServer(){
        super.tickServer();
        
		if(this.scanDelay > 0) {
			scanDelay--;
		}
		
		if(scanDelay < 0) {
			this.buildShelfList();
			this.scanDelay = 20 * 60; //1 Min Delay
			return;
		}

        ItemStack inputBook = this.inventory.getStackInSlot(0);
		if(!inputBook.isEmpty()) {
			importBook();
		}       
    }

    public void importBook() {
		ItemStack inputBook = this.inventory.getStackInSlot(0);
		if(!inputBook.isEmpty()) {
            // System.out.println("Trying to Import : " + inputBook);
			for(BlockPos pos : this.shelves) {
				if(!level.isLoaded(pos))continue;
                System.out.println("Trying to Import : " + pos);
                IItemHandler bookHandler = BlockHelper.getItemHandler(level, pos, null).orElse(null);
                if(bookHandler != null) {
                    //WAMUtilsMod.LOGGER.debug("Trying to Import : " +bookHandler);
                    ItemStack insert = BlockHelper.insertItemStacked(bookHandler, inputBook, false);
                    if(insert.isEmpty()) {
                        this.inventory.extractItem(0, 1, false);
                        break;
                    }
                }
			}
		}
	}

    public boolean validBookshelf(BlockState state) {
        return state.is(Blocks.CHISELED_BOOKSHELF);
    }

    public void buildShelfList() {
		if(this.level == null) {
			return;
		}
		
		shelves.clear();
		
		final int range = 32;
		
		final Queue<BlockPos> searchQueue = new LinkedList<>();
	    final Set<BlockPos> searchDiscovered = new HashSet<>();
	    
	    searchQueue.add(worldPosition);
        searchDiscovered.add(worldPosition);
        
        while(!searchQueue.isEmpty()) {
        	BlockPos pos = searchQueue.remove();
        	int distance = Math.max(Math.max(Math.abs(pos.getX() - worldPosition.getX()), Math.abs(pos.getY() - worldPosition.getY())), Math.abs(pos.getZ() - worldPosition.getZ()));
            if (distance > range) {
                continue;
            }
            
            if(!level.isLoaded(pos)) {
            	continue;
            }
            
            BlockState state = level.getBlockState(pos);
            if(!pos.equals(worldPosition) && !validBookshelf(state)) {
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
        
        System.out.println("Found " + this.shelves.size() + " shelves");

        buildItemList();
	}

    public boolean validBookItem(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK);
    }

    public void buildItemList() {
		if(!this.shelves.isEmpty()) {
			this.itemList.clear();
			for(BlockPos pos : this.shelves) {
				if(!level.isLoaded(pos))continue;
                IItemHandler bookHandler = BlockHelper.getItemHandler(level, pos, null).orElse(null);
				if(bookHandler != null) {
                    for(int i = 0; i < bookHandler.getSlots(); i++) {
                        ItemStack book = bookHandler.getStackInSlot(i);
                        if(!book.isEmpty() && validBookItem(book)) {
                            itemList.add(new ShelfItem(pos, book, Optional.empty()));
                        }
                    }
                }
			}
		}
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
    
}
