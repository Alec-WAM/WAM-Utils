package alec_wam.wam_utils.blocks.machine.auto_farmer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.blocks.machine.BonemealMode;
import alec_wam.wam_utils.blocks.machine.SlotLock;
import alec_wam.wam_utils.capabilities.BlockEnergyStorage;
import alec_wam.wam_utils.capabilities.IEnergyStorageBlockEntity;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ItemInit;
import alec_wam.wam_utils.utils.BlockUtils;
import alec_wam.wam_utils.utils.InventoryUtils;
import alec_wam.wam_utils.utils.ItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FungusBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public class AutoFarmerBE extends WAMUtilsBlockEntity implements IEnergyStorageBlockEntity {

	public static final int INPUT_SLOTS = 5;
	public static final int UPGRADE_SLOTS = 2;
	public static final int OUTPUT_SLOTS = 18;
	public static final int MAX_RANGE_UPGRADES = 4;
	
	private static ItemStack TOOL_NORMAL;
    private static ItemStack TOOL_FORTUNE;
    
    public static final int ENERGY_FOR_PLANTING = 80;
    public static final int ENERGY_FOR_BONEMEAL = 500;
    public static final int ENERGY_FOR_ADVANCED_BONEMEAL = 1000;
	
	protected final ItemStackHandler seedItems = createSeedItemHandler();
	public final LazyOptional<IItemHandler> seedItemHandler = LazyOptional.of(() -> seedItems);
	protected final ItemStackHandler upgradeItems = createUpgradeItemHandler();
	public final LazyOptional<IItemHandler> upgradeItemHandler = LazyOptional.of(() -> upgradeItems);
	protected final ItemStackHandler outputItems = createOutputItemHandler();
	public final LazyOptional<IItemHandler> outputItemHandler = LazyOptional.of(() -> outputItems);
	public final LazyOptional<IItemHandler> combinedAllItemHandler = LazyOptional.of(this::createCombinedAllItemHandler);
	
	public BlockEnergyStorage energyStorage;
    private LazyOptional<BlockEnergyStorage> energy;
	
	protected final RandomSource random = RandomSource.create();

	private RedstoneMode redstoneMode = RedstoneMode.ON;
	private BonemealMode bonemealMode = BonemealMode.OFF;
	private boolean running = false;	
	private int scanDelay;
	private Vec3i lastScanPosition;
	
	private int hopperCooldown;
	private boolean restockSeeds = true;
	private boolean pullSeeds = true;
	private boolean pushOutput = true;
	private int rangeUpgradeCount = 0;
	
	private Map<Vec3i, CropSettings> cropSettings;
	public StructureTemplate template;
	
	private SlotLock[] slotLocks = new SlotLock[INPUT_SLOTS];
	
	public AutoFarmerBE(BlockPos pos, BlockState state) {
		super(BlockInit.AUTO_FARMER_BE.get(), pos, state);
		
		this.energyStorage = new BlockEnergyStorage(this, 0);
        this.energy = LazyOptional.of(() -> this.energyStorage);
        buildCropSettings();
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
		setChanged();
		level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
	}

	public boolean shouldRestockSeeds() {
		return restockSeeds;
	}

	public void setRestockSeeds(boolean restockSeeds) {
		this.restockSeeds = pullSeeds;
		if(!this.level.isClientSide) {
			setChanged();
		}
	}

	public boolean shouldPushOutput() {
		return pushOutput;
	}

	public void setPushOutput(boolean pushOutput) {
		this.pushOutput = pushOutput;
		if(!this.level.isClientSide) {
			setChanged();
		}
	}

	public boolean shouldPullInSeeds() {
		return pullSeeds;
	}

	public void setPullInSeeds(boolean pullSeeds) {
		this.pullSeeds = pullSeeds;
		if(!this.level.isClientSide) {
			setChanged();
		}
	}
	
	public void setBonemealMode(BonemealMode mode) {
		this.bonemealMode = mode;
		if(!this.level.isClientSide) {
			this.setChanged();
		}
	}
	
	public BonemealMode getBonemealMode() {
		return bonemealMode;
	}

	public void tickClient() {
	}

	public void tickServer() {
		
		if(!redstoneMode.isMet(level, worldPosition)) {
			return;
		}
		
		if(hopperCooldown > 0) {
			hopperCooldown --;
		}
		else {			
			boolean didWork = false;

			if(restockSeeds) {
				for(int i = 0; i < outputItems.getSlots(); i++) {
					ItemStack inputStack = outputItems.getStackInSlot(i);
					if(inputStack.isEmpty() || !BlockUtils.isSeedItem(inputStack))continue;
					ItemStack copy = inputStack.copy();
					copy.setCount(1);
					
					for(int j = 0; j < this.seedItems.getSlots(); j++) {
						ItemStack otherStack = this.seedItems.getStackInSlot(j);
						if(otherStack.isEmpty()) {
							SlotLock lock = this.slotLocks[j];
							if(lock != null) {
								if(!lock.getStack().isEmpty() && lock.isEnabled()) {
									otherStack = lock.getStack();
								}
							}
						}
						
						if(InventoryUtils.areItemsEqualIgnoreCount(copy, otherStack, false)) {
							ItemStack insertStack = InventoryUtils.insertStack(seedItems, copy, j);
							if(insertStack.isEmpty()) {						
								inputStack.shrink(1);
								outputItems.setStackInSlot(i, inputStack.isEmpty() ? ItemStack.EMPTY : inputStack);						
								hopperCooldown = 8; //Hopper Speed
								didWork = true;
								break;
							}
						}
					}
				}
			}
			
			if(!didWork) {
				BlockEntity blockEntity = this.level.getBlockEntity(worldPosition.above());
				if(blockEntity !=null) {
					IItemHandler handler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElse(null);
					if(handler !=null) {	
						//Pull in seeds
						if(!didWork && pullSeeds) {
							for(int i = 0; i < handler.getSlots(); i++) {
								ItemStack inputStack = handler.getStackInSlot(i);
								if(inputStack.isEmpty())continue;
								ItemStack simulateExtract = handler.extractItem(i, 1, true);
								if(!simulateExtract.isEmpty()) {
									//TODO Make this match slot locks not just pull everything
									ItemStack insertStack = InventoryUtils.putStackInInventoryAllSlots(seedItems, simulateExtract);
									if(insertStack.isEmpty()) {
										handler.extractItem(i, 1, false);
										hopperCooldown = 8; //Hopper Speed
										didWork = true;
										break;
									}
								}
							}
						}
	
						if(!didWork && pushOutput) {
							//Push out items
							for(int i = 0; i < this.outputItems.getSlots(); i++) {
								ItemStack outputStack = this.outputItems.getStackInSlot(i);
								if(outputStack.isEmpty())continue;
								ItemStack outputSizeCopy = outputStack.copy();
								int transferSize = 1;//Math.min(8, outputStack.getCount());
								outputSizeCopy.setCount(transferSize);
	
								ItemStack remainder = InventoryUtils.putStackInInventoryAllSlots(handler, outputSizeCopy);
								int shrinkAmount = transferSize - remainder.getCount();
								if(shrinkAmount > 0) {
									outputStack.shrink(shrinkAmount);
									outputItems.setStackInSlot(i, outputStack.isEmpty() ? ItemStack.EMPTY : outputStack);
									hopperCooldown = 8; //Hopper Speed
									break;
								}
							}
						}
	
					}
				}
			}
		}
		
		
		if(scanDelay > 0) {
			scanDelay--;
			return;
		}
		
		Vec3i scanPosition = findNextPosition();
		BlockPos scanBlockPos = this.worldPosition.offset(scanPosition);
		CropSettings settings = getCropSettings(scanPosition);
		if(settings != null) {
			boolean didWork = false;
			
			if(plantSeeds(scanPosition)) {
				scanDelay = 20 * 1; //1 Second Delay
				didWork = true;
			}
			
			if(!didWork) {
				if(settings.shouldHarvest()) {
					//Handle Harvesting
					BlockPos harvestPos = scanBlockPos;
					BlockState harvestState = this.level.getBlockState(harvestPos);
					if(BlockUtils.isStackingCrop(harvestState)) {
						harvestPos = scanBlockPos.above();
						harvestState = this.level.getBlockState(harvestPos);
					}
					
					if(BlockUtils.canHarvestCrop(level, harvestPos)) {
						float destroySpeed = harvestState.getDestroySpeed(level, harvestPos);
						int baseCostPerBlock = 100;
						int energyCost = baseCostPerBlock;
						energyCost *= (int) ((destroySpeed + 1) * 2);
						
						int fortune = ItemUtils.getEnchantmentLevel(this.upgradeItems.getStackInSlot(0), Enchantments.BLOCK_FORTUNE);						
						if(fortune > 0) {
							energyCost = (int) ((double)energyCost * (2.5D * fortune));
						}
						if(this.energyStorage.getEnergyStored() >= energyCost) {
							ItemStack hoe = getHoe(fortune);
							//TODO Handle Full
							int harvestCount = BlockUtils.harvestCrop(level, harvestPos, hoe, fortune, seedItems, outputItems, (slot) -> {
								setChanged();
							});
							if(harvestCount > 0) {
								this.energyStorage.consumeEnergy(energyCost, false);
								scanDelay = 20; //1 Second Delay
								didWork = true;
								
								//Try and re-plant seeds after harvest
								if(plantSeeds(scanPosition)) {
									scanDelay += 20; //1 Second Delay
								}
							}
						}
					}
				}
			}
			
			if(!didWork) {
				if(bonemealMode != BonemealMode.OFF) {
					BonemealMode growResult = bonemealBlock(scanPosition);
					if(growResult != null) {
						scanDelay = growResult == BonemealMode.ADVANCED ? 3 * 20 : 20;
						didWork = true;
					}
				}
			}
		}
		this.lastScanPosition = scanPosition;
	}
	
	public boolean plantSeeds(Vec3i pos) {
		CropSettings settings = getCropSettings(pos);
		BlockPos offsetBlockPos = this.worldPosition.offset(pos);
		if(settings.shouldPlant() && pos.getY() == 0) {
			ItemStack seed = findSeedForPosition(settings);
			if(!seed.isEmpty()) {
				if(this.energyStorage.getEnergyStored() >= ENERGY_FOR_PLANTING) {
					if(BlockUtils.placeSeed(level, offsetBlockPos, seed)) {
						InventoryUtils.consumeItem(seedItems, seed, 1, false);
						this.energyStorage.consumeEnergy(ENERGY_FOR_PLANTING, false);
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public BonemealMode bonemealBlock(Vec3i pos) {
		CropSettings settings = getCropSettings(pos);
		BlockPos offsetBlockPos = this.worldPosition.offset(pos);
		if(settings.shouldGrow() && bonemealMode != BonemealMode.OFF) {
			//Handle Growing					
			if(canGrowBlock(offsetBlockPos)) {
				if(this.energyStorage.getEnergyStored() >= ENERGY_FOR_BONEMEAL) {
					if(growBlock(offsetBlockPos)) {
						this.energyStorage.consumeEnergy(ENERGY_FOR_BONEMEAL, false);
						return BonemealMode.NORMAL;
					}
				}
			}
			else if(bonemealMode == BonemealMode.ADVANCED) {
				Block block = this.level.getBlockState(offsetBlockPos).getBlock();
				if(!(block instanceof MushroomBlock) && !(block instanceof FungusBlock)) {
					if(this.energyStorage.getEnergyStored() >= ENERGY_FOR_ADVANCED_BONEMEAL) {
						if(BlockUtils.randomTickBlock((ServerLevel)level, offsetBlockPos)) {
							this.energyStorage.consumeEnergy(ENERGY_FOR_ADVANCED_BONEMEAL, false);
							return BonemealMode.ADVANCED;
						}
					}
				}
			}
		}
		return null;
	}
	
	public ItemStack getHoe(int fortune) {
		if(fortune > 0) {
			if(TOOL_FORTUNE == null || TOOL_FORTUNE.isEmpty()) {
				TOOL_FORTUNE = new ItemStack(Items.DIAMOND_AXE);
			}
			TOOL_FORTUNE.enchant(Enchantments.BLOCK_FORTUNE, fortune);
			return TOOL_FORTUNE;
		}
		else {
			if(TOOL_NORMAL == null || TOOL_NORMAL.isEmpty()) {
				TOOL_NORMAL = new ItemStack(Items.DIAMOND_AXE);
			}
			return TOOL_NORMAL;
		}
	}

	public Vec3i findNextPosition() {
		int yHeight = 0;
		int minRange = -(getRange() / 2);
		int maxRange = (getRange() / 2) + 1;
		if(lastScanPosition == null) {
			return new Vec3i(minRange, 0, minRange);
		}
		int nextX = lastScanPosition.getX();
		int nextY = lastScanPosition.getY();
		int nextZ = lastScanPosition.getZ();
		
		nextX += 1;
		if(nextX > maxRange) {
			nextZ +=1;
			nextX = minRange;
		}
		if(nextZ > maxRange) {
			nextY +=1;
			nextX = minRange;
			nextZ = minRange;
		}		
		if(nextY > yHeight) {
			nextX = minRange;
			nextY = 0;
			nextZ = minRange;			
		}

		return new Vec3i(nextX, nextY, nextZ);
	}
	
	public ItemStack findSeedForPosition(CropSettings settings) {
		Vec3i pos = settings.getPos();
		BlockPos blockPos = this.worldPosition.offset(pos);
		if(settings.getSeed().isEmpty()) {
			//Plant any seed
			for(int i = 0; i < seedItems.getSlots(); i++) {
				ItemStack seed = seedItems.getStackInSlot(i);
				if(!seed.isEmpty()) {
					if(BlockUtils.validPlantingLocation(level, blockPos, seed)){
						return seed;
					}
				}
			}
		}
		else {
			int count = InventoryUtils.countItem(seedItems, settings.getSeed());
			if(count > 0) {
				if(BlockUtils.validPlantingLocation(level, blockPos, settings.getSeed())){
					return settings.getSeed();
				}
			}
		}
		return ItemStack.EMPTY;
	}
	
	public boolean canGrowBlock(BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();
		//Prevent large mushrooms
		if(block instanceof MushroomBlock || block instanceof FungusBlock) {
			return false;
		}
		if(block instanceof BonemealableBlock bonemeal) {
			return bonemeal.isValidBonemealTarget(level, pos, state, false);
		}
		return false;
	}	
	
	public boolean growBlock(BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();
		if(block instanceof BonemealableBlock bonemeal) {
			if (bonemeal.isBonemealSuccess(level, level.random, pos, state)) {
				bonemeal.performBonemeal((ServerLevel)level, level.random, pos, state);
				return true;
			}
		}
		return false;
	}
	
	public void buildCropSettings() {
		int yHeight = 0;
		int minRange = - (getRange() / 2);
		int maxRange = (getRange() / 2) + 1;
		this.cropSettings = new HashMap<Vec3i, CropSettings>();
		for(int y = 0; y <= yHeight; y++) {
			for(int x = minRange; x < maxRange; x++) {
				for(int z = minRange; z < maxRange; z++) {
					Vec3i scanPos = new Vec3i(x, y, z);
					cropSettings.put(scanPos, new CropSettings(scanPos));
				}
			}
		}
	}
	
	public void disabledCropSettings(int type) {
		if(type == 0) {
			cropSettings.forEach((pos, setting) -> {
				setting.setShouldPlant(false);
			});
		}
		else if(type == 1) {
			cropSettings.forEach((pos, setting) -> {
				setting.setSeed(ItemStack.EMPTY);
			});
		}
		else if(type == 2) {
			cropSettings.forEach((pos, setting) -> {
				setting.setShouldHarvest(false);
			});
		}
		else if(type == 3) {
			cropSettings.forEach((pos, setting) -> {
				setting.setShouldGrow(false);
			});
		}
		if(!this.level.isClientSide) {
			setChanged();
		}
	}
	
	public void updateCropSetting(Vec3i pos, CropSettings settings) {
		cropSettings.put(pos, settings);
		//WAMUtilsMod.LOGGER.debug("Update Crop: " + pos + " shouldPlant = " + mapSetting.shouldPlant() + " client = " + this.level.isClientSide);
		if(!this.level.isClientSide) {
			setChanged();
			//this.markBlockForUpdate(null);
		}
	}
	
	public CropSettings getCropSettings(Vec3i pos) {
		return cropSettings.get(pos);
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
	
	@Override
	public void receiveMessageFromClient(CompoundTag nbt) {
		if(nbt.contains("CropSettings")) {
			CropSettings settings = CropSettings.loadFromNBT(nbt.getCompound("CropSettings"));
			updateCropSetting(settings.getPos(), settings);
		}
		if(nbt.contains("Slot")) {
			int slot = nbt.getInt("Slot");
			boolean enabled = nbt.getBoolean("Enabled");
			updateSlotLock(slot, enabled);
		}
		if(nbt.contains("Clear")) {
			int type = nbt.getInt("Type");
			disabledCropSettings(type);
		}
		if(nbt.contains("RedstoneMode")) {
			this.redstoneMode = RedstoneMode.getMode(nbt.getInt("RedstoneMode"));
			this.setChanged();
		}
		if(nbt.contains("BonemealMode")) {
			this.bonemealMode = BonemealMode.getMode(nbt.getInt("BonemealMode"));
			this.setChanged();
		}
		if(nbt.contains("PullSeeds")) {
			this.restockSeeds = nbt.getBoolean("RestockSeeds");
			this.pullSeeds = nbt.getBoolean("PullSeeds");
			this.pushOutput = nbt.getBoolean("PushOutput");
			this.setChanged();
		}
	}

	//TODO Create Method that resizes the cropSettingsList (trims out pos out of range or adds new settings)
	//With upgrades (+2, +4, +6, +8)
	public int getRange() {
		int upgrades = Math.min(MAX_RANGE_UPGRADES, rangeUpgradeCount);
		return 8 + (2 * upgrades);
	}

	public boolean isItemAValidUpgradeItem(ItemStack stack, int slot) {
		if (!stack.isEmpty()) {
			if (stack.getItem() == Items.ENCHANTED_BOOK && slot == 0) {
				int fortune = ItemUtils.getEnchantmentLevel(stack, Enchantments.BLOCK_FORTUNE);
				return fortune > 0;
			}
			if(slot == 1) {
				return stack.is(ItemInit.UPGRADE_RANGE.get());
			}
		}
		return false;
	}
	
	public void updateSeeds() {
		
	}
	
	public void updateSlotLock(int slot, boolean enabled) {
		SlotLock lock = this.slotLocks[slot];
		if(lock == null) {
			lock = new SlotLock(enabled);
		}
		lock.setEnabled(enabled);
		ItemStack stack = this.seedItems.getStackInSlot(slot).copy();
		lock.setStack(stack);
		this.slotLocks[slot] = lock;
		if(!this.level.isClientSide) {
			setChanged();
			this.markBlockForUpdate(null);
		}
	}
	
	public SlotLock getSlotLock(int slot) {
		return slotLocks[slot];
	}
	
	public boolean isItemAValidSeedItem(ItemStack stack, int slot) {
		boolean passesLock = true;
		if(this.slotLocks[slot] != null) {
			SlotLock lock = this.slotLocks[slot];
			if(lock.isEnabled()) {
				if(lock.getStack().isEmpty()) {
					passesLock = false;
				}
				else {
					passesLock = InventoryUtils.areItemsEqualIgnoreCount(stack, lock.getStack(), false);
				}
			}
		}
		
		if(!stack.isEmpty()) {
			return BlockUtils.isSeedItem(stack) && passesLock;
		}
		return false;
	}
	
	public List<Item> getAllSeedItems(){
		List<Item> seeds = new ArrayList<Item>();
		for(int i = 0; i < this.seedItems.getSlots(); i++) {
			ItemStack seed = this.seedItems.getStackInSlot(i);
			if(!seed.isEmpty() && BlockUtils.isSeedItem(seed)) {
				if(!seeds.contains(seed.getItem())) {
					seeds.add(seed.getItem());
				}
			}
		}
		for(int i = 0; i < this.outputItems.getSlots(); i++) {
			ItemStack seed = this.outputItems.getStackInSlot(i);
			if(!seed.isEmpty() && BlockUtils.isSeedItem(seed)) {
				if(!seeds.contains(seed.getItem())) {
					seeds.add(seed.getItem());
				}
			}
		}
		for(int i = 0; i < this.slotLocks.length; i++) {
			SlotLock lock = this.slotLocks[i];
			if(lock == null)continue;
			ItemStack seed = lock.getStack();
			if(!seed.isEmpty() && BlockUtils.isSeedItem(seed)) {
				if(!seeds.contains(seed.getItem())) {
					seeds.add(seed.getItem());
				}
			}
		}
		return seeds;
	}
	
	@Nonnull
	private ItemStackHandler createSeedItemHandler() {
		return new ItemStackHandler(INPUT_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				updateSeeds();
				setChanged();
			}

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return isItemAValidSeedItem(stack, slot);
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				if (!isItemAValidSeedItem(stack, slot)) {
					return stack;
				}
				return super.insertItem(slot, stack, simulate);
			}
		};
	}
	
	public void updateUpgrades() {
		this.rangeUpgradeCount = InventoryUtils.countItem(upgradeItems, ItemInit.UPGRADE_RANGE.get());
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

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				return super.insertItem(slot, stack, simulate);
			}
		};
	}
	
	@Nonnull
	private IItemHandler createCombinedAllItemHandler() {
		return new CombinedInvWrapper(seedItems, upgradeItems, outputItems) {
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
		seedItemHandler.invalidate();
		upgradeItemHandler.invalidate();
		outputItemHandler.invalidate();
		combinedAllItemHandler.invalidate();
	}

	@Override
	public void writeCustomNBT(CompoundTag tag, boolean descPacket) {
		if(this.lastScanPosition != null) {
			tag.put("LastScanPosition", BlockUtils.saveVec3i(lastScanPosition));
		}
		tag.putInt("RedstoneMode", redstoneMode.ordinal());
		tag.putInt("BonemealMode", bonemealMode.ordinal());
		
		tag.putInt("ScanDelay", scanDelay);
		tag.putInt("HopperDelay", hopperCooldown);
		tag.putBoolean("PullInSeeds", pullSeeds);
		tag.putBoolean("PushOutputsOut", pushOutput);
		
		tag.putInt("RangeUpgrades", rangeUpgradeCount);
		if(!descPacket) {
			tag.put("Inventory.Seeds", seedItems.serializeNBT());
			tag.put("Inventory.Upgrades", upgradeItems.serializeNBT());
			tag.put("Inventory.Output", outputItems.serializeNBT());
		}
        energy.ifPresent(h -> tag.put("Energy", h.serializeNBT()));
        
        CompoundTag slotLockTag = new CompoundTag();
        for(int i = 0; i < INPUT_SLOTS; i++) {
        	if(this.slotLocks[i] !=null) {
        		slotLockTag.put("SlotLock" + i, slotLocks[i].serializeNBT());
        	}
        }
        tag.put("SlotLocks", slotLockTag);
        
        ListTag cropSettingsTag = new ListTag();
        for(CropSettings settings : this.cropSettings.values()) {
        	cropSettingsTag.add(settings.serializeNBT());
        }
        tag.put("CropSettings", cropSettingsTag);
	}

	@Override
	public void readCustomNBT(CompoundTag tag, boolean descPacket) {
		if(tag.contains("LastScanPosition")) {
			this.lastScanPosition = BlockUtils.loadVec3i(tag, "LastScanPosition");
		}
		
		redstoneMode = RedstoneMode.getMode(tag.getInt("RedstoneMode"));
		bonemealMode = BonemealMode.getMode(tag.getInt("BonemealMode"));
		scanDelay = tag.getInt("ScanDelay");
		hopperCooldown = tag.getInt("HopperDelay");
		pullSeeds = tag.getBoolean("PullInSeeds");
		pushOutput = tag.getBoolean("PushOutputsOut");
		
		rangeUpgradeCount = tag.getInt("RangeUpgrades");
		
		if(!descPacket) {
			if (tag.contains("Inventory.Seeds")) {
				seedItems.deserializeNBT(tag.getCompound("Inventory.Seeds"));
			}
			if (tag.contains("Inventory.Upgrades")) {
				upgradeItems.deserializeNBT(tag.getCompound("Inventory.Upgrades"));
			}
			if (tag.contains("Inventory.Output")) {
				outputItems.deserializeNBT(tag.getCompound("Inventory.Output"));
			}
		}
		energy.ifPresent(h -> h.deserializeNBT(tag.getCompound("Energy")));
		
		this.slotLocks = new SlotLock[INPUT_SLOTS];
		if(tag.contains("SlotLocks")) {
			CompoundTag slotLockTag = tag.getCompound("SlotLocks");
	        for(int i = 0; i < INPUT_SLOTS; i++) {	        	
	        	if(slotLockTag.contains("SlotLock" + i)) {
	        		CompoundTag slotLock = slotLockTag.getCompound("SlotLock" + i);
	        		this.slotLocks[i] = SlotLock.loadFromNBT(slotLock);
	        	}
	        }
		}
		
		//this.cropSettings = null;
		//this.buildCropSettings();
		if(tag.contains("CropSettings")) {
			ListTag cropSettingsTags = tag.getList("CropSettings", 10);
			for(int i = 0; i < cropSettingsTags.size(); i++) {
				CompoundTag cropSettingTag = cropSettingsTags.getCompound(i);
				CropSettings settings = CropSettings.loadFromNBT(cropSettingTag);
				//WAMUtilsMod.LOGGER.debug("Load Crop: " + settings.getPos() + " " + settings.shouldPlant + " " + this.level.isClientSide);				
				this.cropSettings.put(settings.getPos(), settings);
			}
		}	
//		if(descPacket)
//			this.markBlockForUpdate(null);
	}
	
	@NotNull
	@Override
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if(cap == ForgeCapabilities.ITEM_HANDLER) {
			if(side == null) {
				return combinedAllItemHandler.cast();
			}
			else if(side == Direction.UP) {
				return seedItemHandler.cast();
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
		return 250000;
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

