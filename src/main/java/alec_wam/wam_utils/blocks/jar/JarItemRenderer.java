package alec_wam.wam_utils.blocks.jar;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.Vector3f;

import alec_wam.wam_utils.blocks.jar.JarBE.JarContents;
import alec_wam.wam_utils.client.RenderUtils;
import alec_wam.wam_utils.client.WAMUtilsCustomItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShulkerBulletModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class JarItemRenderer extends WAMUtilsCustomItemRenderer {

	public static final Supplier<BlockEntityWithoutLevelRenderer> RENDERER = Suppliers.memoize(
			() -> new JarItemRenderer()
	);
	
	public static final IClientItemExtensions CLIENT_RENDERER = new IClientItemExtensions()
	{
		@Override
		public BlockEntityWithoutLevelRenderer getCustomRenderer()
		{
			return RENDERER.get();
		}
	};
	
	private ShulkerBulletModel<ShulkerBullet> bulletModel;	
	@Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
		this.bulletModel = new ShulkerBulletModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHULKER_BULLET));		
	}

	private final Vector3f WHITE_COLOR = new Vector3f(255.0F, 255.0F, 255.0F);
	@Override
	public void renderByItem(ItemStack stack, TransformType transformType, PoseStack matrix, MultiBufferSource renderer,
			int light, int overlayLight) {
		matrix.pushPose();
		
        boolean flag1;
        if (transformType != ItemTransforms.TransformType.GUI && !transformType.firstPerson() && stack.getItem() instanceof BlockItem) {
           Block block = ((BlockItem)stack.getItem()).getBlock();
           flag1 = !(block instanceof HalfTransparentBlock) && !(block instanceof StainedGlassPaneBlock);
        } else {
           flag1 = true;
        }
        matrix.pushPose();
        Block block = ((BlockItem)stack.getItem()).getBlock();
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(block.defaultBlockState());        
        for(RenderType type : model.getRenderTypes(stack, flag1)) {
        	VertexConsumer vertexconsumer;
        	if (flag1) {
                vertexconsumer = ItemRenderer.getFoilBufferDirect(renderer, type, true, stack.hasFoil());
             } else {
                vertexconsumer = ItemRenderer.getFoilBuffer(renderer, type, true, stack.hasFoil());
             }
        	Minecraft.getInstance().getItemRenderer().renderModelLists(model, stack, light, OverlayTexture.NO_OVERLAY, matrix, vertexconsumer);
        }
        matrix.popPose();
        
        if(stack.hasTag()) {	        
	        JarContents contents = JarContents.EMPTY;
	        CompoundTag nbt = stack.getTag();
	        if(nbt.contains("JarContents")) {
	        	contents = JarContents.values()[nbt.getInt("JarContents")];
			}
	        Potion potion = null;
	        if(nbt.contains("Potion")) {
				potion = Potion.byName(nbt.getString("Potion"));
	        }
	        
	        float min = 3.0F / 16.0F;
	    	float max = 13.0F / 16.0F;
			matrix.pushPose();	        
	        if(contents == JarContents.POTION) {	        	
				int bottleCount = nbt.getInt("BottleCount");
				if(potion !=null && potion != Potions.EMPTY && bottleCount > 0) {
		        	float renderHeight = ((float)bottleCount / (float)JarBE.BOTTLE_CAPACITY) * (13.0F / 16.0F);
		        	RenderUtils.renderFakeFluid(JarBERenderer.POTION_TEXTURE, min, renderHeight, max, JarBERenderer.getColorFromPotion(potion), 1.0F, renderer.getBuffer(RenderType.translucent()), light, matrix);
		        }
	        }
	        else if(contents == JarContents.HONEY) {
	        	int bottleCount = nbt.getInt("BottleCount");
				if(bottleCount > 0) {
		        	float renderHeight = ((float)bottleCount / (float)JarBE.BOTTLE_CAPACITY) * (13.0F / 16.0F);
		        	RenderUtils.renderFakeFluid(JarBERenderer.HONEY_TEXTURE, min, renderHeight, max, WHITE_COLOR, 1.0F, renderer.getBuffer(RenderType.solid()), light, matrix);
		        }
	        }
	        else if(contents == JarContents.SHULKER) {
	        	matrix.pushPose();	
	        	matrix.translate(0.5D, 0.25D, 0.5D);
	        	JarBERenderer.renderShulkerBullet(bulletModel, 0, matrix, renderer, 0.0F, light);	
	        	matrix.popPose();
	        }
	        else if(contents == JarContents.LIGHTNING) {
	        	int bottleCount = nbt.getInt("BottleCount");
				if(bottleCount > 0) {
		        	float renderHeight = ((float)bottleCount / (float)JarBE.LIGHTNING_CAPACITY) * (13.0F / 16.0F);
		        	VertexConsumer vertex = VertexMultiConsumer.create(renderer.getBuffer(RenderType.glint()), renderer.getBuffer(RenderType.translucent()));
		        	RenderUtils.renderFakeFluid(JarBERenderer.POTION_TEXTURE, min, renderHeight, max, WHITE_COLOR, 1.0F, vertex, light, matrix);
				}
			}
	        
	        for(Direction dir : Direction.Plane.HORIZONTAL){
				if(nbt.contains("Label."+dir.getName().toUpperCase())){
					if(nbt.getBoolean("Label."+dir.getName().toUpperCase())) {
						JarBERenderer.renderLabel(matrix, renderer, dir, light, contents, potion);
					}
				}
        	}
	        
			matrix.popPose();
        }
		
		matrix.popPose();
	}

}
