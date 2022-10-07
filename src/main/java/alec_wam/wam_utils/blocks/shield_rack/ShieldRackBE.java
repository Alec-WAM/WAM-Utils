package alec_wam.wam_utils.blocks.shield_rack;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ShieldRackBE extends WAMUtilsBlockEntity {

	private ItemStack shieldStack = ItemStack.EMPTY;
	private ItemStack leftStack = ItemStack.EMPTY;
	private ItemStack rightStack = ItemStack.EMPTY;
	
	public ShieldRackBE(BlockPos pos, BlockState state) {
		super(BlockInit.SHIELD_RACK_BE.get(), pos, state);
	}

	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		shieldStack = ItemStack.EMPTY;
		leftStack = ItemStack.EMPTY;
		rightStack = ItemStack.EMPTY;
		
		if(nbt.contains("ShieldStack")) {
			shieldStack = ItemStack.of(nbt.getCompound("ShieldStack"));
		}
		if(nbt.contains("LeftStack")) {
			leftStack = ItemStack.of(nbt.getCompound("LeftStack"));
		}
		if(nbt.contains("RightStack")) {
			rightStack = ItemStack.of(nbt.getCompound("RightStack"));
		}
	}

	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		if(!shieldStack.isEmpty()) {
			nbt.put("ShieldStack", shieldStack.save(new CompoundTag()));
		}
		if(!leftStack.isEmpty()) {
			nbt.put("LeftStack", leftStack.save(new CompoundTag()));
		}
		if(!rightStack.isEmpty()) {
			nbt.put("RightStack", rightStack.save(new CompoundTag()));
		}
	}
	
	public ItemStack getLeftStack() {
		return leftStack;
	}

	public void setLeftStack(ItemStack leftStack) {
		this.leftStack = leftStack;
	}

	public ItemStack getRightStack() {
		return rightStack;
	}

	public void setRightStack(ItemStack rightStack) {
		this.rightStack = rightStack;
	}

	public ItemStack getShieldStack() {
		return shieldStack;
	}

	public void setShieldStack(ItemStack shieldStack) {
		this.shieldStack = shieldStack;
	}
	
	@Override
	public AABB getRenderBoundingBox() {
		BlockPos pos = this.getBlockPos();
		return new AABB(pos).inflate(0.2);
	}

}
