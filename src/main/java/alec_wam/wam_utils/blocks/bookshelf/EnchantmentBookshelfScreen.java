package alec_wam.wam_utils.blocks.bookshelf;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.bookshelf.EnchantmentBookshelfBE.EnchantmentCategoryBookshelfFilter;
import alec_wam.wam_utils.server.network.MessageBlockEntityUpdate;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class EnchantmentBookshelfScreen extends AbstractContainerScreen<EnchantmentBookshelfContainer> {

    private final ResourceLocation GUI = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/enchantment_bookshelf.png");

    private EnchantmentBookshelfBE bookshelf;
    
    public EnchantmentBookshelfScreen(EnchantmentBookshelfContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        this.imageWidth = 186;
        this.imageHeight = 175;
        
        this.bookshelf = container.blockEntity;
    }
    
    @Override
    public void init() {
    	super.init();
    	int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        
    	EnchantmentFilterButton filterButton = new EnchantmentFilterButton(this, relX + 166, relY + 5, 16, 16, bookshelf.getEnchantmentFilter()){

			@Override
			public void onPress() {
				super.onPress();
				sendFilterUpdate(getFilter());
			}
        	
        };
        this.addRenderableWidget(filterButton);
    }
    
    public void sendFilterUpdate(EnchantmentCategoryBookshelfFilter filter)
	{
    	bookshelf.setEnchantmentFilter(filter);
		CompoundTag message = new CompoundTag();
		message.putInt("Filter", filter.ordinal());
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(bookshelf, message));
	}
    
    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        //drawString(matrixStack, Minecraft.getInstance().font, "Energy: " + menu.getEnergy(), 10, 10, 0xffffff);
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, GUI);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        this.blit(matrixStack, relX, relY, 0, 0, this.imageWidth, this.imageHeight); 
    }
}
