package alec_wam.wam_utils.common.blocks.enchantment.indexer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.blocks.BaseBE;
import alec_wam.wam_utils.common.blocks.enchantment.indexer.menu.EnchantmentIndexerMenu;
import alec_wam.wam_utils.common.helpers.BlockHelper;
import alec_wam.wam_utils.datagen.WAMUtilsBlockTags;
import alec_wam.wam_utils.network.SyncClientShelfItemsPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

public class EnchantmentIndexerBE extends BaseBE implements MenuProvider{

    public static record ClientShelfItem(UUID uuid, ItemStack stack, ItemEnchantments enchantments, List<String> searchValues) {
        public static final Codec<ClientShelfItem> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
				UUIDUtil.CODEC.fieldOf("uuid").forGetter(ClientShelfItem::uuid),
                ItemStack.CODEC.fieldOf("stack").forGetter(ClientShelfItem::stack),
				ItemEnchantments.CODEC.fieldOf("enchantments").forGetter(ClientShelfItem::enchantments),
				Codec.STRING.listOf().fieldOf("searchValues").forGetter(ClientShelfItem::searchValues)
			).apply(instance, ClientShelfItem::new)
		);

        public static final StreamCodec<RegistryFriendlyByteBuf, ClientShelfItem> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ClientShelfItem::uuid,
            ItemStack.STREAM_CODEC, ClientShelfItem::stack,
            ItemEnchantments.STREAM_CODEC, ClientShelfItem::enchantments,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ClientShelfItem::searchValues,
			ClientShelfItem::new
	    );
    }

    public static class ShelfItem {
        
        public static final Codec<ShelfItem> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
				BlockPos.CODEC.fieldOf("pos").forGetter((shelfItem) -> shelfItem.pos),
                Codec.INT.fieldOf("slot").forGetter((shelfItem) -> shelfItem.slot),
				ItemStack.CODEC.fieldOf("itemstack").forGetter((shelfItem) -> shelfItem.stack),
				Direction.CODEC.optionalFieldOf("direction").forGetter((shelfItem) -> shelfItem.direction)
			).apply(instance, ShelfItem::new)
		);

        public static final StreamCodec<RegistryFriendlyByteBuf, ShelfItem> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, (ShelfItem shelfItem) -> {
                return shelfItem.pos;
            },
            ByteBufCodecs.INT, (ShelfItem shelfItem) -> {
                return shelfItem.slot;
            },
            ItemStack.STREAM_CODEC, (ShelfItem shelfItem) -> {
                return shelfItem.stack;
            },
            ByteBufCodecs.optional(Direction.STREAM_CODEC), (ShelfItem shelfItem) -> {
                return shelfItem.direction;
            },
			ShelfItem::new
	    );

        public final BlockPos pos;
        public final int slot;
        public final ItemStack stack;
        public final Optional<Direction> direction;
        public final List<String> searchValues;
        public final ItemEnchantments enchantments;

        public ShelfItem(BlockPos pos, int slot, ItemStack stack, Optional<Direction> direction) {
            this.pos = pos;
            this.slot = slot;
            this.stack = stack;
            this.direction = direction;
            this.enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
            this.searchValues = this.buildSearchValues();
        }

        public final List<String> buildSearchValues(){
            List<String> searchValues = new ArrayList<>();
            for(Holder<Enchantment> enchantment : this.enchantments.keySet()) {
                searchValues.add(Enchantment.getFullname(enchantment, this.enchantments.getLevel(enchantment)).getString());
            }
            return searchValues;
        }
        
        public ItemStack extractStack(Level level, boolean simulate) {
			if(level !=null) {
				if(level.isLoaded(pos)) {
                    IItemHandler invHandler = BlockHelper.getItemHandler(level, pos, direction.orElse(null)).orElse(null);
                    if(invHandler != null) {                        
                        ItemStack otherBook = invHandler.getStackInSlot(this.slot);
                        if(ItemStack.isSameItemSameComponents(otherBook, this.stack)) {
                            final ItemStack returnStack = invHandler.extractItem(this.slot, 1, simulate);
                            return returnStack;
                        }
                    }
				}
			}
			
			return ItemStack.EMPTY;
		}

        @Override
        public int hashCode() {
            return Objects.hash(
                this.pos,
                this.slot,
                this.stack,
                this.direction
            );
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ShelfItem shelfItem) {
                return (Objects.equals(shelfItem.pos, this.pos) 
                && (Objects.equals(shelfItem.slot, this.slot)) 
                && (Objects.equals(shelfItem.stack, this.stack))) 
                && (Objects.equals(shelfItem.direction, this.direction));
            }
            return false;
        }

        public ClientShelfItem toClientShelfItem(UUID uuid) {
            return new ClientShelfItem(uuid, this.stack, this.enchantments, this.searchValues);
        }
    }

    public static final int INVENTORY_SIZE = 2;
    public static final Predicate<ItemStack> VALID_BOOK = (stack) -> {
        return stack.is(Items.ENCHANTED_BOOK);
    };

    public static class EnchantmentIndexerInventory extends ItemStackHandler {

        public EnchantmentIndexerInventory() {
            super(INVENTORY_SIZE);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return VALID_BOOK.test(stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if(slot != 0) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if(slot != 1) {
                return ItemStack.EMPTY;
            }
            return super.extractItem(slot, amount, simulate);
        }
    }

    private final List<BlockPos> shelves = new ArrayList<BlockPos>();	
	private final Map<UUID,ShelfItem> itemList = new HashMap<UUID, ShelfItem>();	
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
		
		if(scanDelay <= 0) {
			this.buildShelfList();
			this.scanDelay = 20 * 60; //1 Min Delay
			return;
		}

        ItemStack inputBook = this.inventory.getStackInSlot(0);
		if(!inputBook.isEmpty()) {
			importBook();
		}       
    }

    public ShelfItem findSameItem(BlockPos pos, int slot, ItemStack stack, Optional<Direction> direction) {
        return this.itemList.values().stream().filter(shelfItem -> {
           return (Objects.equals(shelfItem.pos, pos) 
                && (Objects.equals(shelfItem.slot, slot)) 
                && (Objects.equals(shelfItem.stack, stack))) 
                && (Objects.equals(shelfItem.direction, direction));
        }).findFirst().orElse(null);
    }

    public void importBook() {
		ItemStack inputBook = this.inventory.getStackInSlot(0);
		if(!inputBook.isEmpty() && EnchantmentIndexerBE.VALID_BOOK.test(inputBook)) {
			for(BlockPos pos : this.shelves) {
				if(!level.isLoaded(pos))continue;
                IItemHandler bookHandler = BlockHelper.getItemHandler(level, pos, null).orElse(null);
                if(bookHandler != null) {
                    int insertSlot = BlockHelper.insertSingleItemStacked(bookHandler, inputBook.copyWithCount(1), false);
                    if(insertSlot != -1) {                        
                        boolean updatedExisting = false;
                        if(inputBook.isStackable()){
                            for (Map.Entry<UUID, ShelfItem> entry : this.itemList.entrySet()) {
                                ShelfItem existing = entry.getValue();
                                if (existing.pos.equals(pos) &&
                                    existing.slot == insertSlot &&
                                    ItemStack.isSameItemSameComponents(existing.stack, inputBook) &&
                                    existing.direction.equals(Optional.empty()) ) {

                                    ItemStack newStack = existing.stack.copy();
                                    newStack.grow(1);

                                    // Recreate ShelfItem with new stack
                                    ShelfItem updated = new ShelfItem(pos, insertSlot, newStack, Optional.empty());
                                    this.itemList.put(entry.getKey(), updated);
                                    updatedExisting = true;
                                    break;
                                }
                            }
                        }

                        if (!updatedExisting) {
                            this.itemList.put(UUID.randomUUID(), new ShelfItem(pos, insertSlot, inputBook.copyWithCount(1), Optional.empty()));
                        }
                        inputBook.shrink(1);
                        this.syncToWatchingPlayers();
                        break;
                    }
                }
			}
		}
	}

    public void extractBook(UUID uuid, ShelfItem item) {
        ItemStack extractSlotItem = this.inventory.getStackInSlot(1);
        if(extractSlotItem.isEmpty() || (extractSlotItem.getCount() < extractSlotItem.getMaxStackSize() && ItemStack.isSameItemSameComponents(extractSlotItem, item.stack))) {
            ItemStack extracted = item.extractStack(level, false);
            if(!extracted.isEmpty()) {
                if(!extractSlotItem.isStackable()) {
                    this.inventory.setStackInSlot(1, extracted);
                }
                else {
                    this.inventory.insertItem(1, extracted, false);
                }
                                  
                if(item.stack.isEmpty() || !item.stack.isStackable()) {                    
                    this.itemList.remove(uuid);      
                }          
                this.syncToWatchingPlayers();
            }            
        }
    }

    public boolean validBookshelf(BlockState state) {
        return state.is(WAMUtilsBlockTags.ENCHANTMENT_INDEXER_BOOKSHELVES);
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
        
        // System.out.println("Found " + this.shelves.size() + " shelves");

        buildItemList();
	}

    public void buildItemList() {
		this.itemList.clear();
		if(!this.shelves.isEmpty()) {
			for(BlockPos pos : this.shelves) {
				if(!level.isLoaded(pos))continue;
                IItemHandler bookHandler = BlockHelper.getItemHandler(level, pos, null).orElse(null);
				if(bookHandler != null) {
                    for(int i = 0; i < bookHandler.getSlots(); i++) {
                        ItemStack book = bookHandler.getStackInSlot(i);
                        if(!book.isEmpty() && VALID_BOOK.test(book)) {
                            itemList.put(UUID.randomUUID(), new ShelfItem(pos, i, book, Optional.empty()));
                        }
                    }
                }
			}
		}
        this.syncToWatchingPlayers();
	}

    public Map<UUID, ShelfItem> getItemList() {
        return this.itemList;
    }

    @Override
    public void handleCustomMessage(String messageType, CompoundTag messageData, boolean isClient) {
        if(!isClient){
            if(messageType.equals("extract_enchantment")) {
                Optional<UUID> uuid = messageData.read("uuid", UUIDUtil.CODEC);
                if(uuid.isPresent()) {
                    ShelfItem item = this.itemList.get(uuid.get());
                    if(item !=null){
                        extractBook(uuid.get(), item);
                        return;
                    }
                }
            }
        }
        super.handleCustomMessage(messageType, messageData, isClient);
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
		
	}

    @Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new EnchantmentIndexerMenu(containerId, playerInventory, this);
	}

    public List<ClientShelfItem> getClientItemList() {
        return this.itemList.entrySet().stream()
        .filter((entry) -> !entry.getValue().stack.isEmpty())
        .map(
            (entry) -> entry.getValue().toClientShelfItem(entry.getKey())
        ).toList();
    }

    public void syncToWatchingPlayers(){
        if (this.level == null || this.level.isClientSide) return;
        List<ClientShelfItem> items = getClientItemList();

        SyncClientShelfItemsPayload packet = new SyncClientShelfItemsPayload(items);
        for (ServerPlayer player : ((ServerLevel) this.level).players()) {
            if (player.containerMenu instanceof EnchantmentIndexerMenu menu) {
                // Check this menu is for *this* BE
                if (menu.isBoundTo(this)) {
                    PacketDistributor.sendToPlayer(player, packet);
                }
            }
        }
    }

	@Override
	public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.getBlockPos());
        buffer.writeCollection(getClientItemList(), ClientShelfItem.STREAM_CODEC.mapStream(otherBuffer -> (RegistryFriendlyByteBuf) otherBuffer));
	}

    @Override
    public Component getDisplayName() {
        return Component.translatable("wamutils.container.enchantment_indexer.title");
    }
    
}
