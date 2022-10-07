package alec_wam.wam_utils.blocks.item_analyzer;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class ItemAnalyzerBE extends WAMUtilsBlockEntity{

	public static final int INPUT_SLOTS = 1;

	protected final ItemStackHandler inputItems = createInputItemHandler();
	public final LazyOptional<IItemHandler> inputItemHandler = LazyOptional.of(() -> inputItems);
	
	public ItemAnalysisResult itemResult = ItemAnalysisResult.EMPTY;
	
	public static class ItemAnalysisResult {
		public static final ItemAnalysisResult EMPTY = new ItemAnalysisResult();
		
		private ResourceLocation itemID;
		private ItemStack stack = ItemStack.EMPTY;
		private CompoundTag itemNBT;
		private Stream<TagKey<Item>> itemTags;
		
		private ItemAnalysisResult() {
		}
		
		public ItemAnalysisResult(ResourceLocation itemID, ItemStack stack, CompoundTag itemNBT, Stream<TagKey<Item>> itemTags) {
			this.itemID = itemID;
			this.stack = stack;
			this.itemNBT = itemNBT;
			this.itemTags = itemTags;
		}

		public ResourceLocation getItemID() {
			return itemID;
		}

		public ItemStack getItemStack() {
			return stack;
		}

		public CompoundTag getItemNBT() {
			return itemNBT;
		}

		public Stream<TagKey<Item>> getItemTags() {
			return itemTags;
		}
	}
	
	public ItemAnalyzerBE(BlockPos pos, BlockState state) {
		super(BlockInit.ITEM_ANALYZER_BE.get(), pos, state);
	}
	
	@SuppressWarnings("deprecation")
	private void updateInput() {
		ItemStack input = this.inputItems.getStackInSlot(0);
		if(input.isEmpty()) {
			this.itemResult = ItemAnalysisResult.EMPTY;
		}
		else {
			Item item = input.getItem();
			
			ResourceLocation itemID = new ResourceLocation(item.getDescriptionId(input));
			/*Item.Properties props = new Item.Properties()
					.tab(item.getItemCategory())
					.rarity(item.getRarity(input))
					.craftRemainder(item.getCraftingRemainingItem(input).getItem())
					.durability(item.getMaxDamage(input))
					.stacksTo(item.getMaxStackSize(input))
					.food(item.getFoodProperties(input, null));
			if(item.isFireResistant()) {
				props = props.fireResistant();
			}
			if(!item.isRepairable(input)) {
				props = props.setNoRepair();
			}*/
			
			CompoundTag nbt = input.getTag() == null ? null : input.getTag().copy();
			Stream<TagKey<Item>> tags = item.builtInRegistryHolder().tags();
			
			this.itemResult = new ItemAnalysisResult(itemID, input, nbt, tags);
		}
	}
	
	@Nonnull
	private ItemStackHandler createInputItemHandler() {
		return new ItemStackHandler(INPUT_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				updateInput();
				setChanged();
			}
			
			@Override
			public int getSlotLimit(int slot) {
				return 1;
			}
		};
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		inputItemHandler.invalidate();
	}

	@Override
	public void writeCustomNBT(CompoundTag tag, boolean decPacket) {
		tag.put("Inventory.Input", inputItems.serializeNBT());
	}

	@Override
	public void readCustomNBT(CompoundTag tag, boolean descPacket) {
		if (tag.contains("Inventory.Input")) {
			inputItems.deserializeNBT(tag.getCompound("Inventory.Input"));
		}
	}
	
	@Override
	public void receiveMessageFromClient(CompoundTag nbt) {
		
	}

}

