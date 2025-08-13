package alec_wam.wam_utils.common.blocks.auto_trader;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.blocks.BaseBE;
import alec_wam.wam_utils.common.blocks.auto_trader.menu.AutoTraderMenu;
import alec_wam.wam_utils.common.helpers.BlockHelper;
import alec_wam.wam_utils.common.helpers.EntityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

public class AutoTraderBE extends BaseBE implements MenuProvider {

    public static final int SLOTS = 7;
    public static final int OUTPUT_INDEX_START = 2;
    public static final int OUTPUT_INDEX_END = 7;
    protected final ItemStackHandler inventory = createItemHandler();
    private final RangedWrapper inputInventory = new RangedWrapper(this.inventory, 0, 2);
    private final RangedWrapper outputInventory = new RangedWrapper(this.inventory, OUTPUT_INDEX_START, OUTPUT_INDEX_END) {
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }
    };

    private UUID villagerUUID;
    private AbstractVillager villager;
    private MerchantOffer selectedOffer;
    private MerchantOffers offerCache;
    private int tradingDelay = 0;

    public AutoTraderBE(BlockPos pos, BlockState blockState) {
        super(ModInit.VILLAGER_AUTO_TRADER_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.storeNullable("villagerUUID", UUIDUtil.CODEC, this.villagerUUID);
        valueOutput.storeNullable("selectedOffer", MerchantOffer.CODEC, this.selectedOffer);
    }

    @Override
    public void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        this.villagerUUID = valueInput.read("villagerUUID", UUIDUtil.CODEC).orElse(null);
        this.selectedOffer = valueInput.read("selectedOffer", MerchantOffer.CODEC).orElse(null);
    }

    @Override
    public void tickServer() {
        super.tickServer();

        if(this.villagerUUID !=null){
            Level level = this.getLevel();
            Entity entity = level.getEntity(villagerUUID);
            if(entity != null && entity instanceof AbstractVillager villager){
                this.villager = villager;
                if(this.offerCache == null || this.offerCache.size() != villager.getOffers().size()){
                    this.updateOffers(villager.getOffers());
                }
            }
            else {
                this.villager = null;
                this.updateOffers(null);
            }
        }

        if(this.selectedOffer !=null && !this.selectedOffer.isOutOfStock()){
            if(this.tradingDelay <= 0) {
                if(this.performTrade()) {
                    this.tradingDelay = 2 * 20; // 2 seconds
                }
            }
            else {
                this.tradingDelay--;
            }
        }
    }

    public void updateOffers(MerchantOffers newOffers){
        this.offerCache = newOffers;
        if(newOffers !=null){
            if(this.selectedOffer !=null){
                int index = EntityHelper.getOfferIndex(newOffers, this.selectedOffer);
                if(index >= 0 && index < newOffers.size()){
                    this.selectedOffer = newOffers.get(index);
                }
            }
            else {
                this.selectedOffer = newOffers.get(0);
            }
        }
        else {
            this.selectedOffer = null;
        }

        //TODO Sync to client
    }

    public AbstractVillager getVillager(){
        return this.villager;
    }

    public MerchantOffer getSelectedOffer(){
        return this.selectedOffer;
    }

    public boolean performTrade(){        
        if (this.villager !=null && this.selectedOffer != null && !this.selectedOffer.isOutOfStock()) {
            this.mergeStacks();

            ItemStack itemstack = this.inventory.getStackInSlot(0);
            ItemStack itemstack1 = this.inventory.getStackInSlot(1);
            ItemStack result = this.selectedOffer.assemble();
            if(BlockHelper.insertItemStacked(this.inventory, result.copy(), OUTPUT_INDEX_START, OUTPUT_INDEX_END,true).isEmpty()){
                if (
                    this.selectedOffer.take(itemstack, itemstack1) 
                    || this.selectedOffer.take(itemstack1, itemstack) 
                    || this.selectedOffer.take(itemstack, ItemStack.EMPTY)
                ) {
                    this.selectedOffer.increaseUses();
                    // System.out.println("Performing Trade: " + this.selectedOffer.getUses() + "/" + this.selectedOffer.getMaxUses());
                    
                    this.inventory.setStackInSlot(0, itemstack);
                    this.inventory.setStackInSlot(1, itemstack1);

                    if(this.villager instanceof Villager realVillager){
                        realVillager.setVillagerXp(realVillager.getVillagerXp() + this.selectedOffer.getXp());
                        if(this.shouldIncreaseLevel(realVillager)) {
                            //This requires AccessTransformer to work
                            realVillager.increaseMerchantCareer();
                            realVillager.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
                        }
                    }
                    else {
                        this.villager.overrideXp(this.villager.getVillagerXp() + this.selectedOffer.getXp());
                    }
                    BlockHelper.insertItemStacked(this.inventory, result, OUTPUT_INDEX_START, OUTPUT_INDEX_END,false);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean shouldIncreaseLevel(Villager realVillager){
        if(realVillager == null) return false;
        int i = realVillager.getVillagerData().level();
        return VillagerData.canLevelUp(i) && realVillager.getVillagerXp() >= VillagerData.getMaxXpPerLevel(i);
    }

    public void mergeStacks(){
        ItemStack itemstack = this.inventory.getStackInSlot(0);
        ItemStack itemstack1 = this.inventory.getStackInSlot(1);
        if(this.selectedOffer !=null){
            if(this.selectedOffer.getCostB().isEmpty() && ItemStack.isSameItemSameComponents(itemstack1, this.selectedOffer.getCostA())){
                if(itemstack.isEmpty() || ItemStack.isSameItemSameComponents(itemstack, itemstack1)){
                    ItemStack remainder = this.inventory.insertItem(0, itemstack1, false);
                    this.inventory.setStackInSlot(1, remainder);
                }
            }
        }
    }

    @Nonnull
	private ItemStackHandler createItemHandler() {
		return new ItemStackHandler(SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				setChanged();
				markDirtyClient();
			}

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                //TODO Prevent inserting items into other slots
                return super.insertItem(slot, stack, simulate);
            }
		};
	}

    @Override
    public ItemStackHandler getInternalInventory() {
        return this.inventory;
    }

    @Override
    public IItemHandler getExternalItemHandler(@Nullable Direction side) {
        if(side == null) return this.inventory;
        if(side == Direction.DOWN) return this.outputInventory;
        return this.inputInventory;
    }

    @Override
    public void saveToItem(ItemStack stack) {
        super.saveToItem(stack);
        stack.set(ModInit.AUTO_TRADER_VILLAGER_DATA_COMPONENT.get(), this.villagerUUID);
    }

    @Override
    public void loadFromItem(Player player, ItemStack stack) {
        super.loadFromItem(player, stack);
        UUID villagerUUID = stack.get(ModInit.AUTO_TRADER_VILLAGER_DATA_COMPONENT.get());
        this.villagerUUID = villagerUUID;
        this.setChanged();
        this.markDirtyClient();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AutoTraderMenu(containerId, playerInventory, this);
    }

	@Override
	public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.getBlockPos());
        
        buffer.writeBoolean(this.villager !=null);
        if(this.villager !=null){
            buffer.writeInt(this.villager.getId());
            buffer.writeNullable(this.villager.getOffers(), MerchantOffers.STREAM_CODEC.mapStream(stream -> (RegistryFriendlyByteBuf) stream));
        }
        buffer.writeNullable(this.getSelectedOffer(), MerchantOffer.STREAM_CODEC.mapStream(stream -> (RegistryFriendlyByteBuf) stream));
	}

    @Override
    public Component getDisplayName() {
        return Component.translatable("wamutils.container.auto_trader.title");
    }

    @Override
    public void handleCustomMessage(String messageType, ValueInput valueInput, boolean isClient) {
        if(messageType.equalsIgnoreCase("SelectOffer")){
            int index = valueInput.getIntOr("index", -1);
            if(this.villager == null) return;
            MerchantOffers villagerOffers = this.villager.getOffers();
            if(villagerOffers !=null && index >= 0 && index < villagerOffers.size()){
                this.selectedOffer = villagerOffers.get(index);
                this.setChanged();
                this.markDirtyClient();
                return;
            }
            else {
                this.selectedOffer = null;
                this.setChanged();
                this.markDirtyClient();
                return;
            }
        }
        
        super.handleCustomMessage(messageType, valueInput, isClient);
    }
    
}
