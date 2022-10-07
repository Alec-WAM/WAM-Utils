package alec_wam.wam_utils.server.network;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.datafixers.util.Pair;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.server.container.GenericDataSerializers;
import alec_wam.wam_utils.server.container.WAMContainerMenu;
import alec_wam.wam_utils.server.container.GenericDataSerializers.DataPair;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent.Context;

public class MessageContainerData implements IMessage
{
	private final List<Pair<Integer, DataPair<?>>> synced;

	public MessageContainerData(List<Pair<Integer, DataPair<?>>> synced)
	{
		this.synced = synced;
	}

	public MessageContainerData(FriendlyByteBuf buf)
	{
		this(PacketUtils.readList(buf, pb -> Pair.of(pb.readVarInt(), GenericDataSerializers.read(pb))));
	}

	@Override
	public void toBytes(FriendlyByteBuf buf)
	{
		PacketUtils.writeList(buf, synced, (pair, b) -> {
			b.writeVarInt(pair.getFirst());
			pair.getSecond().write(b);
		});
	}

	@Override
	public void process(Supplier<Context> context)
	{
		AbstractContainerMenu currentContainer = WAMUtilsMod.proxy.getClientPlayer().containerMenu;
		if(!(currentContainer instanceof WAMContainerMenu container))
			return;
		container.receiveSync(synced);
	}
}
