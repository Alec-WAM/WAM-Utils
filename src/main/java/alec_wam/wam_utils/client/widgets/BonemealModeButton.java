package alec_wam.wam_utils.client.widgets;

import java.util.List;
import java.util.Optional;

import org.apache.commons.compress.utils.Lists;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.blocks.machine.BonemealMode;
import alec_wam.wam_utils.client.GuiUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BonemealModeButton extends Button {

	private BonemealMode bonemealMode;
	private Screen screen;
	
	public BonemealModeButton(Screen screen, int x, int y, int width, int height, BonemealMode mode) {
		this(screen, x, y, width, height, mode, NO_TOOLTIP);
	}
	
	public BonemealModeButton(Screen screen, int x, int y, int width, int height, BonemealMode mode, Button.OnTooltip tooltip) {
		super(x, y, width, height, CommonComponents.EMPTY, null, tooltip);
		this.screen = screen;
		this.bonemealMode = mode;
	}
	
	@Override
	public void onPress() {
		this.bonemealMode = bonemealMode.getNext();
	}
	
	public BonemealMode getBonemealMode() {
		return bonemealMode;
	}
	
	@Override
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float p_94285_) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, GuiUtils.WIDGETS);
		RenderSystem.enableDepthTest();
		float minX = 0.0F + (21 * bonemealMode.ordinal());
    	float maxX = 20.0F + (21 * bonemealMode.ordinal());
    	float minY = this.isHoveredOrFocused() ? 188.0F : 167.0F;
    	float maxY = this.isHoveredOrFocused() ? 208.0F : 187.0F;
    	GuiUtils.drawScaledTexture(poseStack, x, y, 0, getWidth(), getHeight(), minX, maxX, minY, maxY, 256.0F, 256.0F);
		if (this.isHovered) {
			if(this.onTooltip !=null && this.onTooltip != NO_TOOLTIP) {
				this.renderToolTip(poseStack, mouseX, mouseY);
			}
			else {
				this.renderBonemealTooltip(poseStack, mouseX, mouseY);
			}
		}
	}

	public void renderBonemealTooltip(PoseStack poseStack, int mouseX, int mouseY) {
		List<Component> tooltips = Lists.newArrayList();
		tooltips.add(Component.translatable("gui.wam_utils.tooltip.bonemealmode." + this.bonemealMode.toString().toLowerCase()));
		if(getBonemealMode() == BonemealMode.ADVANCED) {
			tooltips.add(Component.translatable("gui.wam_utils.tooltip.bonemealmode." + this.bonemealMode.toString().toLowerCase() + ".tooltip").withStyle(ChatFormatting.ITALIC));			
		}
		this.screen.renderTooltip(poseStack, tooltips, Optional.empty(), mouseX, mouseY);
	}

}
