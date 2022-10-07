package alec_wam.wam_utils.client;

import alec_wam.wam_utils.server.CommonProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ClientProxy extends CommonProxy {

	@SuppressWarnings("resource")
	@Override
	public Level getClientWorld()
	{
		return Minecraft.getInstance().level;
	}

	@SuppressWarnings("resource")
	@Override
	public Player getClientPlayer()
	{
		return Minecraft.getInstance().player;
	}
	
}
