package alec_wam.wam_utils.common.entities.workers;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;

import alec_wam.wam_utils.client.render.entities.WorkerWorldRenderer;
import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager.JobType;
import alec_wam.wam_utils.common.entities.workers.jobs.WorkerJob;
import alec_wam.wam_utils.common.helpers.BlockHelper;
import alec_wam.wam_utils.common.helpers.EntityHelper;
import alec_wam.wam_utils.common.helpers.ItemHelper;
import alec_wam.wam_utils.common.items.WorkerStaffItem;
import alec_wam.wam_utils.network.SyncWorkerJobPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

public class WorkerEntity extends PathfinderMob implements InventoryCarrier, OwnableEntity {
	private static final Logger LOGGER = Logger.getLogger(WorkerEntity.class.getName());

	public static enum ExternalInventoryStatus implements StringRepresentable {
		NONE, UNLOAD, EXTRACT_ITEM;
		
		public static final StringRepresentable.EnumCodec<ExternalInventoryStatus> CODEC = StringRepresentable.fromEnum(ExternalInventoryStatus::values);
        private static final IntFunction<ExternalInventoryStatus> BY_ID = ByIdMap.continuous(ExternalInventoryStatus::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<ByteBuf, ExternalInventoryStatus> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, ExternalInventoryStatus::ordinal);

		@Override
		public String getSerializedName() {
			return name();
		}

        public static ExternalInventoryStatus byId(int id) {
            return BY_ID.apply(id);
        }
	}

	public static record ItemStackRequest(ItemStack stack, int max) {
		public ItemStackRequest(ItemStack stack) {
			this(stack, stack.getMaxStackSize());
		}
	}
	
	public static final String NBT_JOB = "Job";
	public static final String NBT_EXTERNAL_INVENTORY = "ExternalInventorySettings";
	public static final int FOOD_SCAN_DELAY_SECONDS = 10;
	public static final int TOOL_SCAN_DELAY_SECONDS = 10;
	public static final int ARMOR_SCAN_DELAY_SECONDS = 20;
	private static final EquipmentSlot[] ARMOR_EQUIP_ORDER = new EquipmentSlot[] {EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.HEAD, EquipmentSlot.FEET};
	
	
	private boolean searchingForLand = false;
	protected final WaterBoundPathNavigation waterNavigation;
	protected final GroundPathNavigation groundNavigation;
	

    protected Vec3 deltaMovementOnPreviousTick = Vec3.ZERO;
    public float walkDistO;
    public float walkDist;
    
    private WorkerJob job;
	private boolean jobDirty = false;
	private WorkerInventorySettings externalInventorySettings;
	public Pair<Direction, ItemStackRequest> extractionStack;
	private int inventoryInteractDelay = 0;	
	private Container openContainer = null;
	private int openContainerLength = 0;
	private boolean doneWithOpenContainer = false;

	private int toolScanDelay = 0;
	private int foodScanDelay = 0;
	private int armorScanDelay = 0;
	
	private final SimpleContainer inventory = new SimpleContainer(9);	
	
	public static final EntityDataAccessor<ExternalInventoryStatus> INVENTORY_STATUS = SynchedEntityData.defineId(WorkerEntity.class, ModInit.WORKER_EXTERNAL_INVENTORY_STATUS.get());
	protected static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> DATA_OWNERUUID_ID = SynchedEntityData.defineId(
        WorkerEntity.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE
    );

	protected static final ImmutableList<? extends SensorType<? extends Sensor<? super WorkerEntity>>> SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS);
	protected static final ImmutableList<? extends MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.DOORS_TO_CLOSE, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.LOOK_TARGET, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,MemoryModuleType.PATH, MemoryModuleType.ATTACK_TARGET);
	
	public WorkerEntity(EntityType<WorkerEntity> type, Level level) {
		super(type, level);
		this.moveControl = new SwimmingMoveControl(this);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
		this.setPathfindingMalus(PathType.WATER, 0.0F);		
        this.setPathfindingMalus(PathType.DANGER_POWDER_SNOW, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGE_CAUTIOUS, -1.0F);
		this.setPathfindingMalus(PathType.WATER_BORDER, 8.0F);
		this.waterNavigation = new WaterBoundPathNavigation(this, level);
		this.groundNavigation = new GroundPathNavigation(this, level);
		this.groundNavigation.setCanFloat(true);
		this.groundNavigation.setCanOpenDoors(true);
	}
	
	@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(INVENTORY_STATUS, ExternalInventoryStatus.NONE);
        builder.define(DATA_OWNERUUID_ID, Optional.empty());
    }
	
	@Override
	protected Brain.Provider<WorkerEntity> brainProvider() {
		return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
	}

	@Override
	protected Brain<?> makeBrain(Dynamic<?> p_34221_) {
		Brain<WorkerEntity> brain = this.brainProvider().makeBrain(p_34221_);
		initCoreActivity(brain);
		brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
		brain.setDefaultActivity(Activity.CORE);
		brain.useDefaultActivity();
		return brain;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Brain<WorkerEntity> getBrain() {
		return (Brain<WorkerEntity>) super.getBrain();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, (double) 0.3F).add(Attributes.MAX_HEALTH, 20.0D)
				.add(Attributes.ATTACK_DAMAGE, 1.0D)
				.add(Attributes.SCALE, 1.0)
				.add(Attributes.STEP_HEIGHT, 1.0);
	}
	
	public static void initCoreActivity(Brain<WorkerEntity> p_34821_) {
		p_34821_.addActivity(Activity.CORE, 0, ImmutableList.of(new LookAtTargetSink(45, 90), new MoveToTargetSink(),
				InteractWithDoor.create()));
	}	
	
	public ExternalInventoryStatus getExternalInventoryStatus() {
		return this.getEntityData().get(INVENTORY_STATUS);
	}
	
	public void setExternalInventoryStatus(ExternalInventoryStatus status) {
		synchronized (this) {
			this.getEntityData().set(INVENTORY_STATUS, status);
		}
	}
	
	@Override
	public void readAdditionalSaveData(CompoundTag tag) 
	{
	    super.readAdditionalSaveData(tag);
		
		if (tag.contains(NBT_JOB)) {
			WorkerJob job = JobManager.loadFromTag(this, tag.getCompoundOrEmpty(NBT_JOB));
			this.setJob(job);
		}

		EntityReference<LivingEntity> entityreference = EntityReference.readWithOldOwnerConversion(tag, "Owner", this.level());
        if (entityreference != null) {
            try {
                this.entityData.set(DATA_OWNERUUID_ID, Optional.of(entityreference));
            } catch (Throwable throwable) {
				LOGGER.log(java.util.logging.Level.WARNING, "Failed to set owner UUID for WorkerEntity {}: {}", new Object[]{this.getUUID(), throwable.getMessage()});
				this.entityData.set(DATA_OWNERUUID_ID, Optional.empty());
            }
        } else {
            this.entityData.set(DATA_OWNERUUID_ID, Optional.empty());
        }

        this.readInventoryFromTag(tag, this.registryAccess());
        
        this.inventoryInteractDelay = tag.getIntOr("InventoryInteractDelay", 0);
        
        this.externalInventorySettings = tag.read(NBT_EXTERNAL_INVENTORY, WorkerInventorySettings.CODEC).orElse(null);
    }
	
	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if(this.job !=null) {
        	tag.put(NBT_JOB, JobManager.saveToTag(job));
        }

		EntityReference<LivingEntity> entityreference = this.getOwnerReference();
        if (entityreference != null) {
            entityreference.store(tag, "Owner");
        }

        this.writeInventoryToTag(tag, this.registryAccess());
        
        tag.putInt("InventoryInteractDelay", this.inventoryInteractDelay);
        
        if(this.externalInventorySettings !=null) {
        	tag.put(NBT_EXTERNAL_INVENTORY, WorkerInventorySettings.CODEC.encodeStart(NbtOps.INSTANCE, externalInventorySettings).getOrThrow());
        }
    }
	
	public SyncWorkerJobPayload buildSyncJobPayload() {
		Optional<CompoundTag> jobData = Optional.empty();
		
		if(job != null) {
			jobData = Optional.of(JobManager.saveToTag(job));
		}
		
		return new SyncWorkerJobPayload(this.getId(), jobData);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		boolean isClient = this.level().isClientSide;
		ItemStack stack = player.getItemInHand(hand);
		if(!stack.isEmpty()) {
			if(stack.is(ModInit.WORKER_STAFF_ITEM.get())) {
				JobType type = WorkerStaffItem.getJobType(stack);
				if(type !=null) {
					if(this.job == null) {
						WorkerJob newJob = type.getJobFactory().create(this, stack, player);
						if(newJob !=null) {
							this.setJob(newJob);
							if(!isClient) {
								player.displayClientMessage(Component.translatable("wamutils.message.workers.staff.give_job", type.name()), true);								
//								SyncWorkerJobPayload payload = this.buildSyncJobPayload();
//								PacketDistributor.sendToPlayersTrackingEntity(this, payload);
							}
							return isClient ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
						}
						return InteractionResult.PASS;
					} else {
						this.job.stop();
						this.setJob(null);
						if(!isClient) {
							player.displayClientMessage(Component.translatable("wamutils.message.workers.staff.remove_job", type.name()), true);							
							SyncWorkerJobPayload payload = this.buildSyncJobPayload();
							PacketDistributor.sendToPlayersTrackingEntity(this, payload);
						}
						return isClient ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
					}
				}
			}
			
//			if(stack.is(ItemInit.MINION_SKIN.get())) {
//				MinionSkinSettings settings = MinionSkinItem.loadFromStack(stack);
//				if(settings !=null) {
//					this.setSkinSettings(settings);
//					return InteractionResult.sidedSuccess(this.level().isClientSide);
//				}
//				return InteractionResult.PASS;
//			}
			
			if(stack.is(ModInit.WORKER_INVENTORY_ITEM.get())) {
				WorkerInventorySettings settings = stack.get(ModInit.WORKER_INVENTORY_SETTINGS_DATA_COMPONENT);
				if(settings !=null) {
					this.externalInventorySettings = settings;
					return isClient ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
				}
				return InteractionResult.PASS;
			}
			
			if(stack.is(Items.STICK)) {
				final boolean oldCrouch = this.isCrouching();
				this.setPose(oldCrouch ? Pose.STANDING : Pose.CROUCHING);
				if(this.getOwner() == null){
					this.setOwner(player);
				}
				return isClient ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
			}
			
			if(stack.is(Items.HOPPER)) {
//				this.startUnloadingInventory(true);
				
				if(!this.level().isClientSide) {
					this.lookAt(Anchor.EYES, Anchor.EYES.apply(player));
					EntityHelper.throwItemsTowardEntity(this, player, hand, this.inventory.getItems());
					this.inventory.clearContent();
				}
				
				return isClient ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
			}
			
			if(stack.has(DataComponents.EQUIPPABLE)) {
				Equippable equipable = stack.get(DataComponents.EQUIPPABLE);
				if(stack.canEquip(equipable.slot(), this)) {
					if(!isClient) {
						this.equipItemIfPossible((ServerLevel)this.level(), stack);
					}
					
					return isClient ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
				}
			}
			
			if(ItemHelper.isTool(stack) || ItemHelper.isAxe(stack)) {
				if(!isClient) {
					if(!this.getMainHandItem().isEmpty()) {
						EntityHelper.throwItemsTowardEntity(this, player, hand, Collections.singletonList(getMainHandItem()));
						this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
					}
					this.setItemInHand(InteractionHand.MAIN_HAND, stack);
				}
				
				return isClient ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
			}
			
//			if(stack.is(Items.SADDLE)) {
//				double range = 3.0F;
//				AbstractHorse horse = this.level().getNearestEntity(AbstractHorse.class, TargetingConditions.forNonCombat(), this, this.getX(), this.getY(), this.getZ(), getBoundingBox().inflate(range, range, range));
//				if(horse !=null && horse.isSaddled() && horse.getControllingPassenger() == null && this.getVehicle() == null) {
//					if(!this.level().isClientSide) {
//						horse.setEating(false);
//						horse.setStanding(false);
//						this.setYRot(horse.getYRot());
//						this.setXRot(horse.getXRot());
//						this.startRiding(horse);
//					}
//					return InteractionResult.sidedSuccess(this.level().isClientSide);
//				}
//			}
			
			if(stack.is(Items.REDSTONE)) {
				if(!isClient) {
					getBrain().setMemory(MemoryModuleType.LOOK_TARGET, Optional.empty());
					getBrain().setMemory(MemoryModuleType.WALK_TARGET, Optional.empty());
				}
				return isClient ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER; 
			}
			
			if(stack.is(Items.GLOWSTONE_DUST)) {
				boolean isClientSide = this.level().isClientSide;
				boolean visible = true;
				if(!isClientSide) {					
					if(visible) {
						player.displayClientMessage(Component.translatable("wamutils.message.workers.show_job"), true);
					}
					else {
						player.displayClientMessage(Component.translatable("wamutils.message.workers.hide_job"), true);
					}					
					ServerPlayer serverPlayer = (ServerPlayer)player;					
					SyncWorkerJobPayload payload = this.buildSyncJobPayload();
					PacketDistributor.sendToPlayer(serverPlayer, payload);
				}
				else {
					WorkerWorldRenderer.toggleJobVisibility(this);
				}
				
				return isClient ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
			}
		}
		
		//Open Inventory
//		if (!this.level().isClientSide) {
//			NetworkHooks.openScreen((ServerPlayer)player, this, packetBuffer -> {
//                packetBuffer.writeUUID(this.getUUID());
//            });
//		}
		return isClient ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}
	
    @Override
    public SimpleContainer getInventory() {
        return this.inventory;
    }
	
	@Override
	public boolean removeWhenFarAway(double p_21542_) {
		return false;
	}

	@Override
	protected boolean shouldDespawnInPeaceful() {
		return false;
	}
	
	@Override
	public void checkDespawn() {
	}
	
	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(0, new FloatWhenNotSwimming(this));
		this.goalSelector.addGoal(8, new WorkerEntity.SwimOutOfWater(this, 1.0D));
	}
	
	@Override
	public boolean isBaby() {
		return false;
	}
	
	@Override
	public boolean canPickUpLoot() {
		return false;
	}
	
	//Swimming
	
	public void setSearchingForLand(boolean value) {
		this.searchingForLand = value;
	}
	
	@Override
	public boolean isVisuallySwimming() {      
		return super.isVisuallySwimming() || isSwimming();
	}
	
	@Override
	public boolean canDrownInFluidType(FluidType type) {
		return false;
	}
	
	@Override
	public double getFluidJumpThreshold() {
		return (double) 0.2D;
	}
	
	public boolean wantsToSwim() {
		if (this.searchingForLand) {
			return true;
		} 
		LivingEntity livingentity = this.getTarget();
		return livingentity != null && livingentity.isInWater();
	}
	
	@SuppressWarnings("deprecation")
	public boolean isWorkerUnderWater() {
		return this.getFluidHeight(FluidTags.WATER) > this.getFluidJumpThreshold();
	}

    @Override
    public void tick() {
        this.walkDistO = this.walkDist;
        this.deltaMovementOnPreviousTick = this.getDeltaMovement();
        super.tick();
		if (!level().isClientSide) {
	         if(this.jobDirty) {
				SyncWorkerJobPayload payload = this.buildSyncJobPayload();
				PacketDistributor.sendToPlayersTrackingEntity(this, payload);
	        	this.jobDirty = false;
	         }
		}
    }	
	
	@Override
	public void aiStep() {
		super.aiStep();		
		
		if(!level().isClientSide) {
			if(this.fallDistance > 3.5 && !this.isFallFlying()) {
				this.tryToStartFallFlying();
			}
			updateInteractExternalInventory();
			updateContainerLogic();
		}	
		this.updateSwingTime();
		
		if(toolScanDelay > 0) {
			toolScanDelay--;
		}	
		if(foodScanDelay > 0) {
			foodScanDelay--;
		}
		if(armorScanDelay > 0) {
			armorScanDelay--;
		}
		tickJobs();
		
//		if((this.job == null || !this.job.blockEating()) && foodData.needsFood()) {
//			if(!level().isClientSide) {
//				startEating();
//			}
//		}
//		
//		//TODO Tick Special MobEffects (Hunger and Saturation)
//		if(this.isEating) {
//			this.eatFood();
//		}
	}
	
	public boolean tryToStartFallFlying() {

		if(this.canGlide()) {
			this.startFallFlying();
			return true;
		}

		return false;
	}

	public void startFallFlying() {
		this.setSharedFlag(7, true);
	}
	
	@Override
	public void updateSwimming() {
		if (!this.level().isClientSide) {
			if (this.isEffectiveAi() && this.isInWater() && this.wantsToSwim()) {
				this.navigation = this.waterNavigation;
				this.setSwimming(true);
			} else {
				this.navigation = this.groundNavigation;
				this.setSwimming(false);
			}
		}
	}
	
	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		Profiler.get().push("minionBrain");
		this.getBrain().tick(level, this);
		Profiler.get().pop();
	}
	
	public void tickJobs() {
		Profiler.get().push("minion_jobs");

		if (this.job != null) {
			if (job.canKeepRunning()) {
				job.run();
			} 
			//TODO Handle Stop
		}

		Profiler.get().pop();
	}
	
	public void setJob(WorkerJob job) {
		this.job = job;
		this.jobDirty = true;
	}
	
	public WorkerJob getJob() {
		return job;
	}
	
	@Override
	public boolean isPushedByFluid() {
		return !this.isSwimming();
	}
	
	@Override
	protected void hurtArmor(DamageSource pDamageSource, float pDamage) {
		super.hurtArmor(pDamageSource, pDamage);
		if (!(pDamage <= 0.0F)) {
			pDamage /= 4.0F;
			if (pDamage < 1.0F) {
				pDamage = 1.0F;
			}

			for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
				this.doHurtEquipment(pDamageSource, pDamage, slot);
			}
		}
	}

	@Override
	protected void hurtHelmet(DamageSource pDamageSource, float pDamageAmount) {
		if (!(pDamageAmount <= 0.0F)) {
			pDamageAmount /= 4.0F;
			if (pDamageAmount < 1.0F) {
				pDamageAmount = 1.0F;
			}

			this.doHurtEquipment(pDamageSource, pDamageAmount, EquipmentSlot.HEAD);
		}
	}
	
	@Override
	protected boolean shouldDropLoot() {
		return true;
	}
	
	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean p_21387_) {
		super.dropCustomDeathLoot(level, damageSource, p_21387_);
		for(EquipmentSlot equipmentslot : EquipmentSlot.values()) {
	         ItemStack itemstack = this.getItemBySlot(equipmentslot);
	         if (!itemstack.isEmpty() && !EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
	        	 this.spawnAtLocation(level, itemstack);
	             this.setItemSlot(equipmentslot, ItemStack.EMPTY);
	         }
		}
		
		this.inventory.removeAllItems().forEach((itemstack) -> this.spawnAtLocation(level, itemstack));
	}
	
	public void dropItemFromBrokenBlock(BlockPos pos, ItemStack stack) {
		this.dropItemFromBrokenBlock(pos, stack, true, false);
	}
	
	public void dropItemFromBrokenBlock(BlockPos pos, ItemStack stack, boolean inv, boolean autoUnload) {
		ItemStack remainder = inv ? addToInventory(stack) : stack;
		if (remainder.isEmpty())
			return;
		
		if(autoUnload) {
			if(this.hasItemsToUnload()) {
				this.startUnloadingInventory(false);
			}
		}

		Vec3 dropPos = BlockHelper.getCenterOf(pos);
//		float distance = (float) dropPos.distanceTo(this.position());
		ItemEntity entity = new ItemEntity(level(), dropPos.x, dropPos.y, dropPos.z, remainder);
		entity.setDefaultPickUpDelay();
		entity.setDeltaMovement(Vec3.ZERO);
		level().addFreshEntity(entity);
	}

	
	public ItemStack addToInventory(ItemStack stack) {
		ItemStack remainder = this.inventory.addItem(stack);
		if(!remainder.isEmpty()) {
			if(EntityHelper.canFitItemInHand(this, remainder, InteractionHand.MAIN_HAND)) {
				return EntityHelper.addItemToHand(this, remainder, InteractionHand.MAIN_HAND);
			}
		}
		return remainder;
	}

	public boolean canAddToInventory(ItemStack stack) {
		if(this.inventory.canAddItem(stack)) {
			return true;
		}
		if(EntityHelper.canFitItemInHand(this, stack, InteractionHand.MAIN_HAND)) {
			return true;
		}
		return false;
	}	
	
	@Override
	public void pickUpItem(ServerLevel level, ItemEntity itemEntity) {
		ItemStack itemstack = itemEntity.getItem();
		ItemStack itemstack1 = addToInventory(itemstack.copy());
		if (itemstack1.getCount() != itemstack.getCount()) {
			onItemPickup(itemEntity);
			take(itemEntity, itemstack.getCount() - itemstack1.getCount());
			itemstack.setCount(itemstack1.getCount());
			if (itemstack.isEmpty()) {
				itemEntity.discard();
			}
		}
	}
	
	public boolean silentPickupItem(ItemEntity itemEntity) {
		ItemStack itemstack = itemEntity.getItem();
		ItemStack itemstack1 = addToInventory(itemstack.copy());
		if (itemstack1.getCount() != itemstack.getCount()) {
			itemstack.setCount(itemstack1.getCount());
			if (itemstack.isEmpty()) {
				itemEntity.discard();
				return true;
			}
		}
		return false;
	}
	
	public void holdInMainHand(ItemStack stack) {
		this.setItemSlotAndDropWhenKilled(EquipmentSlot.MAINHAND, stack);
	}

	public void holdInOffHand(ItemStack stack) {
		this.setItemSlotAndDropWhenKilled(EquipmentSlot.OFFHAND, stack);
	}
	
	public void swapToHand(ItemStack stack) {
		if(ItemStack.isSameItemSameComponents(stack, this.getMainHandItem()))return;
		
		int index = ItemHelper.findSlot(getInventory(), stack);
		if(!stack.isEmpty() && index < 0)return;
		
		ItemStack handStack = this.getMainHandItem().copy();
		this.holdInMainHand(ItemStack.EMPTY);
		
		if(!stack.isEmpty()) {
			ItemStack otherStack = stack.copy();
			this.holdInMainHand(otherStack);		
			this.getInventory().setItem(index, ItemStack.EMPTY);
		}
		
		putInInventory(handStack);		
	}	
	
	public void swapEquipment(ItemStack stack, EquipmentSlot slot) {
		ItemStack currentStack = this.getItemBySlot(slot);
		if(ItemStack.isSameItemSameComponents(stack, currentStack))return;
		
		int index = ItemHelper.findSlot(getInventory(), stack);
		if(!stack.isEmpty() && index < 0)return;
		
		ItemStack copyStack = currentStack.copy();
		this.setItemSlot(slot, ItemStack.EMPTY);
		
		if(!stack.isEmpty()) {
			ItemStack otherStack = stack.copy();
			this.setItemSlot(slot, otherStack);
			this.getInventory().setItem(index, ItemStack.EMPTY);
		}
		
		putInInventory(copyStack);		
	}
	
	public boolean swapToBestTool(BlockPos pos, boolean ignoreEmptyHand) {
		if(toolScanDelay > 0) {
			return false;
		}
		if(this.getExternalInventoryStatus() != ExternalInventoryStatus.NONE)return false;
		if(pos == null)return false;
//		System.out.println("Swap to tool");
		Level level = level();
		if(!level.isClientSide) {
			BlockPos breakingPos = pos;
			BlockState stateToBreak = level.getBlockState(breakingPos);
			ItemStack hand = this.getMainHandItem();
			
			if(!ItemHelper.isCorrectToolForState(hand, stateToBreak) || (ignoreEmptyHand && hand.isEmpty())) {				
//				System.out.println("Needs Tool");
				ItemStack bestTool = ItemHelper.findBestTool(this, stateToBreak);				
				
				if(bestTool.isEmpty()) {			
					//TODO Check into making this more efficient for things like harvesting
//					System.out.println("Scanning for Tool");		
					Pair<Direction, ItemStack> foundStack = this.findToolInExternalInventory(stateToBreak);
					if(foundStack !=null && foundStack.getFirst() !=null && foundStack.getSecond() !=null && !foundStack.getSecond().isEmpty()) {
						ItemStack checkStack = foundStack.getSecond().copyWithCount(1);
						if(this.canAddToInventory(checkStack)) {
							ItemStackRequest request = new ItemStackRequest(foundStack.getSecond());
							this.startExtractingFromInventory(new Pair<Direction,WorkerEntity.ItemStackRequest>(foundStack.getFirst(), request));
							return true;
						}
						else {
							if(this.hasItemsToUnload()) {
								if(this.startUnloadingInventory(true)) {
									return true;
								}
							}
						}
					}

					//Only swap tools to "Empty Hand" if the inventory has room
					if(this.inventory.canAddItem(hand)) {
						swapToHand(bestTool);
					}
					
					this.toolScanDelay = TOOL_SCAN_DELAY_SECONDS * 20;
				}
				else {
					swapToHand(bestTool);
				}
			}
		}
		return false;
	}
	
	public boolean swapToBestMelee() {
		if(toolScanDelay > 0) {
			return false;
		}
		if(this.getExternalInventoryStatus() != ExternalInventoryStatus.NONE)return false;
		Level level = level();
		if(!level.isClientSide) {
			ItemStack hand = this.getMainHandItem();
			
			//TODO Swap to better one if found
			if(!ItemHelper.isMeleeWeapon(hand)) {				
				ItemStack bestTool = ItemHelper.findBestMelee(this);				
				
				if(bestTool.isEmpty()) {	
					System.out.println("Scanning for Melee");					
					Pair<Direction, ItemStack> foundStack = this.findMeleeInExternalInventory();
					if(foundStack !=null && foundStack.getFirst() !=null && foundStack.getSecond() !=null && !foundStack.getSecond().isEmpty()) {
						ItemStack checkStack = foundStack.getSecond().copyWithCount(1);
						if(this.canAddToInventory(checkStack)) {
							ItemStackRequest request = new ItemStackRequest(foundStack.getSecond());
							this.startExtractingFromInventory(new Pair<Direction,WorkerEntity.ItemStackRequest>(foundStack.getFirst(), request));
							return true;
						}
						else {
							if(this.hasItemsToUnload()) {
								if(this.startUnloadingInventory(true)) {
									return true;
								}
							}
						}
					}

					//Only swap tools to "Empty Hand" if the inventory has room
					if(this.inventory.canAddItem(hand)) {
						swapToHand(bestTool);
					}
					
					this.toolScanDelay = TOOL_SCAN_DELAY_SECONDS * 20;
				}
				else {
					swapToHand(bestTool);
				}
			}
		}
		return false;
	}
	
	public boolean swapToItem(Predicate<ItemStack> searchStack) {
		return swapToItem(searchStack, Optional.empty(), ItemHelper.SORT_STACK_SIZE);
	}

	public boolean swapToItem(Predicate<ItemStack> searchStack, Optional<Integer> maxSize) {
		return swapToItem(searchStack, maxSize, null);
	}

	public boolean swapToItem(Predicate<ItemStack> searchStack, @Nullable Comparator<ItemStack> sorter) {
		return swapToItem(searchStack, Optional.empty(), sorter);
	}
	
	public boolean swapToItem(Predicate<ItemStack> searchStack, Optional<Integer> maxSize, @Nullable Comparator<ItemStack> sorter) {
		Level level = level();
		if(!level.isClientSide) {
			ItemStack hand = this.getMainHandItem();
			if(!searchStack.test(hand)) {
				ItemStack invItem = ItemHelper.findItem(this, searchStack, sorter);
				
				if(invItem.isEmpty()) {
					Pair<Direction, ItemStack> foundStack = this.findItemInExternalInventory(searchStack, sorter);
					if(foundStack !=null && foundStack.getFirst() !=null && foundStack.getSecond() !=null && !foundStack.getSecond().isEmpty()) {
						ItemStack checkStack = foundStack.getSecond().copyWithCount(1);
						if(this.canAddToInventory(checkStack)) {
							ItemStackRequest request = maxSize.isPresent() ? new ItemStackRequest(foundStack.getSecond(), maxSize.get()) : new ItemStackRequest(foundStack.getSecond());
							this.startExtractingFromInventory(new Pair<Direction,WorkerEntity.ItemStackRequest>(foundStack.getFirst(), request));
							return true;
						}
						else {
							if(this.hasItemsToUnload()) {
								if(this.startUnloadingInventory(true)) {
									return true;
								}
							}
						}
					}
				}
				else {
					swapToHand(invItem);
				}
			}
		}
		return false;
	}
	
	public boolean requireItem(Predicate<ItemStack> searchStack) {
		return requireItem(searchStack, Optional.empty(), ItemHelper.SORT_STACK_SIZE);
	}

	public boolean requireItem(Predicate<ItemStack> searchStack, Optional<Integer> maxSize) {
		return requireItem(searchStack, maxSize, null);
	}

	public boolean requireItem(Predicate<ItemStack> searchStack, @Nullable Comparator<ItemStack> sorter) {
		return requireItem(searchStack, Optional.empty(), sorter);
	}
	
	public boolean requireItem(Predicate<ItemStack> searchStack, Optional<Integer> maxSize, @Nullable Comparator<ItemStack> sorter) {
		Level level = level();
		if(!level.isClientSide) {
			ItemStack invItem = ItemHelper.findItem(this, searchStack, sorter);
			
			if(invItem.isEmpty()) {
				Pair<Direction, ItemStack> foundStack = this.findItemInExternalInventory(searchStack, sorter);
				if(foundStack !=null && foundStack.getFirst() !=null && foundStack.getSecond() !=null && !foundStack.getSecond().isEmpty()) {
					ItemStack checkStack = foundStack.getSecond().copyWithCount(1);
					if(this.canAddToInventory(checkStack)) {
						ItemStackRequest request = maxSize.isPresent() ? new ItemStackRequest(foundStack.getSecond(), maxSize.get()) : new ItemStackRequest(foundStack.getSecond());
						this.startExtractingFromInventory(new Pair<Direction,WorkerEntity.ItemStackRequest>(foundStack.getFirst(), request));
						return false;
					}
					else {
						if(this.hasItemsToUnload()) {
							if(this.startUnloadingInventory(true)) {
								return false;
							}
						}
					}
				}
			}
			return !invItem.isEmpty();
		}
		return false;
	}

	public boolean checkForItem(Predicate<ItemStack> searchStack) {
		Level level = level();
		if(!level.isClientSide) {
			ItemStack invItem = ItemHelper.findItem(this, searchStack, null);			
			if(invItem.isEmpty()) {
				Pair<Direction, ItemStack> foundStack = this.findItemInExternalInventory(searchStack, null);
				if(foundStack !=null && foundStack.getFirst() !=null && foundStack.getSecond() !=null && !foundStack.getSecond().isEmpty()) {
					ItemStack checkStack = foundStack.getSecond().copyWithCount(1);
					if(this.canAddToInventory(checkStack)) {
						return true;
					}
				}
			}
			return !invItem.isEmpty();
		}
		return false;
	}
	
	public void equipArmor() {
		if(this.armorScanDelay > 0)return;
		if(this.getExternalInventoryStatus() != ExternalInventoryStatus.NONE)return;
		
		boolean swappedItem = false;
		for(EquipmentSlot slot : ARMOR_EQUIP_ORDER) {
			ItemStack stack = this.getItemBySlot(slot);
			if(stack.isEmpty()) {
				ItemStack bestArmor = ItemHelper.findBestArmor(this, slot);
				
				if(bestArmor.isEmpty()) {	
					System.out.println("Scanning for Armor " + slot);					
					Pair<Direction, ItemStack> foundStack = this.findArmorInExternalInventory(slot);
					if(foundStack !=null && foundStack.getFirst() !=null && foundStack.getSecond() !=null && !foundStack.getSecond().isEmpty()) {
						ItemStack checkStack = foundStack.getSecond().copyWithCount(1);
						if(this.canAddToInventory(checkStack)) {
							ItemStackRequest request = new ItemStackRequest(foundStack.getSecond());
							this.startExtractingFromInventory(new Pair<Direction,WorkerEntity.ItemStackRequest>(foundStack.getFirst(), request));
							return;
						}
						else {
							if(this.hasItemsToUnload()) {
								if(this.startUnloadingInventory(true)) {
									return;
								}
							}
						}
					}
				}
				else {
					swapEquipment(bestArmor, slot);
					swappedItem = true;
					this.armorScanDelay = 20; //Wait 1 second to get next piece
				}
			}
		}
		if(!swappedItem) {			
			this.armorScanDelay = ARMOR_SCAN_DELAY_SECONDS * 20;
		}
	}
	
	public void putInInventory(ItemStack stack) {
		ItemStack itemstack = addToInventory(stack);
		EntityHelper.throwItemsTowardRandomPos(this, InteractionHand.MAIN_HAND, Collections.singletonList(itemstack));
	}
	
	public WorkerInventorySettings getExternalInventorySettings() {
		return this.externalInventorySettings;
	}

	public void startOpenContainer(BlockPos pos, Container container) {
		if(this.openContainer != null) {
			LOGGER.info("Closing old container for worker");
			// Instantly close the old container
			this.stopOpenContainer();
		}
		if (this.level() instanceof ServerLevel serverLevel) {
			FakePlayer fp = EntityHelper.getFakePlayer(serverLevel, this.getOwnerUUID());
			if(container != null){
				fp.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
				LOGGER.info("Start open container for worker");
				container.startOpen(fp);
				this.openContainer = container;
				this.doneWithOpenContainer = false;
			}
		}
	}

	public void stopOpenContainer() {
		if (this.level() instanceof ServerLevel serverLevel) {
			FakePlayer fp = EntityHelper.getFakePlayer(serverLevel, this.getOwnerUUID());
			if(this.openContainer != null){
				LOGGER.info("Stopping open container for worker");
				this.openContainer.stopOpen(fp);
				this.openContainerLength = 0;
				this.openContainer = null;
			}
		}
	}

	public void updateContainerLogic() {
		// if(this.openContainer != null) {
		// 	this.openContainerLength++;
		// 	LOGGER.info("Open container length: " + this.openContainerLength);
		// }
		// if(this.doneWithOpenContainer) {
		// 	// Close the container after a certain time
		// 	if(this.openContainerLength >= 3 * 20) {
		// 		LOGGER.info("Auto closing container for worker");
		// 		this.stopOpenContainer();
		// 		this.openContainerLength = 0;
		// 		this.doneWithOpenContainer = false;
		// 	}
		// }
	}
	
	public boolean startUnloadingInventory(boolean force) {
		if(!force && this.getExternalInventoryStatus() != ExternalInventoryStatus.NONE)return false;
		if(this.getExternalInventorySettings() == null || !this.getExternalInventorySettings().hasInputFace())return false;
		this.setExternalInventoryStatus(ExternalInventoryStatus.UNLOAD);
		return true;
	}
	
	public void startExtractingFromInventory(Pair<Direction, ItemStackRequest> extractStack) {
		if(this.getExternalInventoryStatus() != ExternalInventoryStatus.NONE)return;
		if(this.getExternalInventorySettings() == null || !this.getExternalInventorySettings().hasOutputFace())return;
		this.setExternalInventoryStatus(ExternalInventoryStatus.EXTRACT_ITEM);
		this.extractionStack = extractStack;		
	}
	
	public void updateInteractExternalInventory() {
		if(this.getExternalInventoryStatus() == ExternalInventoryStatus.NONE)return;
		if(this.externalInventorySettings !=null) {
			GlobalPos pos = this.externalInventorySettings.getPos();
			
			if(pos !=null) {
				ResourceKey<Level> dimension = pos.dimension();
				if(!this.level().dimension().equals(dimension)) {
					cancelExternalInventory();
					return;
				}
				BlockPos invPos = pos.pos();
				
				double reachDistance = 2.0D;
				if(!EntityHelper.isCloseToBlockPos(this, invPos, reachDistance)) {
			        BlockPosTracker blockpostracker = new BlockPosTracker(invPos);
			        getBrain().setMemory(MemoryModuleType.LOOK_TARGET, blockpostracker);
					getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(blockpostracker, 1.0F, 1));
				}
				else {				
					BlockEntity blockEntity = level().getBlockEntity(invPos);
					if(blockEntity !=null) {
						BlockPosTracker blockpostracker = new BlockPosTracker(invPos);
						getBrain().setMemory(MemoryModuleType.LOOK_TARGET, blockpostracker);
				        //BlockPosTracker blockpostracker = new BlockPosTracker(invPos);
				        //getBrain().setMemory(MemoryModuleType.LOOK_TARGET, blockpostracker);
						
						if(inventoryInteractDelay > 0) {
							inventoryInteractDelay--;
							return;
						}
						
						if(this.getExternalInventoryStatus() == ExternalInventoryStatus.UNLOAD) {
							unloadInventory(invPos);
						}
						
						if(this.getExternalInventoryStatus() == ExternalInventoryStatus.EXTRACT_ITEM) {
							extractItemFromInventory(invPos);
						}
					}
					else {
						//Cancel unload
						cancelExternalInventory();
					}
				}
			}
		}
		else {
			//Prevent looping if there is no inventory
			cancelExternalInventory();
		}
	}
	
	private void cancelExternalInventory() {
		//Cancel unload
		getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
		getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
		this.setExternalInventoryStatus(ExternalInventoryStatus.NONE);
		this.inventoryInteractDelay = 0;
	}
	
	private void unloadInventory(BlockPos pos) {
		if(this.externalInventorySettings == null)return;
		int exportStackSize = 8;
		MutableBoolean exportedItem = new MutableBoolean(false);
		MutableBoolean foundItem = new MutableBoolean(false);
		
		List<Direction> inputSides = this.externalInventorySettings.getInputFaces();							
		if(!inputSides.isEmpty()) {
			ServerLevel serverLevel = (ServerLevel)this.level();
			BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
			
			// if(blockEntity !=null && blockEntity instanceof Container) {
			// 	this.startOpenContainer(pos, (Container)blockEntity);
			// }
			
			for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
				ItemStack stack = this.getInventory().getItem(i);
				if (!stack.isEmpty() && (this.job !=null ? !job.needsItem(stack) : true)) {
					foundItem.setTrue();
					if(exportItem(stack, pos, inputSides, exportStackSize)) {
						exportedItem.setTrue();
						break;
					}
				}
			}
			if(exportedItem.isFalse()) {
				ItemStack mainHandItem = this.getMainHandItem();
				if(!mainHandItem.isEmpty() && (this.job !=null ? !job.needsItem(mainHandItem) : true)) {
					foundItem.setTrue();
					if(exportItem(mainHandItem, pos, inputSides, exportStackSize)) {
						exportedItem.setTrue();
					}
				}
			}
			if(exportedItem.isFalse()) {
				ItemStack offHandItem = this.getOffhandItem();
				if(!offHandItem.isEmpty() && (this.job !=null ? !job.needsItem(offHandItem) : true)) {
					foundItem.setTrue();
					if(exportItem(offHandItem, pos, inputSides, exportStackSize)) {
						exportedItem.setTrue();
					}
				}
			}			
			
			// if(exportedItem.isTrue()) {				
			// 	if(this.openContainer != null) {
			// 		this.doneWithOpenContainer = true;
			// 	}
			// }
		}
		
		if(foundItem.isFalse()) {
//			System.out.println("Finished Unloading: No Items Left");
			this.setExternalInventoryStatus(ExternalInventoryStatus.NONE);
		}	
//		else if(foundInventory.isFalse()) {
//			System.out.println("Finished Unloading: No Valid Inventory");
//			this.setExternalInventoryStatus(ExternalInventoryStatus.NONE);
//		}
	}
	
	private boolean exportItem(ItemStack stack, BlockPos pos, List<Direction> inputSides, int exportStackSize) {
		for (Direction face : inputSides) {
			Optional<IItemHandler> opHandler = BlockHelper.getItemHandler(level(), pos, face);
			if (opHandler.isPresent()) {
				IItemHandler handler = opHandler.get();
				if (!BlockHelper.isFull(handler)) {					
					ItemStack insertCopy = stack.copyWithCount(Math.min(exportStackSize, stack.getCount()));
					final int oldSize = insertCopy.getCount();
					ItemStack insertedStack = BlockHelper.insertItemStacked(handler, insertCopy, false);
					stack.shrink(oldSize - insertedStack.getCount());
					if (insertedStack.getCount() != oldSize) {
						this.inventoryInteractDelay = 10;
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public void extractItemFromInventory(BlockPos pos) {
		if(this.extractionStack !=null) {
			Direction face = this.extractionStack.getFirst();
			if(face == null) {
				System.out.println("Finished Extracting: No Valid Face");
				this.setExternalInventoryStatus(ExternalInventoryStatus.NONE);
				return;
			}
			ItemStackRequest stackRequest = this.extractionStack.getSecond();
			if(stackRequest == null || stackRequest.stack.isEmpty()) {
				System.out.println("Finished Extracting: No Valid ItemStack");
				this.setExternalInventoryStatus(ExternalInventoryStatus.NONE);
				return;
			}
			ItemStack searchStack = stackRequest.stack.copyWithCount(stackRequest.max);

			Optional<IItemHandler> opHandler = BlockHelper.getItemHandler(level(), pos, face);
			if(opHandler.isPresent()) {
				IItemHandler handler = opHandler.get();
				for(int i = 0; i < handler.getSlots(); i++) {
					ItemStack slotStack = handler.getStackInSlot(i);
					if(ItemStack.isSameItemSameComponents(searchStack, slotStack)) {
						ItemStack extractedStack = handler.extractItem(i, searchStack.getCount(), true);
						if(!extractedStack.isEmpty() && this.canAddToInventory(extractedStack)) {
							final int preExtractSize = extractedStack.getCount();
							ItemStack remainder = this.addToInventory(extractedStack.copy());
							int realExtractSize = preExtractSize - remainder.getCount();
							handler.extractItem(i, realExtractSize, false);
							searchStack.shrink(realExtractSize);
							//TODO Make this a slow extract
							if(searchStack.isEmpty()) {
								this.extractionStack = null;
								this.setExternalInventoryStatus(ExternalInventoryStatus.NONE);								
								if(this.openContainer != null) {
									this.doneWithOpenContainer = true;
								}
								return;
							}
						}
					}
				}

				//No valid item found, so we need to cancel the extraction
				this.extractionStack = null;					

				System.out.println("Finished Extracting: No Valid Item Found");
				this.setExternalInventoryStatus(ExternalInventoryStatus.NONE);
				if(this.openContainer != null) {
					this.doneWithOpenContainer = true;
				}
			}
			else {
				System.out.println("Finished Extracting: No Valid Inventory");
				this.setExternalInventoryStatus(ExternalInventoryStatus.NONE);
			}
		}
	}
	
	public boolean hasItemsToUnload() {
		for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
			ItemStack stack = this.getInventory().getItem(i);
			if (!stack.isEmpty() && (this.job !=null ? !job.needsItem(stack) : true)) {
				return true;
			}
		}
		ItemStack mainHandItem = this.getMainHandItem();
		if(!mainHandItem.isEmpty() && (this.job !=null ? !job.needsItem(mainHandItem) : true)) {
			return true;
		}
		ItemStack offhandItem = this.getOffhandItem();
		if(!offhandItem.isEmpty() && (this.job !=null ? !job.needsItem(offhandItem) : true)) {
			return true;
		}
		return false;
	}
	
	public boolean needsArmor() {
		if(this.armorScanDelay > 0)return false;
		for (EquipmentSlot slot : ARMOR_EQUIP_ORDER) {
			ItemStack stack = this.getItemBySlot(slot);
			if (stack.isEmpty()) {
				return true;
			}
		}
		return false;
	}
	
	public Pair<Direction, ItemStack> findToolInExternalInventory(BlockState state) {
		if(this.externalInventorySettings != null) {
			GlobalPos globalPos = this.externalInventorySettings.getPos();
			
			if(globalPos == null)return Pair.of(null, ItemStack.EMPTY);
			BlockPos pos = globalPos.pos();
			List<Direction> outputSides = this.externalInventorySettings.getOutputFaces();							
			if(!outputSides.isEmpty()) {
				for(Direction face : outputSides) {
					Optional<IItemHandler> opHandler = BlockHelper.getItemHandler(level(), pos, face);
					if(opHandler.isPresent()) {
						ItemStack foundTool = ItemHelper.findBestTool(opHandler.get(), state);
						if(!foundTool.isEmpty()) {
							return Pair.of(face, foundTool);
						}
					}
				}
			}
		}
		return Pair.of(null, ItemStack.EMPTY);
	}
	
	public Pair<Direction, ItemStack> findMeleeInExternalInventory() {
		if(this.externalInventorySettings != null) {
			GlobalPos globalPos = this.externalInventorySettings.getPos();
			
			if(globalPos == null)return Pair.of(null, ItemStack.EMPTY);
			BlockPos pos = globalPos.pos();
			List<Direction> outputSides = this.externalInventorySettings.getOutputFaces();							
			if(!outputSides.isEmpty()) {
				for(Direction face : outputSides) {
					Optional<IItemHandler> opHandler = BlockHelper.getItemHandler(level(), pos, face);
					if(opHandler.isPresent()) {
						ItemStack foundTool = ItemHelper.findBestMelee(opHandler.get());
						if(!foundTool.isEmpty()) {
							return Pair.of(face, foundTool);
						}
					}
				}
			}
		}
		return Pair.of(null, ItemStack.EMPTY);
	}
	
	public Pair<Direction, ItemStack> findFoodInExternalInventory() {
		if(this.externalInventorySettings != null) {
			GlobalPos globalPos = this.externalInventorySettings.getPos();
			
			if(globalPos == null)return Pair.of(null, ItemStack.EMPTY);
			BlockPos pos = globalPos.pos();
			List<Direction> outputSides = this.externalInventorySettings.getOutputFaces();							
			if(!outputSides.isEmpty()) {
				for(Direction face : outputSides) {
					Optional<IItemHandler> opHandler = BlockHelper.getItemHandler(level(), pos, face);
					if(opHandler.isPresent()) {
						ItemStack foundFood = ItemHelper.findBestFood(opHandler.get(), this);
						if(!foundFood.isEmpty()) {
							return Pair.of(face, foundFood);
						}
					}
				}
			}
		}
		return Pair.of(null, ItemStack.EMPTY);
	}
	
	public Pair<Direction, ItemStack> findArmorInExternalInventory(EquipmentSlot slot) {
		if(this.externalInventorySettings != null) {
			GlobalPos globalPos = this.externalInventorySettings.getPos();
			
			if(globalPos == null)return Pair.of(null, ItemStack.EMPTY);
			BlockPos pos = globalPos.pos();
			List<Direction> outputSides = this.externalInventorySettings.getOutputFaces();							
			if(!outputSides.isEmpty()) {
				for(Direction face : outputSides) {
					Optional<IItemHandler> opHandler = BlockHelper.getItemHandler(level(), pos, face);
					if(opHandler.isPresent()) {
						ItemStack foundArmor = ItemHelper.findBestArmor(opHandler.get(), slot);
						if(!foundArmor.isEmpty()) {
							return Pair.of(face, foundArmor);
						}
					}
				}
			}
		}
		return Pair.of(null, ItemStack.EMPTY);
	}
	
	public Pair<Direction, ItemStack> findItemInExternalInventory(Predicate<ItemStack> searchStack) {
		return findItemInExternalInventory(searchStack, ItemHelper.SORT_STACK_SIZE);
	}

	public Pair<Direction, ItemStack> findItemInExternalInventory(Predicate<ItemStack> searchStack, @Nullable Comparator<ItemStack> comp) {
		if(this.externalInventorySettings != null) {
			GlobalPos globalPos = this.externalInventorySettings.getPos();
			
			if(globalPos == null)return Pair.of(null, ItemStack.EMPTY);
			BlockPos pos = globalPos.pos();
			List<Direction> outputSides = this.externalInventorySettings.getOutputFaces();							
			if(!outputSides.isEmpty()) {
				for(Direction face : outputSides) {
					Optional<IItemHandler> opHandler = BlockHelper.getItemHandler(level(), pos, face);
					if(opHandler.isPresent()) {
						ItemStack foundItem = ItemHelper.findItem(opHandler.get(), searchStack, comp);
						if(!foundItem.isEmpty()) {
							return Pair.of(face, foundItem);
						}
					}
				}
			}
		}
		return Pair.of(null, ItemStack.EMPTY);
	}
	
	
    public PlayerSkin getSkin() {
//        PlayerInfo playerinfo = this.getPlayerInfo();
//        return playerinfo == null ? DefaultPlayerSkin.get(this.getUUID()) : playerinfo.getSkin();
    	return DefaultWorkerSkin.getWide(getUUID());
    }
    
    public boolean isModelPartShown(PlayerModelPart p_36171_) {
    	return true;
//        return (this.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION) & p_36171_.getMask()) == p_36171_.getMask();
  	}

    public Vec3 getDeltaMovementLerped(float patialTick) {
        return this.deltaMovementOnPreviousTick.lerp(this.getDeltaMovement(), patialTick);
    }
    
	static class SwimmingMoveControl extends MoveControl {
		private final WorkerEntity minion;

		public SwimmingMoveControl(WorkerEntity minion) {
			super(minion);
			this.minion = minion;
		}

		@Override
		public void tick() {
			LivingEntity livingentity = this.minion.getTarget();
			if (this.minion.wantsToSwim() && this.minion.isInWater()) {
				if (livingentity != null && livingentity.getY() > this.minion.getY()
						|| this.minion.searchingForLand) {
					System.out.println("Searching For Land Move");
					this.minion.setDeltaMovement(this.minion.getDeltaMovement().add(0.0D, 0.002D, 0.0D));
				}
				
				if (this.operation != MoveControl.Operation.MOVE_TO || this.minion.getNavigation().isDone()) {
					this.minion.setSpeed(0.0F);
					return;
				}

				double d0 = this.wantedX - this.minion.getX();
				double d1 = this.wantedY - this.minion.getY();
				double d2 = this.wantedZ - this.minion.getZ();
				double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
				d1 /= d3;
				float f = (float) (Mth.atan2(d2, d0) * (double) (180F / (float) Math.PI)) - 90.0F;
				this.minion.setYRot(this.rotlerp(this.minion.getYRot(), f, 90.0F));
				this.minion.yBodyRot = this.minion.getYRot();
				float f1 = (float) (this.speedModifier * this.minion.getAttributeValue(Attributes.MOVEMENT_SPEED));
				float f2 = Mth.lerp(0.125F, this.minion.getSpeed(), f1);
				this.minion.setSpeed(f2);
				this.minion.setDeltaMovement(this.minion.getDeltaMovement().add((double) f2 * d0 * 0.005D,
						(double) f2 * d1 * 0.1D, (double) f2 * d2 * 0.005D));
				

			} else {
				if (!this.minion.onGround()) {
					this.minion.setDeltaMovement(this.minion.getDeltaMovement().add(0.0D, -0.008D, 0.0D));
				}
				super.tick();
			}

		}
	}

    public boolean isOwnedBy(LivingEntity entity) {
        return entity == this.getOwner();
    }

    @Nullable
    @Override
    public EntityReference<LivingEntity> getOwnerReference() {
        return this.entityData.get(DATA_OWNERUUID_ID).orElse(null);
    }

	public @Nullable UUID getOwnerUUID() {
		LivingEntity owner = this.getOwner();
		if(owner != null) {
			return owner.getUUID();
		}
		return null;
	}

    public void setOwner(@Nullable LivingEntity owner) {
        this.entityData.set(DATA_OWNERUUID_ID, Optional.ofNullable(owner).map(EntityReference::new));
    }

    public void setOwnerReference(@Nullable EntityReference<LivingEntity> owner) {
        this.entityData.set(DATA_OWNERUUID_ID, Optional.ofNullable(owner));
    }

	static class FloatWhenNotSwimming extends FloatGoal {
		private final WorkerEntity minion;
		public FloatWhenNotSwimming(WorkerEntity minion) {
			super(minion);
			this.minion = minion;
		}
		
		@Override
		public boolean canUse() {
			return super.canUse() && !minion.wantsToSwim();
		}
		
		@Override
		public boolean canContinueToUse() {
			return super.canContinueToUse() && !minion.wantsToSwim();
		}
		
	}
	
	static class SwimOutOfWater extends WaterAvoidingRandomStrollGoal {
		private final WorkerEntity minion;
		public SwimOutOfWater(WorkerEntity minion, double p_25988_) {
			super(minion, p_25988_, 0.0F);
			this.minion = minion;
		}
		
		@Override
		public boolean canUse() {
			return super.canUse() && !minion.wantsToSwim() && minion.isInWater();
		}
		
		@Override
		public boolean canContinueToUse() {
			return super.canContinueToUse() && !minion.wantsToSwim();
		}		
	}

}
