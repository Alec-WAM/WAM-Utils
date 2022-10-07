package alec_wam.wam_utils.blocks.advanced_spawner;

import javax.annotation.Nullable;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntityBlock;
import alec_wam.wam_utils.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class AdvancedSpawnerBlock extends WAMUtilsBlockEntityBlock {
    public static final String SCREEN_SPAWNER_BLOCK = "screen.wam_utils.advanced_spawner";
    
    public AdvancedSpawnerBlock() {
    	super(Properties.of(Material.METAL)
                .sound(SoundType.METAL)
                .strength(2.0f)
                .requiresCorrectToolForDrops()
                .isViewBlocking(BlockInit::neverBlock)
                .noOcclusion()
            );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedSpawnerBE(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return (lvl, pos, stt, te) -> {
                if (te instanceof AdvancedSpawnerBE spawer) spawer.tickServer();
            };
        }
        else {
        	return (lvl, pos, stt, te) -> {
                if (te instanceof AdvancedSpawnerBE spawer) spawer.tickClient();
            };
        }
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AdvancedSpawnerBE) {
                MenuProvider containerProvider = new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable(SCREEN_SPAWNER_BLOCK);
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player playerEntity) {
                        return new AdvancedSpawnerContainer(windowId, pos, playerInventory, playerEntity);
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
