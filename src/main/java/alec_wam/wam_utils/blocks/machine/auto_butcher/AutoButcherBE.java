package alec_wam.wam_utils.blocks.machine.auto_butcher;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.blocks.RedstoneMode;
import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.capabilities.BlockEnergyStorage;
import alec_wam.wam_utils.capabilities.IEnergyStorageBlockEntity;
import alec_wam.wam_utils.entities.WAMUtilsFakePlayer;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ItemInit;
import alec_wam.wam_utils.utils.InventoryUtils;
import alec_wam.wam_utils.utils.ItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public class AutoButcherBE extends WAMUtilsBlockEntity implements IEnergyStorageBlockEntity {

	public static final int MAX_RANGE_UPGRADES = 12;
	public static final int UPGRADE_SLOTS = 2;
	public static final int OUTPUT_SLOTS = 18;
	
	private static ItemStack SWORD_NORMAL;
    private static ItemStack SWORD_LOOTING;
	
	protected final ItemStackHandler upgradeItems = createUpgradeItemHandler();
	public final LazyOptional<IItemHandler> upgradeItemHandler = LazyOptional.of(() -> upgradeItems);
	protected final ItemStackHandler outputItems = createOutputItemHandler();
	public final LazyOptional<IItemHandler> externalOutputItemHandler = LazyOptional.of(() -> outputItems);
	public final LazyOptional<IItemHandler> combinedAllItemHandler = LazyOptional.of(this::createCombinedAllItemHandler);
	
	public BlockEnergyStorage energyStorage;
    private LazyOptional<BlockEnergyStorage> energy;
	
	protected final RandomSource random = RandomSource.create();

	private RedstoneMode redstoneMode = RedstoneMode.ON;
	private boolean running = false;	
	private boolean isFull;
	private int killDelay;
	private int minAnimals = 2;
	private int rangeUpgrades = 0;
	private boolean showBoundingBox = true;
	
	public AutoButcherBE(BlockPos pos, BlockState state) {
		super(BlockInit.AUTO_BUTCHER_BE.get(), pos, state);
		
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

	public int getKillDelay() {
		return killDelay;
	}

	public int getMinAnimals() {
		return minAnimals;
	}

	public void setMinAnimals(int minAnimals) {
		this.minAnimals = minAnimals;
		if(!this.level.isClientSide) {
			this.setChanged();
		}
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
		if(nbt.contains("MinAnimals")) {
			setMinAnimals(nbt.getInt("MinAnimals"));
		}
	}
	
	public void tickClient() {
	}

	public void tickServer() {
		
		if(!redstoneMode.isMet(level, worldPosition)) {
			return;
		}
		
		if (killDelay > 0) {
			killDelay--;
		} else {
			if(this.energyStorage.getEnergyStored() <= 0) {
				return;
			}
			
			AABB aabb = getRangeBB();
			List<Animal> animals = level.getEntitiesOfClass(Animal.class, aabb);
			if(!animals.isEmpty()) {
				Animal randomAnimal = animals.get(random.nextInt(0, animals.size()));
				
				List<Animal> sameAnimals = animals.stream().filter((animal) -> {
					return animal.getClass() == randomAnimal.getClass() && animal.isAlive() && animal.getHealth() > 0.0F && !animal.isBaby() && !animal.isInvulnerable();
				}).collect(Collectors.toList());
				
				int count = sameAnimals.size();
				
				if(count > minAnimals) {
					
					ItemStack enchantedBook =  this.upgradeItems.getStackInSlot(0);
					int looting = ItemUtils.getEnchantmentLevel(enchantedBook, Enchantments.MOB_LOOTING);
					int fireAspect = ItemUtils.getEnchantmentLevel(enchantedBook, Enchantments.FIRE_ASPECT);
					WAMUtilsFakePlayer fakePlayer = WAMUtilsFakePlayer.get((ServerLevel)level, worldPosition).get();
					
					Animal randomSameAnimal = sameAnimals.get(random.nextInt(0, sameAnimals.size()));
					
					int energyCostPerAttack = 100;
					
					if(looting > 0) {
						energyCostPerAttack = (int) ((double)energyCostPerAttack * (2.5D * looting));
					}
					if(fireAspect > 0) {
						energyCostPerAttack = (int) ((double)energyCostPerAttack * (3.0D));
					}
					
					if(this.energyStorage.getEnergyStored() >= energyCostPerAttack) {
						ItemStack sword = getSword(looting);
						fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, sword);
						
						/*fakePlayer.setLastHurtMob(randomAnimal);
						sword.hurtEnemy(randomAnimal, fakePlayer);
						if (fireAspect > 0) {
							randomSameAnimal.setSecondsOnFire(8);
						}
						
						DamageSource player = DamageSource.playerAttack(fakePlayer);
						WAMUtilsMod.LOGGER.debug("Kill UUID: " + randomAnimal.getId());
						EntityEventHandler.MOB_DROP_CAPTURES.put(randomAnimal.getId(), outputItems);
						randomSameAnimal.hurt(player, randomSameAnimal.getHealth());
						
						this.level.playSound((Player)null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.BLOCKS, 1.0F, 1.0F);
						
						fakePlayer.resetAttackStrengthTicker();*/
						
						DamageSource player = DamageSource.playerAttack(fakePlayer);
						
						if (fireAspect > 0) {
							randomSameAnimal.setSecondsOnFire(8);
						}
						
						LootTable table = this.level.getServer().getLootTables().get(randomSameAnimal.getLootTable());
				        LootContext.Builder context = new LootContext.Builder((ServerLevel) this.level)
				                .withRandom(this.level.random)
				                .withParameter(LootContextParams.THIS_ENTITY, randomSameAnimal)
				                .withParameter(LootContextParams.DAMAGE_SOURCE, player)
				                .withParameter(LootContextParams.ORIGIN, randomSameAnimal.position())
				                .withParameter(LootContextParams.KILLER_ENTITY, fakePlayer)
				                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, fakePlayer)
				                .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, fakePlayer);
				        final Vec3 position = randomSameAnimal.position();
				        table.getRandomItems(context.create(LootContextParamSets.ENTITY)).forEach(stack -> {
				        	ItemStack copy = stack.copy();
				        	ItemStack remainder = InventoryUtils.forceStackInInventoryAllSlots(outputItems, copy, null);
				        	if(!remainder.isEmpty()) {
				        		//TODO Mark full
				        		ItemEntity itementity = new ItemEntity(this.level, position.x(), position.y(), position.z(), remainder);
				                itementity.setDefaultPickUpDelay();
				        		this.level.addFreshEntity(itementity);
				        	}
				        });
				        
				        List<ItemEntity> extra = new ArrayList<>();
				        ForgeHooks.onLivingDrops(randomSameAnimal, player, extra, looting, true);
				        extra.forEach(itemEntity -> {
				        	ItemStack copy = itemEntity.getItem().copy();
				        	ItemStack remainder = InventoryUtils.forceStackInInventoryAllSlots(outputItems, copy, null);
				        	if(!remainder.isEmpty()) {
				        		ItemEntity itementity = new ItemEntity(this.level, position.x(), position.y(), position.z(), remainder);
				                itementity.setDefaultPickUpDelay();
				        		this.level.addFreshEntity(itementity);
				        	}
				        	else {
				        		itemEntity.remove(Entity.RemovalReason.KILLED);
				        	}
				        });
				        
				        this.level.playSound((Player)null, position.x(), position.y(), position.z(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.BLOCKS, 1.0F, 1.0F);
						randomSameAnimal.setHealth(0);
				        randomSameAnimal.gameEvent(GameEvent.ENTITY_DIE);   
				        randomSameAnimal.getCombatTracker().recheckStatus();
				        randomSameAnimal.setPose(Pose.DYING);				  
				        
				        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
						
						killDelay = 5 * 20;
					}
				}
			}
		}
	}
	
	public ItemStack getSword(int looting) {
		if(looting > 0) {
			if(SWORD_LOOTING == null || SWORD_LOOTING.isEmpty()) {
				SWORD_LOOTING = new ItemStack(Items.DIAMOND_SWORD);
			}
			SWORD_LOOTING.enchant(Enchantments.MOB_LOOTING, looting);
			return SWORD_LOOTING;
		}
		else {
			if(SWORD_NORMAL == null || SWORD_NORMAL.isEmpty()) {
				SWORD_NORMAL = new ItemStack(Items.DIAMOND_SWORD);
			}
			return SWORD_NORMAL;
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
			if (stack.is(Items.ENCHANTED_BOOK) && slot == 0) {
				int fireAspect = ItemUtils.getEnchantmentLevel(stack, Enchantments.FIRE_ASPECT);
				int looting = ItemUtils.getEnchantmentLevel(stack, Enchantments.MOB_LOOTING);
				return fireAspect > 0 || looting > 0;
			}
			if (stack.getItem() == ItemInit.UPGRADE_RANGE.get() && slot == 1) {
				return true;
			}
		}
		return false;
	}
	
	public void updateUpgrades() {
		this.rangeUpgrades = InventoryUtils.countItem(upgradeItems, ItemInit.UPGRADE_RANGE.get());
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
					return super.getSlotLimit(slot);
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
		externalOutputItemHandler.invalidate();
		combinedAllItemHandler.invalidate();
	}

	@Override
	public void writeCustomNBT(CompoundTag tag, boolean descPacket) {
		tag.putInt("RedstoneMode", redstoneMode.ordinal());
		tag.putBoolean("isFull", isFull);
		tag.putInt("KillDelay", killDelay);
		tag.putInt("MinAnimals", minAnimals);
		tag.putInt("RangeUpgrades", rangeUpgrades);
		tag.putBoolean("ShowBoundingBox", showBoundingBox);
		tag.put("Inventory.Upgrades", upgradeItems.serializeNBT());
		tag.put("Inventory.Output", outputItems.serializeNBT());
        energy.ifPresent(h -> tag.put("Energy", h.serializeNBT()));
	}

	@Override
	public void readCustomNBT(CompoundTag tag, boolean descPacket) {
		redstoneMode = RedstoneMode.getMode(tag.getInt("RedstoneMode"));
		isFull = tag.getBoolean("isFull");
		killDelay = tag.getInt("KillDelay");

		minAnimals = tag.getInt("MinAnimals");
		rangeUpgrades = tag.getInt("RangeUpgrades");
		showBoundingBox = tag.getBoolean("ShowBoundingBox");
		if (tag.contains("Inventory.Upgrades")) {
			upgradeItems.deserializeNBT(tag.getCompound("Inventory.Upgrades"));
		}
		if (tag.contains("Inventory.Output")) {
			outputItems.deserializeNBT(tag.getCompound("Inventory.Output"));
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
			return externalOutputItemHandler.cast();
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

