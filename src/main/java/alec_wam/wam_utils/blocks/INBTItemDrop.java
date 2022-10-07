package alec_wam.wam_utils.blocks;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface INBTItemDrop {

	public void readFromItem(ItemStack stack);
	
	public ItemStack getNBTDrop(Item item);
	
}
