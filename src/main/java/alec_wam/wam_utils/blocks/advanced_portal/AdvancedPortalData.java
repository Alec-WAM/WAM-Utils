package alec_wam.wam_utils.blocks.advanced_portal;

import java.util.UUID;

import javax.annotation.Nullable;

import alec_wam.wam_utils.utils.BlockUtils;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;

public class AdvancedPortalData {

	private GlobalPos pos;
	private FrontAndTop orientation;
	
	private boolean isOn;
	private UUID linkedPortalUUID;
	private String name = "";
	private UUID ownerUUID;
	
	public AdvancedPortalData(GlobalPos pos, FrontAndTop orientation) {
		this.pos = pos;
		this.orientation = orientation;
	}
	
	/**
	 * Get the in world position of the portal's controller
	 * 
	 * @return - {@link net.minecraft.core.GlobalPos} of the controller
	 */
	public GlobalPos getControllerPos() {
		return pos;
	}
	
	/**
	 * Get the orientation info of the portal controller
	 * 
	 * @return - {@link net.minecraft.core.FrontAndTop} of the controller
	 */
	public FrontAndTop getOrientation() {
		return orientation;
	}
	
	/**
	 * Check if the portal is linked and turned on
	 * @return - if the portal is on
	 */
	public boolean isOn() {
		return isOn;
	}

	public AdvancedPortalData setIsOn(boolean value) {
		final boolean oldValue = isOn;
		this.isOn = value;
		if(oldValue != isOn) {
			AdvancedPortalDataManager.isDirty = true;
		}
		return this;
	}
	
	/**
	 * Get the UUID of the linked portal (Can be null)
	 * @return - UUID of the linked portal
	 */
	@Nullable
	public UUID getLinkedPortal() {
		return linkedPortalUUID;
	}

	public AdvancedPortalData setLinkedPortal(@Nullable UUID linkedPortal) {
		final UUID oldValue = this.linkedPortalUUID;
		this.linkedPortalUUID = linkedPortal;
		if(oldValue == null && this.linkedPortalUUID != null || oldValue != null && !oldValue.equals(this.linkedPortalUUID)) {
			AdvancedPortalDataManager.isDirty = true;
		}
		return this;
	}
	
	/**
	 * Get the display name of the portal
	 * @return - the display name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the portal
	 * @param name
	 */
	public AdvancedPortalData setName(String name) {
		final String oldValue = this.name;
		this.name = name;
		if(!oldValue.equals(name)) {
			AdvancedPortalDataManager.isDirty = true;
		}
		return this;
	}

	/**
	 * Get the UUID of the owner if the user has marked the portal private
	 * @return - UUID of the owner (Can be null)
	 */
	@Nullable
	public UUID getOwner() {
		return ownerUUID;
	}

	public AdvancedPortalData setOwner(@Nullable UUID ownerUUID) {
		final UUID oldValue = this.ownerUUID;
		this.ownerUUID = ownerUUID;
		if(oldValue == null && this.ownerUUID != null || oldValue != null && !oldValue.equals(this.ownerUUID)) {
			AdvancedPortalDataManager.isDirty = true;
		}
		return this;
	}

	public CompoundTag saveToNBT() {
		CompoundTag tag = new CompoundTag();
		tag.put("Pos", BlockUtils.writeGlobalPos(pos));
		tag.putString("Orientation", orientation.getSerializedName());
		
		tag.putString("Name", name);
		tag.putBoolean("On", isOn);
		if(this.linkedPortalUUID != null) {
			tag.putUUID("LinkedPortal", linkedPortalUUID);
		}
		if(this.ownerUUID != null) {
			tag.putUUID("Owner", ownerUUID);
		}
		return tag;
	}
	
	public static AdvancedPortalData loadFromNBT(CompoundTag tag) {
		GlobalPos pos = BlockUtils.loadGlobalPos(tag.getCompound("Pos"));
		
		FrontAndTop facing = FrontAndTop.NORTH_UP;
		String savedOrientation = tag.getString("Orientation");
		for(FrontAndTop o : FrontAndTop.values()) {
			if(o.getSerializedName().equalsIgnoreCase(savedOrientation)) {
				facing = o;
				break;
			}
		}
		
		boolean isOn = tag.getBoolean("On");
		String name = tag.getString("Name");
		UUID linkedPortal = tag.contains("LinkedPortal") ? tag.getUUID("LinkedPortal") : null;
		UUID owner = tag.contains("Owner") ? tag.getUUID("Owner") : null;
		
		AdvancedPortalData data = new AdvancedPortalData(pos, facing)
				.setIsOn(isOn)
				.setLinkedPortal(linkedPortal)
				.setName(name)
				.setOwner(owner);
		return data;
	}
	
}
