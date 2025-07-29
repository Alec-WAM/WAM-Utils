package alec_wam.wam_utils.common.blocks.enchantment.indexer.menu;

import java.util.List;

import alec_wam.wam_utils.WAMUtils;
import alec_wam.wam_utils.common.blocks.enchantment.indexer.EnchantmentIndexerBE;
import alec_wam.wam_utils.common.blocks.enchantment.indexer.EnchantmentIndexerBE.ClientShelfItem;
import alec_wam.wam_utils.network.BaseBEMessagePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class EnchantmentIndexerScreen extends AbstractContainerScreen<EnchantmentIndexerMenu> {
    
    public static final ResourceLocation ENCHANTMENT_INDEXER_INVENTORY_TEXTURE = ResourceLocation.fromNamespaceAndPath(WAMUtils.MODID, "textures/gui/enchantment_indexer.png");
    // TODO Create our own sprites
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("container/villager/scroller");
    private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/villager/scroller_disabled");

    public static final int LIST_SIZE = 5;
    public static final int LIST_X = 11;
    public static final int LIST_Y = 19;
    public static final int LIST_WIDTH = 142;
    public static final int LIST_HEIGHT = 100;
    public static final int LIST_ITEM_HEIGHT = 20;
    public static final int SCROLL_X = 154;
    public static final int SCROLL_Y = 19;
    public static final int SCROLL_WIDTH = 6;
    public static final int SCROLL_HEIGHT = 100;

    private final EnchantmentIndexerBE blockEntity;

    private final EnchantmentBookButton[] bookButtons = new EnchantmentBookButton[LIST_SIZE];
    private int scrollOffset;
    private boolean isDragging;

    public EnchantmentIndexerScreen(EnchantmentIndexerMenu menu, Inventory playerInventory, Component title) {
        super(
            menu,
            playerInventory,
            Component.translatable("wamutils.container.enchantment_indexer.title")
        );
        this.blockEntity = menu.blockEntity;
        this.titleLabelX = 21;
        this.titleLabelY = 8;
        this.inventoryLabelX = 12;
        this.inventoryLabelY = 82;
        this.imageWidth = 198;
        this.imageHeight = 209;
    }

    @Override
    public void init() {
        super.init();
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        int k = j + LIST_Y;

        for (int l = 0; l < LIST_SIZE; l++) {
            this.bookButtons[l] = this.addRenderableWidget(new EnchantmentBookButton(i + LIST_X, k, l, p_99174_ -> {
                if (p_99174_ instanceof EnchantmentBookButton bookButton) {
                    this.onBookClicked(bookButton.index + this.scrollOffset);
                }
            }));
            k += LIST_ITEM_HEIGHT;
        }
    }

    public void onBookClicked(int index){
        CompoundTag tag = new CompoundTag();
        ClientShelfItem shelfItem = this.menu.getFilteredItemList().get(index);
        tag.store("uuid", UUIDUtil.CODEC, shelfItem.uuid());
        BaseBEMessagePayload message = new BaseBEMessagePayload(
            this.blockEntity.getBlockPos(),
            "extract_enchantment",
            tag
        );
        ClientPacketDistributor.sendToServer(message);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render the labels
        // super.renderLabels(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int p_98876_, int p_98877_, float p_98878_) {
        super.render(guiGraphics, p_98876_, p_98877_, p_98878_);
        
        List<ClientShelfItem> filteredItemList = this.menu.getFilteredItemList();
        boolean emptyList = filteredItemList.isEmpty();
        
        if(!emptyList) {
            int i = (this.width - this.imageWidth) / 2;
            int j = (this.height - this.imageHeight) / 2;
            int listX = i + LIST_X;
            int listY = j + LIST_Y;
            int l = i + SCROLL_X;
            int k = j + SCROLL_Y;
            this.renderScroller(guiGraphics, l, k);

            int i1 = 0;
            for (ClientShelfItem shelfItem : filteredItemList) {
                if (!this.canScroll(filteredItemList.size()) || i1 >= this.scrollOffset && i1 < LIST_SIZE + this.scrollOffset) {
                    ItemStack bookItem = shelfItem.stack();
                    int j1 = k + 2;
                    guiGraphics.renderFakeItem(bookItem, listX + 5, j1);
                    guiGraphics.renderItemDecorations(this.font, bookItem, listX + 5, j1);
                    
                    MutableComponent enchantments = Component.empty();
                    int index = 0;
                    for(Holder<Enchantment> enchantment : shelfItem.enchantments().keySet()) {
                        if(index > 0 && index < shelfItem.enchantments().keySet().size() - 1) {
                            enchantments = enchantments.append(Component.literal(", "));
                        }
                        enchantments = enchantments.append(Enchantment.getFullname(enchantment, shelfItem.enchantments().getLevel(enchantment)));
                    }
                    enchantments = enchantments.withColor(-16777216);
                    guiGraphics.drawString(this.font, enchantments, listX + 24, j1 + 4, -16777216, false);
                    
                    k += LIST_ITEM_HEIGHT;
                    i1++;
                } else {
                    i1++;
                }
            }
        }

        for (EnchantmentBookButton enchantmentButton : this.bookButtons) {
            if(!emptyList) {
                if (enchantmentButton.isHoveredOrFocused()) {
                    enchantmentButton.renderToolTip(guiGraphics, p_98876_, p_98877_);
                }
            }

            enchantmentButton.visible = emptyList ? false : enchantmentButton.index < filteredItemList.size();
            enchantmentButton.active = this.menu.getExtractedItemStack().isEmpty();
        }

        this.renderTooltip(guiGraphics, p_98876_, p_98877_);
    }

    private boolean canScroll(int numOffers) {
        return numOffers > LIST_SIZE;
    }

    @Override
    public boolean mouseScrolled(double p_99127_, double p_99128_, double p_99129_, double p_295610_) {
        if (super.mouseScrolled(p_99127_, p_99128_, p_99129_, p_295610_)) {
            return true;
        } else {
            int i = this.menu.getFilteredItemList().size();
            if (this.canScroll(i)) {
                int j = i - LIST_SIZE;
                this.scrollOffset = Mth.clamp((int)(this.scrollOffset - p_295610_), 0, j);
            }

            return true;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int i = this.menu.getFilteredItemList().size();
        if (this.isDragging) {
            int j = this.topPos + SCROLL_Y;
            int k = j + SCROLL_X;
            int l = i - LIST_SIZE;
            float f = ((float)mouseY - j - 13.5F) / (k - j - 27.0F);
            f = f * l + 0.5F;
            this.scrollOffset = Mth.clamp((int)f, 0, l);
            return true;
        } else {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.isDragging = false;
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        if (this.canScroll(this.menu.getFilteredItemList().size()) && mouseX > i + SCROLL_X && mouseX < i + SCROLL_X + SCROLL_WIDTH && mouseY > j + SCROLL_Y && mouseY <= j + SCROLL_Y + SCROLL_HEIGHT + 1) {
            this.isDragging = true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics p_281500_, float p_281299_, int p_283481_, int p_281831_) {
        int i = this.leftPos;
        int j = this.topPos;
        p_281500_.blit(RenderPipelines.GUI_TEXTURED, ENCHANTMENT_INDEXER_INVENTORY_TEXTURE, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }

    private void renderScroller(GuiGraphics guiGraphics, int posX, int posY) {
        int i = this.menu.getFilteredItemList().size() + 1 - LIST_SIZE;
        final int scrollBarHandleHeight = 15;
        if (i > 1) {
            int j = SCROLL_HEIGHT - (scrollBarHandleHeight + (i - 1) * SCROLL_HEIGHT / i);
            int k = 1 + j / i + SCROLL_HEIGHT / i;
            int i1 = Math.min(SCROLL_HEIGHT, this.scrollOffset * k);
            if (this.scrollOffset == i - 1) {
                i1 = SCROLL_HEIGHT;
            }

            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, posX, posY + i1, SCROLL_WIDTH, scrollBarHandleHeight);
        } else {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_DISABLED_SPRITE, posX, posY, SCROLL_WIDTH, scrollBarHandleHeight);
        }
    }

    class EnchantmentBookButton extends Button {
        final int index;

        public EnchantmentBookButton(int x, int y, int index, Button.OnPress onPress) {
            super(x, y, LIST_WIDTH, LIST_ITEM_HEIGHT, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
            this.index = index;
            this.visible = false;
        }

        public int getIndex() {
            return this.index;
        }

        public void renderToolTip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            List<ClientShelfItem> filteredItemList = EnchantmentIndexerScreen.this.menu.getFilteredItemList();
            if (this.isHovered && filteredItemList.size() > this.index + EnchantmentIndexerScreen.this.scrollOffset) {
                ClientShelfItem shelfItem = filteredItemList.get(this.index + EnchantmentIndexerScreen.this.scrollOffset);
                if (mouseX < this.getX() + 20) {
                    guiGraphics.setTooltipForNextFrame(EnchantmentIndexerScreen.this.font, shelfItem.stack(), mouseX, mouseY);
                } 
            }
        }
    }
    
}
