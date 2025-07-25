package alec_wam.wam_utils.client;

import alec_wam.wam_utils.WAMUtils;
import alec_wam.wam_utils.client.model.WorkerModel;
import alec_wam.wam_utils.client.render.blockentities.ConveyorBeltBERenderer;
import alec_wam.wam_utils.client.render.blockentities.ShieldRackBERenderer;
import alec_wam.wam_utils.client.render.entities.RenderHelper;
import alec_wam.wam_utils.client.render.entities.WorkerEntityRenderer;
import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.network.ClientPayloadHandler;
import alec_wam.wam_utils.network.SyncWorkerFishingPayload;
import alec_wam.wam_utils.network.SyncWorkerJobPayload;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = WAMUtils.MODID, value = Dist.CLIENT)
public class ModClientInit {

	public static final ModelLayerLocation WORKER_MODEL = new ModelLayerLocation(WAMUtils.prefix("worker"), "worker");
	public static final ModelLayerLocation WORKER_SLIM_MODEL = new ModelLayerLocation(WAMUtils.prefix("worker_slim"), "worker_slim");
	
	@SubscribeEvent
	public static void init(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
        	WorkerEntityRenderer.register();
        });
	}
	
	@SubscribeEvent
	public static void registerLayers(RegisterLayerDefinitions event) {
		event.registerLayerDefinition(WORKER_MODEL, () -> LayerDefinition.create(WorkerModel.createMesh(CubeDeformation.NONE, false).apply(HumanoidModel.BABY_TRANSFORMER), 64, 64));
	}
    
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModInit.CONVEYOR_BELT_BLOCK_ENTITY.get(),
                ConveyorBeltBERenderer::new
        );
        event.registerBlockEntityRenderer(
                ModInit.SHIELDRACK_BLOCK_ENTITY.get(),
                ShieldRackBERenderer::new
        );
    }

    @SuppressWarnings("deprecation")
	@SubscribeEvent
    public static void onTexturesStitched(final TextureAtlasStitchedEvent event) {
        if (event.getAtlas().location().equals(TextureAtlas.LOCATION_BLOCKS)) {
            RenderHelper.captureDummySprite(event.getAtlas());
        }
    }

    
    @SubscribeEvent
    public static void registerClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(
            SyncWorkerJobPayload.TYPE,
            ClientPayloadHandler::handleSyncWorkerJobOnMain
        );
        event.register(
            SyncWorkerFishingPayload.TYPE,
            ClientPayloadHandler::handleSyncWorkerFishingOnMain
        );
    }
	
}
