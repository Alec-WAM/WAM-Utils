package alec_wam.wam_utils.blocks.tank;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.client.RenderUtils;
import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

public class TankBERenderer implements BlockEntityRenderer<TankBE> {
	public TankBERenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TankBE tank, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {

         // Always remember to push the current transformation so that you can restore it later
        poseStack.pushPose();	
        FluidStack fluidStack = tank.fluidStorage.getFluid();
        if(!fluidStack.isEmpty()) {
        	Fluid fluid = fluidStack.getFluid();
        	float renderHeight = (float)fluidStack.getAmount() / (float)TankBE.FLUID_CAPACITY;
        	final BlockPos pos = tank.getBlockPos();
        	Set<Direction> dirs = Arrays.stream(Direction.values()).filter(dir -> {
                BlockEntity tile = tank.getLevel().getBlockEntity(pos.relative(dir));
                return !(tile instanceof TankBE && ((TankBE) tile).fluidStorage.getFluid().getFluid() == fluid);
            }).collect(Collectors.toCollection(() -> EnumSet.noneOf(Direction.class)));
        	
        	boolean tankAbove = false;
        	boolean tankBelow = false;
        	BlockEntity beUp = tank.getLevel().getBlockEntity(pos.relative(Direction.UP));
        	BlockEntity beDown = tank.getLevel().getBlockEntity(pos.relative(Direction.DOWN));
        	if(beUp !=null && beUp instanceof TankBE) {
        		tankAbove = true;
        	}
        	if(beDown !=null && beDown instanceof TankBE) {
        		tankBelow = true;
        	}
        	
        	IClientFluidTypeExtensions attributes = IClientFluidTypeExtensions.of(fluid);
            int color = attributes.getTintColor(fluid.defaultFluidState(), tank.getLevel(), tank.getBlockPos());
            int luminosity = fluid.getFluidType().getLightLevel(fluidStack);
//            int block = LightTexture.block(combinedLightIn);
            
            int packed = LightTexture.pack(Math.max(luminosity, combinedLight), 0);
        	for (RenderType renderType : RenderType.chunkBufferLayers()) {
        		if (ItemBlockRenderTypes.getRenderLayer(fluid.defaultFluidState()) == renderType) {
                	RenderUtils.renderFluid(renderHeight, color, bufferSource.getBuffer(renderType), fluid, dirs, packed, tankAbove, tankBelow, poseStack);        			
        		}
        	}
        }
        poseStack.popPose();
    }

    public static void register() {
        BlockEntityRenderers.register(BlockInit.TANK_BE.get(), TankBERenderer::new);
    }

}

