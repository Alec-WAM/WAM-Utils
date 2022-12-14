package alec_wam.wam_utils.blocks.advanced_spawner;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import alec_wam.wam_utils.init.RecipeInit;
import net.minecraft.core.Registry;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class SpawnerBeheadingRecipe implements Recipe<Container>{
    private final ResourceLocation id;
    public static final String NAME = "spawner_beheading";
    private final EntityType<?> entityType;
    private final ItemStack result;

    public SpawnerBeheadingRecipe(ResourceLocation id, EntityType<?> type, ItemStack output) {
        this.id = id;
        this.entityType = type;
        this.result = output;
    }

    @Override
    public boolean matches(@Nonnull Container container, @Nonnull Level level) {
        return false;
    }

    public boolean matches(EntityType<?> typeIn) {
        return typeIn == entityType;
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull Container container) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int p_43999_, int p_44000_) {
        return false;
    }

    @Nonnull
    @Override
    public ItemStack getResultItem() {
        return result.copy();
    }

    @Nonnull
    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeInit.BEHEADING_RECIPE.get();
    }

    @Nonnull
    @Override
    public RecipeType<?> getType() {
        return RecipeInit.BEHEADING_TYPE.get();
    }

    public static class DataRecipe implements FinishedRecipe {
        private final ResourceLocation id;
        private final ResourceLocation entityRes;
        private final ResourceLocation resultRes;

        public DataRecipe(ResourceLocation id, EntityType<?> entityType, ItemStack result) {
            this.id = id;
            this.entityRes = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
            this.resultRes = ForgeRegistries.ITEMS.getKey(result.getItem());
        }

        public DataRecipe(ResourceLocation id, ResourceLocation entityRes, ItemStack result) {
            this.id = id;
            this.entityRes = entityRes;
            this.resultRes = ForgeRegistries.ITEMS.getKey(result.getItem());
        }

        public DataRecipe(ResourceLocation id, ResourceLocation entityRes, ResourceLocation result) {
            this.id = id;
            this.entityRes = entityRes;
            this.resultRes = result;
        }

        public DataRecipe(ResourceLocation id, EntityType<?> entityType, ResourceLocation result) {
            this.id = id;
            this.entityRes = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
            this.resultRes = result;
        }

        @Override
        public void serializeRecipeData(@Nonnull JsonObject json) {
            json.addProperty("entity", entityRes.toString());
            JsonObject resultJson = new JsonObject();
            resultJson.addProperty("item", this.resultRes.toString());
            json.add("result", resultJson);
        }

        @Nonnull
        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Nonnull
        @Override
        public RecipeSerializer<?> getType() {
            return RecipeInit.BEHEADING_RECIPE.get();
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return null;
        }

        @Nullable
        @Override
        public ResourceLocation getAdvancementId() {
            return null;
        }
    }


    public static class Serializer implements RecipeSerializer<SpawnerBeheadingRecipe> {

        @Nonnull
        @Override
        public SpawnerBeheadingRecipe fromJson(@Nonnull ResourceLocation recipeID, JsonObject json) {
            ResourceLocation entityRes = new ResourceLocation(json.get("entity").getAsString());
            Optional<EntityType<?>> type = Registry.ENTITY_TYPE.getOptional(entityRes);
            if (type.isEmpty())
                throw new JsonParseException("unknown entity type");
            ItemStack result = new ItemStack(GsonHelper.getAsItem(json.get("result").getAsJsonObject(), "item"));

            return new SpawnerBeheadingRecipe(recipeID, type.get(), result);
        }

        @Nullable
        @Override
        public SpawnerBeheadingRecipe fromNetwork(ResourceLocation recipeID, FriendlyByteBuf buf) {
            ResourceLocation entityRes = new ResourceLocation(buf.readUtf());
            Optional<EntityType<?>> type = Registry.ENTITY_TYPE.getOptional(entityRes);
            if (type.isEmpty())
                throw new JsonParseException("unknown entity type");
            ItemStack result = buf.readItem();

            return new SpawnerBeheadingRecipe(recipeID, type.get(), result);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, SpawnerBeheadingRecipe recipe) {
            buf.writeUtf(Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(recipe.entityType)).toString());
            buf.writeItem(recipe.result);
        }
    }
}
