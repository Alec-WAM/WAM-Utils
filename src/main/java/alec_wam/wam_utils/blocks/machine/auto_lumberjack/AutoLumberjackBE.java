package alec_wam.wam_utils.blocks.machine.auto_lumberjack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.blocks.machine.BonemealMode;
import alec_wam.wam_utils.blocks.machine.SlotLock;
import alec_wam.wam_utils.capabilities.BlockEnergyStorage;
import alec_wam.wam_utils.capabilities.IEnergyStorageBlockEntity;
import alec_wam.wam_utils.entities.WAMUtilsFakePlayer;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.utils.BlockUtils;
import alec_wam.wam_utils.utils.BlockUtils.HarvestBlockData;
import alec_wam.wam_utils.utils.InventoryUtils;
import alec_wam.wam_utils.utils.ItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public class AutoLumberjackBE extends WAMUtilsBlockEntity implements IEnergyStorageBlockEntity {

	public static final int INPUT_SLOTS = 1;
	public static final int UPGRADE_SLOTS = 1;
	public static final int OUTPUT_SLOTS = 18;
	
	private static ItemStack TOOL_NORMAL;
    private static ItemStack TOOL_SILK;
    private static ItemStack TOOL_FORTUNE;
	
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
	private boolean isFull;
	private int scanDelay;
	private int harvestDelay;
	private Queue<HarvestBlockData> harvestBlockQueue;
	
	private int hopperCooldown;
	
	public static enum LumberjackSetting {
		MEGA_TREE(false), RESTOCK_SEEDS(true), PULL_SEEDS(true), PUSH_OUTPUT(true), PLAY_BLOCK_SOUNDS(true), SHOW_PARTICLES(true);
		
		private final boolean defaultValue;
		
		LumberjackSetting(boolean defaultValue){
			this.defaultValue = defaultValue;
		}
		
		public boolean getDefaultValue() {
			return this.defaultValue;
		}
		
		public static LumberjackSetting byName(String name) {
			for(LumberjackSetting setting : values()) {
				if(setting.name().equalsIgnoreCase(name)) {
					return setting;
				}
			}
			return null;
		}
	}	
	public Map<LumberjackSetting, Boolean> settings = new HashMap<LumberjackSetting, Boolean>();
	
	private SlotLock[] slotLocks = new SlotLock[INPUT_SLOTS];
	
	public AutoLumberjackBE(BlockPos pos, BlockState state) {
		super(BlockInit.AUTO_LUMBERJACK_BE.get(), pos, state);
		
		this.energyStorage = new BlockEnergyStorage(this, 0);
        this.energy = LazyOptional.of(() -> this.energyStorage);
	}

	public boolean getSetting(LumberjackSetting setting) {
		return this.settings.getOrDefault(setting, Boolean.valueOf(setting.getDefaultValue()));
	}
	
	public void updateSetting(LumberjackSetting setting, boolean value) {
		this.settings.put(setting, Boolean.valueOf(value));
		if(!this.level.isClientSide) {
			setChanged();
		}
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
		
		if(!redstoneMode.isMet(level, worldPosition)) {
			return;
		}
		
		if(hopperCooldown > 0) {
			hopperCooldown --;
		}
		else {			
			boolean didWork = false;

			if(getSetting(LumberjackSetting.RESTOCK_SEEDS)) {
				for(int i = 0; i < outputItems.getSlots(); i++) {
					ItemStack inputStack = outputItems.getStackInSlot(i);
					if(inputStack.isEmpty() || !BlockUtils.isSaplingItem(inputStack))continue;
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
			
			BlockEntity blockEntity = this.level.getBlockEntity(worldPosition.above());
			if(blockEntity !=null) {
				IItemHandler handler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElse(null);
				if(handler !=null) {	
					//Pull in seeds
					if(!didWork && getSetting(LumberjackSetting.PULL_SEEDS)) {
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

					if(!didWork && getSetting(LumberjackSetting.PUSH_OUTPUT)) {
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
		
		if(scanDelay > 0) {
			scanDelay--;
			return;
		}
		
		Direction dir = level.getBlockState(worldPosition).getValue(AutoLumberjackBlock.FACING).getOpposite();
		BlockPos scanBlockPos = this.worldPosition.relative(dir);		
		
		//Only do MegaTree style planting/harvesting/growing if we are actually planting a mega tree
		boolean megaTree = false;		
		if(getSetting(LumberjackSetting.MEGA_TREE)) {
			ItemStack sapling = this.seedItems.getStackInSlot(0);
			if(sapling.isEmpty()) {
				SlotLock lock = this.slotLocks[0];
				if(lock !=null && lock.isEnabled()) {
					sapling = lock.getStack();
				}
			}
			
			if(!sapling.isEmpty()) {
				megaTree = BlockUtils.isMegaSapling(sapling.getItem());
			}
		}
		
		if(megaTree) {
			scanBlockPos = this.worldPosition.relative(dir, 3);
		}
		
		boolean doneHarvesting = harvestBlockQueue == null || harvestBlockQueue.isEmpty();
		
		int energyCostPerPlant = 80;
		if(doneHarvesting) {
			//Handle Planting
			ItemStack seed = this.seedItems.getStackInSlot(0);
			BlockPos plantPositionBL = scanBlockPos;
			BlockPos plantPositionTL = scanBlockPos.relative(dir);
			BlockPos plantPositionTR = scanBlockPos.relative(dir).relative(dir.getClockWise());
			BlockPos plantPositionBR = scanBlockPos.relative(dir.getClockWise());

			if(!seed.isEmpty()) {
				if(this.energyStorage.getEnergyStored() >= energyCostPerPlant) {
					if(megaTree) {
						if(BlockUtils.placeSeed(level, plantPositionBL, seed) 
								|| BlockUtils.placeSeed(level, plantPositionTL, seed)
								|| BlockUtils.placeSeed(level, plantPositionTR, seed)
								|| BlockUtils.placeSeed(level, plantPositionBR, seed)) {
							InventoryUtils.consumeItem(seedItems, seed, 1, false);
							this.energyStorage.consumeEnergy(energyCostPerPlant, false);
							scanDelay = 20 * 1; //1 Second Delay
							return;
						}
					}
					else {
						if(BlockUtils.placeSeed(level, scanBlockPos, seed)) {
							InventoryUtils.consumeItem(seedItems, seed, 1, false);
							this.energyStorage.consumeEnergy(energyCostPerPlant, false);
							scanDelay = 20 * 1; //1 Second Delay
							return;
						}
					}
				}
			}

			int energyCostPerBonemeal = 500;
			int energyCostPerAdvancedBonemeal = 1000;
			boolean shouldGrow = bonemealMode != BonemealMode.OFF;
			if(shouldGrow) {
				BlockState saplingState = this.level.getBlockState(scanBlockPos);
				Block block = saplingState.getBlock();
				boolean canBonemeal = false;
				if(block instanceof BonemealableBlock bonemeal) {
					if(megaTree && BlockUtils.isMegaSapling(block)) {
						BlockState stateBL = this.level.getBlockState(plantPositionBL);
						BlockState stateTL = this.level.getBlockState(plantPositionTL);
						BlockState stateTR = this.level.getBlockState(plantPositionTR);
						BlockState stateBR = this.level.getBlockState(plantPositionBR);
						Block targetBlock = stateBL.getBlock();
						canBonemeal = stateTL.is(targetBlock) && stateTR.is(targetBlock) && stateBR.is(targetBlock);
					}					
					else {
						canBonemeal = bonemeal.isValidBonemealTarget(level, scanBlockPos, saplingState, false);
					}
				}

				if(canBonemeal || bonemealMode == BonemealMode.ADVANCED) {
					if(block instanceof BonemealableBlock bonemeal) {
						if(this.energyStorage.getEnergyStored() >= energyCostPerBonemeal) {
							if (bonemeal.isBonemealSuccess(level, level.random, scanBlockPos, saplingState)) {
								bonemeal.performBonemeal((ServerLevel)level, level.random, scanBlockPos, saplingState);
								if(getSetting(LumberjackSetting.PLAY_BLOCK_SOUNDS)) {
									level.levelEvent(1505, scanBlockPos, 0);
								}
								else {
									double d0 = 0.5D;
									double d1 = 1.0D;
									for(int i = 0; i < 15; ++i) {
										double d2 = random.nextGaussian() * 0.02D;
										double d3 = random.nextGaussian() * 0.02D;
										double d4 = random.nextGaussian() * 0.02D;
										double d5 = 0.5D;
										double d6 = (double)scanBlockPos.getX() + 0.5D - d5 + random.nextDouble() * d0 * 2.0D;
										double d7 = (double)scanBlockPos.getY() + random.nextDouble() * d1;
										double d8 = (double)scanBlockPos.getZ() + 0.5D - d5 + random.nextDouble() * d0 * 2.0D;
										((ServerLevel)level).sendParticles(ParticleTypes.HAPPY_VILLAGER, d6, d7, d8, 0, d2, d3, d4, 1.0D);
									}
								}
								this.energyStorage.consumeEnergy(energyCostPerBonemeal, false);
								scanDelay = 20 * 5; //5 Second Delay
								return;
							}
						}
					}
					else if(bonemealMode == BonemealMode.ADVANCED) {
						if(this.energyStorage.getEnergyStored() >= energyCostPerAdvancedBonemeal) {
							BlockPos growPos = scanBlockPos;
							if(block == Blocks.CHORUS_PLANT) {
								List<BlockPos> flowers = Lists.newArrayList();
								BlockUtils.findAllChorusFlowers(level, scanBlockPos, Lists.newArrayList(), flowers, true);
	
								if(!flowers.isEmpty()) {
									BlockPos randomPos = flowers.get(random.nextInt(0, flowers.size()));
									growPos = randomPos;
								}
							}
							if(BlockUtils.randomTickBlock((ServerLevel)level, growPos)) {
								double d0 = 0.5D;
								double d1 = 1.0D;
								for(int i = 0; i < 15; ++i) {
									double d2 = random.nextGaussian() * 0.02D;
									double d3 = random.nextGaussian() * 0.02D;
									double d4 = random.nextGaussian() * 0.02D;
									double d5 = 0.5D;
									double d6 = (double)growPos.getX() + 0.5D - d5 + random.nextDouble() * d0 * 2.0D;
									double d7 = (double)growPos.getY() + random.nextDouble() * d1;
									double d8 = (double)growPos.getZ() + 0.5D - d5 + random.nextDouble() * d0 * 2.0D;
									((ServerLevel)level).sendParticles(ParticleTypes.HAPPY_VILLAGER, d6, d7, d8, 0, d2, d3, d4, 1.0D);
								}
								this.energyStorage.consumeEnergy(energyCostPerAdvancedBonemeal, false);
								scanDelay = 20 * 8; //8 Second Delay for advanced
								return;
							}
						}
					}
				}
			}
		
			if(this.isFull)return;
		
			//Handle Harvesting
			BlockPos harvestPos = scanBlockPos;
			BlockState harvestState = this.level.getBlockState(harvestPos);
			
			//Handle nether "logs" first
			if(harvestState.getMaterial() == Material.NETHER_WOOD) {
				List<HarvestBlockData> harvestBlocks = new ArrayList<HarvestBlockData>();				
				BlockUtils.buildHarvestMap(level, harvestPos, 
				(blockstate) -> {
					return BlockUtils.isLog(blockstate) || blockstate.getBlock() == Blocks.WARPED_WART_BLOCK || blockstate.getBlock() == Blocks.NETHER_WART_BLOCK || blockstate.getBlock() == Blocks.SHROOMLIGHT;
				}
				, Lists.newArrayList(), harvestBlocks);
				
				final BlockPos origin = harvestPos;				
				List<HarvestBlockData> list = new ArrayList<HarvestBlockData>(harvestBlocks);
				list.sort(new BasicTreeComparator(origin));
				this.harvestBlockQueue = new LinkedList<HarvestBlockData>(list);
			}
			else if(BlockUtils.isLog(harvestState)) {
				List<HarvestBlockData> harvestBlocks = new ArrayList<HarvestBlockData>();
				
				BlockUtils.buildHarvestMap(level, harvestPos, 
				(blockstate) -> {
					return BlockUtils.isLog(blockstate) || BlockUtils.isLeaf(blockstate) || blockstate.is(Blocks.BEE_NEST);
				}
				, Lists.newArrayList(), harvestBlocks);
				
				final BlockPos origin = harvestPos;				
				List<HarvestBlockData> list = new ArrayList<HarvestBlockData>(harvestBlocks);
				list.sort(new BasicTreeComparator(origin));
				this.harvestBlockQueue = new LinkedList<HarvestBlockData>(list);
			}			
			else if(harvestState.getBlock() instanceof HugeMushroomBlock) {
				List<HarvestBlockData> harvestBlocks = new ArrayList<HarvestBlockData>();				
				BlockUtils.buildHarvestMap(level, harvestPos, 
				(blockstate) -> {
					return blockstate.getBlock() instanceof HugeMushroomBlock;
				}
				, Lists.newArrayList(), harvestBlocks);
				
				final BlockPos origin = harvestPos;				
				List<HarvestBlockData> list = new ArrayList<HarvestBlockData>(harvestBlocks);
				list.sort(new BasicTreeComparator(origin));
				this.harvestBlockQueue = new LinkedList<HarvestBlockData>(list);
			}
			else if(harvestState.getBlock() == Blocks.CHORUS_PLANT) {
				if(BlockUtils.allChorusFlowersGrown(level, scanBlockPos, Lists.newArrayList())) {
					List<HarvestBlockData> harvestBlocks = new ArrayList<HarvestBlockData>();				
					BlockUtils.buildHarvestMap(level, harvestPos, 
					(blockstate) -> {
						return blockstate.getBlock() == Blocks.CHORUS_PLANT || blockstate.getBlock() == Blocks.CHORUS_FLOWER;
					}
					, Lists.newArrayList(), harvestBlocks);
					
					final BlockPos origin = harvestPos;				
					List<HarvestBlockData> list = new ArrayList<HarvestBlockData>(harvestBlocks);
					//list.sort(new BasicTreeComparator(origin));
					
					Comparator<HarvestBlockData> comparator = new Comparator<HarvestBlockData>() {
	
						@Override
						public int compare(HarvestBlockData o1, HarvestBlockData o2) {
							boolean isFlower1 = o1.getBlockState().getBlock() == Blocks.CHORUS_FLOWER;
							boolean isFlower2 = o2.getBlockState().getBlock() == Blocks.CHORUS_FLOWER;
							return Boolean.compare(isFlower2, isFlower1);
						}
						
					}.thenComparing(new BasicTreeComparator(origin));
				    list.sort(comparator);
					
					this.harvestBlockQueue = new LinkedList<HarvestBlockData>(list);
				}
				else {
					scanDelay = 20 * 10;
				}
			}
			else if(harvestState.getBlock() == Blocks.BAMBOO) {
				int goalHeight = 12;
				int bambooHeight = getPillarHeight(harvestPos, goalHeight, Blocks.BAMBOO);				
				if(bambooHeight >= goalHeight) {
					List<HarvestBlockData> harvestBlocks = new ArrayList<HarvestBlockData>();
					
					BlockUtils.buildHarvestMap(level, harvestPos, 
					(blockstate) -> {
						return blockstate.getBlock() == Blocks.BAMBOO;
					}
					, Lists.newArrayList(), harvestBlocks);
					
					final BlockPos origin = harvestPos;				
					List<HarvestBlockData> list = new ArrayList<HarvestBlockData>(harvestBlocks);
					list.sort(new BasicTreeComparator(origin));
					this.harvestBlockQueue = new LinkedList<HarvestBlockData>(list);
				}
				else {
					scanDelay = 20 * 10;
				}
			}		
		}
		else {
		
			if(this.isFull)return;
			
			//TODO Figure out instant mine for chorus plant stems
			if(harvestDelay > 0) {
				harvestDelay--;
			}
			else {
				HarvestBlockData harvestData = this.harvestBlockQueue.peek(); 
				if(harvestData !=null) {	
					BlockPos harvestPos = harvestData.getPos();
					BlockState savedHarvestState = harvestData.getBlockState();
					BlockState state = this.level.getBlockState(harvestPos);

					if(savedHarvestState.getBlock() == state.getBlock()) {	
						float destroySpeed = state.getDestroySpeed(level, harvestPos);
						boolean isCheapBlock = destroySpeed <= 0.8F; //Diamond Axe instant mine
						WAMUtilsFakePlayer fakePlayer = WAMUtilsFakePlayer.get((ServerLevel)level, harvestPos).get();
						ItemStack enchantedBook = this.upgradeItems.getStackInSlot(0);
						boolean silk = ItemUtils.getEnchantmentLevel(enchantedBook, Enchantments.SILK_TOUCH) > 0;
						int fortune = ItemUtils.getEnchantmentLevel(enchantedBook, Enchantments.BLOCK_FORTUNE);
						int efficiency = ItemUtils.getEnchantmentLevel(enchantedBook, Enchantments.BLOCK_EFFICIENCY);
						
						if(BlockUtils.allowedToBreak(level, state, harvestPos, fakePlayer)) {
							int baseCostPerBlock = 100;
							int energyCost = baseCostPerBlock;
							energyCost *= (int) ((destroySpeed + 1) * 2);
							
							if(silk) {
								energyCost *= 3;
							}
							else if(fortune > 0) {
								energyCost = (int) ((double)energyCost * (2.5D * fortune));
							}
							if(efficiency > 0) {
								energyCost = (int) ((double)energyCost * (1.5D * efficiency));
							}
							
							if(this.energyStorage.getEnergyStored() >= energyCost) {
								ItemStack axe = getAxe(silk, fortune);
								fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, axe);	
								
								handleSpecialBlockBreak(harvestPos, savedHarvestState, axe);
								
								List<ItemStack> drops = BlockUtils.breakBlock(level, harvestPos, fakePlayer, axe, fortune, getSetting(LumberjackSetting.PLAY_BLOCK_SOUNDS), getSetting(LumberjackSetting.SHOW_PARTICLES));            
								for(ItemStack drop : drops) {
									ItemStack insertStack = drop.copy();
									ItemStack remainder = insertStack;
									remainder = InventoryUtils.forceStackInInventoryAllSlots(seedItems, remainder, (slot) -> {
										updateSeeds();
										setChanged();
									});
	
									if(!remainder.isEmpty()) {
										remainder = InventoryUtils.forceStackInInventoryAllSlots(outputItems, remainder, (slot) -> {
											updateOutputs();
											setChanged();
										});
									}
								}
								this.harvestBlockQueue.poll();
								
								
								this.energyStorage.consumeEnergy(energyCost, false);
								int speed = Math.min(5, efficiency);					            
								int delay = (int)(destroySpeed) * 5;
								this.harvestDelay = isCheapBlock ? 0 : Math.max(0, delay - (speed));
								fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);	
							}
						}	
						else {
							//Skip the unbreakable block
							this.harvestBlockQueue.poll();
						}
					}
					else {
						//Skip blocks that have changed
						this.harvestBlockQueue.poll();
					}
				}
			}
		}
	}

	private void handleSpecialBlockBreak(BlockPos harvestPos, BlockState savedHarvestState, ItemStack axe) {
		if(savedHarvestState.is(Blocks.BEE_NEST)) {
			BlockEntity be = level.getBlockEntity(harvestPos);
			if (be !=null && be instanceof BeehiveBlockEntity beehiveblockentity) {
				if (ItemUtils.getEnchantmentLevel(axe, Enchantments.SILK_TOUCH) == 0) {
					beehiveblockentity.emptyAllLivingFromHive(null, savedHarvestState, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
					level.updateNeighbourForOutputSignal(harvestPos, savedHarvestState.getBlock());
					
					//TODO Make this a config option
					//EntityUtils.angerNearbyMobs(level, harvestPos, Bee.class);
				}
			}
		}
	}

	public ItemStack getAxe(boolean silk, int fortune) {
		if(silk) {
			if(TOOL_SILK == null || TOOL_SILK.isEmpty()) {
				TOOL_SILK = new ItemStack(Items.DIAMOND_AXE);
				TOOL_SILK.enchant(Enchantments.SILK_TOUCH, 1);
			}
			return TOOL_SILK;
		}
		else if(fortune > 0) {
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
	
	public int getPillarHeight(BlockPos startPos, int goalHeight, Block block) {
		int height = 0;
		BlockPos testPos = startPos;
		while(height < goalHeight && this.level.getBlockState(testPos).getBlock() == block) {
			height++;
			testPos = testPos.above();
		}
		return height;
	}
	
	public static class BasicTreeComparator implements Comparator<HarvestBlockData> {					
		
		private final BlockPos origin;
		
		public BasicTreeComparator(@Nullable BlockPos origin) {
			this.origin = origin;
		}	
		
		@Override
		public int compare(HarvestBlockData o1, HarvestBlockData o2) {
			BlockPos pos2 = o2.getPos();
			BlockPos pos1 = o1.getPos();
			if(pos2.getY() == pos1.getY() && origin != null) {
				Vec3i offsetOrigin = origin.atY(pos2.getY());
				return pos2.distManhattan(offsetOrigin) - pos1.distManhattan(offsetOrigin);
			}
			return pos2.getY() - pos1.getY();
		}
		
	};
	
	public void setRedstoneMode(RedstoneMode redstoneMode) {
		this.redstoneMode = redstoneMode;
		if(!this.level.isClientSide) {
			this.setChanged();
		}
	}
	
	public RedstoneMode getRedstoneMode() {
		return redstoneMode;
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
	
	@Override
	public void receiveMessageFromClient(CompoundTag nbt) {
		if(nbt.contains("Slot")) {
			int slot = nbt.getInt("Slot");
			boolean enabled = nbt.getBoolean("Enabled");
			updateSlotLock(slot, enabled);
		}
		if(nbt.contains("RedstoneMode")) {
			this.redstoneMode = RedstoneMode.getMode(nbt.getInt("RedstoneMode"));
			this.setChanged();
		}
		if(nbt.contains("BonemealMode")) {
			this.bonemealMode = BonemealMode.getMode(nbt.getInt("BonemealMode"));
			this.setChanged();
		}
		if(nbt.contains("Setting")) {
			int settingIndex = nbt.getInt("Setting");
			LumberjackSetting[] values = LumberjackSetting.values();
			if(settingIndex < values.length) {
				LumberjackSetting setting = values[settingIndex];
				updateSetting(setting, nbt.getBoolean("Value"));
			}
		}
	}

	public boolean isItemAValidUpgradeItem(ItemStack stack) {
		if (!stack.isEmpty()) {
			if (stack.getItem() == Items.ENCHANTED_BOOK) {
				int silk = ItemUtils.getEnchantmentLevel(stack, Enchantments.SILK_TOUCH);
				int fortune = ItemUtils.getEnchantmentLevel(stack, Enchantments.BLOCK_FORTUNE);
				int eff = ItemUtils.getEnchantmentLevel(stack, Enchantments.BLOCK_EFFICIENCY);
				return silk > 0 || fortune > 0 || eff > 0;
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
			return BlockUtils.isSaplingItem(stack) && passesLock;
		}
		return false;
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
	
	public void updateOutputs() {
		isFull = InventoryUtils.isFull(outputItems);
	}
	
	@Nonnull
	private ItemStackHandler createOutputItemHandler() {
		return new ItemStackHandler(OUTPUT_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				updateOutputs();
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
		tag.putInt("RedstoneMode", redstoneMode.ordinal());
		tag.putInt("BonemealMode", bonemealMode.ordinal());
		
		tag.putInt("ScanDelay", scanDelay);
		tag.putInt("HopperDelay", hopperCooldown);
		tag.putBoolean("isFull", isFull);
		
		CompoundTag settingsTag = new CompoundTag();
		for(LumberjackSetting setting : LumberjackSetting.values()) {
			settingsTag.putBoolean(setting.name().toLowerCase(), getSetting(setting));
		}
		tag.put("Settings", settingsTag);
		
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
	}

	@Override
	public void readCustomNBT(CompoundTag tag, boolean descPacket) {
		redstoneMode = RedstoneMode.getMode(tag.getInt("RedstoneMode"));
		bonemealMode = BonemealMode.getMode(tag.getInt("BonemealMode"));
		scanDelay = tag.getInt("ScanDelay");
		hopperCooldown = tag.getInt("HopperDelay");
		isFull = tag.getBoolean("isFull");
		
		if(tag.contains("Settings")) {
			CompoundTag settingsTag = tag.getCompound("Settings");
			this.settings.clear();
			for(LumberjackSetting setting : LumberjackSetting.values()) {
				boolean value = settingsTag.contains(setting.name().toLowerCase()) ? settingsTag.getBoolean(setting.name().toLowerCase()) : Boolean.valueOf(setting.getDefaultValue());
				this.settings.put(setting, value);
			}
		}
		
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
		return 500000;
	}

	@Override
	public int getMaxEnergyInput() {
		int base = 200;
		ItemStack enchantedBook = this.upgradeItems.getStackInSlot(0);
		boolean silk = ItemUtils.getEnchantmentLevel(enchantedBook, Enchantments.SILK_TOUCH) > 0;
		int fortune = ItemUtils.getEnchantmentLevel(enchantedBook, Enchantments.BLOCK_FORTUNE);
		int efficiency = ItemUtils.getEnchantmentLevel(enchantedBook, Enchantments.BLOCK_EFFICIENCY);
		if(silk) {
			base *= 3;
		}
		else if(fortune > 0) {
			base = (int) ((double)base * (2.5D * fortune));
		}
		if(efficiency > 0) {
			base = (int) ((double)base * (1.5D * efficiency));
		}
		return base;
	}

	@Override
	public int getMaxEnergyOutput() {
		return 0;
	}

}

