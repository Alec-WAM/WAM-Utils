package alec_wam.wam_utils.init;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.advanced_beehive.AdvancedBeehiveBE;
import alec_wam.wam_utils.blocks.advanced_beehive.AdvancedBeehiveBlock;
import alec_wam.wam_utils.blocks.advanced_portal.AdvancedPortalHostBE;
import alec_wam.wam_utils.blocks.advanced_portal.AdvancedPortalHostBlock;
import alec_wam.wam_utils.blocks.advanced_spawner.AdvancedSpawnerBE;
import alec_wam.wam_utils.blocks.advanced_spawner.AdvancedSpawnerBlock;
import alec_wam.wam_utils.blocks.bookshelf.EnchantmentBookshelfBE;
import alec_wam.wam_utils.blocks.bookshelf.EnchantmentBookshelfBlock;
import alec_wam.wam_utils.blocks.enchantment_indexer.EnchantmentIndexerBE;
import alec_wam.wam_utils.blocks.enchantment_indexer.EnchantmentIndexerBlock;
import alec_wam.wam_utils.blocks.entity_pod.AbstractEntityPodBE;
import alec_wam.wam_utils.blocks.entity_pod.AbstractEntityPodBlock;
import alec_wam.wam_utils.blocks.entity_pod.piglin.PiglinTradingPodBE;
import alec_wam.wam_utils.blocks.entity_pod.witch.WitchTradingPodBE;
import alec_wam.wam_utils.blocks.fishing_net.FishingNetBE;
import alec_wam.wam_utils.blocks.fishing_net.FishingNetBlock;
import alec_wam.wam_utils.blocks.generator.furnace.FurnaceGeneratorBE;
import alec_wam.wam_utils.blocks.generator.furnace.FurnaceGeneratorBlock;
import alec_wam.wam_utils.blocks.item_analyzer.ItemAnalyzerBE;
import alec_wam.wam_utils.blocks.item_analyzer.ItemAnalyzerBlock;
import alec_wam.wam_utils.blocks.item_grate.ItemGrateBlock;
import alec_wam.wam_utils.blocks.jar.JarBE;
import alec_wam.wam_utils.blocks.jar.JarBlock;
import alec_wam.wam_utils.blocks.machine.auto_animal_farmer.AutoAnimalFarmerBE;
import alec_wam.wam_utils.blocks.machine.auto_animal_farmer.AutoAnimalFarmerBlock;
import alec_wam.wam_utils.blocks.machine.auto_breeder.AutoBreederBE;
import alec_wam.wam_utils.blocks.machine.auto_breeder.AutoBreederBlock;
import alec_wam.wam_utils.blocks.machine.auto_butcher.AutoButcherBE;
import alec_wam.wam_utils.blocks.machine.auto_butcher.AutoButcherBlock;
import alec_wam.wam_utils.blocks.machine.auto_compactor.AutoCompactorBE;
import alec_wam.wam_utils.blocks.machine.auto_compactor.AutoCompactorBlock;
import alec_wam.wam_utils.blocks.machine.auto_farmer.AutoFarmerBE;
import alec_wam.wam_utils.blocks.machine.auto_farmer.AutoFarmerBlock;
import alec_wam.wam_utils.blocks.machine.auto_fisher.AutoFisherBE;
import alec_wam.wam_utils.blocks.machine.auto_fisher.AutoFisherBlock;
import alec_wam.wam_utils.blocks.machine.auto_lumberjack.AutoLumberjackBE;
import alec_wam.wam_utils.blocks.machine.auto_lumberjack.AutoLumberjackBlock;
import alec_wam.wam_utils.blocks.machine.enchantment_remover.EnchantmentRemoverBE;
import alec_wam.wam_utils.blocks.machine.enchantment_remover.EnchantmentRemoverBlock;
import alec_wam.wam_utils.blocks.machine.flower_generator.FlowerSimulatorBE;
import alec_wam.wam_utils.blocks.machine.flower_generator.FlowerSimulatorBlock;
import alec_wam.wam_utils.blocks.machine.quarry.QuarryBE;
import alec_wam.wam_utils.blocks.machine.quarry.QuarryBlock;
import alec_wam.wam_utils.blocks.machine.stone_factory.StoneFactoryBE;
import alec_wam.wam_utils.blocks.machine.stone_factory.StoneFactoryBlock;
import alec_wam.wam_utils.blocks.mirror_block.BlockMirrorBE;
import alec_wam.wam_utils.blocks.mirror_block.BlockMirrorBlock;
import alec_wam.wam_utils.blocks.mob_sign.MobSignBlock;
import alec_wam.wam_utils.blocks.mob_statue.MobStatueBE;
import alec_wam.wam_utils.blocks.mob_statue.MobStatueBlock;
import alec_wam.wam_utils.blocks.pylon.AbstractPylonBE;
import alec_wam.wam_utils.blocks.pylon.EarthPylonBE;
import alec_wam.wam_utils.blocks.pylon.EnderPylonBE;
import alec_wam.wam_utils.blocks.pylon.EvilPylonBE;
import alec_wam.wam_utils.blocks.pylon.PylonBlock;
import alec_wam.wam_utils.blocks.pylon.WaterPylonBE;
import alec_wam.wam_utils.blocks.shield_rack.ShieldRackBE;
import alec_wam.wam_utils.blocks.shield_rack.ShieldRackBlock;
import alec_wam.wam_utils.blocks.tank.TankBE;
import alec_wam.wam_utils.blocks.tank.TankBlock;
import alec_wam.wam_utils.blocks.xp_vacuum.XPVacuumBE;
import alec_wam.wam_utils.blocks.xp_vacuum.XPVacuumBlock;
import alec_wam.wam_utils.init.ItemInit.ModCreativeTab;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class BlockInit {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, WAMUtilsMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, WAMUtilsMod.MODID);

    public static final List<RegistryObject<Block>> autoItemBlockBlacklist = new ArrayList<RegistryObject<Block>>();
    public static final Map<RegistryObject<Block>, CreativeModeTab> autoItemBlockTabs = new HashMap<RegistryObject<Block>, CreativeModeTab>();
    
    //MACHINES
    public static final RegistryObject<Block> ADVANCED_SPAWNER_BLOCK = BLOCKS.register("advanced_spawner",
            () -> new AdvancedSpawnerBlock());
    public static final RegistryObject<BlockEntityType<AdvancedSpawnerBE>> ADVANCED_SPAWNER_BE = BLOCK_ENTITIES.register("advanced_spawner", () -> BlockEntityType.Builder.of(AdvancedSpawnerBE::new, ADVANCED_SPAWNER_BLOCK.get()).build(null));

    public static final RegistryObject<Block> FURNACE_GENERATOR_BLOCK = BLOCKS.register("furnace_generator",
            () -> new FurnaceGeneratorBlock());
    public static final RegistryObject<BlockEntityType<FurnaceGeneratorBE>> FURNACE_GENERATOR_BE = BLOCK_ENTITIES.register("furnace_generator", () -> BlockEntityType.Builder.of(FurnaceGeneratorBE::new, FURNACE_GENERATOR_BLOCK.get()).build(null));
    
    public static final RegistryObject<Block> AUTO_ANIMAL_FARMER = BLOCKS.register("auto_animal_farmer",
            () -> new AutoAnimalFarmerBlock());
    public static final RegistryObject<BlockEntityType<AutoAnimalFarmerBE>> AUTO_ANIMAL_FARMER_BE = BLOCK_ENTITIES.register("auto_animal_farmer", () -> BlockEntityType.Builder.of(AutoAnimalFarmerBE::new, AUTO_ANIMAL_FARMER.get()).build(null));
    
    public static final RegistryObject<Block> AUTO_FISHER_BLOCK = BLOCKS.register("auto_fisher",
            () -> new AutoFisherBlock());
    public static final RegistryObject<BlockEntityType<AutoFisherBE>> AUTO_FISHER_BE = BLOCK_ENTITIES.register("auto_fisher", () -> BlockEntityType.Builder.of(AutoFisherBE::new, AUTO_FISHER_BLOCK.get()).build(null));
    
    public static final RegistryObject<Block> AUTO_BREEDER_BLOCK = BLOCKS.register("auto_breeder",
            () -> new AutoBreederBlock());
    public static final RegistryObject<BlockEntityType<AutoBreederBE>> AUTO_BREEDER_BE = BLOCK_ENTITIES.register("auto_breeder", () -> BlockEntityType.Builder.of(AutoBreederBE::new, AUTO_BREEDER_BLOCK.get()).build(null));

    public static final RegistryObject<Block> AUTO_BUTCHER_BLOCK = BLOCKS.register("auto_butcher",
            () -> new AutoButcherBlock());
    public static final RegistryObject<BlockEntityType<AutoButcherBE>> AUTO_BUTCHER_BE = BLOCK_ENTITIES.register("auto_butcher", () -> BlockEntityType.Builder.of(AutoButcherBE::new, AUTO_BUTCHER_BLOCK.get()).build(null));

    public static final RegistryObject<Block> AUTO_FARMER_BLOCK = BLOCKS.register("auto_farmer",
            () -> new AutoFarmerBlock());
    public static final RegistryObject<BlockEntityType<AutoFarmerBE>> AUTO_FARMER_BE = BLOCK_ENTITIES.register("auto_farmer", () -> BlockEntityType.Builder.of(AutoFarmerBE::new, AUTO_FARMER_BLOCK.get()).build(null));

    public static final RegistryObject<Block> AUTO_LUMBERJACK_BLOCK = BLOCKS.register("auto_lumberjack",
            () -> new AutoLumberjackBlock());
    public static final RegistryObject<BlockEntityType<AutoLumberjackBE>> AUTO_LUMBERJACK_BE = BLOCK_ENTITIES.register("auto_lumberjack", () -> BlockEntityType.Builder.of(AutoLumberjackBE::new, AUTO_LUMBERJACK_BLOCK.get()).build(null));

    public static final RegistryObject<Block> AUTO_COMPACTOR_BLOCK = BLOCKS.register("auto_compactor",
            () -> new AutoCompactorBlock());
    public static final RegistryObject<BlockEntityType<AutoCompactorBE>> AUTO_COMPACTOR_BE = BLOCK_ENTITIES.register("auto_compactor", () -> BlockEntityType.Builder.of(AutoCompactorBE::new, AUTO_COMPACTOR_BLOCK.get()).build(null));
    
    public static final RegistryObject<Block> QUARRY_BLOCK = BLOCKS.register("quarry",
            () -> new QuarryBlock());
    public static final RegistryObject<BlockEntityType<QuarryBE>> QUARRY_BE = BLOCK_ENTITIES.register("quarry", () -> BlockEntityType.Builder.of(QuarryBE::new, QUARRY_BLOCK.get()).build(null));

    public static final RegistryObject<Block> FLOWER_SIMULATOR_BLOCK = BLOCKS.register("flower_simulator",
            () -> new FlowerSimulatorBlock());
    public static final RegistryObject<BlockEntityType<FlowerSimulatorBE>> FLOWER_SIMULATOR_BE = BLOCK_ENTITIES.register("flower_simulator", () -> BlockEntityType.Builder.of(FlowerSimulatorBE::new, FLOWER_SIMULATOR_BLOCK.get()).build(null));
    
    public static final RegistryObject<Block> STONE_FACTORY_BLOCK = BLOCKS.register("stone_factory",
            () -> new StoneFactoryBlock());
    public static final RegistryObject<BlockEntityType<StoneFactoryBE>> STONE_FACTORY_BE = BLOCK_ENTITIES.register("stone_factory", () -> BlockEntityType.Builder.of(StoneFactoryBE::new, STONE_FACTORY_BLOCK.get()).build(null));

    
    public static final RegistryObject<Block> ADVANCED_PORTAL_HOST_BLOCK = BLOCKS.register("advanced_portal_host",
            () -> new AdvancedPortalHostBlock());
    public static final RegistryObject<BlockEntityType<AdvancedPortalHostBE>> ADVANCED_PORTAL_HOST_BE = BLOCK_ENTITIES.register("advanced_portal_host", () -> BlockEntityType.Builder.of(AdvancedPortalHostBE::new, ADVANCED_PORTAL_HOST_BLOCK.get()).build(null));

    
    //PYLONS
    public static final RegistryObject<Block> EARTH_PYLON_BLOCK = BLOCKS.register("earth_pylon",
            () -> new PylonBlock() {

				@Override
				public AbstractPylonBE createPylonBlockEntity(BlockPos pos, BlockState state) {
					return new EarthPylonBE(pos, state);
				}
            	
            });
    public static final RegistryObject<BlockEntityType<EarthPylonBE>> EARTH_PYLON_BE = BLOCK_ENTITIES.register("earth_pylon", () -> BlockEntityType.Builder.of(EarthPylonBE::new, EARTH_PYLON_BLOCK.get()).build(null));
    public static final RegistryObject<Block> WATER_PYLON_BLOCK = BLOCKS.register("water_pylon",
            () -> new PylonBlock() {

				@Override
				public AbstractPylonBE createPylonBlockEntity(BlockPos pos, BlockState state) {
					return new WaterPylonBE(pos, state);
				}
            	
            });
    public static final RegistryObject<BlockEntityType<WaterPylonBE>> WATER_PYLON_BE = BLOCK_ENTITIES.register("water_pylon", () -> BlockEntityType.Builder.of(WaterPylonBE::new, WATER_PYLON_BLOCK.get()).build(null));
    public static final RegistryObject<Block> EVIL_PYLON_BLOCK = BLOCKS.register("evil_pylon",
            () -> new PylonBlock() {

				@Override
				public AbstractPylonBE createPylonBlockEntity(BlockPos pos, BlockState state) {
					return new EvilPylonBE(pos, state);
				}
            	
            });
    public static final RegistryObject<BlockEntityType<EvilPylonBE>> EVIL_PYLON_BE = BLOCK_ENTITIES.register("evil_pylon", () -> BlockEntityType.Builder.of(EvilPylonBE::new, EVIL_PYLON_BLOCK.get()).build(null));
    public static final RegistryObject<Block> ENDER_PYLON_BLOCK = BLOCKS.register("ender_pylon",
            () -> new PylonBlock() {

				@Override
				public AbstractPylonBE createPylonBlockEntity(BlockPos pos, BlockState state) {
					return new EnderPylonBE(pos, state);
				}
            	
            });
    public static final RegistryObject<BlockEntityType<EnderPylonBE>> ENDER_PYLON_BE = BLOCK_ENTITIES.register("ender_pylon", () -> BlockEntityType.Builder.of(EnderPylonBE::new, ENDER_PYLON_BLOCK.get()).build(null));

    //BOOKSHELVES
    public static final RegistryObject<Block> OAK_ENCHANTMENT_BOOKSHELF_BLOCK = BLOCKS.register("oak_enchantment_bookshelf",
            () -> new EnchantmentBookshelfBlock());
    public static final RegistryObject<Block> SPRUCE_ENCHANTMENT_BOOKSHELF_BLOCK = BLOCKS.register("spruce_enchantment_bookshelf",
            () -> new EnchantmentBookshelfBlock());
    public static final RegistryObject<Block> BIRCH_ENCHANTMENT_BOOKSHELF_BLOCK = BLOCKS.register("birch_enchantment_bookshelf",
            () -> new EnchantmentBookshelfBlock());
    public static final RegistryObject<Block> ACACIA_ENCHANTMENT_BOOKSHELF_BLOCK = BLOCKS.register("acacia_enchantment_bookshelf",
            () -> new EnchantmentBookshelfBlock());
    public static final RegistryObject<Block> JUNGLE_ENCHANTMENT_BOOKSHELF_BLOCK = BLOCKS.register("jungle_enchantment_bookshelf",
            () -> new EnchantmentBookshelfBlock());
    public static final RegistryObject<Block> DARK_OAK_ENCHANTMENT_BOOKSHELF_BLOCK = BLOCKS.register("dark_oak_enchantment_bookshelf",
            () -> new EnchantmentBookshelfBlock());
    public static final RegistryObject<Block> CRIMSON_ENCHANTMENT_BOOKSHELF_BLOCK = BLOCKS.register("crimson_enchantment_bookshelf",
            () -> new EnchantmentBookshelfBlock());
    public static final RegistryObject<Block> WARPED_ENCHANTMENT_BOOKSHELF_BLOCK = BLOCKS.register("warped_enchantment_bookshelf",
            () -> new EnchantmentBookshelfBlock());
    public static final RegistryObject<Block> MANGROVE_ENCHANTMENT_BOOKSHELF_BLOCK = BLOCKS.register("mangrove_enchantment_bookshelf",
            () -> new EnchantmentBookshelfBlock());
    public static final RegistryObject<BlockEntityType<EnchantmentBookshelfBE>> ENCHANTMENT_BOOKSHELF_BE = BLOCK_ENTITIES.register("enchantment_bookshelf", () -> BlockEntityType.Builder.of(EnchantmentBookshelfBE::new, 
    		OAK_ENCHANTMENT_BOOKSHELF_BLOCK.get(), SPRUCE_ENCHANTMENT_BOOKSHELF_BLOCK.get(), BIRCH_ENCHANTMENT_BOOKSHELF_BLOCK.get(), ACACIA_ENCHANTMENT_BOOKSHELF_BLOCK.get(), 
    		JUNGLE_ENCHANTMENT_BOOKSHELF_BLOCK.get(), DARK_OAK_ENCHANTMENT_BOOKSHELF_BLOCK.get(), CRIMSON_ENCHANTMENT_BOOKSHELF_BLOCK.get(), WARPED_ENCHANTMENT_BOOKSHELF_BLOCK.get(), 
    		MANGROVE_ENCHANTMENT_BOOKSHELF_BLOCK.get()
		).build(null));
    public static final RegistryObject<Block> ENCHANTMENT_INDEXER_BLOCK = BLOCKS.register("enchantment_indexer",
            () -> new EnchantmentIndexerBlock());
    public static final RegistryObject<BlockEntityType<EnchantmentIndexerBE>> ENCHANTMENT_INDEXER_BE = BLOCK_ENTITIES.register("enchantment_indexer", () -> BlockEntityType.Builder.of(EnchantmentIndexerBE::new, ENCHANTMENT_INDEXER_BLOCK.get()).build(null));
    
    public static final RegistryObject<Block> ENCHANTMENT_REMOVER_BLOCK = BLOCKS.register("enchantment_remover",
            () -> new EnchantmentRemoverBlock());
    public static final RegistryObject<BlockEntityType<EnchantmentRemoverBE>> ENCHANTMENT_REMOVER_BE = BLOCK_ENTITIES.register("enchantment_remover", () -> BlockEntityType.Builder.of(EnchantmentRemoverBE::new, ENCHANTMENT_REMOVER_BLOCK.get()).build(null));
    
    //JARS
    public static final RegistryObject<Block> OAK_JAR_BLOCK = BLOCKS.register("oak_jar",
            () -> new JarBlock());
    public static final RegistryObject<Block> SPRUCE_JAR_BLOCK = BLOCKS.register("spruce_jar",
            () -> new JarBlock());
    public static final RegistryObject<Block> BIRCH_JAR_BLOCK = BLOCKS.register("birch_jar",
            () -> new JarBlock());
    public static final RegistryObject<Block> ACACIA_JAR_BLOCK = BLOCKS.register("acacia_jar",
            () -> new JarBlock());
    public static final RegistryObject<Block> JUNGLE_JAR_BLOCK = BLOCKS.register("jungle_jar",
            () -> new JarBlock());
    public static final RegistryObject<Block> DARK_OAK_JAR_BLOCK = BLOCKS.register("dark_oak_jar",
            () -> new JarBlock());
    public static final RegistryObject<Block> CRIMSON_JAR_BLOCK = BLOCKS.register("crimson_jar",
            () -> new JarBlock());
    public static final RegistryObject<Block> WARPED_JAR_BLOCK = BLOCKS.register("warped_jar",
            () -> new JarBlock());
    public static final RegistryObject<Block> MANGROVE_JAR_BLOCK = BLOCKS.register("mangrove_jar",
            () -> new JarBlock());
    public static final RegistryObject<BlockEntityType<JarBE>> JAR_BE = BLOCK_ENTITIES.register("jar", () -> BlockEntityType.Builder.of(JarBE::new, 
    		OAK_JAR_BLOCK.get(), SPRUCE_JAR_BLOCK.get(), BIRCH_JAR_BLOCK.get(), ACACIA_JAR_BLOCK.get(), 
    		JUNGLE_JAR_BLOCK.get(), DARK_OAK_JAR_BLOCK.get(), CRIMSON_JAR_BLOCK.get(), WARPED_JAR_BLOCK.get(), 
    		MANGROVE_JAR_BLOCK.get()
		).build(null));
    
    //SHIELD RACK
    public static final RegistryObject<Block> OAK_SHIELD_RACK_BLOCK = BLOCKS.register("oak_shield_rack",
            () -> new ShieldRackBlock());
    public static final RegistryObject<Block> SPRUCE_SHIELD_RACK_BLOCK = BLOCKS.register("spruce_shield_rack",
            () -> new ShieldRackBlock());
    public static final RegistryObject<Block> BIRCH_SHIELD_RACK_BLOCK = BLOCKS.register("birch_shield_rack",
            () -> new ShieldRackBlock());
    public static final RegistryObject<Block> ACACIA_SHIELD_RACK_BLOCK = BLOCKS.register("acacia_shield_rack",
            () -> new ShieldRackBlock());
    public static final RegistryObject<Block> JUNGLE_SHIELD_RACK_BLOCK = BLOCKS.register("jungle_shield_rack",
            () -> new ShieldRackBlock());
    public static final RegistryObject<Block> DARK_OAK_SHIELD_RACK_BLOCK = BLOCKS.register("dark_oak_shield_rack",
            () -> new ShieldRackBlock());
    public static final RegistryObject<Block> CRIMSON_SHIELD_RACK_BLOCK = BLOCKS.register("crimson_shield_rack",
            () -> new ShieldRackBlock());
    public static final RegistryObject<Block> WARPED_SHIELD_RACK_BLOCK = BLOCKS.register("warped_shield_rack",
            () -> new ShieldRackBlock());
    public static final RegistryObject<Block> MANGROVE_SHIELD_RACK_BLOCK = BLOCKS.register("mangrove_shield_rack",
            () -> new ShieldRackBlock());
    public static final RegistryObject<BlockEntityType<ShieldRackBE>> SHIELD_RACK_BE = BLOCK_ENTITIES.register("shield_rack", () -> BlockEntityType.Builder.of(ShieldRackBE::new, 
    		OAK_SHIELD_RACK_BLOCK.get(), SPRUCE_SHIELD_RACK_BLOCK.get(), BIRCH_SHIELD_RACK_BLOCK.get(), ACACIA_SHIELD_RACK_BLOCK.get(), 
    		JUNGLE_SHIELD_RACK_BLOCK.get(), DARK_OAK_SHIELD_RACK_BLOCK.get(), CRIMSON_SHIELD_RACK_BLOCK.get(), WARPED_SHIELD_RACK_BLOCK.get(), 
    		MANGROVE_SHIELD_RACK_BLOCK.get()
		).build(null));
    
    //MISC
    public static final RegistryObject<Block> FISHINGNET_BLOCK = BLOCKS.register("fishingnet",
            () -> new FishingNetBlock());
    public static final RegistryObject<BlockEntityType<FishingNetBE>> FISHINGNET_BE = BLOCK_ENTITIES.register("fishingnet", () -> BlockEntityType.Builder.of(FishingNetBE::new, FISHINGNET_BLOCK.get()).build(null));
    public static final RegistryObject<Block> MOB_STATUE_BLOCK = BLOCKS.register("mob_statue",
            () -> new MobStatueBlock());
    public static final RegistryObject<BlockEntityType<MobStatueBE>> MOB_STATUE_BE = BLOCK_ENTITIES.register("mob_statue", () -> BlockEntityType.Builder.of(MobStatueBE::new, MOB_STATUE_BLOCK.get()).build(null));
    
    
    public static final RegistryObject<Block> PIGLIN_TRADING_POD_BLOCK = BLOCKS.register("piglin_trading_pod",
            () -> new AbstractEntityPodBlock() {

				@Override
				public AbstractEntityPodBE<?> createBlockEntity(BlockPos pos, BlockState state) {
					return new PiglinTradingPodBE(pos, state);
				}
            	
            });
    public static final RegistryObject<BlockEntityType<PiglinTradingPodBE>> PIGLIN_TRADING_POD_BE = BLOCK_ENTITIES.register("piglin_trading_pod", () -> BlockEntityType.Builder.of(PiglinTradingPodBE::new, PIGLIN_TRADING_POD_BLOCK.get()).build(null));
    public static final RegistryObject<Block> WITCH_TRADING_POD_BLOCK = BLOCKS.register("witch_trading_pod",
            () -> new AbstractEntityPodBlock() {

				@Override
				public AbstractEntityPodBE<?> createBlockEntity(BlockPos pos, BlockState state) {
					return new WitchTradingPodBE(pos, state);
				}
            	
            });
    public static final RegistryObject<BlockEntityType<WitchTradingPodBE>> WITCH_TRADING_POD_BE = BLOCK_ENTITIES.register("witch_trading_pod", () -> BlockEntityType.Builder.of(WitchTradingPodBE::new, WITCH_TRADING_POD_BLOCK.get()).build(null));
   
    public static final RegistryObject<Block> TANK_BLOCK = BLOCKS.register("tank",
            () -> new TankBlock());
    public static final RegistryObject<BlockEntityType<TankBE>> TANK_BE = BLOCK_ENTITIES.register("tank", () -> BlockEntityType.Builder.of(TankBE::new, TANK_BLOCK.get()).build(null));
    
    public static final RegistryObject<Block> XP_VACUUM_BLOCK = BLOCKS.register("xp_vacuum",
            () -> new XPVacuumBlock());
    public static final RegistryObject<BlockEntityType<XPVacuumBE>> XP_VACUUM_BE = BLOCK_ENTITIES.register("xp_vacuum", () -> BlockEntityType.Builder.of(XPVacuumBE::new, XP_VACUUM_BLOCK.get()).build(null));

    public static final RegistryObject<Block> ITEM_ANALYZER_BLOCK = BLOCKS.register("item_analyzer",
            () -> new ItemAnalyzerBlock());
    public static final RegistryObject<BlockEntityType<ItemAnalyzerBE>> ITEM_ANALYZER_BE = BLOCK_ENTITIES.register("item_analyzer", () -> BlockEntityType.Builder.of(ItemAnalyzerBE::new, ITEM_ANALYZER_BLOCK.get()).build(null));

    public static final RegistryObject<Block> WANDERING_TRADER_SIGN_BLOCK = BLOCKS.register("wandering_trader_sign",
            () -> new MobSignBlock());
    public static final RegistryObject<Block> PILLAGER_SIGN_BLOCK = BLOCKS.register("pillager_sign",
            () -> new MobSignBlock());
    public static final RegistryObject<Block> ENDERMAN_SIGN_BLOCK = BLOCKS.register("enderman_sign",
            () -> new MobSignBlock());
    public static final RegistryObject<Block> PHANTOM_SIGN_BLOCK = BLOCKS.register("phantom_sign",
            () -> new MobSignBlock());
    
    public static final RegistryObject<Block> ADVANCED_BEEHIVE_BLOCK = BLOCKS.register("advanced_beehive",
            () -> new AdvancedBeehiveBlock());
    public static final RegistryObject<BlockEntityType<AdvancedBeehiveBE>> ADVANCED_BEEHIVE_BE = BLOCK_ENTITIES.register("advanced_beehive", () -> BlockEntityType.Builder.of(AdvancedBeehiveBE::new, ADVANCED_BEEHIVE_BLOCK.get()).build(null));

    public static final RegistryObject<Block> ITEM_GRATE_BLOCK = BLOCKS.register("item_grate",
            () -> new ItemGrateBlock());
    
    public static final RegistryObject<Block> BLOCK_MIRROR_BLOCK = BLOCKS.register("block_mirror",
            () -> new BlockMirrorBlock());
    public static final RegistryObject<BlockEntityType<BlockMirrorBE>> BLOCK_MIRROR_BE = BLOCK_ENTITIES.register("block_mirror", () -> BlockEntityType.Builder.of(BlockMirrorBE::new, BLOCK_MIRROR_BLOCK.get()).build(null));

    public static boolean neverBlock(BlockState p_50806_, BlockGetter p_50807_, BlockPos p_50808_) {
       return false;
    }
    
    static {
    	autoItemBlockBlacklist.add(EARTH_PYLON_BLOCK);
    	autoItemBlockBlacklist.add(WATER_PYLON_BLOCK);
    	autoItemBlockBlacklist.add(EVIL_PYLON_BLOCK);
    	autoItemBlockBlacklist.add(ENDER_PYLON_BLOCK);
    	autoItemBlockBlacklist.add(FISHINGNET_BLOCK);
    	autoItemBlockBlacklist.add(PIGLIN_TRADING_POD_BLOCK);
    	autoItemBlockBlacklist.add(WITCH_TRADING_POD_BLOCK);
    	autoItemBlockBlacklist.add(MOB_STATUE_BLOCK);
    	autoItemBlockBlacklist.add(TANK_BLOCK);
    	
    	autoItemBlockTabs.put(OAK_ENCHANTMENT_BOOKSHELF_BLOCK, CreativeModeTab.TAB_DECORATIONS);
    	autoItemBlockTabs.put(SPRUCE_ENCHANTMENT_BOOKSHELF_BLOCK, CreativeModeTab.TAB_DECORATIONS);
    	autoItemBlockTabs.put(BIRCH_ENCHANTMENT_BOOKSHELF_BLOCK, CreativeModeTab.TAB_DECORATIONS);
    	autoItemBlockTabs.put(JUNGLE_ENCHANTMENT_BOOKSHELF_BLOCK, CreativeModeTab.TAB_DECORATIONS);
    	autoItemBlockTabs.put(ACACIA_ENCHANTMENT_BOOKSHELF_BLOCK, CreativeModeTab.TAB_DECORATIONS);
    	autoItemBlockTabs.put(DARK_OAK_ENCHANTMENT_BOOKSHELF_BLOCK, CreativeModeTab.TAB_DECORATIONS);
    	autoItemBlockTabs.put(CRIMSON_ENCHANTMENT_BOOKSHELF_BLOCK, CreativeModeTab.TAB_DECORATIONS);
    	autoItemBlockTabs.put(WARPED_ENCHANTMENT_BOOKSHELF_BLOCK, CreativeModeTab.TAB_DECORATIONS);
    	autoItemBlockTabs.put(MANGROVE_ENCHANTMENT_BOOKSHELF_BLOCK, CreativeModeTab.TAB_DECORATIONS);
    	
    	autoItemBlockTabs.put(OAK_SHIELD_RACK_BLOCK, CreativeModeTab.TAB_DECORATIONS);
    	autoItemBlockTabs.put(SPRUCE_SHIELD_RACK_BLOCK, CreativeModeTab.TAB_DECORATIONS);
    	autoItemBlockTabs.put(BIRCH_SHIELD_RACK_BLOCK, CreativeModeTab.TAB_DECORATIONS);
    	autoItemBlockTabs.put(JUNGLE_SHIELD_RACK_BLOCK, CreativeModeTab.TAB_DECORATIONS);
    	autoItemBlockTabs.put(ACACIA_SHIELD_RACK_BLOCK, CreativeModeTab.TAB_DECORATIONS);
    	autoItemBlockTabs.put(DARK_OAK_SHIELD_RACK_BLOCK, CreativeModeTab.TAB_DECORATIONS);
    	autoItemBlockTabs.put(CRIMSON_SHIELD_RACK_BLOCK, CreativeModeTab.TAB_DECORATIONS);
    	autoItemBlockTabs.put(WARPED_SHIELD_RACK_BLOCK, CreativeModeTab.TAB_DECORATIONS);
    	autoItemBlockTabs.put(MANGROVE_SHIELD_RACK_BLOCK, CreativeModeTab.TAB_DECORATIONS);
    	
    	autoItemBlockBlacklist.add(OAK_JAR_BLOCK);
    	autoItemBlockBlacklist.add(SPRUCE_JAR_BLOCK);
    	autoItemBlockBlacklist.add(BIRCH_JAR_BLOCK);
    	autoItemBlockBlacklist.add(ACACIA_JAR_BLOCK);
    	autoItemBlockBlacklist.add(JUNGLE_JAR_BLOCK);
    	autoItemBlockBlacklist.add(DARK_OAK_JAR_BLOCK);
    	autoItemBlockBlacklist.add(CRIMSON_JAR_BLOCK);
    	autoItemBlockBlacklist.add(WARPED_JAR_BLOCK);
    	autoItemBlockBlacklist.add(MANGROVE_JAR_BLOCK);
    	autoItemBlockTabs.put(OAK_JAR_BLOCK, CreativeModeTab.TAB_BREWING);
    	autoItemBlockTabs.put(SPRUCE_JAR_BLOCK, CreativeModeTab.TAB_BREWING);
    	autoItemBlockTabs.put(BIRCH_JAR_BLOCK, CreativeModeTab.TAB_BREWING);
    	autoItemBlockTabs.put(ACACIA_JAR_BLOCK, CreativeModeTab.TAB_BREWING);
    	autoItemBlockTabs.put(JUNGLE_JAR_BLOCK, CreativeModeTab.TAB_BREWING);
    	autoItemBlockTabs.put(DARK_OAK_JAR_BLOCK, CreativeModeTab.TAB_BREWING);
    	autoItemBlockTabs.put(CRIMSON_JAR_BLOCK, CreativeModeTab.TAB_BREWING);
    	autoItemBlockTabs.put(WARPED_JAR_BLOCK, CreativeModeTab.TAB_BREWING);
    	autoItemBlockTabs.put(MANGROVE_JAR_BLOCK, CreativeModeTab.TAB_BREWING);
    	
    	autoItemBlockTabs.put(ADVANCED_BEEHIVE_BLOCK, CreativeModeTab.TAB_DECORATIONS);
    	
    	autoItemBlockTabs.put(ITEM_GRATE_BLOCK, CreativeModeTab.TAB_REDSTONE);
    	
    	autoItemBlockBlacklist.add(BLOCK_MIRROR_BLOCK);
    	autoItemBlockTabs.put(BLOCK_MIRROR_BLOCK, CreativeModeTab.TAB_REDSTONE);
    }        
    
    @SubscribeEvent
    public static void onRegisterItems(final RegisterEvent event) {
        if (event.getRegistryKey().equals(ForgeRegistries.Keys.ITEMS)){
            BLOCKS.getEntries().forEach((blockRegistryObject) -> {
            	if(autoItemBlockBlacklist.contains(blockRegistryObject))return;
                Block block = blockRegistryObject.get();
                CreativeModeTab tab = autoItemBlockTabs.getOrDefault(blockRegistryObject, ModCreativeTab.instance);
                Item.Properties properties = new Item.Properties().tab(tab);
                Supplier<Item> blockItemFactory = () -> new BlockItem(block, properties);
                event.register(ForgeRegistries.Keys.ITEMS, blockRegistryObject.getId(), blockItemFactory);
            });
        }
    }
}
