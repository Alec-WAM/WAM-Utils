package alec_wam.wam_utils.blocks.machine.auto_compactor;

import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

public class InternalCraftingContainer extends CraftingContainer {
	private final NonNullList<ItemStack> items;
	   
	public InternalCraftingContainer(int size) {
		super(null, size, size);
		this.items = NonNullList.withSize(size * size, ItemStack.EMPTY);	      
	}

	@Override
	public int getContainerSize() {
		return this.items.size();
	}

	@Override
	public boolean isEmpty() {
		for(ItemStack itemstack : this.items) {
			if (!itemstack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public ItemStack getItem(int p_39332_) {
		return p_39332_ >= this.getContainerSize() ? ItemStack.EMPTY : this.items.get(p_39332_);
	}

	@Override
	public ItemStack removeItemNoUpdate(int p_39344_) {
		return ContainerHelper.takeItem(this.items, p_39344_);
	}

	@Override
	public ItemStack removeItem(int p_39334_, int p_39335_) {
		ItemStack itemstack = ContainerHelper.removeItem(this.items, p_39334_, p_39335_);
		return itemstack;
	}

	@Override
	public void setItem(int p_39337_, ItemStack p_39338_) {
		this.items.set(p_39337_, p_39338_);
	}

	@Override
	public void clearContent() {
		this.items.clear();
	}

	@Override
	public void fillStackedContents(StackedContents p_39342_) {
		for(ItemStack itemstack : this.items) {
			p_39342_.accountSimpleStack(itemstack);
		}

	}

}
