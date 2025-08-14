package alec_wam.wam_utils.common.blocks.conveyor.extractor.menu;

import java.util.Optional;

import javax.annotation.Nullable;

import alec_wam.wam_utils.WAMUtils;
import alec_wam.wam_utils.client.screen.GuiIconButton;
import alec_wam.wam_utils.client.util.GuiIcons;
import alec_wam.wam_utils.common.blocks.conveyor.ItemFilter;
import alec_wam.wam_utils.common.blocks.conveyor.ItemFilter.FilterType;
import alec_wam.wam_utils.common.blocks.conveyor.ItemFilterList;
import alec_wam.wam_utils.common.blocks.conveyor.extractor.ItemExtractorBE;
import alec_wam.wam_utils.network.BaseBEMessagePayload;
import alec_wam.wam_utils.network.FilterSlotPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class ItemExtractorScreen extends AbstractContainerScreen<ItemExtractorMenu> {
    
    public static final ResourceLocation INVENTORY_TEXTURE = ResourceLocation.fromNamespaceAndPath(WAMUtils.MODID, "textures/gui/item_extractor.png");
    public static final ResourceLocation OVERLAY_TEXTURE = ResourceLocation.fromNamespaceAndPath(WAMUtils.MODID, "textures/gui/overlay.png");
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

    public int OVERLAY_WIDTH = 182;
    public int OVERLAY_HEIGHT = 114;
    public int OVERLAY_X = 8;
    public int OVERLAY_Y = 8 + (120 / 2) - (OVERLAY_HEIGHT / 2);
    
    private final ItemFilterButton[] filterButtons = new ItemFilterButton[LIST_SIZE];
    private GuiIconButton addFilterButton;
    private GuiIconButton editFilterButton;
    private GuiIconButton deleteFilterButton;

    private int scrollOffset;
    private boolean isDragging;
    private int selectedFilterIndex = -1;
    private boolean overlayOpen = false;
    private int editFilterIndex = -1;
    private ItemFilter editFilter = null;
    private GuiIconButton filterWhitelistButton;
    private CycleButton<FilterType> filterModeButton;
    private EditBox filterTagBox;
    private Button filterCancelButton;
    private Button filterSaveButton;

    private ItemExtractorBE blockEntity;

    public ItemExtractorScreen(ItemExtractorMenu menu, Inventory playerInventory, Component title) {
        super(
            menu,
            playerInventory,
            Component.translatable("wamutils.container.item_extractor.title")
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
            this.filterButtons[l] = this.addRenderableWidget(new ItemFilterButton(i + LIST_X, k, l, p_99174_ -> {
                if (p_99174_ instanceof ItemFilterButton bookButton) {
                    this.changeSelectedFilter(bookButton.index + this.scrollOffset);
                }
            }));
            k += LIST_ITEM_HEIGHT;
        }
        final int buttonSize = 16;
        this.addFilterButton = this.addRenderableWidget(new GuiIconButton(i + 168, j + 30, buttonSize, buttonSize, GuiIcons.ICON_ADD, (button) -> this.onAddFilterClicked(), null));
        this.editFilterButton = this.addRenderableWidget(new GuiIconButton(i + 168, j + 55, buttonSize, buttonSize, GuiIcons.ICON_EDIT, (button) -> this.editFilterButtonClicked(), null));
        this.editFilterButton.active = false;
        this.deleteFilterButton = this.addRenderableWidget(new GuiIconButton(i + 168, j + 80, buttonSize, buttonSize, GuiIcons.ICON_DELETE, (button) -> this.deleteFilterButtonClicked(), null));
        this.deleteFilterButton.active = false;

        int filterButtonWidth = 100;
        int filterButtonX = i + OVERLAY_X + (OVERLAY_WIDTH / 2) - (filterButtonWidth / 2);
        int filterButtonY = j + OVERLAY_Y  + 8;
        this.filterModeButton = CycleButton.builder(
                (FilterType value) -> Component.translatable("wamutils.gui.item_filter.mode." + value.name().toLowerCase())
            )
            .withValues(FilterType.values())
            .create(
                filterButtonX,
                filterButtonY,
                filterButtonWidth,
                20,
                Component.translatable("wamutils.gui.item_filter.type"),
                (cycleButton, filterType) -> ItemExtractorScreen.this.setFilterMode(filterType)
            );
        this.filterModeButton = this.addRenderableWidget(
            this.filterModeButton
        );
        this.filterModeButton.visible = false;


        int filterTagBoxX = i + OVERLAY_X + 40;
        int filterTagBoxY = j + OVERLAY_Y + (OVERLAY_HEIGHT / 2) - (20 / 2);
        this.filterTagBox = new EditBox(this.font, filterTagBoxX, filterTagBoxY, 100, 20, Component.translatable("wamutils.gui.item_filter.tag"));
        this.filterTagBox.setMaxLength(250);
        this.filterTagBox.setResponder(this::itemTagBoxChanged);
        this.filterTagBox.setCanLoseFocus(false);
        this.addRenderableWidget(this.filterTagBox);
        this.filterTagBox.visible = false;


        int filterWhitelistButtonX = i + OVERLAY_X + OVERLAY_WIDTH - 30;
        int filterWhitelistButtonY = j + OVERLAY_Y + 8;
        this.filterWhitelistButton = this.addRenderableWidget(new GuiIconButton(filterWhitelistButtonX, filterWhitelistButtonY, buttonSize, buttonSize, GuiIcons.ICON_WHITELIST, (button) -> ItemExtractorScreen.this.toggleWhitelist(), null) {
            @Override
            public GuiIcons getIcon() {
                ItemFilter selectedFilter = ItemExtractorScreen.this.getEditFilter();
                if (selectedFilter != null && !selectedFilter.isWhiteList()) {
                    return GuiIcons.ICON_BLACKLIST;
                }
                return GuiIcons.ICON_WHITELIST;
            }
        });
        this.filterWhitelistButton.visible = false;

        int bottomButtonsMiddle = i + OVERLAY_X + (OVERLAY_WIDTH / 2);
        int bottomButtonsY = j + OVERLAY_Y + OVERLAY_HEIGHT - 28;

        int filterCancelButtonX = bottomButtonsMiddle - 55;
        int filterCancelButtonY = bottomButtonsY;
        int cancelButtonWidth = 50;
        this.filterCancelButton = this.addRenderableWidget(
            Button.builder(
                Component.translatable("wamutils.gui.cancel"), 
                (button) -> this.closeOverlay()
            )
            .bounds(filterCancelButtonX, filterCancelButtonY, cancelButtonWidth, 20)
            .build()
        );
        this.filterCancelButton.visible = false;

        int filterSaveButtonX = bottomButtonsMiddle + 5;
        int filterSaveButtonY = bottomButtonsY;
        int cancelSaveWidth = 50;
        this.filterSaveButton = this.addRenderableWidget(
            Button.builder(
                Component.translatable("wamutils.gui.save"), 
                (button) -> this.saveFilter()
            )
            .bounds(filterSaveButtonX, filterSaveButtonY, cancelSaveWidth, 20)
            .build()
        );
        this.filterSaveButton.visible = false;
    }

    public void onAddFilterClicked() {
        ItemFilter filter = ItemFilter.empty();
        this.getFilterList().add(filter);
        this.sendAddFilterPacket(filter);
        this.selectedFilterIndex = -1;
        //TODO Scroll and auto select filter
    }

    public void sendAddFilterPacket(ItemFilter filter) {
        CompoundTag tag = new CompoundTag();
        tag.put("Filter", ItemFilter.CODEC.encodeStart(NbtOps.INSTANCE, filter).result().get());
        BaseBEMessagePayload message = new BaseBEMessagePayload(
            this.menu.blockEntity.getBlockPos(),
            "AddFilter",
            tag
        );
        ClientPacketDistributor.sendToServer(message);
    }

    public void editFilterButtonClicked() {
        this.openOverlay(this.selectedFilterIndex);
    }

    public void openOverlay(int index) {
        this.overlayOpen = true;
        this.editFilterIndex = index;
        this.editFilter = this.getFilterList().get(index).copy();
        if(this.getEditFilter() != null){
            this.filterModeButton.setValue(this.getEditFilter().getType());
            this.filterTagBox.setValue(this.getEditFilter().getItemTag().orElse(""));
            ItemStack currentStack = this.getEditFilter().getStack().orElse(ItemStack.EMPTY);            
            this.menu.fakeItemSlotEnabled = this.getEditFilter().getType() == FilterType.ITEM;
            this.menu.fakeSlotContainer.setItem(0, currentStack);
            ClientPacketDistributor.sendToServer(new FilterSlotPayload(0, currentStack, currentStack.getCount()));
        }
    }

    public ItemFilter getEditFilter() {
        return this.editFilter;
    }

    public ItemFilterList getFilterList() {
        return this.blockEntity.getFilterList();
    }

    public void closeOverlay() {
        this.overlayOpen = false;
        this.menu.fakeItemSlotEnabled = false;
        this.editFilter = null;
        this.editFilterIndex = -1;
    }
    
    public void toggleWhitelist(){
        ItemFilter selectedFilter = this.getEditFilter();
        if(selectedFilter != null){
            final boolean oldWhitelist = selectedFilter.isWhiteList();
            selectedFilter.setWhiteList(!oldWhitelist);
        }
    }

    public void setFilterMode(FilterType type){
        ItemFilter selectedFilter = this.getEditFilter();
        if(selectedFilter != null){
            selectedFilter.setType(type);
            this.menu.fakeItemSlotEnabled = selectedFilter.getType() == FilterType.ITEM;
        }
    }

    public void itemTagBoxChanged(String tag) {
        ItemFilter selectedFilter = this.getEditFilter();
        if(selectedFilter != null){
            selectedFilter.setItemTag(tag.isEmpty() ? Optional.empty() : Optional.of(tag));
        }
    }

    public void saveFilter(){
        if(this.getEditFilter() != null && this.editFilterIndex >= 0 && this.editFilterIndex < this.getFilterList().size()){
            this.getFilterList().set(this.editFilterIndex, this.editFilter);
        }
        this.sendSavePacket();
        this.closeOverlay();
    }    

    public void sendSavePacket() {
        if(this.getEditFilter() != null && this.editFilterIndex >= 0 && this.editFilterIndex < this.getFilterList().size()){
            ItemFilter selectedFilter = this.getEditFilter();
            CompoundTag tag = new CompoundTag();
            tag.putInt("index", this.editFilterIndex);
            tag.put("Filter", ItemFilter.CODEC.encodeStart(NbtOps.INSTANCE, selectedFilter).result().get());
            BaseBEMessagePayload message = new BaseBEMessagePayload(
                this.menu.blockEntity.getBlockPos(),
                "UpdateFilter",
                tag
            );
            ClientPacketDistributor.sendToServer(message);
        }
    }

    public void deleteFilterButtonClicked() {
        if(this.selectedFilterIndex >= 0 && this.selectedFilterIndex < this.getFilterList().size()){
            this.sendDeletePacket();   
            this.getFilterList().remove(this.selectedFilterIndex);
        }
        this.selectedFilterIndex = -1;
    }

    public void sendDeletePacket() {
        if(this.selectedFilterIndex >= 0 && this.selectedFilterIndex < this.getFilterList().size()){
            CompoundTag tag = new CompoundTag();
            tag.putInt("index", this.selectedFilterIndex);
            BaseBEMessagePayload message = new BaseBEMessagePayload(
                this.menu.blockEntity.getBlockPos(),
                "RemoveFilter",
                tag
            );
            ClientPacketDistributor.sendToServer(message);
        }
    }

    @Nullable
    public ItemFilter getSelectedFilterIndex() {
        if(this.selectedFilterIndex >= 0 && this.selectedFilterIndex < this.getFilterList().size()){
            return this.getFilterList().get(this.selectedFilterIndex);
        }
        return null;
    }

    public void changeSelectedFilter(int index){
        if(index == this.selectedFilterIndex && index >= 0) {
            //Open edit on double click
            this.editFilterButtonClicked();
            return;
        }
        this.selectedFilterIndex = index;
        this.editFilterButton.active = index >= 0;
        this.deleteFilterButton.active = index >= 0;
        //TODO Show overlay
        // CompoundTag tag = new CompoundTag();
        // ClientShelfItem shelfItem = this.menu.getFilteredItemList().get(index);
        // tag.store("uuid", UUIDUtil.CODEC, shelfItem.uuid());
        // BaseBEMessagePayload message = new BaseBEMessagePayload(
        //     this.blockEntity.getBlockPos(),
        //     "extract_enchantment",
        //     tag
        // );
        // ClientPacketDistributor.sendToServer(message);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Don't render the labels
        // super.renderLabels(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int p_98876_, int p_98877_, float p_98878_) {
        super.render(guiGraphics, p_98876_, p_98877_, p_98878_);
        
        ItemFilterList itemFilterList = this.getFilterList();
        boolean emptyList = itemFilterList.isEmpty();
        
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        
        if(!emptyList && !this.overlayOpen) {
            int listX = i + LIST_X;
            int listY = j + LIST_Y;
            int l = i + SCROLL_X;
            int k = j + SCROLL_Y;
            this.renderScroller(guiGraphics, l, k);

            int i1 = 0;
            for (ItemFilter itemFilter : itemFilterList) {
                if (!this.canScroll(itemFilterList.size()) || i1 >= this.scrollOffset && i1 < LIST_SIZE + this.scrollOffset) {
                    int j1 = k + 2;
                    
                    if(itemFilter.getStack().isEmpty() && itemFilter.getItemTag().isEmpty()){
                        guiGraphics.drawString(this.font, Component.literal("Empty"), listX + 24, j1 + 4, -16777216, false);
                    }
                    else if(itemFilter.getType() == ItemFilter.FilterType.ITEM){
                        if(itemFilter.getStack().isPresent()){
                            guiGraphics.renderFakeItem(itemFilter.getStack().get(), listX + 5, j1);
                            guiGraphics.drawString(this.font, Component.literal("Exact Match"), listX + 24, j1 + 4, -16777216, false);
                        }                        
                    }
                    else if(itemFilter.getType() == ItemFilter.FilterType.TAG){
                        if(itemFilter.getItemTag().isPresent()){
                            guiGraphics.renderFakeItem(new ItemStack(Items.NAME_TAG), listX + 5, j1);
                            guiGraphics.drawString(this.font, Component.literal(itemFilter.getItemTag().get()), listX + 24, j1 + 4, -16777216, false);
                        }                        
                    }
                    
                    // guiGraphics.drawString(this.font, Component.literal("Test"), listX + 24, j1 + 4, -16777216, false);
                    
                    GuiIcons filterIcon = itemFilter.isWhiteList() ? GuiIcons.ICON_WHITELIST : GuiIcons.ICON_BLACKLIST;
                    filterIcon.renderIcon(guiGraphics, listX + LIST_WIDTH - 21, j1, 16, 16, ARGB.color(1.0F, -1));

                    k += LIST_ITEM_HEIGHT;
                    i1++;
                } else {
                    i1++;
                }
            }
        }

        for (ItemFilterButton filterButton : this.filterButtons) {
            if(!emptyList && !this.overlayOpen) {
                if (filterButton.isHoveredOrFocused()) {
                    filterButton.renderToolTip(guiGraphics, p_98876_, p_98877_);
                }
            }

            filterButton.visible = emptyList || this.overlayOpen ? false : filterButton.index < itemFilterList.size();
            filterButton.active = true;
        }

        this.addFilterButton.visible = this.editFilterButton.visible = this.deleteFilterButton.visible = !this.overlayOpen;
        this.filterModeButton.visible = this.filterWhitelistButton.visible = this.overlayOpen;
        this.filterCancelButton.visible = this.filterSaveButton.visible = this.overlayOpen;
        this.filterTagBox.visible = this.overlayOpen && this.getEditFilter() != null && this.getEditFilter().getType() == ItemFilter.FilterType.TAG;

        this.renderTooltip(guiGraphics, p_98876_, p_98877_);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        
        //TODO Fix "E" button not closing overlay
        if(!this.filterTagBox.keyPressed(keyCode, scanCode, modifiers) && !this.filterTagBox.canConsumeInput()){
            //Close Overlay on ESC
            if (keyCode == 256 && this.overlayOpen) {
                this.closeOverlay();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return true;
    }

    private boolean canScroll(int numOffers) {
        return numOffers > LIST_SIZE;
    }

    @Override
    public boolean mouseScrolled(double p_99127_, double p_99128_, double p_99129_, double p_295610_) {
        if (super.mouseScrolled(p_99127_, p_99128_, p_99129_, p_295610_)) {
            return true;
        } else {
            int i = this.getFilterList().size();
            if (this.canScroll(i)) {
                int j = i - LIST_SIZE;
                this.scrollOffset = Mth.clamp((int)(this.scrollOffset - p_295610_), 0, j);
            }

            return true;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int i = this.getFilterList().size();
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
        
        if (hoveredSlot !=null && hoveredSlot.index == 0 && this.overlayOpen){
            ItemStack stack = this.menu.getCarried();// getMinecraft().player.inventoryMenu.getCarried();
            stack = stack.copy().split(hoveredSlot.getMaxStackSize()); // Limit to slot limit
            if (ItemStack.isSameItemSameComponents(stack, hoveredSlot.getItem())) return true;
            if(this.getEditFilter() != null) {
                this.getEditFilter().setStack(stack.isEmpty() ? Optional.empty() : Optional.of(stack));
            }
            hoveredSlot.setByPlayer(stack); // Temporarily update the client for continuity purposes            
            ClientPacketDistributor.sendToServer(new FilterSlotPayload(hoveredSlot.index, stack, stack.getCount()));
            return true;
        }
        
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        if (this.canScroll(this.getFilterList().size()) && mouseX > i + SCROLL_X && mouseX < i + SCROLL_X + SCROLL_WIDTH && mouseY > j + SCROLL_Y && mouseY <= j + SCROLL_Y + SCROLL_HEIGHT + 1) {
            this.isDragging = true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics p_281500_, float p_281299_, int p_283481_, int p_281831_) {
        int i = this.leftPos;
        int j = this.topPos;
        p_281500_.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_TEXTURE, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    
        

        if(this.overlayOpen){
            p_281500_.blit(RenderPipelines.GUI_TEXTURED, OVERLAY_TEXTURE, i + OVERLAY_X, j + OVERLAY_Y, 0.0F, 0.0F, OVERLAY_WIDTH, OVERLAY_HEIGHT, 248, 166, 256, 256);
            
            //Render Fake Slot
            if(this.getEditFilter() != null && this.getEditFilter().getType() == FilterType.ITEM){                
                int fakeSlotX = i + OVERLAY_X + 60;
                int fakeSlotY = j + OVERLAY_Y + (OVERLAY_HEIGHT / 2) - (18 / 2);
                p_281500_.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_TEXTURE, fakeSlotX, fakeSlotY, 18.0F, 126.0F, 18, 18, 256, 256);
            }        
        }
    }

    private void renderScroller(GuiGraphics guiGraphics, int posX, int posY) {
        int i = this.getFilterList().size() + 1 - LIST_SIZE;
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

    class ItemFilterButton extends Button {
        final int index;

        public ItemFilterButton(int x, int y, int index, Button.OnPress onPress) {
            super(x, y, LIST_WIDTH, LIST_ITEM_HEIGHT, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
            this.index = index;
            this.visible = false;
        }

        public int getIndex() {
            return this.index;
        }

        public void renderToolTip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            ItemFilterList itemFilterList = ItemExtractorScreen.this.getFilterList();
            if (this.isHovered && itemFilterList.size() > this.index + ItemExtractorScreen.this.scrollOffset) {
                ItemFilter itemFilter = itemFilterList.get(this.index + ItemExtractorScreen.this.scrollOffset);
                if (mouseX < this.getX() + 20) {
                    if(itemFilter.getType() == ItemFilter.FilterType.ITEM && itemFilter.getStack().isPresent()){
                        guiGraphics.setTooltipForNextFrame(ItemExtractorScreen.this.font, itemFilter.getStack().get(), mouseX, mouseY);
                    }
                    if(itemFilter.getType() == ItemFilter.FilterType.TAG && itemFilter.getItemTag().isPresent()){
                        // guiGraphics.setTooltipForNextFrame(font, elements, mouseX, mouseY);
                        // guiGraphics.setTooltipForNextFrame(ItemExtractorScreen.this.font, itemFilter.getStack().get(), mouseX, mouseY);
                    }
                } 
            }
        }
    }
    
}
