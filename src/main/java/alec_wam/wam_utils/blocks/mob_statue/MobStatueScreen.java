package alec_wam.wam_utils.blocks.mob_statue;

import java.util.function.Consumer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.server.network.MessageBlockEntityUpdate;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class MobStatueScreen extends AbstractContainerScreen<MobStatueContainer> {

    private final ResourceLocation GUI = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/mob_statue.png");

    private MobStatueBE statue;
    
    public MobStatueScreen(MobStatueContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.statue = container.blockEntity;
    }

    @Override
    protected void init() {
    	super.init();
    	int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
    	
    	double rotationValue = (double)this.statue.getRotation() / 360.0D;
        this.addRenderableWidget(new DisplayOptionsSlider(relX + 9, relY + 77, 64, 20, rotationValue, (value -> {
        	double newValue = value * 360.0D;
        	this.statue.setRotation((int)(newValue));
        	sendDisplayOption("Rotation", newValue);
        })));
        
        double scaleValue = (double)this.statue.getScale();
        this.addRenderableWidget(new DisplayOptionsSlider(relX + 104, relY + 77, 64, 20, scaleValue, (value -> {
        	double newValue = value;
        	this.statue.setScale((float)(newValue));
        	sendDisplayOption("Scale", newValue);
        })));
    }
    
    public void sendDisplayOption(String key, double value)
	{
		CompoundTag message = new CompoundTag();
		message.putDouble(key, value);
		WAMUtilsMod.packetHandler.sendToServer(new MessageBlockEntityUpdate(statue, message));
	}
    
    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
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
    }
    
    @OnlyIn(Dist.CLIENT)
    public static class DisplayOptionsSlider extends AbstractSliderButton {
    	private final Consumer<Double> updateValue;
    	public DisplayOptionsSlider(int x, int y, int width, int height, double value, Consumer<Double> updateValue) {
    		super(x, y, width, height, CommonComponents.EMPTY, value);
    		this.updateValue = updateValue;
    		this.updateMessage();
    	}

    	@Override
    	protected void updateMessage() {
    		Component component = (Component)((float)this.value == (float)this.getYImage(false) ? CommonComponents.OPTION_OFF : Component.literal((int)(this.value * 100.0D) + "%"));
    		this.setMessage(component);
    	}

    	protected void applyValue() {
    		this.updateValue.accept(this.value);
    	}
    }
}
