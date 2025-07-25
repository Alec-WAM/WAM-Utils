package alec_wam.wam_utils.common.items;

import java.util.Objects;

import javax.annotation.Nullable;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class WorkerSpawnItem extends Item {
	
	public WorkerSpawnItem(Properties properties) {
		super(properties);
	}
	
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (!(level instanceof ServerLevel)) {
			return InteractionResult.SUCCESS;
		} else {
			ItemStack itemstack = context.getItemInHand();
			BlockPos blockpos = context.getClickedPos();
			Direction direction = context.getClickedFace();
			spawnMinion(level, blockpos, direction, context.getPlayer(), itemstack);

			return InteractionResult.CONSUME;
		}
	}

	public static WorkerEntity spawnMinion(Level level, BlockPos pos, Direction direction, @Nullable Player player, ItemStack stack) {
		BlockState blockstate = level.getBlockState(pos);
		BlockPos blockpos1;
		if (blockstate.getCollisionShape(level, pos).isEmpty()) {
			blockpos1 = pos;
		} else {
			blockpos1 = pos.relative(direction);
		}
		EntityType<?> entitytype = ModInit.WORKER_ENTITY.get();
		Entity entity = entitytype.spawn((ServerLevel) level, stack, player, blockpos1,
				EntitySpawnReason.SPAWN_ITEM_USE, true,
				!Objects.equals(pos, blockpos1) && direction == Direction.UP);
		if (entity != null) {
			if(entity instanceof WorkerEntity) {
//				if(player !=null) {
//					((WorkerEntity)entity).setSkin(player.getGameProfile());
//				}
				((WorkerEntity)entity).setOwner(player);
				entity.lookAt(Anchor.EYES, Anchor.EYES.apply(player));
				return (WorkerEntity)entity;
			}
			stack.shrink(1);
			level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
		}
		return null;
	}
	
}
