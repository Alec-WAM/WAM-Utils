package alec_wam.wam_utils.events;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import alec_wam.wam_utils.blocks.advanced_portal.AdvancedPortalDataManager;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber
public class LevelEventHandler {
	private static final Logger LOGGER = LogUtils.getLogger();
    
	@SubscribeEvent
	public static void levelLoad(LevelEvent.Load event) {
		if(!event.getLevel().isClientSide()) {
			LOGGER.info("Loading Advanced Portals...");
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			AdvancedPortalDataManager.loadFromFile(server);
		}
	}
	
	@SubscribeEvent
	public static void saveLoad(LevelEvent.Save event) {
		if(!event.getLevel().isClientSide()) {
			if(AdvancedPortalDataManager.isDirty) {
				LOGGER.info("Saving Advanced Portals...");
				MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
				AdvancedPortalDataManager.saveToFile(server);
			}
		}
	}
	
}
