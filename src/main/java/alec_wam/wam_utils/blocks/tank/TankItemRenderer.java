package alec_wam.wam_utils.blocks.tank;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.capabilities.ItemFluidStorage;
import alec_wam.wam_utils.client.RenderUtils;
import alec_wam.wam_utils.client.WAMUtilsCustomItemRenderer;
import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fluids.FluidStack;

public class TankItemRenderer extends WAMUtilsCustomItemRenderer {

//	public static final PiglinTradingPodItemRenderer RENDERER = new PiglinTradingPodItemRenderer();
	
	public static final Supplier<BlockEntityWithoutLevelRenderer> RENDERER = Suppliers.memoize(
			() -> new TankItemRenderer()
	);
	
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
		
	}

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
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(BlockInit.TANK_BLOCK.get().defaultBlockState());
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
        	CompoundTag tag = stack.getTag();
        	if(tag.contains("Fluid")) {
        		FluidStack fluidStack = FluidStack.loadFluidStackFromNBT(tag.getCompound(ItemFluidStorage.NBT_FLUID));
        		Fluid fluid = fluidStack.getFluid();
            	float renderHeight = (float)fluidStack.getAmount() / (float)TankBE.FLUID_CAPACITY;
        		
            	matrix.pushPose();
            	IClientFluidTypeExtensions attributes = IClientFluidTypeExtensions.of(fluid);
                int color = attributes.getTintColor(fluidStack);
                int luminosity = fluid.getFluidType().getLightLevel(fluidStack);
                int packed = LightTexture.pack(Math.max(luminosity, light), 0);
                Set<Direction> dirs = Arrays.stream(Direction.values()).collect(Collectors.toCollection(() -> EnumSet.noneOf(Direction.class)));
            	for (RenderType renderType : RenderType.chunkBufferLayers()) {
            		if (ItemBlockRenderTypes.getRenderLayer(fluid.defaultFluidState()) == renderType) {
                    	RenderUtils.renderFluid(renderHeight, color, renderer.getBuffer(renderType), fluid, dirs, packed, false, false, matrix);        			
            		}
            	}
		        
		        matrix.popPose();
        	}
        }
        
		matrix.popPose();
	}

}
