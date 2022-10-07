package alec_wam.wam_utils.init;

import java.util.ArrayList;
import java.util.List;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.advanced_spawner.SpawnerBeheadingRecipe;
import alec_wam.wam_utils.recipe.StoneFactoryRecipe;
import alec_wam.wam_utils.recipe.StoneFactoryRecipeSerializer;
import net.minecraft.core.Registry;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RecipeInit {

	public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, WAMUtilsMod.MODID);
	public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registry.RECIPE_TYPE_REGISTRY, WAMUtilsMod.MODID);
	
	public static final List<SpawnerBeheadingRecipe> BEHEADING_RECIPES = new ArrayList<>();
	public static final RegistryObject<RecipeSerializer<?>> BEHEADING_RECIPE = RECIPES.register(SpawnerBeheadingRecipe.NAME, SpawnerBeheadingRecipe.Serializer::new);
	public static final RegistryObject<RecipeType<SpawnerBeheadingRecipe>> BEHEADING_TYPE = RECIPE_TYPES.register("spawner_beheading", () -> new RecipeType<SpawnerBeheadingRecipe>() {});
	
	public static final RegistryObject<RecipeSerializer<?>> STONE_FACTORY_RECIPE = RECIPES.register(StoneFactoryRecipe.NAME, () -> new StoneFactoryRecipeSerializer(100));
	public static final RegistryObject<RecipeType<StoneFactoryRecipe>> STONE_FACTORY_TYPE = RECIPE_TYPES.register(StoneFactoryRecipe.NAME, () -> new RecipeType<StoneFactoryRecipe>() {});
	
}
