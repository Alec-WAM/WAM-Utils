package alec_wam.wam_utils.blocks.mob_statue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.client.WAMUtilsCustomItemRenderer;
import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class MobStatueItemRenderer extends WAMUtilsCustomItemRenderer {

	public static final Supplier<BlockEntityWithoutLevelRenderer> RENDERER = Suppliers.memoize(
			() -> new MobStatueItemRenderer()
	);
	
	public static final ResourceLocation MODEL_LOCATION = new ResourceLocation(WAMUtilsMod.MODID, "block/mob_statue");
	private Map<EntityType<?>, Entity> renderEntities = new HashMap<EntityType<?>, Entity>();
	private BakedModel baseModel;
	
	public static final IClientItemExtensions CLIENT_RENDERER = new IClientItemExtensions()
	{
		@Override
		public BlockEntityWithoutLevelRenderer getCustomRenderer()
		{
			return RENDERER.get();
		}
	};
	
	@Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
		baseModel = Minecraft.getInstance().getModelManager().getModel(MODEL_LOCATION);
	}

	@Override
	public void renderByItem(ItemStack stack, TransformType transformType, PoseStack matrix, MultiBufferSource renderer,
			int light, int overlayLight) {
		//WAMUtilsMod.LOGGER.debug("Rendering");
		matrix.pushPose();
		
		Entity mob = null;    
		float mobScale = 1.0F;
        Optional<EntityType<?>> entityType = MobStatueBE.loadTypeFromStatueItem(stack);  
        if(entityType.isPresent()) {
        	EntityType<?> type = entityType.get();
        	
        	if(renderEntities.containsKey(type)) {
        		mob = renderEntities.get(type);
        	}
        	else {
        		Entity entity = type.create(WAMUtilsMod.proxy.getClientWorld());
        		mob = entity;
        		renderEntities.put(type, entity);
        	}
        	
        	if(mob !=null) {
        		float scaleMax = Math.max(mob.getBbWidth(), mob.getBbHeight());
        		if ((double)scaleMax > 1.0D) {
        			mobScale /= scaleMax;
        		}
        		mobScale *= 0.9f;
        	}
        }
    	
		
        boolean flag1;
        if (transformType != ItemTransforms.TransformType.GUI && !transformType.firstPerson() && stack.getItem() instanceof BlockItem) {
           Block block = ((BlockItem)stack.getItem()).getBlock();
           flag1 = !(block instanceof HalfTransparentBlock) && !(block instanceof StainedGlassPaneBlock);
        } else {
           flag1 = true;
        }
        matrix.pushPose();
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(BlockInit.MOB_STATUE_BLOCK.get().defaultBlockState());
        
        for(RenderType type : model.getRenderTypes(stack, flag1)) {
        	VertexConsumer vertexconsumer;
        	if (flag1) {
                vertexconsumer = ItemRenderer.getFoilBufferDirect(renderer, type, true, stack.hasFoil());
             } else {
                vertexconsumer = ItemRenderer.getFoilBuffer(renderer, type, true, stack.hasFoil());
             }
        	float blockScale = mobScale;
        	matrix.translate(0.5, 0.0, 0.5);
        	matrix.scale(blockScale, blockScale, blockScale);    
        	matrix.translate(-0.5, 0.0, -0.5);
        	Minecraft.getInstance().getItemRenderer().renderModelLists(model, stack, light, OverlayTexture.NO_OVERLAY, matrix, vertexconsumer);
        }
        matrix.popPose();
        	
    	if(mob !=null) {
    		matrix.pushPose();
    		matrix.translate(0.5, 0.125D * mobScale, 0.5);

    		Direction dir = Direction.EAST;
    		if(transformType == TransformType.FIRST_PERSON_RIGHT_HAND || transformType == TransformType.THIRD_PERSON_RIGHT_HAND) {
    			dir = Direction.WEST;
    		}
    		if(transformType == TransformType.FIXED || transformType == TransformType.HEAD) {
    			dir = Direction.NORTH;
    		}
    		float f = -dir.toYRot();
    		matrix.mulPose(Vector3f.YP.rotationDegrees(f));

    		
    		matrix.scale(mobScale, mobScale, mobScale);    

    		Boolean oldValue = Minecraft.getInstance().getEntityRenderDispatcher().options.entityShadows().get();
    		Minecraft.getInstance().getEntityRenderDispatcher().options.entityShadows().set(Boolean.FALSE);
    		Minecraft.getInstance().getEntityRenderDispatcher().render(mob, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, matrix, renderer, light);
    		Minecraft.getInstance().getEntityRenderDispatcher().options.entityShadows().set(oldValue);
    		matrix.popPose();
    	}
        
		matrix.popPose();
	}

}
