package alec_wam.wam_utils.network;

import alec_wam.wam_utils.WAMUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record FilterSlotPayload(
        int slotNumber,
        ItemStack stack,
        int count
) implements CustomPacketPayload {
    public static final Type<FilterSlotPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(WAMUtils.MODID, "filter_slot"));

    @Override
    public Type<FilterSlotPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, FilterSlotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, FilterSlotPayload::slotNumber,
            ItemStack.OPTIONAL_STREAM_CODEC, FilterSlotPayload::stack,
            ByteBufCodecs.INT, FilterSlotPayload::count,
            FilterSlotPayload::new
    );
}