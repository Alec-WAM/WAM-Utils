package alec_wam.wam_utils.events;

import java.util.stream.Stream;

import alec_wam.wam_utils.blocks.jar.JarBE;
import alec_wam.wam_utils.blocks.jar.JarBE.JarContents;
import alec_wam.wam_utils.blocks.mob_sign.MobSignBlock;
import alec_wam.wam_utils.entities.WAMUtilsFakePlayer;
import alec_wam.wam_utils.init.EntityInit;
import alec_wam.wam_utils.utils.BlockUtils;
import alec_wam.wam_utils.utils.EntityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.VanillaGameEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class EntityEventHandler {
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityDieEvent(final LivingDropsEvent event) {
		if(event.isCanceled())
			return;
		LivingEntity entity = event.getEntity();
		Level level = entity.level;
		if (level.isClientSide)
			return;
		if(entity.getHealth() > 0.0F)
			return;
		
		ItemStack headItem = ItemStack.EMPTY;
		
		if(entity instanceof Player player) {
			headItem = EntityUtils.createPlayerHead(player.getGameProfile());
		}
		
		if(!headItem.isEmpty()) {
			ItemEntity entityItem = new ItemEntity(entity.getCommandSenderWorld(), entity.getX(), entity.getY(), entity.getZ(), headItem);
			entityItem.setDefaultPickUpDelay();
			event.getDrops().add(entityItem);
		}
    }
	
	@SubscribeEvent
	public static void worldUnload(final LevelEvent.Unload event) {
		if (event.getLevel() instanceof ServerLevel) {
			WAMUtilsFakePlayer.unload(event.getLevel());
		}
	}
	
	@SubscribeEvent
	public static void checkSpawn(final LivingSpawnEvent.CheckSpawn event) {
		if(cancelSpawn(event.getEntity().getType(), event.getLevel(), event.getX(), event.getY(), event.getZ(), event.getSpawnReason())) {
			event.setResult(Result.DENY);
		}
	}
	
	@SubscribeEvent
	public static void specialSpawn(final LivingSpawnEvent.SpecialSpawn event) {
		if(cancelSpawn(event.getEntity().getType(), event.getLevel(), event.getX(), event.getY(), event.getZ(), event.getSpawnReason())) {
			event.setCanceled(true);
		}
	}
	
	public static boolean cancelSpawn(EntityType<?> entityType, LevelAccessor levelAccessor, double posX, double posY, double posZ, MobSpawnType spawnType) {
        if (levelAccessor.isClientSide()) return true;
        ServerLevel level = ((ServerLevelAccessor) levelAccessor).getLevel();
        PoiManager poiManager = level.getPoiManager();
        BlockPos pos = new BlockPos(posX, posY, posZ);
        
        if(spawnType == MobSpawnType.EVENT) {
        	if(entityType == EntityType.WANDERING_TRADER || entityType == EntityType.TRADER_LLAMA) {
        		BlockPos closestSign = signInRange(poiManager, EntityInit.WANDERING_TRADER_SIGN_POI.getKey(), pos, MobSignBlock.SIGN_RANGE_HORIZONTAL, MobSignBlock.SIGN_RANGE_VERTICAL);
        		if(closestSign != null) {
        			RandomSource random = level.random;        			
        			BlockUtils.spawnHappyParticles(level, closestSign, random);
        			return true;
        		}
        	}
        }
        if(spawnType == MobSpawnType.PATROL) {
        	if(entityType == EntityType.PILLAGER) {
        		BlockPos closestSign = signInRange(poiManager, EntityInit.PILLAGER_SIGN_POI.getKey(), pos, MobSignBlock.SIGN_RANGE_HORIZONTAL, MobSignBlock.SIGN_RANGE_VERTICAL);
        		if(closestSign != null) {
        			RandomSource random = level.random;        			
        			BlockUtils.spawnHappyParticles(level, closestSign, random);
        			return true;
        		}
        	}
        }
        return false;
    }
	
	@SubscribeEvent
	public static void endermanTeleport(final EntityTeleportEvent.EnderEntity event) {
		BlockPos fromPos = new BlockPos(event.getPrev());
		BlockPos toPos = new BlockPos(event.getTarget());
		if(cancelTeleport(event.getEntity().getType(), event.getEntityLiving().getLevel(), fromPos)) {
			event.setCanceled(true);
		}
		if(cancelTeleport(event.getEntity().getType(), event.getEntityLiving().getLevel(), toPos)) {
			event.setCanceled(true);
		}
	}
	
	public static boolean cancelTeleport(EntityType<?> entityType, LevelAccessor levelAccessor, BlockPos pos) {
        if (levelAccessor.isClientSide()) return true;
        ServerLevel level = ((ServerLevelAccessor) levelAccessor).getLevel();
        PoiManager poiManager = level.getPoiManager();
        
        if(entityType == EntityType.ENDERMAN || entityType == EntityType.SHULKER) {
    		BlockPos closestSign = signInRange(poiManager, EntityInit.ENDERMAN_SIGN_POI.getKey(), pos, MobSignBlock.SMALL_SIGN_RANGE_HORIZONTAL, MobSignBlock.SMALL_SIGN_RANGE_VERTICAL);
    		if(closestSign != null) {
    			//RandomSource random = level.random;        			
    			//BlockUtils.spawnHappyParticles(level, closestSign, random);
    			return true;
    		}
    	}
        return false;
    }
	
	@SubscribeEvent
	public static void mobTargetEntity(final LivingSetAttackTargetEvent event) {
		if (event.getEntity().level == null || event.getEntity().level.isClientSide)return;
        
		LivingEntity attacker = event.getEntity();
		ServerLevel level = ((ServerLevelAccessor) event.getEntity().level).getLevel();
        PoiManager poiManager = level.getPoiManager();
		
		LivingEntity target = event.getTarget();
		
		if (!(target instanceof Player) || target instanceof FakePlayer) {
			return;
		}
		
		EntityType<?> type = attacker.getType();
        BlockPos pos = target.blockPosition();
        
        if(type == EntityType.PHANTOM) {
        	BlockPos closestSign = signInRange(poiManager, EntityInit.PHANTOM_SIGN_POI.getKey(), pos, MobSignBlock.SMALL_SIGN_RANGE_HORIZONTAL, MobSignBlock.SMALL_SIGN_RANGE_VERTICAL);
    		if(closestSign != null) {
    			if(attacker instanceof Mob mob) {
    				EntityUtils.clearAttackTarget(mob);
    			}
    		}
        }
	}
	
	public static BlockPos signInRange(PoiManager poiManager, ResourceKey<PoiType> poiType, BlockPos pos, int horizonal, int vertical) {
		Stream<BlockPos> all = poiManager.findAll(poiType1 -> poiType1.is(poiType), pos1 -> true, pos, (int) Math.ceil(Math.sqrt(horizonal * horizonal + vertical * vertical)), PoiManager.Occupancy.ANY);
		//return all.anyMatch(center -> isInRange(center, pos));
		return all.filter(center -> isInRange(center, pos, horizonal, vertical)).findFirst().orElse(null);
	}
	
	private static boolean isInRange(BlockPos center, BlockPos pos, int horizonal, int vertical) {
        int dimX = Math.abs(center.getX() - pos.getX());
        int dimY = Math.abs(center.getY() - pos.getY());
        int dimZ = Math.abs(center.getZ() - pos.getZ());
        return dimX <= horizonal && dimZ <= horizonal && dimY <= vertical;
    }
	
	@SubscribeEvent
	public static void gameEvents(VanillaGameEvent event) {
		if(!event.isCanceled()) {
			if(event.getVanillaEvent() == GameEvent.LIGHTNING_STRIKE) {
				if(event.getCause() !=null && event.getCause() instanceof LightningBolt lightning) {
					Level level = event.getLevel();
					Vec3 vec3 = lightning.position();
					BlockPos strikePos = new BlockPos(vec3.x, vec3.y - 1.0E-6D, vec3.z);
					BlockState blockstate = level.getBlockState(strikePos);
					
					boolean areaEffect = false;
					if(blockstate.is(Blocks.LIGHTNING_ROD)) {
						areaEffect = true;
						strikePos = strikePos.relative(blockstate.getValue(LightningRodBlock.FACING).getOpposite());
					}
					
					strikeWithLightning(level, strikePos, strikePos);
					
					if(areaEffect) {
						for(BlockPos blockpos : BlockPos.randomInCube(level.random, 10, strikePos, 1)) {
							if(blockpos.equals(strikePos))continue;
							strikeWithLightning(level, strikePos, blockpos);
						}
					}
				}
			}
		}
	}
	
	private static void strikeWithLightning(Level level, BlockPos strikePos, BlockPos currentPos) {
		BlockEntity be = level.getBlockEntity(currentPos);
		//int distance = currentPos.distManhattan(strikePos);
		//if(distance < JarBE.LIGHTNING_CAPACITY) {
			if(be !=null && be instanceof JarBE jar) {
				int lightingValue = 1;
				if(jar.getContents() == JarContents.EMPTY || jar.getContents() == JarContents.LIGHTNING) {
					if(jar.getContents() == JarContents.EMPTY) {
						jar.setContents(JarContents.LIGHTNING);
					}
					int newValue = Math.min(JarBE.LIGHTNING_CAPACITY, jar.getBottleCount() + lightingValue);
					jar.setBottleCount(newValue);
					if(!level.isClientSide) {
						double d0 = 0.5D;
						double d1 = 1.0D;
						for(int i = 0; i < 15; ++i) {
							double d2 = level.random.nextGaussian() * 0.02D;
							double d3 = level.random.nextGaussian() * 0.02D;
							double d4 = level.random.nextGaussian() * 0.02D;
							double d5 = 0.5D;
							double d6 = (double)currentPos.getX() + 0.5D - d5 + level.random.nextDouble() * d0 * 2.0D;
							double d7 = (double)currentPos.getY() + level.random.nextDouble() * d1;
							double d8 = (double)currentPos.getZ() + 0.5D - d5 + level.random.nextDouble() * d0 * 2.0D;
							((ServerLevel)level).sendParticles(ParticleTypes.ELECTRIC_SPARK, d6, d7, d8, 0, d2, d3, d4, 1.0D);
						}
						jar.setChanged();
						jar.markBlockForUpdate(null);
					}
				}
			}
		//}
	}
	
	@SubscribeEvent
	public static void entityInteract(EntityInteractSpecific event) {
		Entity target = event.getTarget();
		Player player = event.getEntity();
		ItemStack held = event.getItemStack();
		Level level = event.getLevel();
		if(target.getType() == EntityType.SQUID) {
			Squid squid = (Squid)target;
			if(held.is(Tags.Items.DUSTS_GLOWSTONE)) {
				if (!player.getAbilities().instabuild) {
					held.shrink(1);
				}
				
				if(!level.isClientSide) {
					EntityUtils.convertEntity(level, squid, EntityType.GLOW_SQUID);
				}
				
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
			}
		}
	}
}
