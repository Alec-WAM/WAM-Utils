package alec_wam.wam_utils.common.blocks.enchantment.bookshelf.menu;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.blocks.enchantment.bookshelf.EnchantmentBookshelfBE;
import alec_wam.wam_utils.common.menu.AbstractBaseBEMenu;
import alec_wam.wam_utils.datagen.WAMUtilsBlockTags;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class EnchantmentBookshelfMenu extends AbstractBaseBEMenu<EnchantmentBookshelfBE> {

    public static final int ENCHANTMENT_BOOK_SHELF_SLOTS = 24;

    public EnchantmentBookshelfMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        super(ModInit.ENCHANTMENT_BOOK_SHELF_MENU_TYPE.get(), containerId, playerInv, extraData);
    }

    public EnchantmentBookshelfMenu(int containerId, Inventory playerInv, final EnchantmentBookshelfBE bookshelfBE) {
        super(ModInit.ENCHANTMENT_BOOK_SHELF_MENU_TYPE.get(), containerId, playerInv, bookshelfBE);
    }

    @Override
    public void addSlots(Inventory playerInv){
        ItemStackHandler inventory = this.blockEntity.getItemHandler(null);
        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 8; j++) {
                this.addSlot(new SlotItemHandler(inventory, i * 8 + j, 22 + j * 18, 19 + i * 22));
            }
        }

        this.addStandardInventorySlots(playerInv, 13, 93);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if(this.blockEntity == null) return ItemStack.EMPTY;
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < ENCHANTMENT_BOOK_SHELF_SLOTS) {
                if (!this.moveItemStackTo(itemstack1, ENCHANTMENT_BOOK_SHELF_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, ENCHANTMENT_BOOK_SHELF_SLOTS, false)) {
                int j = ENCHANTMENT_BOOK_SHELF_SLOTS + 27;
                int k = j + 9;
                if (index >= j && index < k) {
                    if (!this.moveItemStackTo(itemstack1, ENCHANTMENT_BOOK_SHELF_SLOTS, j, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= ENCHANTMENT_BOOK_SHELF_SLOTS && index < j) {
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

    @Override
    public boolean isValidBlock(BlockState state) {
        return state.is(WAMUtilsBlockTags.ENCHANTMENT_BOOKSHELVES);
    }
    
}
