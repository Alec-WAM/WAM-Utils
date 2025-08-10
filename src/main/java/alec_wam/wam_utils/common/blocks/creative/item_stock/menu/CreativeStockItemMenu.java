package alec_wam.wam_utils.common.blocks.creative.item_stock.menu;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.blocks.creative.item_stock.CreativeItemStockBE;
import alec_wam.wam_utils.common.menu.AbstractBaseBEMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class CreativeStockItemMenu extends AbstractBaseBEMenu<CreativeItemStockBE>{

    public CreativeStockItemMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        super(ModInit.CREATIVE_STOCKER_ITEM_BLOCK_MENU_TYPE.get(), containerId, playerInv, extraData);
    }

	public CreativeStockItemMenu(int containerId, Inventory playerInv,
			CreativeItemStockBE blockEntity) {
		super(ModInit.CREATIVE_STOCKER_ITEM_BLOCK_MENU_TYPE.get(), containerId, playerInv, blockEntity);
	}

	@Override
	public void addSlots(Inventory playerInv) {
		ItemStackHandler inventory = this.blockEntity.getInternalInventory();
        this.addSlot(new SlotItemHandler(inventory, 0, 80, 20));

        this.addStandardInventorySlots(playerInv, 8, 51);
	}

	@Override
	public boolean isValidBlock(BlockState state) {
		return state.is(ModInit.CREATIVE_STOCKER_ITEM_BLOCK);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if(this.blockEntity == null) return ItemStack.EMPTY;
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 1) {
                if (!this.moveItemStackTo(itemstack1, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                int j = 1 + 27;
                int k = j + 9;
                if (index >= j && index < k) {
                    if (!this.moveItemStackTo(itemstack1, 1, j, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 1 && index < j) {
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
