package alec_wam.wam_utils.server.network;

import java.util.Objects;
import java.util.function.Supplier;

import alec_wam.wam_utils.WAMUtilsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent.Context;

public class MessageDestroyBlockEffect implements IMessage {
	private final BlockPos pos;
	private final BlockState state;

	public MessageDestroyBlockEffect(BlockPos pos, BlockState state) {
		this.pos = pos;
		this.state = state;
	}

	public MessageDestroyBlockEffect(FriendlyByteBuf buf) {
		this.pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
		this.state = Block.stateById(buf.readInt());
	}

	@Override
	public void toBytes(FriendlyByteBuf buf) {
		buf.writeInt(pos.getX()).writeInt(pos.getY()).writeInt(pos.getZ());
		buf.writeInt(Block.getId(state));
	}

	@Override
	public void process(Supplier<Context> context) {
		Context ctx = context.get();
		if (ctx.getDirection().getReceptionSide() == LogicalSide.SERVER)
			ctx.enqueueWork(() -> {
				ServerLevel world = Objects.requireNonNull(ctx.getSender()).getLevel();
				if (world.isAreaLoaded(pos, 1)) {
					world.addDestroyBlockEffect(pos, state);
				}
			});
		else
			ctx.enqueueWork(() -> {
				Level world = WAMUtilsMod.proxy.getClientWorld();
				if (world != null) // This can happen if the task is scheduled right before leaving the world
				{
					world.addDestroyBlockEffect(pos, state);
				}
			});
	}
}
