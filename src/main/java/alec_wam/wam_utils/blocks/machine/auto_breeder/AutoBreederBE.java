package alec_wam.wam_utils.blocks.machine.auto_breeder;

import java.util.ArrayList;
import java.util.List;

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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public class AutoBreederBE extends WAMUtilsBlockEntity implements IEnergyStorageBlockEntity {

	public static final int INPUT_SLOTS = 3;
	public static final int UPGRADE_SLOTS = 1;
	public static final int MAX_RANGE_UPGRADES = 12;
	
	protected final ItemStackHandler inputItems = createInputItemHandler();
	public final LazyOptional<IItemHandler> inputItemHandler = LazyOptional.of(() -> inputItems);
	protected final ItemStackHandler upgradeItems = createUpgradeItemHandler();
	public final LazyOptional<IItemHandler> upgradeItemHandler = LazyOptional.of(() -> upgradeItems);
	public final LazyOptional<IItemHandler> combinedAllItemHandler = LazyOptional.of(this::createCombinedAllItemHandler);
	
	public BlockEnergyStorage energyStorage;
    private LazyOptional<BlockEnergyStorage> energy;
	
	protected final RandomSource random = RandomSource.create();

	private RedstoneMode redstoneMode = RedstoneMode.ON;
	private boolean running = false;	
	private int feedingDelay;
	private boolean shouldFeedBabies = false;
	private int maxAnimals = 16;
	private int rangeUpgrades;
	private boolean showBoundingBox = true;
	
	public AutoBreederBE(BlockPos pos, BlockState state) {
		super(BlockInit.AUTO_BREEDER_BE.get(), pos, state);
		
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

	public int getFeedingDelay() {
		return feedingDelay;
	}

	public boolean shouldFeedBabies() {
		return shouldFeedBabies;
	}

	public void setShouldFeedBabies(boolean shouldFeedBabies) {
		this.shouldFeedBabies = shouldFeedBabies;
	}

	public int getMaxAnimals() {
		return maxAnimals;
	}

	public void setMaxAnimals(int maxAnimals) {
		this.maxAnimals = maxAnimals;
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

	public boolean showBoundingBox() {
		return showBoundingBox;
	}

	public void setShowBoundingBox(boolean showBoundingBox) {
		this.showBoundingBox = showBoundingBox;
		if(!this.level.isClientSide) {
			this.setChanged();
		}
	}

	public void tickClient() {
	}

	public void tickServer() {
		
		if(!redstoneMode.isMet(level, worldPosition)) {
			return;
		}
		
		ItemStack feedItem = this.inputItems.getStackInSlot(0);
		
		if(feedItem.isEmpty()) {
			if(feedingDelay > 0) {
				feedingDelay = 0;
			}
			return;
		}
		
		if (feedingDelay > 0) {
			feedingDelay--;
		} else {
			if(this.energyStorage.getEnergyStored() <= 0) {
				return;
			}
			
			int energyCostPerFeeding = 50;				
			AABB aabb = getRangeBB();
			List<Animal> allAnimals = level.getEntitiesOfClass(Animal.class, aabb);
			List<Animal> adultAnimals = new ArrayList<Animal>(allAnimals);
			adultAnimals.removeIf((animal) -> {
				return !(canFeedAdultAnimal(animal));
			});
			if(!adultAnimals.isEmpty() && adultAnimals.size() > 0 && this.energyStorage.getEnergyStored() >= energyCostPerFeeding * 2 && allAnimals.size() < maxAnimals) {				
				int count = adultAnimals.size();				
				int randomIndex = random.nextInt(0, count);
				Animal firstAnimal = adultAnimals.get(randomIndex);
				for(int i = 0; i < count; i++) {
					if(i == randomIndex)continue;
					
					Animal otherAnimal = adultAnimals.get(i);
					if(firstAnimal.getClass() == otherAnimal.getClass()) {
						if(feedAnimals(firstAnimal, otherAnimal)) {
							energyStorage.consumeEnergy(energyCostPerFeeding * 2, false);
							feedingDelay = 5 * 20;
							break;
						}
					}
				}
			}
			else if(this.energyStorage.getEnergyStored() >= energyCostPerFeeding && shouldFeedBabies) {				
				//Feed Babies
				List<Animal> babies = new ArrayList<Animal>(allAnimals);
				babies.removeIf((animal) -> {
					return !(canFeedBabyAnimal(animal));
				});

				if(!babies.isEmpty()) {
					int randomIndex = random.nextInt(0, babies.size());
					Animal randomBaby = babies.get(randomIndex);
					int currentAge = randomBaby.getAge();
					ItemStack food = this.findFood(randomBaby);
					if(InventoryUtils.consumeItem(inputItems, food, 1, true) == 0) {
						InventoryUtils.consumeItem(inputItems, food, 1, false);
						randomBaby.ageUp(Animal.getSpeedUpSecondsWhenFeeding(-currentAge), true);
						
						double d0 = 0.5D;
						double d1 = 1.0D;
						for(int i = 0; i < 15; ++i) {
							double d2 = random.nextGaussian() * 0.02D;
							double d3 = random.nextGaussian() * 0.02D;
							double d4 = random.nextGaussian() * 0.02D;
							double d5 = 0.5D;
							double d6 = (double)randomBaby.getX() - d5 + random.nextDouble() * d0 * 2.0D;
							double d7 = (double)randomBaby.getY() + random.nextDouble() * d1;
							double d8 = (double)randomBaby.getZ() - d5 + random.nextDouble() * d0 * 2.0D;
							((ServerLevel)level).sendParticles(ParticleTypes.HAPPY_VILLAGER, d6, d7, d8, 0, d2, d3, d4, 1.0D);
						}
						
						energyStorage.consumeEnergy(energyCostPerFeeding, false);
						feedingDelay = 5 * 20;
					}
				}
			}
		}
	}
	
	public boolean canFeedAdultAnimal(Animal animal) {
		return animal.canFallInLove() && canFeedAnimal(animal, 2) && animal.getAge() == 0;
	}
	
	public boolean canFeedBabyAnimal(Animal animal) {
		return canFeedAnimal(animal, 1) && animal.isBaby();
	}

	@Override
	public void receiveMessageFromClient(CompoundTag nbt) {
		if(nbt.contains("FeedBabies")) {
			setShouldFeedBabies(nbt.getBoolean("FeedBabies"));
		}
		if(nbt.contains("MaxAnimals")) {
			setMaxAnimals(nbt.getInt("MaxAnimals"));
		}
		if(nbt.contains("RedstoneMode")) {
			this.redstoneMode = RedstoneMode.getMode(nbt.getInt("RedstoneMode"));
			this.setChanged();
		}
		if(nbt.contains("ShowBoundingBox")) {
			setShowBoundingBox(nbt.getBoolean("ShowBoundingBox"));
		}
	}
	
	private boolean canFeedAnimal(Animal animal, int neededAmount) {
		int foodCount = 0;
		for(int i = 0; i < this.inputItems.getSlots(); i++) {
			ItemStack stack = this.inputItems.getStackInSlot(i);
			if(animal.isFood(stack)) {
				foodCount += stack.getCount();
			}
		}
		return foodCount >= neededAmount;
	}
	
	private ItemStack findFood(Animal animal) {
		for(int i = 0; i < this.inputItems.getSlots(); i++) {
			ItemStack stack = this.inputItems.getStackInSlot(i);
			if(animal.isFood(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}
	
	private boolean feedAnimals(Animal animal1, Animal animal2) {
		ItemStack food = findFood(animal1);
		if(!food.isEmpty()) {
			if(InventoryUtils.consumeItem(inputItems, food, 2, true) == 0) {
				InventoryUtils.consumeItem(inputItems, food, 2, false);
				animal1.setInLove(null);
				animal2.setInLove(null);
				return true;
			}
		}		
		return false;
	}

	@Override
	public AABB getRenderBoundingBox() {
		return getRangeBB();
	}
	
	public AABB getRangeBB() {
		double radius = getRadius() / 2.0D;
		AABB aabb = new AABB(getBlockPos()).inflate(radius, radius, radius);
		return aabb;
	}
	
	public double getRadius() {
		return 8.0D + (2.0D * Math.min(MAX_RANGE_UPGRADES, rangeUpgrades));
	}

	public boolean isItemAValidUpgradeItem(ItemStack stack) {
		if (!stack.isEmpty()) {
			if (stack.getItem() == ItemInit.UPGRADE_RANGE.get()) {
				return true;
			}
		}
		return false;
	}
	
	public void updateUpgrades() {
		this.rangeUpgrades = InventoryUtils.countItem(upgradeItems, ItemInit.UPGRADE_RANGE.get());
		if(this.level !=null && !this.level.isClientSide) {
			setChanged();
		}
	}
	
	@Nonnull
	private ItemStackHandler createInputItemHandler() {
		return new ItemStackHandler(INPUT_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				setChanged();
			}
		};
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
				return MAX_RANGE_UPGRADES;
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
	private IItemHandler createCombinedAllItemHandler() {
		return new CombinedInvWrapper(inputItems, upgradeItems) {
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
		combinedAllItemHandler.invalidate();
	}

	@Override
	public void writeCustomNBT(CompoundTag tag, boolean descPacket) {
		tag.putInt("RedstoneMode", redstoneMode.ordinal());
		tag.putInt("FeedingDelay", feedingDelay);
		tag.putBoolean("FeedBabies", shouldFeedBabies);
		tag.putInt("MaxAnimals", maxAnimals);
		tag.putInt("RangeUpgrades", rangeUpgrades);
		tag.putBoolean("ShowBoundingBox", showBoundingBox);
		tag.put("Inventory.Input", inputItems.serializeNBT());
		tag.put("Inventory.Upgrades", upgradeItems.serializeNBT());
        energy.ifPresent(h -> tag.put("Energy", h.serializeNBT()));
	}

	@Override
	public void readCustomNBT(CompoundTag tag, boolean descPacket) {
		redstoneMode = RedstoneMode.getMode(tag.getInt("RedstoneMode"));
		feedingDelay = tag.getInt("FeedingDelay");
		shouldFeedBabies = tag.getBoolean("FeedBabies");
		maxAnimals = tag.getInt("MaxAnimals");
		rangeUpgrades = tag.getInt("RangeUpgrades");
		showBoundingBox = tag.getBoolean("ShowBoundingBox");
		if (tag.contains("Inventory.Input")) {
			inputItems.deserializeNBT(tag.getCompound("Inventory.Input"));
		}
		if (tag.contains("Inventory.Upgrades")) {
			upgradeItems.deserializeNBT(tag.getCompound("Inventory.Upgrades"));
		}
		energy.ifPresent(h -> h.deserializeNBT(tag.getCompound("Energy")));
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
			return inputItemHandler.cast();
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
		return 8000;
	}

	@Override
	public int getMaxEnergyInput() {
		return 100;
	}

	@Override
	public int getMaxEnergyOutput() {
		return 0;
	}

}

