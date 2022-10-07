package alec_wam.wam_utils.blocks.advanced_portal;

import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

public class CustomTeleporter implements ITeleporter {

	public final GlobalPos teleportPos;
	public final float rotation;
	public CustomTeleporter(GlobalPos pos, float rotation) {
		this.teleportPos = pos;
		this.rotation = rotation;
	}
	
	@Override
	public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity)
    {
        return repositionEntity.apply(false);
    }
	
	@Override
	public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo)
    {
		BlockPos blockPos = teleportPos.pos();
		Vec3 pos = new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
        return new PortalInfo(pos, entity.getDeltaMovement(), this.rotation, entity.getXRot());
    }
	
	@Override
	public boolean playTeleportSound(ServerPlayer player, ServerLevel sourceWorld, ServerLevel destWorld)
    {
        return false;
    }
	
}
