package alec_wam.wam_utils.common;

import static alec_wam.wam_utils.WAMUtils.MODID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.base.Function;

import alec_wam.wam_utils.WAMUtils;
import alec_wam.wam_utils.common.blocks.BaseBE;
import alec_wam.wam_utils.common.blocks.ItemGrateBlock;
import alec_wam.wam_utils.common.blocks.conveyor.ConveyorBeltBE;
import alec_wam.wam_utils.common.blocks.conveyor.ConveyorBeltBlock;
import alec_wam.wam_utils.common.blocks.conveyor.splitter.ConveyorSplitterBE;
import alec_wam.wam_utils.common.blocks.conveyor.splitter.ConveyorSplitterBlock;
import alec_wam.wam_utils.common.blocks.enchantment.bookshelf.EnchantmentBookshelfBE;
import alec_wam.wam_utils.common.blocks.enchantment.bookshelf.EnchantmentBookshelfBlock;
import alec_wam.wam_utils.common.blocks.enchantment.bookshelf.menu.EnchantmentBookshelfMenu;
import alec_wam.wam_utils.common.blocks.enchantment.indexer.EnchantmentIndexerBE;
import alec_wam.wam_utils.common.blocks.enchantment.indexer.EnchantmentIndexerBlock;
import alec_wam.wam_utils.common.blocks.shieldrack.ShieldRackBE;
import alec_wam.wam_utils.common.blocks.shieldrack.ShieldRackBlock;
import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.WorkerEntity.ExternalInventoryStatus;
import alec_wam.wam_utils.common.entities.workers.WorkerInventorySettings;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager.JobType;
import alec_wam.wam_utils.common.entities.workers.menu.WorkerInventoryMenu;
import alec_wam.wam_utils.common.items.MagicBonemealItem;
import alec_wam.wam_utils.common.items.WorkerInventoryItem;
import alec_wam.wam_utils.common.items.WorkerSpawnItem;
import alec_wam.wam_utils.common.items.WorkerStaffItem;
import alec_wam.wam_utils.common.items.WorkerStaffItem.SelectionType;
import alec_wam.wam_utils.common.items.WorkerStaffItem.WorkerBlockSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModInit {

	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.createEntities(MODID);
    private static final DeferredRegister<EntityDataSerializer<?>> ENTITY_SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);
    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);

    public static final Map<WoodType, BlockFamily> WOOD_BLOCK_FAMILIES = new HashMap<>();
    public static final List<String> VANILLA_WOOD_ORDER = List.of(
        "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
        "mangrove", "cherry", "pale_oak", "bamboo", "crimson", "warped"
    );
    public static final List<WoodType> VANILLA_SORTED_WOOD_TYPES = WoodType.values()
        .sorted(Comparator.comparingInt(wood -> {
            String name = wood.name().toLowerCase(); // or wood.name() if already lowercase
            int index = VANILLA_WOOD_ORDER.indexOf(name);
            return index == -1 ? Integer.MAX_VALUE : index; // put unknowns at the end
        })).toList();

    public static final DeferredBlock<Block> CONVEYOR_BELT_BLOCK = registerBlock("conveyor_belt", ConveyorBeltBlock::new, () -> BlockBehaviour.Properties.of()
			.sound(SoundType.METAL)
			.destroyTime(1.0f)
	        .explosionResistance(10.0f)
			.isRedstoneConductor(ModInit::never)
            .noOcclusion()
            .isValidSpawn(ModInit::never)
            .isSuffocating(ModInit::never)
            .isViewBlocking(ModInit::never)
			);
    public static final DeferredItem<BlockItem> CONVEYOR_BELT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("conveyor_belt", CONVEYOR_BELT_BLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConveyorBeltBE>> CONVEYOR_BELT_BLOCK_ENTITY = BLOCK_ENTITIES.register("conveyor_belt_block_entity", () -> new BlockEntityType<>(ConveyorBeltBE::new, CONVEYOR_BELT_BLOCK.get()));
    
    public static final DeferredBlock<Block> CONVEYOR_SPLITTER_BLOCK = registerBlock("conveyor_splitter", ConveyorSplitterBlock::new, () -> BlockBehaviour.Properties.of()
			.sound(SoundType.METAL)
			.destroyTime(1.0f)
	        .explosionResistance(10.0f)
			);
    public static final DeferredItem<BlockItem> CONVEYOR_SPLITTER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("conveyor_splitter", CONVEYOR_SPLITTER_BLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConveyorSplitterBE>> CONVEYOR_SPLITTER_BLOCK_ENTITY = BLOCK_ENTITIES.register("conveyor_splitter_block_entity", () -> new BlockEntityType<>(ConveyorSplitterBE::new, CONVEYOR_SPLITTER_BLOCK.get()));
    
    
    public static final DeferredBlock<Block> ITEM_GRATE_BLOCK = registerBlock("item_grate", ItemGrateBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.METAL).strength(5.0F, 6.0F).noOcclusion());
    public static final DeferredItem<BlockItem> ITEM_GRATE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("item_grate", ITEM_GRATE_BLOCK);

    public static final Map<WoodType, DeferredBlock<Block>> SHIELDRACK_BLOCKS = new HashMap<>();
    public static final Map<WoodType, DeferredItem<BlockItem>> SHIELDRACK_BLOCK_ITEMS = new HashMap<>(); 

    public static final Map<WoodType, DeferredBlock<Block>> ENCHANTMENT_BOOK_SHELF_BLOCKS = new HashMap<>();
    public static final Map<WoodType, DeferredItem<BlockItem>> ENCHANTMENT_BOOK_SHELF_BLOCK_ITEMS = new HashMap<>();  

    static {
        WOOD_BLOCK_FAMILIES.put(WoodType.OAK, BlockFamilies.OAK_PLANKS);
        WOOD_BLOCK_FAMILIES.put(WoodType.SPRUCE, BlockFamilies.SPRUCE_PLANKS);
        WOOD_BLOCK_FAMILIES.put(WoodType.BIRCH, BlockFamilies.BIRCH_PLANKS);
        WOOD_BLOCK_FAMILIES.put(WoodType.ACACIA, BlockFamilies.ACACIA_PLANKS);
        WOOD_BLOCK_FAMILIES.put(WoodType.CHERRY, BlockFamilies.CHERRY_PLANKS);
        WOOD_BLOCK_FAMILIES.put(WoodType.JUNGLE, BlockFamilies.JUNGLE_PLANKS);
        WOOD_BLOCK_FAMILIES.put(WoodType.DARK_OAK, BlockFamilies.DARK_OAK_PLANKS);
        WOOD_BLOCK_FAMILIES.put(WoodType.PALE_OAK, BlockFamilies.PALE_OAK_PLANKS);
        WOOD_BLOCK_FAMILIES.put(WoodType.CRIMSON, BlockFamilies.CRIMSON_PLANKS);
        WOOD_BLOCK_FAMILIES.put(WoodType.WARPED, BlockFamilies.WARPED_PLANKS);
        WOOD_BLOCK_FAMILIES.put(WoodType.MANGROVE, BlockFamilies.MANGROVE_PLANKS);
        WOOD_BLOCK_FAMILIES.put(WoodType.BAMBOO, BlockFamilies.BAMBOO_PLANKS);
        
        WoodType.values().forEach(wood -> {
            SHIELDRACK_BLOCKS.put(
                wood, 
                registerBlock(
                    wood.name().toLowerCase() + "_shieldrack", 
                    ShieldRackBlock::new, 
                    () -> BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOD)
                        .sound(wood.soundType())
                        .strength(2.5F, 3.0F)
                        .ignitedByLava()
                )
            );
            SHIELDRACK_BLOCK_ITEMS.put(
                wood, 
                ITEMS.registerSimpleBlockItem(
                    wood.name().toLowerCase() + "_shieldrack", 
                    SHIELDRACK_BLOCKS.get(wood)
                )
            );

            ENCHANTMENT_BOOK_SHELF_BLOCKS.put(
                wood, 
                registerBlock(
                    wood.name().toLowerCase() + "_enchantment_book_shelf", 
                    EnchantmentBookshelfBlock::new, 
                    () -> BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOD)
                        .sound(wood.soundType())
                        .strength(2.5F, 3.0F)
                        .ignitedByLava()
                )
            );
            ENCHANTMENT_BOOK_SHELF_BLOCK_ITEMS.put(
                wood, 
                ITEMS.registerSimpleBlockItem(
                    wood.name().toLowerCase() + "_enchantment_book_shelf", 
                    ENCHANTMENT_BOOK_SHELF_BLOCKS.get(wood)
                )
            );
        });
    } 

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShieldRackBE>> SHIELDRACK_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("shieldrack", () -> {
            Collection<Block> blocks = SHIELDRACK_BLOCKS.values().stream()
                .map(DeferredBlock::get)
                .toList();

            return new BlockEntityType<>(ShieldRackBE::new, blocks.toArray(Block[]::new));
        });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnchantmentBookshelfBE>> ENCHANTMENT_BOOK_SHELF_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("enchantment_book_shelf", () -> {
            Collection<Block> blocks = ENCHANTMENT_BOOK_SHELF_BLOCKS.values().stream()
                .map(DeferredBlock::get)
                .toList();

            return new BlockEntityType<>(EnchantmentBookshelfBE::new, blocks.toArray(Block[]::new));
        });
    public static final Supplier<MenuType<EnchantmentBookshelfMenu>> ENCHANTMENT_BOOK_SHELF_MENU_TYPE = MENU_TYPES.register("enchantment_book_shelf", () -> IMenuTypeExtension.create(EnchantmentBookshelfMenu::new));


    public static final DeferredBlock<Block> ENCHANTMENT_INDEXER_BLOCK = registerBlock("enchantment_indexer", EnchantmentIndexerBlock::new, () -> BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(2.5F, 3.0F)
            .ignitedByLava());
    public static final DeferredItem<BlockItem> ENCHANTMENT_INDEXER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("enchantment_indexer", ENCHANTMENT_INDEXER_BLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnchantmentIndexerBE>> ENCHANTMENT_INDEXER_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("enchantment_indexer", () -> {
            return new BlockEntityType<>(EnchantmentIndexerBE::new, ENCHANTMENT_INDEXER_BLOCK.get());
        });

    // ENTITIES
    public static final DeferredHolder<EntityType<?>, EntityType<WorkerEntity>> WORKER_ENTITY = ENTITIES.register(
    		"worker", 
    		() -> 
    		EntityType.Builder.<WorkerEntity>of(WorkerEntity::new, MobCategory.MISC)
    		.sized(0.3F, 0.95F)
    		.eyeHeight(0.85f)
    		.clientTrackingRange(32)
    		.updateInterval(2)
    		.build(ResourceKey.create(
		        Registries.ENTITY_TYPE,
		        WAMUtils.prefix("worker")
		    ))
	);
    public static final Supplier<EntityDataSerializer<ExternalInventoryStatus>> WORKER_EXTERNAL_INVENTORY_STATUS = ENTITY_SERIALIZERS.register(
		"worker_external_inventory_status", 
		() -> EntityDataSerializer.forValueType(ExternalInventoryStatus.STREAM_CODEC)
    );
    public static final Supplier<MenuType<WorkerInventoryMenu>> WORKER_INVENTORY_MENU_TYPE = MENU_TYPES.register("worker_inventory", () -> IMenuTypeExtension.create(WorkerInventoryMenu::new));


    public static final DeferredItem<Item> WORKER_SPAWN_ITEM = ITEMS.registerItem("worker_spawn_item", WorkerSpawnItem::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> WORKER_STAFF_ITEM = ITEMS.registerItem("worker_staff", WorkerStaffItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> WORKER_INVENTORY_ITEM = ITEMS.registerItem("worker_inventory_item", WorkerInventoryItem::new, new Item.Properties().stacksTo(1));
    
    public static final Supplier<DataComponentType<SelectionType>> WORKER_SELECTION_TYPE_COMPONENT = DATA_COMPONENTS.registerComponentType(
    	    "worker_selection_type",
    	    builder -> builder
    	        // The codec to read/write the data to disk
    	        .persistent(SelectionType.CODEC)
    	        // The codec to read/write the data across the network
    	        .networkSynchronized(SelectionType.STREAM_CODEC)
    	);
    public static final Supplier<DataComponentType<JobType>> WORKER_JOB_TYPE_COMPONENT = DATA_COMPONENTS.registerComponentType(
    	    "worker_job_type",
    	    builder -> builder
    	        // The codec to read/write the data to disk
    	        .persistent(JobType.CODEC)
    	        // The codec to read/write the data across the network
    	        .networkSynchronized(JobType.STREAM_CODEC)
    	);
    public static final Supplier<DataComponentType<WorkerBlockSettings>> WORKER_BLOCK_SETTINGS_DATA_COMPONENT = DATA_COMPONENTS.registerComponentType(
    	    "worker_block_settings",
    	    builder -> builder
    	        // The codec to read/write the data to disk
    	        .persistent(WorkerBlockSettings.CODEC)
    	        // The codec to read/write the data across the network
    	        .networkSynchronized(WorkerBlockSettings.STREAM_CODEC)
    	);
    public static final Supplier<DataComponentType<WorkerInventorySettings>> WORKER_INVENTORY_SETTINGS_DATA_COMPONENT = DATA_COMPONENTS.registerComponentType(
    	    "worker_inventory_settings",
    	    builder -> builder
    	        // The codec to read/write the data to disk
    	        .persistent(WorkerInventorySettings.CODEC)
    	        // The codec to read/write the data across the network
    	        .networkSynchronized(WorkerInventorySettings.STREAM_CODEC)
    	);
    
    public static final DeferredItem<Item> MAGIC_BONEMEAL = ITEMS.registerItem("magic_bonemeal", MagicBonemealItem::new, new Item.Properties().stacksTo(1));
    
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> LOGISTICS_TAB = CREATIVE_MODE_TABS.register("logistics_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.wamutils.logistics")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> CONVEYOR_BELT_BLOCK_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(CONVEYOR_BELT_BLOCK_ITEM.get());
                output.accept(CONVEYOR_SPLITTER_BLOCK_ITEM.get());
                output.accept(ITEM_GRATE_BLOCK.get());
            }).build());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WORKER_TAB = CREATIVE_MODE_TABS.register("worker_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.wamutils.workers")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(WAMUtils.prefix("logistics_tab"))
            .icon(() -> WORKER_SPAWN_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(WORKER_SPAWN_ITEM.get());
                output.accept(WORKER_STAFF_ITEM.get());
                output.accept(WORKER_INVENTORY_ITEM.get());
                output.accept(MAGIC_BONEMEAL.get());
            }).build());
    
    
    // public static final Supplier<AttachmentType<ItemStackHandler>> ITEM_HANDLER_ATTACHMENT = ATTACHMENT_TYPES.register(
    //         "wam_utils_item_handler", () -> AttachmentType.serializable(holder -> {                
    //             if(holder instanceof ShieldRackBE shieldRackBE)
    //                 return new ShieldRackInventory(shieldRackBE);
    //             if (holder instanceof BaseBE baseBe)
    //                 return new ItemStackHandler(baseBe.getInventorySize());
    //             return new ItemStackHandler(1);
    //         }).build());
    
    
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITIES.register(modEventBus);
        ENTITY_SERIALIZERS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MENU_TYPES.register(modEventBus);
    }
    
    public static void registerCapabilites(RegisterCapabilitiesEvent event) {
    	List<Block> blocks = new ArrayList<>();
        blocks.add(CONVEYOR_BELT_BLOCK.get());
        blocks.add(CONVEYOR_SPLITTER_BLOCK.get());
        SHIELDRACK_BLOCKS.values().stream()
                .map(DeferredBlock::get).forEach(block -> blocks.add(block));        
        ENCHANTMENT_BOOK_SHELF_BLOCKS.values().stream()
                .map(DeferredBlock::get).forEach(block -> blocks.add(block));
        blocks.add(ENCHANTMENT_INDEXER_BLOCK.get());
        
        event.registerBlock(Capabilities.ItemHandler.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof BaseBE baseBe)
                        return baseBe.getItemHandler(side);
                    return null;
                },
                blocks.toArray(Block[]::new)
        );
    }

    public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        // Check for the specific vanilla tab
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            VANILLA_SORTED_WOOD_TYPES.forEach(woodType -> {
                event.accept(SHIELDRACK_BLOCK_ITEMS.get(woodType));
            });
            VANILLA_SORTED_WOOD_TYPES.forEach(woodType -> {
                event.accept(ENCHANTMENT_BOOK_SHELF_BLOCK_ITEMS.get(woodType));
            });
        }
    }
    
    public static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> block, Supplier<BlockBehaviour.Properties> properties) {
		return BLOCKS.register(name, () -> block.apply(properties.get().setId(ResourceKey.create(Registries.BLOCK, WAMUtils.prefix(name)))));
	}

    private static boolean never(BlockState state, BlockGetter blockGetter, BlockPos pos) {
        return false;
    }

    public static Boolean never(BlockState state, BlockGetter level, BlockPos pos, EntityType<?> entity) {
        return false;
    }

	public static void addCustomEntityAttributes(EntityAttributeCreationEvent event) {
		event.put(WORKER_ENTITY.get(), WorkerEntity.createAttributes().build());
	}
}
