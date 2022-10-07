package alec_wam.wam_utils.server.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.Objects;
import java.util.function.Supplier;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;

public class MessageBlockEntityUpdate implements IMessage {
	private final BlockPos pos;
	private final CompoundTag nbt;

	public MessageBlockEntityUpdate(WAMUtilsBlockEntity tile, CompoundTag nbt) {
		this.pos = tile.getBlockPos();
		this.nbt = nbt;
	}

	public MessageBlockEntityUpdate(FriendlyByteBuf buf) {
		this.pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
		this.nbt = buf.readNbt();
	}

	@Override
	public void toBytes(FriendlyByteBuf buf) {
		buf.writeInt(pos.getX()).writeInt(pos.getY()).writeInt(pos.getZ());
		buf.writeNbt(this.nbt);
	}

	@Override
	public void process(Supplier<Context> context) {
		Context ctx = context.get();
		if (ctx.getDirection().getReceptionSide() == LogicalSide.SERVER)
			ctx.enqueueWork(() -> {
				ServerLevel world = Objects.requireNonNull(ctx.getSender()).getLevel();
				if (world.isAreaLoaded(pos, 1)) {
					BlockEntity tile = world.getBlockEntity(pos);
					if (tile instanceof WAMUtilsBlockEntity)
						((WAMUtilsBlockEntity) tile).receiveMessageFromClient(nbt);
				}
			});
		else
			ctx.enqueueWork(() -> {
				Level world = WAMUtilsMod.proxy.getClientWorld();
				if (world != null) // This can happen if the task is scheduled right before leaving the world
				{
					BlockEntity tile = world.getBlockEntity(pos);
					if (tile instanceof WAMUtilsBlockEntity)
						((WAMUtilsBlockEntity) tile).receiveMessageFromServer(nbt);
				}
			});
	}
}
