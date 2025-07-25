package alec_wam.wam_utils.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import alec_wam.wam_utils.common.ModInit;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

public class WAMUtilsBlockLootTableProvider extends BlockLootSubProvider  {

    public WAMUtilsBlockLootTableProvider(HolderLookup.Provider lookupProvider) {
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, lookupProvider);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        // The contents of our DeferredRegister.
        return ModInit.BLOCKS.getEntries()
                .stream()
                // Cast to Block here, otherwise it will be a ? extends Block and Java will complain.
                .map(e -> (Block) e.value())
                .toList();
    }

    @Override
    protected void generate() {
        List<DeferredBlock<Block>> basicBlocks = new ArrayList<>();
        basicBlocks.add(ModInit.CONVEYOR_BELT_BLOCK);
        basicBlocks.add(ModInit.CONVEYOR_SPLITTER_BLOCK);
        basicBlocks.add(ModInit.ITEM_GRATE_BLOCK);
        basicBlocks.addAll(ModInit.SHIELDRACK_BLOCKS.values());
        basicBlocks.stream().map(DeferredBlock::get).forEach(this::dropSelf);
    }
}