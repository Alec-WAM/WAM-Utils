package alec_wam.wam_utils.blocks.advanced_spawner;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.capabilities.BlockEnergyStorage;
import alec_wam.wam_utils.capabilities.IEnergyStorageBlockEntity;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ItemInit;
import alec_wam.wam_utils.init.RecipeInit;
import alec_wam.wam_utils.server.network.MessageBlockEntityUpdate;
import alec_wam.wam_utils.utils.EntityUtils;
import alec_wam.wam_utils.utils.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.network.PacketDistributor;

public class AdvancedSpawnerBE extends WAMUtilsBlockEntity implements IEnergyStorageBlockEntity {

	public static final int INPUT_SLOTS = 1;
	public static final int UPGRADE_SLOTS = 4;
	public static final int OUTPUT_SLOTS = 18;

	protected final ItemStackHandler inputItems = createInputItemHandler();
	public final LazyOptional<IItemHandler> inputItemHandler = LazyOptional.of(() -> inputItems);
	protected final ItemStackHandler upgradeItems = createUpgradeItemHandler();
	public final LazyOptional<IItemHandler> upgradeItemHandler = LazyOptional.of(() -> upgradeItems);	
	protected final ItemStackHandler outputItems = createOutputItemHandler();
	public final LazyOptional<IItemHandler> externalOutputItemHandler = LazyOptional.of(this::createExternalOutputItemHandler);
	public final LazyOptional<IItemHandler> combinedAllItemHandler = LazyOptional.of(this::createCombinedAllItemHandler);
	
	public BlockEnergyStorage energyStorage;
    private LazyOptional<BlockEnergyStorage> energy;
	
	protected final RandomSource random = RandomSource.create();

	private boolean running = false;
	private boolean isFull = false;
	private int spawnDelay;	
	
	//UpgradeData
	private int damageCost = 1;
	private int beheadingUpgradeCount;
	private int damageUpgradeCount;
	private boolean hasBossUpgrade;
	private int displayDamageCost;
	
	
	public LivingEntity clientRenderEntity;
	public LivingEntity serverKillEntity;
	
	private int killProgress;
	private int maxKillTime;
	
	public AdvancedSpawnerBE(BlockPos pos, BlockState state) {
		super(BlockInit.ADVANCED_SPAWNER_BE.get(), pos, state);
		
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

	public int getKillProgress() {
		return killProgress;
	}

	public void setKillProgress(int killProgress) {
		this.killProgress = killProgress;
	}

	public int getMaxKillTime() {
		return maxKillTime;
	}

	public void setMaxKillTime(int maxKillTime) {
		this.maxKillTime = maxKillTime;
	}

	public int getDamageCost() {
		return displayDamageCost;
	}

	public void setDamageCost(int displayDamageCost) {
		this.displayDamageCost = displayDamageCost;
	}

	public void tickClient() {
		if(this.clientRenderEntity !=null) {
			this.clientRenderEntity.tickCount++;	
			if(!this.clientRenderEntity.isSilent()) {
				this.clientRenderEntity.setSilent(true);
			}
			this.clientRenderEntity.aiStep();			
			
			if (this.clientRenderEntity.hurtTime > 0) {
	         --this.clientRenderEntity.hurtTime;
			}
			
			if(this.energyStorage.getEnergyStored() > 0 && this.maxKillTime > 0 && this.killProgress > 0 && this.killProgress < this.maxKillTime) {
				if(this.clientRenderEntity.tickCount % (3 * 20) == 0) {
					this.clientRenderEntity.hurtDir = (float)((int)(Math.random() * 2.0D) * 180);
					this.clientRenderEntity.animateHurt();
//					DamageSource damage = DamageSource.GENERIC;
//					SoundEvent soundevent = SoundEvents.GENERIC_HURT;//this.clientRenderEntity.getHurtSound(damage);
//					float volume = 1.0F;
//					if (soundevent != null) {
//						this.level.playSound(null, worldPosition, soundevent, SoundSource.BLOCKS, volume, this.clientRenderEntity.getVoicePitch());
//						//this.clientRenderEntity.playSound(soundevent, volume, this.clientRenderEntity.getVoicePitch());
//					}
				}
			}
			//this.clientRenderEntity.tick();
		}
	}

	public void tickServer() {
		
		if(!level.hasNeighborSignal(worldPosition)) {
			return;
		}
		
		if (spawnDelay > 0) {
			spawnDelay--;
		} else {
			if (spawnDelay <= 0 && !isFull && this.serverKillEntity == null) {
				// Spawn
				ItemStack spawnItem = inputItems.getStackInSlot(0);
				if (!spawnItem.isEmpty()) {
					EntityType<?> entityType = getSpawnEntity(spawnItem);
					if (entityType != null) {
						Level level = getLevel();
						CompoundTag tag = getEntityNBT(spawnItem);
						if(tag !=null) {
							Entity entity = EntityType.loadEntityRecursive(tag, level, (p_151310_) -> {
								// p_151310_.moveTo(d0, d1, d2, p_151310_.getYRot(), p_151310_.getXRot());
								return p_151310_;
							});
							if (entity !=null && entity instanceof LivingEntity) {
								LivingEntity livingEntity = (LivingEntity)entity;
								this.killProgress = 0;
								float health = livingEntity.getMaxHealth();
								float speed = 20.0F; //Each heart of health equals a 1/2 second
								this.maxKillTime = (int)(health * speed);
								this.serverKillEntity = livingEntity;	
								//WAMUtilsMod.LOGGER.debug("maxKillTime: " + maxKillTime);
				                setChanged();
							}
						}
					}
				}
			}
			
			if(this.serverKillEntity !=null) {
				boolean canKill = true;
				
				
				if(this.serverKillEntity instanceof WitherBoss || this.serverKillEntity instanceof EnderDragon) {
					if(!hasBossUpgrade) {
						canKill = false;
					}
				}
				
				if(killProgress < maxKillTime) {
					if(canKill) {		
						int multipliedCost = damageCost * (damageUpgradeCount + 1);
						if(energyStorage.getEnergyStored() >= multipliedCost) {
							//WAMUtilsMod.LOGGER.debug("Damage Cost: " + (multipliedCost));
							//WAMUtilsMod.LOGGER.debug("Damage Upgrade: " + (damageUpgradeCount));
							final int origPower = energyStorage.getEnergyStored();
							final int newEnergy = energyStorage.getEnergyStored() - multipliedCost;
							energyStorage.setEnergy(Math.max(newEnergy, 0));
							//WAMUtilsMod.LOGGER.debug("Energy Update: " + (energyStorage.getEnergyStored()) + " / " + newEnergy);
							//WAMUtilsMod.LOGGER.debug("Energy Change: " + (origPower - energyStorage.getEnergyStored()));
							final int oldProgress = killProgress;
							killProgress = killProgress + (damageUpgradeCount + 1);
							//WAMUtilsMod.LOGGER.debug("Progress Change: " + (killProgress - oldProgress));
							
							if(killProgress > maxKillTime) {
								killProgress = maxKillTime;
							}

							//sendSyncPacket(false);
						}
					}
				}
				else {
					//WAMUtilsMod.LOGGER.debug("Entity: " + livingEntity);
					//TODO Handle Player Upgrade
					List<ItemEntity> drops = EntityUtils.captureEntityDrops(level, this.serverKillEntity, random, true, true, 0);
					//WAMUtilsMod.LOGGER.debug("Drops: " + drops);
					
					if(random.nextInt(10) < beheadingUpgradeCount) {
						ItemStack head = getHeadFromEntity(this.serverKillEntity);
						if(!head.isEmpty()) {
							ItemEntity entityItem = new ItemEntity(this.serverKillEntity.getCommandSenderWorld(), this.serverKillEntity.getX(), this.serverKillEntity.getY(), this.serverKillEntity.getZ(), head);
							entityItem.setDefaultPickUpDelay();
							drops.add(entityItem);
						}
					}
					
					for(ItemEntity item : drops) {
						ItemStack stack = item.getItem();
						if(!stack.isEmpty()) {
							ItemStack remainder = InventoryUtils.putStackInInventoryAllSlots(outputItems, stack);
							
							if(!remainder.isEmpty()) {
								//TODO: Update this on inventory change
								this.isFull = true;
							}
						}
					}
					killProgress = 0;
					spawnDelay = 5 * 20;
					//sendSyncPacket(false);
				}
			}
		}
	}
	
	public static ItemStack getHeadFromEntity(LivingEntity target) {
		if (target.isBaby())
			return ItemStack.EMPTY;

		var recipeOptional = RecipeInit.BEHEADING_RECIPES.stream().filter(recipe -> recipe.matches(target.getType())).findFirst();
		if (recipeOptional.isPresent()) {
			return recipeOptional.get().getResultItem();
		}

		return ItemStack.EMPTY;
	}

	public EntityType<?> getSpawnEntity(ItemStack stack) {
		if (!stack.isEmpty()) {
			if (stack.getItem() instanceof SpawnEggItem) {
				EntityType<?> entitytype = ((SpawnEggItem) stack.getItem()).getType(stack.getTag());
				//WAMUtilsMod.LOGGER.debug("SpawnEntity: " + entitytype);
				return entitytype;
			}
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	public CompoundTag getEntityNBT(ItemStack stack) {
		if (!stack.isEmpty()) {
			if (stack.getItem() instanceof SpawnEggItem) {
				CompoundTag tag = stack.getTag() !=null ? stack.getTag().copy() : new CompoundTag();
				if(!tag.contains("EntityTag", 10)) {
					EntityType<?> entitytype = ((SpawnEggItem) stack.getItem()).getType(tag);
					CompoundTag entityTag = new CompoundTag();
					entityTag.putString("id", Registry.ENTITY_TYPE.getKey(entitytype).toString());
					return entityTag;
				}
				else {
					return tag.getCompound("EntityTag");					
				}	
			}
		}
		return null;
	}

	public boolean isItemAValidSpawnItem(ItemStack stack) {
		if (!stack.isEmpty()) {
			if (stack.getItem() instanceof SpawnEggItem) {
				return true;
			}
		}
		return false;
	}

	public boolean isItemAValidUpgradeItem(ItemStack stack) {
		if (!stack.isEmpty()) {
			if (stack.getItem() == ItemInit.UPGRADE_BEHEADING.get()) {
				return true;
			}
			if (stack.getItem() == ItemInit.UPGRADE_DAMAGE.get()) {
				return true;
			}
			if (stack.getItem() == ItemInit.UPGRADE_BOSS.get()) {
				return true;
			}
		}
		return false;
	}
	
	public void updateSpawnEntity() {
		ItemStack stack = this.inputItems.getStackInSlot(0);		
		CompoundTag tag = getEntityNBT(stack);
		if(tag !=null) {
			Entity entity = EntityType.loadEntityRecursive(tag, level, (p_151310_) -> {
				// p_151310_.moveTo(d0, d1, d2, p_151310_.getYRot(), p_151310_.getXRot());
				return p_151310_;
			});
			if (entity !=null && entity instanceof LivingEntity) {
				LivingEntity livingEntity = (LivingEntity) entity;
				this.clientRenderEntity = livingEntity;
				this.killProgress = 0;
				this.maxKillTime = (int)((float)livingEntity.getMaxHealth() * 20.0F);
				//sendSyncPacket(true);
				return;
			}
		}
		this.clientRenderEntity = null;	
		this.killProgress = 0;
		this.maxKillTime = 0;
		this.serverKillEntity = null;
		this.spawnDelay = 5 * 20;
		//sendSyncPacket(true);
	}
	
	public void updateUpgrades() {
		damageCost = 1;
		beheadingUpgradeCount = InventoryUtils.countItem(upgradeItems, ItemInit.UPGRADE_BEHEADING.get());
		damageUpgradeCount = InventoryUtils.countItem(upgradeItems, ItemInit.UPGRADE_DAMAGE.get());
		hasBossUpgrade = InventoryUtils.findItem(upgradeItems, ItemInit.UPGRADE_BOSS.get()) !=-1;
		
		//7437
		
		if(beheadingUpgradeCount > 0) {
			int maxUpgrades = Math.min(10, beheadingUpgradeCount);
			damageCost += maxUpgrades * 10; //5 Energy per upgrade
		}
		
		if(damageUpgradeCount > 0) {
			damageCost += (damageUpgradeCount * 5); //5 Energy per upgrade
		}
		
		if(hasBossUpgrade) {
			damageCost *= 20;
		}
		
		this.displayDamageCost = damageCost;
		//TODO Sync this
		setChanged();
	}
	
	@Nonnull
	private ItemStackHandler createInputItemHandler() {
		return new ItemStackHandler(INPUT_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				updateSpawnEntity();
				setChanged();
			}

			@Override
			public int getSlotLimit(int slot) {
				return 1;
			}

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return isItemAValidSpawnItem(stack);
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				if (!isItemAValidSpawnItem(stack)) {
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

	@Nonnull
	private ItemStackHandler createOutputItemHandler() {
		return new ItemStackHandler(OUTPUT_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				setChanged();
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
		return new CombinedInvWrapper(inputItems, upgradeItems, outputItems) {
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
		externalOutputItemHandler.invalidate();
		combinedAllItemHandler.invalidate();
	}

	@Override
	public void writeCustomNBT(CompoundTag tag, boolean descPacket) {
		//saveClientData(tag);
		tag.putInt("killProgress", killProgress);
		tag.putInt("maxKillTime", maxKillTime);
		tag.put("Inventory.Input", inputItems.serializeNBT());
		tag.put("Inventory.Upgrades", upgradeItems.serializeNBT());
		tag.put("Inventory.Output", outputItems.serializeNBT());
        energy.ifPresent(h -> tag.put("Energy", h.serializeNBT()));
	}

	private void saveClientData(CompoundTag tag) {
		CompoundTag infoTag = new CompoundTag();
		tag.put("Info", infoTag);
		infoTag.putBoolean("running", running);
		infoTag.putInt("killProgress", killProgress);
		infoTag.putInt("maxKillTime", maxKillTime);
		//infoTag.putInt("energyAmount", energyStorage.getEnergyStored());
		
		if(this.clientRenderEntity != null) {
			infoTag.put("RenderEntity", this.clientRenderEntity.serializeNBT());
		}
	}

	@Override
	public void readCustomNBT(CompoundTag tag, boolean descPacket) {
		//loadClientData(tag);
		killProgress = tag.getInt("killProgress");
		maxKillTime = tag.getInt("maxKillTime");
		if (tag.contains("Inventory.Input")) {
			inputItems.deserializeNBT(tag.getCompound("Inventory.Input"));
		}
		if (tag.contains("Inventory.Upgrades")) {
			upgradeItems.deserializeNBT(tag.getCompound("Inventory.Upgrades"));
			updateUpgrades();
		}
		if (tag.contains("Inventory.Output")) {
			outputItems.deserializeNBT(tag.getCompound("Inventory.Output"));
		}
		energy.ifPresent(h -> h.deserializeNBT(tag.getCompound("Energy")));
	}

	private void loadClientData(CompoundTag tag) {
		if (tag.contains("Info")) {
			CompoundTag infoTag = tag.getCompound("Info");
			running = infoTag.getBoolean("running");
			killProgress = infoTag.getInt("killProgress");
			maxKillTime = infoTag.getInt("maxKillTime");
		}
		if(tag.contains("RenderEntity")) {
			CompoundTag compoundtag = tag.getCompound("RenderEntity");
            Optional<EntityType<?>> optional = EntityType.by(compoundtag);
            if(optional.isPresent()) {
            	Entity entity = EntityType.loadEntityRecursive(compoundtag, level, (p_151310_) -> {
            		return p_151310_;
            	});
            	if(entity !=null && entity instanceof LivingEntity livingEntity) {
                    this.clientRenderEntity = livingEntity;
            	}
            }
		}
	}
	
	protected void sendSyncPacket(boolean includeEntity)
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putBoolean("running", running);
		nbt.putInt("killProgress", killProgress);
		nbt.putInt("maxKillTime", maxKillTime);
		if(this.clientRenderEntity != null && includeEntity) {
			nbt.put("RenderEntity", this.clientRenderEntity.serializeNBT());
		}
		WAMUtilsMod.packetHandler.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)),
				new MessageBlockEntityUpdate(this, nbt));
	}
	
	@Override
	public void receiveMessageFromServer(CompoundTag message)
	{
		running = message.getBoolean("running");
		killProgress = message.getInt("killProgress");
		maxKillTime = message.getInt("maxKillTime");
		if(message.contains("RenderEntity")) {
			CompoundTag compoundtag = message.getCompound("RenderEntity");
            Optional<EntityType<?>> optional = EntityType.by(compoundtag);
            if(optional.isPresent()) {
            	Entity entity = EntityType.loadEntityRecursive(compoundtag, level, (p_151310_) -> {
            		return p_151310_;
            	});
            	if(entity !=null && entity instanceof LivingEntity livingEntity) {
                    this.clientRenderEntity = livingEntity;
            	}
            }
		}
	}
	
	@NotNull
	@Override
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) {
			if (side == null) {
				return combinedAllItemHandler.cast();
			} else if (side == Direction.DOWN) {
				return externalOutputItemHandler.cast();
			} else if (side == Direction.UP) {
				return inputItemHandler.cast();
			} else {
				return upgradeItemHandler.cast();
			}
		} 
		else if (cap == ForgeCapabilities.ENERGY) {
            return energy.cast();
		}
		else {
			return super.getCapability(cap, side);
		}
	}

	//ENERGY
	
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

	@Override
	public int getEnergyCapacity() {
		//TODO Make this configurable
		return 500000;
	}

	@Override
	public int getMaxEnergyInput() {
		return 1000 + (damageCost * 2);
	}

	@Override
	public int getMaxEnergyOutput() {
		return 0;
	}

}
