package alec_wam.wam_utils.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.mojang.authlib.GameProfile;

import alec_wam.wam_utils.entities.WAMUtilsFakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;

public class EntityUtils {

	//TODO Add option to respect needing play in radius
	public static boolean spawnRandomCategoryEntity(ServerLevel serverLevel, BlockPos pos, MobCategory mobCategory, MobSpawnType spawnType) {
		RandomSource random = serverLevel.getRandom();
		Holder<Biome> biomeHolder = serverLevel.getBiome(pos);
		if(biomeHolder.value() != null) {
			//WAMUtilsMod.LOGGER.debug("Biome");
			Biome biome = biomeHolder.value();
			MobSpawnSettings mobSettings = biome.getMobSettings();
			if(mobSettings != MobSpawnSettings.EMPTY) {
				WeightedRandomList<MobSpawnSettings.SpawnerData> creatureList = mobSettings.getMobs(mobCategory);
				if(creatureList != MobSpawnSettings.EMPTY_MOB_LIST) {
					Optional<SpawnerData> spawnData = creatureList.getRandom(random);
					if (spawnData.isEmpty()) {
						return false;
					}
					SpawnerData spawn = spawnData.get();
					EntityType<?> entityType = spawn.type;
					if(entityType.canSummon()) {
						SpawnPlacements.Type spawnPlacementType = SpawnPlacements.getPlacementType(entityType);
						boolean validPlacement = false;
						if(spawnPlacementType == SpawnPlacements.Type.NO_RESTRICTIONS) {
							validPlacement = true;
						}
						else if (serverLevel.getWorldBorder().isWithinBounds(pos)) {
							validPlacement = spawnPlacementType.canSpawnAt(serverLevel, pos, entityType);
						}
						if(validPlacement) {
							if(SpawnPlacements.checkSpawnRules(entityType, serverLevel, spawnType, pos, random)) {
								double spawnPosX = (double)pos.getX() + 0.5D;
								double spawnPosY = (double)pos.getY();
								double spawnPosZ = (double)pos.getZ() + 0.5D;
								AABB aabb = entityType.getAABB(spawnPosX, spawnPosY, spawnPosZ);
								if(serverLevel.noCollision(aabb)) {
									Entity entity = entityType.create(serverLevel);
									if(entity instanceof Mob mob) {
										mob.moveTo(spawnPosX, spawnPosY, spawnPosZ, random.nextFloat() * 360.0F, 0.0F);
										int canSpawn = net.minecraftforge.common.ForgeHooks.canEntitySpawn(mob, serverLevel, spawnPosX, spawnPosY, spawnPosZ, null, spawnType);
				                        if(canSpawn != -1 && (canSpawn == 1 || mob.checkSpawnRules(serverLevel, spawnType) && mob.checkSpawnObstruction(serverLevel))){
				                        	SpawnGroupData spawngroupdata = null;
				                        	if (!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(mob, serverLevel, (float)spawnPosX, (float)spawnPosY, (float)spawnPosZ, null, spawnType))
				                        		spawngroupdata = mob.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(mob.blockPosition()), spawnType, spawngroupdata, (CompoundTag)null);
				                        	serverLevel.addFreshEntityWithPassengers(mob);   
				                        	return true;
				                        }
									}
								}
							}
						}
					}
				}
			}
		}
//		ServerLevel serverLevel = (ServerLevel)level;		
//		NaturalSpawner.spawnCategoryForPosition(MobCategory.CREATURE, serverLevel, serverLevel.getChunk(pos), pos, 
//		  //Spawn Test
//		  (p_151606_, p_151607_, p_151608_) -> {
//	         return true;
//	      }, 
//		  //After Spawn Call
//		  (p_151610_, p_151611_) -> {
//	      });
		return false;
	}	

	public static ItemStack createPlayerHead(GameProfile profile) {
		ItemStack stack = new ItemStack(Items.PLAYER_HEAD, 1);
		stack.setTag(new CompoundTag());
		CompoundTag profileData = new CompoundTag();
		NbtUtils.writeGameProfile(profileData, profile);
		stack.getTag().put("SkullOwner", profileData);
		return stack;
	}
	
	public static ItemStack getHeadFromEntity(LivingEntity entity) {
		if(entity instanceof Player) {
			Player player = (Player)entity;
			return createPlayerHead(player.getGameProfile());
		}
		else if(entity.getType() == EntityType.SKELETON) {
			return new ItemStack(Items.SKELETON_SKULL, 1);
		}
		else if(entity.getType() == EntityType.WITHER_SKELETON) {
			return new ItemStack(Items.WITHER_SKELETON_SKULL, 1);
		}
		else if(entity.getType() == EntityType.ZOMBIE) {
			return new ItemStack(Items.ZOMBIE_HEAD, 1);
		}
		else if(entity.getType() == EntityType.CREEPER) {
			return new ItemStack(Items.CREEPER_HEAD, 1);
		}
		else if(entity.getType() == EntityType.ENDER_DRAGON) {
			return new ItemStack(Items.DRAGON_HEAD, 1);
		}
		return ItemStack.EMPTY;
	}

	public static List<ItemEntity> captureEntityDrops(Level level, LivingEntity livingEntity, RandomSource random, boolean useFakePlayer, boolean hurtByPlayer, int looting) {
		List<ItemEntity> items = new ArrayList<>();

		DamageSource damage = DamageSource.GENERIC;
		
		livingEntity.captureDrops(new java.util.ArrayList<>());

		//Method shouldDropLoot = livingEntity.getClass().get("shouldDropLoot");
		boolean shouldDrop = true;//(Boolean)shouldDropLoot.invoke(livingEntity);
		if (shouldDrop && level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
			if(useFakePlayer) {
				WAMUtilsFakePlayer player = WAMUtilsFakePlayer.get((ServerLevel)level, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ()).get();
				damage = DamageSource.playerAttack(player);
				dropFromLootTable(level, livingEntity, damage, hurtByPlayer, player, random);
			}
			else {
				dropFromLootTable(level, livingEntity, damage, hurtByPlayer, null, random);				
			}
			
			try {
				Method dropCustomDeathLootMethod = livingEntity.getClass().getDeclaredMethod("dropCustomDeathLoot", DamageSource.class, int.class, boolean.class);
				dropCustomDeathLootMethod.setAccessible(true);
				dropCustomDeathLootMethod.invoke(livingEntity, damage, looting, hurtByPlayer);
			} catch (Exception e) {
				//Skip dropCustomDeathLoot if entity does not call it
				//e.printStackTrace();
			} 
		}

		//livingEntity.dropEquipment();
		//Method dropEquipmentMethod = livingEntity.getClass().getDeclaredMethod("dropEquipment");
		//dropEquipmentMethod.invoke(livingEntity);
		
		
		//livingEntity.dropExperience();

		Collection<ItemEntity> drops = livingEntity.captureDrops(null);
		if (net.minecraftforge.common.ForgeHooks.onLivingDrops(livingEntity, damage, drops, looting, hurtByPlayer)) {
			return new ArrayList<ItemEntity>();
		}
		items.addAll(drops);
		return items;
	}
	
	public static void dropFromLootTable(Level level, LivingEntity livingEntity, DamageSource p_21021_, boolean p_21022_, Player player, RandomSource random) {
      ResourceLocation resourcelocation = livingEntity.getLootTable();
      LootTable loottable = level.getServer().getLootTables().get(resourcelocation);
      LootContext.Builder lootcontext$builder = createLootContext(level, livingEntity, p_21022_, p_21021_, player, random);
      LootContext ctx = lootcontext$builder.create(LootContextParamSets.ENTITY);
      List<ItemStack> drops = loottable.getRandomItems(ctx);
      drops.forEach(livingEntity::spawnAtLocation);
   }
	
	public static List<ItemStack> getLootTableDrops(Level level, LivingEntity livingEntity, DamageSource p_21021_, boolean p_21022_, Player player, RandomSource random) {
      ResourceLocation resourcelocation = livingEntity.getLootTable();
      LootTable loottable = level.getServer().getLootTables().get(resourcelocation);
      LootContext.Builder lootcontext$builder = createLootContext(level, livingEntity, p_21022_, p_21021_, player, random);
      LootContext ctx = lootcontext$builder.create(LootContextParamSets.ENTITY);
      return loottable.getRandomItems(ctx);
   }
	
	public static LootContext.Builder createLootContext(Level level, LivingEntity livingEntity, boolean p_21105_, DamageSource p_21106_, Player player, RandomSource random) {
      LootContext.Builder lootcontext$builder = (new LootContext.Builder((ServerLevel)level)).withRandom(random).withParameter(LootContextParams.THIS_ENTITY, livingEntity).withParameter(LootContextParams.ORIGIN, livingEntity.position()).withParameter(LootContextParams.DAMAGE_SOURCE, p_21106_).withOptionalParameter(LootContextParams.KILLER_ENTITY, p_21106_.getEntity()).withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, p_21106_.getDirectEntity());
      if (p_21105_ && player != null) {
         lootcontext$builder = lootcontext$builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player).withLuck(player.getLuck());
      }

      return lootcontext$builder;
	}


	public static void angerNearbyMobs(Level p_49650_, BlockPos p_49651_, Class<? extends Mob> entityClass) {
		List<? extends Mob> list = p_49650_.getEntitiesOfClass(entityClass, (new AABB(p_49651_)).inflate(8.0D, 6.0D, 8.0D));
		if (!list.isEmpty()) {
			List<Player> list1 = p_49650_.getEntitiesOfClass(Player.class, (new AABB(p_49651_)).inflate(8.0D, 6.0D, 8.0D));
			if (list1.isEmpty()) return; //Forge: Prevent Error when no players are around.
			int i = list1.size();

			for(Mob entity : list) {
				if (entity.getTarget() == null) {
					entity.setTarget(list1.get(p_49650_.random.nextInt(i)));
				}
			}
		}

	}
	
	public static <E extends LivingEntity> void convertEntity(Level level, LivingEntity target, EntityType<E> convertType) {
		((ServerLevel)level).sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY(0.5D), target.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
		target.discard();
		E newEntity = convertType.create(level);
		newEntity.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
		newEntity.setHealth(target.getHealth());
		newEntity.yBodyRot = target.yBodyRot;
		if (target.hasCustomName()) {
			newEntity.setCustomName(target.getCustomName());
			newEntity.setCustomNameVisible(target.isCustomNameVisible());
		}

		if(target instanceof Mob oldMob) {
			if (oldMob.isPersistenceRequired()) {
				if(newEntity instanceof Mob newMob) {
					newMob.setPersistenceRequired();
				}
			}
		}

		newEntity.setInvulnerable(target.isInvulnerable());
		level.addFreshEntity(newEntity);
	}
	
	private static final Set<MemoryModuleType<?>> TARGET_MEMORIES = ImmutableSet.of(
			MemoryModuleType.ATTACK_TARGET, MemoryModuleType.ANGRY_AT, MemoryModuleType.UNIVERSAL_ANGER,
			MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD, MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER
	);
	
	public static void clearAttackTarget(Mob entity) {
		Brain<?> brain = entity.getBrain();
		for (var memory : TARGET_MEMORIES) {
			if (brain.hasMemoryValue(memory)) {
				brain.setMemory(memory, Optional.empty());
			}
		}

		entity.setTarget(null);
	}
	
}
