package alec_wam.wam_utils.datagen;

import java.util.List;
import java.util.Set;

import alec_wam.wam_utils.common.ModInit;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

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


    public void doubleBlockLoot(Block block) {
        this.add(block, builder -> this.createSinglePropConditionTable(block, DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void generate() {
        List<Block> mobSignBlocks = ModInit.MOB_SIGN_TYPE_OBJECTS.values().stream()
				.map(object -> object.block().get())
				.toList();
        Set<Block> specialBlocks = Set.of(
            mobSignBlocks.toArray(Block[]::new)
        );
        
        mobSignBlocks.forEach(this::doubleBlockLoot);

        ModInit.BLOCKS.getEntries()
            .stream()
            // Cast to Block here, otherwise it will be a ? extends Block and Java will complain.
            .map(e -> (Block) e.value())
            .filter(block -> !specialBlocks.contains(block))
            .forEach(this::dropSelf);
    }
}