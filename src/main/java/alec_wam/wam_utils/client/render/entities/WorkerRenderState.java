package alec_wam.wam_utils.client.render.entities;

import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.world.phys.Vec3;

public class WorkerRenderState extends PlayerRenderState {
    public boolean isFishing;
    public Vec3 bobberPosition;
    public Vec3 fishingRodLocation;
    public Vec3 fishingLineOriginOffset;
}
