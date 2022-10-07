package alec_wam.wam_utils.blocks.advanced_beehive;

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

public class AdvancedBeehiveContainer extends WAMContainerMenu {

	public AdvancedBeehiveBE blockEntity;
    private Player playerEntity;
    private IItemHandler playerInventory;
	
	public AdvancedBeehiveContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
		super(ContainerInit.ADVANCED_BEEHIVE_CONTAINER.get(), windowId);
		blockEntity = (AdvancedBeehiveBE) player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        if (blockEntity != null) {
            addSlot(new SlotItemHandler(blockEntity.inputItems, 0, 80, 49));            
            addSlotRange(blockEntity.outputItems, 0, 44, 71, 5, 18);
        }
        layoutPlayerInventorySlots(this.playerInventory, 8, 104);
	}

	@Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), playerEntity, BlockInit.ADVANCED_BEEHIVE_BLOCK.get());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if(index < 6) {
            	//Chest Area
            	if(!this.moveItemStackTo(stack, 33, 42, false)) {
	            	if (!this.moveItemStackTo(stack, 6, 34, false)) {
	                  return ItemStack.EMPTY;
	            	}
            	}
            }
            else {
            	if (!this.moveItemStackTo(stack, 0, 1, false)) {
            		if (index < 33 && !this.moveItemStackTo(stack, 33, 42, false)) {
                      return ItemStack.EMPTY;
            		}
            		else if (!this.moveItemStackTo(stack, 6, 33, false)) {
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
