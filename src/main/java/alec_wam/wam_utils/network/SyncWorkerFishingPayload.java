package alec_wam.wam_utils.network;

import java.util.Optional;

import org.joml.Vector3f;

import alec_wam.wam_utils.WAMUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SyncWorkerFishingPayload(int entityId, boolean isFishing, Optional<Vector3f> fishingTargetLocation) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SyncWorkerFishingPayload> TYPE = new CustomPacketPayload.Type<>(WAMUtils.prefix("sync_worker_fishing"));
	
	public static final StreamCodec<ByteBuf, SyncWorkerFishingPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        SyncWorkerFishingPayload::entityId,
        ByteBufCodecs.BOOL,
        SyncWorkerFishingPayload::isFishing,
        ByteBufCodecs.optional(ByteBufCodecs.VECTOR3F),
        SyncWorkerFishingPayload::fishingTargetLocation,
        SyncWorkerFishingPayload::new
    );
	
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

}
