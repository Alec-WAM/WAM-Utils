package alec_wam.wam_utils.blocks.mirror_block;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.blocks.INBTItemDrop;
import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.utils.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class BlockMirrorBE extends WAMUtilsBlockEntity implements INBTItemDrop {

	private BlockPos linkedPos;
	
	public BlockMirrorBE(BlockPos p_155229_, BlockState p_155230_) {
		super(BlockInit.BLOCK_MIRROR_BE.get(), p_155229_, p_155230_);
	}

	public BlockPos getLinkedPos() {
		return linkedPos;
	}
	
	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		this.linkedPos = null;
		if(nbt.contains("LinkedPos")) {
			this.linkedPos = BlockUtils.loadBlockPos(nbt, "LinkedPos");
		}
	}

	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		if(this.linkedPos != null) {
			nbt.put("LinkedPos", BlockUtils.saveBlockPos(linkedPos));
		}
	}
	
	@Override
	public void readFromItem(ItemStack stack) {
		if(stack.hasTag()) {
			if(stack.getTag().contains("LinkedPos")) {
				this.linkedPos = BlockUtils.loadBlockPos(stack.getTag(), "LinkedPos");
			}
		}
	}

	@Override
	public ItemStack getNBTDrop(Item item) {
		ItemStack stack = new ItemStack(item);
		if(this.linkedPos !=null) {
			CompoundTag tag = stack.getOrCreateTag();
			tag.put("LinkedPos", BlockUtils.saveBlockPos(linkedPos));
			stack.setTag(tag);
		}
		return stack;
	}
	
	@NotNull
	@Override
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
		if(this.linkedPos !=null) {
			if(this.level !=null) {
				if(level.isLoaded(linkedPos)) {
					BlockEntity be = level.getBlockEntity(linkedPos);
					if(be !=null) {
						return be.getCapability(cap);
					}
				}
			}
		}
		return super.getCapability(cap);
	}
	
	@NotNull
	@Override
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if(this.linkedPos !=null) {
			if(this.level !=null) {
				if(level.isLoaded(linkedPos)) {
					BlockEntity be = level.getBlockEntity(linkedPos);
					if(be !=null) {
						return be.getCapability(cap, side);
					}
				}
			}
		}
		return super.getCapability(cap, side);
	}

}
