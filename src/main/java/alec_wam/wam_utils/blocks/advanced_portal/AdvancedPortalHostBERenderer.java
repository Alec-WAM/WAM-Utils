package alec_wam.wam_utils.blocks.advanced_portal;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;

import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.blocks.advanced_portal.AdvancedPortalHostBE.PortalSettings;
import alec_wam.wam_utils.blocks.advanced_portal.AdvancedPortalHostBE.PortalType;
import alec_wam.wam_utils.client.RenderUtils;
import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.FrontAndTop;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class AdvancedPortalHostBERenderer implements BlockEntityRenderer<AdvancedPortalHostBE> {
	public static final ResourceLocation ADVANCED_PORTAL_TEXTURE = new ResourceLocation("wam_utils:blocks/advanced_portal");
	public static final ResourceLocation NETHER_PORTAL_TEXTURE = new ResourceLocation("minecraft:block/nether_portal");
	
	public AdvancedPortalHostBERenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AdvancedPortalHostBE portal, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
    	Level level = portal.getLevel();
    	BlockPos pos = portal.getBlockPos();
    	boolean redstone = RedstoneMode.ON.isMet(level, pos);

        FrontAndTop orientation = portal.getBlockState().getValue(AdvancedPortalHostBlock.ORIENTATION);
		Direction facing = orientation.front();
		
		Axis axis = facing.getAxis();
		
		PortalSettings settings = portal.getPortalSettings();
        
		//TODO Check if portal link is valid
    	if(redstone && portal.isPortalOn()/*&& (portal.getTeleportPos() !=null || portal.getOtherPortalPos() !=null)*/) {
	    	AABB aabb = portal.getPortalBB();
	    	AABB offsetBounds = aabb.move(-pos.getX(), -pos.getY(), -pos.getZ());
	        poseStack.pushPose();
	        Matrix4f matrix = poseStack.last().pose();
	        
	        float minX = (float) offsetBounds.minX;
	        float maxX = (float) offsetBounds.maxX;
	        
	        float minY = (float) offsetBounds.minY;
	        float maxY = (float) offsetBounds.maxY;
	        
	        float minZ = (float) offsetBounds.minZ;
	        float maxZ = (float) offsetBounds.maxZ;
	
	        
	        int packed = LightTexture.pack(15, 0);
	        
	        if(settings.portalType == PortalType.RGB) {
	        	float r = Math.min(settings.colorSettings.x(), 255.0F);
	        	float g = Math.min(settings.colorSettings.y(), 255.0F);
	        	float b = Math.min(settings.colorSettings.z(), 255.0F);
	        	renderTexturePortal(matrix, bufferSource, ADVANCED_PORTAL_TEXTURE, axis, packed, r, g, b, minX, maxX, minY, maxY, minZ, maxZ);
        	}
	        else if(settings.portalType == PortalType.NETHER) {
	        	renderRepeatingTexturePortal(matrix, bufferSource, NETHER_PORTAL_TEXTURE, axis, packed, 255.0F, 255.0F, 255.0F, minX, maxX, minY, maxY, minZ, maxZ);
		    }
	        else if(settings.portalType == PortalType.END) {
	        	renderEndPortal(matrix, bufferSource, axis, minX, maxX, minY, maxY, minZ, maxZ);
		    }
	
	    	poseStack.popPose();
    	}
        
    	/*poseStack.pushPose();	
        poseStack.translate(-pos.getX(), -pos.getY(), -pos.getZ());
        
        AABB aabb = portal.getPortalBB();
        LevelRenderer.renderLineBox(poseStack, bufferSource.getBuffer(RenderType.lines()), aabb, r, g, b, 1.0F);
        poseStack.popPose();*/
    }

    public void renderTexturePortal(Matrix4f matrix, MultiBufferSource bufferSource, ResourceLocation texture, Axis axis, int brightness, float red, float green, float blue, float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
    	VertexConsumer vertexBuilder = bufferSource.getBuffer(RenderType.translucent());
    	
    	@SuppressWarnings("deprecation")
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
		
		float u1 = sprite.getU0();
        float v1 = sprite.getV0();
        float u2 = sprite.getU1();
        float v2 = sprite.getV1();
        
        int b1 = brightness & '\uffff';
        int b2 = brightness >> 16 & '\uffff';
        float r = red / 255.0F;
        float g = green / 255.0F;
        float b = blue / 255.0F;
        float a = 1.0F;
    	
    	boolean showN = axis == Axis.Z;
        boolean showS = axis == Axis.Z;
        boolean showE = axis == Axis.X;
        boolean showW = axis == Axis.X;
        boolean showU = axis == Axis.Y;
        boolean showD = axis == Axis.Y;
        
        if(showU) {
        	RenderUtils.vt(vertexBuilder, matrix, minX, maxY, minZ, u1, v1, b1, b2, r, g, b, a);
        	RenderUtils.vt(vertexBuilder, matrix, minX, maxY, maxZ, u1, v2, b1, b2, r, g, b, a);
    		RenderUtils.vt(vertexBuilder, matrix, maxX, maxY, maxZ, u2, v2, b1, b2, r, g, b, a);
    		RenderUtils.vt(vertexBuilder, matrix, maxX, maxY, minZ, u2, v1, b1, b2, r, g, b, a);
        }
        
        if(showD) {
        	RenderUtils.vt(vertexBuilder, matrix, minX, minY, maxZ, u1, v1, b1, b2, r, g, b, a);
        	RenderUtils.vt(vertexBuilder, matrix, minX, minY, minZ, u1, v2, b1, b2, r, g, b, a);
        	RenderUtils.vt(vertexBuilder, matrix, maxX, minY, minZ, u2, v2, b1, b2, r, g, b, a);
        	RenderUtils.vt(vertexBuilder, matrix, maxX, minY, maxZ, u2, v1, b1, b2, r, g, b, a);
        }
        
        if(showN) {
	        RenderUtils.vt(vertexBuilder, matrix, maxX, maxY, minZ, u1, v1, b1, b2, r, g, b, a);
	        RenderUtils.vt(vertexBuilder, matrix, maxX, minY, minZ, u1, v2, b1, b2, r, g, b, a);
	        RenderUtils.vt(vertexBuilder, matrix, minX, minY, minZ, u2, v2, b1, b2, r, g, b, a);
	        RenderUtils.vt(vertexBuilder, matrix, minX, maxY, minZ, u2, v1, b1, b2, r, g, b, a);
        }
        
        if(showS) {
        	RenderUtils.vt(vertexBuilder, matrix, maxX, minY, maxZ, u1, v2, b1, b2, r, g, b, a);
        	RenderUtils.vt(vertexBuilder, matrix, maxX, maxY, maxZ, u1, v1, b1, b2, r, g, b, a);
        	RenderUtils.vt(vertexBuilder, matrix, minX, maxY, maxZ, u2, v1, b1, b2, r, g, b, a);
        	RenderUtils.vt(vertexBuilder, matrix, minX, minY, maxZ, u2, v2, b1, b2, r, g, b, a);
        }
        
        if(showE) {
        	RenderUtils.vt(vertexBuilder, matrix, maxX, maxY, maxZ, u1, v1, b1, b2, r, g, b, a);
        	RenderUtils.vt(vertexBuilder, matrix, maxX, minY, maxZ, u1, v2, b1, b2, r, g, b, a);
        	RenderUtils.vt(vertexBuilder, matrix, maxX, minY, minZ, u2, v2, b1, b2, r, g, b, a);
        	RenderUtils.vt(vertexBuilder, matrix, maxX, maxY, minZ, u2, v1, b1, b2, r, g, b, a);
        }
        
        if(showW) {
        	RenderUtils.vt(vertexBuilder, matrix, minX, minY, maxZ, u1, v2, b1, b2, r, g, b, a);
        	RenderUtils.vt(vertexBuilder, matrix, minX, maxY, maxZ, u1, v1, b1, b2, r, g, b, a);
        	RenderUtils.vt(vertexBuilder, matrix, minX, maxY, minZ, u2, v1, b1, b2, r, g, b, a);
        	RenderUtils.vt(vertexBuilder, matrix, minX, minY, minZ, u2, v2, b1, b2, r, g, b, a);
        }
    }
    
    public void renderRepeatingTexturePortal(Matrix4f matrix, MultiBufferSource bufferSource, ResourceLocation texture, Axis axis, int brightness, float red, float green, float blue, float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
    	VertexConsumer vertexBuilder = bufferSource.getBuffer(RenderType.translucent());
    	
    	@SuppressWarnings("deprecation")
		TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
		
		float u1 = sprite.getU0();
        float v1 = sprite.getV0();
        float u2 = sprite.getU1();
        float v2 = sprite.getV1();
        
        int b1 = brightness & '\uffff';
        int b2 = brightness >> 16 & '\uffff';
        float r = red / 255.0F;
        float g = green / 255.0F;
        float b = blue / 255.0F;
        float a = 1.0F;
    	
    	boolean showN = axis == Axis.Z;
        boolean showS = axis == Axis.Z;
        boolean showE = axis == Axis.X;
        boolean showW = axis == Axis.X;
        boolean showU = axis == Axis.Y;
        boolean showD = axis == Axis.Y;
        
        int width = (int) Math.ceil(maxX - minX);
        int height = (int) Math.ceil(maxY - minY);
        
        if(axis == Axis.Y) {
        	height = (int) Math.ceil(maxZ - minZ);
        }
        
        if(axis == Axis.X) {
        	width = (int) Math.ceil(maxZ - minZ);
        }
        
        if(showU) {
        	for(int x = 0; x < width; x++) {
        		for(int z = 0; z < height; z++) {
        			float relMinX = minX + (1.0F * x);
        			float relMaxX = relMinX + (1.0F);
        			float relMinZ = minZ + (1.0F * z);
        			float relMaxZ = relMinZ + (1.0F);
		        	RenderUtils.vt(vertexBuilder, matrix, relMinX, maxY, relMinZ, u1, v1, b1, b2, r, g, b, a);
		        	RenderUtils.vt(vertexBuilder, matrix, relMinX, maxY, relMaxZ, u1, v2, b1, b2, r, g, b, a);
		    		RenderUtils.vt(vertexBuilder, matrix, relMaxX, maxY, relMaxZ, u2, v2, b1, b2, r, g, b, a);
		    		RenderUtils.vt(vertexBuilder, matrix, relMaxX, maxY, relMinZ, u2, v1, b1, b2, r, g, b, a);
        		}
        	}
        }
        
        if(showD) {
        	for(int x = 0; x < width; x++) {
        		for(int z = 0; z < height; z++) {
        			float relMinX = minX + (1.0F * x);
        			float relMaxX = relMinX + (1.0F);
        			float relMinZ = minZ + (1.0F * z);
        			float relMaxZ = relMinZ + (1.0F);
		        	RenderUtils.vt(vertexBuilder, matrix, relMinX, minY, relMaxZ, u1, v1, b1, b2, r, g, b, a);
		        	RenderUtils.vt(vertexBuilder, matrix, relMinX, minY, relMinZ, u1, v2, b1, b2, r, g, b, a);
		        	RenderUtils.vt(vertexBuilder, matrix, relMaxX, minY, relMinZ, u2, v2, b1, b2, r, g, b, a);
		        	RenderUtils.vt(vertexBuilder, matrix, relMaxX, minY, relMaxZ, u2, v1, b1, b2, r, g, b, a);
        		}
        	}
        }
        
        if(showN) {
        	for(int x = 0; x < width; x++) {
        		for(int y = 0; y < height; y++) {
        			float relMinX = minX + (1.0F * x);
        			float relMaxX = relMinX + (1.0F);
        			float relMinY = minY + (1.0F * y);
        			float relMaxY = relMinY + (1.0F);
			        RenderUtils.vt(vertexBuilder, matrix, relMaxX, relMaxY, minZ, u1, v1, b1, b2, r, g, b, a);
			        RenderUtils.vt(vertexBuilder, matrix, relMaxX, relMinY, minZ, u1, v2, b1, b2, r, g, b, a);
			        RenderUtils.vt(vertexBuilder, matrix, relMinX, relMinY, minZ, u2, v2, b1, b2, r, g, b, a);
			        RenderUtils.vt(vertexBuilder, matrix, relMinX, relMaxY, minZ, u2, v1, b1, b2, r, g, b, a);
        		}
        	}
        }
        
        if(showS) {
        	for(int x = 0; x < width; x++) {
        		for(int y = 0; y < height; y++) {
        			float relMinX = minX + (1.0F * x);
        			float relMaxX = relMinX + (1.0F);
        			float relMinY = minY + (1.0F * y);
        			float relMaxY = relMinY + (1.0F);
		        	RenderUtils.vt(vertexBuilder, matrix, relMaxX, relMinY, maxZ, u1, v2, b1, b2, r, g, b, a);
		        	RenderUtils.vt(vertexBuilder, matrix, relMaxX, relMaxY, maxZ, u1, v1, b1, b2, r, g, b, a);
		        	RenderUtils.vt(vertexBuilder, matrix, relMinX, relMaxY, maxZ, u2, v1, b1, b2, r, g, b, a);
		        	RenderUtils.vt(vertexBuilder, matrix, relMinX, relMinY, maxZ, u2, v2, b1, b2, r, g, b, a);
        		}
        	}
        }
        
        if(showE) {
        	for(int z = 0; z < width; z++) {
        		for(int y = 0; y < height; y++) {
        			float relMinZ = minZ + (1.0F * z);
        			float relMaxZ = relMinZ + (1.0F);
        			float relMinY = minY + (1.0F * y);
        			float relMaxY = relMinY + (1.0F);
		        	RenderUtils.vt(vertexBuilder, matrix, maxX, relMaxY, relMaxZ, u1, v1, b1, b2, r, g, b, a);
		        	RenderUtils.vt(vertexBuilder, matrix, maxX, relMinY, relMaxZ, u1, v2, b1, b2, r, g, b, a);
		        	RenderUtils.vt(vertexBuilder, matrix, maxX, relMinY, relMinZ, u2, v2, b1, b2, r, g, b, a);
		        	RenderUtils.vt(vertexBuilder, matrix, maxX, relMaxY, relMinZ, u2, v1, b1, b2, r, g, b, a);
        		}
        	}
        }
        
        if(showW) {
        	for(int z = 0; z < width; z++) {
        		for(int y = 0; y < height; y++) {
        			float relMinZ = minZ + (1.0F * z);
        			float relMaxZ = relMinZ + (1.0F);
        			float relMinY = minY + (1.0F * y);
        			float relMaxY = relMinY + (1.0F);
		        	RenderUtils.vt(vertexBuilder, matrix, minX, relMinY, relMaxZ, u1, v2, b1, b2, r, g, b, a);
		        	RenderUtils.vt(vertexBuilder, matrix, minX, relMaxY, relMaxZ, u1, v1, b1, b2, r, g, b, a);
		        	RenderUtils.vt(vertexBuilder, matrix, minX, relMaxY, relMinZ, u2, v1, b1, b2, r, g, b, a);
		        	RenderUtils.vt(vertexBuilder, matrix, minX, relMinY, relMinZ, u2, v2, b1, b2, r, g, b, a);
        		}
        	}
        }
    }
    
    public void renderEndPortal(Matrix4f matrix, MultiBufferSource bufferSource, Axis axis, float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
    	VertexConsumer vertexBuilder = bufferSource.getBuffer(RenderType.endPortal());
    	
    	boolean showN = axis == Axis.Z;
        boolean showS = axis == Axis.Z;
        boolean showE = axis == Axis.X;
        boolean showW = axis == Axis.X;
        boolean showU = axis == Axis.Y;
        boolean showD = axis == Axis.Y;
        
        if(showU) {
        	vertexBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
        	vertexBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
    		vertexBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
    		vertexBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
        }
        
        if(showD) {
        	vertexBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
        	vertexBuilder.vertex(matrix, minX, minY, minZ).endVertex();
        	vertexBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
        	vertexBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
        }
        
        if(showN) {
	        vertexBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
	        vertexBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
	        vertexBuilder.vertex(matrix, minX, minY, minZ).endVertex();
	        vertexBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
        }
        
        if(showS) {
        	vertexBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
        	vertexBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
        	vertexBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
        	vertexBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
        }
        
        if(showE) {
        	vertexBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
        	vertexBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
        	vertexBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
        	vertexBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
        }
        
        if(showW) {
        	vertexBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
        	vertexBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
        	vertexBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
        	vertexBuilder.vertex(matrix, minX, minY, minZ).endVertex();
        }
    }
    
    public static void register() {
        BlockEntityRenderers.register(BlockInit.ADVANCED_PORTAL_HOST_BE.get(), AdvancedPortalHostBERenderer::new);
    }

}

