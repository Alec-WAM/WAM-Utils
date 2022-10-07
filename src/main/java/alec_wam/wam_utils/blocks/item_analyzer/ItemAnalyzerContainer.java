package alec_wam.wam_utils.blocks.item_analyzer;

import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ContainerInit;
import alec_wam.wam_utils.server.container.WAMContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

public class ItemAnalyzerContainer extends WAMContainerMenu {

	public ItemAnalyzerBE blockEntity;
    private Player playerEntity;
    private IItemHandler playerInventory;
	
	public ItemAnalyzerContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
		super(ContainerInit.ITEM_ANALYZER_CONTAINER.get(), windowId);
		blockEntity = (ItemAnalyzerBE) player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        if (blockEntity != null) {
            addSlot(new SlotItemHandler(blockEntity.inputItems, 0, 8, 18));
        }
        layoutPlayerInventorySlots(this.playerInventory, 8, 140);
	}

	@Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), playerEntity, BlockInit.ITEM_ANALYZER_BLOCK.get());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if(index < 1) {
            	//Chest Area
            	if(!this.moveItemStackTo(stack, 29, 37, false)) {
	            	if (!this.moveItemStackTo(stack, 1, 30, false)) {
	                  return ItemStack.EMPTY;
	            	}
            	}
            }
            else {
            	if (!this.moveItemStackTo(stack, 0, 1, false)) {
            		if (index < 29 && !this.moveItemStackTo(stack, 29, 37, false)) {
            			return ItemStack.EMPTY;
            		}
            		else if (!this.moveItemStackTo(stack, 1, 29, false)) {
            			return ItemStack.EMPTY;
            		}
            	}
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, stack);
        }

        return itemstack;
    }

}
