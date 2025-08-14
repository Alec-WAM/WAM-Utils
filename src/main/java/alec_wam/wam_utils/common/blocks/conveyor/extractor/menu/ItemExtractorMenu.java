package alec_wam.wam_utils.common.blocks.conveyor.extractor.menu;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.blocks.conveyor.ItemFilterList;
import alec_wam.wam_utils.common.blocks.conveyor.extractor.ItemExtractorBE;
import alec_wam.wam_utils.common.menu.AbstractBaseBEMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ItemExtractorMenu extends AbstractBaseBEMenu<ItemExtractorBE>{

    public SimpleContainer fakeSlotContainer;
    private ItemFilterList filterList;
    public boolean fakeItemSlotEnabled = false;

    public ItemExtractorMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        super(ModInit.ITEM_EXTRACTOR_MENU_TYPE.get(), containerId, playerInv, extraData);
        this.filterList = extraData.readNullable(ItemFilterList.STREAM_CODEC.mapStream(stream -> (RegistryFriendlyByteBuf) stream));
    }

	public ItemExtractorMenu(int containerId, Inventory playerInv,
			ItemExtractorBE blockEntity) {
		super(ModInit.ITEM_EXTRACTOR_MENU_TYPE.get(), containerId, playerInv, blockEntity);
        this.filterList = blockEntity.getFilterList();
	}

    public ItemFilterList getFilterList() {
        return this.filterList;
    }

	@Override
	public void addSlots(Inventory playerInv) {
        this.fakeSlotContainer = new SimpleContainer(1);

        this.addSlot(new Slot(this.fakeSlotContainer, 0, 69, 60) {

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 1;
            }

            // @Override
            // public ItemStack safeInsert(ItemStack stack, int increment) {
            //     this.setByPlayer(stack);
            //     return stack;
            // }

            @Override
            public void set(ItemStack stack) {
                // ItemStack correctNewStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
                // if(ItemExtractorMenu.this.editFilter != null){
                //     Optional<ItemStack> stackOpt = !correctNewStack.isEmpty() ? Optional.of(correctNewStack) : Optional.empty();
                //     ItemExtractorMenu.this.editFilter.setStack(stackOpt);
                // }
                super.set(stack);
            }

            @Override
            public ItemStack getItem(){
                return super.getItem();
                // return ItemExtractorMenu.this.editFilter != null ? ItemExtractorMenu.this.editFilter.getStack().orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
            
            @Override
            public boolean isActive() {
                return ItemExtractorMenu.this.fakeItemSlotEnabled;
            }
        });

        this.addStandardInventorySlots(playerInv, 19, 127);
	}

	@Override
	public boolean isValidBlock(BlockState state) {
		return state.is(ModInit.ITEM_EXTRACTOR_BLOCK);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if(this.blockEntity == null) return ItemStack.EMPTY;
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if(index > 0) {   
                
                int j = 1 + 27;
                int k = j + 9;
                if (index >= j && index < k) {
                    if (!this.moveItemStackTo(itemstack1, 1, j, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 1 && index < j) {
                    if (!this.moveItemStackTo(itemstack1, j, k, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, j, j, false)) {
                    return ItemStack.EMPTY;
                }

                return ItemStack.EMPTY;
            }

            // if (itemstack1.isEmpty()) {
            //     slot.setByPlayer(ItemStack.EMPTY);
            // } else {
            //     slot.setChanged();
            // }
        }

        return itemstack;
	}
    
}
