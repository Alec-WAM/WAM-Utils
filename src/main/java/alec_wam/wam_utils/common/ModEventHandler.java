package alec_wam.wam_utils.common;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import alec_wam.wam_utils.WAMUtils;
import alec_wam.wam_utils.common.blocks.mob_sign.MobSignBlock;
import alec_wam.wam_utils.common.blocks.mob_sign.MobSignBlock.MobSignTypeObjects;
import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;

@EventBusSubscriber(modid = WAMUtils.MODID)
public class ModEventHandler {
    
	//TODO Set last hurtby player when LivingHurtEvent is fired to Worker due to it not being a tamable animal

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void mobDrops(LivingDropsEvent event){
        if(event.isCanceled())return;
        DamageSource damageSource = event.getSource();
		
		if(damageSource.getEntity() !=null) {
			Entity causingEntity = damageSource.getEntity();
			if(causingEntity instanceof WorkerEntity worker) {
				
				if(worker.getJob() !=null) {
					List<ItemEntity> toRemove = new ArrayList<>();
					// Copy list to prevent concurrent modification
					for (ItemEntity item : new ArrayList<>(event.getDrops())) {
						if(worker.getJob().pickupMobDrops(item.getItem())) {							
							if(worker.silentPickupItem(item)) {
								toRemove.add(item);
							}
							else if(worker.getJob().unloadWhenFull()){
								//Inventory is full
								worker.startUnloadingInventory(false);
							}
						}
					}
					event.getDrops().removeAll(toRemove);
				}
			}
		}
    }

	@SubscribeEvent
	public static void checkSpawn(final SpawnPlacementCheck event) {
		if(cancelSpawn(event.getEntityType(), event.getLevel(), event.getPos(), event.getSpawnType())) {
			event.setResult(SpawnPlacementCheck.Result.FAIL);
		}
	}
	
	@SubscribeEvent
	public static void cancelFinalizeSpawn(final FinalizeSpawnEvent event) {
		BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
		if(cancelSpawn(event.getEntity().getType(), event.getLevel(), pos, event.getSpawnType())) {
			event.setSpawnCancelled(true);
		}
	}

	@SubscribeEvent
	public static void cancelAddEntity(final EntityJoinLevelEvent event) {
		Entity entity = event.getEntity();
		
		if(entity.getType() == EntityType.SILVERFISH) {
			BlockPos pos = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
			if(cancelSpawn(event.getEntity().getType(), event.getLevel(), pos, null)) {
				event.setCanceled(true);
			}
		}
	}

	public static ResourceKey<PoiType> getMobSignPoiType(MobSignBlock.MobSignType signType){
		MobSignTypeObjects objects = ModInit.MOB_SIGN_TYPE_OBJECTS.get(signType);
		if(objects != null) {
			return objects.poiType().getKey();
		}
		return null;
	}
	
	public static boolean cancelSpawn(EntityType<?> entityType, LevelAccessor levelAccessor, BlockPos pos, EntitySpawnReason spawnType) {
        if (levelAccessor.isClientSide()) return true;
        ServerLevel level = ((ServerLevelAccessor) levelAccessor).getLevel();
        PoiManager poiManager = level.getPoiManager();
        
        if(spawnType == EntitySpawnReason.EVENT) {
        	if(entityType == EntityType.WANDERING_TRADER || entityType == EntityType.TRADER_LLAMA) {
				ResourceKey<PoiType> poiType = getMobSignPoiType(MobSignBlock.MobSignType.WANDERING_TRADER);
        		if(poiType !=null){
					BlockPos closestSign = poiInRange(poiManager, poiType, pos, MobSignBlock.SIGN_RANGE_HORIZONTAL, MobSignBlock.SIGN_RANGE_VERTICAL);
					if(closestSign != null) {
						// RandomSource random = level.random;        			
						// BlockUtils.spawnHappyParticles(level, closestSign, random);
						return true;
					}
				}
        	}
        }
		if(spawnType == EntitySpawnReason.SPAWNER || spawnType == EntitySpawnReason.TRIGGERED || spawnType == null) {
			if(entityType == EntityType.SILVERFISH) {
				ResourceKey<PoiType> poiType = getMobSignPoiType(MobSignBlock.MobSignType.SILVERFISH);
        		if(poiType !=null){
					BlockPos closestSign = poiInRange(poiManager, poiType, pos, MobSignBlock.SIGN_RANGE_HORIZONTAL, MobSignBlock.SIGN_RANGE_VERTICAL);
					if(closestSign != null) {
						// RandomSource random = level.random;        			
						// BlockUtils.spawnHappyParticles(level, closestSign, random);
						return true;
					}
				}
        	}
		}
        if(spawnType == EntitySpawnReason.PATROL) {
        	if(entityType == EntityType.PILLAGER) {
        		ResourceKey<PoiType> poiType = getMobSignPoiType(MobSignBlock.MobSignType.PILLAGER);
        		if(poiType !=null){
					BlockPos closestSign = poiInRange(poiManager, poiType, pos, MobSignBlock.SIGN_RANGE_HORIZONTAL, MobSignBlock.SIGN_RANGE_VERTICAL);
					if(closestSign != null) {
						// RandomSource random = level.random;        			
						// BlockUtils.spawnHappyParticles(level, closestSign, random);
						return true;
					}
				}
        	}
        }
        return false;
    }

	@SubscribeEvent
	public static void endermanTeleport(final EntityTeleportEvent.EnderEntity event) {
		BlockPos fromPos = BlockPos.containing(event.getPrev());
		BlockPos toPos = BlockPos.containing(event.getTarget());
		if(cancelTeleport(event.getEntity().getType(), event.getEntityLiving().level(), fromPos)) {
			event.setCanceled(true);
		}
		if(cancelTeleport(event.getEntity().getType(), event.getEntityLiving().level(), toPos)) {
			event.setCanceled(true);
		}
	}
	
	public static boolean cancelTeleport(EntityType<?> entityType, LevelAccessor levelAccessor, BlockPos pos) {
        if (levelAccessor.isClientSide()) return true;
        ServerLevel level = ((ServerLevelAccessor) levelAccessor).getLevel();
        PoiManager poiManager = level.getPoiManager();
        
        if(entityType == EntityType.ENDERMAN || entityType == EntityType.SHULKER) {
    		ResourceKey<PoiType> poiType = getMobSignPoiType(MobSignBlock.MobSignType.ENDERMAN);
			if(poiType !=null){
				BlockPos closestSign = poiInRange(poiManager, poiType, pos, MobSignBlock.SMALL_SIGN_RANGE_HORIZONTAL, MobSignBlock.SMALL_SIGN_RANGE_VERTICAL);
				if(closestSign != null) {
					//RandomSource random = level.random;        			
					//BlockUtils.spawnHappyParticles(level, closestSign, random);
					return true;
				}
			}
    	}
        return false;
    }
	
	public static BlockPos poiInRange(PoiManager poiManager, ResourceKey<PoiType> poiType, BlockPos pos, int horizonal, int vertical) {
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
	public static void entityInteract(EntityInteract event) {
		Entity entity = event.getTarget();
		Player player = event.getEntity();
		ItemStack stack = event.getItemStack();
		if(entity !=null){
			if(entity instanceof AbstractVillager villager) {
				if(stack.is(ModInit.VILLAGER_AUTO_TRADER_BLOCK_ITEM.get())) {
					if(event.getSide() == LogicalSide.SERVER){
						stack.set(ModInit.AUTO_TRADER_VILLAGER_DATA_COMPONENT.get(), villager.getUUID());
					}
					event.setCancellationResult(InteractionResult.SUCCESS);
					return;
				}
				if(villager instanceof Villager realVillager){
					if(stack.is(ModInit.MAGIC_BONEMEAL.get())) {
						if(event.getSide() == LogicalSide.SERVER){
							realVillager.restock();
						}
						event.setCancellationResult(InteractionResult.SUCCESS);
						return;
					}					
				}
			}
		}
	}
}
