package alec_wam.wam_utils.common.blocks.enchantment.indexer;

import alec_wam.wam_utils.common.blocks.BaseEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class EnchantmentIndexerBlock extends BaseEntityBlock {

    public EnchantmentIndexerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnchantmentIndexerBE(pos, state);
    }

    @Override
    public InteractionResult playerInteract(Level level, Player player, BlockPos blockPos, BlockHitResult hitResult,
            ItemStack stack, InteractionHand hand) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if(blockEntity !=null && blockEntity instanceof EnchantmentIndexerBE indexer) {
            boolean isClient = level.isClientSide;
            if(!isClient && player.isCrouching()) {
                indexer.buildShelfList();
            }
            return isClient ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isValidBE(BlockEntity blockEntity) {
        return blockEntity instanceof EnchantmentIndexerBE;
    }
    
}
