package alec_wam.wam_utils.blocks.machine.auto_fisher;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.client.GuiUtils;
import alec_wam.wam_utils.client.widgets.RedstoneModeButton;
import alec_wam.wam_utils.server.network.MessageBlockEntityUpdate;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public class AutoFisherScreen extends AbstractContainerScreen<AutoFisherContainer> {

    private final ResourceLocation GUI = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/auto_fisher.png");
    private AutoFisherBE fisher;
    
    public AutoFisherScreen(AutoFisherContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        
        this.fisher = container.blockEntity;
    }

    @Override
    protected void init() {
    	super.init();
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        
        RedstoneModeButton redstoneButton = new RedstoneModeButton(this, relX + 154, relY + 6, 16, 16, fisher.getRedstoneMode()){

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
    	fisher.setRedstoneMode(mode);;
		CompoundTag message = new CompoundTag();
		message.putInt("RedstoneMode", mode.ordinal());
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(fisher, message));
	}
    
    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
        
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        int fishingRodX = relX + 72;
        int fishingRodY = relY + 14 + 8;
        boolean notEnoughEnergy = fisher.energyStorage.getEnergyStored() < fisher.getFishingCost();
        if(fisher.isFull() || !fisher.hasWater() || notEnoughEnergy) {
    		if(mouseX > fishingRodX && mouseX < fishingRodX + 30 && mouseY > fishingRodY && mouseY < fishingRodY + 30) {
            	List<Component> textComponents = new ArrayList<>();
            	if(fisher.isFull()) {
            		textComponents.add(Component.translatable("gui.wam_utils.warning.inventory_full"));
            	}
            	if(!fisher.hasWater()) {
            		textComponents.add(Component.translatable("gui.wam_utils.warning.auto_fisher.water"));
            	}
            	if(notEnoughEnergy) {
            		textComponents.add(Component.translatable("gui.wam_utils.warning.energy"));
            	}
            	Optional<TooltipComponent> tooltipComponent = java.util.Optional.empty();
            	this.renderTooltip(matrixStack, textComponents, tooltipComponent, mouseX, mouseY);
            }
    	}
        
        int energyBarX = relX + 16;
        int energyBarY = relY + 8;
        if(mouseX > energyBarX && mouseX < energyBarX + 16 && mouseY > energyBarY && mouseY < energyBarY + 68) {
        	List<Component> textComponents = new ArrayList<>();
        	String energyValue = String.format("%,d", fisher.energyStorage.getEnergyStored());
        	String maxEnergyValue = String.format("%,d", fisher.getEnergyCapacity());
        	textComponents.add(Component.literal(energyValue + " / " + maxEnergyValue));
        	textComponents.add(Component.translatable("gui.wam_utils.energy.cost", fisher.getFishingCost()));
        	Optional<TooltipComponent> tooltipComponent = java.util.Optional.empty();
        	this.renderTooltip(matrixStack, textComponents, tooltipComponent, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        //drawString(matrixStack, Minecraft.getInstance().font, "Energy: " + menu.getEnergy(), 10, 10, 0xffffff);
    }

    private static final int COLOR_GREY = Color.BLACK.brighter().getRGB();
    private static final int COLOR_WHITE = Color.WHITE.getRGB();
    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, GUI);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        this.blit(matrixStack, relX, relY, 0, 0, this.imageWidth, this.imageHeight); 
        
        int fishingRodX = relX + 72;
        int fishingRodY = relY + 20 + 32;
        
        ResourceLocation fishingRod = new ResourceLocation("minecraft:textures/item/fishing_rod.png");
        GuiUtils.drawSpriteInGUI(fishingRodX, fishingRodY, 0, 32, 32, COLOR_GREY, 0.5F, fishingRod);
		
		if(fisher.getFishingTime() > 0) {
			float fishingRatio = (float)(fisher.getFishingTime()) / (float)fisher.getMaxFishingTime();
	        float maxY = 1.0F - fishingRatio;
	        double height = fishingRatio * 32.0D;
	        GuiUtils.drawSpriteInGUI(fishingRodX, fishingRodY, 0, 32, height, COLOR_WHITE, 1.0F, fishingRod, 0, 1, 1, maxY);
		}
    	boolean hasEnoughEnergy = fisher.energyStorage.getEnergyStored() >= fisher.getFishingCost();
		
		if(fisher.isFull() || !fisher.hasWater() || !hasEnoughEnergy) {
			RenderSystem.setShaderTexture(0, GUI);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			this.blit(matrixStack, fishingRodX + 8, fishingRodY - 20, 176, 0, 30, 30); 
		}
        
        int energyBarX = relX + 16;
        int energyBarY = relY + 8;         
        float energyRatio = (float)(fisher.energyStorage.getEnergyStored()) / (float)fisher.getEnergyCapacity();
        int energyHeight = (int)(energyRatio * 68.0F);
        if(energyRatio > 0) {
        	Screen.fill(matrixStack, energyBarX + 1, energyBarY + 68 + 1, energyBarX + 1 + 16, energyBarY + 68 + 1 - energyHeight, Color.red.getRGB());
        }
    }
}
