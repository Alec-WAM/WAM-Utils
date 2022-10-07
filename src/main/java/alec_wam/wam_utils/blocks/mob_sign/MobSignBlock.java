package alec_wam.wam_utils.blocks.mob_sign;

import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MobSignBlock extends Block implements SimpleWaterloggedBlock {
	public static final int SIGN_RANGE_HORIZONTAL = 128;
	public static final int SIGN_RANGE_VERTICAL = 64;
	
	public static final int SMALL_SIGN_RANGE_HORIZONTAL = 32;
	public static final int SMALL_SIGN_RANGE_VERTICAL = 32;
	
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;    
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
    
    private static final VoxelShape POLE_BOTTOM = Block.box(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D);
    private static final VoxelShape POLE_TOP = Block.box(7.0D, 0.0D, 7.0D, 9.0D, 12.0D, 9.0);
    private static final VoxelShape SIGN_NORTH = Shapes.or(POLE_TOP, Block.box(2.0D, 4.0D, 6.0D, 14.0D, 16.0D, 7.0));
    private static final VoxelShape SIGN_SOUTH = Shapes.or(POLE_TOP, Block.box(2.0D, 4.0D, 9.0D, 14.0D, 16.0D, 10.0));
    private static final VoxelShape SIGN_WEST = Shapes.or(POLE_TOP, Block.box(6.0D, 4.0D, 2.0D, 7.0D, 16.0D, 14.0));
    private static final VoxelShape SIGN_EAST = Shapes.or(POLE_TOP, Block.box(9.0D, 4.0D, 2.0D, 10.0D, 16.0D, 14.0));
    
    
    public MobSignBlock() {
    	super(Properties.of(Material.WOOD)
                .sound(SoundType.WOOD)
                .strength(1.5f)
                .noOcclusion()
            );
    	this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, Half.BOTTOM).setValue(WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    public VoxelShape getShape(BlockState p_54105_, BlockGetter p_54106_, BlockPos p_54107_, CollisionContext p_54108_) {
    	Half doubleblockhalf = p_54105_.getValue(HALF);
    	if(doubleblockhalf == Half.BOTTOM) {
    		return POLE_BOTTOM;
    	}
    	switch ((Direction)p_54105_.getValue(FACING)) {
	        
    		case NORTH: default: 
	        	return SIGN_NORTH;
	        case SOUTH:
	        	return SIGN_SOUTH;
	        case EAST:
	        	return SIGN_EAST;
	        case WEST:
	        	return SIGN_WEST;
        }
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
        FluidState fluidstate = p_48689_.getLevel().getFluidState(p_48689_.getClickedPos());
        if (blockpos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(blockpos.above()).canBeReplaced(p_48689_)) {
        	return this.defaultBlockState().setValue(FACING, p_48689_.getHorizontalDirection().getOpposite()).setValue(HALF, Half.BOTTOM).setValue(WATERLOGGED, Boolean.valueOf(fluidstate.getType() == Fluids.WATER));
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
    	p_48725_.add(FACING, HALF, WATERLOGGED);
    }

    @SuppressWarnings("deprecation")
	@Override
    public FluidState getFluidState(BlockState p_153492_) {
       return p_153492_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_153492_);
    }
    
    @Override
    public void setPlacedBy(Level p_52749_, BlockPos p_52750_, BlockState p_52751_, LivingEntity p_52752_, ItemStack p_52753_) {
    	FluidState fluidstate = p_52749_.getFluidState(p_52750_.above());
    	BlockState placedState = p_52751_.setValue(HALF, Half.TOP).setValue(WATERLOGGED, Boolean.valueOf(fluidstate.getType() == Fluids.WATER));
    	p_52749_.setBlock(p_52750_.above(), placedState, 3);
    }

    @Override
    public boolean canSurvive(BlockState p_52783_, LevelReader p_52784_, BlockPos p_52785_) {
       BlockPos blockpos = p_52785_.below();
       BlockState blockstate = p_52784_.getBlockState(blockpos);
       return p_52783_.getValue(HALF) == Half.BOTTOM ? blockstate.isFaceSturdy(p_52784_, blockpos, Direction.UP, SupportType.CENTER) : blockstate.is(this);
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
        if(player.isShiftKeyDown()) {
        	return InteractionResult.PASS;
        }
    	if (!level.isClientSide) {
    		int hori = SIGN_RANGE_HORIZONTAL;
    		int vert = SIGN_RANGE_VERTICAL;
    		
    		if(this == BlockInit.ENDERMAN_SIGN_BLOCK.get()) {
    			hori = SMALL_SIGN_RANGE_HORIZONTAL;
    			vert = SMALL_SIGN_RANGE_VERTICAL;
    		}
    		
        	String message = "Range: " + hori + "XZ " + vert + "Y";
        	((ServerPlayer)player).sendSystemMessage(Component.literal(message));
        }
        return InteractionResult.SUCCESS;
    }
}
