package alec_wam.wam_utils.network;

import java.util.Optional;

import alec_wam.wam_utils.WAMUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SyncWorkerJobPayload(int entityId, Optional<CompoundTag> jobData) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SyncWorkerJobPayload> TYPE = new CustomPacketPayload.Type<>(WAMUtils.prefix("sync_worker_job"));
	
	public static final StreamCodec<ByteBuf, SyncWorkerJobPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        SyncWorkerJobPayload::entityId,
        ByteBufCodecs.OPTIONAL_COMPOUND_TAG,
        SyncWorkerJobPayload::jobData,
        SyncWorkerJobPayload::new
    );
	
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

}
