package alec_wam.wam_utils.blocks.machine.auto_breeder;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.client.GuiIcons;
import alec_wam.wam_utils.client.GuiUtils;
import alec_wam.wam_utils.client.widgets.GuiIconToggleButton;
import alec_wam.wam_utils.client.widgets.RedstoneModeButton;
import alec_wam.wam_utils.client.widgets.ScaledImageButton;
import alec_wam.wam_utils.server.network.MessageBlockEntityUpdate;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public class AutoBreederScreen extends AbstractContainerScreen<AutoBreederContainer> {

    private final ResourceLocation GUI = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/auto_breeder.png");
    private AutoBreederBE breeder;
    
    private boolean settingsOpen = false;
    private int settingsWidth;
    private int settingsHeight;
    private int settingsRelX;
    private int settingsRelY;
    
    private Checkbox feedBabiesCheckBox;
    private EditBox maxAnimalsTextBox;
    private Button maxAnimalsApplyButton;
    
    public AutoBreederScreen(AutoBreederContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        
        this.breeder = container.blockEntity;
    }

    @Override
    protected void init() {
    	super.init();
    	int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        int settingsButtonX = relX + 152;
        int settingsButtonY = relY + 6;
        
        this.settingsWidth = 164;
    	this.settingsHeight = 150;        
    	this.settingsRelX = (this.width - settingsWidth) / 2;
        this.settingsRelY = (this.height - settingsHeight) / 2;
        
        GuiIconToggleButton boundingBoxButton = new GuiIconToggleButton(relX + 114, settingsButtonY, 16, 16, GuiIcons.BUTTON_BOUNDINGBOX_OFF, GuiIcons.BUTTON_BOUNDINGBOX_ON, breeder.showBoundingBox()){

			@Override
			public void onPress() {
				super.onPress();
				sendBoundingBoxUpdate(isSelected());
			}
        	
        };
        this.addRenderableWidget(boundingBoxButton);
        
        RedstoneModeButton redstoneButton = new RedstoneModeButton(this, relX + 134, settingsButtonY, 16, 16, breeder.getRedstoneMode()){

			@Override
			public void onPress() {
				super.onPress();
				sendRedstoneModeUpdate(getRestoneMode());
			}
        	
        };
        this.addRenderableWidget(redstoneButton);
        
        ScaledImageButton settingsButton = new ScaledImageButton(settingsButtonX, settingsButtonY, 16, 16, Component.empty(), GuiUtils.WIDGETS, 0.0F, 0.0F, 20.0F, 20.0F, 21.0F){

			@Override
			public void onPress() {
				setSettingsOpen(true);
			}
        	
        };
        this.addRenderableWidget(settingsButton);
        
        int maxAnimalsApplyButtonX = settingsRelX + 10 + 80;
        int maxAnimalsApplyButtonY = settingsRelY + 20;
        this.maxAnimalsApplyButton = new Button(maxAnimalsApplyButtonX, maxAnimalsApplyButtonY, 40, 20, Component.translatable("gui.wam_utils.apply"), new Button.OnPress() {

			@Override
			public void onPress(Button p_93751_) {
				int value = 0;
	        	try {
	        		value = Integer.parseInt(maxAnimalsTextBox.getValue());
	        	} catch(Exception e) {

	        	}
	        	breeder.setMaxAnimals(value);
	        	sendMaxAnimalsPacket(value);
			}
        	
        }) {
        	@Override
        	public void renderButton(PoseStack p_93843_, int p_93844_, int p_93845_, float p_93846_) {
        		p_93843_.pushPose();
        		p_93843_.translate(0, 0, 650.0F);
        		super.renderButton(p_93843_, p_93844_, p_93845_, p_93846_);
        		p_93843_.popPose();
        	}
        };
        this.maxAnimalsApplyButton.visible = this.settingsOpen;
        this.maxAnimalsApplyButton.active = false;
        this.addRenderableWidget(maxAnimalsApplyButton);
        
        int maxAnimalsTextBoxX = settingsRelX + 20;
        int maxAnimalsTextBoxY = settingsRelY + 20;
        
        this.maxAnimalsTextBox = new EditBox(this.font, maxAnimalsTextBoxX, maxAnimalsTextBoxY, 60, 20, Component.translatable("gui.wam_utils.auto_breeder.maxAnimals")) {        	
        	@Override
        	public void renderButton(PoseStack p_93843_, int p_93844_, int p_93845_, float p_93846_) {
        		p_93843_.pushPose();
        		p_93843_.translate(0, 0, 650.0F);
        		super.renderButton(p_93843_, p_93844_, p_93845_, p_93846_);
        		int i = this.isFocused() ? -1 : -6250336;
                GuiUtils.fill(p_93843_, this.x - 1, this.y - 1, this.x + this.width + 1, this.y + this.height + 1, i, 650.0F);
                GuiUtils.fill(p_93843_, this.x, this.y, this.x + this.width, this.y + this.height, -16777216, 650.0F);
        		p_93843_.popPose();
        	}
        };
        //this.maxAnimalsTextBox.setBordered(true);
        this.maxAnimalsTextBox.setValue(""+this.breeder.getMaxAnimals());
        this.maxAnimalsTextBox.setResponder((p_232916_) -> {
        	int value = -1;
        	try {
        		value = Integer.parseInt(p_232916_);
        	} catch(Exception e) {

        	}
        	maxAnimalsApplyButton.active = value != -1 && value != breeder.getMaxAnimals();
        });
        this.maxAnimalsTextBox.setFilter(this::isNumberString);
        this.maxAnimalsTextBox.visible = this.settingsOpen;
        this.addRenderableWidget(this.maxAnimalsTextBox);
        
        int feedBabiesButtonX = settingsRelX + ((settingsWidth - 80)  / 2);
        int feedBabiesButtonY = settingsRelY + 60;
        
        feedBabiesCheckBox = new Checkbox(feedBabiesButtonX, feedBabiesButtonY, 80, 20, Component.translatable("gui.wam_utils.auto_breeder.babies"), breeder.shouldFeedBabies()) {
        	@Override
        	public void onPress() {
        		super.onPress();
        		breeder.setShouldFeedBabies(selected());
        		sendFeedBabiesPacket(selected());
        	}
        	
        	@Override
        	public void renderButton(PoseStack p_93843_, int p_93844_, int p_93845_, float p_93846_) {
        		p_93843_.pushPose();
        		p_93843_.translate(0, 0, 610.0F);
        		super.renderButton(p_93843_, p_93844_, p_93845_, p_93846_);
        		p_93843_.popPose();
        	}
        };
        feedBabiesCheckBox.visible = this.settingsOpen;
        this.addRenderableWidget(feedBabiesCheckBox);
    }
    
    public boolean isNumberString(String string) {
    	String regex = "[0-9]+";
    	return string.isEmpty() || string.matches(regex);
    }
    
    public void setSettingsOpen(boolean value) {
    	this.settingsOpen = value; 	
        feedBabiesCheckBox.visible = value;    	
        maxAnimalsApplyButton.visible = value;   
        maxAnimalsApplyButton.active = false;
        maxAnimalsTextBox.visible = value;
        maxAnimalsTextBox.setValue(""+this.breeder.getMaxAnimals());
    }
    
    public void sendFeedBabiesPacket(boolean value)
	{
		breeder.setShouldFeedBabies(value);
    	CompoundTag message = new CompoundTag();
		message.putBoolean("FeedBabies", value);
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(breeder, message));
	}
    
    public void sendMaxAnimalsPacket(int value)
	{
		breeder.setMaxAnimals(value);
    	CompoundTag message = new CompoundTag();
		message.putInt("MaxAnimals", value);
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(breeder, message));
	}
    
    public void sendRedstoneModeUpdate(RedstoneMode mode)
	{
    	breeder.setRedstoneMode(mode);;
		CompoundTag message = new CompoundTag();
		message.putInt("RedstoneMode", mode.ordinal());
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(breeder, message));
	}
    
    public void sendBoundingBoxUpdate(boolean value)
	{
    	breeder.setShowBoundingBox(value);
		CompoundTag message = new CompoundTag();
		message.putBoolean("ShowBoundingBox", value);
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(breeder, message));
	}
    
    @Override
    protected boolean isHovering(int p_97768_, int p_97769_, int p_97770_, int p_97771_, double p_97772_, double p_97773_) {
		if(settingsOpen) {
			return false;
		}
    	return super.isHovering(p_97768_, p_97769_, p_97770_, p_97771_, p_97772_, p_97773_);    
    }
    
    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        if(settingsOpen) {
        	RenderSystem.setShaderTexture(0, GuiUtils.BLANK_GUI);
        	GuiUtils.drawScaledTexture(matrixStack, settingsRelX, settingsRelY, 600, settingsWidth, settingsHeight, 248.0F, 166.0F);
            
            int i = this.maxAnimalsTextBox.isFocused() ? -1 : -6250336;
            GuiUtils.fill(matrixStack, this.maxAnimalsTextBox.x - 1, this.maxAnimalsTextBox.y - 1, this.maxAnimalsTextBox.x + this.maxAnimalsTextBox.getWidth() + 1, this.maxAnimalsTextBox.y + this.maxAnimalsTextBox.getHeight() + 1, i, 650.0F);
            GuiUtils.fill(matrixStack, this.maxAnimalsTextBox.x, this.maxAnimalsTextBox.y, this.maxAnimalsTextBox.x + this.maxAnimalsTextBox.getWidth(), this.maxAnimalsTextBox.y + this.maxAnimalsTextBox.getHeight(), -16777216, 650.0F);
        }
        
        if(!settingsOpen) {
	        this.renderTooltip(matrixStack, mouseX, mouseY);  
	        int energyBarX = relX + 16;
	        int energyBarY = relY + 8;
	        if(mouseX > energyBarX && mouseX < energyBarX + 16 && mouseY > energyBarY && mouseY < energyBarY + 68) {
	        	List<Component> textComponents = new ArrayList<>();
	        	String energyValue = String.format("%,d", breeder.energyStorage.getEnergyStored());
	        	String maxEnergyValue = String.format("%,d", breeder.getEnergyCapacity());
	        	textComponents.add(Component.literal(energyValue + " / " + maxEnergyValue));
	        	Optional<TooltipComponent> tooltipComponent = java.util.Optional.empty();
	        	this.renderTooltip(matrixStack, textComponents, tooltipComponent, mouseX, mouseY);
	        }
        }
    }
    
    @Override
    public boolean mouseClicked(double x, double y, int button) {
    	if(this.settingsOpen) {
    		double mouseFixedX = x - settingsRelX;
            double mouseFixedY = y - settingsRelY;
            if(mouseFixedX > 0 && mouseFixedX < settingsWidth && mouseFixedY > 0 && mouseFixedY < settingsHeight) {
            	for(GuiEventListener guieventlistener : this.children()) {
            		if (guieventlistener.mouseClicked(x, y, button)) {
            			this.setFocused(guieventlistener);
            			if (button == 0) {
            				this.setDragging(true);
            			}

            			return true;
            		}
            	}
            	return true;
            }
            this.setSettingsOpen(false);
            return true;
    	}
    	return super.mouseClicked(x, y, button);
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        //drawString(matrixStack, Minecraft.getInstance().font, "Energy: " + menu.getEnergy(), 10, 10, 0xffffff);
    	if(settingsOpen) {
    		matrixStack.pushPose();
            matrixStack.translate(-this.getGuiLeft(), -this.getGuiTop(), 650.0F);
            drawString(matrixStack, font, Component.translatable("gui.wam_utils.auto_breeder.maxAnimals"), settingsRelX + 20, settingsRelY + 10, 14737632);
            matrixStack.popPose();
    	}
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, GUI);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        this.blit(matrixStack, relX, relY, 0, 0, this.imageWidth, this.imageHeight); 
        
        int energyBarX = relX + 16;
        int energyBarY = relY + 8;         
        float energyRatio = (float)(breeder.energyStorage.getEnergyStored()) / (float)breeder.getEnergyCapacity();
        int energyHeight = (int)(energyRatio * 68.0F);
        if(energyRatio > 0) {
        	Screen.fill(matrixStack, energyBarX + 1, energyBarY + 68 + 1, energyBarX + 1 + 16, energyBarY + 68 + 1 - energyHeight, Color.red.getRGB());
        }
    }
}
