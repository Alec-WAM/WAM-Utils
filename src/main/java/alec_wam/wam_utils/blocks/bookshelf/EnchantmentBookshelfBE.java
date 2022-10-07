package alec_wam.wam_utils.blocks.bookshelf;

import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.client.GuiIcons;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ItemInit;
import alec_wam.wam_utils.utils.ItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public class EnchantmentBookshelfBE extends WAMUtilsBlockEntity {

	public static enum EnchantmentCategoryBookshelfFilter {
		ALL(GuiIcons.ENCHANTMENT_ALL, null),
		ARMOR(GuiIcons.ENCHANTMENT_ARMOR, (enchantments) -> {
			return hasMainCategory(enchantments, EnchantmentCategory.ARMOR, EnchantmentCategory.ARMOR_FEET, EnchantmentCategory.ARMOR_LEGS, EnchantmentCategory.ARMOR_CHEST, EnchantmentCategory.ARMOR_HEAD);
		}),
		ARMOR_FEET(GuiIcons.ENCHANTMENT_ARMOR_FEET, (enchantments) -> {
			return hasMainCategory(enchantments, EnchantmentCategory.ARMOR_FEET);
		}),
		ARMOR_LEGS(GuiIcons.ENCHANTMENT_ARMOR_LEGS, (enchantments) -> {
			return hasMainCategory(enchantments, EnchantmentCategory.ARMOR_LEGS);
		}),
		ARMOR_CHEST(GuiIcons.ENCHANTMENT_ARMOR_CHEST, (enchantments) -> {
			return hasMainCategory(enchantments, EnchantmentCategory.ARMOR_CHEST);
		}),
		ARMOR_HEAD(GuiIcons.ENCHANTMENT_ARMOR_HEAD, (enchantments) -> {
			return hasMainCategory(enchantments, EnchantmentCategory.ARMOR_HEAD);
		}),
		WEARABLE(GuiIcons.ENCHANTMENT_WEARABLE, (enchantments) -> {
			return hasMainCategory(enchantments, EnchantmentCategory.WEARABLE, EnchantmentCategory.VANISHABLE);
		}),
		WEAPON(GuiIcons.ENCHANTMENT_WEAPON, (enchantments) -> {
			return hasMainCategory(enchantments, EnchantmentCategory.WEAPON);
		}),
		BOW(GuiIcons.ENCHANTMENT_BOW, (enchantments) -> {
			return hasMainCategory(enchantments, EnchantmentCategory.BOW);
		}),
		CROSSBOW(GuiIcons.ENCHANTMENT_CROSSBOW, (enchantments) -> {
			return hasMainCategory(enchantments, EnchantmentCategory.CROSSBOW);
		}),
		DIGGER(GuiIcons.ENCHANTMENT_DIG, (enchantments) -> {
			return hasMainCategory(enchantments, EnchantmentCategory.DIGGER);
		}),
		FISHING_ROD(GuiIcons.ENCHANTMENT_FISHING_ROD, (enchantments) -> {
			return hasMainCategory(enchantments, EnchantmentCategory.FISHING_ROD);
		}),
		TRIDENT(GuiIcons.ENCHANTMENT_TRIDENT, (enchantments) -> {
			return hasMainCategory(enchantments, EnchantmentCategory.TRIDENT);
		}),
		BREAKABLE(GuiIcons.ENCHANTMENT_BREAKABLE, (enchantments) -> {
			return hasMainCategory(enchantments, EnchantmentCategory.BREAKABLE);
		}),
		CURSE(GuiIcons.ENCHANTMENT_CURSE, (enchantments) -> {
			return hasCurse(enchantments);
		});
		
		private final GuiIcons icon;
		private final Predicate<Set<Enchantment>> filter;
		EnchantmentCategoryBookshelfFilter(GuiIcons icon, Predicate<Set<Enchantment>> filter){
			this.icon = icon;
			this.filter = filter;
		}
		
		public Predicate<Set<Enchantment>> getFilter() {
			return filter;
		}
		
		@OnlyIn(Dist.CLIENT)
		public GuiIcons getIcon() {
			return icon;
		}
		
		public static boolean hasMainCategory(Set<Enchantment> enchantments, EnchantmentCategory... categories) {
			EnchantmentCategory mainCategory = ItemUtils.getMainEnchantmentCategory(enchantments);
			for(EnchantmentCategory category : categories) {
				if(mainCategory == category) {
					return true;
				}
			}
			return false;
		}
		
		public static boolean hasCurse(Set<Enchantment> enchantments) {
			for(Enchantment e : enchantments) {
				if(e.isCurse()) {
					return true;
				}
			}
			return false;
		}
		
		public EnchantmentCategoryBookshelfFilter getPrev() {
			int prevIndex = (this.ordinal() - 1);
			if(prevIndex < 0) {
				prevIndex = EnchantmentCategoryBookshelfFilter.values().length - 1;
			}
			return EnchantmentCategoryBookshelfFilter.values()[prevIndex];
		}
		
		public EnchantmentCategoryBookshelfFilter getNext() {
			return EnchantmentCategoryBookshelfFilter.values()[(this.ordinal() + 1) % (EnchantmentCategoryBookshelfFilter.values().length)];
		}
		
		public static EnchantmentCategoryBookshelfFilter getMode(int index) {
			return EnchantmentCategoryBookshelfFilter.values()[index % (EnchantmentCategoryBookshelfFilter.values().length)];
		}

		public Component getTooltip() {
			return Component.translatable("gui.wam_utils.tooltip.enchantment_category." + name().toLowerCase() + ".tooltip");
		}
	}
	
	public static final int BOOK_SLOTS = 24;
	protected final ItemStackHandler bookItems = createBookItemHandler();
	public final LazyOptional<IItemHandler> bookItemHandler = LazyOptional.of(() -> bookItems);
	public final LazyOptional<IItemHandler> combinedAllItemHandler = LazyOptional.of(this::createCombinedAllItemHandler);
	private EnchantmentCategoryBookshelfFilter filter = EnchantmentCategoryBookshelfFilter.ALL;
		
	public EnchantmentBookshelfBE(BlockPos p_155229_, BlockState p_155230_) {
		super(BlockInit.ENCHANTMENT_BOOKSHELF_BE.get(), p_155229_, p_155230_);
	}
	
	public void setEnchantmentFilter(EnchantmentCategoryBookshelfFilter filter) {
		this.filter = filter;
		if(!this.level.isClientSide) {
			this.setChanged();
		}
	}
	
	public EnchantmentCategoryBookshelfFilter getEnchantmentFilter() {
		return filter;
	}
	
	@Override
	public void receiveMessageFromClient(CompoundTag nbt) {
		if(nbt.contains("Filter")) {
			this.filter = EnchantmentCategoryBookshelfFilter.getMode(nbt.getInt("Filter"));
			this.setChanged();
		}
	}

	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		filter = EnchantmentCategoryBookshelfFilter.getMode(nbt.getInt("Filter"));
		if (nbt.contains("Inventory.Books")) {
			bookItems.deserializeNBT(nbt.getCompound("Inventory.Books"));
		}	
	}

	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		nbt.putInt("Filter", filter.ordinal());
		nbt.put("Inventory.Books", bookItems.serializeNBT());
	}
	
	public void tickServer() {
	}
	
	public void tickClient() {
		
	}
	
	@Override
	public void setRemoved() {
		super.setRemoved();
		bookItemHandler.invalidate();
		combinedAllItemHandler.invalidate();
	}

	public boolean isValidBookForSlot(ItemStack stack, int slot) {
		if(!stack.isEmpty()) {
			if(stack.getItem() == Items.ENCHANTED_BOOK || stack.getItem() == ItemInit.SINGLE_ENCHANTMENT_ITEM.get()) {
				if(this.filter == EnchantmentCategoryBookshelfFilter.ALL) {
					return true;
				}
				
				Set<Enchantment> enchantments = EnchantmentHelper.getEnchantments(stack).keySet();				
				return filter.getFilter().test(enchantments);
			}
			if(stack.getItem() == Items.WRITTEN_BOOK) {
				return true;			
			}
		}
		return false;
	}
	
	@Nonnull
	private ItemStackHandler createBookItemHandler() {
		return new ItemStackHandler(BOOK_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				setChanged();
				markBlockForUpdate(null);
			}

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return isValidBookForSlot(stack, slot);
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				if (!isValidBookForSlot(stack, slot)) {
					return stack;
				}
				return super.insertItem(slot, stack, simulate);
			}
		};
	}
	
	@Nonnull
	private IItemHandler createCombinedAllItemHandler() {
		return new CombinedInvWrapper(bookItems) {
			@NotNull
			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				return ItemStack.EMPTY;
			}

			@NotNull
			@Override
			public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
				return stack;
			}
		};
	}
	
	@NotNull
	@Override
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) {
			Direction dir = getBlockState().getValue(EnchantmentBookshelfBlock.FACING);
			if (side == null) {
				return combinedAllItemHandler.cast();
			} else if (side != dir) {
				return bookItemHandler.cast();
			} 
		} 
		return super.getCapability(cap, side);
	}

}
