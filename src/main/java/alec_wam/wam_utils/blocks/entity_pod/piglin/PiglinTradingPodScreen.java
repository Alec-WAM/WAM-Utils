package alec_wam.wam_utils.blocks.entity_pod.piglin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.WAMUtilsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public class PiglinTradingPodScreen extends AbstractContainerScreen<PiglinTradingPodContainer> {

    private final ResourceLocation GUI = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/piglin_trading_pod.png");

    private PiglinTradingPodBE pod;
    private float xMouse;
    private float yMouse;
    private float lookX;
    private float lookY;
    private boolean playedJealousSound = false;
    
    public PiglinTradingPodScreen(PiglinTradingPodContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        this.width = imageWidth = 176;
        this.height = imageHeight = 185;
        
        this.pod = container.blockEntity;
    }

    @Override
    public void containerTick() {
    	super.containerTick();
    	
    	boolean notTrading = pod.getTradeTime() <= 0;
    	//WAMUtilsMod.LOGGER.debug("Test " + pod.getTradeTime());
    	if(!this.menu.getCarried().isEmpty() && this.menu.getCarried().isPiglinCurrency() && notTrading) {
    		int relX = (this.width - this.imageWidth) / 2;
            int relY = (this.height - this.imageHeight) / 2;
    		int renderX = relX + 107 + 15;
        	int renderY = relY + 11 + 40;
    		lookX = (float)(renderX) - this.xMouse;
    		lookY = (float)(renderY - 25) - this.yMouse;
    		
    		if(!playedJealousSound) {
    			float pitch = pod.getTradeEntity() !=null ? pod.getTradeEntity().getVoicePitch() : 1.0F;
    			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PIGLIN_JEALOUS, pitch, 0.25F));
    			playedJealousSound = true;
    		}
    	}    	
    	else {
    		this.lookX = Mth.lerp(lookX, 0.0F, 0.5F);
    		this.lookY = Mth.lerp(lookY, 0.0F, 0.5F);
			playedJealousSound = false;
    	}
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
        int fullOutputX = relX + 77;
    	int fullOutputY = relY + 24;
    	if(pod.isFull()) {
    		if(mouseX > fullOutputX && mouseX < fullOutputX + 24 && mouseY > fullOutputY && mouseY < fullOutputY + 17) {
            	List<Component> textComponents = new ArrayList<>();
            	textComponents.add(Component.translatable("gui.wam_utils.warning.inventory_full"));
            	Optional<TooltipComponent> tooltipComponent = java.util.Optional.empty();
            	this.renderTooltip(matrixStack, textComponents, tooltipComponent, mouseX, mouseY);
            }
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
        
        int tradeProgressX = relX + 76;
    	int tradeProgressY = relY + 23;
    	float tradeProgress = (float)(pod.getTradeTime()) / (float)pod.getMaxTradeTime();
        int tradeWidth = (int)(tradeProgress * 24.0F);
        if(tradeProgress > 0.0F) {
        	this.blit(matrixStack, tradeProgressX, tradeProgressY, 176, 0, tradeWidth, 17);
        }
        
        int fullOutputX = relX + 77;
    	int fullOutputY = relY + 24;
    	if(pod.isFull()) {
    		this.blit(matrixStack, fullOutputX, fullOutputY, 176, 17, 22, 15);
    	}
        
        if(pod.getTradeEntity() !=null) {
        	EntityDimensions dim = pod.getTradeEntity().getDimensions(Pose.STANDING);
        	float sizeHeight = 17.0F * (2.0f / dim.height);
        	float sizeWidth = 8.0F * (2.0f / dim.width);
        	int size = (int)(Math.min(sizeWidth, sizeHeight));
        	int renderX = relX + 107 + 15;
        	int renderY = relY + 11 + 40;
        	
        	InventoryScreen.renderEntityInInventory(renderX, renderY, size, lookX, lookY, pod.getTradeEntity());
        }     
    }
}
