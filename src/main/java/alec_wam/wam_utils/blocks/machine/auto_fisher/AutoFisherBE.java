package alec_wam.wam_utils.blocks.machine.auto_fisher;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.capabilities.BlockEnergyStorage;
import alec_wam.wam_utils.capabilities.IEnergyStorageBlockEntity;
import alec_wam.wam_utils.entities.WAMUtilsFakePlayer;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.utils.InventoryUtils;
import alec_wam.wam_utils.utils.ItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public class AutoFisherBE extends WAMUtilsBlockEntity implements IEnergyStorageBlockEntity {

	public static final int FISH_SLOTS = 5;
	public static final int UPGRADE_SLOTS = 2;
	protected final ItemStackHandler fishItems = createFishItemHandler();
	protected final ItemStackHandler upgradeItems = createUpgradeItemHandler();
	public final LazyOptional<IItemHandler> externalFishItemHandler = LazyOptional.of(this::createExternalOutputItemHandler);
	public final LazyOptional<IItemHandler> combinedAllItemHandler = LazyOptional.of(this::createCombinedAllItemHandler);
	
	//Warnings
	private boolean isFull;
	private boolean hasWater;
	
	private int fishingCost = 10;
	private int fishingTime = 0;
	private int maxFishingTime = 0;
	private float fishAngle;
	
	public BlockEnergyStorage energyStorage;
    private LazyOptional<BlockEnergyStorage> energy;	
	
	protected final RandomSource random = RandomSource.create();
	private ItemStack fishingRod;
	private RedstoneMode redstoneMode = RedstoneMode.ON;
	
	public AutoFisherBE(BlockPos p_155229_, BlockState p_155230_) {
		super(BlockInit.AUTO_FISHER_BE.get(), p_155229_, p_155230_);
		fishingRod = new ItemStack(Items.FISHING_ROD);
		
		this.energyStorage = new BlockEnergyStorage(this, 0);
        this.energy = LazyOptional.of(() -> this.energyStorage);
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

	public boolean hasWater() {
		return hasWater;
	}

	public void setHasWater(boolean value) {
		hasWater = value;
		if(!this.level.isClientSide) {
			this.setChanged();
		}
	}
	
	public int getFishingTime() {
		return fishingTime;
	}

	public void setFishingTime(int fishingTime) {
		this.fishingTime = fishingTime;
	}

	public int getMaxFishingTime() {
		return maxFishingTime;
	}

	public void setMaxFishingTime(int maxFishingTime) {
		this.maxFishingTime = maxFishingTime;
	}
	
	public int getFishingCost() {
		return fishingCost;
	}

	public void setFishingCost(int energyCost) {
		this.fishingCost = energyCost;
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
	}

	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		redstoneMode = RedstoneMode.getMode(nbt.getInt("RedstoneMode"));
		if(!descPacket) {
			if (nbt.contains("Inventory.Fish")) {
				fishItems.deserializeNBT(nbt.getCompound("Inventory.Fish"));
			}
			if (nbt.contains("Inventory.Upgrades")) {
				upgradeItems.deserializeNBT(nbt.getCompound("Inventory.Upgrades"));
			}
		}
		energy.ifPresent(h -> h.deserializeNBT(nbt.getCompound("Energy")));
		this.isFull = nbt.getBoolean("isFull");
		this.hasWater = nbt.getBoolean("hasWater");
		this.fishingCost = nbt.getInt("FishingCost");	
		this.fishingTime = nbt.getInt("FishingTime");	
		this.maxFishingTime = nbt.getInt("MaxFishingTime");			
	}

	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		nbt.putInt("RedstoneMode", redstoneMode.ordinal());
		if(!descPacket) {
			nbt.put("Inventory.Fish", fishItems.serializeNBT());
			nbt.put("Inventory.Upgrades", upgradeItems.serializeNBT());
		}
		nbt.putInt("FishingCost", fishingCost);
		nbt.putInt("FishingTime", fishingTime);
		nbt.putInt("MaxFishingTime", maxFishingTime);
		nbt.putBoolean("isFull", isFull);
		nbt.putBoolean("hasWater", hasWater);
        energy.ifPresent(h -> nbt.put("Energy", h.serializeNBT()));
	}
	
	public void tickServer() {
		ServerLevel serverlevel = (ServerLevel)this.level;
		
		FluidState fluidstate = level.getFluidState(worldPosition.below());		
		final boolean oldHasWater = hasWater;
		hasWater = fluidstate.is(FluidTags.WATER) && fluidstate.isSource();		
		if(oldHasWater != hasWater) {
			setChanged();
			//markBlockForUpdate(null);
		}
		
		if(!hasWater || isFull) {
			if(fishingTime > 0) {
				fishingTime = 0;
				maxFishingTime = 0;
				//setChanged();
				//markBlockForUpdate(null);
			}
			return;
		}
		
		if(!redstoneMode.isMet(serverlevel, worldPosition)) {
			return;
		}
		
		boolean hasEnoughEnergy = this.energyStorage.getEnergyStored() >= fishingCost;	
		if(!hasEnoughEnergy) {
			return;
		}				
		
		if(this.maxFishingTime <= 0) {
			int lure = getEnchantmentLevel(Enchantments.FISHING_SPEED);
			this.maxFishingTime = Mth.nextInt(this.random, 100, 600);
			this.maxFishingTime -= lure * 20 * 5;
			//setChanged();
		}
		
		if(this.fishingTime < this.maxFishingTime) {
			
			this.energyStorage.consumeEnergy(fishingCost, false);
			
			int i = 1;
			BlockPos blockpos = worldPosition.above();
			if (this.random.nextFloat() < 0.25F && this.level.isRainingAt(blockpos)) {
				++i;
			}

			if (this.random.nextFloat() < 0.5F && !this.level.canSeeSky(blockpos)) {
				--i;
			}
			
			this.fishingTime += i;
			//setChanged();
			if(this.fishingTime < this.maxFishingTime) {
				int remainingTime = this.maxFishingTime - this.fishingTime;
				this.fishAngle += (float)this.random.triangle(0.0D, 9.188D);
	            float f = this.fishAngle * ((float)Math.PI / 180F);
	            float f1 = Mth.sin(f);
	            float f2 = Mth.cos(f);
	            double centerX = worldPosition.getX() + 0.5D;
	            double centerY = worldPosition.getY() - 1.0D;
	            double centerZ = worldPosition.getZ() + 0.5D;
	            double d0 = centerX + (double)(f1 * (float)remainingTime * 0.1F);
	            double d1 = (double)((float)Mth.floor(centerY) + 1.0F);
	            double d2 = centerZ + (double)(f2 * (float)remainingTime * 0.1F);
	            if (serverlevel.getBlockState(new BlockPos((int)d0, (int)d1 - 1, (int)d2)).getMaterial() == net.minecraft.world.level.material.Material.WATER) {
	               if (this.random.nextFloat() < 0.15F) {
	                  serverlevel.sendParticles(ParticleTypes.BUBBLE, d0, d1 - (double)0.1F, d2, 1, (double)f1, 0.1D, (double)f2, 0.0D);
	               }

	               float f3 = f1 * 0.04F;
	               float f4 = f2 * 0.04F;
	               serverlevel.sendParticles(ParticleTypes.FISHING, d0, d1, d2, 0, (double)f4, 0.01D, (double)(-f3), 1.0D);
	               serverlevel.sendParticles(ParticleTypes.FISHING, d0, d1, d2, 0, (double)(-f4), 0.01D, (double)f3, 1.0D);
	            }
			}
		}
		else {
			BlockPos fishingPosition = this.worldPosition.below();
			Vec3 fishingVec = new Vec3(fishingPosition.getX() + 0.5, fishingPosition.getY() + 0.5, fishingPosition.getZ() + 0.5);
			WAMUtilsFakePlayer player = WAMUtilsFakePlayer.get((ServerLevel)level, fishingVec.x, fishingVec.y, fishingVec.z).get();
			int luck = getEnchantmentLevel(Enchantments.FISHING_LUCK);
			int lure = getEnchantmentLevel(Enchantments.FISHING_SPEED);
			FishingHook hook = new FishingHook(player, level, luck, lure);
			hook.setPos(fishingVec);
			
			LootContext.Builder lootcontext$builder = 
					(new LootContext.Builder((ServerLevel)this.level))
					.withParameter(LootContextParams.ORIGIN, fishingVec)
					.withParameter(LootContextParams.TOOL, fishingRod)
					.withParameter(LootContextParams.THIS_ENTITY, hook)
					.withRandom(this.random)
					.withLuck((float)luck + player.getLuck());
            lootcontext$builder.withParameter(LootContextParams.KILLER_ENTITY, player)
            .withParameter(LootContextParams.THIS_ENTITY, hook);
            LootTable loottable = this.level.getServer().getLootTables().get(BuiltInLootTables.FISHING);
            List<ItemStack> list = loottable.getRandomItems(lootcontext$builder.create(LootContextParamSets.FISHING));
            net.minecraftforge.event.entity.player.ItemFishedEvent event = new net.minecraftforge.event.entity.player.ItemFishedEvent(list, 1, hook);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
               return;
            }
            
            for(ItemStack stack : list) {
            	ItemStack remainder = stack.copy();            	
            	InventoryUtils.forceStackInInventoryAllSlots(fishItems, remainder, (slot -> {
            		updateFishItems();
            		setChanged();
            	}));
            }
            
            if(!list.isEmpty()) {
            	level.playSound((Player)null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.BLOCKS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
                this.fishingTime = 0;
                this.maxFishingTime = Mth.nextInt(this.random, 100, 600);
    			this.maxFishingTime -= lure * 20 * 5;
            }
		}
	}
	
	public void tickClient() {
		
	}
	
	@Override
	public void setRemoved() {
		super.setRemoved();
		energy.invalidate();
		externalFishItemHandler.invalidate();
		combinedAllItemHandler.invalidate();
	}

	public void updateFishItems() {
		isFull = InventoryUtils.isFull(fishItems);
	}
	
	@Nonnull
	private ItemStackHandler createFishItemHandler() {
		return new ItemStackHandler(FISH_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				updateFishItems();
				setChanged();
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				return stack;
			}
		};
	}

	public boolean isItemAValidUpgradeItem(int slot, ItemStack stack) {
		if (!stack.isEmpty()) {
			if (stack.getItem() == Items.ENCHANTED_BOOK && slot == 0) {
				int luck = ItemUtils.getEnchantmentLevel(stack, Enchantments.FISHING_LUCK);
				int lure = ItemUtils.getEnchantmentLevel(stack, Enchantments.FISHING_SPEED);
				return luck > 0 || lure > 0;
			}
		}
		return false;
	}
	
	public int getEnchantmentLevel(Enchantment enchant) {
		ItemStack enchantedBook = this.upgradeItems.getStackInSlot(0);
		if(!enchantedBook.isEmpty()) {
			return ItemUtils.getEnchantmentLevel(enchantedBook, enchant);
		}
		return 0;
	}
	
	public void updateUpgrades() {
		//TODO Update fishingRod item with enchantments
		
		int cost = 10;
		int luck = getEnchantmentLevel(Enchantments.FISHING_LUCK);
		int lure = getEnchantmentLevel(Enchantments.FISHING_SPEED);
		cost += luck * 10;
		cost += lure * 10;
		this.fishingCost = cost;
		setChanged();
	}
	
	@Nonnull
	private ItemStackHandler createUpgradeItemHandler() {
		return new ItemStackHandler(UPGRADE_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				updateUpgrades();
			}

			@Override
			public int getSlotLimit(int slot) {
				return super.getSlotLimit(slot);
			}

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return isItemAValidUpgradeItem(slot, stack);
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				if (!isItemAValidUpgradeItem(slot, stack)) {
					return stack;
				}
				return super.insertItem(slot, stack, simulate);
			}
		};
	}

	@Nonnull
	protected IItemHandler createExternalOutputItemHandler() {
		return new CombinedInvWrapper(fishItems) {
			@NotNull
			@Override
			public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
				return stack;
			}
		};
	}
	
	@Nonnull
	private IItemHandler createCombinedAllItemHandler() {
		return new CombinedInvWrapper(fishItems, upgradeItems) {
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
			if (side == null) {
				return combinedAllItemHandler.cast();
			} else if (side != Direction.DOWN) {
				return externalFishItemHandler.cast();
			} 
		} 
		else if (cap == ForgeCapabilities.ENERGY) {
            return energy.cast();
		}
		return super.getCapability(cap, side);
	}

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
		return 10000;
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
