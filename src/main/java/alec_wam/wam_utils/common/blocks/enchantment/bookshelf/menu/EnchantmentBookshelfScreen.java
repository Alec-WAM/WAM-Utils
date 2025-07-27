package alec_wam.wam_utils.common.blocks.enchantment.bookshelf.menu;

import alec_wam.wam_utils.WAMUtils;
import alec_wam.wam_utils.common.blocks.enchantment.bookshelf.EnchantmentBookshelfBE;
import alec_wam.wam_utils.network.BaseBEMessagePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class EnchantmentBookshelfScreen extends AbstractContainerScreen<EnchantmentBookshelfMenu> {
    
    public static final ResourceLocation ENCHANTMENT_BOOKSHELF_INVENTORY_TEXTURE = ResourceLocation.fromNamespaceAndPath(WAMUtils.MODID, "textures/gui/enchantment_bookshelf.png");

    private final EnchantmentBookshelfBE blockEntity;
    private EnchantmentFilterButton enchantmentFilterButton;

    public EnchantmentBookshelfScreen(EnchantmentBookshelfMenu menu, Inventory playerInventory, Component title) {
        super(
            menu,
            playerInventory,
            Component.translatable("wamutils.container.enchantment_bookshelf.title")
        );
        this.blockEntity = menu.blockEntity;
        this.titleLabelX = 21;
        this.titleLabelY = 8;
        this.inventoryLabelX = 12;
        this.inventoryLabelY = 82;
        this.imageWidth = 186;
        this.imageHeight = 175;
    }

    @Override
    public void init(){
        super.init();
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        this.enchantmentFilterButton = new EnchantmentFilterButton(
            i + 166, j + 5, 16, 16, 
            this.blockEntity.getFilter(),
            press -> this.onEnchantmentFilterButtonPress()
        );
        this.addRenderableWidget(this.enchantmentFilterButton);
    }

    public void onEnchantmentFilterButtonPress(){
        this.blockEntity.setFilter(this.enchantmentFilterButton.getFilter());
        CompoundTag tag = new CompoundTag();
        tag.putInt("filter", this.blockEntity.getFilter().ordinal());
        BaseBEMessagePayload message = new BaseBEMessagePayload(
            this.blockEntity.getBlockPos(),
            "set_filter",
            tag
        );
        ClientPacketDistributor.sendToServer(message);
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
        p_281500_.blit(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_BOOKSHELF_INVENTORY_TEXTURE, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }

}
