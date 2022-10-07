package alec_wam.wam_utils.blocks.generator.furnace;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.WAMUtilsMod;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;

public class FurnaceGeneratorScreen extends AbstractContainerScreen<FurnaceGeneratorContainer> {

    private final ResourceLocation GUI = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/furnace_generator.png");

    private FurnaceGeneratorBE generator;
    private float xMouse;
    private float yMouse;
    
    public FurnaceGeneratorScreen(FurnaceGeneratorContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        
        this.generator = container.blockEntity;
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        this.xMouse = (float)mouseX;
        this.yMouse = (float)mouseY;
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
        
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        int fuelProgressX = relX + 45;
    	int fuelProgressY = relY + 26;
        if(mouseX >= fuelProgressX && mouseX <= fuelProgressX + 14 && mouseY >= fuelProgressY && mouseY <= fuelProgressY + 14) {
        	List<Component> textComponents = new ArrayList<>();
        	
        	if(generator.getFuelAmount() > 0) {
        		String fuelValue = String.format("%,d", generator.getFuelAmount());
            	String maxFuelValue = String.format("%,d", generator.getMaxFuelAmount());
        		textComponents.add(Component.literal(fuelValue + " / " + maxFuelValue));
        	}
        	else {
        		textComponents.add(Component.translatable("gui.wam_utils.furnace_generator.fuelEmpty"));
        	}
        	Optional<TooltipComponent> tooltipComponent = java.util.Optional.empty();
        	this.renderTooltip(matrixStack, textComponents, tooltipComponent, mouseX, mouseY);
        }
        
        int energyBarX = relX + 80;
        int energyBarY = relY + 9;
        if(mouseX > energyBarX && mouseX < energyBarX + 16 && mouseY > energyBarY && mouseY < energyBarY + 68) {
        	List<Component> textComponents = new ArrayList<>();
        	String energyValue = String.format("%,d", generator.energyStorage.getEnergyStored());
        	String maxEnergyValue = String.format("%,d", generator.getEnergyCapacity());
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
    	
        int energyBarX = relX + 80;
        int energyBarY = relY + 9;
        float energyRatio = (float)(generator.energyStorage.getEnergyStored()) / (float)generator.getEnergyCapacity();
        int energyHeight = (int)(energyRatio * 68.0F);
        if(energyRatio > 0) {
        	Screen.fill(matrixStack, energyBarX, energyBarY + 68, energyBarX + 16, energyBarY + 68 - energyHeight, Color.red.getRGB());
        }
        
    	int fuelProgressX = relX + 45;
    	int fuelProgressY = relY + 26;        
        float fuelProgress = (float)(generator.getMaxFuelAmount() - generator.getFuelAmount()) / (float)generator.getMaxFuelAmount();
        int progressHeight = (int)(fuelProgress * 14.0F);
        if(generator.getFuelAmount() > 0) {
        	this.blit(matrixStack, fuelProgressX, fuelProgressY + progressHeight, 176, 0 + progressHeight, 14, 14 - progressHeight);
        }
    }
}
