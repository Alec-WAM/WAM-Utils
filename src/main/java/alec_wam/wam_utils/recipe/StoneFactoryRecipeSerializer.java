package alec_wam.wam_utils.recipe;

import com.google.gson.JsonObject;

import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

public class StoneFactoryRecipeSerializer implements RecipeSerializer<StoneFactoryRecipe> {
	private final int defaultCraftTime;

	public StoneFactoryRecipeSerializer(int defaultCraftTime) {
		this.defaultCraftTime = defaultCraftTime;
	}

	@SuppressWarnings({ "deprecation" })
	public StoneFactoryRecipe fromJson(ResourceLocation recipeID, JsonObject json) {
		int water = GsonHelper.getAsInt(json, "water", 0);
		int lava = GsonHelper.getAsInt(json, "lava", 0);
		ItemStack itemstack;
		if (json.get("result").isJsonObject())
			itemstack = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
		else {
			String s1 = GsonHelper.getAsString(json, "result");
			ResourceLocation resourcelocation = new ResourceLocation(s1);
			itemstack = new ItemStack(Registry.ITEM.getOptional(resourcelocation).orElseThrow(() -> {
				return new IllegalStateException("Item: " + s1 + " does not exist");
			}));
		}
		int craftingTime = GsonHelper.getAsInt(json, "craftingTime", defaultCraftTime);
		return new StoneFactoryRecipe(recipeID, water, lava, itemstack, craftingTime);
	}

	public StoneFactoryRecipe fromNetwork(ResourceLocation recipeID, FriendlyByteBuf buffer) {
		int water = buffer.readVarInt();
		int lava = buffer.readVarInt();
		ItemStack itemstack = buffer.readItem();
		int craftingTime = buffer.readVarInt();
		return new StoneFactoryRecipe(recipeID, water, lava, itemstack, craftingTime);
	}

	public void toNetwork(FriendlyByteBuf buffer, StoneFactoryRecipe recipe) {
		buffer.writeVarInt(recipe.waterAmount);
		buffer.writeVarInt(recipe.lavaAmount);
		buffer.writeItem(recipe.result);
		buffer.writeVarInt(recipe.craftingTime);
	}
}
