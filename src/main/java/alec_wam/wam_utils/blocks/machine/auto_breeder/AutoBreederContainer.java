package alec_wam.wam_utils.blocks.machine.auto_breeder;

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

public class AutoBreederContainer extends WAMContainerMenu {

	public AutoBreederBE blockEntity;
    private Player playerEntity;
    private IItemHandler playerInventory;
	
	public AutoBreederContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
		super(ContainerInit.AUTO_BREEDER_CONTAINER.get(), windowId);
		blockEntity = (AutoBreederBE) player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        if (blockEntity != null) {
            addSlotRange(blockEntity.inputItems, 0, 63, 34, AutoBreederBE.INPUT_SLOTS, 18);
            addSlot(new SlotItemHandler(blockEntity.upgradeItems, 0, 143, 34));
            trackEnergyProgress();
        }
        layoutPlayerInventorySlots(this.playerInventory, 8, 84);
	}
	
	private void trackEnergyProgress() {
		addGenericData(GenericContainerData.energy(this.blockEntity.energyStorage));
		addGenericData(GenericContainerData.bool(this.blockEntity::shouldFeedBabies, this.blockEntity::setShouldFeedBabies));
		addGenericData(GenericContainerData.int32(this.blockEntity::getMaxAnimals, this.blockEntity::setMaxAnimals));
    }

	@Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), playerEntity, BlockInit.AUTO_BREEDER_BLOCK.get());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if(index < 4) {
            	//Chest Area
            	if(!this.moveItemStackTo(stack, 31, 40, false)){
	            	if (!this.moveItemStackTo(stack, 4, 31, false)) {
	                  return ItemStack.EMPTY;
	            	}
            	}
            }
            else {
            	if (!this.moveItemStackTo(stack, 3, 4, false)) {
	            	if (!this.moveItemStackTo(stack, 0, 3, false)) {
	            		if (index < 31 && !this.moveItemStackTo(stack, 31, 40, false)) {
	                      return ItemStack.EMPTY;
	            		}
	            		else if (!this.moveItemStackTo(stack, 4, 31, false)) {
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
