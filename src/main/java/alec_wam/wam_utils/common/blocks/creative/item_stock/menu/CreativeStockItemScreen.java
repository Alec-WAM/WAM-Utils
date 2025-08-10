package alec_wam.wam_utils.common.blocks.creative.item_stock.menu;

import alec_wam.wam_utils.WAMUtils;
import alec_wam.wam_utils.common.blocks.creative.item_stock.CreativeItemStockBE;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CreativeStockItemScreen extends AbstractContainerScreen<CreativeStockItemMenu> {
    
    public static final ResourceLocation INVENTORY_TEXTURE = ResourceLocation.fromNamespaceAndPath(WAMUtils.MODID, "textures/gui/creative_stocker_item.png");

    private final CreativeItemStockBE blockEntity;

    public CreativeStockItemScreen(CreativeStockItemMenu menu, Inventory playerInventory, Component title) {
        super(
            menu,
            playerInventory,
            Component.translatable("wamutils.container.creative_stocker_item.title")
        );
        this.blockEntity = menu.blockEntity;
        this.imageWidth = 176;
        this.imageHeight = 133;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void init(){
        super.init();
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
    }

    @Override
    public void render(GuiGraphics p_283246_, int p_98876_, int p_98877_, float p_98878_) {
        super.render(p_283246_, p_98876_, p_98877_, p_98878_);
        this.renderTooltip(p_283246_, p_98876_, p_98877_);
    }

    @Override
    protected void renderBg(GuiGraphics p_281500_, float p_281299_, int p_283481_, int p_281831_) {
        int i = this.leftPos;
        int j = this.topPos;
        p_281500_.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_TEXTURE, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }

}
