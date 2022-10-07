package alec_wam.wam_utils.blocks.entity_pod;

import javax.annotation.Nullable;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public abstract class AbstractEntityPodBlock extends WAMUtilsBlockEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
    
    public AbstractEntityPodBlock() {
    	super(Properties.of(Material.GLASS)
                .sound(SoundType.GLASS)
                .strength(5.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion()
            );
    	this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, Half.BOTTOM));
    }

    @SuppressWarnings("deprecation")
	@Override
    public BlockState updateShape(BlockState p_52796_, Direction p_52797_, BlockState p_52798_, LevelAccessor p_52799_, BlockPos p_52800_, BlockPos p_52801_) {
    	Half doubleblockhalf = p_52796_.getValue(HALF);
       if (p_52797_.getAxis() == Direction.Axis.Y && doubleblockhalf == Half.BOTTOM == (p_52797_ == Direction.UP)) {
          return p_52798_.is(this) && p_52798_.getValue(HALF) != doubleblockhalf ? p_52796_.setValue(FACING, p_52798_.getValue(FACING)) : Blocks.AIR.defaultBlockState();
       } else {
          return doubleblockhalf == Half.BOTTOM && p_52797_ == Direction.DOWN && !p_52796_.canSurvive(p_52799_, p_52800_) ? Blocks.AIR.defaultBlockState() : super.updateShape(p_52796_, p_52797_, p_52798_, p_52799_, p_52800_, p_52801_);
       }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_48689_) {
    	BlockPos blockpos = p_48689_.getClickedPos();
        Level level = p_48689_.getLevel();
        if (blockpos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(blockpos.above()).canBeReplaced(p_48689_)) {
        	return this.defaultBlockState().setValue(FACING, p_48689_.getHorizontalDirection().getOpposite()).setValue(HALF, Half.BOTTOM);
        }
        return null;
    }

    @Override
    public void playerWillDestroy(Level p_52755_, BlockPos p_52756_, BlockState p_52757_, Player p_52758_) {
       if (!p_52755_.isClientSide && p_52758_.isCreative()) {
    	   Half doubleblockhalf = p_52757_.getValue(HALF);
    	   if (doubleblockhalf == Half.TOP) {
    		   BlockPos blockpos = p_52756_.below();
    		   BlockState blockstate = p_52755_.getBlockState(blockpos);
    		   if (blockstate.is(p_52757_.getBlock()) && blockstate.getValue(HALF) == Half.BOTTOM) {
    			   BlockState blockstate1 = Blocks.AIR.defaultBlockState();
    			   p_52755_.setBlock(blockpos, blockstate1, 35);
    			   p_52755_.levelEvent(p_52758_, 2001, blockpos, Block.getId(blockstate));
    		   }
    	   }
       }

       super.playerWillDestroy(p_52755_, p_52756_, p_52757_, p_52758_);
    }
    
    @Override
    public BlockState rotate(BlockState p_48722_, Rotation p_48723_) {
        return p_48722_.setValue(FACING, p_48723_.rotate(p_48722_.getValue(FACING)));
    }

    @SuppressWarnings("deprecation")
	@Override
    public BlockState mirror(BlockState p_48719_, Mirror p_48720_) {
        return p_48719_.rotate(p_48720_.getRotation(p_48719_.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_48725_) {
    	p_48725_.add(FACING, HALF);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    	if(state.getValue(HALF) == Half.BOTTOM) {
    		return createBlockEntity(pos, state);
    	}
    	return null;
    }
    
    public abstract AbstractEntityPodBE<?> createBlockEntity(BlockPos pos, BlockState state);
    
    @Override
    public void setPlacedBy(Level p_52749_, BlockPos p_52750_, BlockState p_52751_, LivingEntity p_52752_, ItemStack p_52753_) {
    	super.setPlacedBy(p_52749_, p_52750_, p_52751_, p_52752_, p_52753_);
    	p_52749_.setBlock(p_52750_.above(), p_52751_.setValue(HALF, Half.TOP), 3);
    }

    @Override
    public boolean canSurvive(BlockState p_52783_, LevelReader p_52784_, BlockPos p_52785_) {
       BlockPos blockpos = p_52785_.below();
       BlockState blockstate = p_52784_.getBlockState(blockpos);
       return p_52783_.getValue(HALF) == Half.BOTTOM ? true : blockstate.is(this);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return (lvl, pos, stt, te) -> {
                if (te !=null && te instanceof AbstractEntityPodBE<?> pod) pod.tickServer();
            };
        }
        else {
        	return (lvl, pos, stt, te) -> {
                if (te !=null && te instanceof AbstractEntityPodBE<?> pod) pod.tickClient();
            };
        }
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
        if (!level.isClientSide) {
        	BlockPos bePos = pos;
        	if(state.getValue(HALF) == Half.TOP) {
        		bePos = pos.below();
        	}
            BlockEntity be = level.getBlockEntity(bePos);
            if (be instanceof AbstractEntityPodBE<?> pod) {
                NetworkHooks.openScreen((ServerPlayer) player, pod, be.getBlockPos());
            } 
        }
        return InteractionResult.SUCCESS;
    }
}
