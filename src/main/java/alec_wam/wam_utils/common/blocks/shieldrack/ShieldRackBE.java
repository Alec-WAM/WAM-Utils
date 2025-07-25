package alec_wam.wam_utils.common.blocks.shieldrack;

import java.util.function.Predicate;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.blocks.BaseBE;
import alec_wam.wam_utils.common.helpers.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ShieldRackBE extends BaseBE {

    public static final int INVENTORY_SIZE = 3;
    public static final int LEFT_SLOT = 0;
    public static final int RIGHT_SLOT = 1;
    public static final int SHIELD_SLOT = 2;

    public static final Predicate<ItemStack> IS_VALID_SIDE_ITEM = stack -> ItemHelper.isMeleeWeapon(stack) || ItemHelper.isRangedWeapon(stack) || ItemHelper.isGenericWeapon(stack);

    public static class ShieldRackInventory extends ItemStackHandler {
        
        private final ShieldRackBE shieldRackBE;

        public ShieldRackInventory(ShieldRackBE shieldRackBE) {
            super(INVENTORY_SIZE);
            this.shieldRackBE = shieldRackBE;
        }

        public ShieldRackInventory(ShieldRackBE shieldRackBE, NonNullList<ItemStack> stacks) {
            super(stacks);
            this.shieldRackBE = shieldRackBE;
        }
        
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == LEFT_SLOT || slot == RIGHT_SLOT) {
                return stack.isEmpty() || IS_VALID_SIDE_ITEM.test(stack);
            } else if (slot == SHIELD_SLOT) {
                return stack.isEmpty() || ItemHelper.isShield(stack);
            }
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            if (shieldRackBE != null) {
                shieldRackBE.markDirtyClient();
            }
        }
    }

    public ShieldRackBE(BlockPos pos, BlockState blockState) {
        super(ModInit.SHIELDRACK_BLOCK_ENTITY.get(), pos, blockState);
    }

	public ItemStackHandler getInventory() {
        return getData(ModInit.ITEM_HANDLER_ATTACHMENT);
    }

	@Override
	public IItemHandler getItemHandler() {
		return getInventory();
	}

    @Override
    public int getInventorySize() {
        return INVENTORY_SIZE;
    }
	
	public ItemStack getLeftStack() {
		return getInventory().getStackInSlot(LEFT_SLOT);
	}

	public void setLeftStack(ItemStack leftStack) {
		getInventory().setStackInSlot(LEFT_SLOT, leftStack);
	}

	public ItemStack getRightStack() {
		return getInventory().getStackInSlot(RIGHT_SLOT);
	}

	public void setRightStack(ItemStack rightStack) {
		getInventory().setStackInSlot(RIGHT_SLOT, rightStack);
	}

	public ItemStack getShieldStack() {
		return getInventory().getStackInSlot(SHIELD_SLOT);
	}

	public void setShieldStack(ItemStack shieldStack) {
		getInventory().setStackInSlot(SHIELD_SLOT, shieldStack);
	}
	
	// @Override
	// public AABB getRenderBoundingBox() {
	// 	BlockPos pos = this.getBlockPos();
	// 	return new AABB(pos).inflate(0.2);
	// }
}
