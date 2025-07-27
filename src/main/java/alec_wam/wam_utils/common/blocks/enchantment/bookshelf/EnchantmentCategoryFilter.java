package alec_wam.wam_utils.common.blocks.enchantment.bookshelf;

import java.util.function.IntFunction;
import java.util.function.Predicate;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.Tags;

public enum EnchantmentCategoryFilter implements StringRepresentable {
    ALL((enchantment) -> {
        return true;
    }),
    ARMOR((enchantment) -> {
        if(
            enchantment.value().matchingSlot(EquipmentSlot.HEAD)
            || enchantment.value().matchingSlot(EquipmentSlot.CHEST)
            || enchantment.value().matchingSlot(EquipmentSlot.LEGS)
            || enchantment.value().matchingSlot(EquipmentSlot.FEET)
        ){
            return true;
        }
        return enchantment.is(EnchantmentTags.ARMOR_EXCLUSIVE) || enchantment.is(EnchantmentTags.BOOTS_EXCLUSIVE);
    }),
    ARMOR_FEET((enchantment) -> {
        return enchantment.value().matchingSlot(EquipmentSlot.FEET) || enchantment.is(EnchantmentTags.BOOTS_EXCLUSIVE);
    }),
    ARMOR_LEGS((enchantment) -> {
        return enchantment.value().matchingSlot(EquipmentSlot.LEGS);
    }),
    ARMOR_CHEST((enchantment) -> {
        return enchantment.value().matchingSlot(EquipmentSlot.CHEST);
    }),
    ARMOR_HEAD((enchantment) -> {
        return enchantment.value().matchingSlot(EquipmentSlot.HEAD);
    }),
    WEAPON((enchantment) -> {
        return enchantment.is(EnchantmentTags.DAMAGE_EXCLUSIVE) || enchantment.is(Tags.Enchantments.WEAPON_DAMAGE_ENHANCEMENTS);
    }),
    BOW((enchantment) -> {
        return enchantment.is(EnchantmentTags.BOW_EXCLUSIVE);
    }),
    CROSSBOW((enchantment) -> {
        return enchantment.is(EnchantmentTags.CROSSBOW_EXCLUSIVE);
    }),
    MINING((enchantment) -> {
        return enchantment.is(EnchantmentTags.MINING_EXCLUSIVE) || enchantment.is(Enchantments.EFFICIENCY);
    }),
    FISHING_ROD((enchantment) -> {
        return holderSetHasTag(enchantment, ItemTags.FISHING_ENCHANTABLE);
    }),
    TRIDENT((enchantment) -> {
        return holderSetHasTag(enchantment, ItemTags.TRIDENT_ENCHANTABLE);
    }),
    DURABILITY((enchantment) -> {
        return holderSetHasTag(enchantment, ItemTags.DURABILITY_ENCHANTABLE);
    }),
    CURSE((enchantment) -> {
        return enchantment.is(EnchantmentTags.CURSE);
    });

    public static final StringRepresentable.EnumCodec<EnchantmentCategoryFilter> CODEC = StringRepresentable.fromEnum(EnchantmentCategoryFilter::values);
    private static final IntFunction<EnchantmentCategoryFilter> BY_ID = ByIdMap.continuous(EnchantmentCategoryFilter::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, EnchantmentCategoryFilter> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, EnchantmentCategoryFilter::ordinal);

    // private final GuiIcons icon;
    private final Predicate<Holder<Enchantment>> filter;

    EnchantmentCategoryFilter(Predicate<Holder<Enchantment>> filter) {
        this.filter = filter;
    }

    public Predicate<Holder<Enchantment>> getFilter() {
        return filter;
    }

    public static boolean holderSetHasTag(Holder<Enchantment> enchantment, TagKey<Item> tag){
        if(
            enchantment == null
            || enchantment.value() == null
            || enchantment.value().definition() == null
            || enchantment.value().definition().supportedItems() == null
        ) {
            return false;
        }
        return holderSetHasTag(enchantment, tag);
    }

    public EnchantmentCategoryFilter getPrev() {
        int prevIndex = (this.ordinal() - 1);
        if (prevIndex < 0) {
            prevIndex = EnchantmentCategoryFilter.values().length - 1;
        }
        return EnchantmentCategoryFilter.values()[prevIndex];
    }

    public EnchantmentCategoryFilter getNext() {
        return EnchantmentCategoryFilter.values()[(this.ordinal() + 1)
                % (EnchantmentCategoryFilter.values().length)];
    }

    public static EnchantmentCategoryFilter getMode(int index) {
        return EnchantmentCategoryFilter.values()[index
                % (EnchantmentCategoryFilter.values().length)];
    }

    public Component getTooltip() {
        return Component
                .translatable("wamutils.gui.tooltip.enchantment_category." + name().toLowerCase() + ".tooltip");
    }

    @Override
    public String getSerializedName() {
        return name();
    }


}