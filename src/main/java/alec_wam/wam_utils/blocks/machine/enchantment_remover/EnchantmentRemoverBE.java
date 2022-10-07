package alec_wam.wam_utils.blocks.machine.enchantment_remover;

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.capabilities.BlockEnergyStorage;
import alec_wam.wam_utils.capabilities.IEnergyStorageBlockEntity;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ItemInit;
import alec_wam.wam_utils.utils.InventoryUtils;
import alec_wam.wam_utils.utils.ItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public class EnchantmentRemoverBE extends WAMUtilsBlockEntity implements IEnergyStorageBlockEntity {

	public static final int INPUT_SLOTS = 1;
	public static final int OUTPUT_SLOTS = 1;
	public static final int ENCHANTMENT_SLOTS = 10;

	private final ItemStackHandler inputItems = createInputItemHandler();
	public final LazyOptional<IItemHandler> inputItemHandler = LazyOptional.of(() -> inputItems);
	private final ItemStackHandler outputItems = createOutputItemHandler();
	public final LazyOptional<IItemHandler> outputItemHandler = LazyOptional.of(() -> outputItems);
	private final ItemStackHandler enchantmentItems = createEnchantmentItemHandler();
	public final LazyOptional<IItemHandler> enchantmentItemHandler = LazyOptional.of(() -> enchantmentItems);
	public final LazyOptional<IItemHandler> combinedAllItemHandler = LazyOptional.of(this::createCombinedAllItemHandler);
	
	public BlockEnergyStorage energyStorage;
    private LazyOptional<BlockEnergyStorage> energy;
	
	private boolean running = false;	

	private boolean isFull;
	private int removalProgress;
	private int maxRemovalProgress;
	private RedstoneMode redstoneMode = RedstoneMode.ON;
	
	public EnchantmentRemoverBE(BlockPos pos, BlockState state) {
		super(BlockInit.ENCHANTMENT_REMOVER_BE.get(), pos, state);
		
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

	public int getRemovalProgress() {
		return removalProgress;
	}

	public void setRemovalProgress(int removalProgress) {
		this.removalProgress = removalProgress;
	}

	public int getMaxRemovalProgress() {
		return maxRemovalProgress;
	}

	public void setMaxRemovalProgress(int removalProgress) {
		this.maxRemovalProgress = removalProgress;
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

	public void setFull(boolean value) {
		isFull = value;
		if(!this.level.isClientSide) {
			this.setChanged();
		}
	}
	
	public boolean isFull() {
		return isFull;
	}

	public void tickClient() {
	}

	public void tickServer() {
		
		final boolean preTickRunning = isRunning();
		
		if(!redstoneMode.isMet(level, worldPosition)) {
			this.running = false;
			/*if(this.running != preTickRunning) {
				BlockState newState = getBlockState().setValue(FurnaceGeneratorBlock.RUNNING, Boolean.valueOf(running));
				level.setBlock(worldPosition, newState, 3);
				setChanged(level, worldPosition, newState);
			}*/
			return;
		}
		
		/*if(!this.outputItems.getStackInSlot(0).isEmpty()) {
			return;
		}*/
		if(this.removalProgress == 0 && this.maxRemovalProgress == 0) {
			ItemStack input = this.inputItems.getStackInSlot(0);
			ItemStack currentOutput = this.outputItems.getStackInSlot(0);
			if(!input.isEmpty() && (currentOutput.isEmpty() || input.is(Items.ENCHANTED_BOOK) && currentOutput.is(Items.BOOK))) {
				Map<Enchantment, Integer> enchantments = ItemUtils.getAllNonCurseEnchantments(input);
				//WAMUtilsMod.LOGGER.debug("Start: " + enchantments.toString());
				if(enchantments.isEmpty()) {
					//MOVE TO OUTPUT
					ItemStack outputStack = input.copy();
					boolean cancelOutput = false;
					if(input.getItem() == Items.ENCHANTED_BOOK) {
						Map<Enchantment, Integer> allEnchantments = EnchantmentHelper.getEnchantments(input);
						if(allEnchantments.isEmpty()) {
							//If all enchantments are gone (ie. no curse enchantments left) convert to a book
							outputStack = new ItemStack(Items.BOOK);
							if (input.hasCustomHoverName()) {
								outputStack.setHoverName(input.getHoverName());
							}
						}
						else {
							cancelOutput = !currentOutput.isEmpty(); //Cancel output if book has curses and the output slot is full
						}
					}
					else {
						//Copied from grindstone
						outputStack.removeTagKey("Enchantments");
						outputStack.removeTagKey("StoredEnchantments");
						
						if (input.getDamageValue() > 0) {
							outputStack.setDamageValue(input.getDamageValue());
						} else {
							outputStack.removeTagKey("Damage");
						}
						
						Map<Enchantment, Integer> curses = ItemUtils.getAllCurseEnchantments(input);
						if(!curses.isEmpty()) {
							EnchantmentHelper.setEnchantments(curses, outputStack);
							outputStack.setRepairCost(0);
							for(int i = 0; i < curses.size(); ++i) {
								outputStack.setRepairCost(AnvilMenu.calculateIncreasedRepairCost(outputStack.getBaseRepairCost()));
							}
						}
						else {
							outputStack.removeTagKey("RepairCost");
						}
					}
					if(!cancelOutput) {
						input.shrink(1);
						InventoryUtils.forceInsertItem(outputItems, outputStack, 0, false, (slot) -> {
							this.setChanged();
						});
					}
				}
				else {
					this.maxRemovalProgress = 20 * 5; //5 Seconds
				}
			}
		}
		
		if(this.isFull)return;
		
		if(this.removalProgress < this.maxRemovalProgress) {
			
			int energyCost = 80;
			if(energyStorage.getEnergyStored() < energyCost) {
				return;
			}
			
			this.energyStorage.consumeEnergy(energyCost, false);
			
			this.removalProgress++;
		}
		else if(maxRemovalProgress > 0 && removalProgress >= maxRemovalProgress){
			ItemStack input = this.inputItems.getStackInSlot(0);
			if(!input.isEmpty()) {
				Map<Enchantment, Integer> enchantments = ItemUtils.getAllNonCurseEnchantments(input);
				Map<Enchantment, Integer> allEnchantments = EnchantmentHelper.getEnchantments(input);
				Entry<Enchantment, Integer> enchantment = enchantments.entrySet().stream().findFirst().orElse(null);
				
				if(enchantment !=null) {
					allEnchantments.remove(enchantment.getKey());
					//WAMUtilsMod.LOGGER.debug(allEnchantments.toString());
					ItemStack enchantmentOutput = SingleEnchantmentItem.createForEnchantment(new EnchantmentInstance(enchantment.getKey(), enchantment.getValue()));
					//TODO Handle Full
					InventoryUtils.forceStackInInventoryAllSlots(enchantmentItems, enchantmentOutput, (slot) -> {
						updateEnchantments();
						setChanged();
					});
					
					if(input.getItem() == Items.ENCHANTED_BOOK) {
						input.getTag().remove("StoredEnchantments");
						allEnchantments.entrySet().stream().forEach((entry) -> {
							EnchantedBookItem.addEnchantment(input, new EnchantmentInstance(entry.getKey(), entry.getValue()));
						});
					}
					else {
						EnchantmentHelper.setEnchantments(allEnchantments, input);
					}
					
					this.inputItems.setStackInSlot(0, input);
					this.removalProgress = this.maxRemovalProgress = 0;
				}
			}
			else {
				//WAMUtilsMod.LOGGER.debug("Cleaing Progress");
				this.removalProgress = this.maxRemovalProgress = 0;
			}
		}
		
		this.running = this.removalProgress > 0;
		if(this.running != preTickRunning) {
			/*BlockState newState = getBlockState().setValue(FurnaceGeneratorBlock.RUNNING, Boolean.valueOf(running));
			level.setBlock(worldPosition, newState, 3);
			setChanged(level, worldPosition, newState);*/
		}
	}
	
	public boolean isValidInputItem(ItemStack stack) {
		if(stack.is(ItemInit.SINGLE_ENCHANTMENT_ITEM.get()))return false;
		return stack.is(Items.ENCHANTED_BOOK) || stack.isEnchanted();
	}
	
	private void updateInput() {
		if(this.inputItems.getStackInSlot(0).isEmpty()) {
			this.removalProgress = 0;
			this.maxRemovalProgress = 0;
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
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return isValidInputItem(stack);
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				if (!isValidInputItem(stack)) {
					return stack;
				}
				return super.insertItem(slot, stack, simulate);
			}
		};
	}
	
	public void updateEnchantments() {
		this.isFull = InventoryUtils.isFull(enchantmentItems);
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
	private ItemStackHandler createEnchantmentItemHandler() {
		return new ItemStackHandler(ENCHANTMENT_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				updateEnchantments();
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
		return new CombinedInvWrapper(inputItems, outputItems, enchantmentItems) {
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
		outputItemHandler.invalidate();
		enchantmentItemHandler.invalidate();
		combinedAllItemHandler.invalidate();
	}

	@Override
	public void writeCustomNBT(CompoundTag tag, boolean decPacket) {
		tag.putInt("RedstoneMode", redstoneMode.ordinal());
		tag.putInt("removalProgress", removalProgress);
		tag.putInt("maxRemovalProgress", maxRemovalProgress);
		tag.putBoolean("isFull", isFull);
		tag.put("Inventory.Input", inputItems.serializeNBT());
		tag.put("Inventory.Output", outputItems.serializeNBT());
		tag.put("Inventory.Enchantments", enchantmentItems.serializeNBT());
        energy.ifPresent(h -> tag.put("energy", h.serializeNBT()));
	}

	@Override
	public void readCustomNBT(CompoundTag tag, boolean descPacket) {
		redstoneMode = RedstoneMode.getMode(tag.getInt("RedstoneMode"));
		removalProgress = tag.getInt("removalProgress");
		maxRemovalProgress = tag.getInt("maxRemovalProgress");
		isFull = tag.getBoolean("isFull");
		if(!descPacket) {
			if (tag.contains("Inventory.Input")) {
				inputItems.deserializeNBT(tag.getCompound("Inventory.Input"));
			}
			if (tag.contains("Inventory.Output")) {
				outputItems.deserializeNBT(tag.getCompound("Inventory.Output"));
			}
			if (tag.contains("Inventory.Enchantments")) {
				enchantmentItems.deserializeNBT(tag.getCompound("Inventory.Enchantments"));
			}
		}
		energy.ifPresent(h -> h.deserializeNBT(tag.getCompound("energy")));
	}
	
	@Override
	public void receiveMessageFromClient(CompoundTag nbt) {
		super.receiveMessageFromClient(nbt);
		if(nbt.contains("RedstoneMode")) {
			this.redstoneMode = RedstoneMode.getMode(nbt.getInt("RedstoneMode"));
			this.setChanged();
		}
	}
	
	@NotNull
	@Override
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) {
			if (side == null) {
				return combinedAllItemHandler.cast();
			} 
			else if (side == Direction.UP) {
				return inputItemHandler.cast();
			} 
			else if (side == Direction.DOWN) {
				return outputItemHandler.cast();
			} 
			else {
				return enchantmentItemHandler.cast();
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
		return true;
	}

	@Override
	public boolean canExtractEnergy() {
		return false;
	}

	//TODO Make these configurable
	@Override
	public int getEnergyCapacity() {
		return 5000;
	}

	@Override
	public int getMaxEnergyInput() {
		return 160;
	}

	@Override
	public int getMaxEnergyOutput() {
		return 0;
	}

}

