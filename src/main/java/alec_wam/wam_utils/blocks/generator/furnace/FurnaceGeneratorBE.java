package alec_wam.wam_utils.blocks.generator.furnace;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.capabilities.BlockEnergyStorage;
import alec_wam.wam_utils.capabilities.IEnergyStorageBlockEntity;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ItemInit;
import alec_wam.wam_utils.utils.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public class FurnaceGeneratorBE extends WAMUtilsBlockEntity implements IEnergyStorageBlockEntity {

	public static final int INPUT_SLOTS = 1;
	public static final int CHARGE_SLOTS = 1;
	public static final int UPGRADE_SLOTS = 2;

	private final ItemStackHandler inputItems = createInputItemHandler();
	public final LazyOptional<IItemHandler> inputItemHandler = LazyOptional.of(() -> inputItems);
	private final ItemStackHandler chargeItems = createChargeItemHandler();
	public final LazyOptional<IItemHandler> chargeItemHandler = LazyOptional.of(() -> chargeItems);
	private final ItemStackHandler upgradeItems = createUpgradeItemHandler();
	public final LazyOptional<IItemHandler> upgradeItemHandler = LazyOptional.of(() -> upgradeItems);
	public final LazyOptional<IItemHandler> combinedAllItemHandler = LazyOptional.of(this::createCombinedAllItemHandler);
	
	public BlockEnergyStorage energyStorage;
    private LazyOptional<BlockEnergyStorage> energy;
	
	private boolean running = false;
	
	private int fuelAmount;
	private int maxFuelAmount;
	
	private int speedUpgradeCount = 0;
	
	public FurnaceGeneratorBE(BlockPos pos, BlockState state) {
		super(BlockInit.FURNACE_GENERATOR_BE.get(), pos, state);
		
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

	public int getFuelAmount() {
		return fuelAmount;
	}

	public void setFuelAmount(int fuelAmount) {
		this.fuelAmount = fuelAmount;
	}

	public int getMaxFuelAmount() {
		return maxFuelAmount;
	}

	public void setMaxFuelAmount(int maxFuelAmount) {
		this.maxFuelAmount = maxFuelAmount;
	}

	public void tickClient() {
	}

	public void tickServer() {
		
		final boolean preTickRunning = isRunning();
		
		if(!level.hasNeighborSignal(worldPosition)) {
			
			this.running = false;
			if(this.running != preTickRunning) {
				BlockState newState = getBlockState().setValue(FurnaceGeneratorBlock.RUNNING, Boolean.valueOf(running));
				level.setBlock(worldPosition, newState, 3);
				setChanged(level, worldPosition, newState);
			}
			
			return;
		}
		boolean pushEnergy = true;		
		
		int chargeAmount = 60;
		
		boolean canFitEnergy = energyStorage.addEnergy(chargeAmount, true) > 0;
		if(this.fuelAmount <= 0) {
			if(canFitEnergy) {
				ItemStack fuelItem = inputItems.getStackInSlot(0);
				int burnTime = ForgeHooks.getBurnTime(fuelItem, RecipeType.SMELTING);
				if(burnTime > 0) {				
					//WAMUtilsMod.LOGGER.debug("Fuel: " + burnTime);
					this.fuelAmount = this.maxFuelAmount = burnTime;
					//WAMUtilsMod.LOGGER.debug("maxFuelAmount: " + maxFuelAmount);
					boolean flag3 = !fuelItem.isEmpty();
					if (fuelItem.hasCraftingRemainingItem())
						inputItems.setStackInSlot(0, fuelItem.getCraftingRemainingItem());
					else if (flag3) {
	                  fuelItem.shrink(1);
	                  if (fuelItem.isEmpty()) {
	                	  inputItems.setStackInSlot(0, fuelItem.getCraftingRemainingItem());
	                  }
					}
				}
			}
		}
		else {
			int speed = Math.min(1 + speedUpgradeCount, fuelAmount);
			int testInsertAmount = chargeAmount * speed;
			int simInsert = energyStorage.addEnergy(testInsertAmount, true);
			if(simInsert > 0) {
				int realMulti = (int)((float)simInsert / (float)chargeAmount);
				int fixedMulti = Math.min(realMulti, fuelAmount);
				int realInsertAmount = chargeAmount * fixedMulti;
				energyStorage.addEnergy(realInsertAmount, false);
				this.fuelAmount -=fixedMulti;					
				if(this.fuelAmount <= 0) {
					this.maxFuelAmount = 0;
				}
			}
		}
		
		ItemStack chargeStack = chargeItems.getStackInSlot(0);
		if(!chargeStack.isEmpty()) {
			chargeStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(itemEnergy -> {
				if(itemEnergy.getEnergyStored() >= 0 && itemEnergy.receiveEnergy(itemEnergy.getEnergyStored(), true) >= 0) {
					//Item can fit energy
					int energyRemoved = itemEnergy.receiveEnergy(Math.min(energyStorage.getEnergyStored(), getMaxEnergyOutput()), false);
		            energyStorage.consumeEnergy(energyRemoved, false);
				}
			});
		}
		
		if(pushEnergy && energyStorage.getEnergyStored() > 0) {
			for(Direction dir : Direction.values()) {
				BlockPos otherPos = worldPosition.relative(dir);
				BlockEntity blockEntity = level.getBlockEntity(otherPos);
				if(blockEntity !=null) {
					blockEntity.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(h -> {
						if(energyStorage.getEnergyStored() > 0) {
							if(h.canReceive()) {
								int output = Math.min(energyStorage.getEnergyStored(), getMaxEnergyOutput());
								if(h.receiveEnergy(output, true) > 0) {
									int received = h.receiveEnergy(output, false);
									this.energyStorage.consumeEnergy(received, false);
								}
							}
						}
					});;
				}
			}
		}
		
		this.running = this.fuelAmount > 0;
		if(this.running != preTickRunning) {
			BlockState newState = getBlockState().setValue(FurnaceGeneratorBlock.RUNNING, Boolean.valueOf(running));
			level.setBlock(worldPosition, newState, 3);
			setChanged(level, worldPosition, newState);
		}
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
				return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				if (ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) <= 0) {
					return stack;
				}
				return super.insertItem(slot, stack, simulate);
			}
		};
	}
	
	public boolean canChargeItem(ItemStack stack) {
		if(!stack.isEmpty()) {
			return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
		}
		return false;
	}
	
	public boolean isValidUpgrade(ItemStack stack) {
		if(!stack.isEmpty()) {
			Item item = stack.getItem();
			if(item == ItemInit.UPGRADE_SPEED.get()) {
				return true;
			}
		}
		return false;
	}
	
	public void upgradesChanged() {
		this.speedUpgradeCount = InventoryUtils.countItem(upgradeItems, ItemInit.UPGRADE_SPEED.get());
	}
	
	@Nonnull
	private ItemStackHandler createChargeItemHandler() {
		return new ItemStackHandler(CHARGE_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				setChanged();
			}

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return canChargeItem(stack);
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				if (!canChargeItem(stack)) {
					return stack;
				}
				return super.insertItem(slot, stack, simulate);
			}
		};
	}
	
	@Nonnull
	private ItemStackHandler createUpgradeItemHandler() {
		return new ItemStackHandler(UPGRADE_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				upgradesChanged();
				setChanged();
			}

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return isValidUpgrade(stack);
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				if (!isValidUpgrade(stack)) {
					return stack;
				}
				return super.insertItem(slot, stack, simulate);
			}
		};
	}

	@Nonnull
	private IItemHandler createCombinedAllItemHandler() {
		return new CombinedInvWrapper(inputItems, chargeItems, upgradeItems) {
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
		chargeItemHandler.invalidate();
		upgradeItemHandler.invalidate();
		combinedAllItemHandler.invalidate();
	}

	@Override
	public void writeCustomNBT(CompoundTag tag, boolean decPacket) {
		tag.putInt("fuelAmount", fuelAmount);
		tag.putInt("maxFuelAmount", maxFuelAmount);
		tag.putInt("SpeedUpgrades", speedUpgradeCount);
		tag.put("Inventory.Input", inputItems.serializeNBT());
		tag.put("Inventory.Charge", chargeItems.serializeNBT());
		tag.put("Inventory.Upgrade", upgradeItems.serializeNBT());
        energy.ifPresent(h -> tag.put("energy", h.serializeNBT()));
	}

	@Override
	public void readCustomNBT(CompoundTag tag, boolean descPacket) {
		//loadClientData(tag);
		fuelAmount = tag.getInt("fuelAmount");
		maxFuelAmount = tag.getInt("maxFuelAmount");
		speedUpgradeCount = tag.getInt("SpeedUpgrades");
		if (tag.contains("Inventory.Input")) {
			inputItems.deserializeNBT(tag.getCompound("Inventory.Input"));
		}
		if (tag.contains("Inventory.Charge")) {
			chargeItems.deserializeNBT(tag.getCompound("Inventory.Charge"));
		}
		if (tag.contains("Inventory.Upgrade")) {
			upgradeItems.deserializeNBT(tag.getCompound("Inventory.Upgrade"));
		}
		energy.ifPresent(h -> h.deserializeNBT(tag.getCompound("energy")));
	}
	
	@NotNull
	@Override
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) {
			if (side == null) {
				return combinedAllItemHandler.cast();
			} else if (side == Direction.DOWN || side == Direction.UP) {
				return chargeItemHandler.cast();
			} else {
				return inputItemHandler.cast();
			}
		} 
		else if (cap == ForgeCapabilities.ENERGY) {
            return energy.cast();
		}
		else {
			return super.getCapability(cap, side);
		}
	}

	@Override
	public void onEnergyChanged() {
		this.setChanged();
	}

	@Override
	public boolean canInsertEnergy() {
		return false;
	}

	@Override
	public boolean canExtractEnergy() {
		return true;
	}

	//TODO Make these configurable
	@Override
	public int getEnergyCapacity() {
		return 1000000;
	}

	@Override
	public int getMaxEnergyInput() {
		return 0;
	}

	@Override
	public int getMaxEnergyOutput() {
		return 80 + (80 * speedUpgradeCount);
	}

}

