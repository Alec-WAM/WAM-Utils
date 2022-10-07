package alec_wam.wam_utils.init;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.advanced_beehive.AdvancedBeehiveContainer;
import alec_wam.wam_utils.blocks.advanced_portal.AdvancedPortalHostContainer;
import alec_wam.wam_utils.blocks.advanced_spawner.AdvancedSpawnerContainer;
import alec_wam.wam_utils.blocks.bookshelf.EnchantmentBookshelfContainer;
import alec_wam.wam_utils.blocks.entity_pod.piglin.PiglinTradingPodContainer;
import alec_wam.wam_utils.blocks.entity_pod.witch.WitchTradingPodContainer;
import alec_wam.wam_utils.blocks.generator.furnace.FurnaceGeneratorContainer;
import alec_wam.wam_utils.blocks.item_analyzer.ItemAnalyzerContainer;
import alec_wam.wam_utils.blocks.machine.auto_animal_farmer.AutoAnimalFarmerContainer;
import alec_wam.wam_utils.blocks.machine.auto_breeder.AutoBreederContainer;
import alec_wam.wam_utils.blocks.machine.auto_butcher.AutoButcherContainer;
import alec_wam.wam_utils.blocks.machine.auto_compactor.AutoCompactorContainer;
import alec_wam.wam_utils.blocks.machine.auto_farmer.AutoFarmerContainer;
import alec_wam.wam_utils.blocks.machine.auto_fisher.AutoFisherContainer;
import alec_wam.wam_utils.blocks.machine.auto_lumberjack.AutoLumberjackContainer;
import alec_wam.wam_utils.blocks.machine.enchantment_remover.EnchantmentRemoverContainer;
import alec_wam.wam_utils.blocks.machine.flower_generator.FlowerSimulatorContainer;
import alec_wam.wam_utils.blocks.machine.quarry.QuarryContainer;
import alec_wam.wam_utils.blocks.machine.stone_factory.StoneFactoryContainer;
import alec_wam.wam_utils.blocks.mob_statue.MobStatueContainer;
import alec_wam.wam_utils.blocks.xp_vacuum.XPVacuumContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ContainerInit {

	public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, WAMUtilsMod.MODID);
	
	public static final RegistryObject<MenuType<AdvancedSpawnerContainer>> ADVANCED_SPAWNER_CONTAINER = CONTAINERS.register("advanced_spawner",
            () -> IForgeMenuType.create((windowId, inv, data) -> new AdvancedSpawnerContainer(windowId, data.readBlockPos(), inv, inv.player)));
	public static final RegistryObject<MenuType<FurnaceGeneratorContainer>> FURNACE_GENERATOR_CONTAINER = CONTAINERS.register("furnace_generator",
            () -> IForgeMenuType.create((windowId, inv, data) -> new FurnaceGeneratorContainer(windowId, data.readBlockPos(), inv, inv.player)));
	public static final RegistryObject<MenuType<AutoAnimalFarmerContainer>> AUTO_ANIMAL_FARMER_CONTAINER = CONTAINERS.register("auto_animal_farmer",
            () -> IForgeMenuType.create((windowId, inv, data) -> new AutoAnimalFarmerContainer(windowId, data.readBlockPos(), inv, inv.player)));
	public static final RegistryObject<MenuType<AutoButcherContainer>> AUTO_BUTCHER_CONTAINER = CONTAINERS.register("auto_butcher",
            () -> IForgeMenuType.create((windowId, inv, data) -> new AutoButcherContainer(windowId, data.readBlockPos(), inv, inv.player)));
	public static final RegistryObject<MenuType<AutoFisherContainer>> AUTO_FISHER_CONTAINER = CONTAINERS.register("auto_fisher",
            () -> IForgeMenuType.create((windowId, inv, data) -> new AutoFisherContainer(windowId, data.readBlockPos(), inv, inv.player)));
	public static final RegistryObject<MenuType<AutoBreederContainer>> AUTO_BREEDER_CONTAINER = CONTAINERS.register("auto_breeder",
            () -> IForgeMenuType.create((windowId, inv, data) -> new AutoBreederContainer(windowId, data.readBlockPos(), inv, inv.player)));
	public static final RegistryObject<MenuType<AutoFarmerContainer>> AUTO_FARMER_CONTAINER = CONTAINERS.register("auto_farmer",
            () -> IForgeMenuType.create((windowId, inv, data) -> new AutoFarmerContainer(windowId, data.readBlockPos(), inv, inv.player)));
	public static final RegistryObject<MenuType<AutoLumberjackContainer>> AUTO_LUMBERJACK_CONTAINER = CONTAINERS.register("auto_lumberjack",
            () -> IForgeMenuType.create((windowId, inv, data) -> new AutoLumberjackContainer(windowId, data.readBlockPos(), inv, inv.player)));
	public static final RegistryObject<MenuType<AutoCompactorContainer>> AUTO_COMPACTOR_CONTAINER = CONTAINERS.register("auto_compactor",
            () -> IForgeMenuType.create((windowId, inv, data) -> new AutoCompactorContainer(windowId, data.readBlockPos(), inv, inv.player)));
	public static final RegistryObject<MenuType<QuarryContainer>> QUARRY_CONTAINER = CONTAINERS.register("quarry",
            () -> IForgeMenuType.create((windowId, inv, data) -> new QuarryContainer(windowId, data.readBlockPos(), inv, inv.player)));
	public static final RegistryObject<MenuType<FlowerSimulatorContainer>> FLOWER_SIMULATOR_CONTAINER = CONTAINERS.register("flower_simulator",
            () -> IForgeMenuType.create((windowId, inv, data) -> new FlowerSimulatorContainer(windowId, data.readBlockPos(), inv, inv.player)));	
	public static final RegistryObject<MenuType<StoneFactoryContainer>> STONE_FACTORY_CONTAINER = CONTAINERS.register("stone_factory",
            () -> IForgeMenuType.create((windowId, inv, data) -> new StoneFactoryContainer(windowId, data.readBlockPos(), inv, inv.player)));
	
	public static final RegistryObject<MenuType<EnchantmentBookshelfContainer>> ENCHANTMENT_BOOKSHELF_CONTAINER = CONTAINERS.register("enchantment_bookshelf",
            () -> IForgeMenuType.create((windowId, inv, data) -> new EnchantmentBookshelfContainer(windowId, data.readBlockPos(), inv, inv.player)));	
	public static final RegistryObject<MenuType<EnchantmentRemoverContainer>> ENCHANTMENT_REMOVER_CONTAINER = CONTAINERS.register("enchantment_remover",
            () -> IForgeMenuType.create((windowId, inv, data) -> new EnchantmentRemoverContainer(windowId, data.readBlockPos(), inv, inv.player)));
	
	public static final RegistryObject<MenuType<PiglinTradingPodContainer>> PIGLIN_TRADING_POD_CONTAINER = CONTAINERS.register("piglin_trading_pod",
            () -> IForgeMenuType.create((windowId, inv, data) -> new PiglinTradingPodContainer(windowId, data.readBlockPos(), inv, inv.player)));
	public static final RegistryObject<MenuType<WitchTradingPodContainer>> WITCH_TRADING_POD_CONTAINER = CONTAINERS.register("witch_trading_pod",
            () -> IForgeMenuType.create((windowId, inv, data) -> new WitchTradingPodContainer(windowId, data.readBlockPos(), inv, inv.player)));
	public static final RegistryObject<MenuType<MobStatueContainer>> MOB_STATUE_CONTAINER = CONTAINERS.register("mob_statue",
            () -> IForgeMenuType.create((windowId, inv, data) -> new MobStatueContainer(windowId, data.readBlockPos(), inv, inv.player)));
	public static final RegistryObject<MenuType<ItemAnalyzerContainer>> ITEM_ANALYZER_CONTAINER = CONTAINERS.register("item_analyzer",
            () -> IForgeMenuType.create((windowId, inv, data) -> new ItemAnalyzerContainer(windowId, data.readBlockPos(), inv, inv.player)));
	public static final RegistryObject<MenuType<AdvancedBeehiveContainer>> ADVANCED_BEEHIVE_CONTAINER = CONTAINERS.register("advanced_beehive",
            () -> IForgeMenuType.create((windowId, inv, data) -> new AdvancedBeehiveContainer(windowId, data.readBlockPos(), inv, inv.player)));
	
	public static final RegistryObject<MenuType<XPVacuumContainer>> XP_VACUUM_CONTAINER = CONTAINERS.register("xp_vacuum",
            () -> IForgeMenuType.create((windowId, inv, data) -> new XPVacuumContainer(windowId, data.readBlockPos(), inv, inv.player)));
	
	public static final RegistryObject<MenuType<AdvancedPortalHostContainer>> ADVANCED_PORTAL_HOST_CONTAINER = CONTAINERS.register("advanced_portal_host",
            () -> IForgeMenuType.create((windowId, inv, data) -> new AdvancedPortalHostContainer(windowId, data.readBlockPos(), inv, inv.player)));
	
}
