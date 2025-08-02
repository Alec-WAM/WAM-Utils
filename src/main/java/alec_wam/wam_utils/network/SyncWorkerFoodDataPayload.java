package alec_wam.wam_utils.network;

import alec_wam.wam_utils.WAMUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SyncWorkerFoodDataPayload(int entityId, int foodLevel, float saturationLevel) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SyncWorkerFoodDataPayload> TYPE = new CustomPacketPayload.Type<>(WAMUtils.prefix("sync_worker_food_data"));
	
	public static final StreamCodec<ByteBuf, SyncWorkerFoodDataPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        SyncWorkerFoodDataPayload::entityId,
        ByteBufCodecs.VAR_INT,
        SyncWorkerFoodDataPayload::foodLevel,
        ByteBufCodecs.FLOAT,
        SyncWorkerFoodDataPayload::saturationLevel,
        SyncWorkerFoodDataPayload::new
    );
	
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

}
