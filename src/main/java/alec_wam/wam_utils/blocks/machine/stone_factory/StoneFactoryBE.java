package alec_wam.wam_utils.blocks.machine.stone_factory;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.capabilities.BlockEnergyStorage;
import alec_wam.wam_utils.capabilities.BlockFluidStorage;
import alec_wam.wam_utils.capabilities.CombinedBlockFluidStorage;
import alec_wam.wam_utils.capabilities.IEnergyStorageBlockEntity;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ItemInit;
import alec_wam.wam_utils.recipe.StoneFactoryRecipe;
import alec_wam.wam_utils.utils.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public class StoneFactoryBE extends WAMUtilsBlockEntity implements IEnergyStorageBlockEntity {

	public static final int FLUID_CAPACITY = 8 * FluidType.BUCKET_VOLUME;
	public static final int UPGRADE_SLOTS = 1;
	public static final int OUTPUT_SLOTS = 1;
	public static final int MAX_SPEED_UPGRADES = 10;
	
	protected final ItemStackHandler upgradeItems = createUpgradeItemHandler();
	public final LazyOptional<IItemHandler> upgradeItemHandler = LazyOptional.of(() -> upgradeItems);
	protected final ItemStackHandler outputItems = createOutputItemHandler();
	public final LazyOptional<IItemHandler> externalOutputItemHandler = LazyOptional.of(this::createExternalOutputItemHandler);
	public final LazyOptional<IItemHandler> combinedAllItemHandler = LazyOptional.of(this::createCombinedAllItemHandler);
	
	public BlockEnergyStorage energyStorage;
    private LazyOptional<BlockEnergyStorage> energy;
    public BlockFluidStorage fluidStorageWater = createFluidHandler(Fluids.WATER);
    public BlockFluidStorage fluidStorageLava = createFluidHandler(Fluids.LAVA);
    public LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(this::createExternalFluidHandler);
	
	private RedstoneMode redstoneMode = RedstoneMode.ON;
	private boolean running = false;	
	private int speedUpgrades = 0;
	private int craftingTime;
	private int maxCraftingTime;
	
	public static final StoneFactoryRecipe FAKE_RECIPE = new StoneFactoryRecipe(new ResourceLocation("wam_utils:fake"), 100, 100, new ItemStack(Blocks.COBBLESTONE), 20);
	
	private ResourceLocation savedRecipe;
	private StoneFactoryRecipe selectedRecipe;
	
	public StoneFactoryBE(BlockPos pos, BlockState state) {
		super(BlockInit.STONE_FACTORY_BE.get(), pos, state);
		
		this.energyStorage = new BlockEnergyStorage(this, 0);
        this.energy = LazyOptional.of(() -> this.energyStorage);
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
		setChanged();
		level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
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

	public StoneFactoryRecipe getSelectedRecipe() {
		return selectedRecipe;
	}

	public void setSelectedRecipe(StoneFactoryRecipe selectedRecipe) {
		this.selectedRecipe = selectedRecipe;
		this.craftingTime = 0;
		this.maxCraftingTime = 0;
		setChanged();
	}

	@Override
	public void receiveMessageFromClient(CompoundTag nbt) {
		if(nbt.contains("RedstoneMode")) {
			this.redstoneMode = RedstoneMode.getMode(nbt.getInt("RedstoneMode"));
			this.setChanged();
		}
		if(nbt.contains("RecipeID")) {
			Optional<? extends Recipe<?>> serverRecipe = this.level.getServer().getRecipeManager().byKey(new ResourceLocation(nbt.getString("RecipeID")));;
			this.selectedRecipe = null;
			if(serverRecipe.isPresent()) {
				Recipe<?> recipe = serverRecipe.get();
				if(recipe instanceof StoneFactoryRecipe) {
					this.setSelectedRecipe((StoneFactoryRecipe)recipe);
				}
			}
		}
	}
	
	public void tickClient() {
	}

	public void tickServer() {		
		if(savedRecipe != null) {
			Optional<? extends Recipe<?>> serverRecipe = this.level.getServer().getRecipeManager().byKey(savedRecipe);
			if(serverRecipe.isPresent()) {
				Recipe<?> recipe = serverRecipe.get();
				if(recipe instanceof StoneFactoryRecipe) {
					this.setSelectedRecipe((StoneFactoryRecipe)recipe);
					this.savedRecipe = null;
				}
			}
		}
		
		if(!redstoneMode.isMet(level, worldPosition)) {
			return;
		}
		
		if(selectedRecipe !=null) {
			int neededWater = selectedRecipe.getWaterAmount();
			int neededLava = selectedRecipe.getLavaAmount();
			
			int maxSpeed = Math.min(MAX_SPEED_UPGRADES, speedUpgrades);			
			int neededEnergy = 10 * (1 + maxSpeed);
			
			if(this.energyStorage.getEnergyStored() >= neededEnergy) {
				if(fluidStorageWater.getFluidAmount() >= neededWater && fluidStorageLava.getFluidAmount() >= neededLava) {
					if(maxCraftingTime <= 0) {
						this.craftingTime = 0;
						this.maxCraftingTime = selectedRecipe.getCraftingTime();
					}
					
					this.energyStorage.consumeEnergy(neededEnergy, false);
					
					if(craftingTime < maxCraftingTime) {
						craftingTime += 1 + maxSpeed;
						if(craftingTime > maxCraftingTime) {
							craftingTime = maxCraftingTime;
						}
					}
					else {
						//Craft
						ItemStack copyOutput = selectedRecipe.getResultItem().copy();
						ItemStack remainder = InventoryUtils.forceInsertItem(outputItems, copyOutput, 0, true, null);
						
						if(remainder.isEmpty()) {
							if(neededWater > 0) {
								this.fluidStorageWater.forceDrain(neededWater, FluidAction.EXECUTE);
							}
							if(neededLava > 0) {
								this.fluidStorageLava.forceDrain(neededLava, FluidAction.EXECUTE);
							}
							copyOutput = selectedRecipe.getResultItem().copy();
							InventoryUtils.forceInsertItem(outputItems, copyOutput, 0, false, null);
							this.craftingTime = 0;
							this.maxCraftingTime = selectedRecipe.getCraftingTime();
						}
					}
				}
			}
		}
		else {
			if(this.craftingTime > 0) {
				this.craftingTime = 0;
				this.maxCraftingTime = 0;
			}
		}
	}
	
	public boolean isItemAValidUpgradeItem(ItemStack stack, int slot) {
		if (!stack.isEmpty()) {
			if (stack.is(ItemInit.UPGRADE_SPEED.get())) {
				return true;
			}
		}
		return false;
	}
	
	public void updateUpgrades() {
		this.speedUpgrades = InventoryUtils.countItem(upgradeItems, ItemInit.UPGRADE_SPEED.get());
	}
	
	public boolean canFillItem(ItemStack stack) {
		if(stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) {
			IFluidHandlerItem handler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
			FluidStack fluid = handler.getFluidInTank(0);
			if(fluid.isFluidEqual(FluidStack.EMPTY)|| fluid.getFluid() == ForgeMod.MILK.get()) {
				return true;
			}
		}
		return false;
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
				if(slot == 0) {
					return MAX_SPEED_UPGRADES;
				}
				return super.getSlotLimit(slot);
			}

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return isItemAValidUpgradeItem(stack, slot);
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				if (!isItemAValidUpgradeItem(stack, slot)) {
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

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return true;
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				return stack;
			}
		};
	}

	@Nonnull
	protected IItemHandler createExternalOutputItemHandler() {
		return new CombinedInvWrapper(outputItems) {
			@NotNull
			@Override
			public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
				return stack;
			}
		};
	}
	
	@Nonnull
	private IItemHandler createCombinedAllItemHandler() {
		return new CombinedInvWrapper(outputItems, upgradeItems) {
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
	
	@Nonnull
	private BlockFluidStorage createFluidHandler(final Fluid validFluid) {
		return new BlockFluidStorage(FLUID_CAPACITY) {
			
			@Override
			public boolean isFluidValid(FluidStack stack) {
				return stack.getFluid().isSame(validFluid);
			}
			
			@Override
			protected void onContentsChanged() {
				setChanged();
				markBlockForUpdate(null);
			}
			
			@Override
			public boolean canFill(FluidStack fluid) {
				return isFluidValid(fluid);
			}
			
			@Override
			public boolean canDrain() {
				return false;
			}
			
		};
	}

	@Nonnull
	protected IFluidHandler createExternalFluidHandler() {
		return new CombinedBlockFluidStorage(fluidStorageWater, fluidStorageLava);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		energy.invalidate();
		fluidHandler.invalidate();
		upgradeItemHandler.invalidate();
		externalOutputItemHandler.invalidate();
		combinedAllItemHandler.invalidate();
	}

	@Override
	public void writeCustomNBT(CompoundTag tag, boolean descPacket) {
		tag.putInt("RedstoneMode", redstoneMode.ordinal());
		tag.putInt("CraftingTime", craftingTime);
		tag.putInt("MaxCraftingTime", maxCraftingTime);
		tag.putInt("SpeedUpgrades", speedUpgrades);
		if(this.selectedRecipe !=null) {
			tag.putString("CurrentRecipe", selectedRecipe.getId().toString());
		}
		tag.put("Inventory.Upgrades", upgradeItems.serializeNBT());
		tag.put("Inventory.Output", outputItems.serializeNBT());
		tag.put("Fluid.Water", fluidStorageWater.serializeNBT());
		tag.put("Fluid.Lava", fluidStorageLava.serializeNBT());
        energy.ifPresent(h -> tag.put("Energy", h.serializeNBT()));
	}

	@Override
	public void readCustomNBT(CompoundTag tag, boolean descPacket) {
		redstoneMode = RedstoneMode.getMode(tag.getInt("RedstoneMode"));
		craftingTime = tag.getInt("CraftingTime");
		maxCraftingTime = tag.getInt("MaxCraftingTime");
		speedUpgrades = tag.getInt("SpeedUpgrades");
		
		selectedRecipe = null;
		if(tag.contains("CurrentRecipe")) {
			ResourceLocation recipeID = new ResourceLocation(tag.getString("CurrentRecipe"));
			if(level !=null) {
				if(!level.isClientSide) {
					Optional<? extends Recipe<?>> serverRecipe = this.level.getServer().getRecipeManager().byKey(recipeID);
					if(serverRecipe.isPresent()) {
						Recipe<?> recipe = serverRecipe.get();
						if(recipe instanceof StoneFactoryRecipe) {
							this.selectedRecipe = (StoneFactoryRecipe)recipe;
						}
					}
				}
				else {
					Optional<? extends Recipe<?>> clientRecipe = this.level.getRecipeManager().byKey(recipeID);
					if(clientRecipe.isPresent()) {
						Recipe<?> recipe = clientRecipe.get();
						if(recipe instanceof StoneFactoryRecipe) {
							this.selectedRecipe = (StoneFactoryRecipe)recipe;
						}
					}
				}
			}
			else {
				this.savedRecipe = recipeID;
			}
		}
		
		if (tag.contains("Inventory.Upgrades")) {
			upgradeItems.deserializeNBT(tag.getCompound("Inventory.Upgrades"));
		}
		if (tag.contains("Inventory.Output")) {
			outputItems.deserializeNBT(tag.getCompound("Inventory.Output"));
		}
		energy.ifPresent(h -> h.deserializeNBT(tag.getCompound("Energy")));
		fluidStorageWater.deserializeNBT(tag.getCompound("Fluid.Water"));
		fluidStorageLava.deserializeNBT(tag.getCompound("Fluid.Lava"));
		if(descPacket)
			this.markBlockForUpdate(null);
	}
	
	@NotNull
	@Override
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if(cap == ForgeCapabilities.ITEM_HANDLER) {
			if(side == null) {
				return combinedAllItemHandler.cast();
			}
			return externalOutputItemHandler.cast();
		}
		else if (cap == ForgeCapabilities.ENERGY) {
            return energy.cast();
		}
		else if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidHandler.cast();
		}
		return super.getCapability(cap, side);
	}

	//ENERGY
	
	@Override
	public void onEnergyChanged() {
		setChanged();
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
		return 8000;
	}

	@Override
	public int getMaxEnergyInput() {
		return 200;
	}

	@Override
	public int getMaxEnergyOutput() {
		return 0;
	}

}

