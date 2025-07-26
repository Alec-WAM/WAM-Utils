package alec_wam.wam_utils.common.blocks;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;

public abstract class BaseEntityBlock extends Block implements EntityBlock {

	public BaseEntityBlock(Properties properties) {
		super(properties);
	}

    public static boolean never(BlockState state, BlockGetter getter, BlockPos pos) {
        return false;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(world, pos, state, entity, stack);
//        TODO Load Data From Item
//        if (!world.isClientSide && entity instanceof Player player) {
//            BlockEntity blockEntity = world.getBlockEntity(pos);
//            if (blockEntity instanceof BaseBE baseBE) {
//                if (stack.has(JustDireDataComponents.CUSTOM_DATA_1)) {
//                    CompoundTag compound = stack.get(JustDireDataComponents.CUSTOM_DATA_1).copyTag();
//                    if (!compound.isEmpty())
//                        blockEntity.loadCustomOnly(compound, world.registryAccess());
//                }
//            	baseBE.setPlacedBy(player.getUUID());
//            }
//        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState blockState, Level level, BlockPos blockPos, Player player, BlockHitResult hit) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        BlockEntity te = level.getBlockEntity(blockPos);
        if (!isValidBE(te))
            return InteractionResult.FAIL;

        return playerInteract(level, player, blockPos, hit, ItemStack.EMPTY, null);
    }

    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        BlockEntity te = level.getBlockEntity(blockPos);
        if (!isValidBE(te))
            return InteractionResult.FAIL;

        return playerInteract(level, player, blockPos, hitResult, stack, hand);
    }

    public abstract InteractionResult playerInteract(Level level, Player player, BlockPos blockPos, BlockHitResult hitResult, @Nullable ItemStack stack, @Nullable InteractionHand hand);

    public abstract boolean isValidBE(BlockEntity blockEntity);

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return (lvl, pos, blockState, t) -> {
                if (t instanceof BaseBE tile) {
                    tile.tickClient();
                }
            };
        }
        return (lvl, pos, blockState, t) -> {
            if (t instanceof BaseBE tile) {
                tile.tickServer();
            }
        };
    }

	@Override
	public void spawnAfterBreak(BlockState state, ServerLevel worldIn, BlockPos pos, ItemStack p_220062_4_, boolean b) {
		super.spawnAfterBreak(state, worldIn, pos, p_220062_4_, b);
	}
    
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder); // Get default drops
        BlockEntity blockEntity = builder.getParameter(LootContextParams.BLOCK_ENTITY);

        if (blockEntity instanceof BaseBE baseBE) {
        	IItemHandler iItemHandler = baseBE.getItemHandler(null);
        	if(iItemHandler !=null) {
	            for (int i = 0; i < iItemHandler.getSlots(); ++i) {
	            	drops.add(iItemHandler.getStackInSlot(i));
	            }
        	}
        }

        return drops;
    }
    
	@Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

}
