package alec_wam.wam_utils.blocks.item_grate;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ItemGrateBlock extends Block {
	protected static final VoxelShape SHAPE_UP = Block.box(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D);
	protected static final VoxelShape SHAPE_DOWN = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);
	protected static final VoxelShape SHAPE_NORTH = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);
	protected static final VoxelShape SHAPE_SOUTH = Block.box(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D);
	protected static final VoxelShape SHAPE_EAST = Block.box(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
	protected static final VoxelShape SHAPE_WEST = Block.box(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D);

	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final IntegerProperty POWER = BlockStateProperties.POWER;

	public ItemGrateBlock() {
		super(Properties.of(Material.METAL).sound(SoundType.METAL).strength(1.0f).noOcclusion());
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWER, Integer.valueOf(0)));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext p_48689_) {
		return this.defaultBlockState().setValue(FACING, p_48689_.getNearestLookingDirection().getOpposite());
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
		p_48725_.add(FACING, POWER);
	}

	@SuppressWarnings("deprecation")
	@Override
	public VoxelShape getCollisionShape(BlockState p_154285_, BlockGetter p_154286_, BlockPos p_154287_,
			CollisionContext p_154288_) {
		if (p_154288_ instanceof EntityCollisionContext entitycollisioncontext) {
			Entity entity = entitycollisioncontext.getEntity();
			if (entity != null) {
				if (canPassThroughGrate(entity)) {
					return Shapes.empty();
				}
			}
		}

		return super.getCollisionShape(p_154285_, p_154286_, p_154287_, p_154288_);
	}

	public boolean canPassThroughGrate(Entity entity) {
		return entity instanceof ItemEntity || entity instanceof ExperienceOrb;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction dir = state.getValue(FACING);
		switch (dir) {
		case NORTH:
		default:
			return SHAPE_NORTH;
		case SOUTH:
			return SHAPE_SOUTH;
		case EAST:
			return SHAPE_EAST;
		case WEST:
			return SHAPE_WEST;
		case UP:
			return SHAPE_UP;
		case DOWN:
			return SHAPE_DOWN;
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean p_49323_) {
		if (!p_49323_ && !newState.is(this)) {
			if (state.getValue(POWER) > 0) {
				this.updateNeighbours(level, pos);
			}

			super.onRemove(state, level, pos, newState, p_49323_);
		}
	}

	protected void updateNeighbours(Level level, BlockPos pos) {
		for(Direction dir : Direction.values()) {
			level.updateNeighborsAt(pos.relative(dir), this);
		}
	}
	
	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (!level.isClientSide) {
			if (state.getValue(POWER) <= 0) {
				this.checkEntityPassThrough(level, pos);
			}
		}
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockState(pos).getValue(POWER) > 0) {
			this.checkEntityPassThrough(level, pos);
		}
	}
	
	private void checkEntityPassThrough(Level level, BlockPos pos) {
		BlockState blockstate = level.getBlockState(pos);
		int currentPower = blockstate.getValue(POWER);
		List<? extends Entity> list = level.getEntities((Entity)null, blockstate.getShape(level, pos).bounds().move(pos), (entity) -> {
			return this.canPassThroughGrate(entity);
		});
		
		boolean itemInside = false;
		boolean xpInside = false;
		if (!list.isEmpty()) {
			for(Entity entity : list) {
				if (!entity.isIgnoringBlockTriggers()) {
					if(entity instanceof ItemEntity) {
						itemInside = true;
					}
					if(entity instanceof ExperienceOrb) {
						xpInside = true;
					}
					
					if(itemInside && xpInside) {
						break;
					}
				}
			}
		}
		
		int newPower = 0;
		if(itemInside) {
			newPower += 1;
		}
		if(xpInside) {
			newPower += 2;
		}

		if (newPower != currentPower) {
			blockstate = blockstate.setValue(POWER, Integer.valueOf(newPower));
			level.setBlock(pos, blockstate, 3);
			this.updateNeighbours(level, pos);
		}

		if (newPower > 0) {
			level.scheduleTick(new BlockPos(pos), this, 10);
		}
	}
	
	@Override
	public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
		return state.getValue(POWER);
	}

	@Override
	public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
		return state.getValue(POWER);
	}

	@Override
	public boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

}
