package alec_wam.wam_utils.blocks.machine.auto_lumberjack;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.compress.utils.Lists;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.blocks.machine.BonemealMode;
import alec_wam.wam_utils.blocks.machine.SlotLock;
import alec_wam.wam_utils.blocks.machine.auto_lumberjack.AutoLumberjackBE.LumberjackSetting;
import alec_wam.wam_utils.client.GuiIcons;
import alec_wam.wam_utils.client.GuiUtils;
import alec_wam.wam_utils.client.widgets.BonemealModeButton;
import alec_wam.wam_utils.client.widgets.LockButton;
import alec_wam.wam_utils.client.widgets.RedstoneModeButton;
import alec_wam.wam_utils.client.widgets.ScaledCheckBox;
import alec_wam.wam_utils.server.network.MessageBlockEntityUpdate;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public class AutoLumberjackScreen extends AbstractContainerScreen<AutoLumberjackContainer> {

    private final ResourceLocation GUI = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/auto_lumberjack.png");
    private AutoLumberjackBE lumberjack;
    
    private boolean settingsOpen = false;
    private int settingsWidth;
    private int settingsHeight;
    private int settingsRelX;
    private int settingsRelY;
    private List<ScaledCheckBox> settingsCheckBoxes = Lists.newArrayList();
    
    public AutoLumberjackScreen(AutoLumberjackContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        this.width = imageWidth = 176;
        this.height = imageHeight = 208;
        
        this.lumberjack = container.blockEntity;
    }

    @Override
    protected void init() {
    	super.init();
    	
    	LumberjackSetting[] settings = LumberjackSetting.values();
    	this.settingsWidth = 120;
    	this.settingsHeight = 5 + (20 * settings.length);        
    	this.settingsRelX = (this.width - settingsWidth) / 2;
        this.settingsRelY = (this.height - settingsHeight) / 2;
        
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        int settingsButtonX = relX + 150;
        int settingsButtonY = relY + 6;
        
        RedstoneModeButton redstoneButton = new RedstoneModeButton(this, relX + 126, settingsButtonY, 20, 20, lumberjack.getRedstoneMode()){

			@Override
			public void onPress() {
				super.onPress();
				sendRedstoneModeUpdate(getRestoneMode());
			}
        	
        };
        this.addRenderableWidget(redstoneButton);
        
        ImageButton settingsButton = new ImageButton(settingsButtonX, settingsButtonY, 20, 20, 0, 0, 21, GuiUtils.WIDGETS, new Button.OnPress() {

			@Override
			public void onPress(Button p_93751_) {
				setSettingsOpen(true);
			}
        	
        });
        this.addRenderableWidget(settingsButton);
        
        BonemealModeButton bonemealModeButton = new BonemealModeButton(this, relX + 141, relY + 56, 20, 20, lumberjack.getBonemealMode()){

			@Override
			public void onPress() {
				super.onPress();
				sendBonemealModeUpdate(getBonemealMode());
			}
        	
        };
        this.addRenderableWidget(bonemealModeButton);
        
        int slotLockX = relX + 83;
    	int slotLockY = relY + 54;
        
    	for(int i = 0; i < AutoLumberjackBE.INPUT_SLOTS; i++) {
    		final int index = i;
	        LockButton lockButton = new LockButton(slotLockX + (i * 18), slotLockY, 10, 10) {
	        	@Override
	        	public void onPress() {
	        		super.onPress();
	        		sendSlotLockUpdate(index, isLocked());
	        	}
	        };
	        SlotLock slotLock = lumberjack.getSlotLock(i);
	        lockButton.setLocked(slotLock !=null && slotLock.isEnabled());
	        this.addRenderableWidget(lockButton);
    	}
    	
    	int checkBoxX = settingsRelX + 10;
    	
    	int buttonIndex = 0;
    	for(LumberjackSetting setting : settings) {
    		String name = setting.name().toLowerCase();
    		final LumberjackSetting buttonSetting = setting;
	    	ScaledCheckBox settingsCheckbox = new ScaledCheckBox(checkBoxX + 5, settingsRelY + 5 + (20 * buttonIndex), 15, 15, Component.translatable("gui.wam_utils.auto_lumberjack.setting."+name), lumberjack.getSetting(buttonSetting)){
	        	
	    		@Override
	        	public void onPress() {
	        		super.onPress();
	        		sendSettingsUpdate(buttonSetting, this.selected());
	        	}
	        	
	    		@Override
	        	public void renderButton(PoseStack p_93843_, int p_93844_, int p_93845_, float p_93846_) {
	        		p_93843_.pushPose();
	        		p_93843_.translate(0, 0, 650.0F);
	        		super.renderButton(p_93843_, p_93844_, p_93845_, p_93846_);
	        		p_93843_.popPose();
	        	}
	        };
	        settingsCheckbox.visible = this.settingsOpen;
	        this.addRenderableWidget(settingsCheckbox);
	        buttonIndex++;
	        settingsCheckBoxes.add(settingsCheckbox);
    	}
    }
    
    public void setSettingsOpen(boolean value) {
    	this.settingsOpen = value; 	
    	for(ScaledCheckBox checkBox : settingsCheckBoxes) {
    		checkBox.visible = value;
    	}
    }
    
    public void sendRedstoneModeUpdate(RedstoneMode mode)
	{
    	lumberjack.setRedstoneMode(mode);;
		CompoundTag message = new CompoundTag();
		message.putInt("RedstoneMode", mode.ordinal());
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(lumberjack, message));
	}
    
    public void sendBonemealModeUpdate(BonemealMode mode)
	{
    	lumberjack.setBonemealMode(mode);
		CompoundTag message = new CompoundTag();
		message.putInt("BonemealMode", mode.ordinal());
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(lumberjack, message));
	}
    
    public void sendSlotLockUpdate(int slot, boolean locked)
	{
    	lumberjack.updateSlotLock(slot, locked);
		CompoundTag message = new CompoundTag();
		message.putInt("Slot", slot);
		message.putBoolean("Enabled", locked);
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(lumberjack, message));
	}
    
    public void sendSettingsUpdate(LumberjackSetting setting, boolean value)
	{
    	lumberjack.updateSetting(setting, value);
		CompoundTag message = new CompoundTag();
		message.putInt("Setting", setting.ordinal());
		message.putBoolean("Value", value);
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(lumberjack, message));
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
        }
        
        if(!settingsOpen) {        	
        	for(int p = 0; p < 2; p++) {
	        	if(p == 0) {
	        		RenderSystem.setShaderTexture(0, GUI);	                
	        	}
        		for(int i = 0; i < AutoLumberjackBE.INPUT_SLOTS; i++) {
	        		int slotLockX = relX + 80 + (i * 18);
		        	int slotLockY = relY + 34;
		        	SlotLock lock = lumberjack.getSlotLock(i);
		        	if(lock != null) {
		        		if(lock.isEnabled() && lumberjack.seedItems.getStackInSlot(i).isEmpty()) {
		        			if(lock.getStack().isEmpty()) {
		        		        if(p == 0)
		        		        	GuiUtils.drawScaledTexture(matrixStack, slotLockX + 0.5D, slotLockY + 0.5D, 0, 15, 15, 176, 176 + 15, 0, 15, 256.0F, 256.0F);
		        			}
		        			else {
		        				if(p == 1) {
		        					this.itemRenderer.renderGuiItem(lock.getStack(), slotLockX, slotLockY);
			        				int myOpaqueColor = 0xff8b8b8b;
			        				int factor = (int)(255.0F * 0.6F);// 0-255;
			        				int color = ( factor << 24 ) | ( myOpaqueColor & 0x008b8b8b );
			        				renderSlotHighlight(matrixStack, slotLockX, slotLockY, getBlitOffset(), color);
		        				}
		        			}
		        		}
		        	}
	        	}
            }
        	
        	
	        this.renderTooltip(matrixStack, mouseX, mouseY);  
	        int energyBarX = relX + 16;
	        int energyBarY = relY + 8;
	        if(mouseX > energyBarX && mouseX < energyBarX + 16 && mouseY > energyBarY && mouseY < energyBarY + 68) {
	        	List<Component> textComponents = new ArrayList<>();
	        	String energyValue = String.format("%,d", lumberjack.energyStorage.getEnergyStored());
	        	String maxEnergyValue = String.format("%,d", lumberjack.getEnergyCapacity());
	        	textComponents.add(Component.literal(energyValue + " / " + maxEnergyValue));
	        	Optional<TooltipComponent> tooltipComponent = java.util.Optional.empty();
	        	this.renderTooltip(matrixStack, textComponents, tooltipComponent, mouseX, mouseY);
	        }
	        
	        int fullWarningX = relX + 80;
	        int fullWarningY = relY + 64;
	        if(mouseX > fullWarningX && mouseX < fullWarningX + 16 && mouseY > fullWarningY && mouseY < fullWarningY + 16) {
	        	if(lumberjack.isFull()) {
		        	List<Component> textComponents = new ArrayList<>();
		        	textComponents.add(Component.translatable("gui.wam_utils.warning.inventory_full"));
		        	Optional<TooltipComponent> tooltipComponent = java.util.Optional.empty();
		        	this.renderTooltip(matrixStack, textComponents, tooltipComponent, mouseX, mouseY);
	        	}
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double p_97755_, double p_97756_) {
    	return super.mouseDragged(mouseX, mouseY, button, p_97755_, p_97756_);
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
    	//super.renderLabels(matrixStack, mouseX, mouseY);
        //drawString(matrixStack, Minecraft.getInstance().font, "Energy: " + menu.getEnergy(), 10, 10, 0xffffff);
    	if(settingsOpen) {
    		/*matrixStack.pushPose();
            matrixStack.translate(-this.getGuiLeft(), -this.getGuiTop(), 650.0F);            
            this.font.draw(matrixStack, Component.literal("N"), settingsRelX + (settingsWidth / 2) - 3, settingsRelY + 52, 0x000000);
            matrixStack.popPose();*/
    	}
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, GUI);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        this.blit(matrixStack, relX, relY, 0, 0, this.imageWidth, this.imageHeight); 
        
        if(!this.settingsOpen) {
        	if(lumberjack.isFull()) {
        		GuiIcons.ICON_WARNING.renderIcon(matrixStack, relX + 80, relY + 64, 0.0F, 16, 16);
        	}
        }
        
        int energyBarX = relX + 16;
        int energyBarY = relY + 8;         
        float energyRatio = (float)(lumberjack.energyStorage.getEnergyStored()) / (float)lumberjack.getEnergyCapacity();
        int energyHeight = (int)(energyRatio * 68.0F);
        if(energyRatio > 0) {
        	Screen.fill(matrixStack, energyBarX + 1, energyBarY + 68 + 1, energyBarX + 1 + 16, energyBarY + 68 + 1 - energyHeight, Color.red.getRGB());
        }       
    }
}
