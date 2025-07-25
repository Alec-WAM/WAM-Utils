package alec_wam.wam_utils.common.blocks.conveyor;

import javax.annotation.Nullable;

import alec_wam.wam_utils.common.blocks.BaseEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class ConveyorBeltBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {

	public enum BeltSlope implements StringRepresentable {
		FLAT("flat"), UP("up"), DOWN("down");

		private final String name;

	    private BeltSlope(String name) {
	        this.name = name;
	    }

	    public String getName() {
	        return this.name;
	    }

	    @Override
	    public String toString() {
	        return this.name;
	    }

	    @Override
	    public String getSerializedName() {
	        return this.name;
	    }
	}
	
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 2.0);
    private static final VoxelShape SHAPE_SLOPE = Block.column(16.0, 0.0, 8.0);
    public static final EnumProperty<BeltSlope> SLOPE = EnumProperty.create("slope", BeltSlope.class);
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
	
	public ConveyorBeltBlock(Properties props) {
		super(props);
	}

	private boolean isConnected(LevelReader level, BlockPos origin, Direction side) {
	    BlockEntity be = level.getBlockEntity(origin.relative(side));
	    return be instanceof ConveyorBeltBE;
	}
	
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if(level.isClientSide) {
			return;
		}
		Direction beltFacing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
		BlockPos posUpFoward = pos.relative(beltFacing).above();
		BlockPos posUpBack = pos.relative(beltFacing.getOpposite()).above();	
		BlockState upStateForward = level.getBlockState(posUpFoward);
		BlockState upStateBackwards = level.getBlockState(posUpBack);
		if(upStateForward.getBlock() instanceof ConveyorBeltBlock) {
			level.setBlock(pos, state.setValue(SLOPE, BeltSlope.UP), 3);
		}
		else if(upStateBackwards.getBlock() instanceof ConveyorBeltBlock) {
			level.setBlock(pos, state.setValue(SLOPE, BeltSlope.DOWN), 3);
		}
	}
    
	@Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
	    BlockPos pos = context.getClickedPos();
	    Direction facing = context.getHorizontalDirection();
        return this.defaultBlockState()
        		.setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(LEFT, isConnected(level, pos, facing.getCounterClockWise()))
                .setValue(RIGHT, isConnected(level, pos, facing.getClockWise()))
                .setValue(SLOPE, BeltSlope.FLAT)
                .setValue(BlockStateProperties.WATERLOGGED, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder
        .add(BlockStateProperties.HORIZONTAL_FACING)
        .add(LEFT).add(RIGHT)
        .add(SLOPE)
        .add(BlockStateProperties.WATERLOGGED);
    }
    
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
    	super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
    }

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ConveyorBeltBE(pos, state);
	}
	
	@Override
	public boolean canBeReplaced(BlockState state, Fluid fluid) {
	    return super.canBeReplaced(state, fluid);
	}
	
	@Override
	public FluidState getFluidState(BlockState state) {
	    return super.getFluidState(state);
	}

	@Override
	public InteractionResult playerInteract(Level level, Player player, BlockPos blockPos, BlockHitResult hitResult, @Nullable ItemStack stack, @Nullable InteractionHand hand) {
		
		BlockEntity blockEntity = level.getBlockEntity(blockPos);
		
		if(blockEntity !=null && blockEntity instanceof ConveyorBeltBE conveyorBelt) {
			return conveyorBelt.playerInteract(player);
		}
		
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isValidBE(BlockEntity blockEntity) {
		return blockEntity instanceof ConveyorBeltBE;
	}

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess scheduledTickAccess,
            BlockPos pos,
            Direction facing,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random
     ) {
    	Direction beltFacing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if(state.getValueOrElse(SLOPE, BeltSlope.FLAT) == BeltSlope.FLAT) {
	    	if (facing == beltFacing.getCounterClockWise()) {
	            return state.setValue(LEFT, isConnected(level, pos, beltFacing.getCounterClockWise()));
	        } else if (facing == beltFacing.getClockWise()) {
	            return state.setValue(RIGHT, isConnected(level, pos, beltFacing.getClockWise()));
	        }
        }
        return state;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return state.getValueOrElse(SLOPE, BeltSlope.FLAT) == BeltSlope.FLAT ? SHAPE : SHAPE_SLOPE;
    }
    
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValueOrElse(SLOPE, BeltSlope.FLAT) == BeltSlope.FLAT ? SHAPE : SHAPE_SLOPE;
    }
    
    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathType) {
    	return false;
    }
    
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
    	BlockEntity blockEntity = level.getBlockEntity(pos);
    	if(blockEntity !=null && blockEntity instanceof ConveyorBeltBE conveyorBelt) {
			return ItemHandlerHelper.calcRedstoneFromInventory(conveyorBelt.getItemHandler());
		}
        return 0;
    }

}
