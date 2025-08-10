package alec_wam.wam_utils.common.items;

import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentClearItem extends Item {

    public EnchantmentClearItem(Properties properties) {
        super(properties);
    }

    // Allow the item to be enchanted with anything similar to a book
    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        // TODO Make this changeable
        return enchantment.is(EnchantmentTags.ARMOR_EXCLUSIVE);
    }
    
}
