package alec_wam.wam_utils.client.widgets;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.client.GuiUtils;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ScaledImageButton extends AbstractButton {
	private final ResourceLocation texture;
	private float textureX;
	private float textureY;
	private float textureHoverY;
	private float textureWidth;
	private float textureHeight;
	private Button.OnPress onPress;
	
	public ScaledImageButton(int x, int y, int width, int height, Component component, ResourceLocation texture, float textureX, float textureY, float textureWidth, float textureHeight, float textureHoverY) {
		super(x, y, width, height, component);
		this.texture = texture;
		this.textureHoverY = textureHoverY;
		this.textureX = textureX;
		this.textureY = textureY;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
	}
	
	@Override
	public void renderButton(PoseStack p_93843_, int p_93844_, int p_93845_, float p_93846_) {
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.enableDepthTest();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		float minX = textureX;
		float minY = this.isHoveredOrFocused() ? textureHoverY : textureY;
		GuiUtils.drawScaledTexture(p_93843_, x, y, 0, getWidth(), getHeight(), minX, minX + textureWidth, minY, minY + textureHeight, 256.0F, 256.0F);
	}

	@Override
	public void updateNarration(NarrationElementOutput p_169152_) {
		
	}

	@Override
	public void onPress() {
		
	}

}
