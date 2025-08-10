package alec_wam.wam_utils.common.blocks.creative.item_stock;

import javax.annotation.Nullable;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.blocks.BaseBE;
import alec_wam.wam_utils.common.blocks.creative.item_stock.menu.CreativeStockItemMenu;
import alec_wam.wam_utils.common.helpers.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class CreativeItemStockBE extends BaseBE implements MenuProvider {

    private final ItemStackHandler inventory = new ItemStackHandler(1);
    private int itemStockCount;
    private int itemDelay;    
	private boolean isRedstonePowered = false;
    
	public CreativeItemStockBE(BlockPos pos, BlockState blockState) {
		super(ModInit.CREATIVE_STOCKER_ITEM_BLOCK_ENTITY.get(), pos, blockState);
	}

    @Override
    public void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.putBoolean("redstonePowered", isRedstonePowered);
        valueOutput.putInt("itemStockCount", itemStockCount);
        valueOutput.putInt("itemDelay", itemDelay);
    }

    @Override
    public void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        this.isRedstonePowered = valueInput.getBooleanOr("redstonePowered", false);
        this.itemStockCount = valueInput.getIntOr("itemStockCount", 0);
        this.itemDelay = valueInput.getIntOr("itemDelay", 0);
    }

    @Override
    public ItemStackHandler getInternalInventory() {
        return inventory;
    }

    @Override
    public IItemHandler getExternalItemHandler(@Nullable Direction side) {
        // TODO Make this a "copy" inventory that does not affect the original
        return inventory;
    }

    @Override
    public void tickServer(){
        super.tickServer();
        boolean redstonePowered = getLevel().hasNeighborSignal(getBlockPos());
        if(this.isRedstonePowered != redstonePowered) {
        	this.isRedstonePowered = redstonePowered;
        	this.markDirtyClient();
        }
        
        if(this.isRedstonePowered){
            if(itemDelay > 0) {
                itemDelay--;
                return;
            }
            this.stockItem();
        }
    }

    public void stockItem(){
        Direction facing = this.getBlockState().getValue(CreativeItemStockBlock.FACING);
        BlockPos facingPos = this.getBlockPos().relative(facing);
        IItemHandler facingInvHandler = BlockHelper.getItemHandler(getLevel(), facingPos, facing.getOpposite()).orElse(null);
        if(facingInvHandler == null) return;

        ItemStack fillStack = this.inventory.getStackInSlot(0);
        if(fillStack.isEmpty()) return;
        int currentCount = BlockHelper.countItems(facingInvHandler, stack -> ItemStack.isSameItemSameComponents(stack, fillStack));
    
        //TODO Make this match the itemStockCount amount
        if(currentCount < fillStack.getCount()) {
            ItemStack insertStack = fillStack.copyWithCount(1);
            BlockHelper.insertItemStacked(facingInvHandler, insertStack, false);
            itemDelay = 20;
        }
    }

    @Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new CreativeStockItemMenu(containerId, playerInventory, this);
	}

	@Override
	public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.getBlockPos());
	}

    @Override
    public Component getDisplayName() {
        return Component.translatable("wamutils.container.creative_stocker_item.title");
    }
    
}
