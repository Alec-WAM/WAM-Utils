package alec_wam.wam_utils.blocks.xp_vacuum;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.client.GuiIcons;
import alec_wam.wam_utils.client.GuiUtils;
import alec_wam.wam_utils.client.widgets.GuiIconToggleButton;
import alec_wam.wam_utils.client.widgets.RedstoneModeButton;
import alec_wam.wam_utils.server.network.MessageBlockEntityUpdate;
import alec_wam.wam_utils.utils.XPUtil;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class XPVacuumScreen extends AbstractContainerScreen<XPVacuumContainer> {

    private final ResourceLocation GUI = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/xp_vacuum.png");

    private XPVacuumBE vacuum;
    
    private Button giveOneLevelButton;
    private Button giveFiveLevelsButton;
    private Button giveAllXPButton;
    
    public XPVacuumScreen(XPVacuumContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        
        this.vacuum = container.blockEntity;
    }

    @Override
    public void containerTick() {
    	super.containerTick();
    	
    	if(this.vacuum != null) {
    		giveOneLevelButton.active = vacuum.fluidStorage.getExperienceLevel() >= 1;
    		giveFiveLevelsButton.active = vacuum.fluidStorage.getExperienceLevel() >= 5;
    		giveAllXPButton.active = vacuum.fluidStorage.getExperienceTotal() > 0;
    	}
    	else {
    		giveOneLevelButton.active = false;
    		giveFiveLevelsButton.active = false;
    		giveAllXPButton.active = false;
    	}
    }
    
    @Override
    public void init() {
    	super.init();
    	int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        
        GuiIconToggleButton boundingBoxButton = new GuiIconToggleButton(relX + 138, relY + 6, 16, 16, GuiIcons.BUTTON_BOUNDINGBOX_OFF, GuiIcons.BUTTON_BOUNDINGBOX_ON, vacuum.showBoundingBox()){

			@Override
			public void onPress() {
				super.onPress();
				sendBoundingBoxUpdate(isSelected());
			}
			
			//TODO Add tooltip
        	
        };
        this.addRenderableWidget(boundingBoxButton);
        
        RedstoneModeButton redstoneButton = new RedstoneModeButton(this, relX + 156, relY + 6, 16, 16, vacuum.getRedstoneMode()){

			@Override
			public void onPress() {
				super.onPress();
				sendRedstoneModeUpdate(getRestoneMode());
			}
        	
        };
        this.addRenderableWidget(redstoneButton);
        
        int widthButtons = 24 * 3;
        int buttonX = (this.width - widthButtons) / 2;
        int buttonY = relY + 50;
        
        this.giveOneLevelButton = new Button(buttonX + 2, buttonY, 20, 20, Component.literal("-1"), new Button.OnPress() {
			
			@Override
			public void onPress(Button p_93751_) {
				givePlayerXP(1);
			}
		});
        this.addRenderableWidget(giveOneLevelButton);
        
        this.giveFiveLevelsButton = new Button(buttonX + 26, buttonY, 20, 20, Component.literal("-5"), new Button.OnPress() {
			
			@Override
			public void onPress(Button p_93751_) {
				givePlayerXP(5);
			}
		});
        this.addRenderableWidget(giveFiveLevelsButton);
        
        this.giveAllXPButton = new Button(buttonX + 50, buttonY, 20, 20, Component.literal("All"), new Button.OnPress() {
			
			@Override
			public void onPress(Button p_93751_) {
				givePlayerXP(-1);
			}
		});
        this.addRenderableWidget(giveAllXPButton);
        
        giveOneLevelButton.active = vacuum.fluidStorage.getExperienceLevel() >= 1;
		giveFiveLevelsButton.active = vacuum.fluidStorage.getExperienceLevel() >= 5;
		giveAllXPButton.active = vacuum.fluidStorage.getExperienceTotal() > 0;
    }
    
    public void sendRedstoneModeUpdate(RedstoneMode mode)
	{
    	vacuum.setRedstoneMode(mode);;
		CompoundTag message = new CompoundTag();
		message.putInt("RedstoneMode", mode.ordinal());
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(vacuum, message));
	}
    
    public void sendBoundingBoxUpdate(boolean value)
	{
    	vacuum.setShowBoundingBox(value);
		CompoundTag message = new CompoundTag();
		message.putBoolean("ShowBoundingBox", value);
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(vacuum, message));
	}
    
    public void givePlayerXP(int value)
	{
    	vacuum.givePlayerXP(minecraft.player, value);
		CompoundTag message = new CompoundTag();
		message.putUUID("PlayerUUID", minecraft.player.getUUID());
		message.putInt("Levels", value);
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(vacuum, message));
	}
    
    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
        
        int relY = (this.height - this.imageHeight) / 2;
        
        float scale = 0.8F;
        float progress = vacuum.fluidStorage.getExperience();
        int level = vacuum.fluidStorage.getExperienceLevel();
        
        float xpBarWidth = 183.0F * scale;
        int xpBarX = (this.width - (int)xpBarWidth) / 2;
        renderExperienceBar(matrixStack, xpBarX, relY + 40, xpBarWidth, 5.0F * scale, progress, level);
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
    
    public void renderExperienceBar(PoseStack matrixStack, int x, int y, float width, float height, float xpProgress, int xpLevels) {
        RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
        int i = XPUtil.getXpBarCapacity(xpLevels);
        if (i > 0) {
           int k = (int)(xpProgress * 183.0F);
           GuiUtils.drawScaledTexture(matrixStack, x, y, 0, width, height, 0, 182, 64, 64 + 5, 256.0F, 256.0F);
           if (k > 0) {
              GuiUtils.drawScaledTexture(matrixStack, x, y, 0, xpProgress * width, height, 0, k, 69, 69 + 5, 256.0F, 256.0F);
           }
        }

        if (xpLevels > 0) {
           String s = "" + xpLevels;
           float i1 = x + ((width - this.font.width(s)) / 2);
           int j1 = (int) (y - height - 2);
           this.font.draw(matrixStack, s, (float)(i1 + 1), (float)j1, 0);
           this.font.draw(matrixStack, s, (float)(i1 - 1), (float)j1, 0);
           this.font.draw(matrixStack, s, (float)i1, (float)(j1 + 1), 0);
           this.font.draw(matrixStack, s, (float)i1, (float)(j1 - 1), 0);
           this.font.draw(matrixStack, s, (float)i1, (float)j1, 8453920);
        }
     }
}
