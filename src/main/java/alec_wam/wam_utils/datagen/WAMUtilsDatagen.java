package alec_wam.wam_utils.datagen;

import java.util.List;
import java.util.Set;

import alec_wam.wam_utils.WAMUtils;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableProvider.SubProviderEntry;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = WAMUtils.MODID)
public class WAMUtilsDatagen {

	@SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        event.createProvider(WAMUtilsBlockTags::new);
        event.createProvider(WAMUtilsModelProvider::new);
        event.createProvider(WAMUtilsRecipeProvider.Runner::new);

        event.createProvider((output, lookupProv) -> {
            return new LootTableProvider(output, Set.of(), List.of(new SubProviderEntry(
                    WAMUtilsBlockLootTableProvider::new,
                    LootContextParamSets.BLOCK
                )), lookupProv
            );
        });
    }
	
}
