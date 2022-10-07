package alec_wam.wam_utils.blocks.machine.quarry;

import com.mojang.datafixers.util.Pair;

import alec_wam.wam_utils.client.GuiUtils;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ContainerInit;
import alec_wam.wam_utils.server.container.GenericContainerData;
import alec_wam.wam_utils.server.container.WAMContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

public class QuarryContainer extends WAMContainerMenu {

	public QuarryBE blockEntity;
    private Player playerEntity;
    private IItemHandler playerInventory;
	
	public QuarryContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
		super(ContainerInit.QUARRY_CONTAINER.get(), windowId);
		blockEntity = (QuarryBE) player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        if (blockEntity != null) {
           addSlot(new SlotItemHandler(blockEntity.upgradeItems, 0, 152, 27) {
            	@Override
            	public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            		return Pair.of(InventoryMenu.BLOCK_ATLAS, GuiUtils.EMPTY_SLOT_BOOK);
            	}
            });
           	addSlot(new SlotItemHandler(blockEntity.upgradeItems, 1, 152, 45));
            addSlotBox(blockEntity.outputItems, 0, 8, 67, 9, 18, 3, 18);
            trackEnergyProgress();
        }
        layoutPlayerInventorySlots(this.playerInventory, 8, 126);
	}
	
	private void trackEnergyProgress() {
		addGenericData(GenericContainerData.energy(this.blockEntity.energyStorage));
		addGenericData(GenericContainerData.bool(this.blockEntity::isDoneMining, this.blockEntity::setDoneMining));
		addGenericData(GenericContainerData.bool(this.blockEntity::isFull, this.blockEntity::setFull));
    }

	@Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), playerEntity, BlockInit.QUARRY_BLOCK.get());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if(index < 29) {
            	//Chest Area
            	if(!this.moveItemStackTo(stack, 56, 65, false)) {
	            	if (!this.moveItemStackTo(stack, 29, 56, false)) {
	                  return ItemStack.EMPTY;
	            	}
            	}
            }
            else {
            	if (!this.moveItemStackTo(stack, 0, 1, false)) {
            		if (!this.moveItemStackTo(stack, 1, 29, false)) {
	            		if (index < 56 && !this.moveItemStackTo(stack, 56, 65, false)) {
	                      return ItemStack.EMPTY;
	            		}
	            		else if (!this.moveItemStackTo(stack, 29, 56, false)) {
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
