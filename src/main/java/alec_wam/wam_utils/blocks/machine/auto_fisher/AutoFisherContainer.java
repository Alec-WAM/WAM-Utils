package alec_wam.wam_utils.blocks.machine.auto_fisher;

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

public class AutoFisherContainer extends WAMContainerMenu {

	public AutoFisherBE blockEntity;
    private Player playerEntity;
    private IItemHandler playerInventory;
	
	public AutoFisherContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
		super(ContainerInit.AUTO_FISHER_CONTAINER.get(), windowId);
		blockEntity = (AutoFisherBE) player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        if (blockEntity != null) {
            addSlotRange(blockEntity.fishItems, 0, 44, 61, 5, 18);
            addSlot(new SlotItemHandler(blockEntity.upgradeItems, 0, 143, 24) {
            	@Override
            	public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            		return Pair.of(InventoryMenu.BLOCK_ATLAS, GuiUtils.EMPTY_SLOT_BOOK);
            	}
            });
            addSlot(new SlotItemHandler(blockEntity.upgradeItems, 1, 143, 42));   
            trackProgressAndEnergy();
        }
        layoutPlayerInventorySlots(this.playerInventory, 8, 84);
	}
	
	private void trackProgressAndEnergy() {
		addGenericData(GenericContainerData.bool(blockEntity::isFull, blockEntity::setFull));               
		addGenericData(GenericContainerData.bool(blockEntity::hasWater, blockEntity::setHasWater));         
        addGenericData(GenericContainerData.energy(this.blockEntity.energyStorage));
        addGenericData(GenericContainerData.int32(blockEntity::getFishingCost, blockEntity::setFishingCost));   
		addGenericData(GenericContainerData.int32(blockEntity::getMaxFishingTime, blockEntity::setMaxFishingTime));
        addGenericData(GenericContainerData.int32(blockEntity::getFishingTime, blockEntity::setFishingTime));   
    }

	@Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), playerEntity, BlockInit.AUTO_FISHER_BLOCK.get());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if(index < 7) {
            	//Chest Area
            	if(!this.moveItemStackTo(stack, 36, 43, false)) {
	            	if (!this.moveItemStackTo(stack, 7, 36, false)) {
	                  return ItemStack.EMPTY;
	            	}
            	}
            }
            else {
            	if (!this.moveItemStackTo(stack, 5, 7, false)) {
	            	if (!this.moveItemStackTo(stack, 0, 5, false)) {
	            		if (index < 31 && !this.moveItemStackTo(stack, 36, 43, false)) {
	                      return ItemStack.EMPTY;
	            		}
	            		else if (!this.moveItemStackTo(stack, 7, 36, false)) {
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
