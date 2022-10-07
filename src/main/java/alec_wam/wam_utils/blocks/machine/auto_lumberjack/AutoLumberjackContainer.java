package alec_wam.wam_utils.blocks.machine.auto_lumberjack;

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

public class AutoLumberjackContainer extends WAMContainerMenu {

	public AutoLumberjackBE blockEntity;
    private Player playerEntity;
    private IItemHandler playerInventory;
	
	public AutoLumberjackContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
		super(ContainerInit.AUTO_LUMBERJACK_CONTAINER.get(), windowId);
		blockEntity = (AutoLumberjackBE) player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        if (blockEntity != null) {
            addSlot(new SlotItemHandler(blockEntity.seedItems, 0, 80, 34));
            addSlot(new SlotItemHandler(blockEntity.upgradeItems, 0, 143, 34) {
            	@Override
            	public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            		return Pair.of(InventoryMenu.BLOCK_ATLAS, GuiUtils.EMPTY_SLOT_BOOK);
            	}
            });
            addSlotBox(blockEntity.outputItems, 0, 8, 85, 9, 18, 2, 18);
            trackEnergyProgress();
        }
        layoutPlayerInventorySlots(this.playerInventory, 8, 126);
	}
	
	private void trackEnergyProgress() {
		addGenericData(GenericContainerData.energy(this.blockEntity.energyStorage));    
        addGenericData(GenericContainerData.bool(blockEntity::isFull, blockEntity::setFull));           
    }

	@Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), playerEntity, BlockInit.AUTO_LUMBERJACK_BLOCK.get());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if(index < 20) {
            	//Chest Area
            	if(!this.moveItemStackTo(stack, 47, 56, false)) {
	            	if (!this.moveItemStackTo(stack, 20, 48, false)) {
	                  return ItemStack.EMPTY;
	            	}
            	}
            }
            else {
            	if (!this.moveItemStackTo(stack, 1, 2, false)) {
	            	if (!this.moveItemStackTo(stack, 0, 1, false)) {
	            		if (index < 47 && !this.moveItemStackTo(stack, 47, 56, false)) {
	                      return ItemStack.EMPTY;
	            		}
	            		else if (!this.moveItemStackTo(stack, 20, 47, false)) {
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
