package alec_wam.wam_utils.blocks;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class WAMUtilsBlockEntity extends BlockEntity {

	public WAMUtilsBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void load(CompoundTag nbtIn) {
		super.load(nbtIn);
		this.readCustomNBT(nbtIn, false);
	}

	public abstract void readCustomNBT(CompoundTag nbt, boolean descPacket);

	@Override
	protected void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		this.writeCustomNBT(nbt, false);
	}

	public abstract void writeCustomNBT(CompoundTag nbt, boolean descPacket);

	@Override
	public boolean triggerEvent(int id, int type) {
		if (id == 0 || id == 255) {
			markBlockForUpdate(null);
			return true;
		} else if (id == 254) {
			BlockState state = level.getBlockState(worldPosition);
			level.sendBlockUpdated(worldPosition, state, state, 3);
			return true;
		}
		return super.triggerEvent(id, type);
	}
	
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this, be -> {
			CompoundTag nbttagcompound = new CompoundTag();
			this.writeCustomNBT(nbttagcompound, true);
			return nbttagcompound;
		});
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
		CompoundTag nonNullTag = pkt.getTag() != null ? pkt.getTag() : new CompoundTag();
		this.readCustomNBT(nonNullTag, true);
	}

	@Override
	public void handleUpdateTag(CompoundTag tag) {
		this.readCustomNBT(tag, true);
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag nbt = super.getUpdateTag();
		writeCustomNBT(nbt, true);
		return nbt;
	}

	public void receiveMessageFromServer(CompoundTag nbt) {

	}

	public void receiveMessageFromClient(CompoundTag nbt) {
	}
	
	public void markBlockForUpdate(@Nullable BlockState newState) {
		if (this.level != null)
			markBlockForUpdate(getBlockPos(), newState);
	}

	public void markBlockForUpdate(BlockPos pos, @Nullable BlockState newState) {
		BlockState state = level.getBlockState(pos);
		if (newState == null)
			newState = state;
		level.sendBlockUpdated(pos, state, newState, 3);
		level.updateNeighborsAt(pos, newState.getBlock());
	}

}
