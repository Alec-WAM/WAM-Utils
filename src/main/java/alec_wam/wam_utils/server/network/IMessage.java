package alec_wam.wam_utils.server.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.function.Supplier;

public interface IMessage
{
	void toBytes(FriendlyByteBuf buf);

	void process(Supplier<Context> context);
}
