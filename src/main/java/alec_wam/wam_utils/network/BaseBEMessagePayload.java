package alec_wam.wam_utils.network;

import alec_wam.wam_utils.WAMUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record BaseBEMessagePayload(BlockPos pos, String messageType, CompoundTag messageData) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<BaseBEMessagePayload> TYPE = new CustomPacketPayload.Type<>(WAMUtils.prefix("basebe_message"));
	
	public static final StreamCodec<ByteBuf, BaseBEMessagePayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        BaseBEMessagePayload::pos,
        ByteBufCodecs.STRING_UTF8,
        BaseBEMessagePayload::messageType,
        ByteBufCodecs.COMPOUND_TAG,
        BaseBEMessagePayload::messageData,
        BaseBEMessagePayload::new
    );
	
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

}

