package alec_wam.wam_utils.common.blocks.conveyor;

import java.util.ArrayList;
import java.util.Collection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public class ItemFilterList extends ArrayList<ItemFilter> {

    public static final Codec<ItemFilterList> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.list(ItemFilter.CODEC).fieldOf("filters").forGetter(itemFilterList -> itemFilterList))
        .apply(instance, ItemFilterList::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemFilterList> STREAM_CODEC = ItemFilter.STREAM_CODEC
        .apply(ByteBufCodecs.collection(ItemFilterList::new));

    public ItemFilterList() {
    }

    private ItemFilterList(int size) {
        super(size);
    }

    private ItemFilterList(Collection<ItemFilter> offers) {
        super(offers);
    }

    public boolean passesAllFilters(ItemStack stack) {
        boolean hasWhitelist = false;
        boolean matchesWhitelist = false;

        for (int i = 0; i < this.size(); i++) {
            ItemFilter filter = this.get(i);
            if (filter.isWhiteList()) {
                hasWhitelist = true;
                if (filter.rawMatches(stack)) {
                    matchesWhitelist = true;
                }
            } else { // blacklist
                if (filter.rawMatches(stack)) {
                    return false; // blacklist match -> block immediately
                }
            }
        }

        // If any whitelist filters exist -> only allow if we matched at least one
        if (hasWhitelist) {
            return matchesWhitelist;
        }

        // No whitelist filters -> allow since no blacklist match happened
        return true;
    }

    public ItemFilter addTagFilter(String string, boolean whiteList) {
        ItemFilter tagFilter = ItemFilter.itemTagFilter(string, whiteList);
        this.add(tagFilter);
        return tagFilter;
    }

    public ItemFilter addItemStackFilter(ItemStack stack, boolean whiteList) {
        ItemFilter tagFilter = ItemFilter.itemStackFilter(stack, whiteList);
        this.add(tagFilter);
        return tagFilter;
    }

}
