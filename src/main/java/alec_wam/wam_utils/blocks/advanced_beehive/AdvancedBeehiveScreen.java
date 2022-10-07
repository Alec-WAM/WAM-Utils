package alec_wam.wam_utils.blocks.advanced_beehive;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.client.GuiUtils;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;

public class AdvancedBeehiveScreen extends AbstractContainerScreen<AdvancedBeehiveContainer> {

    private final ResourceLocation GUI = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/advanced_beehive.png");

    private AdvancedBeehiveBE hive;
    
    public AdvancedBeehiveScreen(AdvancedBeehiveContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        this.width = imageWidth = 176;
        this.height = imageHeight = 185;
        
        this.hive = container.blockEntity;
    }
    
    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
        
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        int honeycombX = relX + 72;
    	int honeycombY = relY + 9;
    	if(mouseX > honeycombX && mouseX < honeycombX + 32 && mouseY > honeycombY && mouseY < honeycombY + 32) {
    		int honeyLevel = BeehiveBlockEntity.getHoneyLevel(hive.getBlockState());
    		float honeyRatio = (float)(honeyLevel) / (float)BeehiveBlock.MAX_HONEY_LEVELS;
    		int honeyPercentage = (int)(honeyRatio * 100.0F);
    		List<Component> textComponents = new ArrayList<>();
    		textComponents.add(Component.translatable("gui.wam_utils.advanced_beehive.honey", ""+honeyPercentage));
    		Optional<TooltipComponent> tooltipComponent = java.util.Optional.empty();
    		this.renderTooltip(matrixStack, textComponents, tooltipComponent, mouseX, mouseY);
    	}
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        //drawString(matrixStack, Minecraft.getInstance().font, "Energy: " + menu.getEnergy(), 10, 10, 0xffffff);
    	int beeCount = hive.clientBeeCount;
    	drawCenteredString(matrixStack, font, Component.translatable("gui.wam_utils.advanced_beehive.bees", ""+beeCount), 148, 10, 0xffffff);
    }

    private static final int COLOR_GREY = Color.BLACK.brighter().getRGB();
    private static final int COLOR_WHITE = Color.WHITE.getRGB();
    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, GUI);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        this.blit(matrixStack, relX, relY, 0, 0, this.imageWidth, this.imageHeight); 
        
        int honeycombX = relX + 72;
        int honeycombY = relY + 32 + 9;   
        ResourceLocation honeycomb = new ResourceLocation("minecraft:textures/item/honeycomb.png");
        GuiUtils.drawSpriteInGUI(honeycombX, honeycombY, 0, 32, 32, COLOR_GREY, 0.3F, honeycomb);
        
        int honeyLevel = BeehiveBlockEntity.getHoneyLevel(hive.getBlockState());
        if(honeyLevel > 0) {
			float honeyRatio = (float)(honeyLevel) / (float)BeehiveBlock.MAX_HONEY_LEVELS;
	        float maxY = 1.0F - honeyRatio;
	        double height = honeyRatio * 32.0D;
	        GuiUtils.drawSpriteInGUI(honeycombX, honeycombY, 0, 32, height, COLOR_WHITE, 1.0F, honeycomb, 0, 1, 1, maxY);
		}
    }
}
