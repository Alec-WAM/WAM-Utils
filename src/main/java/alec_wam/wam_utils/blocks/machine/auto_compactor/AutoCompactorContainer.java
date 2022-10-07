package alec_wam.wam_utils.blocks.machine.auto_compactor;

import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ContainerInit;
import alec_wam.wam_utils.server.container.GenericContainerData;
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

public class AutoCompactorContainer extends WAMContainerMenu {

	public AutoCompactorBE blockEntity;
    private Player playerEntity;
    private IItemHandler playerInventory;
	
	public AutoCompactorContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
		super(ContainerInit.AUTO_COMPACTOR_CONTAINER.get(), windowId);
		blockEntity = (AutoCompactorBE) player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        if (blockEntity != null) {
            blockEntity.inputItemHandler.ifPresent(h -> {
            	addSlotBox(blockEntity.inputItems, 0, 35, 17, 3, 18, 3, 18);
            });
            
            blockEntity.upgradeItemHandler.ifPresent(h -> {
                addSlot(new SlotItemHandler(h, 0, 152, 53));
            });
            
            blockEntity.outputItemHandler.ifPresent(h -> {
                addSlot(new SlotItemHandler(blockEntity.outputItems, 0, 124, 35));
            });
            
            trackProgressAndEnergy();
        }
        layoutPlayerInventorySlots(this.playerInventory, 8, 84);
	}
	
	private void trackProgressAndEnergy() {
		addGenericData(GenericContainerData.energy(this.blockEntity.energyStorage));
		addGenericData(GenericContainerData.int32(blockEntity::getCraftingTime, blockEntity::setCraftingTime));
		addGenericData(GenericContainerData.int32(blockEntity::getMaxCraftingTime, blockEntity::setMaxCraftingTime));
    }

	@Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), playerEntity, BlockInit.AUTO_COMPACTOR_BLOCK.get());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if(index < 11) {
            	//Chest Area
            	if(!this.moveItemStackTo(stack, 38, 47, false)) {
	            	if (!this.moveItemStackTo(stack, 11, 38, false)) {
	                  return ItemStack.EMPTY;
	            	}
            	}
            }
            else {
            	if (!this.moveItemStackTo(stack, 9, 10, false)) {
            		if (!this.moveItemStackTo(stack, 0, 9, false)) {
	            		if (index < 38 && !this.moveItemStackTo(stack, 38, 47, false)) {
	                      return ItemStack.EMPTY;
	            		}
	            		else if (!this.moveItemStackTo(stack, 11, 38, false)) {
	                        return ItemStack.EMPTY;
	              		}
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
