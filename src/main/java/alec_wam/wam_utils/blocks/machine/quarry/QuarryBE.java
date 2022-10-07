package alec_wam.wam_utils.blocks.machine.quarry;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.capabilities.BlockEnergyStorage;
import alec_wam.wam_utils.capabilities.IEnergyStorageBlockEntity;
import alec_wam.wam_utils.entities.WAMUtilsFakePlayer;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ItemInit;
import alec_wam.wam_utils.utils.BlockUtils;
import alec_wam.wam_utils.utils.InventoryUtils;
import alec_wam.wam_utils.utils.ItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public class QuarryBE extends WAMUtilsBlockEntity implements IEnergyStorageBlockEntity {

	public static final int UPGRADE_SLOTS = 2;
	public static final int OUTPUT_SLOTS = 27;
	public static final int MAX_RANGE_UPGRADES = 4;
	
	private static ItemStack TOOL_NORMAL;
    private static ItemStack TOOL_SILK;
    private static ItemStack TOOL_FORTUNE;
	
	protected final ItemStackHandler upgradeItems = createUpgradeItemHandler();
	public final LazyOptional<IItemHandler> upgradeItemHandler = LazyOptional.of(() -> upgradeItems);
	protected final ItemStackHandler outputItems = createOutputItemHandler();
	public final LazyOptional<IItemHandler> outputItemHandler = LazyOptional.of(() -> outputItems);
	public final LazyOptional<IItemHandler> combinedAllItemHandler = LazyOptional.of(this::createCombinedAllItemHandler);
	
	public BlockEnergyStorage energyStorage;
    private LazyOptional<BlockEnergyStorage> energy;
	
	protected final RandomSource random = RandomSource.create();

	private RedstoneMode redstoneMode = RedstoneMode.ON;
	private boolean running = false;	
	private int scanDelay;
	private Vec3i lastScanPosition;
	private boolean doneMining;
	private int rangeUpgradeCount = 0;
	private boolean isFull;
	
	private int hopperCooldown;
	private boolean pushOutput = true;
	private List<VoidFilter> voidFilters = Lists.newArrayList();
	
	public static class VoidFilter implements INBTSerializable<CompoundTag> {
		private ResourceLocation tagFilter;
		private Item itemFilter;
		private List<Item> tagItemsCache;
		
		public VoidFilter(CompoundTag nbt) {
			this.deserializeNBT(nbt);
		}
		
		public VoidFilter(Item item) {
			this.itemFilter = item;
		}
		
		public VoidFilter(ResourceLocation tag) {
			this.tagFilter = tag;
		}
		
		public Item getItem() {
			return itemFilter;
		}
		
		public ResourceLocation getTag() {
			return tagFilter;
		}
		
		@SuppressWarnings("deprecation")
		public boolean matchesFilter(Item item) {
			if(this.tagFilter !=null) {
				return item.builtInRegistryHolder().tags().anyMatch(tag -> tag.location().toString().equalsIgnoreCase(this.tagFilter.toString()));
			}
			if(this.itemFilter !=null) {
				return this.itemFilter.equals(item);
			}
			return false;
		}

		@SuppressWarnings("deprecation")
		@Override
		public CompoundTag serializeNBT() {
			CompoundTag tag = new CompoundTag();
			if(this.itemFilter !=null) {
				ResourceLocation resourcelocation = Registry.ITEM.getKey(this.itemFilter);
				tag.putString("Item", resourcelocation.toString());
			}
			else {
				tag.putString("Tag", tagFilter.toString());
			}
			return tag;
		}

		@SuppressWarnings("deprecation")
		@Override
		public void deserializeNBT(CompoundTag nbt) {
			if(nbt.contains("Item")) {
				ResourceLocation itemKey = new ResourceLocation(nbt.getString("Item"));
				this.itemFilter = Registry.ITEM.get(itemKey);
			}
			else if(nbt.contains("Tag")) {
				this.tagFilter = new ResourceLocation(nbt.getString("Tag"));
			}
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj == null || !(obj instanceof VoidFilter))return false;
			VoidFilter filter = (VoidFilter)obj;
			
			if(this.itemFilter == null && filter.getItem() !=null) {
				return false;
			}
			if(this.tagFilter == null && filter.getTag() !=null) {
				return false;
			}
			
			if(this.itemFilter !=null) {
				if(filter.getItem() == null || !this.itemFilter.equals(filter.getItem())) {
					return false;
				}
			}
			if(this.tagFilter !=null) {
				if(filter.getTag() == null || !this.tagFilter.equals(filter.getTag())) {
					return false;
				}
			}
			return true;
		}

		public List<Item> getTagItems() {
			if(tagItemsCache == null) {
				this.tagItemsCache = ItemUtils.getTagItems(tagFilter);
			}
			return this.tagItemsCache;
		}
	}
	
	public QuarryBE(BlockPos pos, BlockState state) {
		super(BlockInit.QUARRY_BE.get(), pos, state);
		
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

	public boolean shouldPushOutput() {
		return pushOutput;
	}

	public void setPushOutput(boolean pushOutput) {
		this.pushOutput = pushOutput;
		if(!this.level.isClientSide) {
			setChanged();
		}
	}

	public List<VoidFilter> getVoidFilters(){
		return this.voidFilters;
	}
	
	public void addVoidFilter(VoidFilter filter) {
		this.voidFilters.add(filter);
		if(!this.level.isClientSide) {
			setChanged();
		}
	}
	
	public boolean removeVoidFilter(VoidFilter filter) {
		boolean removed = this.voidFilters.remove(filter);
		if(removed && !this.level.isClientSide) {
			setChanged();
		}
		return removed;
	}

	public void setDoneMining(boolean value) {
		this.doneMining = value;
	}
	
	public boolean isDoneMining() {
		return doneMining;
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
			BlockEntity blockEntity = this.level.getBlockEntity(worldPosition.above());
			if(blockEntity !=null) {
				IItemHandler handler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElse(null);
				if(handler !=null) {	
					if(pushOutput) {
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
								//hopperCooldown = 8; //Hopper Speed
								hopperCooldown = 0;
								break;
							}
						}
					}

				}
			}
		}
		
		if(this.isFull)return;
		
		if(scanDelay > 0) {
			scanDelay--;
			return;
		}
		
		if(doneMining) {
			//WAMUtilsMod.LOGGER.debug("Done Mining");
			return;
		}
		
		Vec3i nextPos = this.findNextPosition();
		//WAMUtilsMod.LOGGER.debug("Next Pos = " + nextPos);
		if(nextPos == null) {
			//WAMUtilsMod.LOGGER.debug("Done Mining");
			doneMining = true;
			setChanged();
			return; //We are done mining
		}
		
		Direction dir = this.getBlockState().getValue(QuarryBlock.FACING).getOpposite();
		BlockPos startPos = this.worldPosition.relative(dir).below();
		BlockPos scanBlockPos = startPos.offset(nextPos);
		//WAMUtilsMod.LOGGER.debug("Scan Pos = " + scanBlockPos);
		
		if (!level.isLoaded(scanBlockPos)) {
			return;
		}
		
		//Handle Mining
		final BlockPos minePos = scanBlockPos;
		final BlockState mineState = this.level.getBlockState(minePos);
		ItemStack enchantedBook =  this.upgradeItems.getStackInSlot(0);
		boolean silk = ItemUtils.getEnchantmentLevel(enchantedBook, Enchantments.SILK_TOUCH) > 0;
		int fortune = ItemUtils.getEnchantmentLevel(enchantedBook, Enchantments.BLOCK_FORTUNE);
		int efficiency = ItemUtils.getEnchantmentLevel(enchantedBook, Enchantments.BLOCK_EFFICIENCY);
		WAMUtilsFakePlayer fakePlayer = WAMUtilsFakePlayer.get((ServerLevel)level, minePos).get();
		
		ItemStack pickaxe = getPick(silk, fortune);
		float f = pickaxe.getDestroySpeed(mineState);
		if (f > 1.0F) {
			if (efficiency > 0) {
				f += (float)(efficiency * efficiency + 1);
			}
		}
		final boolean isFluidBlock = BlockUtils.isFluidBlock(mineState.getBlock());
		final float blockDestorySpeed = isFluidBlock ? 1.0F : mineState.getDestroySpeed(level, minePos);
		
		boolean pauseScanning = false;
		
		if(blockDestorySpeed >= 0 && !isEmptyBlock(mineState, mineState.getBlock()) && BlockUtils.allowedToBreak(level, mineState, minePos, fakePlayer)) {
			if(!mineState.hasBlockEntity()) {				
				//TODO Allow User to filter block
				if(isFluidBlock) {
					handleFluidBlock(minePos);
				}
				else {
					//Is WaterLogged
					FluidState fluidState = mineState.getFluidState();
					if(fluidState !=null && fluidState != Fluids.EMPTY.defaultFluidState()) {
						handleFluidBlock(minePos);
					}
					
					int baseCostPerBlock = 100;
					int energyCost = baseCostPerBlock;
					energyCost *= (int) ((blockDestorySpeed + 1) * 2);
					
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
						fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
						boolean particles = true;
						boolean sounds = true;
						List<ItemStack> drops = BlockUtils.breakBlock(level, minePos, fakePlayer, pickaxe, fortune, sounds, particles);            
						for(ItemStack drop : drops) {
							ItemStack insertStack = drop.copy();
							ItemStack remainder = insertStack;
							
							boolean voidDrop = false;
							filterSearch : for(VoidFilter filter : this.voidFilters) {
								if(filter.matchesFilter(insertStack.getItem())) {
									voidDrop = true;
									break filterSearch;
								}
							}
							if(voidDrop) {
								continue;
							}
							
							remainder = InventoryUtils.forceStackInInventoryAllSlots(outputItems, remainder, (slot) -> {
								updateOutputItems();
								setChanged();
							});
						}
						this.energyStorage.consumeEnergy(energyCost, false);
						final float destroySpeed = net.minecraftforge.event.ForgeEventFactory.getBreakSpeed(fakePlayer, mineState, f, minePos);
						int i = net.minecraftforge.common.ForgeHooks.isCorrectToolForDrops(mineState, fakePlayer) ? 30 : 100;
						float calcSpeed = blockDestorySpeed / destroySpeed / (float)i;
						int delay = (int)(calcSpeed);
						this.scanDelay = delay;
						fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);	
					}
					else {
						pauseScanning = true;
					}
				}
			}
		}
		if(!pauseScanning) {
			this.lastScanPosition = nextPos;
		}
	}
	
	public void handleFluidBlock(BlockPos minePos) {
		BlockState blockState = level.getBlockState(minePos);
		FluidState fluidState = level.getFluidState(minePos);
		
		if(fluidState.isEmpty() || !fluidState.isSource()) {
			return;
		}
		
		if(blockState.getDestroySpeed(level, minePos) < 0) {
			return;
		}
		
		FluidStack fluidStack = BlockUtils.getFluidFromSource(level, minePos);
		if(!fluidStack.isEmpty()) {
			IFluidHandler fillTank = null;
			for(Direction dir : Direction.values()) {
				BlockPos otherPos = worldPosition.relative(dir);
				BlockEntity blockEntity = level.getBlockEntity(otherPos);
				if(blockEntity !=null) {
					IFluidHandler tank = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).orElse(null);
					if(tank !=null) {
						if(tank.fill(fluidStack, FluidAction.SIMULATE) == fluidStack.getAmount()) {
							fillTank = tank;
							break;
						}
					}
				}
			}
			
			if(fillTank !=null) {
				int fluidDrainCost = 100;
				if(this.energyStorage.getEnergyStored() >= fluidDrainCost) {
					FluidStack drainedFluid = BlockUtils.consumeFluidSource(level, minePos, Blocks.AIR.defaultBlockState());
					fillTank.fill(drainedFluid, FluidAction.EXECUTE);
					this.energyStorage.consumeEnergy(fluidDrainCost, false);
					this.scanDelay = 20;
				}
			}
		}
	}
	
	public static boolean isEmptyBlock(BlockState state, Block block) {
        if (block == null) {
            return true;
        }
        if (state.getMaterial() == Material.AIR || state.isAir()) {
            return true;
        }
        return false;
    }
	
	public ItemStack getPick(boolean silk, int fortune) {
		if(silk) {
			if(TOOL_SILK == null || TOOL_SILK.isEmpty()) {
				TOOL_SILK = new ItemStack(Items.NETHERITE_PICKAXE);
				TOOL_SILK.enchant(Enchantments.SILK_TOUCH, 1);
			}
			return TOOL_SILK;
		}
		else if(fortune > 0) {
			if(TOOL_FORTUNE == null || TOOL_FORTUNE.isEmpty()) {
				TOOL_FORTUNE = new ItemStack(Items.NETHERITE_PICKAXE);
			}
			TOOL_FORTUNE.enchant(Enchantments.BLOCK_FORTUNE, fortune);
			return TOOL_FORTUNE;
		}
		else {
			if(TOOL_NORMAL == null || TOOL_NORMAL.isEmpty()) {
				TOOL_NORMAL = new ItemStack(Items.NETHERITE_PICKAXE);
			}
			return TOOL_NORMAL;
		}
	}

	public Vec3i findNextPosition() {
		Direction dir = this.getBlockState().getValue(QuarryBlock.FACING).getOpposite();
		
		
		int remainingHeight = this.worldPosition.below().getY() - this.level.getMinBuildHeight();
		//int maxX = 0;
		int maxY = remainingHeight;
		//int maxZ = 0;
		
		Vec3i maxRangePos = getMaxRangeOffset();	
		
		if(lastScanPosition == null) {
			return new Vec3i(0, 0, 0);
		}
		int nextX = lastScanPosition.getX();
		int nextY = lastScanPosition.getY();
		int nextZ = lastScanPosition.getZ();
		/*WAMUtilsMod.LOGGER.debug("Dir: " + dir);	
		WAMUtilsMod.LOGGER.debug("nextX: " + nextX);
		WAMUtilsMod.LOGGER.debug("nextY: " + nextY);
		WAMUtilsMod.LOGGER.debug("nextZ: " + nextZ);*/		
		
		
		boolean positive = dir.getAxisDirection() == AxisDirection.POSITIVE;
		if(dir.getAxis() == Axis.X) {
			nextX = positive ? nextX + 1 : nextX - 1;
			if(positive ? nextX > maxRangePos.getX() : nextX < maxRangePos.getX()) {
				nextZ = positive ? nextZ - 1 : nextZ + 1; //Do the opposite
				nextX = 0;
			}
			if(positive ? (nextZ < maxRangePos.getZ()) : (nextZ > maxRangePos.getZ())) {
				nextY--;
				nextX = 0;
				nextZ = 0;
			}
			if(nextY < -maxY) {
				//WAMUtilsMod.LOGGER.debug("Done X");	
				return null;
			}
		}
		else if(dir.getAxis() == Axis.Z) {
			nextZ = positive ? nextZ + 1 : nextZ - 1;
			if(positive ? nextZ > maxRangePos.getZ() : nextZ < maxRangePos.getZ()) {
				nextX = positive ? nextX + 1 : nextX - 1; //Do the opposite
				nextZ = 0;
			}
			if(positive ? (nextX > maxRangePos.getX()) : (nextX < maxRangePos.getX())) {
				nextY--;
				nextX = 0;
				nextZ = 0;
			}
			if(nextY < -maxY) {
				//WAMUtilsMod.LOGGER.debug("Done Z");	
				return null;
			}
		}
		
		/*nextX += 1;
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
		}*/

		return new Vec3i(nextX, nextY, nextZ);
	}
	
	public Vec3i getMaxRangeOffset() {
		Direction dir = this.getBlockState().getValue(QuarryBlock.FACING).getOpposite();
		int range = getRange() - 1;
		int maxX = 0;
		int maxZ = 0;
		if(dir == Direction.NORTH) {
			maxX = -range;
			maxZ = -range;
		}
		else if(dir == Direction.SOUTH) {
			maxX = range;
			maxZ = range;
		}
		else if(dir == Direction.EAST) {
			maxX = range;
			maxZ = -range;
		}
		else if(dir == Direction.WEST) {
			maxX = -range;
			maxZ = range;
		}
		return new Vec3i(maxX, 0, maxZ);
	}
	
	@Override
	public AABB getRenderBoundingBox() {
		Direction dir = getBlockState().getValue(QuarryBlock.FACING).getOpposite();
        Vec3i maxRangePos = getMaxRangeOffset();        
		BlockPos back = getBlockPos().relative(dir);
        AABB aabb = new AABB(back).expandTowards(maxRangePos.getX(), 0, maxRangePos.getZ());
        return aabb;
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
		if(nbt.contains("RedstoneMode")) {
			this.redstoneMode = RedstoneMode.getMode(nbt.getInt("RedstoneMode"));
			this.setChanged();
		}
		if(nbt.contains("PushOutput")) {
			this.pushOutput = nbt.getBoolean("PushOutput");
			this.setChanged();
		}
		if(nbt.contains("AddFilter")) {
			VoidFilter filter = new VoidFilter(nbt.getCompound("Filter"));
			this.addVoidFilter(filter);
		}
		if(nbt.contains("RemoveFilter")) {
			VoidFilter filter = new VoidFilter(nbt.getCompound("Filter"));
			this.removeVoidFilter(filter);
		}
	}

	//With upgrades (+2, +4, +6, +8)
	public int getRange() {
		int upgrades = Math.min(MAX_RANGE_UPGRADES, rangeUpgradeCount);
		return 8 + (2 * upgrades);
	}

	public boolean isItemAValidUpgradeItem(ItemStack stack, int slot) {
		if (!stack.isEmpty()) {
			if (stack.getItem() == Items.ENCHANTED_BOOK && slot == 0) {
				int silk = ItemUtils.getEnchantmentLevel(stack, Enchantments.SILK_TOUCH);
				int fortune = ItemUtils.getEnchantmentLevel(stack, Enchantments.BLOCK_FORTUNE);
				int eff = ItemUtils.getEnchantmentLevel(stack, Enchantments.BLOCK_EFFICIENCY);
				return silk > 0 || fortune > 0 || eff > 0;
			}
			if(slot == 1) {
				return stack.getItem() == ItemInit.UPGRADE_RANGE.get();
			}
		}
		return false;
	}
	
	public void updateUpgrades() {
		this.rangeUpgradeCount = InventoryUtils.countItem(upgradeItems, ItemInit.UPGRADE_RANGE.get());
		
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
				if(slot == 1) {
					return MAX_RANGE_UPGRADES;
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
	
	public void updateOutputItems() {
		this.isFull = InventoryUtils.isFull(outputItems);
	}
	
	@Nonnull
	private ItemStackHandler createOutputItemHandler() {
		return new ItemStackHandler(OUTPUT_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				updateOutputItems();
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
		return new CombinedInvWrapper(upgradeItems, outputItems) {
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
		
		tag.putBoolean("DoneMining", doneMining);
		tag.putInt("ScanDelay", scanDelay);
		tag.putInt("HopperDelay", hopperCooldown);
		tag.putBoolean("PushOutputsOut", pushOutput);
		
		tag.putInt("RangeUpgrades", rangeUpgradeCount);
		tag.putBoolean("isFull", isFull);
		
		if(!descPacket) {
			tag.put("Inventory.Upgrades", upgradeItems.serializeNBT());
			tag.put("Inventory.Output", outputItems.serializeNBT());
		}
        energy.ifPresent(h -> tag.put("Energy", h.serializeNBT()));
        
        ListTag filterList = new ListTag();
        for(VoidFilter filter : this.voidFilters) {
        	CompoundTag filterTag = filter.serializeNBT();
        	filterList.add(filterTag);
        }
        tag.put("VoidFilters", filterList);
	}

	@Override
	public void readCustomNBT(CompoundTag tag, boolean descPacket) {
		if(tag.contains("LastScanPosition")) {
			this.lastScanPosition = BlockUtils.loadVec3i(tag, "LastScanPosition");
		}
		
		doneMining = tag.getBoolean("DoneMining");
		redstoneMode = RedstoneMode.getMode(tag.getInt("RedstoneMode"));
		scanDelay = tag.getInt("ScanDelay");
		hopperCooldown = tag.getInt("HopperDelay");
		pushOutput = tag.getBoolean("PushOutputsOut");
		
		rangeUpgradeCount = tag.getInt("RangeUpgrades");
		isFull = tag.getBoolean("isFull");
		
		if(!descPacket) {
			if (tag.contains("Inventory.Upgrades")) {
				upgradeItems.deserializeNBT(tag.getCompound("Inventory.Upgrades"));
			}
			if (tag.contains("Inventory.Output")) {
				outputItems.deserializeNBT(tag.getCompound("Inventory.Output"));
			}
		}
		energy.ifPresent(h -> h.deserializeNBT(tag.getCompound("Energy")));
		
		if(tag.contains("VoidFilters")) {
			this.voidFilters.clear();
			ListTag filterList = tag.getList("VoidFilters", 10);
			for(int i = 0; i < filterList.size(); i++) {
				this.voidFilters.add(new VoidFilter(filterList.getCompound(i)));
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
		return 20000;
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

