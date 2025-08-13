package alec_wam.wam_utils.common.blocks.conveyor.extractor;

import java.util.Optional;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.blocks.BaseBE;
import alec_wam.wam_utils.common.blocks.conveyor.ItemFilter;
import alec_wam.wam_utils.common.blocks.conveyor.ItemFilterList;
import alec_wam.wam_utils.common.blocks.conveyor.extractor.menu.ItemExtractorMenu;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.IItemHandler;

public class ItemExtractorBE extends BaseBE implements MenuProvider{

    private ItemFilterList filterList;
    private int transferDelay = 0;

	public ItemExtractorBE(BlockPos pos, BlockState blockState) {
		super(ModInit.ITEM_EXTRACTOR_BLOCK_ENTITY.get(), pos, blockState);
        this.filterList = new ItemFilterList();
	}

    @Override
    public void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.store("Filters", ItemFilterList.CODEC, filterList);
        valueOutput.putInt("TransferDelay", transferDelay);
    }

    @Override
    public void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        Optional<ItemFilterList> loadedList = valueInput.read("Filters", ItemFilterList.CODEC);
        if(loadedList.isPresent()) {
            this.filterList = loadedList.get();
        }
        else {
            this.filterList.clear();
        }
        this.transferDelay = valueInput.getIntOr("TransferDelay", 0);
    }

    @Override
    public void tickServer() {
        super.tickServer();

        if(this.transferDelay > 0) {
            this.transferDelay--;
            return;
        }

        this.transferItems();
    }

    public void transferItems(){
        Direction facing = this.getBlockState().getValue(BlockStateProperties.FACING);
        BlockPos thisPos = this.getBlockPos();
        BlockPos attachedPos = thisPos.relative(facing.getOpposite());
        BlockPos facingPos = thisPos.relative(facing);
        Optional<IItemHandler> attachedInv = BlockHelper.getItemHandler(this.level, attachedPos, facing);
        Optional<IItemHandler> facingInv = BlockHelper.getItemHandler(this.level, facingPos, facing.getOpposite());
        if(attachedInv.isPresent()) {
            if(!facingInv.isPresent()){
                this.dropItems(attachedInv.get());
                return;
            }
            else {
                this.transferItem(attachedInv.get(), facingInv.get());
            }
        }
    }

    public void dropItems(IItemHandler attachedInv){

    }

    public void transferItem(IItemHandler attachedInv, IItemHandler facingInv) {
        for(int i = 0; i < attachedInv.getSlots(); i++) {
            ItemStack stack = attachedInv.getStackInSlot(i);
            //TODO Make this a limited size transfer
            if(!stack.isEmpty() && this.isStackAllowed(stack)) {
                final int originalCount = stack.getCount();
                ItemStack testInsert = BlockHelper.insertItemStacked(facingInv, stack.copy(), true);
                if(testInsert.getCount() != originalCount) {
                    ItemStack extractedStack = attachedInv.extractItem(i, originalCount - testInsert.getCount(), false);
                    BlockHelper.insertItemStacked(facingInv, extractedStack, false);
                    this.delayTransfer();
                    return;
                }
            }
        }
    }

    public void delayTransfer() {
        this.transferDelay = 1 * 20; // 1 second
    }

    public boolean isStackAllowed(ItemStack stack) {
        return this.filterList.isEmpty() || this.filterList.passesAllFilters(stack);
    }

    public boolean updateFilter(int index, ItemFilter filter) {
        if(index < this.filterList.size()) {
            this.filterList.set(index, filter);
            return true;
        }
        return false;
    }

    public ItemFilter getFilter(int index) {
        if(index < this.filterList.size()) {
            return this.filterList.get(index);
        }
        return null;
    }

    public ItemFilterList getFilterList() {
        return this.filterList;
    }

    @Override
    public void handleCustomMessage(String messageType, ValueInput valueInput, boolean isClient) {
        if(messageType.equalsIgnoreCase("AddFilter")) {
            ItemFilter filter = valueInput.read("Filter", ItemFilter.CODEC).orElse(null);
            if(filter == null) return;
            this.filterList.add(filter);
            this.setChanged();
            this.markDirtyClient();
        }
        else if(messageType.equalsIgnoreCase("UpdateFilter")) {
            int index = valueInput.getIntOr("index", -1);
            ItemFilter filter = valueInput.read("Filter", ItemFilter.CODEC).orElse(null);
            if(filter != null && index >= 0 && index < this.filterList.size()) {
                this.filterList.set(index, filter);
                this.setChanged();
                this.markDirtyClient();
            }
        }
        else if(messageType.equalsIgnoreCase("RemoveFilter")) {
            int index = valueInput.getIntOr("index", -1);
            if(index >= 0 && index < this.filterList.size()) {
                this.filterList.remove(index);
                this.setChanged();
                this.markDirtyClient();
            }
        }
        super.handleCustomMessage(messageType, valueInput, isClient);
    }

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		return new ItemExtractorMenu(containerId, playerInventory, this);
	}

	@Override
	public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.getBlockPos());
        buffer.writeNullable(this.filterList, ItemFilterList.STREAM_CODEC.mapStream(stream -> (RegistryFriendlyByteBuf) stream));
	}

    @Override
    public Component getDisplayName() {
        return Component.translatable("wamutils.container.item_extractor.title");
    }
    
}
