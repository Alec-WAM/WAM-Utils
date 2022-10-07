package alec_wam.wam_utils.blocks.machine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;

public class SlotLock implements INBTSerializable<CompoundTag> {

	private boolean enabled;
	private ItemStack stack = ItemStack.EMPTY;
	
	private SlotLock() {
		
	}
	
	public SlotLock(boolean enabled) {
		this(enabled, ItemStack.EMPTY);
	}
	
	public SlotLock(boolean enabled, ItemStack stack) {
		this.enabled = enabled;
		this.stack = stack;
	}
	
	
	@Override
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("Enabled", enabled);
		if(!stack.isEmpty()) {
			tag.put("Item", stack.serializeNBT());
		}
		return tag;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		this.enabled = nbt.getBoolean("Enabled");
		if(nbt.contains("Item")) {
			this.stack = ItemStack.of(nbt.getCompound("Item"));
		}
	}
	
	public static SlotLock loadFromNBT(CompoundTag nbt) {
		SlotLock lock = new SlotLock();
		lock.deserializeNBT(nbt);
		return lock;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public ItemStack getStack() {
		return stack;
	}

	public void setStack(ItemStack stack) {
		this.stack = stack;
	}
	
}
