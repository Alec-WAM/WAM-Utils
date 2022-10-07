package alec_wam.wam_utils.server.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.function.Supplier;

import alec_wam.wam_utils.server.container.WAMContainerMenu;

public class MessageContainerUpdate implements IMessage
{
	private int windowId;
	private CompoundTag nbt;

	public MessageContainerUpdate(int windowId, CompoundTag nbt)
	{
		this.windowId = windowId;
		this.nbt = nbt;
	}

	public MessageContainerUpdate(FriendlyByteBuf buf)
	{
		this.windowId = buf.readByte();
		this.nbt = buf.readNbt();
	}

	@Override
	public void toBytes(FriendlyByteBuf buf)
	{
		buf.writeByte(this.windowId);
		buf.writeNbt(this.nbt);
	}

	@Override
	public void process(Supplier<Context> context)
	{
		Context ctx = context.get();
		ServerPlayer player = ctx.getSender();
		assert player!=null;
		ctx.enqueueWork(() -> {
			player.resetLastActionTime();
			if(player.containerMenu.containerId==windowId && player.containerMenu instanceof WAMContainerMenu menu)
				menu.receiveMessageFromScreen(nbt);
		});
	}
}
