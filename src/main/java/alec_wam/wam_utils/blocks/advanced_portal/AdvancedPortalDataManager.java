package alec_wam.wam_utils.blocks.advanced_portal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.advanced_portal.AdvancedPortalClientDataManager.ClientAdvancedPortalData;
import alec_wam.wam_utils.server.network.MessageUpdateClientAdvancedPortals;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.network.NetworkDirection;

public class AdvancedPortalDataManager {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String FILE_ID = "advanced_portals";
	public static final LevelResource DATA_DIR = new LevelResource("data");
	private static final Map<UUID, AdvancedPortalData> PORTALS = new HashMap<UUID, AdvancedPortalData>();
	public static final List<ServerPlayer> UI_PLAYERS = new ArrayList<ServerPlayer>();
	public static boolean isDirty;

	public static void addPortal(UUID uuid, AdvancedPortalData data) {
		PORTALS.put(uuid, data);
		isDirty = true;
		sendAllUIPlayersPortals();
	}
	
	public static void removePortal(UUID uuid) {
		if(uuid !=null) {
			PORTALS.remove(uuid);
			for(AdvancedPortalData data : PORTALS.values()) {
				if(data.getLinkedPortal() !=null && data.getLinkedPortal().equals(uuid)) {
					data.setLinkedPortal(null);
				}
			}
			isDirty = true;
			sendAllUIPlayersPortals();
		}
	}
	
	public static AdvancedPortalData getPortal(UUID uuid) {
		return PORTALS.get(uuid);
	}
	
	private static void loadFromNBT(CompoundTag tag) {
		ListTag listtag = tag.getList("AdvancedPortals", 10);

		PORTALS.clear();
		for(int i = 0; i < listtag.size(); ++i) {
			CompoundTag compoundtag = listtag.getCompound(i);
			UUID uuid = compoundtag.getUUID("UUID");
			AdvancedPortalData data = AdvancedPortalData.loadFromNBT(compoundtag.getCompound("PortalData"));
			PORTALS.put(uuid, data);
		}
	}
	
	private static CompoundTag saveToNBT(CompoundTag tag) {
		ListTag listtag = new ListTag();

		for(Entry<UUID, AdvancedPortalData> entry : PORTALS.entrySet()) {
			CompoundTag compoundtag = new CompoundTag();
			compoundtag.putUUID("UUID", entry.getKey());
			compoundtag.put("PortalData", entry.getValue().saveToNBT());
			listtag.add(compoundtag);
		}

		tag.put("AdvancedPortals", listtag);
		return tag;
	}
	
	public static File getDataFile(MinecraftServer server) {
		File dataFolder = server.getWorldPath(DATA_DIR).toFile();
		return new File(dataFolder, FILE_ID + ".dat");
	}
	
	public static void loadFromFile(MinecraftServer server) {
		try {
			File file1 = getDataFile(server);
			if (file1.exists()) {
				CompoundTag compoundtag = NbtIo.readCompressed(file1);
				loadFromNBT(compoundtag.getCompound("data"));
			}
		} catch (Exception exception) {
			LOGGER.error("Error loading saved advanced portal data: {}", exception);
		}
	}
	
	@SuppressWarnings("deprecation")
	public static void saveToFile(MinecraftServer server) {
		if (isDirty) {
			CompoundTag compoundtag = new CompoundTag();
			compoundtag.put("data", saveToNBT(new CompoundTag()));
			compoundtag.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());

			try {
				NbtIo.writeCompressed(compoundtag, getDataFile(server));
			} catch (IOException ioexception) {
				LOGGER.error("Could not save advanced portal data {}", ioexception);
			}

			isDirty = false;
		}
	}

	public static List<ClientAdvancedPortalData> getPlayerVisiblePortals(UUID uuid) {
		List<ClientAdvancedPortalData> portals = new ArrayList<ClientAdvancedPortalData>();
		for(Entry<UUID, AdvancedPortalData> entry : PORTALS.entrySet()) {
			AdvancedPortalData data = entry.getValue();
			if(data.getOwner() == null || data.getOwner().equals(uuid)) {
				portals.add(new ClientAdvancedPortalData(entry.getKey(), data.getLinkedPortal(), data.getName(), data.getOwner() != null));
			}
		}
		return portals;
	}

	public static void sendPlayerPortals(ServerPlayer serverPlayer) {
		List<ClientAdvancedPortalData> visiblePortals = getPlayerVisiblePortals(serverPlayer.getUUID());
		WAMUtilsMod.packetHandler.sendTo(new MessageUpdateClientAdvancedPortals(visiblePortals), serverPlayer.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
	}
	
	public static void sendAllUIPlayersPortals() {
		for(ServerPlayer serverPlayer : UI_PLAYERS) {
			List<ClientAdvancedPortalData> visiblePortals = getPlayerVisiblePortals(serverPlayer.getUUID());
			WAMUtilsMod.packetHandler.sendTo(new MessageUpdateClientAdvancedPortals(visiblePortals), serverPlayer.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
		}
	}
}
