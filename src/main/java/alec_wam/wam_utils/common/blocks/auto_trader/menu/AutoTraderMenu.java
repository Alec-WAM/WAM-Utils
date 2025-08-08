package alec_wam.wam_utils.common.blocks.auto_trader.menu;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.blocks.auto_trader.AutoTraderBE;
import alec_wam.wam_utils.common.menu.AbstractBaseBEMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class AutoTraderMenu extends AbstractBaseBEMenu<AutoTraderBE> {

    public static final int AUTO_TRADER_SLOTS = 7;

    public MerchantOffers offers;
    public MerchantOffer selectedOffer;
    public final AbstractVillager villagerEntity;

    public AutoTraderMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        super(ModInit.VILLAGER_AUTO_TRADER_MENU_TYPE.get(), containerId, playerInv, extraData);

        boolean hasVillager = extraData.readBoolean();
        if(hasVillager){
            Level level = playerInv.player.level();
            Entity entity = level.getEntity(extraData.readInt());
            this.villagerEntity = entity !=null && entity instanceof AbstractVillager ? (AbstractVillager)entity : null;
            this.offers = extraData.readNullable(MerchantOffers.STREAM_CODEC.mapStream(stream -> (RegistryFriendlyByteBuf) stream));
        }
        else {
            this.villagerEntity = null;
            this.offers = new MerchantOffers();
        }        
        this.selectedOffer = extraData.readNullable(MerchantOffer.STREAM_CODEC.mapStream(stream -> (RegistryFriendlyByteBuf) stream));
    }

    public AutoTraderMenu(int containerId, Inventory playerInv, final AutoTraderBE autoTraderBE) {
        super(ModInit.VILLAGER_AUTO_TRADER_MENU_TYPE.get(), containerId, playerInv, autoTraderBE);
        this.villagerEntity = autoTraderBE.getVillager();
        this.offers = this.villagerEntity !=null ? this.villagerEntity.getOffers() : new MerchantOffers();
        this.selectedOffer = autoTraderBE.getSelectedOffer();
    }

    @Override
    public void addSlots(Inventory playerInv){
        ItemStackHandler inventory = this.blockEntity.getInternalInventory();
        this.addSlot(new SlotItemHandler(inventory, 0, 16, 84));
        this.addSlot(new SlotItemHandler(inventory, 1, 34, 84));

        for(int i = 0; i < 5; i++) {
            this.addSlot(new SlotItemHandler(inventory, 2 + i, 72 + (i * 18), 84) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });            
        }

        this.addStandardInventorySlots(playerInv, 8, 116);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if(this.blockEntity == null) return ItemStack.EMPTY;
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < AUTO_TRADER_SLOTS) {
                if (!this.moveItemStackTo(itemstack1, AUTO_TRADER_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 2, false)) {
                int j = AUTO_TRADER_SLOTS + 27;
                int k = j + 9;
                if (index >= j && index < k) {
                    if (!this.moveItemStackTo(itemstack1, AUTO_TRADER_SLOTS, j, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= AUTO_TRADER_SLOTS && index < j) {
                    if (!this.moveItemStackTo(itemstack1, j, k, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, j, j, false)) {
                    return ItemStack.EMPTY;
                }

                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean isValidBlock(BlockState state) {
        return state.is(ModInit.VILLAGER_AUTO_TRADER_BLOCK.get());
    }
    
}
