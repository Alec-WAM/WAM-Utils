package alec_wam.wam_utils.init;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.advanced_beehive.AdvancedBeehiveScreen;
import alec_wam.wam_utils.blocks.advanced_portal.AdvancedPortalHostBERenderer;
import alec_wam.wam_utils.blocks.advanced_portal.AdvancedPortalHostScreen;
import alec_wam.wam_utils.blocks.advanced_spawner.AdvancedSpawnerRenderer;
import alec_wam.wam_utils.blocks.advanced_spawner.AdvancedSpawnerScreen;
import alec_wam.wam_utils.blocks.bookshelf.EnchantedBookModel;
import alec_wam.wam_utils.blocks.bookshelf.EnchantmentBookshelfBERenderer;
import alec_wam.wam_utils.blocks.bookshelf.EnchantmentBookshelfScreen;
import alec_wam.wam_utils.blocks.bookshelf.ScrollModel;
import alec_wam.wam_utils.blocks.entity_pod.piglin.PiglinTradingPodItemRenderer;
import alec_wam.wam_utils.blocks.entity_pod.piglin.PiglinTradingPodRenderer;
import alec_wam.wam_utils.blocks.entity_pod.piglin.PiglinTradingPodScreen;
import alec_wam.wam_utils.blocks.entity_pod.witch.WitchTradingPodItemRenderer;
import alec_wam.wam_utils.blocks.entity_pod.witch.WitchTradingPodRenderer;
import alec_wam.wam_utils.blocks.entity_pod.witch.WitchTradingPodScreen;
import alec_wam.wam_utils.blocks.fishing_net.FishingNetRenderer;
import alec_wam.wam_utils.blocks.generator.furnace.FurnaceGeneratorScreen;
import alec_wam.wam_utils.blocks.item_analyzer.ItemAnalyzerScreen;
import alec_wam.wam_utils.blocks.jar.JarBERenderer;
import alec_wam.wam_utils.blocks.jar.JarItemRenderer;
import alec_wam.wam_utils.blocks.machine.auto_animal_farmer.AutoAnimalFarmerBERenderer;
import alec_wam.wam_utils.blocks.machine.auto_animal_farmer.AutoAnimalFarmerScreen;
import alec_wam.wam_utils.blocks.machine.auto_breeder.AutoBreederBERenderer;
import alec_wam.wam_utils.blocks.machine.auto_breeder.AutoBreederScreen;
import alec_wam.wam_utils.blocks.machine.auto_butcher.AutoButcherBERenderer;
import alec_wam.wam_utils.blocks.machine.auto_butcher.AutoButcherScreen;
import alec_wam.wam_utils.blocks.machine.auto_compactor.AutoCompactorScreen;
import alec_wam.wam_utils.blocks.machine.auto_farmer.AutoFarmerScreen;
import alec_wam.wam_utils.blocks.machine.auto_fisher.AutoFisherScreen;
import alec_wam.wam_utils.blocks.machine.auto_lumberjack.AutoLumberjackScreen;
import alec_wam.wam_utils.blocks.machine.enchantment_remover.EnchantmentRemoverScreen;
import alec_wam.wam_utils.blocks.machine.flower_generator.FlowerSimulatorScreen;
import alec_wam.wam_utils.blocks.machine.quarry.QuarryBERenderer;
import alec_wam.wam_utils.blocks.machine.quarry.QuarryScreen;
import alec_wam.wam_utils.blocks.machine.stone_factory.StoneFactoryScreen;
import alec_wam.wam_utils.blocks.mob_statue.MobStatueItemRenderer;
import alec_wam.wam_utils.blocks.mob_statue.MobStatueRenderer;
import alec_wam.wam_utils.blocks.mob_statue.MobStatueScreen;
import alec_wam.wam_utils.blocks.shield_rack.ShieldRackBERenderer;
import alec_wam.wam_utils.blocks.tank.TankBERenderer;
import alec_wam.wam_utils.blocks.tank.TankItemRenderer;
import alec_wam.wam_utils.blocks.xp_vacuum.XPVacuumBERenderer;
import alec_wam.wam_utils.blocks.xp_vacuum.XPVacuumScreen;
import alec_wam.wam_utils.client.GuiUtils;
import alec_wam.wam_utils.entities.chest_boats.EnderChestBoatRenderer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = WAMUtilsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientInit {

    @SuppressWarnings("removal")
	public static void init(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ContainerInit.ADVANCED_SPAWNER_CONTAINER.get(), AdvancedSpawnerScreen::new);
            MenuScreens.register(ContainerInit.FURNACE_GENERATOR_CONTAINER.get(), FurnaceGeneratorScreen::new);
            MenuScreens.register(ContainerInit.AUTO_ANIMAL_FARMER_CONTAINER.get(), AutoAnimalFarmerScreen::new);
            MenuScreens.register(ContainerInit.AUTO_FISHER_CONTAINER.get(), AutoFisherScreen::new);
            MenuScreens.register(ContainerInit.AUTO_BREEDER_CONTAINER.get(), AutoBreederScreen::new);
            MenuScreens.register(ContainerInit.AUTO_BUTCHER_CONTAINER.get(), AutoButcherScreen::new);
            MenuScreens.register(ContainerInit.AUTO_FARMER_CONTAINER.get(), AutoFarmerScreen::new);
            MenuScreens.register(ContainerInit.AUTO_LUMBERJACK_CONTAINER.get(), AutoLumberjackScreen::new);
            MenuScreens.register(ContainerInit.AUTO_COMPACTOR_CONTAINER.get(), AutoCompactorScreen::new);
            MenuScreens.register(ContainerInit.QUARRY_CONTAINER.get(), QuarryScreen::new);
            MenuScreens.register(ContainerInit.FLOWER_SIMULATOR_CONTAINER.get(), FlowerSimulatorScreen::new);
            MenuScreens.register(ContainerInit.STONE_FACTORY_CONTAINER.get(), StoneFactoryScreen::new);
            
            MenuScreens.register(ContainerInit.ENCHANTMENT_BOOKSHELF_CONTAINER.get(), EnchantmentBookshelfScreen::new);            
            MenuScreens.register(ContainerInit.ENCHANTMENT_REMOVER_CONTAINER.get(), EnchantmentRemoverScreen::new);
            
            MenuScreens.register(ContainerInit.PIGLIN_TRADING_POD_CONTAINER.get(), PiglinTradingPodScreen::new);            
            MenuScreens.register(ContainerInit.WITCH_TRADING_POD_CONTAINER.get(), WitchTradingPodScreen::new);            
            MenuScreens.register(ContainerInit.MOB_STATUE_CONTAINER.get(), MobStatueScreen::new);         
            MenuScreens.register(ContainerInit.ITEM_ANALYZER_CONTAINER.get(), ItemAnalyzerScreen::new);
            MenuScreens.register(ContainerInit.ADVANCED_BEEHIVE_CONTAINER.get(), AdvancedBeehiveScreen::new);
            
            MenuScreens.register(ContainerInit.XP_VACUUM_CONTAINER.get(), XPVacuumScreen::new);
            
            MenuScreens.register(ContainerInit.ADVANCED_PORTAL_HOST_CONTAINER.get(), AdvancedPortalHostScreen::new);
            
            AdvancedSpawnerRenderer.register();
            FishingNetRenderer.register(); 
            PiglinTradingPodRenderer.register(); 
            WitchTradingPodRenderer.register(); 
            MobStatueRenderer.register(); 
            TankBERenderer.register(); 
            JarBERenderer.register(); 
            ShieldRackBERenderer.register(); 
            AdvancedPortalHostBERenderer.register(); 
            
            AutoBreederBERenderer.register(); 
            AutoButcherBERenderer.register(); 
            AutoAnimalFarmerBERenderer.register(); 
            QuarryBERenderer.register(); 
            XPVacuumBERenderer.register(); 
            
            EnchantmentBookshelfBERenderer.register(); 
            
            EnderChestBoatRenderer.register();
            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.ADVANCED_SPAWNER_BLOCK.get(), RenderType.cutoutMipped());            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.FISHINGNET_BLOCK.get(), RenderType.cutoutMipped());          
            ItemBlockRenderTypes.setRenderLayer(BlockInit.PIGLIN_TRADING_POD_BLOCK.get(), RenderType.cutoutMipped());   
            ItemBlockRenderTypes.setRenderLayer(BlockInit.WITCH_TRADING_POD_BLOCK.get(), RenderType.cutoutMipped());       
            ItemBlockRenderTypes.setRenderLayer(BlockInit.TANK_BLOCK.get(), RenderType.cutoutMipped());         
            ItemBlockRenderTypes.setRenderLayer(BlockInit.WANDERING_TRADER_SIGN_BLOCK.get(), RenderType.cutoutMipped()); 
            ItemBlockRenderTypes.setRenderLayer(BlockInit.PILLAGER_SIGN_BLOCK.get(), RenderType.cutoutMipped()); 
            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.OAK_ENCHANTMENT_BOOKSHELF_BLOCK.get(), RenderType.cutoutMipped());             
            ItemBlockRenderTypes.setRenderLayer(BlockInit.SPRUCE_ENCHANTMENT_BOOKSHELF_BLOCK.get(), RenderType.cutoutMipped());            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.BIRCH_ENCHANTMENT_BOOKSHELF_BLOCK.get(), RenderType.cutoutMipped());            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.ACACIA_ENCHANTMENT_BOOKSHELF_BLOCK.get(), RenderType.cutoutMipped());               
            ItemBlockRenderTypes.setRenderLayer(BlockInit.JUNGLE_ENCHANTMENT_BOOKSHELF_BLOCK.get(), RenderType.cutoutMipped());         
            ItemBlockRenderTypes.setRenderLayer(BlockInit.DARK_OAK_ENCHANTMENT_BOOKSHELF_BLOCK.get(), RenderType.cutoutMipped());            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.CRIMSON_ENCHANTMENT_BOOKSHELF_BLOCK.get(), RenderType.cutoutMipped());            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.WARPED_ENCHANTMENT_BOOKSHELF_BLOCK.get(), RenderType.cutoutMipped());            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.MANGROVE_ENCHANTMENT_BOOKSHELF_BLOCK.get(), RenderType.cutoutMipped()); 
            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.OAK_JAR_BLOCK.get(), RenderType.cutoutMipped());             
            ItemBlockRenderTypes.setRenderLayer(BlockInit.SPRUCE_JAR_BLOCK.get(), RenderType.cutoutMipped());            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.BIRCH_JAR_BLOCK.get(), RenderType.cutoutMipped());            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.ACACIA_JAR_BLOCK.get(), RenderType.cutoutMipped());               
            ItemBlockRenderTypes.setRenderLayer(BlockInit.JUNGLE_JAR_BLOCK.get(), RenderType.cutoutMipped());         
            ItemBlockRenderTypes.setRenderLayer(BlockInit.DARK_OAK_JAR_BLOCK.get(), RenderType.cutoutMipped());            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.CRIMSON_JAR_BLOCK.get(), RenderType.cutoutMipped());            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.WARPED_JAR_BLOCK.get(), RenderType.cutoutMipped());            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.MANGROVE_JAR_BLOCK.get(), RenderType.cutoutMipped()); 
            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.OAK_SHIELD_RACK_BLOCK.get(), RenderType.cutoutMipped());             
            ItemBlockRenderTypes.setRenderLayer(BlockInit.SPRUCE_SHIELD_RACK_BLOCK.get(), RenderType.cutoutMipped());            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.BIRCH_SHIELD_RACK_BLOCK.get(), RenderType.cutoutMipped());            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.ACACIA_SHIELD_RACK_BLOCK.get(), RenderType.cutoutMipped());               
            ItemBlockRenderTypes.setRenderLayer(BlockInit.JUNGLE_SHIELD_RACK_BLOCK.get(), RenderType.cutoutMipped());         
            ItemBlockRenderTypes.setRenderLayer(BlockInit.DARK_OAK_SHIELD_RACK_BLOCK.get(), RenderType.cutoutMipped());            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.CRIMSON_SHIELD_RACK_BLOCK.get(), RenderType.cutoutMipped());            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.WARPED_SHIELD_RACK_BLOCK.get(), RenderType.cutoutMipped());            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.MANGROVE_SHIELD_RACK_BLOCK.get(), RenderType.cutoutMipped());   
            
            ItemBlockRenderTypes.setRenderLayer(BlockInit.ITEM_GRATE_BLOCK.get(), RenderType.cutoutMipped()); 
        });
    }
    
    /*@OnlyIn(Dist.CLIENT)
    public static IClientItemExtensions piglinPodRenderer() {
    	return new CustomItemRenderProperties(PiglinTradingPodItemRenderer.RENDERER);
    }*/
    
    public record CustomItemRenderProperties(BlockEntityWithoutLevelRenderer renderer) implements IClientItemExtensions {

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return renderer;
        }
    }
    
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    	//bindTileEntityRenderer(event, )
    }
    
    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
    	event.registerLayerDefinition(EnchantedBookModel.BOOK_LAYER, EnchantedBookModel::createBodyLayer);
    	event.registerLayerDefinition(ScrollModel.LAYER_LOCATION, ScrollModel::createBodyLayer);
    }
    
    @SuppressWarnings("deprecation")
	@SubscribeEvent
    public static void registerSprites(TextureStitchEvent.Pre event) {
    	WAMUtilsMod.LOGGER.debug("Registered Sprites: " + event.getAtlas().location());
    	if(event.getAtlas().location().equals(TextureAtlas.LOCATION_BLOCKS)) {
    		WAMUtilsMod.LOGGER.debug("Registered Slot Sprites");
    		event.addSprite(GuiUtils.EMPTY_SLOT_BOOK);    		
    		
    		WAMUtilsMod.LOGGER.debug("Registered Entity Sprites");
    		event.addSprite(EnchantmentBookshelfBERenderer.BOOK_TEXTURE);
    		event.addSprite(EnchantmentBookshelfBERenderer.ENCHANTED_BOOK_TEXTURE);
    		event.addSprite(EnchantmentBookshelfBERenderer.SCROLL_TEXTURE);
    		
    		WAMUtilsMod.LOGGER.debug("Registered Block Sprites");
    		event.addSprite(JarBERenderer.POTION_TEXTURE);
    		event.addSprite(AdvancedPortalHostBERenderer.ADVANCED_PORTAL_TEXTURE);
    	}
    }
    
    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
    	WAMUtilsMod.LOGGER.debug("Registered Model");
    	event.register(PiglinTradingPodItemRenderer.MODEL_LOCATION);
    }
    
    @SubscribeEvent
    public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
    	event.registerReloadListener(PiglinTradingPodItemRenderer.RENDERER.get());
    	event.registerReloadListener(WitchTradingPodItemRenderer.RENDERER.get());
    	event.registerReloadListener(MobStatueItemRenderer.RENDERER.get());
    	event.registerReloadListener(TankItemRenderer.RENDERER.get());
    	event.registerReloadListener(JarItemRenderer.RENDERER.get());
    }
    
}
