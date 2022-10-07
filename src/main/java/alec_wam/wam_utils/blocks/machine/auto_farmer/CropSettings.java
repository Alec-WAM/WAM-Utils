package alec_wam.wam_utils.blocks.machine.auto_farmer;

import alec_wam.wam_utils.utils.BlockUtils;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;

public class CropSettings implements INBTSerializable<CompoundTag> {
	private boolean shouldPlant;
	private boolean shouldHarvest;
	private boolean shouldGrow;
	private ItemStack seed = ItemStack.EMPTY;
	private Vec3i pos;
	
	private CropSettings() {
		
	}
	
	public CropSettings(CropSettings settings) {
		this.pos = new Vec3i(settings.getPos().getX(), settings.getPos().getY(), settings.getPos().getZ());
		this.shouldPlant = settings.shouldPlant;
		this.shouldHarvest = settings.shouldHarvest;
		this.shouldGrow = settings.shouldGrow;
		this.seed = settings.seed.copy();
	}
	
	public CropSettings(Vec3i pos) {
		this(pos, true, true, true, ItemStack.EMPTY);
	}
	
	public CropSettings(Vec3i pos, boolean shouldPlant, boolean shouldHarvest, boolean shouldGrow) {
		this(pos, shouldPlant, shouldHarvest, shouldGrow, ItemStack.EMPTY);
	}
	
	public CropSettings(Vec3i pos, boolean shouldPlant, boolean shouldHarvest, boolean shouldGrow, ItemStack seed) {
		this.pos = pos;
		this.setShouldPlant(shouldPlant);
		this.setShouldHarvest(shouldHarvest);
		this.setShouldGrow(shouldGrow);
		this.seed = seed;
	}

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.put("Pos", BlockUtils.saveVec3i(pos));
		tag.putBoolean("Plant", shouldPlant);
		tag.putBoolean("Harvest", shouldHarvest);
		tag.putBoolean("Grow", shouldGrow);
		if(!seed.isEmpty())tag.put("Seed", seed.serializeNBT());
		return tag;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		this.pos = BlockUtils.loadVec3i(nbt, "Pos");
		this.shouldPlant = nbt.getBoolean("Plant");
		this.shouldHarvest = nbt.getBoolean("Harvest");
		this.shouldGrow = nbt.getBoolean("Grow");
		if(nbt.contains("Seed")) {
			this.seed = ItemStack.of(nbt.getCompound("Seed"));
		}
	}
	
	public static CropSettings loadFromNBT(CompoundTag nbt) {
		CropSettings settings = new CropSettings();
		settings.deserializeNBT(nbt);
		return settings;
	}

	public ItemStack getSeed() {
		return seed;
	}

	public void setSeed(ItemStack seed) {
		this.seed = seed;
	}

	public Vec3i getPos() {
		return pos;
	}

	public void setPos(Vec3i pos) {
		this.pos = pos;
	}

	public boolean shouldPlant() {
		return shouldPlant;
	}

	public void setShouldPlant(boolean shouldPlant) {
		this.shouldPlant = shouldPlant;
	}

	public boolean shouldHarvest() {
		return shouldHarvest;
	}

	public void setShouldHarvest(boolean shouldHarvest) {
		this.shouldHarvest = shouldHarvest;
	}

	public boolean shouldGrow() {
		return shouldGrow;
	}

	public void setShouldGrow(boolean shouldGrow) {
		this.shouldGrow = shouldGrow;
	}
}
