package alec_wam.wam_utils.entities.chest_boats;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;

import alec_wam.wam_utils.blocks.advanced_spawner.AdvancedSpawnerRenderer;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.EntityInit;

import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EnderChestBoatRenderer extends EntityRenderer<EnderChestBoat> {
   private final Map<Boat.Type, Pair<ResourceLocation, BoatModel>> boatResources;

   public EnderChestBoatRenderer(EntityRendererProvider.Context p_234563_) {
      super(p_234563_);
      this.shadowRadius = 0.8F;
      this.boatResources = Stream.of(Boat.Type.values()).collect(ImmutableMap.toImmutableMap((p_173938_) -> {
         return p_173938_;
      }, (p_234575_) -> {
         return Pair.of(new ResourceLocation(getTextureLocation(p_234575_)), this.createBoatModel(p_234563_, p_234575_));
      }));
   }

   private BoatModel createBoatModel(EntityRendererProvider.Context p_234569_, Boat.Type p_234570_) {
      ModelLayerLocation modellayerlocation = ModelLayers.createChestBoatModelName(p_234570_);
      return new BoatModel(p_234569_.bakeLayer(modellayerlocation), true);
   }

   private static String getTextureLocation(Boat.Type p_234566_) {
      return "wam_utils:textures/entities/enderchest_boat/" + p_234566_.getName() + ".png";
   }

   @Override
   public void render(EnderChestBoat p_113929_, float p_113930_, float p_113931_, PoseStack p_113932_, MultiBufferSource p_113933_, int p_113934_) {
      p_113932_.pushPose();
      p_113932_.translate(0.0D, 0.375D, 0.0D);
      p_113932_.mulPose(Vector3f.YP.rotationDegrees(180.0F - p_113930_));
      float f = (float)p_113929_.getHurtTime() - p_113931_;
      float f1 = p_113929_.getDamage() - p_113931_;
      if (f1 < 0.0F) {
         f1 = 0.0F;
      }

      if (f > 0.0F) {
         p_113932_.mulPose(Vector3f.XP.rotationDegrees(Mth.sin(f) * f * f1 / 10.0F * (float)p_113929_.getHurtDir()));
      }

      float f2 = p_113929_.getBubbleAngle(p_113931_);
      if (!Mth.equal(f2, 0.0F)) {
         p_113932_.mulPose(new Quaternion(new Vector3f(1.0F, 0.0F, 1.0F), p_113929_.getBubbleAngle(p_113931_), true));
      }

      Pair<ResourceLocation, BoatModel> pair = getModelWithLocation(p_113929_);
      ResourceLocation resourcelocation = pair.getFirst();
      BoatModel boatmodel = pair.getSecond();
      p_113932_.scale(-1.0F, -1.0F, 1.0F);
      p_113932_.mulPose(Vector3f.YP.rotationDegrees(90.0F));
      boatmodel.setupAnim(p_113929_, p_113931_, 0.0F, -0.1F, 0.0F, 0.0F);
      VertexConsumer vertexconsumer = p_113933_.getBuffer(boatmodel.renderType(resourcelocation));
      boatmodel.renderToBuffer(p_113932_, vertexconsumer, p_113934_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
      if (!p_113929_.isUnderWater()) {
         VertexConsumer vertexconsumer1 = p_113933_.getBuffer(RenderType.waterMask());
         boatmodel.waterPatch().render(p_113932_, vertexconsumer1, p_113934_, OverlayTexture.NO_OVERLAY);
      }

      p_113932_.popPose();
      super.render(p_113929_, p_113930_, p_113931_, p_113932_, p_113933_, p_113934_);
   }

   @Deprecated // forge: override getModelWithLocation to change the texture / model
   public ResourceLocation getTextureLocation(EnderChestBoat p_113927_) {
      return getModelWithLocation(p_113927_).getFirst();
   }

   public Pair<ResourceLocation, BoatModel> getModelWithLocation(Boat boat) { return this.boatResources.get(boat.getBoatType()); }

   public static void register() {
	   EntityRenderers.register(EntityInit.ENDER_CHEST_BOAT.get(), EnderChestBoatRenderer::new);
   }
}

