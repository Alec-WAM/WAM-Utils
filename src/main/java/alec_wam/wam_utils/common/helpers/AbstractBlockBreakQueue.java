package alec_wam.wam_utils.common.helpers;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;

public abstract class AbstractBlockBreakQueue {
	protected Consumer<BlockPos> makeCallbackFor(Level world, float effectChance, Function<BlockState, ItemStack> tool,
		@Nullable LivingEntity entity, BiConsumer<BlockPos, ItemStack> drop) {
		return pos -> {
			BlockState state = world.getBlockState(pos);
			ItemStack toDamage = tool.apply(state);
			ItemStack usedTool = toDamage.copy();
			BlockHelper.destroyBlockAs(world, pos, entity, toDamage, effectChance,
				stack -> drop.accept(pos, stack));
			Player playerEntity = entity instanceof Player ? ((Player) entity) : null;
			if (toDamage.isEmpty() && !usedTool.isEmpty() && playerEntity !=null)
				EventHooks.onPlayerDestroyItem(playerEntity, usedTool, InteractionHand.MAIN_HAND);
		};
	}

	public void destroyBlocks(Level world, @Nullable LivingEntity entity, BiConsumer<BlockPos, ItemStack> drop) {
		Player playerEntity = entity instanceof Player ? ((Player) entity) : null;
		ItemStack toDamage =
			playerEntity != null && !playerEntity.isCreative() ? playerEntity.getMainHandItem() : ItemStack.EMPTY;
		destroyBlocks(world, (state) -> {
			return toDamage;
		}, playerEntity, drop);
	}

	public abstract void destroyBlocks(Level world, Function<BlockState, ItemStack> tool, @Nullable LivingEntity playerEntity,
		BiConsumer<BlockPos, ItemStack> drop);
}
