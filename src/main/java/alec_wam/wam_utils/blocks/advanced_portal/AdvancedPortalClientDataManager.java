package alec_wam.wam_utils.blocks.advanced_portal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.network.FriendlyByteBuf;

public class AdvancedPortalClientDataManager {

	public static class ClientAdvancedPortalData {
		private UUID portalUUID;
		private String name;
		private UUID linkedPortalUUID;
		private boolean isPrivatePortal;
		
		public ClientAdvancedPortalData(UUID portalUUID, UUID linkedPortalUUID, String name, boolean isPrivatePortal) {
			this.portalUUID = portalUUID;
			this.linkedPortalUUID = linkedPortalUUID;
			this.name = name == null ? "" : name;
			this.isPrivatePortal = isPrivatePortal;
		}
		
		public void writeToBuf(FriendlyByteBuf buf) {
			buf.writeUUID(portalUUID);
			buf.writeBoolean(linkedPortalUUID != null);
			if(linkedPortalUUID != null) {
				buf.writeUUID(linkedPortalUUID);
			}
			buf.writeUtf(name);
			buf.writeBoolean(isPrivatePortal);
		}
		
		public static ClientAdvancedPortalData readFromBuf(FriendlyByteBuf buf) {
			UUID uuid = buf.readUUID();
			boolean hasLinkedPortal = buf.readBoolean();
			UUID linkedUUID = null;
			if(hasLinkedPortal) {
				linkedUUID = buf.readUUID();
			}
			String name = buf.readUtf();
			boolean isPrivate = buf.readBoolean();
			return new ClientAdvancedPortalData(uuid, linkedUUID, name, isPrivate);
		}
		
		public UUID getPortalUUID() {
			return portalUUID;
		}
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			if(name !=null) {
				this.name = name;
			}
		}
		
		public void setPrivatePortal(boolean value) {
			isPrivatePortal = value;
		}
		
		public boolean isPrivatePortal() {
			return isPrivatePortal;
		}
		
		public UUID getLinkedPortalUUID() {
			return linkedPortalUUID;
		}
		
		public void setLinkedPortalUUID(UUID otherPortalUUID) {
			this.linkedPortalUUID = otherPortalUUID;
		}
	}
	
	private static List<ClientAdvancedPortalData> CLIENT_PORTALS = new ArrayList<ClientAdvancedPortalData>();
	
	public static void setPortals(List<ClientAdvancedPortalData> portals) {
		CLIENT_PORTALS = portals;
	}
	
	public static List<ClientAdvancedPortalData> getAllPortals(boolean onlyPrivate){
		if(onlyPrivate) {
			return CLIENT_PORTALS.stream().filter((portal) -> portal.isPrivatePortal()).toList();
		}
		return CLIENT_PORTALS;
	}
	
	public static void unlinkPortals(UUID portalUUID) {
		Optional<ClientAdvancedPortalData> data = AdvancedPortalClientDataManager.getAllPortals(false).stream().filter((portal) -> portal.getPortalUUID().equals(portalUUID)).findFirst();
		if(data.isPresent()) {
			ClientAdvancedPortalData mainPortal = data.get();
			if(mainPortal.getLinkedPortalUUID() != null) {
				Optional<ClientAdvancedPortalData> otherData = AdvancedPortalClientDataManager.getAllPortals(false).stream().filter((portal) -> portal.getPortalUUID().equals(portalUUID)).findFirst();
				if(otherData.isPresent()) {
					ClientAdvancedPortalData otherPortal = otherData.get();
					otherPortal.setLinkedPortalUUID(null);					
				}
			}
			mainPortal.setLinkedPortalUUID(null);
		}
	}
	
}
