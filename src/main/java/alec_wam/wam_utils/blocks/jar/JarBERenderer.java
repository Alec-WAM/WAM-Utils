package alec_wam.wam_utils.blocks.jar;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;

import alec_wam.wam_utils.blocks.jar.JarBE.JarContents;
import alec_wam.wam_utils.client.RenderUtils;
import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.ShulkerBulletModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;

public class JarBERenderer implements BlockEntityRenderer<JarBE> {
	
	public static final ResourceLocation POTION_TEXTURE = new ResourceLocation("wam_utils:blocks/potion");
	public static final ResourceLocation HONEY_TEXTURE = new ResourceLocation("minecraft:block/honey_block_bottom");
	public static final ResourceLocation ITEM_FRAME_TEXTURE = new ResourceLocation("minecraft:block/item_frame");
	private static final ResourceLocation BULLET_TEXTURE = new ResourceLocation("textures/entity/shulker/spark.png");
	   
	private final ShulkerBulletModel<ShulkerBullet> bulletModel;
	
	public JarBERenderer(BlockEntityRendererProvider.Context context) {
		this.bulletModel = new ShulkerBulletModel<>(context.bakeLayer(ModelLayers.SHULKER_BULLET));
    }

	private final Vector3f WHITE_COLOR = new Vector3f(255.0F, 255.0F, 255.0F);
    @Override
    public void render(JarBE jar, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
    	poseStack.pushPose();	
    	
    	float min = 3.0F / 16.0F;
    	float max = 13.0F / 16.0F;
    	Potion potion = null;
		if(jar.getContents() == JarContents.POTION) {
			if(jar.getPotion() != Potions.EMPTY) {
				potion = jar.getPotion();
	        	float renderHeight = ((float)jar.getBottleCount() / (float)JarBE.BOTTLE_CAPACITY) * (13.0F / 16.0F);
	        	int packed = LightTexture.pack(combinedLight, 0);
	        	RenderUtils.renderFakeFluid(POTION_TEXTURE, min, renderHeight, max, getColorFromPotion(jar.getPotion()), 1.0F, bufferSource.getBuffer(RenderType.translucent()), packed, poseStack);
	        }
        }
		if(jar.getContents() == JarContents.HONEY) {
			float renderHeight = ((float)jar.getBottleCount() / (float)JarBE.BOTTLE_CAPACITY) * (13.0F / 16.0F);
        	int packed = LightTexture.pack(combinedLight, 0);
        	RenderUtils.renderFakeFluid(HONEY_TEXTURE, min, renderHeight, max, WHITE_COLOR, 1.0F, bufferSource.getBuffer(RenderType.solid()), packed, poseStack);        
        		
		}
		if(jar.getContents() == JarContents.SHULKER) {
			poseStack.pushPose();	
			poseStack.translate(0.5D, 0.1D, 0.5D);
			float f1 = Mth.sin(((float)jar.shulkerTick + partialTicks) / 20.0F + jar.shulkerBobOffset) * 0.1F + 0.1F;
			float f2 = 0.3125F / 2.0F;
			poseStack.translate(0.0D, (double)(f1 + 0.25F * f2), 0.0D);
			renderShulkerBullet(this.bulletModel, jar.shulkerTick, poseStack, bufferSource, partialTicks, combinedLight);
			poseStack.popPose();
		}
		if(jar.getContents() == JarContents.LIGHTNING) {
			poseStack.pushPose();
			float renderHeight = ((float)jar.getBottleCount() / (float)JarBE.LIGHTNING_CAPACITY) * (13.0F / 16.0F);
        	int packed = LightTexture.pack(combinedLight, 0);
        	VertexConsumer vertex = VertexMultiConsumer.create(bufferSource.getBuffer(RenderType.entityGlintDirect()), bufferSource.getBuffer(RenderType.translucent()));
        	RenderUtils.renderFakeFluid(POTION_TEXTURE, min, renderHeight, max, WHITE_COLOR, 1.0F, vertex, packed, poseStack);
			poseStack.popPose();
        
//        	List<Direction> labels = jar.getLabelMap().keySet().stream().filter((dir) -> jar.hasLabel(dir)).toList();
//        	for(Direction dir : labels) {
//        		renderLabel(poseStack, bufferSource, dir, combinedLight, JarContents.HONEY, null);
//        	}		
		}
		
		List<Direction> labels = jar.getLabelMap().keySet().stream().filter((dir) -> jar.hasLabel(dir)).toList();
    	for(Direction dir : labels) {
    		renderLabel(poseStack, bufferSource, dir, combinedLight, jar.getContents(), potion);
    	}	
        poseStack.popPose();
    }

    public static void register() {
        BlockEntityRenderers.register(BlockInit.JAR_BE.get(), JarBERenderer::new);
    }
    
    public static void renderShulkerBullet(ShulkerBulletModel<ShulkerBullet> bulletModel, int tick, PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks, int light) {
    	poseStack.pushPose();
        float f2 = (float)(tick) + partialTicks;
        f2 *= 0.5F;
        poseStack.translate(0.0D, (double)0.15F, 0.0D);
        poseStack.mulPose(Vector3f.YP.rotationDegrees(Mth.sin(f2 * 0.1F) * 180.0F));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(Mth.cos(f2 * 0.1F) * 180.0F));
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(Mth.sin(f2 * 0.15F) * 360.0F));
        poseStack.scale(-0.5F, -0.5F, 0.5F);
        //this.bulletModel.setupAnim(p_115862_, 0.0F, 0.0F, 0.0F, f, f1);
        VertexConsumer vertexconsumer = bufferSource.getBuffer(bulletModel.renderType(BULLET_TEXTURE));
        bulletModel.renderToBuffer(poseStack, vertexconsumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        VertexConsumer vertexconsumer1 = bufferSource.getBuffer(RenderType.entityTranslucent(BULLET_TEXTURE));
        bulletModel.renderToBuffer(poseStack, vertexconsumer1, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.15F);
        poseStack.popPose();
    }
    
    @SuppressWarnings("deprecation")
	public static void renderLabel(PoseStack poseStack, MultiBufferSource bufferSource, Direction dir, int brightness, JarContents contents, Potion potion) {
    	poseStack.pushPose();
    	if(dir == Direction.NORTH){
    		poseStack.translate(1, 0, 1);
		}
    	else if(dir == Direction.EAST){
			poseStack.translate(0, 0, 1);
		}
    	else if(dir == Direction.WEST){
			poseStack.translate(1, 0, 0);
		}
    	float f = -dir.toYRot();
    	poseStack.mulPose(Vector3f.YP.rotationDegrees(f));
    	
    	TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ITEM_FRAME_TEXTURE);
    	
    	float u1 = sprite.getU0();
        float v1 = sprite.getV0();
        float u2 = sprite.getU1();
        float v2 = sprite.getV1();
    	
        int b1 = brightness & '\uffff';
        int b2 = brightness >> 16 & '\uffff';
        VertexConsumer vertex = bufferSource.getBuffer(RenderType.solid());
        Matrix4f matrix = poseStack.last().pose();
        float r = 1.0F;
        float g = 1.0F;
        float b = 1.0F;
        float a = 1.0F;
        
        float labelZ = 0.819F;        
        float minY = 0.2F;
        float maxY = 0.6F;
        
        float minX = 0.3F;
        float maxX = 0.7F;
        
        //FRONT
        RenderUtils.vt(vertex, matrix, maxX, minY, labelZ, u1, v1, b1, b2, r, g, b, a);
        RenderUtils.vt(vertex, matrix, maxX, maxY, labelZ, u1, v2, b1, b2, r, g, b, a);
        RenderUtils.vt(vertex, matrix, minX, maxY, labelZ, u2, v2, b1, b2, r, g, b, a);
        RenderUtils.vt(vertex, matrix, minX, minY, labelZ, u2, v1, b1, b2, r, g, b, a);
        
        //BACK
        RenderUtils.vt(vertex, matrix, minX, minY, labelZ, u1, v1, b1, b2, r, g, b, a);
        RenderUtils.vt(vertex, matrix, minX, maxY, labelZ, u1, v2, b1, b2, r, g, b, a);
        RenderUtils.vt(vertex, matrix, maxX, maxY, labelZ, u2, v2, b1, b2, r, g, b, a);
        RenderUtils.vt(vertex, matrix, maxX, minY, labelZ, u2, v1, b1, b2, r, g, b, a);
        
        if(contents == JarContents.POTION) {
        	if(potion !=null) {
        		if(!potion.getEffects().isEmpty()) {
        			MobEffectInstance effectInstance = potion.getEffects().get(0);
        			MobEffect effect = effectInstance.getEffect();
    				
        			float border = 0.1F;
        			boolean ignoreIcon = false;
        			if(!ignoreIcon && effectInstance.showIcon()) {
			        	poseStack.pushPose();
			        	poseStack.translate(0.3 + 0.2, 0.6, labelZ + 0.001);
			        	poseStack.mulPose(Vector3f.XP.rotationDegrees(180.0F));
			        	var renderer = net.minecraftforge.client.extensions.common.IClientMobEffectExtensions.of(effectInstance);
        				boolean skipRender = false;
        				
        				@SuppressWarnings("resource")
						Font font = Minecraft.getInstance().font;
						
        				List<String> lines = Lists.newArrayList();
        				String displayName = effect.getDisplayName().getString();
        				lines.add(displayName);        				
        				
        				Component numeral = Component.translatable("potion.potency." + effectInstance.getAmplifier());
        				if(!numeral.getString().isEmpty()) {
			        		lines.add(numeral.getString());
			        	}
        				
        				if (renderer.renderGuiIcon(effectInstance, null, poseStack, 2, 2, 0, 1.0F)) {
        					skipRender = true;
        				}
        				
        				if(!skipRender) {
        					float textOffset = 0.02F * lines.size();
        					poseStack.pushPose();
        			    	MobEffectTextureManager mobeffecttexturemanager = Minecraft.getInstance().getMobEffectTextures();
        					TextureAtlasSprite textureatlassprite = mobeffecttexturemanager.get(effect);

        			        VertexConsumer vertexIcon = bufferSource.getBuffer(RenderType.entityCutout(textureatlassprite.atlas().location()));
        			        RenderSystem.setShaderTexture(0, textureatlassprite.atlas().location());
        	                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        	                
        	                float potionR = 255.0F;
        	                float potionG = 255.0F;
        	                float potionB = 255.0F;
        	                float potionA = 255.0F;
        			        float potionU1 = textureatlassprite.getU0();
        			        float potionV1 = textureatlassprite.getV0();
        			        float potionU2 = textureatlassprite.getU1();
        			        float potionV2 = textureatlassprite.getV1();
        			        RenderUtils.vtEntity(vertexIcon, matrix, minX + border, maxY - border + textOffset, labelZ + 0.002F, potionU1, potionV1, b1, b2, potionR, potionG, potionB, potionA);
        			        RenderUtils.vtEntity(vertexIcon, matrix, minX + border, minY + border + textOffset, labelZ + 0.002F, potionU1, potionV2, b1, b2, potionR, potionG, potionB, potionA);
        			        RenderUtils.vtEntity(vertexIcon, matrix, maxX - border, minY + border + textOffset, labelZ + 0.002F, potionU2, potionV2, b1, b2, potionR, potionG, potionB, potionA);
        			        RenderUtils.vtEntity(vertexIcon, matrix, maxX - border, maxY - border + textOffset, labelZ + 0.002F, potionU2, potionV1, b1, b2, potionR, potionG, potionB, potionA);
        			        poseStack.popPose();
        				}
        				
        				int index = 0;
        				for(String line : lines) {
        					poseStack.pushPose();
	        				float stringWidth = font.getSplitter().stringWidth(line);
	    					float scaleText = Math.min(180.0F / (float) (stringWidth + 10), 0.8F);
	        				float scale = scaleText / 180.0F;
	    					double renderY = 10.0F + (index * (10.0F * (1.0 - scale)));
							//poseStack.translate(0, 15.0F, 0);
							poseStack.scale(scale, scale, 1.0F);
							poseStack.translate(-stringWidth / 2.0F, 55.0F + renderY, 0);
							font.draw(poseStack, Component.literal(line), 0, 0, 0);
	    	                poseStack.popPose();
	    	                index++;
        				}
        				
    			        poseStack.popPose();
        			}
			        else {
			        	//Display Text
			        	String displayName = effect.getDisplayName().getString();
			        	List<String> lines = Lists.newArrayList();
			        	List<String> nameList = Arrays.<String>asList(displayName.split(" "));
			        	lines.addAll(nameList);
			        	
			        	Component numeral = Component.translatable("potion.potency." + effectInstance.getAmplifier());
			        	if(!numeral.getString().isEmpty()) {
			        		lines.add(numeral.getString());
			        	}
			        	
			        	if(!potion.hasInstantEffects()){
			        		lines.add(MobEffectUtil.formatDuration(effectInstance, 1.0F));
			        	} 
			        	
			        	float startY = 5.0F;
			        	float scale = 1.0F / 90.0F;
			            if(lines.size() > 3){
			            	scale = 1.0F / 100.0F;
			            	startY = 3.0F;
			            } else {
			            	startY += (3 - lines.size()) * 5.0F;
			            }
			            
			            poseStack.pushPose();
			            poseStack.translate(0.3 + 0.2, 0.6, labelZ + 0.001);
			        	poseStack.mulPose(Vector3f.XP.rotationDegrees(180.0F));
			        	
						poseStack.scale(scale, scale, 1.0F);
						@SuppressWarnings("resource")
						Font font = Minecraft.getInstance().font;
						int index = 0;
						for (String line : lines) {
							poseStack.pushPose();
				            float stringWidth = font.getSplitter().stringWidth(line);
							scale = Math.min(30.0F / (float) (stringWidth + 10), 0.8F);
							double renderY = startY + (index * (font.lineHeight * (1.0 - scale)));
							poseStack.translate(0, renderY, 0);
							poseStack.scale(scale, scale, 1.0F);
							poseStack.translate(-stringWidth / 2.0F, 0, 0);
							font.draw(poseStack, Component.literal(line), 0, index * font.lineHeight, 0);
							poseStack.popPose();
							
							index++;
						}
						poseStack.popPose();
			        }
        		}
        	}
        }
        else if(contents == JarContents.HONEY) {
        	String displayName = "Honey";
        	float scale = 1.0F / 90.0F;
        	@SuppressWarnings("resource")
			Font font = Minecraft.getInstance().font;
        	float startY = 15.0F;
        	poseStack.pushPose();            
        	poseStack.translate(0.3 + 0.2, 0.6, labelZ + 0.001);
        	poseStack.mulPose(Vector3f.XP.rotationDegrees(180.0F));
			poseStack.scale(scale, scale, 1.0F);
			
			
			poseStack.pushPose();
            float stringWidth = font.getSplitter().stringWidth(displayName);
			scale = Math.min(30.0F / (float) (stringWidth + 10), 0.8F);
			double renderY = startY;
			poseStack.translate(0, renderY, 0);
			poseStack.scale(scale, scale, 1.0F);
			poseStack.translate(-stringWidth / 2.0F, 0, 0);
			font.draw(poseStack, Component.literal(displayName), 0, 0, 0);
			poseStack.popPose();
			
			poseStack.popPose();
        }
        
    	poseStack.popPose();
    }
    
	public static Vector3f getColorFromPotion(Potion type){
		if (type.getEffects().isEmpty())
		{
			return null;
		}
		else
		{
			float f = 0.0F;
			float f1 = 0.0F;
			float f2 = 0.0F;
			int j = 0;

			for (MobEffectInstance potioneffect : type.getEffects())
			{
				if (potioneffect.isVisible())
				{
					int k = potioneffect.getEffect().getColor();
					int l = potioneffect.getAmplifier() + 1;
					f += (float)(l * (k >> 16 & 255)) / 255.0F;
					f1 += (float)(l * (k >> 8 & 255)) / 255.0F;
					f2 += (float)(l * (k >> 0 & 255)) / 255.0F;
					j += l;
				}
			}

			if (j == 0)
			{
				return new Vector3f(0, 0, 0);
			}
			else
			{
				f = f / (float)j * 255.0F;
				f1 = f1 / (float)j * 255.0F;
				f2 = f2 / (float)j * 255.0F;
				return new Vector3f(f, f1, f2);
			}
		}
	}
	
}
