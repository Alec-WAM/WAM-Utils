package alec_wam.wam_utils.server.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import alec_wam.wam_utils.blocks.advanced_portal.AdvancedPortalClientDataManager;
import alec_wam.wam_utils.blocks.advanced_portal.AdvancedPortalClientDataManager.ClientAdvancedPortalData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent.Context;

public class MessageUpdateClientAdvancedPortals implements IMessage
{
	private List<ClientAdvancedPortalData> portals;

	public MessageUpdateClientAdvancedPortals(List<ClientAdvancedPortalData> portals)
	{
		this.portals = portals;
	}

	public MessageUpdateClientAdvancedPortals(FriendlyByteBuf buf)
	{
		portals = new ArrayList<ClientAdvancedPortalData>();
		int size = buf.readInt();
		for(int i = 0; i < size; i++) {
			portals.add(ClientAdvancedPortalData.readFromBuf(buf));
		}
	}

	@Override
	public void toBytes(FriendlyByteBuf buf)
	{
		buf.writeInt(portals.size());
		for(ClientAdvancedPortalData portal : portals) {
			portal.writeToBuf(buf);
		}
	}

	@Override
	public void process(Supplier<Context> context)
	{
		Context ctx = context.get();
		if (ctx.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
			ctx.enqueueWork(() -> {
				AdvancedPortalClientDataManager.setPortals(portals);
			});
		}
	}
}
