package alec_wam.wam_utils.common.blocks.conveyor;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntFunction;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemFilter {
    private static final ItemFilter EMPTY = new ItemFilter(FilterType.ITEM, true, Optional.empty(), Optional.empty());

    public static enum FilterType implements StringRepresentable {
        ITEM, TAG;

        public static final StringRepresentable.EnumCodec<FilterType> CODEC = StringRepresentable.fromEnum(FilterType::values);
        private static final IntFunction<FilterType> BY_ID = ByIdMap.continuous(FilterType::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<ByteBuf, FilterType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, FilterType::ordinal);

		@Override
		public String getSerializedName() {
            return name();
		}
    }

    public static final Codec<ItemFilter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FilterType.CODEC.fieldOf("type").forGetter((ItemFilter filter) -> filter.type),
            Codec.BOOL.fieldOf("whiteList").forGetter((ItemFilter filter) -> filter.whiteList),
            ItemStack.CODEC.optionalFieldOf("itemstack").forGetter((ItemFilter filter) -> filter.stack),
            Codec.STRING.optionalFieldOf("itemtag").forGetter((ItemFilter filter) -> filter.itemTag))
            .apply(instance, ItemFilter::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemFilter> STREAM_CODEC = StreamCodec.composite(
            FilterType.STREAM_CODEC, (ItemFilter filter) -> filter.type,
            ByteBufCodecs.BOOL, (ItemFilter filter) -> filter.whiteList,
            ByteBufCodecs.optional(ItemStack.STREAM_CODEC), (ItemFilter filter) -> filter.stack,
            ByteBufCodecs.optional(ByteBufCodecs.stringUtf8(50)), (ItemFilter filter) -> filter.itemTag,
            ItemFilter::new
    );

	private FilterType type;
    private boolean whiteList;
    private Optional<ItemStack> stack;
    private Optional<String> itemTag;

    private ItemFilter(FilterType type, boolean whiteList, Optional<ItemStack> stack, Optional<String> itemTag) {
        this.type = type;
        this.whiteList = whiteList;
        this.stack = stack;
        this.itemTag = itemTag;
    }

    public static ItemFilter empty() { return EMPTY.copy(); }

    public static ItemFilter itemStackFilter(ItemStack stack, boolean whiteList) {
        return new ItemFilter(FilterType.ITEM, whiteList, Optional.of(stack), Optional.empty());
    }

    public static ItemFilter itemTagFilter(String tag, boolean whiteList) {
        return new ItemFilter(FilterType.TAG, whiteList, Optional.empty(), Optional.of(tag));
    }

	public ItemFilter copy() {
		Optional<ItemStack> stackCopy = stack.isPresent() ? Optional.of(stack.get().copy()) : Optional.empty(); // copy the stack if present
        Optional<String> tagCopy = itemTag.isPresent() ? Optional.of(itemTag.get()) : Optional.empty();
        return new ItemFilter(type, whiteList, stackCopy, tagCopy);
	}
    
    public void setType(FilterType type) { 
        this.type = type;
        if(type == FilterType.TAG) {
            this.stack = Optional.empty();
        }
        else if(type == FilterType.ITEM) {
            this.itemTag = Optional.empty();
        }
    }

    public FilterType getType() {
		return type;
	}

	public boolean isWhiteList() {
		return whiteList;
	}

    public void setWhiteList(boolean whiteList) {
        System.out.println(this.whiteList);
        System.out.println(this.getStack());
        this.whiteList = whiteList;
        System.out.println(this.whiteList);
        System.out.println(this.getStack());
    }

	public Optional<ItemStack> getStack() {
		return stack;
	}

    public void setStack(Optional<ItemStack> stack) {
        this.stack = stack;
        this.itemTag = Optional.empty();
    }

	public Optional<String> getItemTag() {
		return itemTag;
    }

    public void setItemTag(Optional<String> itemTag) {
        this.itemTag = itemTag;
        this.stack = Optional.empty();
    }

    public boolean passesFilter(ItemStack filterStack) {
        return whiteList ? rawMatches(filterStack) : !rawMatches(filterStack);
    }

    public boolean rawMatches(ItemStack filterStack) {
        if (type == FilterType.ITEM) {
            // TODO Make this able to be "fuzzy" instead of exact
            if (filterStack.isEmpty() || this.stack.isEmpty()) return false;
            return ItemStack.isSameItemSameComponents(this.stack.get(), filterStack);
        } else if (type == FilterType.TAG) {
            return filterStack.getTags().anyMatch(this::tagMatches);
        }
        return false;
    }

    private boolean tagMatches(TagKey<Item> tagKey) {
        if (this.itemTag.isEmpty()) {
            return false;
        }
        String otherTag = tagKey.location().toString().toLowerCase(Locale.ROOT);
        return this.itemTag.get().equalsIgnoreCase(otherTag);
    }

    @Override
    public int hashCode(){
        return Objects.hash(type, whiteList, stack, itemTag);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof ItemFilter) {
            ItemFilter other = (ItemFilter) obj;
            ItemStack thisStack = this.stack.orElse(ItemStack.EMPTY);
            ItemStack otherStack = other.stack.orElse(ItemStack.EMPTY);
            return type == other.type && whiteList == other.whiteList && ItemStack.isSameItemSameComponents(thisStack, otherStack) && itemTag.equals(other.itemTag);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("type", type)
            .add("whiteList", whiteList)
            .add("stack", stack)
            .add("itemTag", itemTag)
            .toString();
    }
}
