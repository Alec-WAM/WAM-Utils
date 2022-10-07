package alec_wam.wam_utils.blocks.generator.furnace;

import javax.annotation.Nullable;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class FurnaceGeneratorBlock extends WAMUtilsBlockEntityBlock {
    public static final String SCREEN_FURNACE_GENERATOR_BLOCK = "screen.wam_utils.furnace_spawner";
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty RUNNING = BooleanProperty.create("lit");
    
    public FurnaceGeneratorBlock() {
    	super(Properties.of(Material.METAL)
                .sound(SoundType.METAL)
                .strength(2.0f)
                .requiresCorrectToolForDrops()
            );
    	this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RUNNING, Boolean.valueOf(false)));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_48689_) {
    	return this.defaultBlockState().setValue(FACING, p_48689_.getHorizontalDirection().getOpposite());
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
    	p_48725_.add(FACING, RUNNING);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FurnaceGeneratorBE(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return (lvl, pos, stt, te) -> {
                if (te instanceof FurnaceGeneratorBE generator) generator.tickServer();
            };
        }
        else {
        	return (lvl, pos, stt, te) -> {
                if (te instanceof FurnaceGeneratorBE generator) generator.tickClient();
            };
        }
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FurnaceGeneratorBE) {
                MenuProvider containerProvider = new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable(SCREEN_FURNACE_GENERATOR_BLOCK);
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player playerEntity) {
                        return new FurnaceGeneratorContainer(windowId, pos, playerInventory, playerEntity);
                    }
                };
                NetworkHooks.openScreen((ServerPlayer) player, containerProvider, be.getBlockPos());
            } else {
                throw new IllegalStateException("Our named container provider is missing!");
            }
        }
        return InteractionResult.SUCCESS;
    }
}
