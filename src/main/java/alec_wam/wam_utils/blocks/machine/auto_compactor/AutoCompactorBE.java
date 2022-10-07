package alec_wam.wam_utils.blocks.machine.auto_compactor;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.capabilities.BlockEnergyStorage;
import alec_wam.wam_utils.capabilities.IEnergyStorageBlockEntity;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ItemInit;
import alec_wam.wam_utils.utils.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public class AutoCompactorBE extends WAMUtilsBlockEntity implements IEnergyStorageBlockEntity {

	public static final int INPUT_SLOTS = 9;
	public static final int UPGRADE_SLOTS = 1;
	public static final int OUTPUT_SLOTS = 1;
	
	protected final ItemStackHandler inputItems = createInputItemHandler();
	public final LazyOptional<IItemHandler> inputItemHandler = LazyOptional.of(() -> inputItems);
	protected final ItemStackHandler upgradeItems = createUpgradeItemHandler();
	public final LazyOptional<IItemHandler> upgradeItemHandler = LazyOptional.of(() -> upgradeItems);
	protected final ItemStackHandler outputItems = createOutputItemHandler();
	public final LazyOptional<IItemHandler> outputItemHandler = LazyOptional.of(() -> outputItems);
	public final LazyOptional<IItemHandler> combinedAllItemHandler = LazyOptional.of(this::createCombinedAllItemHandler);
	
	public BlockEnergyStorage energyStorage;
    private LazyOptional<BlockEnergyStorage> energy;

	private RedstoneMode redstoneMode = RedstoneMode.ON;
	private CompactingMode compactingMode = CompactingMode.TWO_BY_TWO;
	
	public static enum CompactingMode {
		TWO_BY_TWO, THREE_BY_THREE, REPAIR;
		
		public CompactingMode getNext() {
			return CompactingMode.values()[(this.ordinal() + 1) % (CompactingMode.values().length)];
		}
		
		public static CompactingMode getMode(int index) {
			return CompactingMode.values()[index % (CompactingMode.values().length)];
		}
	}
	
	private CraftingRecipe currentRecipe;
	private ItemStack craftingIngredient = ItemStack.EMPTY;
	private int craftingTime;
	private int maxCraftingTime;
	private ItemStack filterStack = ItemStack.EMPTY;
	private InternalCraftingContainer craftingContainer;
	
	public AutoCompactorBE(BlockPos pos, BlockState state) {
		super(BlockInit.AUTO_COMPACTOR_BE.get(), pos, state);
		
		this.energyStorage = new BlockEnergyStorage(this, 0);
        this.energy = LazyOptional.of(() -> this.energyStorage);
        
        this.craftingContainer = new InternalCraftingContainer(3);
	}

	public int getCraftingTime() {
		return craftingTime;
	}

	public void setCraftingTime(int craftingTime) {
		this.craftingTime = craftingTime;
	}

	public int getMaxCraftingTime() {
		return maxCraftingTime;
	}

	public void setMaxCraftingTime(int maxCraftingTime) {
		this.maxCraftingTime = maxCraftingTime;
	}
	
	public void setRedstoneMode(RedstoneMode redstoneMode) {
		this.redstoneMode = redstoneMode;
		if(!this.level.isClientSide) {
			this.setChanged();
		}
	}
	
	public RedstoneMode getRedstoneMode() {
		return redstoneMode;
	}
	
	public void setCompactingMode(CompactingMode value) {
		this.compactingMode = value;
		if(!this.level.isClientSide) {
			this.setChanged();
		}
	}
	
	public CompactingMode getCompactingMode() {
		return compactingMode;
	}

	public void tickClient() {
	}

	public void tickServer() {		
		if(!redstoneMode.isMet(level, worldPosition)) {
			return;
		}	
		
		
		if(this.currentRecipe == null) {
			this.craftingContainer.clearContent();
			CraftingRecipe recipe = null;
			ItemStack ingredient = ItemStack.EMPTY;
			if(compactingMode == CompactingMode.THREE_BY_THREE) {
				Item craftingItem = pickRandomItem(9);
				if(craftingItem != null) {
					ItemStack fakeItem = new ItemStack(craftingItem, 1);
					fakeItem.setCount(1);
					for(int i = 0; i < 9; i++) {
						this.craftingContainer.setItem(i, fakeItem);
					}
					ingredient = fakeItem;
					Optional<CraftingRecipe> testRecipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingContainer, level);
					recipe = testRecipe.isPresent() ? testRecipe.get() : null;
				}
			}
			else if(compactingMode == CompactingMode.TWO_BY_TWO){
				Item craftingItem = pickRandomItem(4);
				if(craftingItem != null) {
					ItemStack fakeItem = new ItemStack(craftingItem, 1);
					this.craftingContainer.setItem(0, fakeItem);
					this.craftingContainer.setItem(1, fakeItem);
					this.craftingContainer.setItem(3, fakeItem);
					this.craftingContainer.setItem(4, fakeItem);
					ingredient = fakeItem;
					Optional<CraftingRecipe> testRecipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingContainer, level);
					recipe = testRecipe.isPresent() ? testRecipe.get() : null;
				}
			}
			else if(compactingMode == CompactingMode.REPAIR){
				int randomToolSlot = pickRandomTool(-1);
				if(randomToolSlot != -1) {
					ItemStack craftingTool = this.inputItems.getStackInSlot(randomToolSlot);
					if(!craftingTool.isEmpty()) {
						int otherRandomToolSlot = pickRandomTool(randomToolSlot);
						if(otherRandomToolSlot != -1) {
							ItemStack otherTool = this.inputItems.getStackInSlot(otherRandomToolSlot);
							if(!otherTool.isEmpty()) {
								this.craftingContainer.setItem(0, craftingTool.copy());
								this.craftingContainer.setItem(1, otherTool.copy());
								Optional<CraftingRecipe> testRecipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingContainer, level);
								recipe = testRecipe.isPresent() ? testRecipe.get() : null;
							}
						}
					}
				}
			}
			
			if(recipe !=null) {
				ItemStack currentOutput = this.outputItems.getStackInSlot(0);
				ItemStack output = recipe.assemble(craftingContainer).copy();
				if(currentOutput.isEmpty() || (InventoryUtils.areItemsEqualIgnoreCount(currentOutput, output, false) && output.getMaxStackSize() > 1)) {
					this.currentRecipe = recipe;
					if(compactingMode != CompactingMode.REPAIR) {
						this.craftingIngredient = ingredient;
					}
		        	this.craftingTime = 0;
		        	int time = 20 * 5;
		        	ItemStack speedUpgrade = this.upgradeItems.getStackInSlot(0);
		        	int upgradeCount = Math.min(10, speedUpgrade.getCount());
		        	this.maxCraftingTime = time - (upgradeCount * 8);
		        	this.setChanged();
				}
	        }
		} 
		else {		
			if(this.craftingTime < this.maxCraftingTime) {
	        	ItemStack speedUpgrade = this.upgradeItems.getStackInSlot(0);
	        	int upgradeCount = Math.min(10, speedUpgrade.getCount());
	        	int cost = 10 + (15 * upgradeCount);
				if(this.energyStorage.getEnergyStored() >= cost) {
					this.energyStorage.consumeEnergy(cost, false);
					this.craftingTime++;
		        	this.setChanged();
				}
			}
			else {
				//Craft
				ItemStack output = this.currentRecipe.assemble(craftingContainer).copy();
				if(compactingMode == CompactingMode.TWO_BY_TWO || compactingMode == CompactingMode.THREE_BY_THREE) {
					if(InventoryUtils.forceInsertItem(outputItems, output, 0, true, null).isEmpty()) {
						int neededItems = compactingMode == CompactingMode.THREE_BY_THREE ? 9 : 4;
						if(InventoryUtils.consumeItem(inputItems, this.craftingIngredient, neededItems, true) == 0) {
							boolean insertedResults = true;
							if(!this.craftingIngredient.getCraftingRemainingItem().isEmpty()) {
								ItemStack resultOutput = this.craftingIngredient.getCraftingRemainingItem().copy();
								resultOutput.setCount(resultOutput.getCount() * neededItems);
								insertedResults = InventoryUtils.forceStackInInventoryAllSlots(inputItems, resultOutput, (slot) -> {
									setChanged();
								}).isEmpty();
							}
							if(insertedResults) {
								InventoryUtils.consumeItem(inputItems, this.craftingIngredient, neededItems, false);
								InventoryUtils.forceInsertItem(outputItems, output, 0, false, (slot) -> {
									setChanged();
								});
								this.currentRecipe = null;
								this.craftingIngredient = ItemStack.EMPTY;
								this.maxCraftingTime = 0;
					        	this.setChanged();
							}
						}
					}
				}
				else {
					//HANDLE TOOLS
					if(InventoryUtils.forceInsertItem(outputItems, output, 0, true, null).isEmpty()) {
						boolean foundLeftTool = InventoryUtils.consumeItem(inputItems, this.craftingContainer.getItem(0), 1, true) == 0;
						boolean foundRightTool = InventoryUtils.consumeItem(inputItems, this.craftingContainer.getItem(1), 1, true) == 0;
						if(foundLeftTool && foundRightTool) {
							InventoryUtils.consumeItem(inputItems, this.craftingContainer.getItem(0), 1, false);
							InventoryUtils.consumeItem(inputItems, this.craftingContainer.getItem(1), 1, false);
							InventoryUtils.forceInsertItem(outputItems, output, 0, false, (slot) -> {
								setChanged();
							});
							this.currentRecipe = null;
							this.maxCraftingTime = 0;
				        	this.setChanged();
						}
					}
				}
			}
		}
	}
	
	private Item pickRandomItem(int minSize) {
		List<Item> validSlots = Lists.newArrayList();
		for(int i = 0; i < this.inputItems.getSlots(); i++) {
			ItemStack slotStack = this.inputItems.getStackInSlot(i);
			if(!slotStack.isEmpty() && !validSlots.contains(slotStack.getItem())) {
				int count = InventoryUtils.countItem(inputItems, slotStack);
				if(count >= minSize) {
					validSlots.add(slotStack.getItem());
				}
			}
		}
		if(validSlots.isEmpty()) {
			return null;
		}
		int index = this.level.random.nextInt(0, validSlots.size());
		return validSlots.get(index);
	}
	
	private int pickRandomTool(int ignoreSlot) {
		List<Integer> validSlots = Lists.newArrayList();
		for(int i = 0; i < this.inputItems.getSlots(); i++) {
			if(ignoreSlot != -1 && i == ignoreSlot)continue;
			
			ItemStack slotStack = this.inputItems.getStackInSlot(i);
			if(!slotStack.isEmpty()) {				
				if(slotStack.isDamageableItem() && slotStack.isDamaged()) {
					validSlots.add(Integer.valueOf(i));
				}
			}
		}
		if(validSlots.isEmpty()) {
			return -1;
		}
		int index = this.level.random.nextInt(0, validSlots.size());
		return validSlots.get(index);
	}
	
	@Override
	public void receiveMessageFromClient(CompoundTag nbt) {
		if(nbt.contains("RedstoneMode")) {
			this.redstoneMode = RedstoneMode.getMode(nbt.getInt("RedstoneMode"));
			this.setChanged();
		}
		if(nbt.contains("CompactingMode")) {
			this.compactingMode = CompactingMode.getMode(nbt.getInt("CompactingMode"));
			this.setChanged();
		}
	}

	public boolean isItemAValidUpgradeItem(ItemStack stack) {
		if (!stack.isEmpty()) {
			return stack.getItem() == ItemInit.UPGRADE_SPEED.get();
		}
		return false;
	}
	
	@Nonnull
	private ItemStackHandler createInputItemHandler() {
		return new ItemStackHandler(INPUT_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				setChanged();
			}

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return true;
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				return super.insertItem(slot, stack, simulate);
			}
		};
	}
	
	public void updateUpgrades() {
		//this.fluidCapacityUpgrades = InventoryUtils.countItem(upgradeItems, ItemInit.UPGRADE_CAPACITY_FLUID.get());
	}
	
	@Nonnull
	private ItemStackHandler createUpgradeItemHandler() {
		return new ItemStackHandler(UPGRADE_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				updateUpgrades();
				setChanged();
			}

			@Override
			public int getSlotLimit(int slot) {
				return super.getSlotLimit(slot);
			}

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return isItemAValidUpgradeItem(stack);
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				if (!isItemAValidUpgradeItem(stack)) {
					return stack;
				}
				return super.insertItem(slot, stack, simulate);
			}
		};
	}
	
	@Nonnull
	private ItemStackHandler createOutputItemHandler() {
		return new ItemStackHandler(OUTPUT_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				setChanged();
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				return stack;
			}
		};
	}
	
	@Nonnull
	private IItemHandler createCombinedAllItemHandler() {
		return new CombinedInvWrapper(inputItems, upgradeItems, outputItems) {
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

	@Override
	public void setRemoved() {
		super.setRemoved();
		energy.invalidate();
		inputItemHandler.invalidate();
		upgradeItemHandler.invalidate();
		outputItemHandler.invalidate();
		combinedAllItemHandler.invalidate();
	}

	@Override
	public void writeCustomNBT(CompoundTag tag, boolean descPacket) {
		tag.putInt("RedstoneMode", redstoneMode.ordinal());

		tag.putInt("CompactingMode", compactingMode.ordinal());
		tag.putInt("CraftingTime", craftingTime);
		tag.putInt("MaxCraftingTime", maxCraftingTime);
		
		if(this.currentRecipe !=null) {
			tag.putString("CurrentRecipe", currentRecipe.getId().toString());
		}
		if(!this.craftingIngredient.isEmpty()) {
			tag.put("CurrentIngredient", this.craftingIngredient.serializeNBT());
		}
		
		if(!descPacket) {
			tag.put("Inventory.Input", inputItems.serializeNBT());
			tag.put("Inventory.Upgrades", upgradeItems.serializeNBT());
			tag.put("Inventory.Output", outputItems.serializeNBT());
		}
        energy.ifPresent(h -> tag.put("Energy", h.serializeNBT()));
	}

	@Override
	public void readCustomNBT(CompoundTag tag, boolean descPacket) {
		redstoneMode = RedstoneMode.getMode(tag.getInt("RedstoneMode"));
		
		compactingMode = CompactingMode.getMode(tag.getInt("CompactingMode"));
		craftingTime = tag.getInt("CraftingTime");
		maxCraftingTime = tag.getInt("MaxCraftingTime");	
		currentRecipe = null;
		if(tag.contains("CurrentRecipe") && !level.isClientSide) {
			Optional<? extends Recipe<?>> serverRecipe = this.level.getServer().getRecipeManager().byKey(new ResourceLocation(tag.getString("CurrentRecipe")));;
			if(serverRecipe.isPresent()) {
				Recipe<?> recipe = serverRecipe.get();
				if(recipe instanceof CraftingRecipe) {
					this.currentRecipe = (CraftingRecipe)recipe;
				}
			}
		}
		if(tag.contains("CurrentIngredient")) {
			this.craftingIngredient = ItemStack.of(tag.getCompound("CurrentIngredient"));
		}
		else {
			this.craftingIngredient = ItemStack.EMPTY;
		}
		
		if(!descPacket) {
			if (tag.contains("Inventory.Input")) {
				inputItems.deserializeNBT(tag.getCompound("Inventory.Input"));
			}
			if (tag.contains("Inventory.Upgrades")) {
				upgradeItems.deserializeNBT(tag.getCompound("Inventory.Upgrades"));
			}
			if (tag.contains("Inventory.Output")) {
				outputItems.deserializeNBT(tag.getCompound("Inventory.Output"));
			}
		}
		energy.ifPresent(h -> h.deserializeNBT(tag.getCompound("Energy")));
	}
	
	@NotNull
	@Override
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if(cap == ForgeCapabilities.ITEM_HANDLER) {
			if(side == null) {
				return combinedAllItemHandler.cast();
			}
			else if(side == Direction.UP) {
				return inputItemHandler.cast();
			}
			return outputItemHandler.cast();
		}
		else if (cap == ForgeCapabilities.ENERGY) {
            return energy.cast();
		}
		return super.getCapability(cap, side);
	}

	//ENERGY
	
	@Override
	public void onEnergyChanged() {
		setChanged();
		markBlockForUpdate(null);
	}

	@Override
	public boolean canInsertEnergy() {
		return true;
	}

	@Override
	public boolean canExtractEnergy() {
		return false;
	}

	@Override
	public int getEnergyCapacity() {
		//TODO Make this configurable
		return 10000;
	}

	@Override
	public int getMaxEnergyInput() {
		ItemStack speedUpgrade = this.upgradeItems.getStackInSlot(0);
    	int upgradeCount = Math.min(10, speedUpgrade.getCount());
		return 80 + (80 * upgradeCount);
	}

	@Override
	public int getMaxEnergyOutput() {
		return 0;
	}

}

