package alec_wam.wam_utils.blocks.advanced_spawner;

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

public class AdvancedSpawnerScreen extends AbstractContainerScreen<AdvancedSpawnerContainer> {

    private final ResourceLocation GUI = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/advanced_spawner.png");

    private AdvancedSpawnerBE spawner;
    private float xMouse;
    private float yMouse;
    
    public AdvancedSpawnerScreen(AdvancedSpawnerContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        this.width = imageWidth = 176;
        this.height = imageHeight = 208;
        
        this.spawner = container.blockEntity;
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
        int killProgressX = relX + 79 - 5;
    	int killProgressY = relY + 56 - 5;
        if(mouseX > killProgressX && mouseX < killProgressX + 25 && mouseY > killProgressY && mouseY < killProgressY + 25) {
        	List<Component> textComponents = new ArrayList<>();
        	textComponents.add(Component.literal(spawner.getKillProgress() + " / " + spawner.getMaxKillTime()));
        	Optional<TooltipComponent> tooltipComponent = java.util.Optional.empty();
        	this.renderTooltip(matrixStack, textComponents, tooltipComponent, mouseX, mouseY);
        }
        
        int energyBarX = relX + 34;
        int energyBarY = relY + 6;
        if(mouseX > energyBarX && mouseX < energyBarX + 16 && mouseY > energyBarY && mouseY < energyBarY + 68) {
        	List<Component> textComponents = new ArrayList<>();
        	String energyValue = String.format("%,d", spawner.energyStorage.getEnergyStored());
        	String maxEnergyValue = String.format("%,d", spawner.getEnergyCapacity());
        	String costPerTick = String.format("%,d", spawner.getDamageCost());
        	textComponents.add(Component.literal(energyValue + " / " + maxEnergyValue));
        	textComponents.add(Component.translatable("gui.wam_utils.energy.cost", costPerTick));
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

        
        int energyBarX = relX + 34;
        int energyBarY = relY + 6;        
        this.blit(matrixStack, energyBarX, energyBarY, 176, 49, 18, 80);        
        float energyRatio = (float)(spawner.energyStorage.getEnergyStored()) / (float)spawner.getEnergyCapacity();
        int energyHeight = (int)(energyRatio * 68.0F);
        if(energyRatio > 0) {
        	Screen.fill(matrixStack, energyBarX + 1, energyBarY + 68 + 1, energyBarX + 1 + 16, energyBarY + 68 + 1 - energyHeight, Color.red.getRGB());
        }
        
        if(spawner.clientRenderEntity !=null) {
        	EntityDimensions dim = spawner.clientRenderEntity.getDimensions(Pose.STANDING);
        	float sizeHeight = 17.0F * (2.0f / dim.height);
        	float sizeWidth = 8.0F * (2.0f / dim.width);
        	int size = (int)(Math.min(sizeWidth, sizeHeight));
        	int renderX = relX + 72 + 15;
        	int renderY = relY + 7 + 40;
        	InventoryScreen.renderEntityInInventory(renderX, renderY, size, (float)(renderX) - this.xMouse, (float)(renderY - 25) - this.yMouse, spawner.clientRenderEntity);
        }
        
        
        RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
        float killProgress = (float)spawner.getKillProgress() / (float)spawner.getMaxKillTime();
        int progressHeight = (int)(killProgress * 19.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int killProgressX = relX + 79;
    	int killProgressY = relY + 56;
        this.blit(matrixStack, killProgressX, killProgressY, 0, 94, 18, 18);
        if(killProgress > 0.0F) {
        	this.blit(matrixStack, killProgressX, killProgressY + 18 - progressHeight, 18, 112 - progressHeight, 18, progressHeight);
        }
    }
}
