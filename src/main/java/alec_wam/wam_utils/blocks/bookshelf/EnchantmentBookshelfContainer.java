package alec_wam.wam_utils.blocks.bookshelf;

import alec_wam.wam_utils.init.ContainerInit;
import alec_wam.wam_utils.server.container.WAMContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

public class EnchantmentBookshelfContainer extends WAMContainerMenu {

	public EnchantmentBookshelfBE blockEntity;
    private Player playerEntity;
    private IItemHandler playerInventory;
	
	public EnchantmentBookshelfContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
		super(ContainerInit.ENCHANTMENT_BOOKSHELF_CONTAINER.get(), windowId);
		blockEntity = (EnchantmentBookshelfBE) player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        if (blockEntity != null) {
            addSlotBox(blockEntity.bookItems, 0, 22, 19, 8, 18, 3, 22);
        }
        layoutPlayerInventorySlots(this.playerInventory, 13, 93);
	}

	@Override
    public boolean stillValid(Player playerIn) {
		ContainerLevelAccess access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        return access.evaluate((p_38916_, p_38917_) -> {
            return !(p_38916_.getBlockState(p_38917_).getBlock() instanceof EnchantmentBookshelfBlock) ? false : playerIn.distanceToSqr((double)p_38917_.getX() + 0.5D, (double)p_38917_.getY() + 0.5D, (double)p_38917_.getZ() + 0.5D) <= 64.0D;
        }, true);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if(index < 24) {
            	//Chest Area
            	if (!this.moveItemStackTo(stack, 51, 60, false)) {
	            	if (!this.moveItemStackTo(stack, 24, 51, false)) {
	                  return ItemStack.EMPTY;
	            	}
            	}
            }
            else {
            	if (!this.moveItemStackTo(stack, 0, 24, false)) {
            		if (index < 46 && !this.moveItemStackTo(stack, 51, 60, false)) {
                      return ItemStack.EMPTY;
            		}
            		else if (!this.moveItemStackTo(stack, 24, 51, false)) {
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
