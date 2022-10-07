package alec_wam.wam_utils.blocks.machine.auto_compactor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.blocks.machine.auto_compactor.AutoCompactorBE.CompactingMode;
import alec_wam.wam_utils.client.GuiUtils;
import alec_wam.wam_utils.client.widgets.RedstoneModeButton;
import alec_wam.wam_utils.client.widgets.ScaledToggleButton;
import alec_wam.wam_utils.server.network.MessageBlockEntityUpdate;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public class AutoCompactorScreen extends AbstractContainerScreen<AutoCompactorContainer> {

    private final ResourceLocation GUI = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/auto_compactor.png");

    private AutoCompactorBE compactor;
    
    public AutoCompactorScreen(AutoCompactorContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        
        this.compactor = container.blockEntity;
    }

    @Override
    protected void init() {
    	super.init();
    	
    	int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        
        RedstoneModeButton redstoneButton = new RedstoneModeButton(this, relX + 151, relY + 5, 20, 20, compactor.getRedstoneMode()){

			@Override
			public void onPress() {
				super.onPress();
				sendRedstoneModeUpdate(getRestoneMode());
			}
        	
        };
        this.addRenderableWidget(redstoneButton);
        
        CompactModeButton craftingMode = new CompactModeButton(this, relX + 130, relY + 5, 20, 20, compactor.getCompactingMode()){

			@Override
			public void onPress() {
				super.onPress();
				sendCompactingModeUpdate(getCompactingMode());
			}
        	
        };
        this.addRenderableWidget(craftingMode);
    }
    
    public void sendRedstoneModeUpdate(RedstoneMode mode)
	{
    	compactor.setRedstoneMode(mode);;
		CompoundTag message = new CompoundTag();
		message.putInt("RedstoneMode", mode.ordinal());
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(compactor, message));
	}
    
    public void sendCompactingModeUpdate(CompactingMode value)
	{
    	compactor.setCompactingMode(value);
		CompoundTag message = new CompoundTag();
		message.putInt("CompactingMode", value.ordinal());
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(compactor, message));
	}
    
    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
        
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        int energyBarX = relX + 8;
        int energyBarY = relY + 8;
        if(mouseX > energyBarX && mouseX < energyBarX + 16 && mouseY > energyBarY && mouseY < energyBarY + 68) {
        	List<Component> textComponents = new ArrayList<>();
        	String energyValue = String.format("%,d", compactor.energyStorage.getEnergyStored());
        	String maxEnergyValue = String.format("%,d", compactor.getEnergyCapacity());
        	textComponents.add(Component.literal(energyValue + " / " + maxEnergyValue));
        	Optional<TooltipComponent> tooltipComponent = java.util.Optional.empty();
        	this.renderTooltip(matrixStack, textComponents, tooltipComponent, mouseX, mouseY);
        }
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
        
    	int craftingProgressX = relX + 95;
    	int craftingProgressY = relY + 34;        
    	float craftingProgress = (float)(compactor.getCraftingTime()) / (float)compactor.getMaxCraftingTime();
        int craftingWidth = (int)(craftingProgress * 24.0F);
        if(craftingProgress > 0.0F) {
        	this.blit(matrixStack, craftingProgressX, craftingProgressY, 176, 0, craftingWidth, 17);
        }
        
        int energyBarX = relX + 8;
        int energyBarY = relY + 8;
        float energyRatio = (float)(compactor.energyStorage.getEnergyStored()) / (float)compactor.getEnergyCapacity();
        int energyHeight = (int)(energyRatio * 68.0F);
        if(energyRatio > 0) {
        	Screen.fill(matrixStack, energyBarX, energyBarY + 68, energyBarX + 16, energyBarY + 68 - energyHeight, Color.red.getRGB());
        }
    }
}
