package alec_wam.wam_utils.blocks.machine.enchantment_remover;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.client.GuiIcons;
import alec_wam.wam_utils.client.widgets.RedstoneModeButton;
import alec_wam.wam_utils.server.network.MessageBlockEntityUpdate;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public class EnchantmentRemoverScreen extends AbstractContainerScreen<EnchantmentRemoverContainer> {

    private final ResourceLocation GUI = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/enchantment_remover.png");

    private EnchantmentRemoverBE remover;
    
    public EnchantmentRemoverScreen(EnchantmentRemoverContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        
        this.remover = container.blockEntity;
    }

    @Override
    protected void init() {
    	super.init();
    	
    	int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        
        RedstoneModeButton redstoneButton = new RedstoneModeButton(this, relX + 152, relY + 6, 16, 16, remover.getRedstoneMode()){

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
    	remover.setRedstoneMode(mode);;
		CompoundTag message = new CompoundTag();
		message.putInt("RedstoneMode", mode.ordinal());
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(remover, message));
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
        	String energyValue = String.format("%,d", remover.energyStorage.getEnergyStored());
        	String maxEnergyValue = String.format("%,d", remover.getEnergyCapacity());
        	textComponents.add(Component.literal(energyValue + " / " + maxEnergyValue));
        	Optional<TooltipComponent> tooltipComponent = java.util.Optional.empty();
        	this.renderTooltip(matrixStack, textComponents, tooltipComponent, mouseX, mouseY);
        }
        
        int fullWarningX = relX + 56;
        int fullWarningY = relY + 36;
        if(mouseX > fullWarningX && mouseX < fullWarningX + 16 && mouseY > fullWarningY && mouseY < fullWarningY + 16) {
        	if(remover.isFull()) {
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
        
    	int craftingProgressX = relX + 54;
    	int craftingProgressY = relY + 35;        
    	float craftingProgress = (float)(remover.getRemovalProgress()) / (float)remover.getMaxRemovalProgress();
        int craftingWidth = (int)(craftingProgress * 24.0F);
        if(craftingProgress > 0.0F) {
        	this.blit(matrixStack, craftingProgressX, craftingProgressY, 176, 0, craftingWidth, 17);
        }
        
        if(remover.isFull()) {
        	GuiIcons.ICON_WARNING.renderIcon(matrixStack, relX + 56, relY + 36, 0.0, 16, 16);
        }
        
        int energyBarX = relX + 8;
        int energyBarY = relY + 8;
        float energyRatio = (float)(remover.energyStorage.getEnergyStored()) / (float)remover.getEnergyCapacity();
        int energyHeight = (int)(energyRatio * 68.0F);
        if(energyRatio > 0) {
        	Screen.fill(matrixStack, energyBarX, energyBarY + 68, energyBarX + 16, energyBarY + 68 - energyHeight, Color.red.getRGB());
        }
    }
}
