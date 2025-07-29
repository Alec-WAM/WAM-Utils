package alec_wam.wam_utils.common.blocks.enchantment.indexer.menu;

import java.util.List;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.blocks.enchantment.indexer.EnchantmentIndexerBE;
import alec_wam.wam_utils.common.blocks.enchantment.indexer.EnchantmentIndexerBE.ClientShelfItem;
import alec_wam.wam_utils.common.menu.AbstractBaseBEMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class EnchantmentIndexerMenu extends AbstractBaseBEMenu<EnchantmentIndexerBE> {

    public static final int INDEXER_INPUT_SLOT = 0;
    public static final int INDEXER_FILTER_SLOT = 1;
    public static final int INDEXER_OUTPUT_SLOT = 2;

    public SimpleContainer filterItemContainer;
    private List<ClientShelfItem> itemList;
    private List<ClientShelfItem> filteredItemList;
    private String filterSearchValue;

    public EnchantmentIndexerMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        super(ModInit.ENCHANTMENT_INDEXER_MENU_TYPE.get(), containerId, playerInv, extraData);
        this.itemList = extraData.readList(ClientShelfItem.STREAM_CODEC.mapStream((stream -> (RegistryFriendlyByteBuf) stream)));
        this.filteredItemList = this.itemList;
    }

    public EnchantmentIndexerMenu(int containerId, Inventory playerInv, EnchantmentIndexerBE blockEntity) {
        super(ModInit.ENCHANTMENT_INDEXER_MENU_TYPE.get(), containerId, playerInv, blockEntity);
        this.itemList = this.blockEntity.getClientItemList();
        this.filteredItemList = this.itemList;
    }

    @Override
    public void addSlots(Inventory playerInv) {

        ItemStackHandler inventory = this.blockEntity.getItemHandler(null);
        this.filterItemContainer = new SimpleContainer(1);
        // Input
        this.addSlot(new SlotItemHandler(inventory, 0, 172, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EnchantmentIndexerBE.VALID_BOOK.test(stack);
            }
        });       

        // Filter
        this.addSlot(new Slot(filterItemContainer, 0, 172, 6) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !EnchantmentIndexerBE.VALID_BOOK.test(stack) 
                    && (stack.isEnchantable() || stack.getEquipmentSlot() != EquipmentSlot.MAINHAND);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                EnchantmentIndexerMenu.this.updateFilter();
            }
        });

        //Export
        this.addSlot(new SlotItemHandler(inventory, 1, 172, 82) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        this.addStandardInventorySlots(playerInv, 19, 127);
    }

    public ItemStack getExtractedItemStack() {
        return this.blockEntity.getItemHandler(null).getStackInSlot(1);
    }

    public String getFilterSearchValue() {
        return this.filterSearchValue;
    }

    public void setFilterSearchValue(String filterSearchValue) {
        this.filterSearchValue = filterSearchValue;
        this.updateFilter();
    }

    public List<ClientShelfItem> getFilteredItemList() {
        return this.filteredItemList;
    }

    public void setItemList(List<ClientShelfItem> itemList) {
        this.itemList = itemList;
        this.updateFilter();
    }

    public void updateFilter() {
        ItemStack filterStack = this.filterItemContainer.getItem(0);
        String filter = this.filterSearchValue;
        if((filter == null || filter.isEmpty()) && filterStack.isEmpty()) {
            this.filteredItemList = this.itemList;
            return;
        }

        List<ClientShelfItem> baseList = filterStack.isEmpty() ? this.itemList : this.itemList.stream().filter(item -> item.enchantments().keySet().stream().anyMatch(enchantment -> filterStack.supportsEnchantment(enchantment))).toList();
        this.filteredItemList = (filter == null || filter.isEmpty()) ? baseList : baseList.stream().filter(item -> item.searchValues().stream().anyMatch(value -> value.toLowerCase().contains(filter.toLowerCase()))).toList();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((p_39796_, p_39797_) -> this.clearContainer(player, this.filterItemContainer));
    }

    @Override
    public boolean isValidBlock(BlockState state) {
        return state.is(ModInit.ENCHANTMENT_INDEXER_BLOCK);
    }

    public boolean isBoundTo(EnchantmentIndexerBE enchantmentIndexerBE) {
        return this.blockEntity == enchantmentIndexerBE;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if(this.blockEntity == null) return ItemStack.EMPTY;
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < INDEXER_OUTPUT_SLOT) {
                if (!this.moveItemStackTo(itemstack1, INDEXER_OUTPUT_SLOT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, INDEXER_FILTER_SLOT + 1, false)) {
                int j = INDEXER_OUTPUT_SLOT + 27;
                int k = j + 9;
                if (index >= j && index < k) {
                    if (!this.moveItemStackTo(itemstack1, INDEXER_OUTPUT_SLOT, j, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= INDEXER_OUTPUT_SLOT && index < j) {
                    if (!this.moveItemStackTo(itemstack1, j, k, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, j, j, false)) {
                    return ItemStack.EMPTY;
                }

                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }
    
}
