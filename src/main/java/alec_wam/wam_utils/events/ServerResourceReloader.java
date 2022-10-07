package alec_wam.wam_utils.events;

import javax.annotation.Nonnull;

import alec_wam.wam_utils.init.RecipeInit;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.crafting.RecipeManager;

public class ServerResourceReloader implements ResourceManagerReloadListener {
    private final ReloadableServerResources dataPackRegistries;
    public ServerResourceReloader(ReloadableServerResources dataPackRegistries) {
        this.dataPackRegistries = dataPackRegistries;
    }
    @Override
    public void onResourceManagerReload(@Nonnull ResourceManager resourceManager) {
        RecipeManager recipeManager = this.dataPackRegistries.getRecipeManager();
        RecipeInit.BEHEADING_RECIPES.clear();
        RecipeInit.BEHEADING_RECIPES.addAll(recipeManager.getAllRecipesFor(RecipeInit.BEHEADING_TYPE.get()));
    }
}
