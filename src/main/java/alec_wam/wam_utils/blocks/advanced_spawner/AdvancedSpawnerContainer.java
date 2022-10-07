package alec_wam.wam_utils.blocks.advanced_spawner;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.capabilities.BlockEnergyStorage;
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
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

public class AdvancedSpawnerContainer extends WAMContainerMenu {

	public AdvancedSpawnerBE blockEntity;
    private Player playerEntity;
    private IItemHandler playerInventory;
	
	public AdvancedSpawnerContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
		super(ContainerInit.ADVANCED_SPAWNER_CONTAINER.get(), windowId);
		blockEntity = (AdvancedSpawnerBE) player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        if (blockEntity != null) {
            addSlot(new SlotItemHandler(blockEntity.inputItems, 0, 134, 17));
            addSlotBox(blockEntity.upgradeItems, 0, 125, 41, 2, 18, 2, 18);
            addSlotBox(blockEntity.outputItems, 0, 8, 85, 9, 18, 2, 18);
            trackKillAndEnergyProgress();
        }
        layoutPlayerInventorySlots(this.playerInventory, 8, 126);
	}
	
	private void trackKillAndEnergyProgress() {
        addGenericData(GenericContainerData.energy(this.blockEntity.energyStorage));
		addGenericData(new GenericContainerData<>(GenericDataSerializers.INT32, new GetterAndSetter<Integer>(blockEntity::getMaxKillTime, blockEntity::setMaxKillTime)));
        addGenericData(new GenericContainerData<>(GenericDataSerializers.INT32, new GetterAndSetter<Integer>(blockEntity::getKillProgress, blockEntity::setKillProgress)));
        addGenericData(new GenericContainerData<>(GenericDataSerializers.INT32, new GetterAndSetter<Integer>(blockEntity::getDamageCost, blockEntity::setDamageCost)));
    }

	@Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), playerEntity, BlockInit.ADVANCED_SPAWNER_BLOCK.get());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
//            if(index < 19) {
//            	//Chest Area
//            	if (!this.moveItemStackTo(stack, 19, 55, false)) {
//                  return ItemStack.EMPTY;
//            	}
//            }
//            else {
//            	if (!this.moveItemStackTo(stack, 0, 1, false)) {
//            		if (index < 46 && !this.moveItemStackTo(stack, 46, 55, false)) {
//                      return ItemStack.EMPTY;
//            		}
//            		else if (!this.moveItemStackTo(stack, 19, 46, false)) {
//                        return ItemStack.EMPTY;
//              		}
//            	}
//            }
            WAMUtilsMod.LOGGER.debug("Index:" + index);
            if(index < 23) {
            	//Chest Area
            	if(!this.moveItemStackTo(stack, 50, 59, false)) {
	            	if (!this.moveItemStackTo(stack, 23, 51, false)) {
	                  return ItemStack.EMPTY;
	            	}
            	}
            }
            else {
            	if (!this.moveItemStackTo(stack, 0, 1, false)) {
            		boolean moved = false;
            		if(this.blockEntity.isItemAValidUpgradeItem(stack)) {
        				if (this.moveItemStackTo(stack, 1, 5, false)) {
        					moved = true;
                  		}
        			}
            		
            		if(!moved) {
	            		if (index < 51 && !this.moveItemStackTo(stack, 50, 59, false)) {
	                      return ItemStack.EMPTY;
	            		}
	            		else if (!this.moveItemStackTo(stack, 23, 49, false)) {
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
