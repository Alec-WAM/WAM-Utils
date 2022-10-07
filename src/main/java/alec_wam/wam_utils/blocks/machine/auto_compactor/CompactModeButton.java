package alec_wam.wam_utils.blocks.machine.auto_compactor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.blocks.machine.auto_compactor.AutoCompactorBE.CompactingMode;
import alec_wam.wam_utils.client.GuiUtils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CompactModeButton extends Button {

	private CompactingMode mode;
	private Screen screen;
	
	public CompactModeButton(Screen screen, int x, int y, int width, int height, CompactingMode mode) {
		this(screen, x, y, width, height, mode, NO_TOOLTIP);
	}
	
	public CompactModeButton(Screen screen, int x, int y, int width, int height, CompactingMode mode, Button.OnTooltip tooltip) {
		super(x, y, width, height, CommonComponents.EMPTY, null, tooltip);
		this.screen = screen;
		this.mode = mode;
	}
	
	@Override
	public void onPress() {
		this.mode = mode.getNext();
	}
	
	public CompactingMode getCompactingMode() {
		return mode;
	}
	
	@Override
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float p_94285_) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, GuiUtils.WIDGETS);
		RenderSystem.enableDepthTest();
		float minX = 0.0F + (21 * mode.ordinal());
    	float maxX = 20.0F + (21 * mode.ordinal());
    	float minY = this.isHoveredOrFocused() ? 230.0F : 209.0F;
    	float maxY = this.isHoveredOrFocused() ? 250.0F : 229.0F;
    	GuiUtils.drawScaledTexture(poseStack, x, y, 0, getWidth(), getHeight(), minX, maxX, minY, maxY, 256.0F, 256.0F);
		if (this.isHovered) {
			if(this.onTooltip !=null && this.onTooltip != NO_TOOLTIP) {
				this.renderToolTip(poseStack, mouseX, mouseY);
			}
			else {
				this.renderCustomTooltip(poseStack, mouseX, mouseY);
			}
		}
	}

	public void renderCustomTooltip(PoseStack poseStack, int mouseX, int mouseY) {
		Component tooltip = Component.translatable("gui.wam_utils.tooltip.compactingmode." + this.mode.toString().toLowerCase());
		this.screen.renderTooltip(poseStack, tooltip, mouseX, mouseY);
	}

}
