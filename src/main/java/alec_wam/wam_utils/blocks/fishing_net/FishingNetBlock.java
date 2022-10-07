package alec_wam.wam_utils.blocks.fishing_net;

import javax.annotation.Nullable;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FishingNetBlock extends WAMUtilsBlockEntityBlock {
	protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    
    public FishingNetBlock() {
    	super(Properties.of(Material.WOOD)
                .sound(SoundType.WOOD)
                .strength(1.0f)
                .noOcclusion()
            );
    	this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_48689_) {
    	return this.defaultBlockState().setValue(FACING, p_48689_.getHorizontalDirection());
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
    	p_48725_.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState p_51309_, BlockGetter p_51310_, BlockPos p_51311_, CollisionContext p_51312_) {
       return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState p_51307_) {
       return RenderShape.MODEL;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FishingNetBE(pos, state);
    }
    
    @Override
    public void tick(BlockState p_222543_, ServerLevel p_222544_, BlockPos p_222545_, RandomSource p_222546_) {
    	if (!p_222543_.canSurvive(p_222544_, p_222545_)) {
    		p_222544_.destroyBlock(p_222545_, true);
    	}
    }

    @SuppressWarnings({ "deprecation" })
    @Override
    public BlockState updateShape(BlockState p_57179_, Direction p_57180_, BlockState p_57181_, LevelAccessor p_57182_, BlockPos p_57183_, BlockPos p_57184_) {
    	if (!p_57179_.canSurvive(p_57182_, p_57183_)) {
    		p_57182_.scheduleTick(p_57183_, this, 1);
    	}
    	return super.updateShape(p_57179_, p_57180_, p_57181_, p_57182_, p_57183_, p_57184_);
    }
    
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
    	FluidState fluidstate = level.getFluidState(pos.below());
    	BlockState blockstate = level.getBlockState(pos.below());
    	return fluidstate.is(FluidTags.WATER) && fluidstate.isSource() && blockstate.getCollisionShape(level, pos.below()).isEmpty();
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return (lvl, pos, stt, te) -> {
                if (te instanceof FishingNetBE net) net.tickServer();
            };
        }
        else {
        	return (lvl, pos, stt, te) -> {
                if (te instanceof FishingNetBE net) net.tickClient();
            };
        }
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FishingNetBE) {
        	FishingNetBE net = (FishingNetBE)be;
        	if(net.hasFish()) {
        		if(!level.isClientSide) {
        			net.dropFishItems();
        		}
        		return InteractionResult.SUCCESS;
        	}        	
        }
        return InteractionResult.PASS;
    }
}
