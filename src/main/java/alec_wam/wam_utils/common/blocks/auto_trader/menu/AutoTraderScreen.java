package alec_wam.wam_utils.common.blocks.auto_trader.menu;

import alec_wam.wam_utils.WAMUtils;
import alec_wam.wam_utils.common.helpers.EntityHelper;
import alec_wam.wam_utils.network.BaseBEMessagePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class AutoTraderScreen extends AbstractContainerScreen<AutoTraderMenu> {
    
    public static final ResourceLocation AUTO_TRADER_TEXTURE = ResourceLocation.fromNamespaceAndPath(WAMUtils.MODID, "textures/gui/auto_trader.png");
    private static final ResourceLocation TRADE_ARROW_OUT_OF_STOCK_SPRITE = ResourceLocation.withDefaultNamespace("container/villager/trade_arrow_out_of_stock");
    private static final ResourceLocation TRADE_ARROW_SPRITE = ResourceLocation.withDefaultNamespace("container/villager/trade_arrow");
    private static final ResourceLocation DISCOUNT_STRIKETHRUOGH_SPRITE = ResourceLocation.withDefaultNamespace("container/villager/discount_strikethrough");
    
    /**
     * The old x position of the mouse pointer
     */
    private float xMouse;
    /**
     * The old y position of the mouse pointer
     */
    private float yMouse;

    private final AbstractVillager villagerEntity;
    private int currentOfferIndex;
    private Button prevOfferButton;
    private Button nextOfferButton;

    public AutoTraderScreen(AutoTraderMenu menu, Inventory playerInventory, Component title) {
        super(
            menu,
            playerInventory,
            Component.translatable("wamutils.container.auto_trader.title")
        );
        this.villagerEntity = menu.villagerEntity;
        this.titleLabelX = 70;
        this.titleLabelY = 10;
        this.inventoryLabelY = 104;
        this.imageWidth = 176;
        this.imageHeight = 198;
    }

    @Override
    public void init(){
        super.init();

        int topX = (this.width - this.imageWidth) / 2;
        int topY = (this.height - this.imageHeight) / 2;

        this.currentOfferIndex = EntityHelper.getOfferIndex(this.menu.offers, this.menu.selectedOffer);
        if(this.currentOfferIndex == -1) this.currentOfferIndex = 0;
        this.prevOfferButton = this.addRenderableWidget(Button.builder(
            Component.literal("<"), 
            pressedButton -> {
                final int oldIndex = this.currentOfferIndex;
                this.currentOfferIndex = Math.max(this.currentOfferIndex - 1, 0);
                this.menu.selectedOffer = this.menu.offers.get(this.currentOfferIndex);
                if(oldIndex != this.currentOfferIndex){
                    this.syncOfferSelected();                             
                }
            }
        ).bounds(topX + 60, topY +32, 10, 20).build());
        this.nextOfferButton = this.addRenderableWidget(Button.builder(
            Component.literal(">"), 
            pressedButton -> {
                final int oldIndex = this.currentOfferIndex;
                final int size = this.menu.offers !=null ? this.menu.offers.size() - 1 : 0;
                this.currentOfferIndex = Math.min(this.currentOfferIndex + 1, size);
                this.menu.selectedOffer = this.menu.offers.get(this.currentOfferIndex);
                if(oldIndex != this.currentOfferIndex){
                    this.syncOfferSelected();                    
                }
            }
        ).bounds(topX + 162, topY + 32, 10, 20).build());
    }


    public void syncOfferSelected() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("index", this.currentOfferIndex);
        BaseBEMessagePayload message = new BaseBEMessagePayload(
            this.menu.blockEntity.getBlockPos(),
            "SelectOffer",
            tag
        );
        ClientPacketDistributor.sendToServer(message);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);        
        this.xMouse = mouseX;
        this.yMouse = mouseY;

        int topX = (this.width - this.imageWidth) / 2;
        int topY = (this.height - this.imageHeight) / 2;

        MerchantOffers merchantoffers = this.menu.offers;
        if (merchantoffers !=null && !merchantoffers.isEmpty() && this.currentOfferIndex >= 0) {
            MerchantOffer merchantoffer = merchantoffers.get(this.currentOfferIndex);
            if(merchantoffer !=null){
                int offerX = topX + 72 + 5;
                int offerY = topY + 27 + 4;
                
                ItemStack itemstack = merchantoffer.getBaseCostA();
                ItemStack itemstack1 = merchantoffer.getCostA();
                ItemStack itemstack2 = merchantoffer.getCostB();
                ItemStack itemstack3 = merchantoffer.getResult();
                
                int offerYMiddle = offerY + 2;
                this.renderAndDecorateCostA(guiGraphics, itemstack1, itemstack, offerX, offerYMiddle);
                if (!itemstack2.isEmpty()) {
                    guiGraphics.renderFakeItem(itemstack2, offerX + 18, offerYMiddle);
                    guiGraphics.renderItemDecorations(this.font, itemstack2, offerX + 18, offerYMiddle);
                }

                this.renderButtonArrows(guiGraphics, merchantoffer, offerX + 42, offerYMiddle);
                guiGraphics.renderFakeItem(itemstack3, offerX + 58, offerYMiddle);
                guiGraphics.renderItemDecorations(this.font, itemstack3, offerX + 58, offerYMiddle);
            
                if(mouseX >= offerX && mouseX <= offerX + 20 && mouseY >= offerY && mouseY <= offerY + 20){
                    guiGraphics.setTooltipForNextFrame(this.font, itemstack1, mouseX, mouseY);
                }
                else if(!itemstack2.isEmpty() && mouseX >= offerX + 18 && mouseX <= offerX + 18 + 20 && mouseY >= offerY && mouseY <= offerY + 20){
                    guiGraphics.setTooltipForNextFrame(this.font, itemstack2, mouseX, mouseY);
                }
                else if(!itemstack3.isEmpty() && mouseX >= offerX + 58 && mouseX <= offerX + 58 + 20 && mouseY >= offerY && mouseY <= offerY + 20){
                    guiGraphics.setTooltipForNextFrame(this.font, itemstack3, mouseX, mouseY);
                }            
            }
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics p_281500_, float p_281299_, int p_283481_, int p_281831_) {
        int i = this.leftPos;
        int j = this.topPos;
        p_281500_.blit(RenderPipelines.GUI_TEXTURED, AUTO_TRADER_TEXTURE, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        if(this.villagerEntity !=null){
            InventoryScreen.renderEntityInInventoryFollowsMouse(p_281500_, i + 8, j + 8, i + 58, j + 78, 30, 0.0625F, this.xMouse, this.yMouse, this.villagerEntity);
        }
    }

    private void renderButtonArrows(GuiGraphics guiGraphics, MerchantOffer merchantOffers, int posX, int posY) {
        if (merchantOffers.isOutOfStock()) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, TRADE_ARROW_OUT_OF_STOCK_SPRITE, posX, posY + 3, 10, 9);
        } else {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, TRADE_ARROW_SPRITE, posX, posY + 3, 10, 9);
        }
    }

    private void renderAndDecorateCostA(GuiGraphics guiGraphics, ItemStack realCost, ItemStack baseCost, int x, int y) {
        guiGraphics.renderFakeItem(realCost, x, y);
        if (baseCost.getCount() == realCost.getCount()) {
            guiGraphics.renderItemDecorations(this.font, realCost, x, y);
        } else {
            guiGraphics.renderItemDecorations(this.font, baseCost, x, y, baseCost.getCount() == 1 ? "1" : null);
            // Neo: Fixes MCForge#8806: forced item decorations (e.g., ItemStack#isBarVisible() returns true) were rendered for discount count text
            // Code for count rendering taken from GuiGraphics#renderItemCount
            String count = realCost.getCount() == 1 ? "1" : String.valueOf(realCost.getCount());
            guiGraphics.drawString(font, count, x + 14 + 19 - 2 - font.width(count), y + 6 + 3, 0xFFFFFFFF, true);

            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, DISCOUNT_STRIKETHRUOGH_SPRITE, x + 7, y + 12, 9, 2);
        }
    }

}
