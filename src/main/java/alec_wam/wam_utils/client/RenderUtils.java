package alec_wam.wam_utils.client;

import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

public class RenderUtils {

	@SuppressWarnings("deprecation")
	public static void renderFluid(float scale, int color, VertexConsumer vertexBuilder, Fluid fluidToRender, Set<Direction> dirs, int brightness, boolean tankAbove, boolean tankBelow, PoseStack matrixStack) {

        float offset = -0.002f;
        ResourceLocation stillTexture = IClientFluidTypeExtensions.of(fluidToRender).getStillTexture();
        TextureAtlasSprite fluid = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(stillTexture);

        float u1 = fluid.getU0();
        float v1 = fluid.getV0();
        float u2 = fluid.getU1();
        float v2 = fluid.getV1();
        float edge = 1.0f / 16f;
        float edgeBottom = tankBelow ? 0.0F : edge;

        //int b1 = brightness >> 16 & 65535;
        //int b2 = brightness & 65535;
        int b1 = brightness & '\uffff';
        int b2 = brightness >> 16 & '\uffff';

//        vertexBuilder.setColorRGBA_I(color, 255);
//        vertexBuilder.setBrightness(brightness);
//        vertexBuilder.setMatrix(matrixStack.getLast().getMatrix());
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = (color >> 24 & 0xFF) / 255.0F;

        Matrix4f matrix = matrixStack.last().pose();

        if (scale > 0.0f) {
            //TOP
        	if (dirs.contains(Direction.UP)) {
        		vt(vertexBuilder, matrix, 0, scale + offset, 0, u1, v1, b1, b2, r, g, b, a);
        		vt(vertexBuilder, matrix, 0, scale + offset, 1, u1, v2, b1, b2, r, g, b, a);
        		vt(vertexBuilder, matrix, 1, scale + offset, 1, u2, v2, b1, b2, r, g, b, a);
        		vt(vertexBuilder, matrix, 1, scale + offset, 0, u2, v1, b1, b2, r, g, b, a);
        	}
        	
        	if (dirs.contains(Direction.DOWN)) {
        		vt(vertexBuilder, matrix, 0, -offset, 1, u1, v1, b1, b2, r, g, b, a);
        		vt(vertexBuilder, matrix, 0, -offset, 0, u1, v2, b1, b2, r, g, b, a);
        		vt(vertexBuilder, matrix, 1, -offset, 0, u2, v2, b1, b2, r, g, b, a);
        		vt(vertexBuilder, matrix, 1, -offset, 1, u2, v1, b1, b2, r, g, b, a);
        	}

            if (scale > edge) {
            	float scaleTop = scale;
            	if(!tankAbove) {
            		if (scale > 1 - edge) {
            			scaleTop = 1 - edge;
                    }
            	}
                /*if (scale > 1 - edge) {
                    scale = 1 - edge;
                }*/

                v2 -= (fluid.getV1() - fluid.getV0()) * (1 - scale);

                if (dirs.contains(Direction.NORTH)) {
                    vt(vertexBuilder, matrix, 1 - edge, scaleTop, -offset, u1, v1, b1, b2, r, g, b, a);
                    vt(vertexBuilder, matrix, 1 - edge, edgeBottom, -offset, u1, v2, b1, b2, r, g, b, a);
                    vt(vertexBuilder, matrix, edge, edgeBottom, -offset, u2, v2, b1, b2, r, g, b, a);
                    vt(vertexBuilder, matrix, edge, scaleTop, -offset, u2, v1, b1, b2, r, g, b, a);
                }

                if (dirs.contains(Direction.WEST)) {
                    vt(vertexBuilder, matrix, -offset, edgeBottom, 1 - edge, u1, v2, b1, b2, r, g, b, a);
                    vt(vertexBuilder, matrix, -offset, scaleTop, 1 - edge, u1, v1, b1, b2, r, g, b, a);
                    vt(vertexBuilder, matrix, -offset, scaleTop, edge, u2, v1, b1, b2, r, g, b, a);
                    vt(vertexBuilder, matrix, -offset, edgeBottom, edge, u2, v2, b1, b2, r, g, b, a);
                }

                if (dirs.contains(Direction.SOUTH)) {
                    vt(vertexBuilder, matrix, 1 - edge, edgeBottom, 1 + offset, u1, v2, b1, b2, r, g, b, a);
                    vt(vertexBuilder, matrix, 1 - edge, scaleTop, 1 + offset, u1, v1, b1, b2, r, g, b, a);
                    vt(vertexBuilder, matrix, edge, scaleTop, 1 + offset, u2, v1, b1, b2, r, g, b, a);
                    vt(vertexBuilder, matrix, edge, edgeBottom, 1 + offset, u2, v2, b1, b2, r, g, b, a);
                }

                if (dirs.contains(Direction.EAST)) {
                    vt(vertexBuilder, matrix, 1 + offset, scaleTop, 1 - edge, u1, v1, b1, b2, r, g, b, a);
                    vt(vertexBuilder, matrix, 1 + offset, edgeBottom, 1 - edge, u1, v2, b1, b2, r, g, b, a);
                    vt(vertexBuilder, matrix, 1 + offset, edgeBottom, edge, u2, v2, b1, b2, r, g, b, a);
                    vt(vertexBuilder, matrix, 1 + offset, scaleTop, edge, u2, v1, b1, b2, r, g, b, a);
                }
            }
        }
    }

	public static void renderFakeFluid(ResourceLocation texture, float min, float scale, float max, Vector3f color, float alpha, VertexConsumer vertexBuilder, int brightness, PoseStack matrixStack) {

		@SuppressWarnings("deprecation")
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
		
		float u1 = sprite.getU0();
        float v1 = sprite.getV0();
        float u2 = sprite.getU1();
        float v2 = sprite.getV1();
		
        float offset = -0.002f;
        int b1 = brightness & '\uffff';
        int b2 = brightness >> 16 & '\uffff';
        float r = color.x() / 255f;
        float g = color.y() / 255f;
        float b = color.z() / 255f;
        float a = alpha;

        Matrix4f matrix = matrixStack.last().pose();

        if (scale > 0.0f) {
            //TOP
        	vt(vertexBuilder, matrix, min, scale + offset, min, u1, v1, b1, b2, r, g, b, a);
    		vt(vertexBuilder, matrix, min, scale + offset, max, u1, v2, b1, b2, r, g, b, a);
    		vt(vertexBuilder, matrix, max, scale + offset, max, u2, v2, b1, b2, r, g, b, a);
    		vt(vertexBuilder, matrix, max, scale + offset, min, u2, v1, b1, b2, r, g, b, a);
    		
    		//BOTTOM
    		vt(vertexBuilder, matrix, min, -offset, max, u1, v1, b1, b2, r, g, b, a);
    		vt(vertexBuilder, matrix, min, -offset, min, u1, v2, b1, b2, r, g, b, a);
    		vt(vertexBuilder, matrix, max, -offset, min, u2, v2, b1, b2, r, g, b, a);
    		vt(vertexBuilder, matrix, max, -offset, max, u2, v1, b1, b2, r, g, b, a);
    		
            //NORTH
            vt(vertexBuilder, matrix, max, scale, min - offset, u1, v1, b1, b2, r, g, b, a);
            vt(vertexBuilder, matrix, max, offset, min - offset, u1, v2, b1, b2, r, g, b, a);
            vt(vertexBuilder, matrix, min, offset, min - offset, u2, v2, b1, b2, r, g, b, a);
            vt(vertexBuilder, matrix, min, scale, min - offset, u2, v1, b1, b2, r, g, b, a);
            
            //SOUTH
            vt(vertexBuilder, matrix, max, offset, max + offset, u1, v2, b1, b2, r, g, b, a);
            vt(vertexBuilder, matrix, max, scale, max + offset, u1, v1, b1, b2, r, g, b, a);
            vt(vertexBuilder, matrix, min, scale, max + offset, u2, v1, b1, b2, r, g, b, a);
            vt(vertexBuilder, matrix, min, offset, max + offset, u2, v2, b1, b2, r, g, b, a);

            //EAST
            vt(vertexBuilder, matrix, max + offset, scale, max, u1, v1, b1, b2, r, g, b, a);
            vt(vertexBuilder, matrix, max + offset, offset, max, u1, v2, b1, b2, r, g, b, a);
            vt(vertexBuilder, matrix, max + offset, offset, min, u2, v2, b1, b2, r, g, b, a);
            vt(vertexBuilder, matrix, max + offset, scale, min, u2, v1, b1, b2, r, g, b, a);

            //WEST
            vt(vertexBuilder, matrix, min - offset, offset, max, u1, v2, b1, b2, r, g, b, a);
            vt(vertexBuilder, matrix, min - offset, scale, max, u1, v1, b1, b2, r, g, b, a);
            vt(vertexBuilder, matrix, min - offset, scale, min, u2, v1, b1, b2, r, g, b, a);
            vt(vertexBuilder, matrix, min - offset, offset, min, u2, v2, b1, b2, r, g, b, a);
        }
    }
	
    public static void vt(VertexConsumer renderer, Matrix4f matrix, float x, float y, float z, float u, float v, int lu, int lv, float r, float g, float b, float a) {
        renderer
                .vertex(matrix, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .uv2(lu, lv)
                .normal(1, 0, 0)
                .endVertex();
    }
    
    public static void vtEntity(VertexConsumer renderer, Matrix4f matrix, float x, float y, float z, float u, float v, int lu, int lv, float r, float g, float b, float a) {
        renderer
                .vertex(matrix, x, y, z)
                .color((int)r, (int)g, (int)b, (int)a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(lu, lv)
                .normal(-1, 0, 0)
                .endVertex();
    }
    
    public static void vc(VertexConsumer renderer, Matrix4f matrix, float x, float y, float z, int lu, int lv, float r, float g, float b, float a) {
        renderer
                .vertex(matrix, x, y, z)
                .color(r, g, b, a)
                .uv2(lu, lv)
                .normal(1, 0, 0)
                .endVertex();
    }
	
	
}
