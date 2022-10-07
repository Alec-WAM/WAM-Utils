package alec_wam.wam_utils.blocks.machine.quarry;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.compress.utils.Lists;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.blocks.machine.quarry.QuarryBE.VoidFilter;
import alec_wam.wam_utils.client.GuiIcons;
import alec_wam.wam_utils.client.GuiUtils;
import alec_wam.wam_utils.client.widgets.RedstoneModeButton;
import alec_wam.wam_utils.client.widgets.ScaledCheckBox;
import alec_wam.wam_utils.client.widgets.ScaledImageButton;
import alec_wam.wam_utils.server.network.MessageBlockEntityUpdate;
import alec_wam.wam_utils.utils.ItemUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class QuarryScreen extends AbstractContainerScreen<QuarryContainer> {

    private final ResourceLocation GUI = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/quarry.png");
    private QuarryBE quarry;
    
    private boolean settingsOpen = false;
    private int settingsWidth;
    private int settingsHeight;
    private int settingsRelX;
    private int settingsRelY;
    
    private Item filterItem;
    private List<ScaledCheckBox> settingsCheckBoxes = Lists.newArrayList();
    private EditBox filterTextBox;
    private Button addFilterButton;
    
    private int filterListOffset = 0;
    
    public QuarryScreen(QuarryContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        this.width = imageWidth = 176;
        this.height = imageHeight = 208;
        
        this.quarry = container.blockEntity;
    }

    @Override
    protected void init() {
    	super.init();
    	
    	//LumberjackSetting[] settings = LumberjackSetting.values();
    	
        
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        int settingsButtonX = relX + 152;
        int settingsButtonY = relY + 6;
        
        this.settingsWidth = 164;
    	this.settingsHeight = 150;        
    	this.settingsRelX = (this.width - settingsWidth) / 2;
        this.settingsRelY = (this.height - settingsHeight) / 2;
        
        RedstoneModeButton redstoneButton = new RedstoneModeButton(this, relX + 134, settingsButtonY, 16, 16, quarry.getRedstoneMode()){

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
    	
    	int checkBoxX = settingsRelX + 10;
    	
    	int addFilterButtonX = settingsRelX + 140;
        int addFilterButtonY = settingsRelY + 95;
        this.addFilterButton = new Button(addFilterButtonX, addFilterButtonY, 10, 10, Component.literal("+"), new Button.OnPress() {

			@Override
			public void onPress(Button p_93751_) {
				addFilter();
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
        this.addFilterButton.visible = this.settingsOpen;
        this.addFilterButton.active = false;
        this.addRenderableWidget(addFilterButton);
        
        int filterTextBoxX = settingsRelX + 30;
        int filterTextBoxY = settingsRelY + 91;
        
        this.filterTextBox = new EditBox(this.font, filterTextBoxX, filterTextBoxY, 105, 20, Component.empty()) {        	
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
        this.filterTextBox.setValue("");
        this.filterTextBox.setResponder((p_232916_) -> {
        	addFilterButton.active = isValidFilter(p_232916_);
        });
        this.filterTextBox.visible = this.settingsOpen;
        this.addRenderableWidget(this.filterTextBox);        
        
    	/*int buttonIndex = 0;
    	for(LumberjackSetting setting : settings) {
    		String name = setting.name().toLowerCase();
    		final LumberjackSetting buttonSetting = setting;
	    	ScaledCheckBox settingsCheckbox = new ScaledCheckBox(checkBoxX + 5, settingsRelY + 5 + (20 * buttonIndex), 15, 15, Component.translatable("gui.wam_utils.auto_quarry.setting."+name), quarry.getSetting(buttonSetting)){
	        	
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
    	}*/
    }
    
    public boolean isValidFilter(String string) {
    	if(string.isEmpty())return false;
    	
    	if(string.startsWith("#")) {
    		//TODO Validate Tag
    		String tagString = string.substring(1);
    		return !tagString.isEmpty() && tagString.contains(":") && ResourceLocation.isValidResourceLocation(tagString);
    	}
    	
    	if(!ResourceLocation.isValidResourceLocation(string)) {
    		return false;
    	}
    	
    	ResourceLocation itemKey = new ResourceLocation(string);
		Optional<Item> item = Registry.ITEM.getOptional(itemKey);
		return item.isPresent() && item.get() != Items.AIR;
    }
    
    public void addFilter() {
    	VoidFilter filter = null;
    	if(this.filterItem !=null) {
    		filter = new VoidFilter(this.filterItem);
    	}
    	else {
    		String value = this.filterTextBox.getValue();
    		if(!value.isEmpty()) {
    			if(value.startsWith("#")) {
    				//Tag
    				String tagValue = value.substring(1);
    				ResourceLocation tagKey = new ResourceLocation(tagValue);
    				filter = new VoidFilter(tagKey);
    			}
    			else {
    				//Find Item
    				ResourceLocation itemKey = new ResourceLocation(value);
    				Optional<Item> item = Registry.ITEM.getOptional(itemKey);
    				if(item.isPresent()) {
    		    		filter = new VoidFilter(item.get());    					
    				}
    			}
    		}
    	}
    	if(filter !=null) {
    		this.filterItem = null;
    		this.filterTextBox.setValue("");
    		quarry.addVoidFilter(filter);
    		CompoundTag message = new CompoundTag();
    		message.putBoolean("AddFilter", true);
    		message.put("Filter", filter.serializeNBT());
    		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(quarry, message));
    	}
    }
    
    public boolean removeFilter(VoidFilter filter) {
    	boolean removed = quarry.removeVoidFilter(filter);
    	CompoundTag message = new CompoundTag();
		message.putBoolean("RemoveFilter", true);
		message.put("Filter", filter.serializeNBT());
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(quarry, message));
    	return removed;
    }
    
    public void setSettingsOpen(boolean value) {
    	this.settingsOpen = value; 	
    	
    	this.addFilterButton.visible = value;
    	this.filterTextBox.visible = value;
    	/*for(ScaledCheckBox checkBox : settingsCheckBoxes) {
    		checkBox.visible = value;
    	}*/
    }
    
    public void sendRedstoneModeUpdate(RedstoneMode mode)
	{
    	quarry.setRedstoneMode(mode);;
		CompoundTag message = new CompoundTag();
		message.putInt("RedstoneMode", mode.ordinal());
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(quarry, message));
	}
    
    /*public void sendSettingsUpdate(LumberjackSetting setting, boolean value)
	{
    	quarry.updateSetting(setting, value);
		CompoundTag message = new CompoundTag();
		message.putInt("Setting", setting.ordinal());
		message.putBoolean("Value", value);
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(quarry, message));
	}*/
    
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
        	
        	int listX = settingsRelX + 10;
        	int listY = settingsRelY + 15;
        	int listWidth = 144;
        	int listHeight = 74;
        	matrixStack.pushPose();
        	matrixStack.translate(0, 0, 600);
        	Screen.fill(matrixStack, listX, listY, listX + listWidth, listY + listHeight, Color.BLACK.getRGB());
        	
        	int textBoxX = settingsRelX + 30;
        	int textBoxY = settingsRelY + 93;
        	int textBoxWidth = 105;
        	int textBoxHeight = 16;
        	Screen.fill(matrixStack, textBoxX, textBoxY, textBoxX + textBoxWidth, textBoxY + textBoxHeight, Color.BLACK.getRGB());
        	
        	int itemCount = this.quarry.getVoidFilters().size();
        	int maxIndex = Math.min(this.filterListOffset + 4, itemCount);
        	int renderIndex = 0;
        	long time = Minecraft.getInstance().level.getGameTime();
        	for(int i = this.filterListOffset; i < maxIndex; i++) {
        		VoidFilter filter = this.quarry.getVoidFilters().get(i);
        		Item displayItem = filter.getItem();
        		String displayValue = displayItem == null ? "#"+filter.getTag().toString().toLowerCase() : Registry.ITEM.getKey(displayItem).toString();
        		
        		if(displayItem == null) {
        			List<Item> items = filter.getTagItems();
        			displayItem = items.get((int) (time / 20L % items.size()));
        		}
        		if(displayItem !=null) {
	        		ItemStack renderStack = new ItemStack(displayItem);
	        		this.itemRenderer.blitOffset = 600.0F;
	        		this.itemRenderer.renderGuiItem(renderStack, listX + 2, listY + 2 + (18 * renderIndex));
	        		this.itemRenderer.blitOffset = 0.0F;
        		}
        		
        		float strWidth = 110.0F / this.font.getSplitter().stringWidth(displayValue);        		
        		matrixStack.pushPose();
        		float scale = Math.min(strWidth, 0.8F);
        		int textX = listX + 2 + 18;
        		int textY = listY + 2 + (18 * renderIndex) + 5;
        		matrixStack.translate(textX, textY, 0);
        		matrixStack.scale(scale, scale, 1.0F);
        		Component valueComp = Component.literal(displayValue).withStyle(ChatFormatting.WHITE);
        		this.font.draw(matrixStack, valueComp, 0, 0, 610);
        		matrixStack.popPose();
        		
        		matrixStack.pushPose();
        		int deleteX = listX + 135;
        		int deleteY = listY + 2 + (18 * renderIndex) + 4;
        		Component closeComp = Component.literal("x").withStyle(ChatFormatting.RED);
        		this.font.draw(matrixStack, closeComp, deleteX, deleteY, 610);
        		matrixStack.popPose();
        		
        		renderIndex++;
        	}
        	
        	int filterItemSlotX = settingsRelX + 10;
        	int filterItemSlotY = settingsRelY + 92;
        	RenderSystem.setShaderTexture(0, GUI);
            this.blit(matrixStack, filterItemSlotX, filterItemSlotY, 7, 66, 18, 18);        	
        	//Text Box item
        	String textBox = this.filterTextBox.getValue();
        	if(!textBox.isEmpty()) {
        		Item displayItem = null;
        		if(textBox.contains(":")) {
	        		if(textBox.startsWith("#") && ResourceLocation.isValidResourceLocation(textBox.substring(1))) {
	        			//WAMUtilsMod.LOGGER.debug("Valid Tag");
	        			//Tag Item
	        			ResourceLocation tagKey = new ResourceLocation(textBox.substring(1));	    				
	        			List<Item> items = ItemUtils.getTagItems(tagKey);
	        			if(!items.isEmpty()) {
	        				displayItem = items.get((int) (time / 20L % items.size()));
	        			}
	        		}
	        		else if(ResourceLocation.isValidResourceLocation(textBox)){
	        			ResourceLocation itemKey = new ResourceLocation(textBox);	    				
	        			Optional<Item> item = Registry.ITEM.getOptional(itemKey);
	    				if(item.isPresent()) {
	    		    		displayItem = item.get();    					
	    				}
	        		}
        		}
        		
        		if(displayItem !=null) {
	        		ItemStack renderStack = new ItemStack(displayItem);
	        		this.itemRenderer.blitOffset = 600.0F;
	        		this.itemRenderer.renderGuiItem(renderStack, filterItemSlotX + 1, filterItemSlotY + 1);
	        		this.itemRenderer.blitOffset = 0.0F;
        		}
        	}
        	
        	matrixStack.popPose();
        }
        
        if(!settingsOpen) { 
	        this.renderTooltip(matrixStack, mouseX, mouseY);  
	        int energyBarX = relX + 17;
	        int energyBarY = relY + 6;
	        if(mouseX > energyBarX && mouseX < energyBarX + 16 && mouseY > energyBarY && mouseY < energyBarY + 56) {
	        	List<Component> textComponents = new ArrayList<>();
	        	String energyValue = String.format("%,d", quarry.energyStorage.getEnergyStored());
	        	String maxEnergyValue = String.format("%,d", quarry.getEnergyCapacity());
	        	textComponents.add(Component.literal(energyValue + " / " + maxEnergyValue));
	        	Optional<TooltipComponent> tooltipComponent = java.util.Optional.empty();
	        	this.renderTooltip(matrixStack, textComponents, tooltipComponent, mouseX, mouseY);
	        }
	        
	        if(quarry.isDoneMining()) {
	        	RenderSystem.setShaderTexture(0, GUI);
	        	GuiUtils.drawScaledTexture(matrixStack, relX + 50, relY + 23, 0, 18, 18, 176, 176 + 18, 16, 16 + 18, 256.0F, 256.0F); 	
	        	font.draw(matrixStack, Component.translatable("gui.wam_utils.quarry.done_mining"), relX + 70, relY + 20 + 9, 4210752);
	        }
	        
	        int fullWarningX = relX + 80;
	        int fullWarningY = relY + 46;
	        if(mouseX > fullWarningX && mouseX < fullWarningX + 16 && mouseY > fullWarningY && mouseY < energyBarY + 16) {
	        	if(quarry.isFull()) {
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
            	int listX = 10;
            	int listY = 15;
            	int listWidth = 144;
            	int listHeight = 74;
            	
            	int itemCount = this.quarry.getVoidFilters().size();
            	int maxIndex = Math.min(this.filterListOffset + 4, itemCount);
            	int renderIndex = 0;
            	int deleteX = listX + 135;
            	if(mouseFixedX > deleteX && mouseFixedX < deleteX + 5) {
            		WAMUtilsMod.LOGGER.debug("Remove");
	        		for(int i = this.filterListOffset; i < maxIndex; i++) {
	            		VoidFilter filter = this.quarry.getVoidFilters().get(i);
	            		int deleteY = listY + 2 + (18 * renderIndex) + 4;
	            		if(mouseFixedY > deleteY && mouseFixedY < deleteY + 5) {
	            			this.removeFilter(filter);
	            			return true;
	            		}
	            		renderIndex++;
	            	}
            	}
            	
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
    	if(this.settingsOpen) {
    		int itemCount = this.quarry.getVoidFilters().size();
    		if(scroll < 0.0F) {
    			if(this.filterListOffset < itemCount - 1) {
    				this.filterListOffset++;
    			}
    		}
    		if(scroll > 0.0F) {
    			if(this.filterListOffset > 0) {
    				this.filterListOffset--;
    			}
    		}
    	}
    	return super.mouseScrolled(mouseX, mouseY, scroll);
    }

    @Override
    public boolean keyPressed(int p_97765_, int p_97766_, int p_97767_) {
    	if (p_97765_ == 256 && this.settingsOpen) {
    		this.setSettingsOpen(false);
    		return true;
    	}
    	
    	if(this.filterTextBox.isFocused() && this.settingsOpen) {
    		return this.filterTextBox.keyPressed(p_97765_, p_97766_, p_97767_);
    	}
    	
    	return super.keyPressed(p_97765_, p_97766_, p_97767_);
    }
    
    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
    	//super.renderLabels(matrixStack, mouseX, mouseY);
        //drawString(matrixStack, Minecraft.getInstance().font, "Energy: " + menu.getEnergy(), 10, 10, 0xffffff);
    	
    	if(settingsOpen) {
    		matrixStack.pushPose();
            matrixStack.translate(-this.getGuiLeft(), -this.getGuiTop(), 650.0F);            
            this.font.draw(matrixStack, Component.literal("Void Items"), settingsRelX + 10, settingsRelY + 5, 0x000000);
            matrixStack.popPose();
    	}
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, GUI);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        this.blit(matrixStack, relX, relY, 0, 0, this.imageWidth, this.imageHeight); 
        
        int energyBarX = relX + 17;
        int energyBarY = relY + 6;         
        float energyRatio = (float)(quarry.energyStorage.getEnergyStored()) / (float)quarry.getEnergyCapacity();
        int energyHeight = (int)(energyRatio * 56.0F);
        if(energyRatio > 0) {
        	Screen.fill(matrixStack, energyBarX + 1, energyBarY + 56 + 1, energyBarX + 1 + 16, energyBarY + 56 + 1 - energyHeight, Color.red.getRGB());
        }    
        
        if(!settingsOpen) {
        	if(quarry.isFull()) {
        		GuiIcons.ICON_WARNING.renderIcon(matrixStack, relX + 80, relY + 46, 0.0, 16, 16);
        	}
        }
    }
}
