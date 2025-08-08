package alec_wam.wam_utils.datagen;

import java.util.concurrent.CompletableFuture;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.blocks.mob_sign.MobSignBlock.MobSignType;
import alec_wam.wam_utils.common.blocks.mob_sign.MobSignBlock.MobSignTypeObjects;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.BlockFamily.Variant;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

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

    protected RecipeBuilder mobSign(ItemLike mobSign, Ingredient material) {
        int i = 1;
        return this.shaped(RecipeCategory.MISC, mobSign, i).define('F', ItemTags.FENCES).define('S', ItemTags.SIGNS).define('#', material).pattern("#").pattern("S").pattern("F");
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

        ModInit.MOB_SIGN_TYPE_OBJECTS.entrySet().forEach((entry) -> {
            MobSignType mobSignType = entry.getKey();
            MobSignTypeObjects object = entry.getValue();
            Item mobSign = object.item().asItem();
            if (mobSign != null) {
                Ingredient ingredient = null;
                if(mobSignType == MobSignType.PILLAGER){
                    ingredient = DataComponentIngredient.of(true, Raid.getOminousBannerInstance(this.registries.lookupOrThrow(Registries.BANNER_PATTERN)));
                }
                else {
                    ingredient = mobSignType.getRecipeIngredient();
                }
                if(ingredient !=null){
                    RecipeBuilder recipeBuilder = this.mobSign(mobSign, ingredient);
                    recipeBuilder.unlockedBy("has_fences", this.has(ItemTags.FENCES));
                    recipeBuilder.group("mob_signs");
                    recipeBuilder.save(this.output);
                }
            }
        });

        RecipeBuilder recipeBuilderAutoDrader = this.shaped(RecipeCategory.MISC, ModInit.VILLAGER_AUTO_TRADER_BLOCK_ITEM.get(), 1).define('S', ItemTags.WOODEN_SLABS).define('#', Items.EMERALD).pattern("SSS").pattern("###").pattern("SSS");
        recipeBuilderAutoDrader.unlockedBy("has_wooden_slabs", this.has(ItemTags.WOODEN_SLABS));
        recipeBuilderAutoDrader.save(this.output);
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
