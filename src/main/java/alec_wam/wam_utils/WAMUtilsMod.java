package alec_wam.wam_utils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import alec_wam.wam_utils.client.ClientProxy;
import alec_wam.wam_utils.events.ServerResourceReloader;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ClientInit;
import alec_wam.wam_utils.init.ContainerInit;
import alec_wam.wam_utils.init.EntityInit;
import alec_wam.wam_utils.init.FluidInit;
import alec_wam.wam_utils.init.ItemInit;
import alec_wam.wam_utils.init.LootInit;
import alec_wam.wam_utils.init.RecipeInit;
import alec_wam.wam_utils.init.StructureInit;
import alec_wam.wam_utils.server.CommonProxy;
import alec_wam.wam_utils.server.network.IMessage;
import alec_wam.wam_utils.server.network.MessageBlockEntityUpdate;
import alec_wam.wam_utils.server.network.MessageContainerData;
import alec_wam.wam_utils.server.network.MessageContainerUpdate;
import alec_wam.wam_utils.server.network.MessageDestroyBlockEffect;
import alec_wam.wam_utils.server.network.MessagePlayerMovementUpdate;
import alec_wam.wam_utils.server.network.MessageUpdateClientAdvancedPortals;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(WAMUtilsMod.MODID)
public class WAMUtilsMod
{
    public static final String MODID = "wam_utils";
    public static final String VERSION = "1.0";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final CommonProxy proxy = DistExecutor.safeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
    
    public static final SimpleChannel packetHandler = NetworkRegistry.ChannelBuilder
			.named(new ResourceLocation(MODID, "main"))
			.networkProtocolVersion(() -> VERSION)
			.serverAcceptedVersions(VERSION::equals)
			.clientAcceptedVersions(VERSION::equals)
			.simpleChannel();
    
    public WAMUtilsMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        ItemInit.ITEMS.register(modEventBus);
        BlockInit.BLOCKS.register(modEventBus);
        BlockInit.BLOCK_ENTITIES.register(modEventBus);
        ContainerInit.CONTAINERS.register(modEventBus);
        EntityInit.ENTITIES.register(modEventBus);
        FluidInit.FLUID_TYPES.register(modEventBus);
        FluidInit.FLUIDS.register(modEventBus);
        EntityInit.POI.register(modEventBus);
        RecipeInit.RECIPES.register(modEventBus);
        RecipeInit.RECIPE_TYPES.register(modEventBus);
        StructureInit.FEATURES.register(modEventBus);
        StructureInit.CONFIGURED_FEATURES.register(modEventBus);
        StructureInit.BIOME_MODIFIERS.register(modEventBus);
        StructureInit.PLACED_FEATURES.register(modEventBus);
        LootInit.GLM.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        
        MinecraftForge.EVENT_BUS.addListener(this::serverReloadListener);
        
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modEventBus.addListener(ClientInit::init));
        
        ForgeMod.enableMilkFluid();
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
        
        registerMessage(MessageContainerUpdate.class, MessageContainerUpdate::new, NetworkDirection.PLAY_TO_SERVER);
		registerMessage(MessageContainerData.class, MessageContainerData::new, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(MessageBlockEntityUpdate.class, MessageBlockEntityUpdate::new);		
		registerMessage(MessageDestroyBlockEffect.class, MessageDestroyBlockEffect::new);	
		registerMessage(MessagePlayerMovementUpdate.class, MessagePlayerMovementUpdate::new, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(MessageUpdateClientAdvancedPortals.class, MessageUpdateClientAdvancedPortals::new, NetworkDirection.PLAY_TO_CLIENT);
		
		event.enqueueWork(() -> {
			StructureInit.registerConfiguredFeatures();
		});
    }
    
    private void serverReloadListener(final AddReloadListenerEvent event) {
		event.addListener(new ServerResourceReloader(event.getServerResources()));
	}

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
    
    //Packets
	private int messageId = 0;

	private <T extends IMessage> void registerMessage(Class<T> packetType, Function<FriendlyByteBuf, T> decoder) {
		registerMessage(packetType, decoder, Optional.empty());
	}

	private <T extends IMessage> void registerMessage(Class<T> packetType, Function<FriendlyByteBuf, T> decoder,
			NetworkDirection direction) {
		registerMessage(packetType, decoder, Optional.of(direction));
	}

	private final Set<Class<?>> knownPacketTypes = new HashSet<>();

	private <T extends IMessage> void registerMessage(Class<T> packetType, Function<FriendlyByteBuf, T> decoder,
			Optional<NetworkDirection> direction) {
		if (!knownPacketTypes.add(packetType))
			throw new IllegalStateException("Duplicate packet type: " + packetType.getName());
		packetHandler.registerMessage(messageId++, packetType, IMessage::toBytes, decoder, (t, ctx) -> {
			t.process(ctx);
			ctx.get().setPacketHandled(true);
		}, direction);
	}
}
