package alec_wam.wam_utils.blocks.xp_vacuum;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.blocks.INBTItemDrop;
import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.capabilities.XPFluidStorage;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ItemInit;
import alec_wam.wam_utils.utils.InventoryUtils;
import alec_wam.wam_utils.utils.XPUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;

public class XPVacuumBE extends WAMUtilsBlockEntity implements INBTItemDrop {

	public static final int MAX_RANGE_UPGRADES = 4;
	public static final int UPGRADE_SLOTS = 1;
	
	public XPFluidStorage fluidStorage = createFluidHandler();
    private LazyOptional<XPFluidStorage> fluid = LazyOptional.of(() -> fluidStorage);
    protected final ItemStackHandler upgradeItems = createUpgradeItemHandler();
	
    private RedstoneMode redstoneMode = RedstoneMode.ON;
	private int rangeUpgrades = 0;
	private boolean showBoundingBox = true;
	
	public XPVacuumBE(BlockPos pos, BlockState state) {
		super(BlockInit.XP_VACUUM_BE.get(), pos, state);
	}
	
	public void setRedstoneMode(RedstoneMode redstoneMode) {
		this.redstoneMode = redstoneMode;
		if(!this.level.isClientSide) {
			this.setChanged();
		}
	}
	
	public RedstoneMode getRedstoneMode() {
		return redstoneMode;
	}

	public boolean showBoundingBox() {
		return showBoundingBox;
	}

	public void setShowBoundingBox(boolean showBoundingBox) {
		this.showBoundingBox = showBoundingBox;
		if(!this.level.isClientSide) {
			this.setChanged();
		}
	}

	@Override
	public void receiveMessageFromClient(CompoundTag nbt) {
		if(nbt.contains("RedstoneMode")) {
			this.redstoneMode = RedstoneMode.getMode(nbt.getInt("RedstoneMode"));
			this.setChanged();
		}
		if(nbt.contains("ShowBoundingBox")) {
			setShowBoundingBox(nbt.getBoolean("ShowBoundingBox"));
		}
		if(nbt.contains("GiveXP")) {
			UUID uuid = nbt.getUUID("PlayerUUID");
			int levels = nbt.getInt("Levels");
			Player player = level.getPlayerByUUID(uuid);
			givePlayerXP(player, levels);
		}
	}
	
	public void givePlayerXP(Player player, int level) {
		int levels = level < 0 ? this.fluidStorage.getExperienceTotal() : level;
		if(player !=null) {
			this.fluidStorage.givePlayerXp(player, levels);
		}
	}
	
	public void tickCommon() {
		if(!this.redstoneMode.isMet(level, worldPosition)) {
			return;
		}
		
		vacuumXP(level, worldPosition, getRangeBB(), 8.0, 1.0D);
	}
	
	public void vacuumXP(Level level, BlockPos pos, AABB bb, double range, double pickUpDistance){
		//double maxDist = range * 2;

		List<ExperienceOrb> xp = level.getEntitiesOfClass(ExperienceOrb.class, bb, (entity) -> {
			return entity.isAlive();
		});
		for (ExperienceOrb entity : xp) {
			double xDist = (pos.getX() + 0.5D - entity.getX());
			double yDist = (pos.getY() + 0.5D - entity.getY());
			double zDist = (pos.getZ() + 0.5D - entity.getZ());

			double totalDistance = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);

			if (totalDistance < pickUpDistance) {
				if (entity.isAlive()) {
					int xpValue = entity.getValue();
					if (xpValue > 0) {
						int added = fluidStorage.addExperience(xpValue);
						if(added > 0){
							entity.value -= added;
							if(entity.getValue() <=0){
								if(!level.isClientSide){
									level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.05F, (level.random.nextFloat() - level.random.nextFloat()) * 0.35F + 0.9F);
								}
								entity.remove(RemovalReason.KILLED);
							}
						}
					}
				}
			} else if(fluidStorage.getFluidAmount() < fluidStorage.getCapacity()){
				/*double d = 1 - (Math.max(0.1, totalDistance) / maxDist);
				double speed = 0.01 + (d * 0.02);
				double x = entity.getDeltaMovement().x + (xDist / totalDistance * speed);
				double z = entity.getDeltaMovement().z + (zDist / totalDistance * speed);
				double y = entity.getDeltaMovement().y + (yDist / totalDistance * speed);
				if (yDist > 0.5) {
					y = 0.12;
				}
				entity.setDeltaMovement(entity.getDeltaMovement().add(x, y, z));*/
				Vec3 targetPos = new Vec3(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D);
				Vec3 vec3 = new Vec3(targetPos.x() - entity.getX(), targetPos.y() - entity.getY(), targetPos.z() - entity.getZ());
				double d0 = vec3.lengthSqr();
				if (d0 < 64.0D) {
					double d1 = 1.0D - Math.sqrt(d0) / 8.0D;
					Vec3 motion = vec3.normalize().scale(d1 * d1 * 0.1D);
					entity.setDeltaMovement(entity.getDeltaMovement().add(motion));
					entity.move(MoverType.SELF, entity.getDeltaMovement());
				}
			}
		}
	}
	
	@Override
	public AABB getRenderBoundingBox() {
		return getRangeBB();
	}
	
	public AABB getRangeBB() {
		double radius = getRadius() / 2.0D;
		AABB aabb = new AABB(getBlockPos()).inflate(radius, radius, radius);
        return aabb; 
	}
	
	public double getRadius() {
		return 8.0D + (2.0D * Math.min(MAX_RANGE_UPGRADES, rangeUpgrades));
	}

	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		redstoneMode = RedstoneMode.getMode(nbt.getInt("RedstoneMode"));
		showBoundingBox = nbt.getBoolean("ShowBoundingBox");
		rangeUpgrades = nbt.getInt("RangeUpgrades");
		fluid.ifPresent(h -> h.deserializeNBT(nbt.getCompound("XP")));
		if (nbt.contains("Inventory.Upgrades")) {
			upgradeItems.deserializeNBT(nbt.getCompound("Inventory.Upgrades"));
		}
	}

	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		nbt.putInt("RedstoneMode", redstoneMode.ordinal());
		nbt.putBoolean("ShowBoundingBox", showBoundingBox);
		nbt.putInt("RangeUpgrades", rangeUpgrades);
		fluid.ifPresent(h -> nbt.put("XP", h.serializeNBT()));
		nbt.put("Inventory.Upgrades", upgradeItems.serializeNBT());
	}

	@Override
	public void readFromItem(ItemStack stack) {
		if(stack.hasTag()) {
			CompoundTag nbt = stack.getTag();
			redstoneMode = RedstoneMode.getMode(nbt.getInt("RedstoneMode"));
			showBoundingBox = nbt.getBoolean("ShowBoundingBox");
			fluid.ifPresent(h -> h.deserializeNBT(nbt.getCompound("XP")));
		}
	}

	@Override
	public ItemStack getNBTDrop(Item item) {
		ItemStack drop = new ItemStack(item, 1);
		CompoundTag nbt = new CompoundTag();
		nbt.putInt("RedstoneMode", redstoneMode.ordinal());
		nbt.putBoolean("ShowBoundingBox", showBoundingBox);
		fluid.ifPresent(h -> nbt.put("XP", h.serializeNBT()));
		drop.setTag(nbt);
		return drop;
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		fluid.invalidate();
	}
	
	public boolean isItemAValidUpgradeItem(ItemStack stack, int slot) {
		if (!stack.isEmpty()) {
			if (stack.getItem() == ItemInit.UPGRADE_RANGE.get()) {
				return true;
			}
		}
		return false;
	}
	
	public void updateUpgrades() {
		this.rangeUpgrades = InventoryUtils.countItem(upgradeItems, ItemInit.UPGRADE_RANGE.get());
	}
	
	@Nonnull
	private ItemStackHandler createUpgradeItemHandler() {
		return new ItemStackHandler(UPGRADE_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				updateUpgrades();
				setChanged();
			}

			@Override
			public int getSlotLimit(int slot) {
				return MAX_RANGE_UPGRADES;
			}

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return isItemAValidUpgradeItem(stack, slot);
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				if (!isItemAValidUpgradeItem(stack, slot)) {
					return stack;
				}
				return super.insertItem(slot, stack, simulate);
			}
		};
	}

	@Nonnull
	private XPFluidStorage createFluidHandler() {
		return new XPFluidStorage(XPUtil.getExperienceForLevel(50)) {
			@Override
			protected void onContentsChanged() {
				setChanged();
				markBlockForUpdate(null);
			}
			
			@Override
			public boolean canFill(FluidStack fluid) {
				return false;
			}
		};
	}
	
	@NotNull
	@Override
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluid.cast();
		}
		return super.getCapability(cap, side);
	}

}
