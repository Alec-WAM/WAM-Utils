package alec_wam.wam_utils.common.helpers;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public class EntityHelper {
    public static final double DEFAULT_ATTACK_REACH = Math.sqrt(2.04F) - 0.6F;
    public static final Vec3 DEFAULT_ATTACK_REACH_RANGE = new Vec3(DEFAULT_ATTACK_REACH, 0, DEFAULT_ATTACK_REACH);

	public static final Comparator<Entity> getEntityDistanceComparator(LivingEntity entity) {
		return (e1, e2) -> {
			double dist1 = e1.distanceToSqr(entity);
			double dist2 = e2.distanceToSqr(entity);
			return Double.compare(dist1, dist2);
		};
	}

	public static boolean isCloseToBlockPos(Entity entity, BlockPos pos, double range) {
		if(pos == null)return false;
		return pos.closerToCenterThan(entity.position(), range);
	}
	
	public static void throwItemsTowardRandomPos(LivingEntity entity, InteractionHand hand, List<ItemStack> items) {
		Vec3 pos = (entity !=null && entity instanceof PathfinderMob) ? getRandomNearbyPos((PathfinderMob)entity) : entity.position();
		throwItemsTowardPos(entity, hand, items, pos);
	}
	
	public static void throwItemsTowardEntity(LivingEntity entity, Entity otherEntity, InteractionHand hand, List<ItemStack> items) {
		throwItemsTowardPos(entity, hand, items, otherEntity.position());
	}
	
	public static void throwItemsTowardPos(LivingEntity entity, InteractionHand hand, List<ItemStack> items, Vec3 pos) {
		if (!items.isEmpty()) {
			entity.swing(hand);

			for (ItemStack itemstack : items) {
				BehaviorUtils.throwItem(entity, itemstack, pos.add(0.0D, 1.0D, 0.0D));
			}
		}
	}
	
	public static Vec3 getRandomNearbyPos(PathfinderMob entity) {
		Vec3 vec3 = LandRandomPos.getPos(entity, 4, 2);
		return vec3 == null ? entity.position() : vec3;
	}
	

	
	public static boolean canFitItemInHand(LivingEntity entity, ItemStack stack, InteractionHand hand) {
		ItemStack handItem = entity.getItemInHand(hand);
		if (handItem.isEmpty()
				|| ItemStack.isSameItemSameComponents(handItem, stack) && handItem.getCount() < handItem.getMaxStackSize()) {
			return true;
		}
		return false;
	}
	
	public static ItemStack addItemToHand(LivingEntity entity, ItemStack stack, InteractionHand hand) {
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		} else {
			ItemStack handItem = entity.getItemInHand(hand);
			ItemStack itemstack = stack.copy();
			if (ItemStack.isSameItemSameComponents(handItem, itemstack)) {
	            moveItemsBetweenStacks(itemstack, handItem);
	            if (itemstack.isEmpty()) {
	            	return ItemStack.EMPTY;
	            }
	            return itemstack;
	         }
			if (itemstack.isEmpty()) {
				return ItemStack.EMPTY;
			} else {
				entity.setItemInHand(hand, itemstack.copyAndClear());
				return itemstack.isEmpty() ? ItemStack.EMPTY : itemstack;
			}
		}
	}
	
	private static void moveItemsBetweenStacks(ItemStack p_19186_, ItemStack p_19187_) {
		int i = Math.min(64, p_19187_.getMaxStackSize());
		int j = Math.min(p_19186_.getCount(), i - p_19187_.getCount());
		if (j > 0) {
			p_19187_.grow(j);
			p_19186_.shrink(j);
		}

	}
	
	public static boolean canWalkHere(Level level, BlockPos pos) {
		BlockState at = level.getBlockState(pos);
		BlockState below = level.getBlockState(pos.below());
				
		// Must be air and have solid block below
		if (at.getCollisionShape(level, pos).isEmpty() 
			&& Block.isFaceFull(below.getCollisionShape(level, pos.below()), Direction.UP) 
			&& at.getFluidState().isEmpty()
			&& level.getFluidState(pos).isEmpty()
		) {
			return true;
		}
		
		return false;
	}

    /**
     * Finds a walkable position for a given Mob entity starting from a specified center position. 
     * It checks if the center position is walkable and, if not, looks for adjacent positions that are.
     *
     * @param entity The Mob entity for which a walkable position is being determined.
     * @param center The central BlockPos to start searching for a walkable or adjacent position.
     * @return A BlockPos that is walkable, either the center if walkable or an adjacent position.
     */

	public static BlockPos findWalkableAtOrAdjacentPos(Mob entity, BlockPos center) {
		Level level = entity.level();
		if(canWalkHere(level, center)){
			return center;
		}		
		return findWalkableAdjacentPos(entity, center);
	}

	public static BlockPos findWalkableAdjacentPos(Mob entity, BlockPos center) {
	    Level level = entity.level();
	    for(BlockPos customCenter : new BlockPos[]{center, center.above(), center.below()}) {	        
			for (Direction dir : Direction.Plane.HORIZONTAL) {
				BlockPos candidate = customCenter.relative(dir);
				
				// Must be air and have solid block below
				if (canWalkHere(level, candidate)) {
					return candidate;
				}
			}
	    }
	    return center; // fallback: return the center block if nothing found
	}

	public static FakePlayer getFakePlayer(Level level, @Nullable UUID uuid) {
		if (level == null || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
			return null;
		}
		String name = "[WAMUtils Fake Player]";
		if (uuid == null) {
			uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
		}
		GameProfile gameProfile = new GameProfile(uuid, name);
		return FakePlayerFactory.get(serverLevel, gameProfile);
	}

	public static <T extends Entity> Pair<T, T> findClosestPair(List<T> entities) {
		if(entities == null || entities.size() < 2) {
			return null; // Not enough entities to form a pair
		}
		T closestA = null;
		T closestB = null;
		double closestDistance = Double.MAX_VALUE;

		for (int i = 0; i < entities.size(); i++) {
			for (int j = i + 1; j < entities.size(); j++) {
				double distance = entities.get(i).distanceToSqr(entities.get(j));
				if (distance < closestDistance) {
					closestDistance = distance;
					closestA = entities.get(i);
					closestB = entities.get(j);
				}
			}
		}

		return (closestA != null && closestB != null) ? new Pair<T, T>(closestA, closestB) : null;
	}

	public static float getKnockback(LivingEntity attacker, Entity target, DamageSource damageSource) {
        float f = (float)attacker.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        return attacker.level() instanceof ServerLevel serverlevel
            ? EnchantmentHelper.modifyKnockback(serverlevel, attacker.getWeaponItem(), target, damageSource, f)
            : f;
    }

	public static boolean attackEntity(ServerLevel level, LivingEntity attacker, Entity target) {
		if (attacker == null || target == null || !target.isAttackable()) {
			return false;
		}
		float f = (float)attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
        ItemStack itemstack = attacker.getWeaponItem();
        DamageSource damagesource = Optional.ofNullable(itemstack.getItem().getDamageSource(attacker)).orElse(attacker.damageSources().mobAttack(attacker));
        f = EnchantmentHelper.modifyDamage(level, itemstack, target, damagesource, f);
        f += itemstack.getItem().getAttackDamageBonus(target, f, damagesource);
        boolean flag = target.hurtServer(level, damagesource, f);
        boolean postHurt = false;
		if (flag) {
            float f1 = getKnockback(attacker, target, damagesource);
            if (f1 > 0.0F && target instanceof LivingEntity livingentity) {
                livingentity.knockback(f1 * 0.5F, Mth.sin(attacker.getYRot() * (float) (Math.PI / 180.0)), -Mth.cos(attacker.getYRot() * (float) (Math.PI / 180.0)));
                attacker.setDeltaMovement(attacker.getDeltaMovement().multiply(0.6, 1.0, 0.6));
            }

            if (!itemstack.isEmpty() && target instanceof LivingEntity livingentity1) {
                postHurt = itemstack.hurtEnemy(livingentity1, attacker);
            }

            EnchantmentHelper.doPostAttackEffects(level, target, damagesource);
            attacker.setLastHurtMob(target);

			if(postHurt && !itemstack.isEmpty() && target instanceof LivingEntity livingentity1){
				itemstack.postHurtEnemy(livingentity1, attacker);
			}
        }
		return flag;
	}

	public static Vec3 getEntityThirdPersonHandPos(LivingEntity entity, HumanoidArm arm, float scale, float partialTick){
		int i = arm == HumanoidArm.RIGHT ? 1 : -1;
		float f = Mth.lerp(partialTick, entity.yBodyRotO, entity.yBodyRot) * (float) (Math.PI / 180.0);
		double d0 = Mth.sin(f);
		double d1 = Mth.cos(f);
		double d2 = i * 0.35 * scale;
		double d3 = 0.8 * scale;
		float f2 = entity.isCrouching() ? -0.1875F : 0.0F;
		return entity.getEyePosition(partialTick).add(-d1 * d2 - d0 * d3, f2 - 0.45 * scale, -d0 * d2 + d1 * d3);
	}

	public static boolean isLookingAtHorizontally(LivingEntity entity, BlockPos pos, double maxDegrees){
		return isLookingAtHorizontally(entity, Vec3.atCenterOf(pos), maxDegrees);
	}

	public static boolean isLookingAtHorizontally(LivingEntity entity, Entity targetEntity, double maxDegrees){
		Vec3 targetEntityPos = targetEntity.position();
		return isLookingAtHorizontally(entity, targetEntityPos, maxDegrees);
	}

	public static boolean isLookingAtHorizontally(LivingEntity entity, Vec3 target, double maxDegrees) {
		Vec3 entityPos = entity.position();
		// Horizontal delta only
		double dx = target.x - entityPos.x;
		double dz = target.z - entityPos.z;

		// Angle from entity to target in world space
		double angleToTarget = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;

		// Entity yaw is in -180 to 180, atan2 gives -180 to 180
		float entityYaw = entity.yBodyRot;

		// Convert to same reference
		float angleDifference = Math.abs(Mth.wrapDegrees((float) angleToTarget - entityYaw));
		// System.out.println( angleToTarget + " / " + angleDifference);
		return angleDifference <= maxDegrees;
	}

	public static boolean isWithinMeleeAttackRange(LivingEntity attacker, Entity target) {
		return isWithinMeleeAttackRange(attacker, target, DEFAULT_ATTACK_REACH_RANGE);
	}

	public static boolean isWithinMeleeAttackRange(LivingEntity attacker, Entity target, Vec3 range) {
        Entity vehicle = attacker.getVehicle();
        AABB aabb;
        if (vehicle != null) {
            AABB aabb1 = vehicle.getBoundingBox();
            AABB aabb2 = attacker.getBoundingBox();
            aabb = new AABB(
                Math.min(aabb2.minX, aabb1.minX),
                aabb2.minY,
                Math.min(aabb2.minZ, aabb1.minZ),
                Math.max(aabb2.maxX, aabb1.maxX),
                aabb2.maxY,
                Math.max(aabb2.maxZ, aabb1.maxZ)
            );
        } else {
            aabb = attacker.getBoundingBox();
        }

        AABB attackAABB = aabb.inflate(range.x, range.y, range.z);
		
		return attackAABB.intersects(getEntityHitbox(target));
    }	

    public static AABB getEntityHitbox(Entity entity) {
        AABB aabb = entity.getBoundingBox();
        Entity vehicle = entity.getVehicle();
        if (vehicle != null) {
            Vec3 vec3 = vehicle.getPassengerRidingPosition(entity);
            return aabb.setMinY(Math.max(vec3.y, aabb.minY));
        } else {
            return aabb;
        }
    }

	public static int getOfferIndex(MerchantOffers offers, MerchantOffer offer) {
		for (int i = 0; i < offers.size(); i++) {
			MerchantOffer merchantoffer = offers.get(i);
			if (merchantoffer.satisfiedBy(offer.getCostA(), offer.getCostB()) && ItemStack.isSameItemSameComponents(offer.getResult(), merchantoffer.getResult())) {
				return i;
			}
		}
		return -1;
	}
}
