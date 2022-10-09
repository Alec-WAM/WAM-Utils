package alec_wam.wam_utils.init;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.advanced_portal.PortalCardItem;
import alec_wam.wam_utils.blocks.entity_pod.EntityPodItem;
import alec_wam.wam_utils.blocks.entity_pod.piglin.PiglinTradingPodItemRenderer;
import alec_wam.wam_utils.blocks.entity_pod.witch.WitchTradingPodItemRenderer;
import alec_wam.wam_utils.blocks.jar.JarBlockItem;
import alec_wam.wam_utils.blocks.machine.enchantment_remover.SingleEnchantmentItem;
import alec_wam.wam_utils.blocks.mirror_block.BlockMirrorItem;
import alec_wam.wam_utils.blocks.mob_statue.MobStatueItemRenderer;
import alec_wam.wam_utils.blocks.pylon.PylonCreateItem;
import alec_wam.wam_utils.blocks.tank.TankItem;
import alec_wam.wam_utils.entities.chest_boats.CustomBoatItem;
import alec_wam.wam_utils.entities.chest_boats.EnderChestBoat;
import alec_wam.wam_utils.entities.minecarts.CustomMinecartItem;
import alec_wam.wam_utils.entities.minecarts.EnderChestMinecart;
import alec_wam.wam_utils.items.WitherBonemealItem;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.data.loading.DatagenModLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ItemInit {
	
	public static final DeferredRegister<Item> ITEMS = 
            DeferredRegister.create(ForgeRegistries.ITEMS, WAMUtilsMod.MODID);
	
	public static final RegistryObject<Item> IRON_PLATE = ITEMS.register("iron_plate",
            () -> new Item(new Item.Properties().tab(ModCreativeTab.instance)));
	
	public static final RegistryObject<Item> UPGRADE_SPEED = ITEMS.register("upgrade_speed",
            () -> new Item(new Item.Properties().tab(ModCreativeTab.instance)));
	public static final RegistryObject<Item> UPGRADE_DAMAGE = ITEMS.register("upgrade_damage",
            () -> new Item(new Item.Properties().tab(ModCreativeTab.instance)));
	public static final RegistryObject<Item> UPGRADE_BOSS = ITEMS.register("upgrade_boss",
            () -> new Item(new Item.Properties().tab(ModCreativeTab.instance)));
	public static final RegistryObject<Item> UPGRADE_PLAYER = ITEMS.register("upgrade_player",
            () -> new Item(new Item.Properties().tab(ModCreativeTab.instance)));
	public static final RegistryObject<Item> UPGRADE_BEHEADING = ITEMS.register("upgrade_beheading",
            () -> new Item(new Item.Properties().tab(ModCreativeTab.instance)));
	public static final RegistryObject<Item> UPGRADE_CAPACITY_ENERGY = ITEMS.register("upgrade_capacity_energy",
            () -> new Item(new Item.Properties().tab(ModCreativeTab.instance)));
	public static final RegistryObject<Item> UPGRADE_CAPACITY_FLUID = ITEMS.register("upgrade_capacity_fluid",
            () -> new Item(new Item.Properties().tab(ModCreativeTab.instance)));
	public static final RegistryObject<Item> UPGRADE_RANGE = ITEMS.register("upgrade_range",
            () -> new Item(new Item.Properties().tab(ModCreativeTab.instance)));
	
	public static final RegistryObject<Item> EMPTY_ENTITY_POD = ITEMS.register("empty_entity_pod",
            () -> new EntityPodItem(new Item.Properties().tab(ModCreativeTab.instance)));
	
	public static final RegistryObject<Item> SINGLE_ENCHANTMENT_ITEM = ITEMS.register("single_enchantment",
            () -> new SingleEnchantmentItem(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(16).rarity(Rarity.UNCOMMON)));
	
	public static final RegistryObject<Item> TELEPORT_CARD = ITEMS.register("teleport_card",
            () -> new PortalCardItem(new Item.Properties().tab(ModCreativeTab.instance)));
	
	//ITEM BLOCKS
	public static final RegistryObject<PylonCreateItem> EARTH_CORE = ITEMS.register("earth_core",
            () -> new PylonCreateItem(BlockInit.EARTH_PYLON_BLOCK.get(), new Item.Properties().tab(ModCreativeTab.instance)));
	public static final RegistryObject<PylonCreateItem> WATER_CORE = ITEMS.register("water_core",
            () -> new PylonCreateItem(BlockInit.WATER_PYLON_BLOCK.get(), new Item.Properties().tab(ModCreativeTab.instance)));
	public static final RegistryObject<PylonCreateItem> EVIL_CORE = ITEMS.register("evil_core",
            () -> new PylonCreateItem(BlockInit.EVIL_PYLON_BLOCK.get(), new Item.Properties().tab(ModCreativeTab.instance)));
	public static final RegistryObject<PylonCreateItem> ENDER_CORE = ITEMS.register("ender_core",
            () -> new PylonCreateItem(BlockInit.ENDER_PYLON_BLOCK.get(), new Item.Properties().tab(ModCreativeTab.instance)));
	
	public static final RegistryObject<PlaceOnWaterBlockItem> FISHINGNET_ITEMBLOCK = ITEMS.register("fishingnet",
            () -> new PlaceOnWaterBlockItem(BlockInit.FISHINGNET_BLOCK.get(), new Item.Properties().tab(ModCreativeTab.instance)));
	
	public static final RegistryObject<BlockItem> PIGLIN_TRADING_POD_ITEMBLOCK = ITEMS.register("piglin_trading_pod",
            () -> new BlockItem(BlockInit.PIGLIN_TRADING_POD_BLOCK.get(), new Item.Properties().tab(ModCreativeTab.instance)) {
            	@Override
            	public void initializeClient(@Nonnull Consumer<IClientItemExtensions> consumer)
            	{
            		if (!DatagenModLoader.isRunningDataGen())
            			consumer.accept(PiglinTradingPodItemRenderer.CLIENT_RENDERER);
            	}
            });
	public static final RegistryObject<BlockItem> WITCH_TRADING_POD_ITEMBLOCK = ITEMS.register("witch_trading_pod",
            () -> new BlockItem(BlockInit.WITCH_TRADING_POD_BLOCK.get(), new Item.Properties().tab(ModCreativeTab.instance)) {
            	@Override
            	public void initializeClient(@Nonnull Consumer<IClientItemExtensions> consumer)
            	{
            		if (!DatagenModLoader.isRunningDataGen())
            			consumer.accept(WitchTradingPodItemRenderer.CLIENT_RENDERER);
            	}
            });
	public static final RegistryObject<BlockItem> MOB_STATUE_ITEMBLOCK = ITEMS.register("mob_statue",
            () -> new BlockItem(BlockInit.MOB_STATUE_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC)) {
            	@Override
            	public void initializeClient(@Nonnull Consumer<IClientItemExtensions> consumer)
            	{
            		if (!DatagenModLoader.isRunningDataGen())
            			consumer.accept(MobStatueItemRenderer.CLIENT_RENDERER);
            	}
            });
	public static final RegistryObject<BlockItem> TANK_ITEMBLOCK = ITEMS.register("tank",
            () -> new TankItem(BlockInit.TANK_BLOCK.get(), new Item.Properties().tab(ModCreativeTab.instance)));
	
	public static final RegistryObject<BlockItem> BLOCK_MIRROR_ITEM = ITEMS.register("block_mirror",
            () -> new BlockMirrorItem(BlockInit.BLOCK_MIRROR_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_REDSTONE)));
	
	//JARS
	public static final RegistryObject<BlockItem> OAK_JAR_ITEMBLOCK = ITEMS.register("oak_jar",
            () -> new JarBlockItem(BlockInit.OAK_JAR_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BREWING)));
	public static final RegistryObject<BlockItem> BIRCH_JAR_ITEMBLOCK = ITEMS.register("birch_jar",
            () -> new JarBlockItem(BlockInit.BIRCH_JAR_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BREWING)));
	public static final RegistryObject<BlockItem> SPRUCE_JAR_ITEMBLOCK = ITEMS.register("spruce_jar",
            () -> new JarBlockItem(BlockInit.SPRUCE_JAR_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BREWING)));
	public static final RegistryObject<BlockItem> JUNGLE_JAR_ITEMBLOCK = ITEMS.register("jungle_jar",
            () -> new JarBlockItem(BlockInit.JUNGLE_JAR_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BREWING)));
	public static final RegistryObject<BlockItem> ACACIA_JAR_ITEMBLOCK = ITEMS.register("acacia_jar",
            () -> new JarBlockItem(BlockInit.ACACIA_JAR_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BREWING)));;
	public static final RegistryObject<BlockItem> DARK_OAK_JAR_ITEMBLOCK = ITEMS.register("dark_oak_jar",
            () -> new JarBlockItem(BlockInit.DARK_OAK_JAR_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BREWING)));
	public static final RegistryObject<BlockItem> CRIMSON_JAR_ITEMBLOCK = ITEMS.register("crimson_jar",
            () -> new JarBlockItem(BlockInit.CRIMSON_JAR_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BREWING)));
	public static final RegistryObject<BlockItem> WARPED_JAR_ITEMBLOCK = ITEMS.register("warped_jar",
            () -> new JarBlockItem(BlockInit.WARPED_JAR_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BREWING)));;
	public static final RegistryObject<BlockItem> MANGROVE_JAR_ITEMBLOCK = ITEMS.register("mangrove_jar",
            () -> new JarBlockItem(BlockInit.MANGROVE_JAR_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BREWING)));
	
	//BOATS
	public static final RegistryObject<Item> ACACIA_ENDERCHEST_BOAT = ITEMS.register("acacia_enderchest_boat", () -> new CustomBoatItem(Boat.Type.ACACIA, (new Item.Properties()).stacksTo(1).tab(CreativeModeTab.TAB_TRANSPORTATION)) {

		@Override
		public Boat createBoatEntity(Level level, HitResult hitresult, double x, double y, double z) {
			return new EnderChestBoat(level, x, y, z);
		}

	});
	public static final RegistryObject<Item> BIRCH_ENDERCHEST_BOAT = ITEMS.register("birch_enderchest_boat", () -> new CustomBoatItem(Boat.Type.BIRCH, (new Item.Properties()).stacksTo(1).tab(CreativeModeTab.TAB_TRANSPORTATION)) {

		@Override
		public Boat createBoatEntity(Level level, HitResult hitresult, double x, double y, double z) {
			return new EnderChestBoat(level, x, y, z);
		}

	});
	public static final RegistryObject<Item> DARK_OAK_ENDERCHEST_BOAT = ITEMS.register("dark_oak_enderchest_boat", () -> new CustomBoatItem(Boat.Type.DARK_OAK, (new Item.Properties()).stacksTo(1).tab(CreativeModeTab.TAB_TRANSPORTATION)) {

		@Override
		public Boat createBoatEntity(Level level, HitResult hitresult, double x, double y, double z) {
			return new EnderChestBoat(level, x, y, z);
		}

	});
	public static final RegistryObject<Item> JUNGLE_ENDERCHEST_BOAT = ITEMS.register("jungle_enderchest_boat", () -> new CustomBoatItem(Boat.Type.JUNGLE, (new Item.Properties()).stacksTo(1).tab(CreativeModeTab.TAB_TRANSPORTATION)) {

		@Override
		public Boat createBoatEntity(Level level, HitResult hitresult, double x, double y, double z) {
			return new EnderChestBoat(level, x, y, z);
		}

	});
	public static final RegistryObject<Item> MANGROVE_ENDERCHEST_BOAT = ITEMS.register("mangrove_enderchest_boat", () -> new CustomBoatItem(Boat.Type.MANGROVE, (new Item.Properties()).stacksTo(1).tab(CreativeModeTab.TAB_TRANSPORTATION)) {

		@Override
		public Boat createBoatEntity(Level level, HitResult hitresult, double x, double y, double z) {
			return new EnderChestBoat(level, x, y, z);
		}

	});
	public static final RegistryObject<Item> OAK_ENDERCHEST_BOAT = ITEMS.register("oak_enderchest_boat", () -> new CustomBoatItem(Boat.Type.OAK, (new Item.Properties()).stacksTo(1).tab(CreativeModeTab.TAB_TRANSPORTATION)) {

		@Override
		public Boat createBoatEntity(Level level, HitResult hitresult, double x, double y, double z) {
			return new EnderChestBoat(level, x, y, z);
		}

	});
	public static final RegistryObject<Item> SPRUCE_ENDERCHEST_BOAT = ITEMS.register("spruce_enderchest_boat", () -> new CustomBoatItem(Boat.Type.SPRUCE, (new Item.Properties()).stacksTo(1).tab(CreativeModeTab.TAB_TRANSPORTATION)) {

		@Override
		public Boat createBoatEntity(Level level, HitResult hitresult, double x, double y, double z) {
			return new EnderChestBoat(level, x, y, z);
		}

	});
	
	//MINECARTS
	public static final RegistryObject<Item> ENDERCHEST_MINECART = ITEMS.register("enderchest_minecart", () -> new CustomMinecartItem((new Item.Properties()).stacksTo(1).tab(CreativeModeTab.TAB_TRANSPORTATION)) {

		@Override
		public AbstractMinecart createMinecartEntity(Level level, double x, double y, double z) {
			return new EnderChestMinecart(level, x, y, z);
		}

	});
	  
	//MISC ITEMS
	public static final RegistryObject<Item> WITHER_BONE = ITEMS.register("wither_bone",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MATERIALS)));
	public static final RegistryObject<Item> WITHER_BONE_MEAL = ITEMS.register("wither_bone_meal",
            () -> new WitherBonemealItem(new Item.Properties().tab(CreativeModeTab.TAB_MATERIALS)));
	
	
	public static class ModCreativeTab extends CreativeModeTab {
		public static final ModCreativeTab instance = new ModCreativeTab(CreativeModeTab.TABS.length, WAMUtilsMod.MODID + ".items");

		private ModCreativeTab(int index, String label) {
	        super(index, label);
	    }

	    @Override
	    public ItemStack makeIcon() {
	        return new ItemStack(IRON_PLATE.get());
	    }
	}
}
