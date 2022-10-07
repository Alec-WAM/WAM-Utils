package alec_wam.wam_utils.blocks.advanced_spawner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;

import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;

public class AdvancedSpawnerRenderer implements BlockEntityRenderer<AdvancedSpawnerBE> {
	private final EntityRenderDispatcher entityRenderer;
	
    public AdvancedSpawnerRenderer(BlockEntityRendererProvider.Context context) {
    	this.entityRenderer = context.getEntityRenderer();
    }

    @Override
    public void render(AdvancedSpawnerBE spawner, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {

         // Always remember to push the current transformation so that you can restore it later
        poseStack.pushPose();

        poseStack.translate(0.5D, 0.0D, 0.5D);
        Entity entity = spawner.clientRenderEntity;
        if (entity != null) {
           float f = 0.53125F;
           float f1 = Math.max(entity.getBbWidth(), entity.getBbHeight());
           if ((double)f1 > 1.0D) {
              f /= f1;
           }

           poseStack.translate(0.0D, (double)(0.4f * f), 0.0D);
           //poseStack.translate(0.0D, (double)0.4F, 0.0D);
           //poseStack.mulPose(Vector3f.YP.rotationDegrees((float)Mth.lerp((double)p_112564_, basespawner.getoSpin(), basespawner.getSpin()) * 10.0F));
           //poseStack.translate(0.0D, (double)-0.2F, 0.0D);
           //poseStack.mulPose(Vector3f.XP.rotationDegrees(-30.0F));
           poseStack.scale(f, f, f);
           float ticks = Minecraft.getInstance().getFrameTime();
           this.entityRenderer.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, ticks, poseStack, bufferSource, combinedLight);
        }
        poseStack.popPose();
    }

    public static void register() {
        BlockEntityRenderers.register(BlockInit.ADVANCED_SPAWNER_BE.get(), AdvancedSpawnerRenderer::new);
    }

}
