package alec_wam.wam_utils.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import alec_wam.wam_utils.WAMUtils;
import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.blocks.conveyor.ConveyorBeltBlock;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public class WAMUtilsModelProvider extends ModelProvider {

	public WAMUtilsModelProvider(PackOutput output) {
		super(output, WAMUtils.MODID);
	}	

    @SuppressWarnings("deprecation")
	public static ResourceLocation createEmptyTexture(String name) {
        return ModelLocationUtils.decorateBlockModelLocation(name);
    }

	public static final ModelTemplate SHEILD_RACK_MODEL = ModelTemplates.create(
        WAMUtils.MODID + ":shield_rack", TextureSlot.ALL
    );

	public static final ModelTemplate ENCHANTMENT_BOOK_SHELF_MODEL = ModelTemplates.create(
        WAMUtils.MODID + ":enchantment_book_shelf", TextureSlot.ALL
    );

	public static final ResourceLocation CONVEYOR_BELT_MODEL = createEmptyTexture(
        WAMUtils.MODID + ":conveyor_belt"
    );
	public static final ResourceLocation CONVEYOR_BELT_LEFT_MODEL = createEmptyTexture(
        WAMUtils.MODID + ":conveyor_belt_n_left"
    );
	public static final ResourceLocation CONVEYOR_BELT_RIGHT_MODEL = createEmptyTexture(
        WAMUtils.MODID + ":conveyor_belt_n_right"
    );
	public static final ResourceLocation CONVEYOR_BELT_BOTH_MODEL = createEmptyTexture(
        WAMUtils.MODID + ":conveyor_belt_n_both"
    );
	public static final ResourceLocation CONVEYOR_BELT_ANGLED_MODEL = createEmptyTexture(
        WAMUtils.MODID + ":conveyor_belt_angled"
    );

	@Override
	protected Stream<? extends Holder<Block>> getKnownBlocks() {
		List<DeferredBlock<?>> blocks = new ArrayList<>();
		blocks.addAll(ModInit.SHIELDRACK_BLOCKS.values());
		blocks.addAll(ModInit.ENCHANTMENT_BOOK_SHELF_BLOCKS.values());
		blocks.add(ModInit.CONVEYOR_BELT_BLOCK);
		return blocks.stream()
				.map(DeferredBlock::get)
				.map(Holder::direct);
	}

	@Override
	protected Stream<? extends Holder<Item>> getKnownItems() {
		List<DeferredItem<?>> items = new ArrayList<>();
		items.addAll(ModInit.SHIELDRACK_BLOCK_ITEMS.values());
		items.addAll(ModInit.ENCHANTMENT_BOOK_SHELF_BLOCK_ITEMS.values());
		items.add(ModInit.CONVEYOR_BELT_BLOCK_ITEM);
		return items.stream()
				.map(DeferredItem::get)
				.map(Holder::direct);
	}

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        ModInit.SHIELDRACK_BLOCKS.forEach((woodType, blockReg) -> {
			Block block = blockReg.get();
			String woodName = woodType.name().toLowerCase();
			final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("minecraft", "block/" + woodName + "_planks");

			TexturedModel.Provider WOOD = TexturedModel.createDefault(
				(Block blk) -> TextureMapping.cube(texture),
				SHEILD_RACK_MODEL
			);

			blockModels.createHorizontallyRotatedBlock(block, WOOD);
		});

		ModInit.ENCHANTMENT_BOOK_SHELF_BLOCKS.forEach((woodType, blockReg) -> {
			Block block = blockReg.get();
			String woodName = woodType.name().toLowerCase();
			final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("minecraft", "block/" + woodName + "_planks");

			TexturedModel.Provider WOOD = TexturedModel.createDefault(
				(Block blk) -> TextureMapping.cube(texture),
				ENCHANTMENT_BOOK_SHELF_MODEL
			);

			blockModels.createHorizontallyRotatedBlock(block, WOOD);
		});

		Block block = ModInit.CONVEYOR_BELT_BLOCK.get();
		MultiVariant multivariant_normal = BlockModelGenerators.plainVariant(CONVEYOR_BELT_MODEL);
		MultiVariant multivariant_left = BlockModelGenerators.plainVariant(CONVEYOR_BELT_LEFT_MODEL);
		MultiVariant multivariant_right = BlockModelGenerators.plainVariant(CONVEYOR_BELT_RIGHT_MODEL);
        MultiVariant multivariant_both = BlockModelGenerators.plainVariant(CONVEYOR_BELT_BOTH_MODEL);
        MultiVariant multivariant_angled = BlockModelGenerators.plainVariant(CONVEYOR_BELT_ANGLED_MODEL);
        blockModels.blockStateOutput.accept(
			MultiVariantGenerator.dispatch(block)
				.with(PropertyDispatch.initial(ConveyorBeltBlock.SLOPE, ConveyorBeltBlock.LEFT, ConveyorBeltBlock.RIGHT, BlockStateProperties.HORIZONTAL_FACING).generate((slope, left, right, facing) -> {
					MultiVariant variant = multivariant_normal;
					boolean altRotate = slope == ConveyorBeltBlock.BeltSlope.DOWN;
					if (slope == ConveyorBeltBlock.BeltSlope.FLAT) {
						if (left && right) {
							variant = multivariant_both;
						} else if (left) {
							variant = multivariant_left;
						} else if (right) {
							variant = multivariant_right;
						} else {
							variant = multivariant_normal;
						}
					} else {
						variant = multivariant_angled;
					}

					if (facing == Direction.NORTH) {
						variant = variant.with(altRotate ? BlockModelGenerators.Y_ROT_180 : BlockModelGenerators.NOP);
					}
					else if (facing == Direction.SOUTH) {
						variant = variant.with(altRotate ? BlockModelGenerators.NOP : BlockModelGenerators.Y_ROT_180);
					}
					else if (facing == Direction.EAST) {
						variant = variant.with(altRotate ? BlockModelGenerators.Y_ROT_270 : BlockModelGenerators.Y_ROT_90);
					}
					else if (facing == Direction.WEST) {
						variant = variant.with(altRotate ? BlockModelGenerators.Y_ROT_90 : BlockModelGenerators.Y_ROT_270);
					}
					
					return variant;
				}))
		);
    }
}
