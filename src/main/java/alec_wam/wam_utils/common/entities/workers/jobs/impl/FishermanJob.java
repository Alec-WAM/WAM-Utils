package alec_wam.wam_utils.common.entities.workers.jobs.impl;

import java.util.List;
import java.util.function.Predicate;

import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager.JobType;
import alec_wam.wam_utils.common.entities.workers.jobs.MultiBlockPosWorkerJob;
import alec_wam.wam_utils.common.entities.workers.jobs.WorkerJob;
import alec_wam.wam_utils.common.helpers.EntityHelper;
import alec_wam.wam_utils.common.helpers.ItemHelper;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.FakePlayer;

public class FishermanJob extends MultiBlockPosWorkerJob {
	private static final Predicate<ItemStack> IS_FISHING_ROD = (stack) -> {
		return ItemHelper.canPerformAny(stack, ItemAbilities.DEFAULT_FISHING_ROD_ACTIONS);
	};
	
	private int timeUntilLured;
	private int timeUntilHooked;
	private float fishAngle;

	public FishermanJob(WorkerEntity worker, ResourceKey<Level> dimension, List<BlockPos> blockPosList) {
		super(worker, dimension, blockPosList);
	}
	
	public FishermanJob(WorkerEntity worker, ResourceKey<Level> dimension, BlockPos posA, BlockPos posB) {
		super(worker, dimension, posA, posB);
	}
	
	@Override
	public void save(ValueOutput valueOutput) {
		super.save(valueOutput);
	}
	
	@Override
	public void load(ValueInput valueInput) {
		super.load(valueInput);
	}

	public static FishermanJob createJob(WorkerEntity worker, ItemStack stack, Player player) {
		return JobManager.createJobFromSelection(
            worker, stack, player,
            posList -> new FishermanJob(worker, player.level().dimension(), posList),
            (pos1, pos2) -> new FishermanJob(worker, player.level().dimension(), pos1, pos2)
        );
	}
	
	@Override
	public boolean needsItem(ItemStack stack) {
		// Prevent eating these items
		if(stack.is(Tags.Items.FOODS_RAW_FISH)){
			return false;
		}
        return IS_FISHING_ROD.test(stack) || super.needsItem(stack);
	}
	
	@Override
	public void run() {
		super.run();
			
		lookAtBlock();
		if(!worker.level().isClientSide) {
			handleFishing();
		}
		
		//Unload Inventory if idling for 5 seconds
		if (this.idleTimer >= 5 * 20) {
			if (worker.getExternalInventorySettings() != null) {
				// Only check every whole second to prevent unneeded inventory scans
				if ((this.idleTimer % 20 == 0) && worker.getExternalInventorySettings().hasInputFace()
						&& worker.hasItemsToUnload()) {
					this.idleTimer = 0;
					worker.startUnloadingInventory(false);
				}
			}
		}
	}
	
	@Override	
	public double getInteractionRange(BlockState state) {
		return 2.25D;
	}

	@Override
	public boolean canInteractWithBlock(Level level, BlockPos pos) {
		FluidState fluidstate = level.getFluidState(pos);
		return fluidstate.is(FluidTags.WATER) && fluidstate.isSource();
	}
	
	public void handleFishing(){
		//TODO Do specific fishing rod enchantment sort
		if(worker.swapToItem(IS_FISHING_ROD, ItemHelper.BEST_ITEM_SORTER)){
			this.scanDelay = 10 * 20;
			return;
		}
		
		Level level = worker.level();
		if(level instanceof ServerLevel serverlevel){
			ItemStack handItem = worker.getMainHandItem();
			if(this.getWorkingPos() == null || handItem.isEmpty() || !IS_FISHING_ROD.test(handItem)){
				return;
			}

			BlockPos pos = this.getWorkingPos();
			BlockPos abovePos = this.getWorkingPos().above();

			float bobberX = pos.getX() + 0.5F;
			float bobberY = pos.getY() + 0.8F;
			float bobberZ = pos.getZ() + 0.5F;
			float bobberWidth = 0.25F;

			if(!this.worker.isFishing()){
				if(EntityHelper.isLookingAtHorizontally(worker, pos, 20)){
					this.worker.swing(InteractionHand.MAIN_HAND);
					
					level.playSound(
						null,
						this.worker.getX(),
						this.worker.getY(),
						this.worker.getZ(),
						SoundEvents.FISHING_BOBBER_THROW,
						SoundSource.NEUTRAL,
						0.5F,
						0.4F / (this.worker.getRandom().nextFloat() * 0.4F + 0.8F)
					);
					
					this.worker.startFishing(new Vec3(bobberX, bobberY, bobberZ));
				}
				return;
			}
			if(this.worker.hasBobberLanded()){

				int i = 1;

				if (worker.getRandom().nextFloat() < 0.25F && level.isRainingAt(abovePos)) {
					i++;
				}

				if (worker.getRandom().nextFloat() < 0.5F && !level.canSeeSky(abovePos)) {
					i--;
				}
				
				if (this.timeUntilHooked > 0) {
					this.timeUntilHooked -= i;
					if (this.timeUntilHooked > 0) {
						this.fishAngle = this.fishAngle + (float)worker.getRandom().triangle(0.0, 9.188);
						float f = this.fishAngle * (float) (Math.PI / 180.0);
						float f1 = Mth.sin(f);
						float f2 = Mth.cos(f);
						double d0 = bobberX + f1 * this.timeUntilHooked * 0.1F;
						double d1 = Mth.floor(bobberY) + 1.0F;
						double d2 = bobberZ + f2 * this.timeUntilHooked * 0.1F;
						BlockState blockstate = serverlevel.getBlockState(BlockPos.containing(d0, d1 - 1.0, d2));
						if (blockstate.is(Blocks.WATER)) {
							if (worker.getRandom().nextFloat() < 0.15F) {
								serverlevel.sendParticles(ParticleTypes.BUBBLE, d0, d1 - 0.1F, d2, 1, f1, 0.1, f2, 0.0);
							}

							float f3 = f1 * 0.04F;
							float f4 = f2 * 0.04F;
							serverlevel.sendParticles(ParticleTypes.FISHING, d0, d1, d2, 0, f4, 0.01, -f3, 1.0);
							serverlevel.sendParticles(ParticleTypes.FISHING, d0, d1, d2, 0, -f4, 0.01, f3, 1.0);
						}
					} else {
						level.playSound(null, bobberX, bobberY, bobberZ, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.25F, 1.0F + (worker.getRandom().nextFloat() - worker.getRandom().nextFloat()) * 0.4F);
						// this.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.25F, 1.0F + (worker.getRandom().nextFloat() - worker.getRandom().nextFloat()) * 0.4F);
						double d3 = bobberY + 0.5;
						serverlevel.sendParticles(
							ParticleTypes.BUBBLE,
							bobberX,
							d3,
							bobberZ,
							(int)(1.0F + bobberWidth * 20.0F),
							bobberWidth,
							0.0,
							bobberWidth,
							0.2F
						);
						serverlevel.sendParticles(
							ParticleTypes.FISHING,
							bobberX,
							d3,
							bobberZ,
							(int)(1.0F + bobberWidth * 20.0F),
							bobberWidth,
							0.0,
							bobberWidth,
							0.2F
						);
						this.catchFish(bobberX, bobberY, bobberZ);
					}
				}else if (this.timeUntilLured > 0) {
					this.timeUntilLured -= i;
					float f5 = 0.15F;
					if (this.timeUntilLured < 20) {
						f5 += (20 - this.timeUntilLured) * 0.05F;
					} else if (this.timeUntilLured < 40) {
						f5 += (40 - this.timeUntilLured) * 0.02F;
					} else if (this.timeUntilLured < 60) {
						f5 += (60 - this.timeUntilLured) * 0.01F;
					}

					if (worker.getRandom().nextFloat() < f5) {
						float f6 = Mth.nextFloat(worker.getRandom(), 0.0F, 360.0F) * (float) (Math.PI / 180.0);
						float f7 = Mth.nextFloat(worker.getRandom(), 25.0F, 60.0F);
						double d4 = bobberX + Mth.sin(f6) * f7 * 0.1;
						double d5 = Mth.floor(bobberY) + 1.0F;
						double d6 = bobberZ + Mth.cos(f6) * f7 * 0.1;
						BlockState blockstate1 = serverlevel.getBlockState(BlockPos.containing(d4, d5 - 1.0, d6));
						if (blockstate1.is(Blocks.WATER)) {
							serverlevel.sendParticles(ParticleTypes.SPLASH, d4, d5, d6, 2 + worker.getRandom().nextInt(2), 0.1F, 0.0, 0.1F, 0.0);
						}
					}

					if (this.timeUntilLured <= 0) {
						this.fishAngle = Mth.nextFloat(worker.getRandom(), 0.0F, 360.0F);
						this.timeUntilHooked = Mth.nextInt(worker.getRandom(), 20, 80);
					}
				} else {
					ItemStack stack = worker.getMainHandItem();
					int lureSpeed = (int)(EnchantmentHelper.getFishingTimeReduction(serverlevel, stack, worker) * 20.0F);
					this.timeUntilLured = Mth.nextInt(worker.getRandom(), 100, 600);
					this.timeUntilLured = this.timeUntilLured - lureSpeed;
				}
			}
		}
	}

	public void catchFish(float bobberX, float bobberY, float bobberZ){
		Level level = worker.level();
		ServerLevel serverLevel = (ServerLevel)level;
		ItemStack stack = worker.getMainHandItem();
		this.timeUntilHooked = 0;
		this.timeUntilLured = 0;
		Vec3 bobberVec = new Vec3(bobberX, bobberY, bobberZ);
		FakePlayer player = EntityHelper.getFakePlayer(level, worker.getOwnerUUID());
		player.setPos(bobberX, bobberY, bobberZ);
		FishingHook hook = new FishingHook(player, level, 0, 0);
		hook.setPos(bobberX, bobberY, bobberZ);
		
		int fishingRodLuck = EnchantmentHelper.getFishingLuckBonus(serverLevel, stack, worker);

		LootParams lootparams = new LootParams.Builder(serverLevel)
			.withParameter(LootContextParams.ORIGIN, bobberVec)
			.withParameter(LootContextParams.TOOL, stack)
			.withParameter(LootContextParams.THIS_ENTITY, hook)
			.withParameter(LootContextParams.ATTACKING_ENTITY, player)
			.withLuck(fishingRodLuck + worker.getLuck())
			.create(LootContextParamSets.FISHING);
		LootTable loottable = serverLevel.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
		List<ItemStack> list = loottable.getRandomItems(lootparams);
		net.neoforged.neoforge.event.entity.player.ItemFishedEvent event = new net.neoforged.neoforge.event.entity.player.ItemFishedEvent(list, 1, hook);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
		if (event.isCanceled()) {
			this.reelInRod(event.getRodDamage());
			return;
		}
		this.worker.swing(InteractionHand.MAIN_HAND);
		CriteriaTriggers.FISHING_ROD_HOOKED.trigger((ServerPlayer)player, stack, hook, list);
		
		for (ItemStack itemstack : list) {
			ItemStack remainder = worker.addToInventory(itemstack);
			if(!remainder.isEmpty()){
				ItemEntity itementity = new ItemEntity(serverLevel, bobberX, bobberY, bobberZ, remainder);
				double d0 = worker.getX() - bobberX;
				double d1 = worker.getY() - bobberY;
				double d2 = worker.getZ() - bobberZ;
				itementity.setDeltaMovement(d0 * 0.1, d1 * 0.1 + Math.sqrt(Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2)) * 0.08, d2 * 0.1);
				serverLevel.addFreshEntity(itementity);
			}
		}
		
		this.reelInRod(event.getRodDamage());
	}

	public void reelInRod(int damage){
		this.worker.level().playSound(
			null,
			this.worker.getX(),
			this.worker.getY(),
			this.worker.getZ(),
			SoundEvents.FISHING_BOBBER_RETRIEVE,
			SoundSource.NEUTRAL,
			1.0F,
			0.4F / (this.worker.getRandom().nextFloat() * 0.4F + 0.8F)
		);
		
		ItemStack stack = worker.getMainHandItem();
		// Wait 10 more seconds to fish
		this.scanDelay = 10 * 20;
		if(damage > 0){
			stack.hurtAndBreak(damage, worker, InteractionHand.MAIN_HAND);
		}
		worker.stopFishing();
		this.finishWorking();
	}

	@Override
	public void stop() {
		
	}

	@Override
	public boolean isSame(WorkerJob job) {
		return job instanceof FishermanJob && super.isSame(job);
	}

	@Override
	public JobType getJobType() {
		return JobType.FISHING;
	}

}
