package alec_wam.wam_utils.client.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.client.GuiUtils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LockButton extends Button {

	private boolean isLocked = false;
	
	public LockButton(int x, int y, int width, int height) {
		super(x, y, width, height, CommonComponents.EMPTY, null);
	}
	
	public LockButton(int x, int y, int width, int height, Button.OnTooltip tooltip) {
		super(x, y, width, height, CommonComponents.EMPTY, null, tooltip);
	}
	
	@Override
	public void onPress() {
		this.isLocked = !isLocked;
	}
	
	public boolean isLocked() {
		return isLocked;
	}
	
	public void setLocked(boolean locked) {
		this.isLocked = locked;
	}
	
	@Override
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float p_94285_) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, GuiUtils.WIDGETS);
		RenderSystem.enableDepthTest();
		float minX = isLocked ? 0.0F : 20.0F;
    	float maxX = isLocked ? 20.0F : 40.0F;
    	float minY = this.isHoveredOrFocused() ? 62.0F : 42.0F;
    	float maxY = this.isHoveredOrFocused() ? 82.0F : 62.0F;
    	GuiUtils.drawScaledTexture(poseStack, x, y, 0, getWidth(), getHeight(), minX, maxX, minY, maxY, 256.0F, 256.0F);
		if (this.isHovered) {
			this.renderToolTip(poseStack, mouseX, mouseY);
		}
	}

}
