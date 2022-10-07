package alec_wam.wam_utils.recipe;

import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.init.RecipeInit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class StoneFactoryRecipe extends WAMUtilsRecipe {
	public static final String NAME = "stone_factory";
    
	protected final int waterAmount;

	protected final int lavaAmount;
	protected final ItemStack result;
	protected final int craftingTime;
	
	public StoneFactoryRecipe(ResourceLocation id, int water, int lava, @NotNull ItemStack result, int craftingTime) {
		super(RecipeInit.STONE_FACTORY_TYPE.get(), id);
		this.waterAmount = water;
		this.lavaAmount = lava;
		this.result = result;
		this.craftingTime = craftingTime;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return RecipeInit.STONE_FACTORY_RECIPE.get();
	}

	@Override
	public ItemStack getResultItem() {
		return result;
	}
	
	public int getWaterAmount() {
		return waterAmount;
	}

	public int getLavaAmount() {
		return lavaAmount;
	}

	public int getCraftingTime() {
		return craftingTime;
	}

}
