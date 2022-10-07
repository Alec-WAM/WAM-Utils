package alec_wam.wam_utils.client.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.client.GuiIcons;
import alec_wam.wam_utils.client.GuiUtils;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiIconButton extends AbstractButton {
	
	private GuiIcons icon;
	
	public GuiIconButton(int x, int y, int width, int height, GuiIcons icon) {
		this(x, y, width, height, Component.empty(), icon);
	}
	
	public GuiIconButton(int x, int y, int width, int height, Component component, GuiIcons icon) {
		super(x, y, width, height, component);
		this.icon = icon;
	}
	
	@Override
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float p_93846_) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, GuiUtils.WIDGETS);
		RenderSystem.enableDepthTest();
		float minX = 21.0F;
    	float maxX = 41.0F;
    	float minY = this.isHoveredOrFocused() ? 21.0F : 0.0F;
    	float maxY = this.isHoveredOrFocused() ? 41.0F : 20.0F;
    	GuiUtils.drawScaledTexture(poseStack, x, y, 0, getWidth(), getHeight(), minX, maxX, minY, maxY, 256.0F, 256.0F);
    	getIcon().renderIcon(poseStack, x + 1, y + 1, 0, getWidth() - 2, getHeight() - 2);
		if (this.isHovered) {
			this.renderTooltip(poseStack, mouseX, mouseY);
		}		
	}
	
	public GuiIcons getIcon() {
		return icon;
	}

	protected void renderTooltip(PoseStack poseStack, int mouseX, int mouseY) {
		
	}

	@Override
	public void updateNarration(NarrationElementOutput p_169152_) {
		
	}

	@Override
	public void onPress() {
		
	}

}
