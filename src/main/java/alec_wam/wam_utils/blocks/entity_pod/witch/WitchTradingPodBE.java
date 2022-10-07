package alec_wam.wam_utils.blocks.entity_pod.witch;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.blocks.entity_pod.AbstractEntityPodBE;
import alec_wam.wam_utils.blocks.entity_pod.AbstractEntityPodBlock;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.server.network.MessageBlockEntityUpdate;
import alec_wam.wam_utils.utils.InventoryUtils;
import alec_wam.wam_utils.utils.ItemUtils;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.PacketDistributor;

public class WitchTradingPodBE extends AbstractEntityPodBE<Witch> {
	public static final String SCREEN_PIGLIN_TRADE_BLOCK = "screen.wam_utils.witch_trade";
	public static final int INPUT_SLOTS = 1;
    public static final int OUTPUT_SLOTS = 5;
	
	private final RandomSource random = RandomSource.create();
	private boolean isFull;
	private int ambientSoundTime;
	private LivingEntity lookTarget;
	
	public WitchTradingPodBE(BlockPos p_155229_, BlockState p_155230_) {
		super(BlockInit.WITCH_TRADING_POD_BE.get(), p_155229_, p_155230_);
	}

	@Override
	public Witch createNewTradeEntity() {
		BlockPos pos = worldPosition;
		Witch witch = new Witch(EntityType.WITCH, level);
		witch.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
		return witch;
	}

	public boolean isFull() {
		return isFull;
	}
	
	public void setFull(boolean isFull) {
		this.isFull = isFull;
	}

	@Override
	public boolean isValidInputItem(ItemStack stack) {
		if(stack.isEmpty())return false;
		if(stack.isEnchanted() || stack.is(Items.ENCHANTED_BOOK)) {
			Map<Enchantment, Integer> curses = ItemUtils.getAllCurseEnchantments(stack);
			return !curses.isEmpty();
		}
		return false;
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
			Witch witch = getTradeEntity();
			witch.tickCount++;
			
			if(this.lookTarget !=null /*&& witch.getSensing().hasLineOfSight(this.lookTarget)*/) {
				//WAMUtilsMod.LOGGER.debug("LOOKING");
				lookAtEntity(witch, this.lookTarget);
			}
			else {
				if(witch.yHeadRot != 0.0F) {
					float speed = (float)witch.getHeadRotSpeed();					
					witch.yHeadRotO = witch.yHeadRot = rotateTowards(witch.yHeadRotO, 0.0F, speed);
				}
				if(witch.xRotO != 0.0F) {
					float speed = (float)witch.getHeadRotSpeed();					
					witch.xRotO = rotateTowards(witch.xRotO, 0.0F, speed);
				}
				//if(witch.getXRot() != 0.0F) {
					//witch.setXRot(Mth.lerp(witch.getXRot(), 0.0F, 1.0F));
				//}
			}
			
//			BlockPos pos = this.worldPosition.below();
//			LookControl control = this.getTradeEntity().getLookControl();
//			control.setLookAt(pos.getX(), pos.getY(), pos.getZ());			
//			control.tick();
			
			
			
			ItemStack input = inputItems.getStackInSlot(0);
			ItemStack hand = witch.getItemBySlot(EquipmentSlot.MAINHAND);
			if(getTradeTime() < getMaxTradeTime() && !isFull) {
				if(hand.isEmpty()) {
					witch.setItemSlot(EquipmentSlot.MAINHAND, input.copy());
				}
			}
			if(input.isEmpty() || isFull){
				if(!hand.isEmpty()) {
					witch.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);					
				}
			}
		}
	}
	
	public float rotateTowards(float p_24957_, float p_24958_, float p_24959_) {
		float f = Mth.degreesDifference(p_24957_, p_24958_);
		float f1 = Mth.clamp(f, -p_24959_, p_24959_);
		return p_24957_ + f1;
	}
	
	public void lookAtEntity(Witch witch, Entity entity) {
		Vec3 vec3 = Anchor.EYES.apply(witch);
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
    	
    	float speed = (float)witch.getHeadRotSpeed() / 2.0F;    	
    	float adjustedXRot = headRotX;//rotateTowards(witch.getXRot(), headRotX, speed);
    	witch.setXRot(adjustedXRot);
    	witch.xRotO = witch.getXRot();
    	
    	float tartgetYRot = headRotY;
    	tartgetYRot = Mth.rotateIfNecessary(tartgetYRot, witch.yBodyRot, (float)witch.getMaxHeadYRot());
		witch.yHeadRotO = witch.yHeadRot = rotateTowards(witch.yHeadRotO, tartgetYRot, speed);
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
		
		Witch witch = this.getTradeEntity();
		//getTradeEntity().setImmuneToZombification(true);
		BlockState topOfPod = this.level.getBlockState(worldPosition.above(2));		
		BlockState belowPod = this.level.getBlockState(worldPosition.below());		
		boolean noSounds = topOfPod.is(BlockTags.WOOL) || belowPod.is(BlockTags.WOOL);
		
		ItemStack input = inputItems.getStackInSlot(0);
		if(input.isEmpty() || !isValidInputItem(input) || isFull) {
			//IDLE
			
			if(!noSounds) {
				if(this.random.nextInt(1000) < this.ambientSoundTime++) {
					if(getTradeTime() <= 0) {
						BlockPos pos = this.worldPosition;
						this.level.playSound((Player)null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.WITCH_AMBIENT, SoundSource.HOSTILE, 0.35F, getTradeEntity().getVoicePitch());
					}
					this.ambientSoundTime = -80 * 3;
				}
			}
			
			
			final LivingEntity oldLookTarget = this.lookTarget;
			
			this.lookTarget = getLookTarget(witch);
			if(oldLookTarget == null && lookTarget !=null || oldLookTarget !=null && !oldLookTarget.equals(lookTarget)) {
				this.sendLookSyncPacket(lookTarget);
				if(this.lookTarget !=null && !noSounds) {
					BlockPos pos = this.worldPosition;
					//this.level.playSound((Player)null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.PIGLIN_JEALOUS, SoundSource.HOSTILE, 0.35F, getTradeEntity().getVoicePitch());
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
				//Remove Curse Enchantments
				Map<Enchantment, Integer> allEnchantments = EnchantmentHelper.getEnchantments(input);
				Map<Enchantment, Integer> curses = ItemUtils.getAllCurseEnchantments(input);				
				if(!curses.isEmpty()) {
					Entry<Enchantment, Integer> curse = curses.entrySet().stream().findFirst().orElse(null);					
					
					if(curse !=null) {
						allEnchantments.remove(curse.getKey());

						ItemStack outputStack = input.copy();						
						if(!allEnchantments.isEmpty()) {
							final ItemStack newStack = input.copy();
							if(newStack.getItem() == Items.ENCHANTED_BOOK) {
								newStack.getTag().remove("StoredEnchantments");
								allEnchantments.entrySet().stream().forEach((entry) -> {
									EnchantedBookItem.addEnchantment(newStack, new EnchantmentInstance(entry.getKey(), entry.getValue()));
								});
							}
							else {
								EnchantmentHelper.setEnchantments(allEnchantments, newStack);
							}
							
							newStack.setRepairCost(0);
							for(int i = 0; i < allEnchantments.size(); ++i) {
								newStack.setRepairCost(AnvilMenu.calculateIncreasedRepairCost(newStack.getBaseRepairCost()));
							}
							outputStack = newStack;
						}
						else {
							if(input.getItem() == Items.ENCHANTED_BOOK) {
								//If all enchantments are gone (ie. no curse enchantments left) convert to a book
								outputStack = new ItemStack(Items.BOOK);
								if (input.hasCustomHoverName()) {
									outputStack.setHoverName(input.getHoverName());
								}
							}
							else {
								//Copied from grindstone
								outputStack.removeTagKey("Enchantments");
								outputStack.removeTagKey("StoredEnchantments");
								
								if (input.getDamageValue() > 0) {
									outputStack.setDamageValue(input.getDamageValue());
								} else {
									outputStack.removeTagKey("Damage");
								}
								
								outputStack.removeTagKey("RepairCost");
							}
						}
						input.shrink(1);
						ItemStack remainder = InventoryUtils.forceStackInInventoryAllSlots(outputItems, outputStack, (slot -> {
							updateOutputItems();
							setChanged();
						}));
						
						BlockPos pos = this.worldPosition;
						this.level.playSound((Player)null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 0.5F, getTradeEntity().getVoicePitch());
						
						setTradeTime(0);
						setMaxTradeTime(Mth.randomBetweenInclusive(random, 5 * 20, 10 * 20));
					}
					//markBlockForUpdate(null);
				}
			}
		}
	}
	
	private Player getLookTarget(Witch witch) {
		float maxDistance = 6.0F;
		TargetingConditions target = TargetingConditions.forNonCombat().selector((player) -> {
			ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
			ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
			return !player.isSpectator() && !(player instanceof FakePlayer) && (isValidInputItem(mainHand) || isValidInputItem(offHand));
		});
		BlockPos pos = this.worldPosition.above();
		Player player = this.level.getNearestPlayer(target, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
		if(player !=null && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < maxDistance * maxDistance) {
			//if(witch.getSensing().hasLineOfSight(player)) {
				return player;
			//}
		}
		return null;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable(SCREEN_PIGLIN_TRADE_BLOCK);
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player playerEntity) {
		return new WitchTradingPodContainer(windowId, worldPosition, playerInventory, playerEntity);
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
