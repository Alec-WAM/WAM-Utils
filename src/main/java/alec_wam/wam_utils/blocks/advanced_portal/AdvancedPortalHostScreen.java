package alec_wam.wam_utils.blocks.advanced_portal;

import java.awt.Color;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.google.common.base.Strings;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.advanced_portal.AdvancedPortalClientDataManager.ClientAdvancedPortalData;
import alec_wam.wam_utils.blocks.advanced_portal.AdvancedPortalHostBE.PortalType;
import alec_wam.wam_utils.client.GuiIcons;
import alec_wam.wam_utils.client.GuiUtils;
import alec_wam.wam_utils.client.widgets.GuiIconButton;
import alec_wam.wam_utils.client.widgets.GuiIconToggleButton;
import alec_wam.wam_utils.client.widgets.LockButton;
import alec_wam.wam_utils.server.network.MessageBlockEntityUpdate;
import alec_wam.wam_utils.server.network.MessageContainerUpdate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class AdvancedPortalHostScreen extends AbstractContainerScreen<AdvancedPortalHostContainer> {

    private final ResourceLocation GUI = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/advanced_portal_host.png");

    private AdvancedPortalHostBE blockEntity;    
    private int portalListOffset = 0;
    private int selectedPortal = -1;
    private boolean onlyOwnerPortals = false;
    
    private ClientAdvancedPortalData currentPortalData;
    private ClientAdvancedPortalData linkedPortalData;
    
    private EditBox portalNameTextBox;    
    private Button portalNameApplyButton;
    private LockButton lockButton;
    private Button unlinkButton;
    
    private EditBox portalSearchTextBox;    
    private GuiIconButton connectButton;
    private GuiIconToggleButton publicSearchToggleButton;

    private Button rgbPortalTypeButton;
    private Button netherPortalTypeButton;
    private Button endPortalTypeButton;
    
    private ColorOptionsSlider redColorSlider;
    private ColorOptionsSlider greenColorSlider;
    private ColorOptionsSlider blueColorSlider;
    
    private int wantedHeight;
    
    public AdvancedPortalHostScreen(AdvancedPortalHostContainer container, Inventory inv, Component name) {
        super(container, inv, name);

    	this.blockEntity = container.blockEntity;
    	
    	PortalType portalType = this.blockEntity.getPortalSettings().portalType;    	
    	this.wantedHeight = this.imageHeight = (this.linkedPortalData == null || portalType == PortalType.RGB) ? 166 : 94;
    }

    public void getPortalInfo(boolean updateButtons) {
    	Optional<ClientAdvancedPortalData> data = AdvancedPortalClientDataManager.getAllPortals(false).stream().filter((portal) -> portal.getPortalUUID().equals(this.blockEntity.getPortalUUID())).findFirst();
		if(data.isPresent()) {
			this.currentPortalData = data.get();
			
			if(updateButtons) {
				this.portalNameTextBox.setValue("" + this.currentPortalData.getName());
			}
			
			WAMUtilsMod.LOGGER.debug("Load Portal 1");
			if(this.currentPortalData.getLinkedPortalUUID() !=null) {
				WAMUtilsMod.LOGGER.debug("Load Other Portal 0");
    			Optional<ClientAdvancedPortalData> dataLinked = AdvancedPortalClientDataManager.getAllPortals(false).stream().filter((portal) -> portal.getPortalUUID().equals(this.currentPortalData.getLinkedPortalUUID())).findFirst();
				if(dataLinked.isPresent()) {
	    			this.linkedPortalData = dataLinked.get();
	    			if(updateButtons) {
	    				updateLinkedData();
	    			}
				}
			}
		}
		
		PortalType portalType = this.blockEntity.getPortalSettings().portalType;
		this.wantedHeight = (portalType == PortalType.RGB || this.linkedPortalData == null) ? 166 : 94;
    }
    
    @Override
    protected void containerTick() {
    	if(this.currentPortalData == null) {
    		getPortalInfo(true);
    	}
    	else {
    		if(this.currentPortalData.getLinkedPortalUUID() !=null && this.linkedPortalData == null) {
				WAMUtilsMod.LOGGER.debug("Load Other Portal 2");
    			Optional<ClientAdvancedPortalData> dataLinked = AdvancedPortalClientDataManager.getAllPortals(false).stream().filter((portal) -> portal.getPortalUUID().equals(this.currentPortalData.getLinkedPortalUUID())).findFirst();
				if(dataLinked.isPresent()) {
	    			this.linkedPortalData = dataLinked.get();
	    			updateLinkedData();
				}
			}
    	}
    	
    	boolean linked = this.linkedPortalData != null;    
    	
    	PortalType portalType = this.blockEntity.getPortalSettings().portalType;
    	this.wantedHeight = (portalType == PortalType.RGB || !linked) ? 166 : 94;
    	
    	if(this.imageHeight != wantedHeight) {
	    	this.imageHeight = (int) Mth.lerp(0.25D, imageHeight, wantedHeight + 4);
	    	
	    	int relY = (this.height - this.imageHeight) / 2;
	    	
	        int portalNameApplyY = relY + 10;
	        
	        this.portalNameApplyButton.y = portalNameApplyY;
	        
	        int portalNameTextBoxY = relY + 10;	        
	        this.portalNameTextBox.y = portalNameTextBoxY;
	        
	        int lockButtonY = relY + 10;
	        this.lockButton.y = lockButtonY;
	        
	        int unlinkButtonY = relY + 40;
	        this.unlinkButton.y = unlinkButtonY;
	        

	        int portalSearchTextBoxY = relY + 138;    
	        this.portalSearchTextBox.y = portalSearchTextBoxY;
	        this.publicSearchToggleButton.y = portalSearchTextBoxY;
	        this.connectButton.y = portalSearchTextBoxY;
			
			this.portalSearchTextBox.visible = this.imageHeight > 155 && !linked;
			this.publicSearchToggleButton.visible = this.imageHeight > 155 && !linked;
			this.connectButton.visible = this.imageHeight > 155 && !linked;
	        
	        int portalTypeButtonsY = relY + 70;
	        this.rgbPortalTypeButton.y = portalTypeButtonsY;
	        this.netherPortalTypeButton.y = portalTypeButtonsY;
	        this.endPortalTypeButton.y = portalTypeButtonsY;
	
	        int sliderY = portalTypeButtonsY + 25;
	        this.redColorSlider.y = sliderY;
	        this.greenColorSlider.y = sliderY + 20;
	        this.blueColorSlider.y = sliderY + 20 + 20;
	        
	        this.redColorSlider.visible = portalType == PortalType.RGB && this.imageHeight > 155 && linked;
			this.greenColorSlider.visible = portalType == PortalType.RGB && this.imageHeight > 155 && linked;
			this.blueColorSlider.visible = portalType == PortalType.RGB && this.imageHeight > 155 && linked;
    	}
		
    	this.portalSearchTextBox.visible = !linked;
    	this.publicSearchToggleButton.visible = !linked;
    	
		this.connectButton.visible = !linked;
        this.connectButton.active = this.selectedPortal != -1;
    }
    
    @Override
    protected void init() {
    	super.init();
    	
    	getPortalInfo(false);
    	
    	PortalType portalType = this.blockEntity.getPortalSettings().portalType;
    	boolean linked = this.linkedPortalData != null;     
    	//this.wantedHeight = (portalType == PortalType.RGB || !linked) ? 166 : 94;
    	this.wantedHeight = this.imageHeight = (!linked || portalType == PortalType.RGB) ? 166 : 94;
        
    	int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        
        /*RedstoneModeButton redstoneButton = new RedstoneModeButton(this, relX + 152, relY + 6, 16, 16, remover.getRedstoneMode()){

			@Override
			public void onPress() {
				super.onPress();
				sendRedstoneModeUpdate(getRestoneMode());
			}
        	
        };
        this.addRenderableWidget(redstoneButton);*/
        
        int portalNameApplyX = relX + 130;
        int portalNameApplyY = relY + 10;
        
        this.portalNameApplyButton = new Button(portalNameApplyX, portalNameApplyY, 40, 20, Component.translatable("gui.wam_utils.apply"), new Button.OnPress() {

			@Override
			public void onPress(Button p_93751_) {
				String value = portalNameTextBox.getValue();
				if(value !=null && !currentPortalData.getName().equals(value)) {
					sendNameUpdatePacket(value);
				}
			}
        	
        });
        this.portalNameApplyButton.active = false;
        this.addRenderableWidget(portalNameApplyButton);
        
        int portalNameTextBoxX = relX + 30;
        int portalNameTextBoxY = relY + 10;        
        
        this.portalNameTextBox = new EditBox(this.font, portalNameTextBoxX, portalNameTextBoxY, 95, 20, Component.empty());        
        if(this.currentPortalData !=null) {
        	this.portalNameTextBox.setValue(""+this.currentPortalData.getName());
        }
        else {
        	this.portalNameTextBox.setSuggestion("Portal Name");
        }
        this.portalNameTextBox.setResponder((value) -> {
        	if(Strings.isNullOrEmpty(value)) {
        		this.portalNameTextBox.setSuggestion("Portal Name");
        	}
        	else {
        		this.portalNameTextBox.setSuggestion(null);
        	}
        	this.portalNameApplyButton.active = this.currentPortalData !=null && value !=null && !this.currentPortalData.getName().equals(value);
        });
        this.addRenderableWidget(this.portalNameTextBox);
        
        
        int lockButtonX = relX + 5;
        int lockButtonY = relY + 10;
        this.lockButton = new LockButton(lockButtonX, lockButtonY, 20, 20) {
        	@Override
        	public void onPress() {
        		super.onPress();
        		sendPortalLockUpdate(isLocked());
        	}
			
			@Override
			public void renderToolTip(PoseStack poseStack, int mouseX, int mouseY) {
				String tooltip = "gui.wam_utils." + (isLocked() ? "private" : "public");
				AdvancedPortalHostScreen.this.renderTooltip(poseStack, Component.translatable(tooltip), mouseX, mouseY);
			}
        };
        lockButton.setLocked(currentPortalData !=null && currentPortalData.isPrivatePortal());
        this.addRenderableWidget(lockButton);
        
        int unlinkButtonX = relX + 140;
        int unlinkButtonY = relY + 40;
        this.unlinkButton = new Button(unlinkButtonX, unlinkButtonY, 20, 20, Component.literal("X"), new Button.OnPress() {
			
			@Override
			public void onPress(Button p_93751_) {
				unlinkPortal();
			}
		});
        this.unlinkButton.visible = linked;
        this.addRenderableWidget(unlinkButton); 
        
        int portalSearchTextBoxX = relX + 30;
        int portalSearchTextBoxY = relY + 138;        
        
        this.portalSearchTextBox = new EditBox(this.font, portalSearchTextBoxX, portalSearchTextBoxY, 115, 20, Component.empty());  
        this.portalSearchTextBox.setSuggestion("Search");
        this.portalSearchTextBox.setResponder((value) -> {
        	if(Strings.isNullOrEmpty(value)) {
        		this.portalSearchTextBox.setSuggestion("Search");
        	}
        	else {
        		this.portalSearchTextBox.setSuggestion(null);
        	}
        });
        this.portalSearchTextBox.visible = !linked;
        this.addRenderableWidget(this.portalSearchTextBox);        
        
        int publicButtonX = relX + 6;
        int publicButtonY = relY + 138;
        publicSearchToggleButton = new GuiIconToggleButton(publicButtonX, publicButtonY, 20, 20, GuiIcons.BUTTON_PUBLIC, GuiIcons.BUTTON_PRIVATE, onlyOwnerPortals){

			@Override
			public void onPress() {
				super.onPress();
				onlyOwnerPortals = isSelected();
			}
			
			@Override
			protected void renderTooltip(PoseStack poseStack, int mouseX, int mouseY) {
				String tooltip = "gui.wam_utils." + (isSelected() ? "private" : "public");
				AdvancedPortalHostScreen.this.renderTooltip(poseStack, Component.translatable(tooltip), mouseX, mouseY);
			}
        };
        this.publicSearchToggleButton.visible = !linked;
        this.addRenderableWidget(publicSearchToggleButton);
        
        int connectX = relX + 148;
        int connectY = relY + 138;
        
        this.connectButton = new GuiIconButton(connectX, connectY, 20, 20, GuiIcons.ICON_CHECKMARK) {
        	@Override
			public void onPress() {
				connectToPortal();
			}
        };
        
        this.connectButton.visible = !linked;
        this.connectButton.active = this.selectedPortal != -1;
        this.addRenderableWidget(connectButton);
        
        
        
        int portalTypeButtonsX = relX + 8;
        int portalTypeButtonsY = relY + 70;
        
        this.rgbPortalTypeButton = new Button(portalTypeButtonsX, portalTypeButtonsY, 50, 20, Component.translatable("gui.wam_utils.advanced_portal.rgb"), new Button.OnPress() {
			@Override
			public void onPress(Button p_93751_) {
				setPortalType(PortalType.RGB);
			}
		});
    	this.rgbPortalTypeButton.active = portalType != PortalType.RGB;
    	this.rgbPortalTypeButton.visible = linked;
        this.addRenderableWidget(rgbPortalTypeButton);
        
        this.netherPortalTypeButton = new Button(portalTypeButtonsX + 55, portalTypeButtonsY, 50, 20, Component.translatable("gui.wam_utils.advanced_portal.nether"), new Button.OnPress() {
			@Override
			public void onPress(Button p_93751_) {
				setPortalType(PortalType.NETHER);
			}
		});
    	this.netherPortalTypeButton.active = portalType != PortalType.NETHER;
    	this.netherPortalTypeButton.visible = linked;
        this.addRenderableWidget(netherPortalTypeButton);
        
        this.endPortalTypeButton = new Button(portalTypeButtonsX + 55 + 55, portalTypeButtonsY, 50, 20, Component.translatable("gui.wam_utils.advanced_portal.end"), new Button.OnPress() {
			@Override
			public void onPress(Button p_93751_) {
				setPortalType(PortalType.END);
			}
		});
    	this.endPortalTypeButton.active = portalType != PortalType.END;
    	this.endPortalTypeButton.visible = linked;
        this.addRenderableWidget(endPortalTypeButton);
        
        Vector3f color = this.blockEntity.getPortalSettings().colorSettings;
        
        float redValue = color.x() / 255.0F;
        float greenValue = color.y() / 255.0F;
        float blueValue = color.z() / 255.0F;
        
        int sliderX = portalTypeButtonsX;
        int sliderY = portalTypeButtonsY + 25;
        
        redColorSlider = new ColorOptionsSlider(sliderX, sliderY, 100, 20, redValue, (value -> {
        	float newValue = value.floatValue() * 255.0F;
        	Vector3f colors = this.blockEntity.getPortalSettings().colorSettings;
        	setColorSettings(newValue, colors.y(), colors.z());
        }));
        this.redColorSlider.visible = portalType == PortalType.RGB && linked;
        this.addRenderableWidget(redColorSlider);
        
        greenColorSlider = new ColorOptionsSlider(sliderX, sliderY + 20, 100, 20, greenValue, (value -> {
        	float newValue = value.floatValue() * 255.0F;
        	Vector3f colors = this.blockEntity.getPortalSettings().colorSettings;
        	setColorSettings(colors.x(), newValue, colors.z());
        }));
        this.greenColorSlider.visible = portalType == PortalType.RGB && linked;
        this.addRenderableWidget(greenColorSlider);
        
        blueColorSlider = new ColorOptionsSlider(sliderX, sliderY + 20 + 20, 100, 20, blueValue, (value -> {
        	float newValue = value.floatValue() * 255.0F;
        	Vector3f colors = this.blockEntity.getPortalSettings().colorSettings;
        	setColorSettings(colors.x(), colors.y(), newValue);
        }));
        this.blueColorSlider.visible = portalType == PortalType.RGB && linked;
        this.addRenderableWidget(blueColorSlider);
    }
    
    //The user can always connect to the portal because we filter out the owner on the server side
    public void connectToPortal() {
    	if(this.selectedPortal > -1) {
    		if(this.selectedPortal <= getFilteredPortals().size()) {
    			ClientAdvancedPortalData selectedData = getFilteredPortals().get(selectedPortal);
    			if(this.currentPortalData !=null) {
    				this.currentPortalData.setLinkedPortalUUID(selectedData.getPortalUUID());
    				selectedData.setLinkedPortalUUID(this.currentPortalData.getPortalUUID());
    			}
    			else {
    				selectedData.setLinkedPortalUUID(blockEntity.getPortalUUID());
    			}
    			this.linkedPortalData = selectedData;
    			
    			updateLinkedData();
    			
    			CompoundTag message = new CompoundTag();
    			message.putUUID("PortalLink", selectedData.getPortalUUID());
    			WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(this.blockEntity, message));
    			
    			this.selectedPortal = -1;
    		}
    	}
    }
    
    public void unlinkPortal() {
    	if(this.linkedPortalData !=null) {
    		this.linkedPortalData.setLinkedPortalUUID(null);
    		this.linkedPortalData = null;
    	}
    	if(this.currentPortalData !=null) {
			this.currentPortalData.setLinkedPortalUUID(null);
		}
    	
    	this.updateLinkedData();
    	
    	AdvancedPortalClientDataManager.unlinkPortals(this.blockEntity.getPortalUUID());
		
		CompoundTag message = new CompoundTag();
		message.putBoolean("PortalUnlink", true);
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(this.blockEntity, message));
		
		syncPortals();
    }
    
    public void updateLinkedData() {        
    	PortalType portalType = this.blockEntity.getPortalSettings().portalType;
    	boolean linked = this.linkedPortalData != null;    
    	this.unlinkButton.visible = linked;
    	this.rgbPortalTypeButton.visible = linked;
    	this.netherPortalTypeButton.visible = linked;
    	this.endPortalTypeButton.visible = linked;
    	this.redColorSlider.visible = portalType == PortalType.RGB && this.imageHeight > 155 && linked;
		this.greenColorSlider.visible = portalType == PortalType.RGB && this.imageHeight > 155 && linked;
		this.blueColorSlider.visible = portalType == PortalType.RGB && this.imageHeight > 155 && linked;
		
		//this.wantedHeight = (portalType == PortalType.RGB || !linked) ? 166 : 94;
    }
    
    public void sendNameUpdatePacket(String value)
	{
    	currentPortalData.setName(value);
    	CompoundTag message = new CompoundTag();
		message.putString("PortalName", value);
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(this.blockEntity, message));
		this.portalNameApplyButton.active = false;
	}
    
    public void sendPortalLockUpdate(boolean value)
	{
    	currentPortalData.setPrivatePortal(value);
    	CompoundTag message = new CompoundTag();
		message.putBoolean("PortalLocked", value);
		if(value) {
			message.putUUID("Owner", WAMUtilsMod.proxy.getClientPlayer().getUUID());
		}
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(this.blockEntity, message));
	}
    
    public void setPortalType(PortalType type) {
    	this.blockEntity.getPortalSettings().portalType = type;
    	CompoundTag message = new CompoundTag();
		message.putInt("PortalType", type.ordinal());
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(this.blockEntity, message));
		this.rgbPortalTypeButton.active = type != PortalType.RGB;
		this.netherPortalTypeButton.active = type != PortalType.NETHER;
		this.endPortalTypeButton.active = type != PortalType.END;
		
		this.redColorSlider.visible = type == PortalType.RGB && this.imageHeight > 155;
		this.greenColorSlider.visible = type == PortalType.RGB && this.imageHeight > 155;
		this.blueColorSlider.visible = type == PortalType.RGB && this.imageHeight > 155;
		
		//this.wantedHeight = type == PortalType.RGB ? 166 : 94;
    }
    
    public void setColorSettings(float red, float green, float blue)
	{
    	Vector3f colors = new Vector3f(red, green, blue);
    	this.blockEntity.getPortalSettings().colorSettings = colors;
    	
    	CompoundTag message = new CompoundTag();
		message.putIntArray("PortalColors", new int[] {(int)red, (int)green, (int)blue});
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(this.blockEntity, message));
	}
    
    public void syncPortals()
	{
    	CompoundTag message = new CompoundTag();
		message.putBoolean("SyncPortals", true);
		WAMUtilsMod.packetHandler.sendToServer(new MessageContainerUpdate(this.menu.containerId, message));
	}
    
    public List<ClientAdvancedPortalData> getFilteredPortals(){
    	String filter = this.portalSearchTextBox.getValue();
    	
    	return AdvancedPortalClientDataManager.getAllPortals(onlyOwnerPortals).stream().filter((portal) -> {
    		return !portal.getPortalUUID().equals(this.blockEntity.getPortalUUID()) && portal.getLinkedPortalUUID() == null && (Strings.isNullOrEmpty(filter) || portal.getName().toLowerCase().contains(filter.toLowerCase()));
    	}).sorted((a, b) -> {
    		if(Strings.isNullOrEmpty(a.getName())) {
    			return -1;
    		}
    		if(Strings.isNullOrEmpty(b.getName())) {
    			return -1;
    		}
    		return a.getName().compareTo(b.getName());
    	}).toList();
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
    	int itemCount = getFilteredPortals().size();
		if(scroll < 0.0F) {
			if(this.portalListOffset < itemCount - 1) {
				this.portalListOffset++;
			}
		}
		if(scroll > 0.0F) {
			if(this.portalListOffset > 0) {
				this.portalListOffset--;
			}
		}
    	return super.mouseScrolled(mouseX, mouseY, scroll);
    }

    @Override
    public boolean keyPressed(int p_97765_, int p_97766_, int p_97767_) {
    	if(this.portalNameTextBox.isFocused()) {
    		if(p_97765_ == 256) {
    			this.portalNameTextBox.setFocus(false);
    			return true;
    		}
    		return this.portalNameTextBox.keyPressed(p_97765_, p_97766_, p_97767_);
    	}
    	if(this.portalSearchTextBox.isFocused()) {
    		if(p_97765_ == 256) {
    			this.portalSearchTextBox.setFocus(false);
    			return true;
    		}
    		return this.portalSearchTextBox.keyPressed(p_97765_, p_97766_, p_97767_);
    	}
    	boolean linked = this.linkedPortalData !=null;
    	if(!linked) {
    		if(p_97765_ == 256 && this.selectedPortal >= 0) {
    			this.selectedPortal = -1;
    			this.portalListOffset = 0;
    			return true;
    		}
    		
    		if(p_97765_ == InputConstants.KEY_UP || p_97765_ == InputConstants.KEY_DOWN) {
	    		int itemCount = getFilteredPortals().size();
	    		if(itemCount > 0) {
		    		if(p_97765_ == InputConstants.KEY_DOWN) {
		    			final int lastPos = this.selectedPortal == -1 ? this.portalListOffset - 1 : this.selectedPortal;
	    				if(lastPos < itemCount - 1) {
		    				this.selectedPortal = lastPos + 1;
		    				if(selectedPortal >= this.portalListOffset + 5 && this.portalListOffset < itemCount - 1) {
		    					this.portalListOffset++;
		    				}
		    				return true;
		    			}
		    		}
		    		if(p_97765_ == InputConstants.KEY_UP) {
		    			if(this.selectedPortal > 0) {
		    				this.selectedPortal--;
		    				if(selectedPortal < this.portalListOffset && this.portalListOffset > 0) {
		    					this.portalListOffset--;
		    				}
		    				return true;
		    			}
		    		}
	    		}
    		}
    	}
    	return super.keyPressed(p_97765_, p_97766_, p_97767_);
    }
    
    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
        
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        
        if(this.linkedPortalData == null) {
        	if(this.imageHeight > 155) {
		        int listX = relX + 5;
		        int listY = relY + 35;
	        	
		        fill(matrixStack, listX, listY, listX + 165, listY + 100, Color.BLACK.getRGB());
	        	
	        	List<ClientAdvancedPortalData> portals = getFilteredPortals();
	        	
	        	if(portals.isEmpty()) {
	        		matrixStack.pushPose();
		        	Component itemText = Component.translatable("gui.wam_utils.tooltip.empty");
		        	int width = this.font.width(itemText);
		        	int emptyX = (155 - width) / 2;
		        	this.font.draw(matrixStack, itemText, listX + emptyX, listY + 45, 0xFFFFFF);
		        	matrixStack.popPose();
	        	}
	        	else {
			        int itemCount = portals.size();
			    	int maxIndex = Math.min(this.portalListOffset + 5, itemCount);
			    	int realLines = 0;
			        for(int l = this.portalListOffset; l < maxIndex; l++) {
			        	ClientAdvancedPortalData portal = portals.get(l);
			        	int boxY = listY + 4 + ((15 + 4) * realLines);
			        	boolean isHovering = mouseX >= listX + 2 && mouseX <= listX + 165 && mouseY >= boxY && mouseY <= boxY + 15;
			        	int color = isHovering ? Color.GRAY.brighter().getRGB() : Color.GRAY.getRGB();
			        	if(this.selectedPortal == l) {
			        		color = Color.GREEN.darker().getRGB();
			        	}
			        	fill(matrixStack, listX + 2, boxY, listX + 163, boxY + 15, color);
			        	
			        	matrixStack.pushPose();
			        	String displayName = Strings.isNullOrEmpty(portal.getName()) ? portal.getPortalUUID().toString() : portal.getName();
			        	Component itemText = Component.literal(displayName);
			        	if(portal.isPrivatePortal()) {
			        		itemText = Component.literal(displayName).withStyle(ChatFormatting.YELLOW);
			        	}
			        	float strWidth = 156.0F / this.font.getSplitter().stringWidth(displayName);       
			        	float scale = Math.min(strWidth, 1.0F);
			        	float scaleOffset = scale > 1.0F ? 15.0F * scale : 0.0F;
			        	float renderX = listX + 5;
			        	float renderY = boxY + 4 + scaleOffset;
			        	matrixStack.translate(renderX, renderY, 0.0F);
			        	matrixStack.scale(scale, scale, 1.0F);
			        	this.font.draw(matrixStack, itemText, 0, 0, 0xFFFFFF);
			        	matrixStack.popPose();
			        	realLines++;
			        }
	        	}
        	}
        }
        else {
        	String displayName = Strings.isNullOrEmpty(linkedPortalData.getName()) ? linkedPortalData.getPortalUUID().toString() : linkedPortalData.getName();
        	Component portalText = Component.literal(displayName);
        	if(linkedPortalData.isPrivatePortal()) {
        		portalText = Component.literal(displayName).withStyle(ChatFormatting.YELLOW);
        	}
        	
        	float renderX = relX + 12;
        	float renderY = relY + 40;
        	
        	this.font.draw(matrixStack, Component.literal("Linked to:"), renderX, renderY, 0x000000);
        	
        	float strWidth = 150.0F / this.font.getSplitter().stringWidth(displayName);       
        	float scale = Math.min(strWidth, 1.0F);
        	float scaleOffset = scale > 1.0F ? this.font.lineHeight * scale : 0.0F;
        	matrixStack.translate(renderX, renderY + this.font.lineHeight + 5 + scaleOffset, 0.0F);
        	matrixStack.scale(scale, scale, 1.0F);
        	this.font.draw(matrixStack, portalText, 0, 0, 0x000000);
        	matrixStack.popPose();
        	
        	if(this.blockEntity.getPortalSettings().portalType == PortalType.RGB && this.imageHeight > 155) {
	        	matrixStack.pushPose();
	        	int colorX = relX + 115;
	            int colorY = relY + 100;
	            Vector3f colors = this.blockEntity.getPortalSettings().colorSettings;
	            float r = colors.x() / 255.0F;
	            float g = colors.y() / 255.0F;
	            float b = colors.z() / 255.0F;
	            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(AdvancedPortalHostBERenderer.ADVANCED_PORTAL_TEXTURE);
	    		GuiUtils.drawTexturedColoredRect(matrixStack, colorX, colorY, 50.0F, 50.0F, r, g, b, 1.0F, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());
	        	matrixStack.popPose();
        	}
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
    	int relX = this.getGuiLeft();
        int relY = this.getGuiTop();
        if(this.linkedPortalData == null) {
    		List<ClientAdvancedPortalData> portals = getFilteredPortals();
    		if(!portals.isEmpty()) {
    			int itemCount = portals.size();
		    	int maxIndex = Math.min(this.portalListOffset + 5, itemCount);
		    	int realLines = 0;
		    	
		    	int listX = relX + 5;
		        int listY = relY + 35;
		        int listWidth = 165;
		        int listHeight = 100;
	        	
		        if(mouseX > listX && mouseX < listX + listWidth && mouseY > listY && mouseY < listY + listHeight) {
			        for(int l = this.portalListOffset; l < maxIndex; l++) {
			        	int boxY = listY + 4 + ((15 + 4) * realLines);
			        	boolean isClicking = mouseX >= listX + 2 && mouseX <= listX + 165 && mouseY >= boxY && mouseY <= boxY + 15;
			        	if(isClicking) {
			        		if(this.selectedPortal == l) {
			        			this.selectedPortal = -1;
			        		}
			        		else {
			        			this.selectedPortal = l;
			        		}
			        		return true;
			        	}
			        	realLines++;
			        }
		        }
		        /*else {
		        	this.selectedPortal = -1;
		        }*/
    		}
    	}
    	return super.mouseClicked(mouseX, mouseY, button);
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
        GuiUtils.drawScaledTexture(matrixStack, relX, relY, 0, this.imageWidth, this.imageHeight, 0.0F, 177.0F, 0.0F, 166.0F, 256.0F, 256.0F);
    }
    
    @OnlyIn(Dist.CLIENT)
    public static class ColorOptionsSlider extends AbstractSliderButton {
    	private final Consumer<Double> updateValue;
    	public ColorOptionsSlider(int x, int y, int width, int height, double value, Consumer<Double> updateValue) {
    		super(x, y, width, height, CommonComponents.EMPTY, value);
    		this.updateValue = updateValue;
    		this.updateMessage();
    	}

    	@Override
    	protected void updateMessage() {
    		Component component = Component.literal("" + (int)(this.value * 255.0F));
    		this.setMessage(component);
    	}

    	protected void applyValue() {
    		this.updateValue.accept(this.value);
    	}
    }
}
