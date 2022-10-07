package alec_wam.wam_utils.blocks.machine.auto_animal_farmer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.client.GuiIcons;
import alec_wam.wam_utils.client.GuiUtils;
import alec_wam.wam_utils.client.widgets.GuiIconToggleButton;
import alec_wam.wam_utils.client.widgets.RedstoneModeButton;
import alec_wam.wam_utils.server.network.MessageBlockEntityUpdate;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.fluids.FluidStack;

public class AutoAnimalFarmerScreen extends AbstractContainerScreen<AutoAnimalFarmerContainer> {

    private final ResourceLocation GUI = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/auto_animal_farmer.png");

    private AutoAnimalFarmerBE milker;
    
    public AutoAnimalFarmerScreen(AutoAnimalFarmerContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        
        this.imageWidth = 176;
        this.imageHeight = 212;
        this.milker = container.blockEntity;
    }

    @Override
    public void init() {
    	super.init();
    	int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        
        GuiIconToggleButton boundingBoxButton = new GuiIconToggleButton(relX + 138, relY + 6, 16, 16, GuiIcons.BUTTON_BOUNDINGBOX_OFF, GuiIcons.BUTTON_BOUNDINGBOX_ON, milker.showBoundingBox()){

			@Override
			public void onPress() {
				super.onPress();
				sendBoundingBoxUpdate(isSelected());
			}
			
			//TODO Add tooltip
        	
        };
        this.addRenderableWidget(boundingBoxButton);
        
    	RedstoneModeButton redstoneButton = new RedstoneModeButton(this, relX + 156, relY + 6, 16, 16, milker.getRedstoneMode()){

			@Override
			public void onPress() {
				super.onPress();
				sendRedstoneModeUpdate(getRestoneMode());
			}
        	
        };
        this.addRenderableWidget(redstoneButton);
    }
    
    public void sendRedstoneModeUpdate(RedstoneMode mode)
	{
    	milker.setRedstoneMode(mode);;
		CompoundTag message = new CompoundTag();
		message.putInt("RedstoneMode", mode.ordinal());
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(milker, message));
	}
    
    public void sendBoundingBoxUpdate(boolean value)
	{
    	milker.setShowBoundingBox(value);
		CompoundTag message = new CompoundTag();
		message.putBoolean("ShowBoundingBox", value);
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(milker, message));
	}
    
    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
        
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        int tankX = relX + 80;
    	int tankY = relY + 9;
        if(mouseX >= tankX && mouseX <= tankX + 16 && mouseY >= tankY && mouseY <= tankY + 68) {
        	List<Component> textComponents = new ArrayList<>();
        	
        	FluidStack fluid = milker.fluidStorage.getFluid();        	
        	if(!fluid.isEmpty()) {
    			textComponents.add(fluid.getDisplayName());
    		}
        	String fluidValue = String.format("%,d", milker.fluidStorage.getFluidAmount());
        	String maxFluidValue = String.format("%,d", milker.fluidStorage.getCapacity());
    		textComponents.add(Component.literal(fluidValue + " / " + maxFluidValue + " mB"));
        	Optional<TooltipComponent> tooltipComponent = java.util.Optional.empty();
        	this.renderTooltip(matrixStack, textComponents, tooltipComponent, mouseX, mouseY);
        }
        
        int energyBarX = relX + 35;
        int energyBarY = relY + 9;
        if(mouseX > energyBarX && mouseX < energyBarX + 16 && mouseY > energyBarY && mouseY < energyBarY + 68) {
        	List<Component> textComponents = new ArrayList<>();
        	String energyValue = String.format("%,d", milker.energyStorage.getEnergyStored());
        	String maxEnergyValue = String.format("%,d", milker.getEnergyCapacity());
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
    	
        int energyBarX = relX + 35;
        int energyBarY = relY + 9;
        float energyRatio = (float)(milker.energyStorage.getEnergyStored()) / (float)milker.getEnergyCapacity();
        int energyHeight = (int)(energyRatio * 68.0F);
        if(energyRatio > 0) {
        	Screen.fill(matrixStack, energyBarX, energyBarY + 68, energyBarX + 16, energyBarY + 68 - energyHeight, Color.red.getRGB());
        }
        
    	int tankX = relX + 80;
    	int tankY = relY + 9;        
        float fluidRatio = (float)(milker.fluidStorage.getFluidAmount()) / (float)milker.fluidStorage.getCapacity();
        int tankFluidHeight = (int)(fluidRatio * 68.0F);
        if(milker.fluidStorage.getFluidAmount() > 0) {
        	FluidStack fluid = milker.fluidStorage.getFluid();
        	//GuiUtils.drawFluid(fluid, tankX, tankY + 68, 0, 16, tankFluidHeight);        	
        	matrixStack.pushPose();
        	MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        	GuiUtils.drawRepeatedFluidSprite(matrixStack, fluid, tankX, tankY + 68 - tankFluidHeight, 16, tankFluidHeight);
        	buffer.endBatch();
        	matrixStack.popPose();
        }
    }
}
