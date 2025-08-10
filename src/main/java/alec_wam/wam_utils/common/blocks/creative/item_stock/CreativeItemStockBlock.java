package alec_wam.wam_utils.common.blocks.creative.item_stock;

import alec_wam.wam_utils.common.blocks.BaseEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class CreativeItemStockBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

	public CreativeItemStockBlock(Properties properties) {
		super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
	}

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CreativeItemStockBE(pos, state);
	}

	@Override
	public InteractionResult playerInteract(Level level, Player player, BlockPos blockPos, BlockHitResult hitResult,
			ItemStack stack, InteractionHand hand) {
		BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if(!player.isCrouching()) {
            if(blockEntity !=null && blockEntity instanceof CreativeItemStockBE creativeItemStock) {                
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.openMenu(creativeItemStock);
                }
            }
        }
        return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isValidBE(BlockEntity blockEntity) {
        return blockEntity instanceof CreativeItemStockBE;
	}
    
}
