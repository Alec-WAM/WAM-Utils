package alec_wam.wam_utils.blocks.advanced_portal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.math.Vector3f;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.server.network.MessageBlockEntityUpdate;
import alec_wam.wam_utils.server.network.MessagePlayerMovementUpdate;
import alec_wam.wam_utils.utils.BlockUtils;
import alec_wam.wam_utils.utils.StringUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;

public class AdvancedPortalHostBE extends WAMUtilsBlockEntity {

	public static enum PortalType {
		RGB, NETHER, END;
	}
	
	public static class PortalSettings implements INBTSerializable<CompoundTag>{
		public PortalType portalType = PortalType.RGB;
		public Vector3f colorSettings = new Vector3f(255.0F, 255.0F, 255.0F);
		
		@Override
		public CompoundTag serializeNBT() {
			CompoundTag tag = new CompoundTag();
			tag.putInt("PortalType", portalType.ordinal());
			tag.putFloat("Red", colorSettings.x());
			tag.putFloat("Green", colorSettings.y());
			tag.putFloat("Blue", colorSettings.z());
			return tag;
		}
		
		@Override
		public void deserializeNBT(CompoundTag nbt) {
			this.portalType = PortalType.values()[nbt.getInt("PortalType") % PortalType.values().length];
			float r = nbt.contains("Red") ? nbt.getFloat("Red") : 255.0F;
			float g = nbt.contains("Green") ? nbt.getFloat("Green") : 255.0F;
			float b = nbt.contains("Blue") ? nbt.getFloat("Blue") : 255.0F;
			this.colorSettings = new Vector3f(r, g, b);
		}
	}
	
	private UUID portalUUID = Mth.createInsecureUUID();
	private GlobalPos teleportPos;
	private PortalSettings portalSettings = new PortalSettings();
	private List<UUID> ignoreEntityList = new ArrayList<UUID>();
	
	private boolean portalOn;
	
	public AdvancedPortalHostBE(BlockPos pos, BlockState state) {
		super(BlockInit.ADVANCED_PORTAL_HOST_BE.get(), pos, state);
	}

	public GlobalPos getTeleportPos() {
		return teleportPos;
	}

	public void setTeleportPos(GlobalPos teleportPos) {
		this.teleportPos = teleportPos;
		this.setChanged();
		this.markBlockForUpdate(null);
	}

	public void setPortalSettings(PortalSettings settings) {
		this.portalSettings = settings;
		this.setChanged();
		this.markBlockForUpdate(null);
	}

	public PortalSettings getPortalSettings() {
		return portalSettings;
	}
	
	public void ignoreEntity(UUID uuid) {
		if(!this.ignoreEntityList.contains(uuid)) {
			this.ignoreEntityList.add(uuid);
		}
	}
	
	public boolean isPortalOn() {
		return portalOn;
	}
	
	@Override
	public void receiveMessageFromClient(CompoundTag tag) {
		super.receiveMessageFromClient(tag);
		if(tag.contains("PortalName")) {
			AdvancedPortalData portalData = AdvancedPortalDataManager.getPortal(getPortalUUID());
			
			if(portalData != null) {
				portalData.setName(tag.getString("PortalName"));
			}
		}
		
		if(tag.contains("PortalLocked")) {
			AdvancedPortalData portalData = AdvancedPortalDataManager.getPortal(getPortalUUID());
			
			boolean isLocked = tag.getBoolean("PortalLocked");
			if(portalData != null) {
				if(!isLocked) {
					portalData.setOwner(null);
				}
				else {
					UUID uuid = tag.getUUID("Owner");
					portalData.setOwner(uuid);
				}
			}
		}
		
		if(tag.contains("PortalType")) {
			int type = tag.getInt("PortalType");
			this.portalSettings.portalType = PortalType.values()[type];
			this.setChanged();
			this.markBlockForUpdate(null);
		}
		
		if(tag.contains("PortalColors")) {
			int[] rgb = tag.getIntArray("PortalColors");
			Vector3f colors = new Vector3f(rgb[0], rgb[1], rgb[2]);
			this.portalSettings.colorSettings = colors;
			this.setChanged();
			this.markBlockForUpdate(null);
		}
		
		if(tag.contains("PortalLink")) {
			UUID uuid = tag.getUUID("PortalLink");
			if(uuid !=null) {
				this.linkToOtherPortal(uuid);
			}
		}
		
		if(tag.contains("PortalUnlink")) {
			this.unlinkFromOtherPortal();
		}
	}
	
	@Override
	public void receiveMessageFromServer(CompoundTag tag) {
		super.receiveMessageFromServer(tag);
		/*if(tag.contains("PortalOn")) {
			this.portalOn = tag.getBoolean("PortalOn");
		}*/
	}

	public void updatePortalOn(boolean value) {
		if(portalOn != value) {
			portalOn = value;
			if(!this.level.isClientSide) {
				this.setChanged();
				this.markBlockForUpdate(null);
			}
			/*CompoundTag tag = new CompoundTag();
			tag.putBoolean("PortalOn", value);
			WAMUtilsMod.packetHandler.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)), new MessageBlockEntityUpdate(this, tag));*/
		}
	}
	
	public void tickServer() {
		AdvancedPortalData portalData = AdvancedPortalDataManager.getPortal(getPortalUUID());
		
		if(portalData == null) {
			GlobalPos pos = GlobalPos.of(level.dimension(), getBlockPos());
			BlockState state = this.getBlockState();
			FrontAndTop orientation = state.getValue(AdvancedPortalHostBlock.ORIENTATION);
			AdvancedPortalData newPortalData = new AdvancedPortalData(pos, orientation);
			newPortalData.setName(StringUtils.getRandomName(level.random));
			AdvancedPortalDataManager.addPortal(getPortalUUID(), newPortalData);
			return;
		}

		boolean redstone = RedstoneMode.ON.isMet(level, worldPosition);
		if(teleportPos != null) {
			updatePortalOn(redstone);
		}
		else {
			updatePortalOn(portalData.isOn());
		}
		
		if(!redstone) {
			if(portalData.isOn()) {
				portalData.setIsOn(false);
			}
			return;
		}
		
		ServerLevel serverLevel = (ServerLevel)this.level;
			
		if(teleportPos != null) {
			AABB portalBB = getPortalBB();
			List<Entity> portalEntities = serverLevel.getEntitiesOfClass(Entity.class, portalBB, this::canTransportEntity);
			if(!portalEntities.isEmpty()) {
				for(Entity entity : portalEntities) {
					teleportEntity(entity, serverLevel, teleportPos);
				}
			}
		}
		else if(portalData.getLinkedPortal() !=null) {
			AdvancedPortalData otherPortalData = AdvancedPortalDataManager.getPortal(portalData.getLinkedPortal());
			
			if(otherPortalData == null) {
				if(portalData.isOn()) {
					portalData.setIsOn(false);
				}
				return;
			}
			
			if(!portalData.isOn()) {
				portalData.setIsOn(true);
			}
			
			AABB portalBB = getPortalBB();
			List<Entity> portalEntities = serverLevel.getEntitiesOfClass(Entity.class, portalBB, this::canTransportEntity);
			List<UUID> uuidToRemove = new ArrayList<UUID>(ignoreEntityList);
			if(!portalEntities.isEmpty()) {
				for(Entity entity : portalEntities) {
					if(ignoreEntityList.contains(entity.getUUID())) {
						uuidToRemove.remove(entity.getUUID());
						continue;
					}
					teleportEntityToOtherPortal(entity, serverLevel, otherPortalData);
				}
			}
			ignoreEntityList.removeAll(uuidToRemove);
		}
	}
	
	public void teleportEntity(Entity entity, ServerLevel level, GlobalPos pos) {
		/*if(entity.isVehicle()) {
			Entity driver = entity.getControllingPassenger();
			if(driver != null && driver instanceof Player) {
				return;
			}
		}*/
		if(pos !=null) {
			if(pos.dimension() == level.dimension()) {
				if(entity instanceof ServerPlayer player) {
					player.connection.teleport(teleportPos.pos().getX() + 0.5, teleportPos.pos().getY() + 0.5, teleportPos.pos().getZ() + 0.5, entity.getYRot(), entity.getXRot());
					player.fallDistance = 0.0F;
				}
				else {
					entity.moveTo(teleportPos.pos().getX() + 0.5, teleportPos.pos().getY() + 0.5, teleportPos.pos().getZ() + 0.5, entity.getYRot(), entity.getXRot());
					entity.fallDistance = 0.0F;
				}
			}
			else {
				if(entity.canChangeDimensions()) {
					final List<Entity> passengers = new ArrayList<Entity>(entity.getPassengers());
					ServerLevel otherLevel = level.getServer().getLevel(pos.dimension());							
					Entity teleportEntity = entity.changeDimension(otherLevel, new CustomTeleporter(pos, entity.getYRot()));
					if(teleportEntity !=null) {
						teleportEntity.fallDistance = 0.0F;
						if(!passengers.isEmpty()) {
							for(Entity passenger : passengers) {
								if(passenger.canChangeDimensions()) {
									Entity teleportPassenger = passenger.changeDimension(otherLevel, new CustomTeleporter(pos, passenger.getYRot()));
									teleportPassenger.startRiding(teleportEntity, true);							
								}
							}
						}
					}
				}
			}
		}
	}
	
	public void teleportEntityToOtherPortal(Entity entity, ServerLevel level, AdvancedPortalData otherPortalData) {
		teleportEntityToOtherPortal(entity, level, otherPortalData, null);
	}
	
	public void teleportEntityToOtherPortal(Entity entity, ServerLevel level, AdvancedPortalData otherPortalData, @Nullable Entity vehicle) {
		if(otherPortalData == null) return;
		
		if(!otherPortalData.isOn())return;
		
		/*if(entity.isVehicle()) {
			Entity driver = entity.getControllingPassenger();
			if(driver != null && driver instanceof Player) {
				return;
			}
		}*/
//		if(vehicle !=null) {
//			entity.startRiding(vehicle, true);
//		}
//		
//		
//		if(entity.isVehicle()) {
//			for(Entity passenger : entity.getPassengers()) {
//				if(passenger.isAlive()) {
//					teleportEntityToOtherPortal(passenger, level, otherPortalData, entity);
//				}
//			}
//		}
		
		GlobalPos otherPortalPos = otherPortalData.getControllerPos();
		
		ServerLevel otherLevel = otherPortalPos.dimension() == this.level.dimension() ? (ServerLevel)this.level : this.level.getServer().getLevel(otherPortalPos.dimension());	
		
		if(otherLevel == null)return;
		
		AdvancedPortalHostBE otherPortal = null;
		BlockEntity be = otherLevel.getBlockEntity(otherPortalPos.pos());
		if(be !=null && be instanceof AdvancedPortalHostBE portal) {
			otherPortal = portal;
		}
		
		if(otherPortal == null)return;
		
		FrontAndTop orientation = otherPortalData.getOrientation();
		Direction facing = orientation.front();
		//Direction rotation = orientation.top();
		
		if(otherLevel !=null) {
			float otherYRot = entity.getYRot();
			if(facing.getAxis().isHorizontal()) {
				otherYRot = facing.toYRot();
			}
			
			GlobalPos otherEntryPos = GlobalPos.of(otherLevel.dimension(), otherPortal.getEntryPos());
			if(otherLevel.dimension() == level.dimension()) {
				if(entity instanceof ServerPlayer player) {
					final Vec3 preTeleport = player.getDeltaMovement();
					player.connection.teleport(otherEntryPos.pos().getX() + 0.5, otherEntryPos.pos().getY() + 0.5, otherEntryPos.pos().getZ() + 0.5, otherYRot, entity.getXRot());
					player.fallDistance = 0.0F;
					
					double speed = preTeleport.length();
					Vec3 newMotion = new Vec3(speed * facing.getNormal().getX(), speed * facing.getNormal().getY(), speed * facing.getNormal().getZ());
					player.setDeltaMovement(newMotion);
					player.setYHeadRot(otherYRot);
					WAMUtilsMod.packetHandler.sendTo(new MessagePlayerMovementUpdate(newMotion), player.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
					otherPortal.ignoreEntity(player.getUUID());
				}
				else {
					final Vec3 preTeleport = entity.getDeltaMovement();
					double speed = preTeleport.length();
					Vec3 newMotion = new Vec3(speed * facing.getNormal().getX(), speed * facing.getNormal().getY(), speed * facing.getNormal().getZ());
					entity.moveTo(otherEntryPos.pos().getX() + 0.5, otherEntryPos.pos().getY() + 0.5, otherEntryPos.pos().getZ() + 0.5, otherYRot, entity.getXRot());
					
					entity.fallDistance = 0.0F;
					entity.setDeltaMovement(newMotion);
					entity.setYRot(otherYRot);
					entity.setYHeadRot(otherYRot);
					entity.setYBodyRot(otherYRot);
					otherPortal.ignoreEntity(entity.getUUID());
					
					Entity passenger = entity.getControllingPassenger();
					if(passenger == null && !entity.getPassengers().isEmpty()) {
						passenger = entity.getPassengers().get(0);
					}
					
					if(passenger !=null) {				
						passenger.setYHeadRot(otherYRot);		
						passenger.setYRot(otherYRot);
						passenger.setYBodyRot(otherYRot);
					}
				}
			}
			else {
				if(entity.canChangeDimensions()) {
					final Vec3 preTeleport = entity.getDeltaMovement();
					final List<Entity> passengers = new ArrayList<Entity>(entity.getPassengers());
					Entity teleportEntity = entity.changeDimension(otherLevel, new CustomTeleporter(otherEntryPos, otherYRot));
					if(teleportEntity !=null) {
						teleportEntity.fallDistance = 0.0F;
						
						double speed = preTeleport.length();
						Vec3 newMotion = new Vec3(speed * facing.getNormal().getX(), speed * facing.getNormal().getY(), speed * facing.getNormal().getZ());
						teleportEntity.setDeltaMovement(newMotion);
						teleportEntity.setYHeadRot(otherYRot);
						if(teleportEntity instanceof ServerPlayer player) {
							WAMUtilsMod.packetHandler.sendTo(new MessagePlayerMovementUpdate(newMotion), player.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
						}
						otherPortal.ignoreEntity(teleportEntity.getUUID());
						if(!passengers.isEmpty()) {
							for(Entity passenger : passengers) {
								if(passenger.canChangeDimensions()) {
									Entity teleportPassenger = passenger.changeDimension(otherLevel, new CustomTeleporter(otherEntryPos, otherYRot));
									teleportPassenger.startRiding(teleportEntity, true);
									otherPortal.ignoreEntity(teleportEntity.getUUID());									
								}
							}
						}
					}
				}
			}
		}
	}
	
	public boolean canTransportEntity(Entity entity) {
		return entity.isAlive() && !entity.isPassenger();
	}
	
	public AABB getPortalBB() {
		BlockState state = this.getBlockState();
		FrontAndTop orientation = state.getValue(AdvancedPortalHostBlock.ORIENTATION);
		Direction facing = orientation.front();
		Direction rotation = orientation.top();
		Axis axis = facing.getAxis();
		BlockPos middle = this.worldPosition.relative(rotation, 2);
		double inflateX = axis == Axis.X ? 0.0 : 1.0D;
		double inflateY = axis == Axis.Y ? 0.0 : 1.0D;
		double inflateZ = axis == Axis.Z ? 0.0 : 1.0D;
		return new AABB(middle).inflate(inflateX, inflateY, inflateZ);
	}
	
	public BlockPos getEntryPos() {
		BlockState state = this.getBlockState();
		FrontAndTop orientation = state.getValue(AdvancedPortalHostBlock.ORIENTATION);
		Direction facing = orientation.front();
		Axis axis = facing.getAxis();
		if(axis.isHorizontal()) {
			return getBlockPos().above();
		}
		else {
			Direction rotation = orientation.top();
			BlockPos middle = getBlockPos().relative(rotation, 2);
			return middle;
		}
	}
	
	@Override
	public AABB getRenderBoundingBox() {
		return getPortalBB();
	}
	
	public UUID getPortalUUID() {
		return portalUUID;
	}

	public void setPortalUUID(UUID portalUUID) {
		this.portalUUID = portalUUID;
	}
	
	public void unlinkFromOtherPortal() {
		if(this.level !=null && !this.level.isClientSide) {
			AdvancedPortalData data = AdvancedPortalDataManager.getPortal(getPortalUUID());
			if(data !=null) {
				if(data.getLinkedPortal() != null) {
					AdvancedPortalData otherData = AdvancedPortalDataManager.getPortal(data.getLinkedPortal());
					if(otherData !=null) {
						otherData.setLinkedPortal(null);
					}
					data.setLinkedPortal(null);
				}
			}
		}
	}
	
	public void linkToOtherPortal(UUID uuid) {
		if(this.level !=null && !this.level.isClientSide) {
			AdvancedPortalData data = AdvancedPortalDataManager.getPortal(getPortalUUID());
			if(data.getLinkedPortal() == null) {
				AdvancedPortalData otherData = AdvancedPortalDataManager.getPortal(uuid);
				if(otherData !=null) {
					if(otherData.getLinkedPortal() == null) {
						data.setLinkedPortal(uuid);			
						otherData.setLinkedPortal(getPortalUUID());
					}
				}
			}
		}
	}

	public void removePortal() {
		if(level !=null && !level.isClientSide) {
			AdvancedPortalDataManager.removePortal(portalUUID);
		}
	}

	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		this.teleportPos = null;
		if(nbt.contains("TeleportPos")) {
			this.teleportPos = BlockUtils.loadGlobalPos(nbt.getCompound("TeleportPos"));
		}
		
		if(nbt.contains("PortalUUID")) {
			this.portalUUID = nbt.getUUID("PortalUUID");
		}
		
		this.portalSettings = new PortalSettings();
		if(nbt.contains("PortalSettings")) {
			this.portalSettings.deserializeNBT(nbt.getCompound("PortalSettings"));
		}
		
		this.portalOn = nbt.getBoolean("PortalOn");
	}

	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		if(this.teleportPos != null) {
			nbt.put("TeleportPos", BlockUtils.writeGlobalPos(teleportPos));
		}
		
		if(this.portalUUID != null) {
			nbt.putUUID("PortalUUID", portalUUID);
		}
		
		if(this.portalSettings != null) {
			nbt.put("PortalSettings", this.portalSettings.serializeNBT());
		}
		
		nbt.putBoolean("PortalOn", this.portalOn);
	}

}
