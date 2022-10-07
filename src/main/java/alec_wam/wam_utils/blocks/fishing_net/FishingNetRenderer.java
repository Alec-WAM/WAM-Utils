package alec_wam.wam_utils.blocks.fishing_net;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;

import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandler;

@OnlyIn(Dist.CLIENT)
public class FishingNetRenderer implements BlockEntityRenderer<FishingNetBE> {
   private final ItemRenderer itemRenderer;
   private final BlockRenderDispatcher blockRenderer;

   public FishingNetRenderer(BlockEntityRendererProvider.Context p_173602_) {
      this.itemRenderer = p_173602_.getItemRenderer();
      this.blockRenderer = p_173602_.getBlockRenderDispatcher();
   }

   @Override
   public void render(FishingNetBE fishingNet, float p_112345_, PoseStack p_112346_, MultiBufferSource p_112347_, int p_112348_, int p_112349_) {
      Direction direction = fishingNet.getBlockState().getValue(FishingNetBlock.FACING);
      int i = (int)fishingNet.getBlockPos().asLong();
      
      IItemHandler handler = fishingNet.fishItems;
      if(handler != null) {
	      for(int j = 0; j < handler.getSlots(); ++j) {
	         ItemStack itemstack = handler.getStackInSlot(j);
	         if (itemstack != ItemStack.EMPTY) {
	            p_112346_.pushPose();
	            p_112346_.translate(0.5D, 0.05/*0.44921875D*/, 0.5D);
	            Direction direction1 = Direction.from2DDataValue((j + direction.get2DDataValue()) % 4);
	            float f = -direction1.toYRot();
	            p_112346_.mulPose(Vector3f.YP.rotationDegrees(f));
	            p_112346_.mulPose(Vector3f.XP.rotationDegrees(90.0F));
	            double offset = 0.225D;
	            //0.3125D
	            p_112346_.translate(-offset, -offset, 0.0D);
	            p_112346_.scale(0.375F, 0.375F, 0.375F);
	            this.itemRenderer.renderStatic(itemstack, ItemTransforms.TransformType.FIXED, p_112348_, p_112349_, p_112346_, p_112347_, i + j);
	            p_112346_.popPose();
	         }
	      }
      }
      
//      p_112346_.pushPose();
//      p_112346_.translate(0.0D, 0.01D, 0.0D);
//      BlockState state = fishingNet.getBlockState();
//      float scale = 1.0f;
//      p_112346_.scale(scale, scale, scale);
//      RandomSource random = RandomSource.create();
//      random.setSeed(state.getSeed(fishingNet.getBlockPos()));
//      VertexConsumer vertex = p_112347_.getBuffer(RenderType.glint());
//      //this.blockRenderer.renderSingleBlock(state, fishingNet.getBlockPos(), fishingNet.getLevel(), p_112346_, vertex, false, random);
//      this.blockRenderer.renderSingleBlock(state, p_112346_, p_112347_, p_112348_, OverlayTexture.NO_OVERLAY, net.minecraftforge.client.model.data.ModelData.EMPTY, RenderType.glint());
//      p_112346_.popPose();
      //RenderType.glintTranslucent();
   }

   public static void register() {
       BlockEntityRenderers.register(BlockInit.FISHINGNET_BE.get(), FishingNetRenderer::new);
   }
}
