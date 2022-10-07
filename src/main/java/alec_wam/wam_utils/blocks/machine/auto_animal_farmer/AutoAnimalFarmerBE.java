package alec_wam.wam_utils.blocks.machine.auto_animal_farmer;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.capabilities.BlockEnergyStorage;
import alec_wam.wam_utils.capabilities.BlockFluidStorage;
import alec_wam.wam_utils.capabilities.IEnergyStorageBlockEntity;
import alec_wam.wam_utils.entities.WAMUtilsFakePlayer;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ItemInit;
import alec_wam.wam_utils.utils.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.IForgeShearable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public class AutoAnimalFarmerBE extends WAMUtilsBlockEntity implements IEnergyStorageBlockEntity {

	public static final int BASE_FLUID_CAPACITY = 16 * FluidType.BUCKET_VOLUME;
	public static final int MAX_FLUID_UPGRADES = 16;
	public static final int MAX_RANGE_UPGRADES = 12;
	public static final int INPUT_SLOTS = 1;
	public static final int UPGRADE_SLOTS = 2;
	public static final int FLUID_OUTPUT_SLOTS = 1;
	public static final int OUTPUT_SLOTS = 18;
	
	protected final ItemStackHandler inputItems = createInputItemHandler();
	public final LazyOptional<IItemHandler> inputItemHandler = LazyOptional.of(() -> inputItems);
	protected final ItemStackHandler upgradeItems = createUpgradeItemHandler();
	public final LazyOptional<IItemHandler> upgradeItemHandler = LazyOptional.of(() -> upgradeItems);
	protected final ItemStackHandler fluidOutputItems = createFluidOutputItemHandler();
	protected final ItemStackHandler outputItems = createOutputItemHandler();
	public final LazyOptional<IItemHandler> externalOutputItemHandler = LazyOptional.of(this::createExternalOutputItemHandler);
	public final LazyOptional<IItemHandler> combinedAllItemHandler = LazyOptional.of(this::createCombinedAllItemHandler);
	
	public BlockEnergyStorage energyStorage;
    private LazyOptional<BlockEnergyStorage> energy;
    public BlockFluidStorage fluidStorage = createFluidHandler();
    private LazyOptional<BlockFluidStorage> fluid = LazyOptional.of(() -> fluidStorage);
	
	protected final RandomSource random = RandomSource.create();

	private RedstoneMode redstoneMode = RedstoneMode.ON;
	private boolean running = false;	
	private int harvestDelay;
	private int fluidCapacityUpgrades = 0;
	private int rangeUpgrades = 0;
	private boolean showBoundingBox = true;
	
	public AutoAnimalFarmerBE(BlockPos pos, BlockState state) {
		super(BlockInit.AUTO_ANIMAL_FARMER_BE.get(), pos, state);
		
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

	public int getHarvestDelay() {
		return harvestDelay;
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

	@Override
	public void receiveMessageFromClient(CompoundTag nbt) {
		if(nbt.contains("RedstoneMode")) {
			this.redstoneMode = RedstoneMode.getMode(nbt.getInt("RedstoneMode"));
			this.setChanged();
		}
		if(nbt.contains("ShowBoundingBox")) {
			setShowBoundingBox(nbt.getBoolean("ShowBoundingBox"));
		}
	}
	
	public void tickClient() {
	}

	public void tickServer() {
		
		//Fill Fluid Containers
		ItemStack fluidContainer = this.inputItems.getStackInSlot(0);
		if(!fluidContainer.isEmpty()) {
			if(fluidContainer.getItem() == Items.BUCKET) {
				if(fluidStorage.getFluidAmount() >= FluidType.BUCKET_VOLUME) {
					if(fluidOutputItems.getStackInSlot(0).isEmpty()) {
						fluidStorage.forceDrain(FluidType.BUCKET_VOLUME, FluidAction.EXECUTE);
						fluidContainer.shrink(1);
						fluidOutputItems.setStackInSlot(0, new ItemStack(Items.MILK_BUCKET, 1));
					}
				}
			}
			else if(canFillItem(fluidContainer) && fluidContainer.getCount() == 1 && this.fluidOutputItems.getStackInSlot(0).isEmpty()) {
				IFluidHandlerItem handler = fluidContainer.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
				FluidStack fluid = new FluidStack(this.fluidStorage.getFluid().getFluid(), this.fluidStorage.getFluidAmount());
				int toFill = handler.fill(fluid, FluidAction.SIMULATE);
				if(toFill > 0) {
					fluidStorage.forceDrain(toFill, FluidAction.EXECUTE);
					handler.fill(fluid, FluidAction.EXECUTE);					
				}
				else {
					ItemStack copy = fluidContainer.copy();
					fluidContainer.shrink(1);
					fluidOutputItems.setStackInSlot(0, copy);
				}
			}
		}
		
		if(!redstoneMode.isMet(level, worldPosition)) {
			return;
		}
		
		boolean pushLiquid = true;
		if(pushLiquid && fluidStorage.getFluidAmount() > 0) {
			for(Direction dir : Direction.values()) {
				BlockPos otherPos = worldPosition.relative(dir);
				BlockEntity blockEntity = level.getBlockEntity(otherPos);
				if(blockEntity !=null) {
					blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).ifPresent(h -> {
						if(fluidStorage.getFluidAmount() > 0) {
							if(h.fill(fluidStorage.getFluid(), FluidAction.SIMULATE) > 0) {
								int output = Math.min(fluidStorage.getFluidAmount(), 100);
								FluidStack stack = fluidStorage.getFluid().copy();
								stack.setAmount(output);
								int filled = h.fill(stack, FluidAction.EXECUTE);
								this.fluidStorage.forceDrain(filled, FluidAction.EXECUTE);
							}
						}
					});;
				}
			}
		}
		
		if (harvestDelay > 0) {
			harvestDelay--;
		} else {
			if(this.energyStorage.getEnergyStored() <= 0) {
				return;
			}
			
			AABB aabb = getRangeBB();
			List<PathfinderMob> mobs = level.getEntitiesOfClass(PathfinderMob.class, aabb);
			if(!mobs.isEmpty()) {				
				PathfinderMob randomMob = mobs.get(level.random.nextInt(0, mobs.size()));
				WAMUtilsFakePlayer fakePlayer = WAMUtilsFakePlayer.get((ServerLevel)level, worldPosition).get();
				boolean didAction = false;
				int energyCostPerAction = 100;				
				
				if(this.energyStorage.getEnergyStored() >= energyCostPerAction) {
					
					//Shear Sheep
					if(randomMob instanceof IForgeShearable) {
						ItemStack shears = new ItemStack(Items.SHEARS);
						IForgeShearable shearableMob = (IForgeShearable)randomMob;
						if(shearableMob.isShearable(shears, level, randomMob.blockPosition())) {
							List<ItemStack> drops = shearableMob.onSheared(fakePlayer, shears, level, randomMob.blockPosition(), 0);
							if(!drops.isEmpty()) {
								drops.forEach((stack) -> {
									insertItemIntoOutputs(stack, randomMob.position());
								});
								didAction = true;
							}
						}
					}
					
					//Ink Sacs from Squid
					if(!didAction) {
						if(randomMob instanceof Squid) {
							//10% Squid ink
							if(random.nextInt(100) < 10) {
								Item item = Items.INK_SAC;
								if(randomMob instanceof GlowSquid) {
									item = Items.GLOW_INK_SAC;
								}
								
								insertItemIntoOutputs(new ItemStack(item, 1), randomMob.position());
								didAction = true;
							}
						}
					}
					
					//Feathers from Chickens
					if(!didAction) {
						if(randomMob instanceof Chicken) {
							//10% Feather
							if(random.nextInt(100) < 10) {
								insertItemIntoOutputs(new ItemStack(Items.FEATHER, 1), randomMob.position());
								didAction = true;
							}
						}
					}
					
					//Milk Animals
					if(!didAction) {
						if (randomMob instanceof Animal && fluidStorage.getFluidAmount() + 1000 <= fluidStorage.getCapacity()) {
							Animal animal = (Animal)randomMob;
							ItemStack bucket = new ItemStack(Items.BUCKET);
							fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, bucket);							
							if(animal.mobInteract(fakePlayer, InteractionHand.MAIN_HAND).consumesAction()) {
								ItemStack fullBucket = fakePlayer.getItemInHand(InteractionHand.MAIN_HAND);
								IFluidHandlerItem fluidHandlerItem = fullBucket.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
	                            if (fluidHandlerItem != null) {
	                                fluidStorage.forceFill(fluidHandlerItem.drain(Integer.MAX_VALUE, FluidAction.EXECUTE), FluidAction.EXECUTE);
	                                didAction = true;
	                            }
							}
                            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
						}
					}
					
					if(didAction) {
						energyStorage.consumeEnergy(energyCostPerAction, false);
					}
					harvestDelay = (didAction ? 10 : 5) * 20; //10 seconds for each action or 5 if not
				}
			}
			
			//Pickup Eggs
			List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, aabb, (item) -> {
				return !item.getItem().isEmpty() && item.isAlive() && canPickupItem(item.getItem());
			});
			if(!items.isEmpty()) {	
				ItemEntity randomItem = items.get(level.random.nextInt(0, items.size()));
				ItemStack copy = randomItem.getItem().copy();
				final int count = copy.getCount();
				
				int energyPerItem = 10;
				if(this.energyStorage.getEnergyStored() >= count * energyPerItem) {
					ItemStack remainder = InventoryUtils.forceStackInInventoryAllSlots(outputItems, copy, null);
					
					int inserted = count - remainder.getCount();
					if(inserted > 0) {
						if(!remainder.isEmpty()) {
							randomItem.setItem(remainder);
						}
						else {
							randomItem.remove(RemovalReason.KILLED);
						}
						
						energyStorage.consumeEnergy(inserted * energyPerItem, false);
					}
				}
			}
		}
	}
	
	public boolean canPickupItem(ItemStack stack) {
		return stack.is(Items.EGG);
	}
	
	public void insertItemIntoOutputs(ItemStack stack, Vec3 position) {
		ItemStack copy = stack.copy();
    	ItemStack remainder = InventoryUtils.forceStackInInventoryAllSlots(outputItems, copy, null);
    	if(!remainder.isEmpty()) {
    		//TODO Mark full
    		ItemEntity itementity = new ItemEntity(this.level, position.x(), position.y(), position.z(), remainder);
            itementity.setDefaultPickUpDelay();
    		this.level.addFreshEntity(itementity);
    	}
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

	public boolean isItemAValidUpgradeItem(ItemStack stack, int slot) {
		if (!stack.isEmpty()) {
			if (stack.getItem() == ItemInit.UPGRADE_CAPACITY_FLUID.get() && slot == 0) {
				return true;
			}
			if (stack.getItem() == ItemInit.UPGRADE_RANGE.get() && slot == 1) {
				return true;
			}
		}
		return false;
	}
	
	public void updateUpgrades() {
		this.fluidCapacityUpgrades = InventoryUtils.countItem(upgradeItems, ItemInit.UPGRADE_CAPACITY_FLUID.get());
		this.rangeUpgrades = InventoryUtils.countItem(upgradeItems, ItemInit.UPGRADE_RANGE.get());
		
		int capacity = this.fluidStorage.getCapacity();
		if(fluidStorage.getFluidAmount() > capacity) {
			FluidStack stack = fluidStorage.getFluid().copy();
			stack.setAmount(capacity);
			fluidStorage.setFluid(stack);
			setChanged();
		}
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
	private ItemStackHandler createInputItemHandler() {
		return new ItemStackHandler(INPUT_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				setChanged();
			}

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return canFillItem(stack);
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				if (!canFillItem(stack)) {
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
				updateUpgrades();
				setChanged();
			}

			@Override
			public int getSlotLimit(int slot) {
				if(slot == 0) {
					return MAX_FLUID_UPGRADES;
				}
				return MAX_RANGE_UPGRADES;
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
	private ItemStackHandler createFluidOutputItemHandler() {
		return new ItemStackHandler(FLUID_OUTPUT_SLOTS) {
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
				return super.insertItem(slot, stack, simulate);
			}
		};
	}

	@Nonnull
	protected IItemHandler createExternalOutputItemHandler() {
		return new CombinedInvWrapper(fluidOutputItems, outputItems) {
			@NotNull
			@Override
			public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
				return stack;
			}
		};
	}
	
	@Nonnull
	private IItemHandler createCombinedAllItemHandler() {
		return new CombinedInvWrapper(inputItems, fluidOutputItems, outputItems, upgradeItems) {
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
	private BlockFluidStorage createFluidHandler() {
		return new BlockFluidStorage(BASE_FLUID_CAPACITY) {
			@Override
			protected void onContentsChanged() {
				markBlockForUpdate(null);
			}
			
			@Override
			public boolean canFill(FluidStack fluid) {
				return false;
			}
			
			@Override
			public int getCapacity() {
				int capacity = BASE_FLUID_CAPACITY;
				if(fluidCapacityUpgrades > 0) {
					int realCount = Math.min(fluidCapacityUpgrades, MAX_FLUID_UPGRADES);
					capacity += realCount * FluidType.BUCKET_VOLUME;
				}
				return capacity;
			}
		};
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		energy.invalidate();
		fluid.invalidate();
		inputItemHandler.invalidate();
		upgradeItemHandler.invalidate();
		externalOutputItemHandler.invalidate();
		combinedAllItemHandler.invalidate();
	}

	@Override
	public void writeCustomNBT(CompoundTag tag, boolean descPacket) {
		tag.putInt("RedstoneMode", redstoneMode.ordinal());
		tag.putInt("HarvestDelay", harvestDelay);
		tag.putInt("CapacityUpgrades", fluidCapacityUpgrades);
		tag.putInt("RangeUpgrades", rangeUpgrades);
		tag.putBoolean("ShowBoundingBox", showBoundingBox);
		tag.put("Inventory.Input", inputItems.serializeNBT());
		tag.put("Inventory.Upgrades", upgradeItems.serializeNBT());
		tag.put("Inventory.Fluid_Output", fluidOutputItems.serializeNBT());
		tag.put("Inventory.Item_Output", outputItems.serializeNBT());
		fluid.ifPresent(h -> tag.put("Fluid", h.serializeNBT()));
        energy.ifPresent(h -> tag.put("Energy", h.serializeNBT()));
	}

	@Override
	public void readCustomNBT(CompoundTag tag, boolean descPacket) {
		redstoneMode = RedstoneMode.getMode(tag.getInt("RedstoneMode"));
		harvestDelay = tag.getInt("HarvestDelay");
		
		fluidCapacityUpgrades = tag.getInt("CapacityUpgrades");
		rangeUpgrades = tag.getInt("RangeUpgrades");
		showBoundingBox = tag.getBoolean("ShowBoundingBox");
		if (tag.contains("Inventory.Input")) {
			inputItems.deserializeNBT(tag.getCompound("Inventory.Input"));
		}
		if (tag.contains("Inventory.Upgrades")) {
			upgradeItems.deserializeNBT(tag.getCompound("Inventory.Upgrades"));
		}
		if (tag.contains("Inventory.Fluid_Output")) {
			fluidOutputItems.deserializeNBT(tag.getCompound("Inventory.Fluid_Output"));
		}
		if (tag.contains("Inventory.Item_Output")) {
			outputItems.deserializeNBT(tag.getCompound("Inventory.Item_Output"));
		}
		energy.ifPresent(h -> h.deserializeNBT(tag.getCompound("Energy")));
		fluid.ifPresent(h -> h.deserializeNBT(tag.getCompound("Fluid")));
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
			else if(side == Direction.UP) {
				return inputItemHandler.cast();
			}
			else {
				return externalOutputItemHandler.cast();
			}
		}
		else if (cap == ForgeCapabilities.ENERGY) {
            return energy.cast();
		}
		else if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluid.cast();
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
		return 100;
	}

	@Override
	public int getMaxEnergyOutput() {
		return 0;
	}

}

