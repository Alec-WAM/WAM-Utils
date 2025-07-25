package alec_wam.wam_utils.client.render.entities;

import java.awt.Color;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

public class RenderHelper {

	public static final ResourceLocation DUMMY_TEXTURE = ResourceLocation.fromNamespaceAndPath("neoforge", "white");
    private static float dummyU0 = 0F;
    private static float dummyU1 = 1F;
    private static float dummyV0 = 0F;
    private static float dummyV1 = 1F;
	
	public static void renderLines(PoseStack matrix, BlockPos startPos, BlockPos endPos, Color color, MultiBufferSource buffer) {
        //We want to draw from the starting position to the (ending position)+1
        int x = Math.min(startPos.getX(), endPos.getX()), y = Math.min(startPos.getY(), endPos.getY()), z = Math.min(startPos.getZ(), endPos.getZ());

        int dx = (startPos.getX() > endPos.getX()) ? startPos.getX() + 1 : endPos.getX() + 1;
        int dy = (startPos.getY() > endPos.getY()) ? startPos.getY() + 1 : endPos.getY() + 1;
        int dz = (startPos.getZ() > endPos.getZ()) ? startPos.getZ() + 1 : endPos.getZ() + 1;

        VertexConsumer builder = buffer.getBuffer(RenderType.lines());

        matrix.pushPose();
        Matrix4f matrix4f = matrix.last().pose();
        PoseStack.Pose matrix3f = matrix.last();
        int colorRGB = color.getRGB();

        builder.addVertex(matrix4f, x, y, z).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, dx, y, z).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, x, y, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F);
        builder.addVertex(matrix4f, x, dy, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F);
        builder.addVertex(matrix4f, x, y, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F);
        builder.addVertex(matrix4f, x, y, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F);
        builder.addVertex(matrix4f, dx, y, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F);
        builder.addVertex(matrix4f, dx, dy, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F);
        builder.addVertex(matrix4f, dx, dy, z).setColor(colorRGB).setNormal(matrix3f, -1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, x, dy, z).setColor(colorRGB).setNormal(matrix3f, -1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, x, dy, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F);
        builder.addVertex(matrix4f, x, dy, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F);
        builder.addVertex(matrix4f, x, dy, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, -1.0F, 0.0F);
        builder.addVertex(matrix4f, x, y, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, -1.0F, 0.0F);
        builder.addVertex(matrix4f, x, y, dz).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, dx, y, dz).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, dx, y, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, -1.0F);
        builder.addVertex(matrix4f, dx, y, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, -1.0F);
        builder.addVertex(matrix4f, x, dy, dz).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, dx, dy, dz).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, dx, y, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F);
        builder.addVertex(matrix4f, dx, dy, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F);
        builder.addVertex(matrix4f, dx, dy, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F);
        builder.addVertex(matrix4f, dx, dy, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F);

        matrix.popPose();
    }

    public static void renderLines(PoseStack matrix, AABB aabb, Color color, MultiBufferSource buffer) {
        //We want to draw from the starting position to the (ending position)+1
        float x = (float) aabb.minX;
        float y = (float) aabb.minY;
        float z = (float) aabb.minZ;
        float dx = (float) aabb.maxX;
        float dy = (float) aabb.maxY;
        float dz = (float) aabb.maxZ;

        VertexConsumer builder = buffer.getBuffer(RenderType.lines());

        matrix.pushPose();
        Matrix4f matrix4f = matrix.last().pose();
        PoseStack.Pose matrix3f = matrix.last();
        int colorRGB = color.getRGB();

        builder.addVertex(matrix4f, x, y, z).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, dx, y, z).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, x, y, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F);
        builder.addVertex(matrix4f, x, dy, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F);
        builder.addVertex(matrix4f, x, y, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F);
        builder.addVertex(matrix4f, x, y, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F);
        builder.addVertex(matrix4f, dx, y, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F);
        builder.addVertex(matrix4f, dx, dy, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F);
        builder.addVertex(matrix4f, dx, dy, z).setColor(colorRGB).setNormal(matrix3f, -1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, x, dy, z).setColor(colorRGB).setNormal(matrix3f, -1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, x, dy, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F);
        builder.addVertex(matrix4f, x, dy, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F);
        builder.addVertex(matrix4f, x, dy, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, -1.0F, 0.0F);
        builder.addVertex(matrix4f, x, y, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, -1.0F, 0.0F);
        builder.addVertex(matrix4f, x, y, dz).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, dx, y, dz).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, dx, y, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, -1.0F);
        builder.addVertex(matrix4f, dx, y, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, -1.0F);
        builder.addVertex(matrix4f, x, dy, dz).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, dx, dy, dz).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, dx, y, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F);
        builder.addVertex(matrix4f, dx, dy, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F);
        builder.addVertex(matrix4f, dx, dy, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F);
        builder.addVertex(matrix4f, dx, dy, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F);

        matrix.popPose();
    }

    public static void renderLines(PoseStack matrix, BlockPos startPos, BlockPos endPos, Color color) {
        //We want to draw from the starting position to the (ending position)+1
        int x = Math.min(startPos.getX(), endPos.getX()), y = Math.min(startPos.getY(), endPos.getY()), z = Math.min(startPos.getZ(), endPos.getZ());

        int dx = (startPos.getX() > endPos.getX()) ? startPos.getX() + 1 : endPos.getX() + 1;
        int dy = (startPos.getY() > endPos.getY()) ? startPos.getY() + 1 : endPos.getY() + 1;
        int dz = (startPos.getZ() > endPos.getZ()) ? startPos.getZ() + 1 : endPos.getZ() + 1;

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer builder = buffer.getBuffer(RenderType.lines());

        matrix.pushPose();
        Matrix4f matrix4f = matrix.last().pose();
        PoseStack.Pose matrix3f = matrix.last();
        int colorRGB = color.getRGB();

        builder.addVertex(matrix4f, x, y, z).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, dx, y, z).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, x, y, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F);
        builder.addVertex(matrix4f, x, dy, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F);
        builder.addVertex(matrix4f, x, y, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F);
        builder.addVertex(matrix4f, x, y, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F);
        builder.addVertex(matrix4f, dx, y, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F);
        builder.addVertex(matrix4f, dx, dy, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F);
        builder.addVertex(matrix4f, dx, dy, z).setColor(colorRGB).setNormal(matrix3f, -1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, x, dy, z).setColor(colorRGB).setNormal(matrix3f, -1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, x, dy, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F);
        builder.addVertex(matrix4f, x, dy, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F);
        builder.addVertex(matrix4f, x, dy, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, -1.0F, 0.0F);
        builder.addVertex(matrix4f, x, y, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, -1.0F, 0.0F);
        builder.addVertex(matrix4f, x, y, dz).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, dx, y, dz).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, dx, y, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, -1.0F);
        builder.addVertex(matrix4f, dx, y, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, -1.0F);
        builder.addVertex(matrix4f, x, dy, dz).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, dx, dy, dz).setColor(colorRGB).setNormal(matrix3f, 1.0F, 0.0F, 0.0F);
        builder.addVertex(matrix4f, dx, y, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F);
        builder.addVertex(matrix4f, dx, dy, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 1.0F, 0.0F);
        builder.addVertex(matrix4f, dx, dy, z).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F);
        builder.addVertex(matrix4f, dx, dy, dz).setColor(colorRGB).setNormal(matrix3f, 0.0F, 0.0F, 1.0F);

        buffer.endBatch(RenderType.lines());
        matrix.popPose();
    }

    public static void renderBoxSolid(PoseStack pose, Matrix4f matrix, MultiBufferSource buffer, BlockPos pos, float r, float g, float b, float alpha) {
        double x = pos.getX() - 0.001;
        double y = pos.getY() - 0.001;
        double z = pos.getZ() - 0.001;
        double xEnd = pos.getX() + 1.0015;
        double yEnd = pos.getY() + 1.0015;
        double zEnd = pos.getZ() + 1.0015;

        renderBoxSolid(pose.last(), matrix, buffer, x, y, z, xEnd, yEnd, zEnd, r, g, b, alpha);
    }

    public static void renderFaceSolid(PoseStack pose, Matrix4f matrix, MultiBufferSource buffer, BlockPos pos, Direction direction, float r, float g, float b, float alpha) {
        double x = pos.getX() - 0.001;
        double y = pos.getY() - 0.001;
        double z = pos.getZ() - 0.001;
        double xEnd = pos.getX() + 1.0015;
        double yEnd = pos.getY() + 1.0015;
        double zEnd = pos.getZ() + 1.0015;

        switch (direction) {
            case DOWN:
                // Draw on the bottom face (y = pos.getY())
                renderBoxSolid(pose.last(), matrix, buffer, x, y - 0.001, z, xEnd, y, zEnd, r, g, b, alpha);
                break;
            case UP:
                // Draw on the top face (y = pos.getY() + 1)
                renderBoxSolid(pose.last(), matrix, buffer, x, yEnd, z, xEnd, yEnd + 0.0015, zEnd, r, g, b, alpha);
                break;
            case NORTH:
                // Draw on the north face (z = pos.getZ())
                renderBoxSolid(pose.last(), matrix, buffer, x, y, z - 0.001, xEnd, yEnd, z, r, g, b, alpha);
                break;
            case SOUTH:
                // Draw on the south face (z = pos.getZ() + 1)
                renderBoxSolid(pose.last(), matrix, buffer, x, y, zEnd, xEnd, yEnd, zEnd + 0.0015, r, g, b, alpha);
                break;
            case WEST:
                // Draw on the west face (x = pos.getX())
                renderBoxSolid(pose.last(), matrix, buffer, x - 0.001, y, z, x, yEnd, zEnd, r, g, b, alpha);
                break;
            case EAST:
                // Draw on the east face (x = pos.getX() + 1)
                renderBoxSolid(pose.last(), matrix, buffer, xEnd, y, z, xEnd + 0.0015, yEnd, zEnd, r, g, b, alpha);
                break;
        }
    }

    public static void renderBoxSolid(PoseStack pose, Matrix4f matrix, MultiBufferSource buffer, AABB aabb, float r, float g, float b, float alpha) {
        float minX = (float) aabb.minX;
        float minY = (float) aabb.minY;
        float minZ = (float) aabb.minZ;
        float maxX = (float) aabb.maxX;
        float maxY = (float) aabb.maxY;
        float maxZ = (float) aabb.maxZ;

        renderBoxSolid(pose.last(), matrix, buffer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, alpha);
    }

    public static void renderBoxSolidOpaque(PoseStack pose, Matrix4f matrix, MultiBufferSource buffer, AABB aabb, float r, float g, float b, float alpha) {
        float minX = (float) aabb.minX;
        float minY = (float) aabb.minY;
        float minZ = (float) aabb.minZ;
        float maxX = (float) aabb.maxX;
        float maxY = (float) aabb.maxY;
        float maxZ = (float) aabb.maxZ;

        renderBoxSolidOpaque(pose.last(), matrix, buffer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, alpha);
    }

    //This one does not block water
    public static void renderBoxSolid(PoseStack.Pose pose, Matrix4f matrix, MultiBufferSource buffer, double x, double y, double z, double xEnd, double yEnd, double zEnd, float red, float green, float blue, float alpha) {
        VertexConsumer builder = buffer.getBuffer(RenderType.translucent());

        float startX = (float) x;
        float startY = (float) y;
        float startZ = (float) z;
        float endX = (float) xEnd;
        float endY = (float) yEnd;
        float endZ = (float) zEnd;

        //down
        builder.addVertex(matrix, startX, startY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, startY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, startY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, startY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);

        //up
        builder.addVertex(matrix, startX, endY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, endY, endZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, endY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, endY, startZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);

        //east
        builder.addVertex(matrix, startX, startY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, endY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, endY, startZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, startY, startZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);

        //west
        builder.addVertex(matrix, startX, startY, endZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, startY, endZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, endY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, endY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);

        //south
        builder.addVertex(matrix, endX, startY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, endY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, endY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, startY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);

        //north
        builder.addVertex(matrix, startX, startY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, startY, endZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, endY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, endY, startZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
    }

    //This one blocks water
    public static void renderBoxSolidOpaque(PoseStack.Pose pose, Matrix4f matrix, MultiBufferSource buffer, double x, double y, double z, double xEnd, double yEnd, double zEnd, float red, float green, float blue, float alpha) {
        VertexConsumer builder = buffer.getBuffer(RenderType.DEBUG_QUADS);

        float startX = (float) x;
        float startY = (float) y;
        float startZ = (float) z;
        float endX = (float) xEnd;
        float endY = (float) yEnd;
        float endZ = (float) zEnd;

        //down
        builder.addVertex(matrix, startX, startY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, startY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, startY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, startY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);

        //up
        builder.addVertex(matrix, startX, endY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, endY, endZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, endY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, endY, startZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);

        //east
        builder.addVertex(matrix, startX, startY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, endY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, endY, startZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, startY, startZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);

        //west
        builder.addVertex(matrix, startX, startY, endZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, startY, endZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, endY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, endY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);

        //south
        builder.addVertex(matrix, endX, startY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, endY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, endY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, endX, startY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);

        //north
        builder.addVertex(matrix, startX, startY, startZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, startY, endZ).setColor(red, green, blue, alpha).setUv(dummyU0, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, endY, endZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
        builder.addVertex(matrix, startX, endY, startZ).setColor(red, green, blue, alpha).setUv(dummyU1, dummyV0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0F, 0F, 1F);
    }

    public static void captureDummySprite(TextureAtlas atlas) {
        TextureAtlasSprite sprite = atlas.getSprite(DUMMY_TEXTURE);
        dummyU0 = sprite.getU0();
        dummyU1 = sprite.getU1();
        dummyV0 = sprite.getV0();
        dummyV1 = sprite.getV1();
    }
	
    
    public static Vector3f[] getFaceQuad(Direction dir, AABB box) {
    	float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
        float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;

        return switch (dir) {
            case NORTH -> new Vector3f[] {
                new Vector3f(minX, maxY, minZ),
                new Vector3f(maxX, maxY, minZ),
                new Vector3f(maxX, minY, minZ),
                new Vector3f(minX, minY, minZ)
            };
            case SOUTH -> new Vector3f[] {
                new Vector3f(maxX, maxY, maxZ),
                new Vector3f(minX, maxY, maxZ),
                new Vector3f(minX, minY, maxZ),
                new Vector3f(maxX, minY, maxZ)
            };
            case EAST -> new Vector3f[] {
                new Vector3f(maxX, maxY, minZ),
                new Vector3f(maxX, maxY, maxZ),
                new Vector3f(maxX, minY, maxZ),
                new Vector3f(maxX, minY, minZ)
            };
            case WEST -> new Vector3f[] {
                new Vector3f(minX, maxY, maxZ),
                new Vector3f(minX, maxY, minZ),
                new Vector3f(minX, minY, minZ),
                new Vector3f(minX, minY, maxZ)
            };
            case UP -> new Vector3f[] {
                new Vector3f(minX, maxY, minZ),
                new Vector3f(minX, maxY, maxZ),
                new Vector3f(maxX, maxY, maxZ),
                new Vector3f(maxX, maxY, minZ)
            };
            case DOWN -> new Vector3f[] {
                new Vector3f(minX, minY, maxZ),
                new Vector3f(minX, minY, minZ),
                new Vector3f(maxX, minY, minZ),
                new Vector3f(maxX, minY, maxZ)
            };
        };
    }
    
}
