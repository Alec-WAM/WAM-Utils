package alec_wam.wam_utils.blocks.mirror_block;

import java.util.List;

import javax.annotation.Nullable;

import alec_wam.wam_utils.utils.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class BlockMirrorItem extends BlockItem {

	public BlockMirrorItem(Block block, Properties props) {
		super(block, props);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flags) {
		super.appendHoverText(stack, null, tooltip, flags);
		if (stack.hasTag()) {
			CompoundTag tag = stack.getTag();
			if (tag.contains("LinkedPos")) {
				BlockPos blockPos = BlockUtils.loadBlockPos(tag, "LinkedPos");
				tooltip.add(Component.literal(blockPos.toString()));
			}
		}
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack heldStack = player.getItemInHand(hand);

		if (player.isShiftKeyDown()) {
			heldStack.removeTagKey("LinkedPos");
			return InteractionResultHolder.success(heldStack);
		}

		return InteractionResultHolder.pass(heldStack);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Player player = context.getPlayer();
		ItemStack heldStack = context.getItemInHand();

		if (player.isShiftKeyDown()) {
			CompoundTag tag = heldStack.getOrCreateTag();
			tag.put("LinkedPos", BlockUtils.saveBlockPos(pos));
			heldStack.setTag(tag);
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		return super.useOn(context);
	}

}
