package alec_wam.wam_utils.blocks.entity_pod;

import alec_wam.wam_utils.init.ItemInit;
import alec_wam.wam_utils.utils.InventoryUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class EntityPodItem extends Item {

	public EntityPodItem(Properties p_41383_) {
		super(p_41383_);
	}
	
	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
		if (entity instanceof Piglin piglin) {
			if (piglin.isAlive() && !piglin.isBaby() && !piglin.isAggressive()) {
				if (!player.level.isClientSide) {
					InventoryUtils.givePlayerItem(player, new ItemStack(ItemInit.PIGLIN_TRADING_POD_ITEMBLOCK.get(), 1));
					RandomSource random = player.level.random;
					for(int i = 0; i < 20; ++i) {
						double d0 = random.nextGaussian() * 0.02D;
						double d1 = random.nextGaussian() * 0.02D;
						double d2 = random.nextGaussian() * 0.02D;
						((ServerLevel)player.level).sendParticles(ParticleTypes.POOF, entity.getRandomX(1.0D), entity.getRandomY(), entity.getRandomZ(1.0D), 0, d0, d1, d2, 1.0D);
					}
					PiglinAi.angerNearbyPiglins(player, true);
					entity.discard();
					stack.shrink(1);
				}

				return InteractionResult.sidedSuccess(player.level.isClientSide);
			}
		}
		if (entity instanceof Witch witch) {
			if (witch.isAlive() && !witch.isBaby() && !witch.isAggressive()) {
				if (!player.level.isClientSide) {
					InventoryUtils.givePlayerItem(player, new ItemStack(ItemInit.WITCH_TRADING_POD_ITEMBLOCK.get(), 1));
					RandomSource random = player.level.random;
					for(int i = 0; i < 20; ++i) {
						double d0 = random.nextGaussian() * 0.02D;
						double d1 = random.nextGaussian() * 0.02D;
						double d2 = random.nextGaussian() * 0.02D;
						((ServerLevel)player.level).sendParticles(ParticleTypes.POOF, entity.getRandomX(1.0D), entity.getRandomY(), entity.getRandomZ(1.0D), 0, d0, d1, d2, 1.0D);
					}
					entity.discard();
					stack.shrink(1);
				}

				return InteractionResult.sidedSuccess(player.level.isClientSide);
			}
		}

		return InteractionResult.PASS;
	}

}
