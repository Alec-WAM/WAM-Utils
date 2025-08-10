package alec_wam.wam_utils.client;

import alec_wam.wam_utils.WAMUtils;
import alec_wam.wam_utils.client.model.EnchantedBookModel;
import alec_wam.wam_utils.client.model.WorkerModel;
import alec_wam.wam_utils.client.render.blockentities.ConveyorBeltBERenderer;
import alec_wam.wam_utils.client.render.blockentities.EnchantmentBookshelfBERenderer;
import alec_wam.wam_utils.client.render.blockentities.ShieldRackBERenderer;
import alec_wam.wam_utils.client.render.entities.WorkerEntityRenderer;
import alec_wam.wam_utils.client.util.RenderHelper;
import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.blocks.auto_trader.menu.AutoTraderScreen;
import alec_wam.wam_utils.common.blocks.creative.item_stock.menu.CreativeStockItemScreen;
import alec_wam.wam_utils.common.blocks.enchantment.bookshelf.menu.EnchantmentBookshelfScreen;
import alec_wam.wam_utils.common.blocks.enchantment.indexer.menu.EnchantmentIndexerScreen;
import alec_wam.wam_utils.common.entities.workers.menu.WorkerInventoryScreen;
import alec_wam.wam_utils.network.BaseBEMessagePayload;
import alec_wam.wam_utils.network.ClientPayloadHandler;
import alec_wam.wam_utils.network.SyncClientShelfItemsPayload;
import alec_wam.wam_utils.network.SyncWorkerFishingPayload;
import alec_wam.wam_utils.network.SyncWorkerFoodDataPayload;
import alec_wam.wam_utils.network.SyncWorkerJobPayload;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.LayerDefinitions;
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
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = WAMUtils.MODID, value = Dist.CLIENT)
public class ModClientInit {

	public static final ModelLayerLocation WORKER_MODEL = new ModelLayerLocation(WAMUtils.prefix("worker"), "main");
    public static final ModelLayerLocation WORKER_MODEL_INNER_ARMOR = new ModelLayerLocation(WAMUtils.prefix("worker"), "inner_armor");
    public static final ModelLayerLocation WORKER_MODEL_OUTER_ARMOR = new ModelLayerLocation(WAMUtils.prefix("worker"), "outer_armor");
	public static final ModelLayerLocation WORKER_SLIM_MODEL = new ModelLayerLocation(WAMUtils.prefix("worker_slim"), "main");
    public static final ModelLayerLocation WORKER_SLIM_MODEL_INNER_ARMOR = new ModelLayerLocation(WAMUtils.prefix("worker_slim"), "inner_armor");
    public static final ModelLayerLocation WORKER_SLIM_MODEL_OUTER_ARMOR = new ModelLayerLocation(WAMUtils.prefix("worker_slim"), "outer_armor");
	
    public static final ModelLayerLocation BOOK_LAYER = new ModelLayerLocation(WAMUtils.prefix("book"), "main");
    

	@SubscribeEvent
	public static void init(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
        	WorkerEntityRenderer.register();
        });
	}
	
	@SubscribeEvent
	public static void registerLayers(RegisterLayerDefinitions event) {
		event.registerLayerDefinition(WORKER_MODEL, () -> LayerDefinition.create(WorkerModel.createMesh(CubeDeformation.NONE, false), 64, 64).apply(HumanoidModel.BABY_TRANSFORMER));
        event.registerLayerDefinition(WORKER_MODEL_INNER_ARMOR, () -> LayerDefinition.create(HumanoidArmorModel.createBodyLayer(LayerDefinitions.INNER_ARMOR_DEFORMATION), 64, 32).apply(HumanoidModel.BABY_TRANSFORMER));
        event.registerLayerDefinition(WORKER_MODEL_OUTER_ARMOR, () -> LayerDefinition.create(HumanoidArmorModel.createBodyLayer(LayerDefinitions.OUTER_ARMOR_DEFORMATION), 64, 32).apply(HumanoidModel.BABY_TRANSFORMER));
        event.registerLayerDefinition(WORKER_SLIM_MODEL, () -> LayerDefinition.create(WorkerModel.createMesh(CubeDeformation.NONE, true), 64, 64).apply(HumanoidModel.BABY_TRANSFORMER));
	    event.registerLayerDefinition(WORKER_SLIM_MODEL_INNER_ARMOR, () -> LayerDefinition.create(HumanoidArmorModel.createBodyLayer(LayerDefinitions.INNER_ARMOR_DEFORMATION), 64, 32).apply(HumanoidModel.BABY_TRANSFORMER));
        event.registerLayerDefinition(WORKER_SLIM_MODEL_OUTER_ARMOR, () -> LayerDefinition.create(HumanoidArmorModel.createBodyLayer(LayerDefinitions.OUTER_ARMOR_DEFORMATION), 64, 32).apply(HumanoidModel.BABY_TRANSFORMER));
    
        event.registerLayerDefinition(BOOK_LAYER, () -> EnchantedBookModel.createBodyLayer());
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
        event.registerBlockEntityRenderer(
                ModInit.ENCHANTMENT_BOOK_SHELF_BLOCK_ENTITY.get(),
                EnchantmentBookshelfBERenderer::new
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
        event.register(
            SyncWorkerFoodDataPayload.TYPE,
            ClientPayloadHandler::handleSyncWorkerFoodDataOnMain
        );
        event.register(
            BaseBEMessagePayload.TYPE,
            ClientPayloadHandler::handleBaseBEMessageOnMain
        );
        event.register(
            SyncClientShelfItemsPayload.TYPE,
            ClientPayloadHandler::handleClientShelfItemMessageOnMain
        );
    }

    @SubscribeEvent // on the mod event bus only on the physical client
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModInit.WORKER_INVENTORY_MENU_TYPE.get(), WorkerInventoryScreen::new);
        event.register(ModInit.ENCHANTMENT_BOOK_SHELF_MENU_TYPE.get(), EnchantmentBookshelfScreen::new);
        event.register(ModInit.ENCHANTMENT_INDEXER_MENU_TYPE.get(), EnchantmentIndexerScreen::new);
        event.register(ModInit.VILLAGER_AUTO_TRADER_MENU_TYPE.get(), AutoTraderScreen::new);
        event.register(ModInit.CREATIVE_STOCKER_ITEM_BLOCK_MENU_TYPE.get(), CreativeStockItemScreen::new);
    }
	
}
