package alec_wam.wam_utils.blocks.shield_rack;

import javax.annotation.Nullable;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntityBlock;
import alec_wam.wam_utils.utils.ItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ShieldRackBlock extends WAMUtilsBlockEntityBlock implements SimpleWaterloggedBlock {
	protected static final VoxelShape SHAPE_NORTH = Block.box(0.0D, 1.0D, 15.0D, 16.0D, 15.0D, 16.0D);
	protected static final VoxelShape SHAPE_SOUTH = Block.box(0.0D, 1.0D, 0.0D, 16.0D, 15.0D, 1.0D);
	protected static final VoxelShape SHAPE_EAST = Block.box(0.0D, 1.0D, 0.0D, 1.0D, 15.0D, 16.0D);
	protected static final VoxelShape SHAPE_WEST = Block.box(15.0D, 1.0D, 0.0D, 16.0D, 15.0D, 16.0D);
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    
    public ShieldRackBlock() {
    	//TODO Map color
    	super(Properties.of(Material.WOOD)
                .sound(SoundType.WOOD)
                .strength(1.5f)
                .noOcclusion()
            );
    	this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_48689_) {
    	FluidState fluidstate = p_48689_.getLevel().getFluidState(p_48689_.getClickedPos());
    	return this.defaultBlockState().setValue(FACING, p_48689_.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, Boolean.valueOf(fluidstate.getType() == Fluids.WATER));
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
    	p_48725_.add(FACING, WATERLOGGED);
    }

    @SuppressWarnings("deprecation")
	@Override
    public FluidState getFluidState(BlockState p_153492_) {
       return p_153492_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_153492_);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter p_51310_, BlockPos p_51311_, CollisionContext p_51312_) {
       Direction dir = state.getValue(FACING);
       if(dir == Direction.SOUTH) {
    	   return SHAPE_SOUTH;
       }
       if(dir == Direction.EAST) {
    	   return SHAPE_EAST;
       }
       if(dir == Direction.WEST) {
    	   return SHAPE_WEST;
       }
       return SHAPE_NORTH;
    }
    
    @Override
    public boolean hasAnalogOutputSignal(BlockState p_151986_) {
    	return true;
    }
    
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
    	int comparatorValue = 0;
    	BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity !=null && blockentity instanceof ShieldRackBE be) {
        	if(!be.getLeftStack().isEmpty()) {
        		comparatorValue += 1;
        	}
        	if(!be.getRightStack().isEmpty()) {
        		comparatorValue += 2;
        	}
        	if(!be.getShieldStack().isEmpty()) {
        		comparatorValue += 4;
        	}
        }
    	return comparatorValue;
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
    	Direction rotation = state.getValue(FACING);
    	BlockPos otherPos = pos.relative(rotation.getOpposite());
    	return level.getBlockState(otherPos).isFaceSturdy(level, otherPos, rotation);
    }

    @Override
    public RenderShape getRenderShape(BlockState p_51307_) {
       return RenderShape.MODEL;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShieldRackBE(pos, state);
    }
    
    @Override
    public void dropInventoryItems(Level level, BlockPos pos) {
    	BlockEntity blockentity = level.getBlockEntity(pos);
		if (blockentity instanceof ShieldRackBE be) {
			Containers.dropItemStack(level, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), be.getLeftStack());
			Containers.dropItemStack(level, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), be.getShieldStack());
			Containers.dropItemStack(level, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), be.getRightStack());
			level.updateNeighbourForOutputSignal(pos, this);
		}
    }
    
    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player)
    {
    	BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity instanceof ShieldRackBE rack) {
        	Direction rotation = state.getValue(FACING);
        	double hitX = target.getLocation().x - pos.getX();
        	double hitZ = target.getLocation().z - pos.getZ();
        	
        	boolean onMiddle = false;
        	boolean onLeftSide = false;
        	boolean onRightSide = false;
        	
        	if(rotation.getAxis() == Axis.X){
        		onMiddle = hitZ > 0.3 && hitZ < 0.6;
        		onLeftSide = rotation == Direction.WEST ? hitZ < 0.3 : hitZ > 0.6;
    			onRightSide = rotation == Direction.WEST ? hitZ > 0.6 : hitZ < 0.3;
        	}
        	if(rotation.getAxis() == Axis.Z){
        		onMiddle = hitX > 0.3 && hitX < 0.6;
        		onLeftSide = rotation == Direction.SOUTH ? hitX < 0.3 : hitX > 0.6;
    			onRightSide = rotation == Direction.SOUTH ? hitX > 0.6 : hitX < 0.3;
        	}
        	
        	if(onMiddle && !rack.getShieldStack().isEmpty()) {
        		return rack.getShieldStack().copy();
        	}
        	if(onLeftSide && !rack.getLeftStack().isEmpty()) {
        		return rack.getLeftStack().copy();
        	}
        	if(onRightSide && !rack.getRightStack().isEmpty()) {
        		return rack.getRightStack().copy();
        	}
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
    	BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ShieldRackBE) {
        	ShieldRackBE rack = (ShieldRackBE)be;
        	
        	ItemStack heldItem = player.getItemInHand(hand);
        	Direction rotation = state.getValue(FACING);
        	double hitX = trace.getLocation().x - pos.getX();
        	double hitZ = trace.getLocation().z - pos.getZ();
        	
        	boolean performedAction = false;
        	
        	boolean onMiddle = false;
        	boolean onLeftSide = false;
        	boolean onRightSide = false;
        	
        	if(rotation.getAxis() == Axis.X){
        		onMiddle = hitZ > 0.3 && hitZ < 0.6;
        		onLeftSide = rotation == Direction.WEST ? hitZ < 0.3 : hitZ > 0.6;
    			onRightSide = rotation == Direction.WEST ? hitZ > 0.6 : hitZ < 0.3;
        	}
        	if(rotation.getAxis() == Axis.Z){
        		onMiddle = hitX > 0.3 && hitX < 0.6;
        		onLeftSide = rotation == Direction.SOUTH ? hitX < 0.3 : hitX > 0.6;
    			onRightSide = rotation == Direction.SOUTH ? hitX > 0.6 : hitX < 0.3;
        	}
        	
        	if(onMiddle && ((heldItem.isEmpty() && !rack.getShieldStack().isEmpty()) || ItemUtils.isShield(heldItem))){
				//Shield
				final ItemStack handStack = heldItem.copy();
				final ItemStack shieldStack = rack.getShieldStack();
				rack.setShieldStack(handStack);
				player.setItemInHand(hand, shieldStack);
				performedAction = true;
			}
			if(onLeftSide && ((heldItem.isEmpty() && !rack.getLeftStack().isEmpty()) || ItemUtils.isMeleeWeapon(heldItem))){
				//Left
				final ItemStack handStack = heldItem.copy();
				final ItemStack shieldStack = rack.getLeftStack();
				rack.setLeftStack(handStack);
				player.setItemInHand(hand, shieldStack);
				performedAction = true;
			}
			else if(onRightSide && ((heldItem.isEmpty() && !rack.getRightStack().isEmpty()) || ItemUtils.isMeleeWeapon(heldItem))){
				//Right
				final ItemStack handStack = heldItem.copy();
				final ItemStack shieldStack = rack.getRightStack();
				rack.setRightStack(handStack);
				player.setItemInHand(hand, shieldStack);
				performedAction = true;
			}
        	
        	if(performedAction) {
        		rack.setChanged();
				rack.markBlockForUpdate(null);
				return InteractionResult.sidedSuccess(level.isClientSide);
        	}
        }
        return InteractionResult.PASS;
    }
}
