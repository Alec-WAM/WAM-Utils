package alec_wam.wam_utils.blocks.machine.auto_animal_farmer;

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

public class AutoAnimalFarmerContainer extends WAMContainerMenu {

	public AutoAnimalFarmerBE blockEntity;
    private Player playerEntity;
    private IItemHandler playerInventory;
	
	public AutoAnimalFarmerContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
		super(ContainerInit.AUTO_ANIMAL_FARMER_CONTAINER.get(), windowId);
		blockEntity = (AutoAnimalFarmerBE) player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        if (blockEntity != null) {
            addSlot(new SlotItemHandler(blockEntity.inputItems, 0, 116, 13));               
            addSlot(new SlotItemHandler(blockEntity.fluidOutputItems, 0, 116, 55));       
            addSlot(new SlotItemHandler(blockEntity.upgradeItems, 0, 152, 25));
            addSlot(new SlotItemHandler(blockEntity.upgradeItems, 1, 152, 43));  
            addSlotBox(blockEntity.outputItems, 0, 8, 87, 9, 18, 2, 18);
            trackFluidAndEnergyProgress();
        }
        layoutPlayerInventorySlots(this.playerInventory, 8, 130);
	}
	
	private void trackFluidAndEnergyProgress() {
		addGenericData(GenericContainerData.energy(this.blockEntity.energyStorage));
		addGenericData(GenericContainerData.fluid(this.blockEntity.fluidStorage));
    }

	@Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), playerEntity, BlockInit.AUTO_ANIMAL_FARMER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if(index < 22) {
            	//Chest Area
            	if(!this.moveItemStackTo(stack, 49, 58, false)) {
	            	if (!this.moveItemStackTo(stack, 22, 49, false)) {
	                  return ItemStack.EMPTY;
	            	}
            	}
            }
            else {
            	if (!this.moveItemStackTo(stack, 2, 4, false)) {
	            	if (!this.moveItemStackTo(stack, 0, 1, false)) {
	            		if (index < 49 && !this.moveItemStackTo(stack, 49, 58, false)) {
	                      return ItemStack.EMPTY;
	            		}
	            		else if (!this.moveItemStackTo(stack, 22, 49, false)) {
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
