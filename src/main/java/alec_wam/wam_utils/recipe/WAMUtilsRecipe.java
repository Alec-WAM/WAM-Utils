package alec_wam.wam_utils.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public abstract class WAMUtilsRecipe implements Recipe<Container>
{
	protected final RecipeType<?> type;
	protected final ResourceLocation id;

	protected WAMUtilsRecipe(RecipeType<?> type, ResourceLocation id)
	{
		this.type = type;
		this.id = id;
	}

	@Override
	public boolean isSpecial()
	{
		return true;
	}

	@Override
	public boolean matches(Container inv, Level worldIn)
	{
		return false;
	}

	@Override
	public ItemStack assemble(Container inv)
	{
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack getResultItem() {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height)
	{
		return false;
	}

	@Override
	public ResourceLocation getId()
	{
		return this.id;
	}

	@Override
	public RecipeType<?> getType()
	{
		return this.type;
	}

}
