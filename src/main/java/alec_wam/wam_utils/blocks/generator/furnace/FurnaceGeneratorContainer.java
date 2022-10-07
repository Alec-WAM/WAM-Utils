package alec_wam.wam_utils.blocks.generator.furnace;

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

public class FurnaceGeneratorContainer extends WAMContainerMenu {

	public FurnaceGeneratorBE blockEntity;
    private Player playerEntity;
    private IItemHandler playerInventory;
	
	public FurnaceGeneratorContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
		super(ContainerInit.FURNACE_GENERATOR_CONTAINER.get(), windowId);
		blockEntity = (FurnaceGeneratorBE) player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        if (blockEntity != null) {
            blockEntity.inputItemHandler.ifPresent(h -> {
                addSlot(new SlotItemHandler(h, 0, 44, 43));
            });
            
            blockEntity.chargeItemHandler.ifPresent(h -> {
                    addSlot(new SlotItemHandler(h, 0, 116, 35));
            });
            
            blockEntity.upgradeItemHandler.ifPresent(h -> {
                addSlot(new SlotItemHandler(h, 0, 152, 26));
                addSlot(new SlotItemHandler(h, 1, 152, 44));
            });
            trackFuelAndEnergyProgress();
        }
        layoutPlayerInventorySlots(this.playerInventory, 8, 84);
	}
	
	private void trackFuelAndEnergyProgress() {
		addGenericData(GenericContainerData.energy(this.blockEntity.energyStorage));
		addGenericData(new GenericContainerData<>(GenericDataSerializers.INT32, new GetterAndSetter<Integer>(blockEntity::getFuelAmount, blockEntity::setFuelAmount)));
		addGenericData(new GenericContainerData<>(GenericDataSerializers.INT32, new GetterAndSetter<Integer>(blockEntity::getMaxFuelAmount, blockEntity::setMaxFuelAmount)));
    }

	@Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), playerEntity, BlockInit.FURNACE_GENERATOR_BLOCK.get());
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
            	if(!this.moveItemStackTo(stack, 31, 40, false)) {
	            	if (!this.moveItemStackTo(stack, 4, 32, false)) {
	                  return ItemStack.EMPTY;
	            	}
            	}
            }
            else {
            	if (!this.moveItemStackTo(stack, 0, 4, false)) {
            		if (index < 31 && !this.moveItemStackTo(stack, 31, 40, false)) {
                      return ItemStack.EMPTY;
            		}
            		else if (!this.moveItemStackTo(stack, 4, 31, false)) {
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
