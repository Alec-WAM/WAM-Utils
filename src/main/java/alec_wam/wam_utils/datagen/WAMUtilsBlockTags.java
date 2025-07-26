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
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

public class WAMUtilsBlockTags extends BlockTagsProvider {

	public WAMUtilsBlockTags(PackOutput output, CompletableFuture<Provider> lookupProvider) {
		super(output, lookupProvider, WAMUtils.MODID);
	}

	public static final TagKey<Block> TREE_ATTACHMENTS = BlockTags.create(ResourceLocation.fromNamespaceAndPath(WAMUtils.MODID, "tree_attachments"));

	public static final TagKey<Block> WORKER_FLOWERS = BlockTags.create(ResourceLocation.fromNamespaceAndPath(WAMUtils.MODID, "worker_flowers"));
	public static final TagKey<Block> WORKER_FLOWERS_BONEMEAL = BlockTags.create(ResourceLocation.fromNamespaceAndPath(WAMUtils.MODID, "worker_flowers_bonemeal"));
	
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
		
		tag(WORKER_FLOWERS)
			.addTag(Tags.Blocks.FLOWERS)
			.remove(Tags.Blocks.FLOWERS_TALL)
			.addTag(BlockTags.EDIBLE_FOR_SHEEP)
			.add(
				Blocks.TALL_GRASS, Blocks.LARGE_FERN, 
				Blocks.AZALEA, Blocks.FLOWERING_AZALEA, 
				Blocks.MOSS_CARPET, Blocks.PALE_MOSS_CARPET,
				Blocks.LEAF_LITTER
			);

		tag(WORKER_FLOWERS_BONEMEAL)
			.add(Blocks.GRASS_BLOCK, Blocks.MOSS_BLOCK, Blocks.PALE_MOSS_BLOCK);
	}

    @Override
    public String getName() {
        return WAMUtils.MODID + " Block Tags";
    }

}
