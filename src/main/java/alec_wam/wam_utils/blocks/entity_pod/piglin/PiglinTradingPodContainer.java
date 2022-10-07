package alec_wam.wam_utils.blocks.entity_pod.piglin;

import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ContainerInit;
import alec_wam.wam_utils.server.container.GenericContainerData;
import alec_wam.wam_utils.server.container.GenericDataSerializers;
import alec_wam.wam_utils.server.container.GetterAndSetter;
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

public class PiglinTradingPodContainer extends WAMContainerMenu {

	public PiglinTradingPodBE blockEntity;
    private Player playerEntity;
    private IItemHandler playerInventory;
	
	public PiglinTradingPodContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
		super(ContainerInit.PIGLIN_TRADING_POD_CONTAINER.get(), windowId);
		blockEntity = (PiglinTradingPodBE) player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        if (blockEntity != null) {
            addSlot(new SlotItemHandler(blockEntity.inputItems, 0, 53, 24));            
            addSlotBox(blockEntity.outputItems, 0, 8, 63, 9, 18, 2, 18);
            trackTradeProgress();
        }
        layoutPlayerInventorySlots(this.playerInventory, 8, 104);
	}
	
	private void trackTradeProgress() {
		addGenericData(new GenericContainerData<>(GenericDataSerializers.INT32, new GetterAndSetter<Integer>(blockEntity::getTradeTime, blockEntity::setTradeTime)));
		addGenericData(new GenericContainerData<>(GenericDataSerializers.INT32, new GetterAndSetter<Integer>(blockEntity::getMaxTradeTime, blockEntity::setMaxTradeTime)));
		addGenericData(new GenericContainerData<>(GenericDataSerializers.BOOLEAN, new GetterAndSetter<Boolean>(blockEntity::isFull, blockEntity::setFull)));
    }

	@Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), playerEntity, BlockInit.PIGLIN_TRADING_POD_BLOCK.get());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if(index < 19) {
            	//Chest Area
            	if(!this.moveItemStackTo(stack, 46, 55, false)) {
	            	if (!this.moveItemStackTo(stack, 19, 47, false)) {
	                  return ItemStack.EMPTY;
	            	}
            	}
            }
            else {
            	if (!this.moveItemStackTo(stack, 0, 1, false)) {
            		if (index < 46 && !this.moveItemStackTo(stack, 46, 55, false)) {
                      return ItemStack.EMPTY;
            		}
            		else if (!this.moveItemStackTo(stack, 19, 46, false)) {
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
