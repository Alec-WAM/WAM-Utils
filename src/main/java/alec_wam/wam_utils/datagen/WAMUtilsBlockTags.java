package alec_wam.wam_utils.datagen;

import java.util.concurrent.CompletableFuture;

import alec_wam.wam_utils.WAMUtils;
import alec_wam.wam_utils.common.ModInit;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

public class WAMUtilsBlockTags extends BlockTagsProvider {

	public WAMUtilsBlockTags(PackOutput output, CompletableFuture<Provider> lookupProvider) {
		super(output, lookupProvider, WAMUtils.MODID);
	}

	public static final TagKey<Block> TREE_ATTACHMENTS = BlockTags.create(ResourceLocation.fromNamespaceAndPath(WAMUtils.MODID, "tree_attachments"));
	
	@Override
	protected void addTags(Provider provider) {
		tag(TREE_ATTACHMENTS)
        .add(Blocks.BEE_NEST, Blocks.COCOA, Blocks.MOSS_CARPET, Blocks.SHROOMLIGHT, Blocks.VINE);
		
		tag(BlockTags.MINEABLE_WITH_AXE)
			.add(ModInit.SHIELDRACK_BLOCKS.values().stream()
				.map(block -> block.get())
				.toList().toArray(Block[]::new));
				
		tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.add(ModInit.CONVEYOR_BELT_BLOCK.get(), ModInit.CONVEYOR_SPLITTER_BLOCK.get(), ModInit.ITEM_GRATE_BLOCK.get());
	}

    @Override
    public String getName() {
        return WAMUtils.MODID + " Block Tags";
    }

}
