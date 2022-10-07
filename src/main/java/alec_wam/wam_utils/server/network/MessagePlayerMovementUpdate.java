package alec_wam.wam_utils.server.network;

import java.util.function.Supplier;

import alec_wam.wam_utils.WAMUtilsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent.Context;

public class MessagePlayerMovementUpdate implements IMessage
{
	private Vec3 motion;

	public MessagePlayerMovementUpdate(Vec3 motion)
	{
		this.motion = motion;
	}

	public MessagePlayerMovementUpdate(FriendlyByteBuf buf)
	{
		double x = buf.readDouble();
		double y = buf.readDouble();
		double z = buf.readDouble();
		this.motion = new Vec3(x, y, z);
	}

	@Override
	public void toBytes(FriendlyByteBuf buf)
	{
		buf.writeDouble(motion.x);
		buf.writeDouble(motion.y);
		buf.writeDouble(motion.z);
	}

	@Override
	public void process(Supplier<Context> context)
	{
		Context ctx = context.get();
		if (ctx.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
			ctx.enqueueWork(() -> {
				Player player = WAMUtilsMod.proxy.getClientPlayer();
				if (player != null)
				{
					player.setDeltaMovement(motion);
				}
			});
		}
	}
}
