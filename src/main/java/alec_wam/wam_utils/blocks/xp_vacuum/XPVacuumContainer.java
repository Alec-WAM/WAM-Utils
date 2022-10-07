package alec_wam.wam_utils.blocks.xp_vacuum;

import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ContainerInit;
import alec_wam.wam_utils.server.container.GenericContainerData;
import alec_wam.wam_utils.server.container.GenericDataSerializers;
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

public class XPVacuumContainer extends WAMContainerMenu {

	public XPVacuumBE blockEntity;
    private Player playerEntity;
    private IItemHandler playerInventory;
	
	public XPVacuumContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
		super(ContainerInit.XP_VACUUM_CONTAINER.get(), windowId);
		blockEntity = (XPVacuumBE) player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        if (blockEntity != null) {     
            addSlot(new SlotItemHandler(blockEntity.upgradeItems, 0, 152, 58));
            trackFluidProgress();
        }
        layoutPlayerInventorySlots(this.playerInventory, 8, 84);
	}
	
	private void trackFluidProgress() {
		addGenericData(new GenericContainerData<>(GenericDataSerializers.FLUID_STACK, blockEntity.fluidStorage::getFluid, blockEntity.fluidStorage::setFluid));
    }

	@Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), playerEntity, BlockInit.XP_VACUUM_BLOCK.get());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            
            if(index < 1) {
            	if(!this.moveItemStackTo(stack, 1, 10, false)) {
	            	if (!this.moveItemStackTo(stack, 10, 36, false)) {
	                  return ItemStack.EMPTY;
	            	}
	        	}
            }
            else {
            	if(!this.moveItemStackTo(stack, 0, 1, false)) {
		            if(!this.moveItemStackTo(stack, 1, 10, false)) {
		            	if (!this.moveItemStackTo(stack, 10, 36, false)) {
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
