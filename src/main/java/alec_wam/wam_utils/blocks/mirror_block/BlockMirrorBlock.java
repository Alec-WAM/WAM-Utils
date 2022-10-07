package alec_wam.wam_utils.blocks.mirror_block;

import javax.annotation.Nullable;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;

public class BlockMirrorBlock extends WAMUtilsBlockEntityBlock {
     
	public BlockMirrorBlock() {
    	super(Properties.of(Material.METAL)
                .sound(SoundType.METAL)
                .strength(2.0f)
                .requiresCorrectToolForDrops()
            );
	}
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockMirrorBE(pos, state);
    }  
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BlockMirrorBE) {
        	BlockMirrorBE mirror = (BlockMirrorBE)be;
        	if(mirror.getLinkedPos() !=null) {
        		BlockPos otherPos = mirror.getLinkedPos();
        		if(level.isLoaded(otherPos)) {
        			BlockState otherState = level.getBlockState(otherPos);
        			BlockHitResult otherResult = new BlockHitResult(trace.getLocation(), trace.getDirection(), otherPos, trace.isInside());
        			return otherState.use(level, player, hand, otherResult);
        		}
        	}
        } 
        return InteractionResult.PASS;
    }
}
