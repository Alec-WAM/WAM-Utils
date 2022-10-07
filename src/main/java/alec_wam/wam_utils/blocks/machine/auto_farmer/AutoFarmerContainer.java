package alec_wam.wam_utils.blocks.machine.auto_farmer;

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

public class AutoFarmerContainer extends WAMContainerMenu {

	public AutoFarmerBE blockEntity;
    private Player playerEntity;
    private IItemHandler playerInventory;
	
	public AutoFarmerContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
		super(ContainerInit.AUTO_FARMER_CONTAINER.get(), windowId);
		blockEntity = (AutoFarmerBE) player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        if (blockEntity != null) {
            addSlotRange(blockEntity.seedItems, 0, 45, 34, AutoFarmerBE.INPUT_SLOTS, 18);
            addSlot(new SlotItemHandler(blockEntity.upgradeItems, 0, 143, 25){
            	@Override
            	public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            		return Pair.of(InventoryMenu.BLOCK_ATLAS, GuiUtils.EMPTY_SLOT_BOOK);
            	}
            });
            addSlot(new SlotItemHandler(blockEntity.upgradeItems, 1, 143, 43));
            addSlotBox(blockEntity.outputItems, 0, 8, 85, 9, 18, 2, 18);
            trackEnergyProgress();
        }
        layoutPlayerInventorySlots(this.playerInventory, 8, 126);
	}
	
	private void trackEnergyProgress() {
		addGenericData(GenericContainerData.energy(this.blockEntity.energyStorage));
    }

	@Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), playerEntity, BlockInit.AUTO_FARMER_BLOCK.get());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if(index < 25) {
            	//Chest Area
            	if(!this.moveItemStackTo(stack, 52, 61, false)) {
	            	if (!this.moveItemStackTo(stack, 25, 52, false)) {
	                  return ItemStack.EMPTY;
	            	}
            	}
            }
            else {
            	if (!this.moveItemStackTo(stack, 5, 6, false)) {
	            	if (!this.moveItemStackTo(stack, 0, 5, false)) {
	            		if (index < 52 && !this.moveItemStackTo(stack, 52, 61, false)) {
	                      return ItemStack.EMPTY;
	            		}
	            		else if (!this.moveItemStackTo(stack, 25, 52, false)) {
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
