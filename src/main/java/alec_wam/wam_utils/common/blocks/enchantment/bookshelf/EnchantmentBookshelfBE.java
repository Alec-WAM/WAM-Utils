package alec_wam.wam_utils.common.blocks.enchantment.bookshelf;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.blocks.BaseBE;
import alec_wam.wam_utils.common.blocks.enchantment.bookshelf.menu.EnchantmentBookshelfMenu;
import alec_wam.wam_utils.common.helpers.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class EnchantmentBookshelfBE extends BaseBE implements MenuProvider{

    public static final int BOOK_SLOTS = 24;
    protected final ItemStackHandler inventory = createBookItemHandler();

    private EnchantmentCategoryFilter filter = EnchantmentCategoryFilter.ALL;

    public EnchantmentBookshelfBE(BlockPos pos, BlockState blockState) {
        super(ModInit.ENCHANTMENT_BOOK_SHELF_BLOCK_ENTITY.get(), pos, blockState);
    }

    public EnchantmentCategoryFilter getFilter() {
        return filter;
    }

    public void setFilter(EnchantmentCategoryFilter filter) {
        this.filter = filter;
    }

    @Override
    public void handleCustomMessage(String messageType, CompoundTag messageData, boolean isClient) {
        if(messageType.equals("set_filter")) {
            this.filter = EnchantmentCategoryFilter.values()[messageData.getIntOr("filter", 0)];
            this.setChanged();
            this.markDirtyClient();
        }
    }
    
    @Override
    public void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.storeNullable("filter", EnchantmentCategoryFilter.CODEC, filter);
    }

    @Override
    public void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        filter = valueInput.read("filter", EnchantmentCategoryFilter.CODEC).orElse(EnchantmentCategoryFilter.ALL);
    }


    @Override
    public ItemStackHandler getInternalInventory() {
        return this.inventory;
    }

	@Override
	public IItemHandler getExternalItemHandler(@Nullable Direction side) {
		return inventory;
	}

    public boolean isValidBookForSlot(ItemStack stack, int slot) {
		if(!stack.isEmpty()) {
            //TODO Make this a tag
			if(stack.is(Items.ENCHANTED_BOOK)) {
				if(this.filter == EnchantmentCategoryFilter.ALL) {
					return true;
				}
				
				Set<Holder<Enchantment>> enchantments = ItemHelper.getEnchantments(stack);
                if(enchantments.isEmpty()) {
                    return false;
                }
                return enchantments.stream().anyMatch(filter.getFilter());
			}
			if(stack.getItem() == Items.WRITTEN_BOOK) {
				return true;			
			}
		}
		return false;
	}

    @Nonnull
	private ItemStackHandler createBookItemHandler() {
		return new ItemStackHandler(BOOK_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				setChanged();
				markDirtyClient();
			}

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return isValidBookForSlot(stack, slot);
			}
		};
	}

    @Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new EnchantmentBookshelfMenu(containerId, playerInventory, this);
	}

	@Override
	public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.getBlockPos());
	}

    @Override
    public Component getDisplayName() {
        return Component.translatable("wamutils.container.enchantment_bookshelf.title");
    }
    
}
