package alec_wam.wam_utils.items;

import alec_wam.wam_utils.WAMUtilsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;

public class WitherBonemealItem extends BoneMealItem {

	public WitherBonemealItem(Properties props) {
		super(props);
	}
	
	@Override
	public InteractionResult useOn(UseOnContext p_40637_) {
		Level level = p_40637_.getLevel();
		BlockPos blockpos = p_40637_.getClickedPos();
		BlockPos blockpos1 = blockpos.relative(p_40637_.getClickedFace());
		if (applySpecialBonemeal(p_40637_.getItemInHand(), level, blockpos, p_40637_.getPlayer())) {
			if (!level.isClientSide) {
				level.levelEvent(1505, blockpos, 0);
			}

			return InteractionResult.sidedSuccess(level.isClientSide);
		} else {
			BlockState blockstate = level.getBlockState(blockpos);
			boolean flag = blockstate.isFaceSturdy(level, blockpos, p_40637_.getClickedFace());
			if (flag && growWaterPlant(p_40637_.getItemInHand(), level, blockpos1, p_40637_.getClickedFace())) {
				if (!level.isClientSide) {
					level.levelEvent(1505, blockpos1, 0);
				}

				return InteractionResult.sidedSuccess(level.isClientSide);
			} else {
				return InteractionResult.PASS;
			}
		}
	}
	
	public boolean applySpecialBonemeal(ItemStack stack, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
		BlockState blockstate = level.getBlockState(pos);
		
		if(blockstate.getBlock() == Blocks.NETHER_WART) {
			int age = blockstate.getValue(NetherWartBlock.AGE);
			if(age < NetherWartBlock.MAX_AGE) {
				if (!level.isClientSide) {				
					Integer newAge = Integer.valueOf(age + 1);	
					BlockState newState = blockstate.getBlock().defaultBlockState().setValue(NetherWartBlock.AGE, newAge);
					level.setBlock(pos, newState, 2);
					stack.shrink(1);
				}
				return true;
			}
			return false;
		}

		return applyBonemeal(stack, level, pos, player);
	}

}
