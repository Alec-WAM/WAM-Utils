package alec_wam.wam_utils.common.blocks.conveyor.splitter;

import java.util.Objects;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemStackKey {
    public final Holder<Item> item;
    public final DataComponentPatch dataComponents;
    private final int hash;


    public ItemStackKey(ItemStack stack, boolean compareNBT) {
        this.item = stack.getItemHolder();
        this.dataComponents = compareNBT ? stack.getComponentsPatch() : DataComponentPatch.EMPTY;
        this.hash = Objects.hash(item, dataComponents);
    }

    public ItemStack getStack() {
        return new ItemStack(item, 1, dataComponents);
    }

    public ItemStack getStack(int amt) {
        return new ItemStack(item, amt, dataComponents);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ItemStackKey) {
            return (((ItemStackKey) obj).item == this.item) && Objects.equals(((ItemStackKey) obj).dataComponents, this.dataComponents);
        }
        return false;
    }
}
