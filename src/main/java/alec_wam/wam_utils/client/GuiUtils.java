package alec_wam.wam_utils.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;

import alec_wam.wam_utils.WAMUtilsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

public class GuiUtils {
	public static final ResourceLocation WIDGETS = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/gui_widgets.png");
	public static final ResourceLocation BLANK_GUI = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/blank_gui.png");
	
	public static final ResourceLocation EMPTY_SLOT_BOOK = new ResourceLocation(WAMUtilsMod.MODID, "items/empty_slot_book");
    
	public static void drawFluid(FluidStack fluidStack, int x, int y, int z, int width, int height) {
		IClientFluidTypeExtensions fluidExtensions = IClientFluidTypeExtensions.of(fluidStack.getFluid());
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidExtensions.getStillTexture());
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder buffer = tessellator.getBuilder();
		
		int col = fluidExtensions.getTintColor(fluidStack);
		
		float red = (col>>16&255)/255.0f;
		float green = (col>>8&255)/255.0f;
		float blue = (col&255)/255.0f;
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
		RenderSystem.setShaderColor(red, green, blue, 1.0F);

		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.vertex(x, y, z).uv(sprite.getU0(), sprite.getV0()).endVertex();
		buffer.vertex(x + width, y, z).uv(sprite.getU1(), sprite.getV0()).endVertex();
		buffer.vertex(x + width, y - height, z).uv(sprite.getU1(), sprite.getV1()).endVertex();
		buffer.vertex(x, y - height, z).uv(sprite.getU0(), sprite.getV1()).endVertex();
		tessellator.end();
	}
	
	public static void drawRepeatedFluidSprite(PoseStack transform, FluidStack fluid, float x, float y, float w, float h)
	{
		IClientFluidTypeExtensions props = IClientFluidTypeExtensions.of(fluid.getFluid());
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(props.getStillTexture());
		int col = props.getTintColor(fluid);
		int iW = sprite.getWidth();
		int iH = sprite.getHeight();
		if(iW > 0&&iH > 0)
			drawRepeatedSprite(transform, x, y, w, h, iW, iH,
					sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(),
					(col>>16&255)/255.0f, (col>>8&255)/255.0f, (col&255)/255.0f, 1);
	}
	
	public static void drawRepeatedSprite(PoseStack transform, float x, float y, float w,
			float h, int iconWidth, int iconHeight, float uMin, float uMax, float vMin, float vMax, float r, float g,
			float b, float alpha) {
		int iterMaxW = (int) (w / iconWidth);
		int iterMaxH = (int) (h / iconHeight);
		float leftoverW = w % iconWidth;
		float leftoverH = h % iconHeight;
		float leftoverWf = leftoverW / (float) iconWidth;
		float leftoverHf = leftoverH / (float) iconHeight;
		float iconUDif = uMax - uMin;
		float iconVDif = vMax - vMin;
		for (int ww = 0; ww < iterMaxW; ww++) {
			for (int hh = 0; hh < iterMaxH; hh++)
				drawTexturedColoredRect(transform, x + ww * iconWidth, y + hh * iconHeight, iconWidth,
						iconHeight, r, g, b, alpha, uMin, uMax, vMin, vMax);
			drawTexturedColoredRect(transform, x + ww * iconWidth, y + iterMaxH * iconHeight, iconWidth,
					leftoverH, r, g, b, alpha, uMin, uMax, vMin, (vMin + iconVDif * leftoverHf));
		}
		if (leftoverW > 0) {
			for (int hh = 0; hh < iterMaxH; hh++)
				drawTexturedColoredRect(transform, x + iterMaxW * iconWidth, y + hh * iconHeight, leftoverW,
						iconHeight, r, g, b, alpha, uMin, (uMin + iconUDif * leftoverWf), vMin, vMax);
			drawTexturedColoredRect(transform, x + iterMaxW * iconWidth, y + iterMaxH * iconHeight, leftoverW,
					leftoverH, r, g, b, alpha, uMin, (uMin + iconUDif * leftoverWf), vMin,
					(vMin + iconVDif * leftoverHf));
		}
	}
	
	public static void drawTexturedColoredRect(PoseStack transform,
			float x, float y, float w, float h,
			float r, float g, float b, float alpha,
			float u0, float u1, float v0, float v1
	)
	{
//		TransformingVertexBuilder innerBuilder = new TransformingVertexBuilder(builder, transform, DefaultVertexFormat.BLOCK);
//		innerBuilder.defaultColor((int)(255*r), (int)(255*g), (int)(255*b), (int)(255*alpha));
//		innerBuilder.setLight(LightTexture.pack(15, 15));
//		innerBuilder.setOverlay(OverlayTexture.NO_OVERLAY);
//		innerBuilder.setNormal(1, 1, 1);
//		innerBuilder.vertex(x, y+h, 0).uv(u0, v1).endVertex();
//		innerBuilder.vertex(x+w, y+h, 0).uv(u1, v1).endVertex();
//		innerBuilder.vertex(x+w, y, 0).uv(u1, v0).endVertex();
//		innerBuilder.vertex(x, y, 0).uv(u0, v0).endVertex();
//		innerBuilder.unsetDefaultColor();
		
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder buffer = tessellator.getBuilder();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
		RenderSystem.setShaderColor(r, g, b, alpha);

		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.vertex(x, y+h, 0).uv(u0, v1).endVertex();
		buffer.vertex(x+w, y+h, 0).uv(u1, v1).endVertex();
		buffer.vertex(x+w, y, 0).uv(u1, v0).endVertex();
		buffer.vertex(x, y, 0).uv(u0, v0).endVertex();
		tessellator.end();
	}
	
	public static void drawSpriteInGUI(double x, double y, double z, double width, double height, int color, float alpha, ResourceLocation itemTexture) {
		drawSpriteInGUI(x, y, z, width, height, color, alpha, itemTexture, 0, 1, 1, 0);
	}
	
	public static void drawSpriteInGUI(double x, double y, double z, double width, double height, int color, float alpha, ResourceLocation itemTexture, float u0, float u1, float v0, float v1) {
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder buffer = tessellator.getBuilder();		
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, itemTexture);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		buffer.vertex(x, y, z).uv(u0, v0).color(color).endVertex();
		buffer.vertex(x + width, y, z).uv(u1, v0).color(color).endVertex();
		buffer.vertex(x + width, y - height, z).uv(u1, v1).color(color).endVertex();
		buffer.vertex(x, y - height, z).uv(u0, v1).color(color).endVertex();
		tessellator.end();
	}
	
	public static void drawScaledTexture(PoseStack matrixStack, double x, double y, double z, double width, double height, float textureMaxWidth, float textureMaxHeight) {
        drawScaledTexture(matrixStack, x, y, z, width, height, 0, textureMaxWidth, 0, textureMaxHeight, 256.0F, 256.0F);
	}
	public static void drawScaledTexture(PoseStack matrixStack, double x, double y, double z, double width, double height, float textureMinWidth, float textureMaxWidth, float textureMinHeight, float textureMaxHeight, float imageWidth, float imageHeight) {
		float u0 = textureMinWidth / imageWidth;
        float u1 = textureMaxWidth / imageWidth;
        float v0 = textureMinHeight / imageHeight;
        float v1 = textureMaxHeight / imageHeight;
        
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(matrixStack.last().pose(),(float)x, (float)(y + height), (float)z).uv(u0, v1).endVertex();
        bufferbuilder.vertex(matrixStack.last().pose(),(float)(x + width), (float)(y + height), (float)z).uv(u1, v1).endVertex();
        bufferbuilder.vertex(matrixStack.last().pose(),(float)(x + width), (float)y, (float)z).uv(u1, v0).endVertex();
        bufferbuilder.vertex(matrixStack.last().pose(),(float)x, (float)y, (float)z).uv(u0, v0).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());
	}
	
	public static void fill(PoseStack p_93173_, double p_93174_, double p_93175_, double p_93176_, double p_93177_, int p_93178_, float z) {
		innerFill(p_93173_.last().pose(), p_93174_, p_93175_, p_93176_, p_93177_, p_93178_, z);
	}

	private static void innerFill(Matrix4f p_93106_, double p_93107_, double p_93108_, double p_93109_, double p_93110_, int p_93111_, float zLevel) {
		if (p_93107_ < p_93109_) {
			double i = p_93107_;
			p_93107_ = p_93109_;
			p_93109_ = i;
		}

		if (p_93108_ < p_93110_) {
			double j = p_93108_;
			p_93108_ = p_93110_;
			p_93110_ = j;
		}

		float f3 = (float)(p_93111_ >> 24 & 255) / 255.0F;
		float f = (float)(p_93111_ >> 16 & 255) / 255.0F;
		float f1 = (float)(p_93111_ >> 8 & 255) / 255.0F;
		float f2 = (float)(p_93111_ & 255) / 255.0F;
		BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		bufferbuilder.vertex(p_93106_, (float)p_93107_, (float)p_93110_, zLevel).color(f, f1, f2, f3).endVertex();
		bufferbuilder.vertex(p_93106_, (float)p_93109_, (float)p_93110_, zLevel).color(f, f1, f2, f3).endVertex();
		bufferbuilder.vertex(p_93106_, (float)p_93109_, (float)p_93108_, zLevel).color(f, f1, f2, f3).endVertex();
		bufferbuilder.vertex(p_93106_, (float)p_93107_, (float)p_93108_, zLevel).color(f, f1, f2, f3).endVertex();
		BufferUploader.drawWithShader(bufferbuilder.end());
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
	}


	public static void renderGuiItem(ItemRenderer itemRenderer, ItemStack stack, double x, double y, double z, float scale) {
		renderGuiItem(itemRenderer, stack, x, y, z, scale, itemRenderer.getModel(stack, (Level)null, (LivingEntity)null, 0));
	}
	
	public static void renderGuiItem(ItemRenderer itemRenderer, ItemStack stack, double x, double y, double z, float scale, BakedModel model) {
	      Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, false);
	      RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
	      RenderSystem.enableBlend();
	      RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
	      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	      PoseStack posestack = RenderSystem.getModelViewStack();
	      posestack.pushPose();
	      posestack.translate((double)x, (double)y, (double)(100.0F + z));
	      posestack.translate(scale * 0.5D, scale * 0.5D, 0.0D);
	      posestack.scale(1.0F, -1.0F, 1.0F);
	      posestack.scale(scale, scale, scale);
	      RenderSystem.applyModelViewMatrix();
	      PoseStack posestack1 = new PoseStack();
	      MultiBufferSource.BufferSource multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
	      boolean flag = !model.usesBlockLight();
	      if (flag) {
	         Lighting.setupForFlatItems();
	      }

	      itemRenderer.render(stack, ItemTransforms.TransformType.GUI, false, posestack1, multibuffersource$buffersource, 15728880, OverlayTexture.NO_OVERLAY, model);
	      multibuffersource$buffersource.endBatch();
	      RenderSystem.enableDepthTest();
	      if (flag) {
	         Lighting.setupFor3DItems();
	      }

	      posestack.popPose();
	      RenderSystem.applyModelViewMatrix();
	   }
	
}
