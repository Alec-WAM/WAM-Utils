package alec_wam.wam_utils.datagen;

import java.util.concurrent.CompletableFuture;

import alec_wam.wam_utils.common.ModInit;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.BlockFamily.Variant;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.properties.WoodType;

public class WAMUtilsRecipeProvider extends RecipeProvider {

    public WAMUtilsRecipeProvider(HolderLookup.Provider provider, RecipeOutput output) {
        super(provider, output);
    }

    protected RecipeBuilder shieldRack(ItemLike shieldRack, Ingredient material) {
        int i = 2;
        Item item = Items.TRIPWIRE_HOOK;
        return this.shaped(RecipeCategory.DECORATIONS, shieldRack, i).define('W', material).define('#', item).pattern("W#W").pattern(" W ");
    }

    protected RecipeBuilder enchantmentBookshelf(ItemLike enchantmentBookshelf, Ingredient material) {
        int i = 1;
        Item item = Items.BOOKSHELF;
        return this.shaped(RecipeCategory.DECORATIONS, enchantmentBookshelf, i).define('W', material).define('#', item).pattern("WWW").pattern(" # ").pattern("WWW");
    }

    @Override
    protected void buildRecipes() {
        WoodType.values().forEach((woodType) -> {
            BlockFamily blockFamily = ModInit.WOOD_BLOCK_FAMILIES.get(woodType);
            Item shieldRack = ModInit.SHIELDRACK_BLOCK_ITEMS.get(woodType).asItem();
            if (shieldRack != null) {
                ItemLike itemlike = blockFamily.get(Variant.SLAB);
                RecipeBuilder recipeBuilder = this.shieldRack(shieldRack, Ingredient.of(itemlike));
                blockFamily.getRecipeGroupPrefix()
                    .ifPresent(
                        prefix -> recipeBuilder.group(prefix + "_" + Variant.SLAB.getRecipeGroup())
                    );
                recipeBuilder.unlockedBy(blockFamily.getRecipeUnlockedBy().orElseGet(() -> getHasName(itemlike)), this.has(itemlike));
                recipeBuilder.save(this.output);
            }

            Item enchantedBookshelf = ModInit.ENCHANTMENT_BOOK_SHELF_BLOCKS.get(woodType).asItem();
            if (enchantedBookshelf != null) {
                ItemLike itemlike = blockFamily.get(Variant.SLAB);
                RecipeBuilder recipeBuilder = this.enchantmentBookshelf(enchantedBookshelf, Ingredient.of(itemlike));
                blockFamily.getRecipeGroupPrefix()
                    .ifPresent(
                        prefix -> recipeBuilder.group(prefix + "_" + Variant.SLAB.getRecipeGroup())
                    );
                recipeBuilder.unlockedBy(blockFamily.getRecipeUnlockedBy().orElseGet(() -> getHasName(itemlike)), this.has(itemlike));
                recipeBuilder.save(this.output);
            }
        });
    }

    // The runner to add to the data generator
    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(output, lookupProvider);
        }

        @Override
        public String getName() {
            return "WAM Utils Recipes";
        }

        @Override
        protected RecipeProvider createRecipeProvider(Provider provider, RecipeOutput output) {
            return new WAMUtilsRecipeProvider(provider, output);
        }
    }
}
