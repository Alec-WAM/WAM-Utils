package alec_wam.wam_utils.blocks.machine.auto_farmer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.blocks.machine.BonemealMode;
import alec_wam.wam_utils.blocks.machine.SlotLock;
import alec_wam.wam_utils.client.GuiUtils;
import alec_wam.wam_utils.client.widgets.BonemealModeButton;
import alec_wam.wam_utils.client.widgets.LockButton;
import alec_wam.wam_utils.client.widgets.RedstoneModeButton;
import alec_wam.wam_utils.client.widgets.ScaledCheckBox;
import alec_wam.wam_utils.client.widgets.ScaledImageButton;
import alec_wam.wam_utils.server.network.MessageBlockEntityUpdate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class AutoFarmerScreen extends AbstractContainerScreen<AutoFarmerContainer> {

    private final ResourceLocation GUI = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/auto_farmer.png");
    private AutoFarmerBE farmer;
    
    private boolean settingsOpen = false;
    private int settingsWidth;
    private int settingsHeight;
    private int settingsRelX;
    private int settingsRelY;
    private int settingsPageIndex = PAGE_PLANT;
    private static final int PAGE_PLANT = 0;
    private static final int PAGE_HARVEST = 1;
    private static final int PAGE_GROW = 2;
    private ScaledCheckBox settingsRestockSeedsButton;
    private ScaledCheckBox settingsPushItemsButton;
    private ScaledCheckBox settingsPullItemsButton;
    private Button settingsPagePlantButton;
    private Button settingsPageHarvestButton;
    private Button settingsPageGrowButton;
    private Button settingsClearCropSettingsButton;
    private ItemStack hoverPlantItem = ItemStack.EMPTY;
    
    private List<ItemStack> ghostItems = new ArrayList<ItemStack>();
    
    public AutoFarmerScreen(AutoFarmerContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        this.width = imageWidth = 176;
        this.height = imageHeight = 208;
        
        this.farmer = container.blockEntity;
    }

    @Override
    protected void init() {
    	super.init();
    	
    	this.settingsWidth = 170;
        this.settingsHeight = 170;
        this.settingsRelX = (this.width - settingsWidth) / 2;
        this.settingsRelY = (this.height - settingsHeight) / 2;
        
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        int settingsButtonX = relX + 152;
        int settingsButtonY = relY + 6;
        
        RedstoneModeButton redstoneButton = new RedstoneModeButton(this, relX + 134, settingsButtonY, 16, 16, farmer.getRedstoneMode()){

			@Override
			public void onPress() {
				super.onPress();
				sendRedstoneModeUpdate(getRestoneMode());
			}
        	
        };
        this.addRenderableWidget(redstoneButton);
        
        
        BonemealModeButton bonemealModeButton = new BonemealModeButton(this, relX + 143, relY + 63, 16, 16, farmer.getBonemealMode()){

			@Override
			public void onPress() {
				super.onPress();
				sendBonemealModeUpdate(getBonemealMode());
			}
        	
        };
        this.addRenderableWidget(bonemealModeButton);
        
        ScaledImageButton settingsButton = new ScaledImageButton(settingsButtonX, settingsButtonY, 16, 16, Component.empty(), GuiUtils.WIDGETS, 0.0F, 0.0F, 20.0F, 20.0F, 21.0F){

			@Override
			public void onPress() {
				setSettingsOpen(true);
			}
        	
        };
        this.addRenderableWidget(settingsButton);
        
        int slotLockX = relX + 48;
    	int slotLockY = relY + 54;
        
    	for(int i = 0; i < AutoFarmerBE.INPUT_SLOTS; i++) {
    		final int index = i;
	        LockButton lockButton = new LockButton(slotLockX + (i * 18), slotLockY, 10, 10) {
	        	@Override
	        	public void onPress() {
	        		super.onPress();
	        		sendSlotLockUpdate(index, isLocked());
	        	}
	        };
	        SlotLock slotLock = farmer.getSlotLock(i);
	        lockButton.setLocked(slotLock !=null && slotLock.isEnabled());
	        this.addRenderableWidget(lockButton);
    	}
    	
    	this.settingsRestockSeedsButton = new ScaledCheckBox(settingsRelX + 5 + 50, settingsRelY + 5, 10, 10, Component.translatable("gui.wam_utils.auto_farmer.restock_seeds"), farmer.shouldRestockSeeds()){
        	
    		@Override
        	public void onPress() {
        		super.onPress();
        		sendRestockPushPullUpdate(this.selected(), farmer.shouldPullInSeeds(), farmer.shouldPushOutput());
        	}
        	
    		@Override
        	public void renderButton(PoseStack p_93843_, int p_93844_, int p_93845_, float p_93846_) {
        		p_93843_.pushPose();
        		p_93843_.translate(0, 0, 650.0F);
        		super.renderButton(p_93843_, p_93844_, p_93845_, p_93846_);
        		p_93843_.popPose();
        	}
        };
        this.settingsRestockSeedsButton.visible = this.settingsOpen;
        this.addRenderableWidget(settingsRestockSeedsButton);
        
    	this.settingsPullItemsButton = new ScaledCheckBox(settingsRelX + 5, settingsRelY + 5, 10, 10, Component.translatable("gui.wam_utils.auto_farmer.pull_seeds"), farmer.shouldPullInSeeds()){
        	
    		@Override
        	public void onPress() {
        		super.onPress();
        		sendRestockPushPullUpdate(farmer.shouldRestockSeeds(), this.selected(), farmer.shouldPushOutput());
        	}
        	
    		@Override
        	public void renderButton(PoseStack p_93843_, int p_93844_, int p_93845_, float p_93846_) {
        		p_93843_.pushPose();
        		p_93843_.translate(0, 0, 650.0F);
        		super.renderButton(p_93843_, p_93844_, p_93845_, p_93846_);
        		p_93843_.popPose();
        	}
        };
        this.settingsPullItemsButton.visible = this.settingsOpen;
        this.addRenderableWidget(settingsPullItemsButton);
        
    	this.settingsPushItemsButton = new ScaledCheckBox(settingsRelX + 5, settingsRelY + 17, 10, 10, Component.translatable("gui.wam_utils.auto_farmer.push_output"), farmer.shouldPushOutput()){
        	
    		@Override
        	public void onPress() {
        		super.onPress();
        		sendRestockPushPullUpdate(farmer.shouldRestockSeeds(), farmer.shouldPullInSeeds(), this.selected());
        	}
        	
    		@Override
        	public void renderButton(PoseStack p_93843_, int p_93844_, int p_93845_, float p_93846_) {
        		p_93843_.pushPose();
        		p_93843_.translate(0, 0, 650.0F);
        		super.renderButton(p_93843_, p_93844_, p_93845_, p_93846_);
        		p_93843_.popPose();
        	}
        };
        this.settingsPushItemsButton.visible = this.settingsOpen;
        this.addRenderableWidget(settingsPushItemsButton);
    	
        this.settingsPagePlantButton = new Button(settingsRelX + 5, settingsRelY + 30, 50, 20, Component.translatable("gui.wam_utils.auto_farmer.plant"), new Button.OnPress() {
			
			@Override
			public void onPress(Button p_93751_) {
				setSettingsPage(PAGE_PLANT);
			}
		}){
        	@Override
        	public void renderButton(PoseStack p_93843_, int p_93844_, int p_93845_, float p_93846_) {
        		p_93843_.pushPose();
        		p_93843_.translate(0, 0, 650.0F);
        		super.renderButton(p_93843_, p_93844_, p_93845_, p_93846_);
        		p_93843_.popPose();
        	}
        };
    	this.settingsPagePlantButton.active = this.settingsPageIndex != PAGE_PLANT;
        this.settingsPagePlantButton.visible = this.settingsOpen;
        this.addRenderableWidget(settingsPagePlantButton);
        
        this.settingsPageHarvestButton = new Button(settingsRelX + 5 + 50 + 5, settingsRelY + 30, 50, 20, Component.translatable("gui.wam_utils.auto_farmer.harvest"), new Button.OnPress() {
			
			@Override
			public void onPress(Button p_93751_) {
				setSettingsPage(PAGE_HARVEST);
			}
		}){
        	@Override
        	public void renderButton(PoseStack p_93843_, int p_93844_, int p_93845_, float p_93846_) {
        		p_93843_.pushPose();
        		p_93843_.translate(0, 0, 650.0F);
        		super.renderButton(p_93843_, p_93844_, p_93845_, p_93846_);
        		p_93843_.popPose();
        	}
        };
    	this.settingsPageHarvestButton.active = this.settingsPageIndex != PAGE_HARVEST;
        this.settingsPageHarvestButton.visible = this.settingsOpen;
        this.addRenderableWidget(settingsPageHarvestButton);
        
        this.settingsPageGrowButton = new Button(settingsRelX + 5 + 50 + 50 + 10, settingsRelY + 30, 50, 20, Component.translatable("gui.wam_utils.auto_farmer.grow"), new Button.OnPress() {
			
			@Override
			public void onPress(Button p_93751_) {
				setSettingsPage(PAGE_GROW);
			}
		}){
        	@Override
        	public void renderButton(PoseStack p_93843_, int p_93844_, int p_93845_, float p_93846_) {
        		p_93843_.pushPose();
        		p_93843_.translate(0, 0, 650.0F);
        		super.renderButton(p_93843_, p_93844_, p_93845_, p_93846_);
        		p_93843_.popPose();
        	}
        };
    	this.settingsPageGrowButton.active = this.settingsPageIndex != PAGE_GROW;
        this.settingsPageGrowButton.visible = this.settingsOpen;
        this.addRenderableWidget(settingsPageGrowButton);
        
        this.settingsClearCropSettingsButton = new ImageButton(settingsRelX + settingsWidth - 20, settingsRelY + settingsHeight - 20, 10, 10, 0, 83, 21, GuiUtils.WIDGETS, new Button.OnPress() {

			@Override
			public void onPress(Button p_93751_) {
				clickClearCropSettingsButton();
			}
        	
        }){
        	@Override
        	public void renderButton(PoseStack p_93843_, int p_93844_, int p_93845_, float p_93846_) {
        		p_93843_.pushPose();
        		p_93843_.translate(0, 0, 650.0F);
        		RenderSystem.setShader(GameRenderer::getPositionTexShader);
        		RenderSystem.setShaderTexture(0, GuiUtils.WIDGETS);
        		RenderSystem.enableDepthTest();
        		float minY = this.isHoveredOrFocused() ? 104.0F : 83.0F;
            	float maxY = this.isHoveredOrFocused() ? 124.0F : 103.0F;
            	GuiUtils.drawScaledTexture(p_93843_, x, y, 0, getWidth(), getHeight(), 0.0F, 20.0F, minY, maxY, 256.0F, 256.0F);
        		if (this.isHovered) {
        			this.renderToolTip(p_93843_, p_93844_, p_93845_);
        		}
        		p_93843_.popPose();
        	}
        };
        this.settingsClearCropSettingsButton.visible = this.settingsOpen;
        this.addRenderableWidget(settingsClearCropSettingsButton);
    }
    
    public void setSettingsOpen(boolean value) {
    	this.settingsOpen = value; 	
    	this.settingsPagePlantButton.visible = value;
    	this.settingsPageHarvestButton.visible = value;
    	this.settingsPageGrowButton.visible = value;
    	this.settingsClearCropSettingsButton.visible = value;
    	this.settingsRestockSeedsButton.visible = value;
    	this.settingsPullItemsButton.visible = value;
    	this.settingsPushItemsButton.visible = value;
    	
    	if(value) {        	
        	this.ghostItems = new ArrayList<ItemStack>();
        	farmer.getAllSeedItems().forEach((item) -> {
        		this.ghostItems.add(new ItemStack(item));
        	});
    	}
    }
    
    public void setSettingsPage(int page) {
    	this.settingsPageIndex = page;
    	this.settingsPagePlantButton.active = page != PAGE_PLANT;
    	this.settingsPageHarvestButton.active = page != PAGE_HARVEST;
    	this.settingsPageGrowButton.active = page != PAGE_GROW;    	
    	this.settingsClearCropSettingsButton.active = true;
    }
    
    public void clickClearCropSettingsButton() {
    	if(this.settingsPageIndex == PAGE_PLANT) {
    		this.sendCropSettingsClearUpdate(0);
    	}
    	if(this.settingsPageIndex == PAGE_HARVEST) {
    		this.sendCropSettingsClearUpdate(2);
    	}
    	if(this.settingsPageIndex == PAGE_GROW) {
    		this.sendCropSettingsClearUpdate(3);
    	}
    	this.settingsClearCropSettingsButton.active = false;
    }
    
    public void sendRedstoneModeUpdate(RedstoneMode mode)
	{
    	farmer.setRedstoneMode(mode);;
		CompoundTag message = new CompoundTag();
		message.putInt("RedstoneMode", mode.ordinal());
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(farmer, message));
	}
    
    public void sendBonemealModeUpdate(BonemealMode mode)
	{
    	farmer.setBonemealMode(mode);
		CompoundTag message = new CompoundTag();
		message.putInt("BonemealMode", mode.ordinal());
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(farmer, message));
	}
    
    public void sendSlotLockUpdate(int slot, boolean locked)
	{
    	farmer.updateSlotLock(slot, locked);
		CompoundTag message = new CompoundTag();
		message.putInt("Slot", slot);
		message.putBoolean("Enabled", locked);
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(farmer, message));
	}
    
    public void sendCropSettingsUpdate(CropSettings settings)
	{
    	farmer.updateCropSetting(settings.getPos(), settings);
		CompoundTag message = new CompoundTag();
		message.put("CropSettings", settings.serializeNBT());
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(farmer, message));
		this.settingsClearCropSettingsButton.active = true;
	}
    
    public void sendCropSettingsClearUpdate(int type)
	{
    	farmer.disabledCropSettings(type);
		CompoundTag message = new CompoundTag();
		message.putBoolean("Clear", true);
		message.putInt("Type", type);
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(farmer, message));
	}
    
    public void sendRestockPushPullUpdate(boolean restock, boolean pull, boolean push)
	{
    	farmer.setRestockSeeds(restock);
    	farmer.setPullInSeeds(pull);
    	farmer.setPushOutput(push);
		CompoundTag message = new CompoundTag();
		message.putBoolean("RestockSeeds", restock);
		message.putBoolean("PullSeeds", pull);
		message.putBoolean("PushOutput", push);
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(farmer, message));
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
            
            int range = farmer.getRange() + 1;
            int boxSize = 100 / range;
            
            int gridSize = boxSize * range;
            int gridX = settingsRelX + (settingsWidth - gridSize) / 2;
            int gridY = settingsRelY + 60;
        	int minRange = -(farmer.getRange() / 2);
        	double spacing = 8.0D / farmer.getRange();
        	for(int y = 0; y < range; y++) {
        		for(int x = 0; x < range; x++) {
	            	int renderX = gridX + (x * boxSize);
	            	int renderY = gridY + (y * boxSize);
	            	int color = Color.GRAY.getRGB();
	            	
	            	int offsetX = minRange + x;
	            	int offsetY = minRange + y;
	            	Vec3i pos = new Vec3i(offsetX, 0, offsetY);
	            	CropSettings settings = this.farmer.getCropSettings(pos);
	            	
	            	if(settings !=null) {
		            	if(this.settingsPageIndex == PAGE_PLANT) {
		            		if(!settings.shouldPlant()) {
		            			color = Color.RED.getRGB();
		            		}
		            		else {
		            			color = Color.GREEN.getRGB();
		            		}
		            	}
		            	
		            	if(this.settingsPageIndex == PAGE_HARVEST) {
		            		if(!settings.shouldHarvest()) {
		            			color = Color.RED.getRGB();
		            		}
		            		else {
		            			color = Color.GREEN.getRGB();
		            		}
		            	}
		            	
		            	if(this.settingsPageIndex == PAGE_GROW) {
		            		if(!settings.shouldGrow()) {
		            			color = Color.RED.getRGB();
		            		}
		            		else {
		            			color = Color.GREEN.getRGB();
		            		}
		            	}
	            	}
	            	if(x == range / 2 && y == range / 2) {
	            		color = Color.BLACK.getRGB();
	            	}
	            	
	            	GuiUtils.fill(matrixStack, renderX + spacing, renderY + spacing, renderX + boxSize - spacing, renderY + boxSize - spacing, color, 650.0F);
	            	//if(settingsPageIndex == PAGE_PLANT) {
	            		if(settings !=null && !settings.getSeed().isEmpty()) {
	            			GuiUtils.renderGuiItem(this.itemRenderer, settings.getSeed(), renderX + spacing, renderY + spacing, 650, (float)(boxSize-(spacing * 2)));
	            		}
	            	//}
            	}
            }
        	boolean hoverPlant = false;
        	if(settingsPageIndex == PAGE_PLANT) {
        		int ghostItemListWidth = 10;
            	int fixedSize = Math.min(8, ghostItems.size());
	        	int ghostItemListHeight = fixedSize * 12;
	        	int ghostItemListX = settingsRelX + 14;
	        	int ghostItemListY = gridY + ((gridSize - ghostItemListHeight) / 2);
	        	
	        	for(int i = 0; i < fixedSize; i++) {
	        		ItemStack seed = this.ghostItems.get(i);
		        	int slotX = ghostItemListX;
		        	int slotY = ghostItemListY + (i * 12);
		        	int color = Color.GRAY.getRGB();
		        	GuiUtils.fill(matrixStack, slotX, slotY, slotX + 10, slotY + 10, color, 650.0F);
		        	GuiUtils.renderGuiItem(this.itemRenderer, seed, slotX, slotY, 650, 10.0F);
	        	}
	        	
	        	if(this.hoverPlantItem !=null && !this.hoverPlantItem.isEmpty()) {
	        		hoverPlant = true;
	        		matrixStack.pushPose();
	        		float offset = 8.0F / 10.0F;
		        	GuiUtils.renderGuiItem(this.itemRenderer, hoverPlantItem, (int)(mouseX - offset), (int)(mouseY - offset), 650, 10.0F);
	        		matrixStack.popPose();
	        	}       	
	        	
	        	
	        	if(mouseX > ghostItemListX && mouseX < ghostItemListX + ghostItemListWidth && mouseY > ghostItemListY && mouseY < ghostItemListY + ghostItemListHeight) {
	        		double ghostMouseY = mouseY - ghostItemListY;
	            	int ghostSlot = (int) (ghostMouseY / 12);
	            	ItemStack seed = this.ghostItems.get(ghostSlot);
	            	if(!seed.isEmpty()) {        			
	        			matrixStack.pushPose();
	        			matrixStack.translate(0, 0, 400.0D);
	        			this.renderTooltip(matrixStack, seed, mouseX, mouseY);
	        			matrixStack.popPose();
	        		}
	        	}
        	}
        	if(!hoverPlant && mouseX > gridX && mouseX < gridX + gridSize && mouseY > gridY && mouseY < gridY + gridSize) {
        		double gridMouseX = mouseX - gridX;
            	double gridMouseY = mouseY - gridY;
            	int gridBoxX = (int) (gridMouseX / boxSize);
            	int gridBoxY = (int) (gridMouseY / boxSize);
            	int offsetX = minRange + gridBoxX;
            	int offsetY = minRange + gridBoxY;
            	Vec3i pos = new Vec3i(offsetX, 0, offsetY);
            	CropSettings settings = this.farmer.getCropSettings(pos);
            	if(settings !=null) {
            		if(!settings.getSeed().isEmpty()) {
            			
            			List<Component> itemToolTip = this.getTooltipFromItem(settings.getSeed());
            			if(settingsPageIndex == PAGE_PLANT) {
            				itemToolTip.add(Component.translatable("gui.wam_utils.auto_farmer.seed_right_click").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            			}
            			Optional<TooltipComponent> image = settings.getSeed().getTooltipImage();
            			
            			matrixStack.pushPose();
            			matrixStack.translate(0, 0, 400.0D);
            			this.renderTooltip(matrixStack, itemToolTip, image, mouseX, mouseY);
            			matrixStack.popPose();
            		}
            	}
        	}
        	
        	if(!hoverPlant && mouseX > gridX && mouseX < gridX + gridSize && mouseY > gridY && mouseY < gridY + gridSize) {
        		double gridMouseX = mouseX - gridX;
            	double gridMouseY = mouseY - gridY;
            	int gridBoxX = (int) (gridMouseX / boxSize);
            	int gridBoxY = (int) (gridMouseY / boxSize);
            	int offsetX = minRange + gridBoxX;
            	int offsetY = minRange + gridBoxY;
            	Vec3i pos = new Vec3i(offsetX, 0, offsetY);
            	CropSettings settings = this.farmer.getCropSettings(pos);
            	if(settings !=null) {
            		if(!settings.getSeed().isEmpty()) {
            			
            			List<Component> itemToolTip = this.getTooltipFromItem(settings.getSeed());
            			if(settingsPageIndex == PAGE_PLANT) {
            				itemToolTip.add(Component.translatable("gui.wam_utils.auto_farmer.seed_right_click").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            			}
            			Optional<TooltipComponent> image = settings.getSeed().getTooltipImage();
            			
            			matrixStack.pushPose();
            			matrixStack.translate(0, 0, 400.0D);
            			this.renderTooltip(matrixStack, itemToolTip, image, mouseX, mouseY);
            			matrixStack.popPose();
            		}
            	}
        	}
        
        }
        
        if(!settingsOpen) {        	
        	for(int p = 0; p < 2; p++) {
	        	if(p == 0) {
	        		RenderSystem.setShaderTexture(0, GUI);	                
	        	}
        		for(int i = 0; i < AutoFarmerBE.INPUT_SLOTS; i++) {
	        		int slotLockX = relX + 45 + (i * 18);
		        	int slotLockY = relY + 34;
		        	SlotLock lock = farmer.getSlotLock(i);
		        	if(lock != null) {
		        		if(lock.isEnabled() && farmer.seedItems.getStackInSlot(i).isEmpty()) {
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
	        	String energyValue = String.format("%,d", farmer.energyStorage.getEnergyStored());
	        	String maxEnergyValue = String.format("%,d", farmer.getEnergyCapacity());
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
            	int gridY = 60;
            	int range = farmer.getRange() + 1;
                int boxSize = 100 / range;
                int gridSize = boxSize * range;
            	int ghostItemListWidth = 10;
            	int fixedSize = Math.min(8, ghostItems.size());
	        	int ghostItemListHeight = fixedSize * 12;
	        	int ghostItemListX = 14;
	        	int ghostItemListY = gridY + ((gridSize - ghostItemListHeight) / 2);
	        	
	        	if(mouseFixedX > ghostItemListX && mouseFixedX < ghostItemListX + ghostItemListWidth && mouseFixedY > ghostItemListY && mouseFixedY < ghostItemListY + ghostItemListHeight) {

                	double ghostMouseY = mouseFixedY - ghostItemListY;
                	int ghostSlot = (int) (ghostMouseY / 12);
                	ItemStack ghostItem = this.ghostItems.get(ghostSlot);
                	if(!ghostItem.isEmpty()) {
                		this.hoverPlantItem = ghostItem.copy();
                	}
                	//WAMUtilsMod.LOGGER.debug("GHOST ITEMS: " + ghostSlot);
                	return true;
	        	}
	        	
	        	if(clickInGrid(mouseFixedX, mouseFixedY, button, false)) {
	        		return true;
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double p_97755_, double p_97756_) {
    	/*if(this.settingsOpen) {
    		double mouseFixedX = mouseX - settingsRelX;
            double mouseFixedY = mouseY - settingsRelY;
            if(clickInGrid(mouseFixedX, mouseFixedY, button, true)) {
            	return true;
            }
    	}*/
    	return super.mouseDragged(mouseX, mouseY, button, p_97755_, p_97756_);
    }
    
    public boolean clickInGrid(double mouseFixedX, double mouseFixedY, int mouseButton, boolean isDragging) {    	
    	//Grid
    	int range = farmer.getRange() + 1;
        int boxSize = 100 / range;
        int gridSize = boxSize * range;
        int gridX = (settingsWidth - gridSize) / 2;
        int gridY = 60;
    	if(mouseFixedX > gridX && mouseFixedX < gridX + gridSize && mouseFixedY > gridY && mouseFixedY < gridY + gridSize) {
        	double gridMouseX = mouseFixedX - gridX;
        	double gridMouseY = mouseFixedY - gridY;
        	int gridBoxX = (int) (gridMouseX / boxSize);
        	int gridBoxY = (int) (gridMouseY / boxSize);
        	
        	if(gridBoxX == range / 2 && gridBoxY == range / 2) {
        		return true;
        	}   
        	
        	int minRange = -(farmer.getRange() / 2);
        	int offsetX = minRange + gridBoxX;
        	int offsetY = minRange + gridBoxY;
        	Vec3i pos = new Vec3i(offsetX, 0, offsetY);
        	CropSettings settings = this.farmer.getCropSettings(pos);
        	
        	if(settings !=null) {
            	boolean updated = false;
        		if(this.settingsPageIndex == PAGE_PLANT) {
            		if(mouseButton == 1) {
            			//RightClick
            			if(!settings.getSeed().isEmpty()) {
            				settings.setSeed(ItemStack.EMPTY);
            				updated = true;
            			}
            		}
            		if(mouseButton == 0) {
	            		if(!this.hoverPlantItem.isEmpty()) {
	            			settings.setSeed(hoverPlantItem.copy());
		            		updated = true;
	            		}
	            		else {
	            			settings.setShouldPlant(!settings.shouldPlant());
		            		updated = true;
	            		}
            		}
            	}
            	
            	if(this.settingsPageIndex == PAGE_HARVEST) {
            		settings.setShouldHarvest(!settings.shouldHarvest());
            		updated = true;
            	}
            	
            	if(this.settingsPageIndex == PAGE_GROW) {
            		settings.setShouldGrow(!settings.shouldGrow());
            		updated = true;
            	}
            	if(updated)sendCropSettingsUpdate(settings);
        	}
        	
        	//WAMUtilsMod.LOGGER.debug("GRID: " + gridBoxX + " " + gridBoxY);
        	return true;
        }
        else if(!this.hoverPlantItem.isEmpty() && !isDragging){
        	this.hoverPlantItem = ItemStack.EMPTY;
        	return true;
        }
    	return false;
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
    	//super.renderLabels(matrixStack, mouseX, mouseY);
        //drawString(matrixStack, Minecraft.getInstance().font, "Energy: " + menu.getEnergy(), 10, 10, 0xffffff);
    	if(settingsOpen) {
    		matrixStack.pushPose();
            matrixStack.translate(-this.getGuiLeft(), -this.getGuiTop(), 650.0F);            
            this.font.draw(matrixStack, Component.literal("N"), settingsRelX + (settingsWidth / 2) - 3, settingsRelY + 52, 0x000000);
            matrixStack.popPose();
    	}
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, GUI);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        this.blit(matrixStack, relX, relY, 0, 0, this.imageWidth, this.imageHeight); 
        
        if(!this.settingsOpen) {
        	
//        	RenderSystem.setShaderTexture(0, GuiUtils.WIDGETS);
//        	int lockButtonSize = 10;
//        	for(int i = 0; i < 5; i++) {
//	        	int slotLockX = relX + 48 + (i * 18);
//	        	int slotLockY = relY + 54;
//	        	boolean isLocked = false;
//	        	boolean isHovering = mouseX > slotLockX && mouseX < slotLockX + lockButtonSize && mouseY > slotLockY && mouseY < slotLockY + lockButtonSize;
//	        	float minX = isLocked ? 0.0F : 20.0F;
//	        	float maxX = isLocked ? 20.0F : 40.0F;
//	        	float minY = isHovering ? 62.0F : 42.0F;
//	        	float maxY = isHovering ? 82.0F : 62.0F;
//	        	GuiUtils.drawScaledTexture(matrixStack, slotLockX, slotLockY, 0, lockButtonSize, lockButtonSize, minX, maxX, minY, maxY, 256.0F, 256.0F);
//        	}
        }
        
        int energyBarX = relX + 16;
        int energyBarY = relY + 8;         
        float energyRatio = (float)(farmer.energyStorage.getEnergyStored()) / (float)farmer.getEnergyCapacity();
        int energyHeight = (int)(energyRatio * 68.0F);
        if(energyRatio > 0) {
        	Screen.fill(matrixStack, energyBarX + 1, energyBarY + 68 + 1, energyBarX + 1 + 16, energyBarY + 68 + 1 - energyHeight, Color.red.getRGB());
        }       
    }
}
