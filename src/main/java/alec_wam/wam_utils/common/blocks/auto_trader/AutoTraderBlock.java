package alec_wam.wam_utils.common.blocks.auto_trader;

import alec_wam.wam_utils.common.blocks.BaseEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class AutoTraderBlock extends BaseEntityBlock{

    public AutoTraderBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutoTraderBE(pos, state);
    }

    @Override
    public InteractionResult playerInteract(Level level, Player player, BlockPos blockPos, BlockHitResult hitResult,
            ItemStack stack, InteractionHand hand) {
        if(!level.isClientSide){
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if(blockEntity !=null && blockEntity instanceof AutoTraderBE autoTrader){
                //Open Inventory
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.openMenu(autoTrader);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isValidBE(BlockEntity blockEntity) {
        return blockEntity instanceof AutoTraderBE;
    }
    
}
