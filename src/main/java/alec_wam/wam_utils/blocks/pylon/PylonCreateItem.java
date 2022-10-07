package alec_wam.wam_utils.blocks.pylon;

import java.util.List;

import javax.annotation.Nullable;

import alec_wam.wam_utils.blocks.pylon.PylonBlock.PylonLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class PylonCreateItem extends BlockItem {

	public PylonCreateItem(Block block, Properties props) {
		super(block, props);
	}

	@Override
	public InteractionResult place(BlockPlaceContext context) {
	      if (!context.canPlace()) {
	         return InteractionResult.FAIL;
	      } else {
	         BlockPlaceContext blockplacecontext = this.updatePlacementContext(context);
	         if (blockplacecontext == null) {
	            return InteractionResult.FAIL;
	         } else {
	        	 if(!validMultiBlockPosition(context)) {
	        		 return InteractionResult.FAIL;
	        	 }
	        	 
	        	 Level level = blockplacecontext.getLevel();
	        	 BlockPos blockpos = blockplacecontext.getClickedPos();
	        	 BlockPos middle = blockpos.below(1);
	        	 BlockPos bottom = blockpos.below(2);
	        	 
	        	 BlockState defaultBlock = this.getBlock().defaultBlockState();
	        	 BlockState topBlock = defaultBlock.setValue(PylonBlock.PYLON_LEVEL, PylonLevel.TOP);
	        	 BlockState middleBlock = defaultBlock.setValue(PylonBlock.PYLON_LEVEL, PylonLevel.MIDDLE);
	        	 BlockState bottomBlock = defaultBlock.setValue(PylonBlock.PYLON_LEVEL, PylonLevel.BOTTOM);
	        	 
	        	 boolean placedTop = level.setBlock(blockpos, topBlock, 11);
	        	 boolean placedMiddle = level.setBlock(middle, middleBlock, 11);
	        	 boolean placedBottom = level.setBlock(bottom, bottomBlock, 11);
	        	 
	        	 if(placedTop && placedMiddle && placedBottom) {
	        		 Player player = blockplacecontext.getPlayer();
	        		 ItemStack itemstack = blockplacecontext.getItemInHand();
	        		 level.gameEvent(GameEvent.BLOCK_PLACE, blockpos, GameEvent.Context.of(player, topBlock));
	        		 BlockState stoneBrickState = Blocks.STONE_BRICKS.defaultBlockState();
	        		 level.gameEvent(GameEvent.BLOCK_DESTROY, middle, GameEvent.Context.of(player, stoneBrickState));
	        		 level.gameEvent(GameEvent.BLOCK_DESTROY, bottom, GameEvent.Context.of(stoneBrickState));
	        		 level.levelEvent(2001, middle, Block.getId(stoneBrickState));
	        		 level.levelEvent(2001, bottom, Block.getId(stoneBrickState));
	        		 SoundType soundtype = topBlock.getSoundType(level, blockpos, player);
	        		 level.playSound(player, blockpos, this.getPlaceSound(topBlock, level, blockpos, player), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
	        		 if (player == null || !player.getAbilities().instabuild) {
		                  itemstack.shrink(1);
	        		 }
	
	        		 return InteractionResult.sidedSuccess(level.isClientSide);
	        	 }
        		 return InteractionResult.FAIL;
	         }
	      }
	   }
	
	@Override
	public String getDescriptionId() {
      return getOrCreateDescriptionId();
   }

	@Override
	public void appendHoverText(ItemStack p_40572_, @Nullable Level p_40573_, List<Component> p_40574_, TooltipFlag p_40575_) {

	}
	
	@Override
	protected boolean canPlace(BlockPlaceContext p_40611_, BlockState p_40612_) {
      //Player player = p_40611_.getPlayer();
      //CollisionContext collisioncontext = player == null ? CollisionContext.empty() : CollisionContext.of(player);
      return (!this.mustSurvive() || validMultiBlockPosition(p_40611_)); //&& p_40611_.getLevel().isUnobstructed(p_40612_, p_40611_.getClickedPos(), collisioncontext);
	}
	
	public boolean validMultiBlockPosition(BlockPlaceContext context) {
		BlockPos placePos = context.getClickedPos();
		BlockState middle = context.getLevel().getBlockState(placePos.below(1));
		BlockState bottom = context.getLevel().getBlockState(placePos.below(2));
		BlockState floor = context.getLevel().getBlockState(placePos.below(3));
		boolean solidFloor = !floor.isAir() && floor.isFaceSturdy(context.getLevel(), placePos.below(3), Direction.UP);
		return middle.is(BlockTags.STONE_BRICKS) && bottom.is(BlockTags.STONE_BRICKS) && solidFloor;
	}
}
