package alec_wam.wam_utils.client.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.client.GuiUtils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RedstoneModeButton extends Button {

	private RedstoneMode redstoneMode;
	private Screen screen;
	
	public RedstoneModeButton(Screen screen, int x, int y, int width, int height, RedstoneMode mode) {
		this(screen, x, y, width, height, mode, NO_TOOLTIP);
	}
	
	public RedstoneModeButton(Screen screen, int x, int y, int width, int height, RedstoneMode mode, Button.OnTooltip tooltip) {
		super(x, y, width, height, CommonComponents.EMPTY, null, tooltip);
		this.screen = screen;
		this.redstoneMode = mode;
	}
	
	@Override
	public void onPress() {
		this.redstoneMode = redstoneMode.getNext();
	}
	
	public RedstoneMode getRestoneMode() {
		return redstoneMode;
	}
	
	@Override
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float p_94285_) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, GuiUtils.WIDGETS);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.enableDepthTest();
		float minX = 0.0F + (21 * redstoneMode.ordinal());
    	float maxX = 20.0F + (21 * redstoneMode.ordinal());
    	float minY = this.isHoveredOrFocused() ? 146.0F : 125.0F;
    	float maxY = this.isHoveredOrFocused() ? 166.0F : 145.0F;
    	GuiUtils.drawScaledTexture(poseStack, x, y, 0, getWidth(), getHeight(), minX, maxX, minY, maxY, 256.0F, 256.0F);
		if (this.isHovered) {
			if(this.onTooltip !=null && this.onTooltip != NO_TOOLTIP) {
				this.renderToolTip(poseStack, mouseX, mouseY);
			}
			else {
				this.renderRedstoneTooltip(poseStack, mouseX, mouseY);
			}
		}
	}

	public void renderRedstoneTooltip(PoseStack poseStack, int mouseX, int mouseY) {
		Component tooltip = Component.translatable("gui.wam_utils.tooltip.redstonemode." + this.redstoneMode.toString().toLowerCase());
		this.screen.renderTooltip(poseStack, tooltip, mouseX, mouseY);
	}

}
