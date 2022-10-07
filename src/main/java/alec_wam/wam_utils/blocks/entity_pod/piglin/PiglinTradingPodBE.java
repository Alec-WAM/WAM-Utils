package alec_wam.wam_utils.blocks.entity_pod.piglin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.entity_pod.AbstractEntityPodBE;
import alec_wam.wam_utils.blocks.entity_pod.AbstractEntityPodBlock;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.server.network.MessageBlockEntityUpdate;
import alec_wam.wam_utils.utils.InventoryUtils;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.PacketDistributor;

public class PiglinTradingPodBE extends AbstractEntityPodBE<Piglin> {
	public static final String SCREEN_PIGLIN_TRADE_BLOCK = "screen.wam_utils.piglin_trade";
	public static final int INPUT_SLOTS = 1;
    public static final int OUTPUT_SLOTS = 18;
	
	private final RandomSource random = RandomSource.create();
	private boolean isFull;
	private int ambientSoundTime;
	private LivingEntity lookTarget;
	
	public PiglinTradingPodBE(BlockPos p_155229_, BlockState p_155230_) {
		super(BlockInit.PIGLIN_TRADING_POD_BE.get(), p_155229_, p_155230_);
	}

	@Override
	public Piglin createNewTradeEntity() {
		BlockPos pos = worldPosition;
		Piglin piglin = new Piglin(EntityType.PIGLIN, level);
		piglin.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
		piglin.setImmuneToZombification(true);
		return piglin;
	}

	public boolean isFull() {
		return isFull;
	}
	
	public void setFull(boolean isFull) {
		this.isFull = isFull;
	}

	@Override
	public boolean isValidInputItem(ItemStack stack) {
		return !stack.isEmpty() && stack.isPiglinCurrency();
	}
	
	@Override
	public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
		super.writeCustomNBT(nbt, descPacket);
		nbt.putBoolean("isFull", isFull);
	}
	
	@Override
	public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
		super.readCustomNBT(nbt, descPacket);
		isFull = nbt.getBoolean("isFull");
	}
	
	@Override
	public void updateOutputItems() {
		super.updateOutputItems();
		
		final boolean oldIsFull = isFull;
		isFull = InventoryUtils.isFull(outputItems);
		if(oldIsFull != isFull) {
			markBlockForUpdate(null);
		}
	}
	
	@Override
	public void tickClient() {
		if(this.getTradeEntity() !=null) {
			Piglin piglin = getTradeEntity();
			piglin.tickCount++;
			//getTradeEntity().setImmuneToZombification(true);
			
			if(this.lookTarget !=null /*&& piglin.getSensing().hasLineOfSight(this.lookTarget)*/) {
				//WAMUtilsMod.LOGGER.debug("LOOKING");
				lookAtEntity(piglin, this.lookTarget);
			}
			else {
				if(piglin.yHeadRot != 0.0F) {
					float speed = (float)piglin.getHeadRotSpeed();					
					piglin.yHeadRotO = piglin.yHeadRot = rotateTowards(piglin.yHeadRotO, 0.0F, speed);
				}
				if(piglin.xRotO != 0.0F) {
					float speed = (float)piglin.getHeadRotSpeed();					
					piglin.xRotO = rotateTowards(piglin.xRotO, 0.0F, speed);
				}
				//if(piglin.getXRot() != 0.0F) {
					//piglin.setXRot(Mth.lerp(piglin.getXRot(), 0.0F, 1.0F));
				//}
			}
			
//			BlockPos pos = this.worldPosition.below();
//			LookControl control = this.getTradeEntity().getLookControl();
//			control.setLookAt(pos.getX(), pos.getY(), pos.getZ());			
//			control.tick();
			
			
			
			ItemStack input = inputItems.getStackInSlot(0);
			ItemStack hand = piglin.getItemBySlot(EquipmentSlot.OFFHAND);
			if(getTradeTime() < getMaxTradeTime() && !isFull) {
				if(hand.isEmpty()) {
					piglin.setItemSlot(EquipmentSlot.OFFHAND, input.copy());
				}
			}
			if(input.isEmpty() || isFull){
				if(!hand.isEmpty()) {
					piglin.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);					
				}
			}
		}
	}
	
	public float rotateTowards(float p_24957_, float p_24958_, float p_24959_) {
		float f = Mth.degreesDifference(p_24957_, p_24958_);
		float f1 = Mth.clamp(f, -p_24959_, p_24959_);
		return p_24957_ + f1;
	}
	
	public void lookAtEntity(Piglin piglin, Entity entity) {
		Vec3 vec3 = Anchor.EYES.apply(piglin);
		Vec3 target = new Vec3(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ());
		double d0 = target.x - vec3.x;
		double d1 = target.y - vec3.y;
		double d2 = target.z - vec3.z;
		double d3 = Math.sqrt(d0 * d0 + d2 * d2);
		float headRotX = Mth.wrapDegrees((float)(-(Mth.atan2(d1, d3) * (double)(180F / (float)Math.PI))));
		float headRotY = Mth.wrapDegrees((float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F);
		
		Direction dir = getBlockState().getValue(AbstractEntityPodBlock.FACING);
    	float f = -dir.toYRot();
    	headRotY += f;
    	
    	float speed = (float)piglin.getHeadRotSpeed() / 2.0F;    	
    	float adjustedXRot = headRotX;//rotateTowards(piglin.getXRot(), headRotX, speed);
    	piglin.setXRot(adjustedXRot);
    	piglin.xRotO = piglin.getXRot();
    	
    	float tartgetYRot = headRotY;
    	tartgetYRot = Mth.rotateIfNecessary(tartgetYRot, piglin.yBodyRot, (float)piglin.getMaxHeadYRot());
		piglin.yHeadRotO = piglin.yHeadRot = rotateTowards(piglin.yHeadRotO, tartgetYRot, speed);
	}
	
	protected void sendLookSyncPacket(LivingEntity target)
	{
		CompoundTag nbt = new CompoundTag();
		nbt.putBoolean("hasValidLookTarget", target !=null);
		if(target != null) {
			if(target instanceof Player player) {
				nbt.putString("PlayerUUID", player.getStringUUID());
			}
			else {
				nbt.put("LookTarget", target.serializeNBT());
			}
		}
		WAMUtilsMod.packetHandler.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)),
				new MessageBlockEntityUpdate(this, nbt));
	}
	
	@Override
	public void receiveMessageFromServer(CompoundTag message)
	{
		boolean validTarget = message.getBoolean("hasValidLookTarget");
		if(validTarget && message.contains("LookTarget")) {
			CompoundTag compoundtag = message.getCompound("LookTarget");
			Optional<EntityType<?>> optional = EntityType.by(compoundtag);
			WAMUtilsMod.LOGGER.debug(optional.toString());
            if(optional.isPresent()) {
            	Entity entity = EntityType.loadEntityRecursive(compoundtag, level, (p_151310_) -> {
            		return p_151310_;
            	});
            	if(entity !=null && entity instanceof LivingEntity livingEntity) {
            		this.lookTarget = livingEntity;
            	}
            }
		} 
		else if(validTarget && message.contains("PlayerUUID")) {
			UUID uuid = UUID.fromString(message.getString("PlayerUUID"));
			Player player = null;
			if(uuid !=null) {
				player = this.level.getPlayerByUUID(uuid);				
			}
			this.lookTarget = player;
		}
		else {
			this.lookTarget = null;
		}
	}
	
	@Override
	public void tickServer() {
		
		if(this.getTradeEntity() == null) {
			this.tradeEntity = createNewTradeEntity();
			this.markBlockForUpdate(null);
		}
		
		Piglin piglin = this.getTradeEntity();
		//getTradeEntity().setImmuneToZombification(true);
		BlockState topOfPod = this.level.getBlockState(worldPosition.above(2));		
		BlockState belowPod = this.level.getBlockState(worldPosition.below());		
		boolean noSounds = topOfPod.is(BlockTags.WOOL) || belowPod.is(BlockTags.WOOL);
		
		ItemStack input = inputItems.getStackInSlot(0);
		if(input.isEmpty() || !input.isPiglinCurrency() || isFull) {
			//IDLE
			
			if(!noSounds) {
				if(this.random.nextInt(1000) < this.ambientSoundTime++) {
					if(getTradeTime() <= 0) {
						BlockPos pos = this.worldPosition;
						this.level.playSound((Player)null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.PIGLIN_AMBIENT, SoundSource.HOSTILE, 0.35F, getTradeEntity().getVoicePitch());
					}
					this.ambientSoundTime = -80 * 3;
				}
			}
			
			
			final LivingEntity oldLookTarget = this.lookTarget;
			
			this.lookTarget = getLookTarget(piglin);
			if(oldLookTarget == null && lookTarget !=null || oldLookTarget !=null && !oldLookTarget.equals(lookTarget)) {
				this.sendLookSyncPacket(lookTarget);
				if(this.lookTarget !=null && !noSounds) {
					BlockPos pos = this.worldPosition;
					this.level.playSound((Player)null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.PIGLIN_JEALOUS, SoundSource.HOSTILE, 0.35F, getTradeEntity().getVoicePitch());
				}		
			}
			if(getTradeTime() > 0) {
				setTradeTime(0);
				setMaxTradeTime(0);
			}
			return;
		}
		
		if(getMaxTradeTime() == 0) {
			//Random time between 5-10 seconds
			this.setMaxTradeTime(Mth.randomBetweenInclusive(random, 5 * 20, 10 * 20));
			//markBlockForUpdate(null);
		}
		else {
			if(getTradeTime() < getMaxTradeTime()) {
				setTradeTime(getTradeTime() + 1);
				
				if(this.lookTarget != null) {
					this.lookTarget = null;
					this.sendLookSyncPacket(null);
				}
			}
			else {
				//Give Loot
				List<ItemStack> tradeItems = getRandomBarterResponseItems(input);
				if(!tradeItems.isEmpty()) {
					ItemStack copy = input.copy();
					copy.shrink(1);
					if(copy.isEmpty()) {
						copy = ItemStack.EMPTY;
					}
					this.inputItems.setStackInSlot(0, copy);
					
					for(ItemStack stack : tradeItems) {
						InventoryUtils.forceStackInInventoryAllSlots(outputItems, stack, (slot -> {
							updateOutputItems();
							setChanged();
						}));
					}
					
					BlockPos pos = this.worldPosition;
					this.level.playSound((Player)null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.PIGLIN_ADMIRING_ITEM, SoundSource.HOSTILE, 0.5F, getTradeEntity().getVoicePitch());
					
					setTradeTime(0);
					setMaxTradeTime(Mth.randomBetweenInclusive(random, 5 * 20, 10 * 20));
					//markBlockForUpdate(null);
				}
			}
		}
	}
	
	private Player getLookTarget(Piglin piglin) {
		float maxDistance = 6.0F;
		TargetingConditions target = TargetingConditions.forNonCombat().selector((player) -> {
			return !player.isSpectator() && !(player instanceof FakePlayer) && PiglinAi.isPlayerHoldingLovedItem(player);
		});
		BlockPos pos = this.worldPosition.above();
		Player player = this.level.getNearestPlayer(target, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
		if(player !=null && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < maxDistance * maxDistance) {
			//if(piglin.getSensing().hasLineOfSight(player)) {
				return player;
			//}
		}
		return null;
	}
	
	private List<ItemStack> getRandomBarterResponseItems(ItemStack input) {
		LootTable loottable = level.getServer().getLootTables().get(BuiltInLootTables.PIGLIN_BARTERING);
		List<ItemStack> list = loottable.getRandomItems((new LootContext.Builder((ServerLevel)level)).withParameter(LootContextParams.THIS_ENTITY, getTradeEntity()).withRandom(level.random).create(LootContextParamSets.PIGLIN_BARTER));
		return list;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable(SCREEN_PIGLIN_TRADE_BLOCK);
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player playerEntity) {
		return new PiglinTradingPodContainer(windowId, worldPosition, playerInventory, playerEntity);
	}
	
	public void updateInput() {
		this.markBlockForUpdate(null);
	}

	@Override
	protected ItemStackHandler createInputItemHandler() {
		return new ItemStackHandler(INPUT_SLOTS) {
			@Override
			protected void onContentsChanged(int slot) {
				setChanged();
				updateInput();
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

	@Override
	protected ItemStackHandler createOutputItemHandler() {
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
	
	@Override
	public AABB getRenderBoundingBox() {
		BlockPos pos = getBlockPos();
		return new AABB(pos, pos.offset(1, 2, 1));
	}

}
