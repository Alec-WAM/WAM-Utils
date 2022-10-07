package alec_wam.wam_utils.client.widgets;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.client.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ScaledCheckBox extends Checkbox {
	private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/checkbox.png");
	private final boolean showLabel;

	public ScaledCheckBox(int x, int y, int width, int height, Component component, boolean isChecked) {
		this(x, y, width, height, component, isChecked, true);
	}
	
	public ScaledCheckBox(int x, int y, int width, int height, Component component, boolean isChecked, boolean showLabel) {
		super(x, y, width, height, component, isChecked, true);
		this.showLabel = showLabel;
	}
	
	@Override
	public void renderButton(PoseStack p_93843_, int p_93844_, int p_93845_, float p_93846_) {
		Minecraft minecraft = Minecraft.getInstance();
		RenderSystem.setShaderTexture(0, TEXTURE);
		RenderSystem.enableDepthTest();
		Font font = minecraft.font;
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		float minX = this.isFocused() ? 20.0F : 0.0F;
		float minY = this.selected() ? 20.0F : 0.0F;
		GuiUtils.drawScaledTexture(p_93843_, x, y, 0, getWidth(), getHeight(), minX, minX + 20.0F, minY, minY + 20.0F, 64.0F, 64.0F);
		//blit(p_93843_, this.x, this.y, , , 20, this.height, 64, 64);
		this.renderBg(p_93843_, minecraft, p_93844_, p_93845_);
		if (this.showLabel) {
			p_93843_.pushPose();
			int maxSize = Math.max(this.width, this.height);
			float scale = (float)maxSize / 20.0F;
			float offsetX = 4.0F * scale;
			float offsetY = 8.0F * scale;
			p_93843_.translate(this.x + getWidth() + offsetX, this.y + (this.height - offsetY) / 2, 0);
			p_93843_.scale(scale, scale, 1.0F);
			drawString(p_93843_, font, this.getMessage(), 0, 0, 14737632 | Mth.ceil(this.alpha * 255.0F) << 24);
			p_93843_.popPose();
		}

	}

}
