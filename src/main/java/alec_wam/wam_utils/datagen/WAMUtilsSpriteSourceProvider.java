package alec_wam.wam_utils.datagen;

import java.util.concurrent.CompletableFuture;

import alec_wam.wam_utils.WAMUtils;
import alec_wam.wam_utils.client.render.blockentities.EnchantmentBookshelfBERenderer;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.data.SpriteSourceProvider;

public class WAMUtilsSpriteSourceProvider extends SpriteSourceProvider {

    public WAMUtilsSpriteSourceProvider(PackOutput output, CompletableFuture<Provider> lookupProvider) {
        super(output, lookupProvider, WAMUtils.MODID);
    }

    private static SpriteSource forMaterial(Material material) {
        return new SingleFile(material.texture());
    }

    @Override
    protected void gather() {
        atlas(BLOCKS_ATLAS)
            .addSource(forMaterial(EnchantmentBookshelfBERenderer.BOOK_LOCATION))
            .addSource(forMaterial(EnchantmentBookshelfBERenderer.ENCHANTED_BOOK_LOCATION));
    }
    
}
