package alec_wam.wam_utils.datagen;

import java.util.concurrent.CompletableFuture;

import alec_wam.wam_utils.WAMUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;

public class WAMUtilsPotionTags extends TagsProvider<Potion> {

    public WAMUtilsPotionTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.POTION, lookupProvider,WAMUtils.MODID);
    }

    public static final TagKey<Potion> WORKER_POTION_JOB_BLACKLIST = TagKey.create(
		Registries.POTION,
		ResourceLocation.fromNamespaceAndPath(WAMUtils.MODID, "worker_potion_job_blacklist")
	);


    @Override
    protected void addTags(Provider provider) {
        this.getOrCreateRawBuilder(WORKER_POTION_JOB_BLACKLIST)
            .addElement(Potions.THICK.getKey().location())
            .addElement(Potions.MUNDANE.getKey().location());
    }
    
}
