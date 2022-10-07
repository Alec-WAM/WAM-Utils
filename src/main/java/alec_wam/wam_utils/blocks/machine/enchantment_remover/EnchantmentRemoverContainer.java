package alec_wam.wam_utils.blocks.machine.enchantment_remover;

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

public class EnchantmentRemoverContainer extends WAMContainerMenu {

	public EnchantmentRemoverBE blockEntity;
    private Player playerEntity;
    private IItemHandler playerInventory;
	
	public EnchantmentRemoverContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
		super(ContainerInit.ENCHANTMENT_REMOVER_CONTAINER.get(), windowId);
		blockEntity = (EnchantmentRemoverBE) player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        if (blockEntity != null) {
            blockEntity.inputItemHandler.ifPresent(h -> {
            	addSlot(new SlotItemHandler(h, 0, 35, 17));
            });
            
            blockEntity.outputItemHandler.ifPresent(h -> {
            	addSlot(new SlotItemHandler(h, 0, 35, 53));
            });
            
            blockEntity.enchantmentItemHandler.ifPresent(h -> {
                addSlotBox(h, 0, 80, 27, 5, 18, 2, 18);
            });
            
            trackProgressAndEnergy();
        }
        layoutPlayerInventorySlots(this.playerInventory, 8, 84);
	}
	
	private void trackProgressAndEnergy() {
		addGenericData(GenericContainerData.bool(blockEntity::isFull, blockEntity::setFull));
		addGenericData(GenericContainerData.energy(this.blockEntity.energyStorage));
		addGenericData(GenericContainerData.int32(blockEntity::getRemovalProgress, blockEntity::setRemovalProgress));
		addGenericData(GenericContainerData.int32(blockEntity::getMaxRemovalProgress, blockEntity::setMaxRemovalProgress));
    }

	@Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), playerEntity, BlockInit.ENCHANTMENT_REMOVER_BLOCK.get());
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if(index < 12) {
            	//Chest Area
            	if(!this.moveItemStackTo(stack, 38, 48, false)) {
	            	if (!this.moveItemStackTo(stack, 12, 38, false)) {
	                  return ItemStack.EMPTY;
	            	}
            	}
            }
            else {
            	if (!this.moveItemStackTo(stack, 0, 1, false)) {
            		if (index < 38 && !this.moveItemStackTo(stack, 38, 48, false)) {
            			return ItemStack.EMPTY;
            		}
            		else if (!this.moveItemStackTo(stack, 12, 38, false)) {
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
