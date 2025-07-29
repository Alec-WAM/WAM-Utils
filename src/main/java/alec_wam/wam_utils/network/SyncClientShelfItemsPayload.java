package alec_wam.wam_utils.network;

import java.util.List;

import alec_wam.wam_utils.WAMUtils;
import alec_wam.wam_utils.common.blocks.enchantment.indexer.EnchantmentIndexerBE.ClientShelfItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SyncClientShelfItemsPayload(List<ClientShelfItem> shelfItems) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SyncClientShelfItemsPayload> TYPE = new CustomPacketPayload.Type<>(WAMUtils.prefix("sync_client_shelf_items"));
	
	public static final StreamCodec<RegistryFriendlyByteBuf, SyncClientShelfItemsPayload> STREAM_CODEC = StreamCodec.composite(
        ClientShelfItem.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncClientShelfItemsPayload::shelfItems,
        SyncClientShelfItemsPayload::new
    );
	
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

}
