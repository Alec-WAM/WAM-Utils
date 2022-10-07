package alec_wam.wam_utils.blocks.entity_pod.witch;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.client.WAMUtilsCustomItemRenderer;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class WitchTradingPodItemRenderer extends WAMUtilsCustomItemRenderer {

//	public static final PiglinTradingPodItemRenderer RENDERER = new PiglinTradingPodItemRenderer();
	
	public static final Supplier<BlockEntityWithoutLevelRenderer> RENDERER = Suppliers.memoize(
			() -> new WitchTradingPodItemRenderer()
	);
	
	public static final ResourceLocation MODEL_LOCATION = new ResourceLocation(WAMUtilsMod.MODID, "item/entity_pod_item");
	private Witch witch;
	private BakedModel podModel;
	
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
		podModel = Minecraft.getInstance().getModelManager().getModel(MODEL_LOCATION);
	}

	@Override
	public void renderByItem(ItemStack stack, TransformType transformType, PoseStack matrix, MultiBufferSource renderer,
			int light, int overlayLight) {
		//WAMUtilsMod.LOGGER.debug("Rendering");
		if(witch == null) {
			witch = new Witch(EntityType.WITCH, WAMUtilsMod.proxy.getClientWorld());
		}
		matrix.pushPose();
		
        boolean flag1;
        if (transformType != ItemTransforms.TransformType.GUI && !transformType.firstPerson() && stack.getItem() instanceof BlockItem) {
           Block block = ((BlockItem)stack.getItem()).getBlock();
           flag1 = !(block instanceof HalfTransparentBlock) && !(block instanceof StainedGlassPaneBlock);
        } else {
           flag1 = true;
        }
        matrix.pushPose();
        for(RenderType type : podModel.getRenderTypes(stack, flag1)) {
        	VertexConsumer vertexconsumer;
        	if (flag1) {
                vertexconsumer = ItemRenderer.getFoilBufferDirect(renderer, type, true, stack.hasFoil());
             } else {
                vertexconsumer = ItemRenderer.getFoilBuffer(renderer, type, true, stack.hasFoil());
             }
        	Minecraft.getInstance().getItemRenderer().renderModelLists(podModel, stack, light, OverlayTexture.NO_OVERLAY, matrix, vertexconsumer);
        }
        matrix.popPose();
        
        matrix.pushPose();
        matrix.translate(0.5, 0.05D, 0.5);
        float width = 0.75F;
    	float height = 0.75F;
    	Direction dir = Direction.EAST;
    	if(transformType == TransformType.FIRST_PERSON_RIGHT_HAND || transformType == TransformType.THIRD_PERSON_RIGHT_HAND) {
    		dir = Direction.WEST;
    	}
    	if(transformType == TransformType.FIXED || transformType == TransformType.HEAD) {
    		dir = Direction.NORTH;
    	}
    	float f = -dir.toYRot();
    	matrix.mulPose(Vector3f.YP.rotationDegrees(f));
    	matrix.scale(width, height, width);    
        
        Minecraft.getInstance().getEntityRenderDispatcher().render(witch, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, matrix, renderer, light);
        matrix.popPose();
        
		matrix.popPose();
	}

}
