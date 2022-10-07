package alec_wam.wam_utils.blocks;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

public abstract class WAMUtilsBlockEntityBlock extends Block implements EntityBlock {

	public WAMUtilsBlockEntityBlock(Properties props) {
		super(props);
	}
    
    @SuppressWarnings("deprecation")
	@Override
	public void onRemove(BlockState p_49076_, Level p_49077_, BlockPos p_49078_, BlockState p_49079_, boolean p_49080_) {
    	if (!p_49076_.is(p_49079_.getBlock())) {
    		dropInventoryItems(p_49077_, p_49078_);
    		super.onRemove(p_49076_, p_49077_, p_49078_, p_49079_, p_49080_);
    	}
    }
    
    public void dropInventoryItems(Level level, BlockPos pos) {
    	BlockEntity blockentity = level.getBlockEntity(pos);
		if (blockentity !=null) {
			LazyOptional<IItemHandler> handler = blockentity.getCapability(ForgeCapabilities.ITEM_HANDLER).cast();

			handler.ifPresent(h -> {
				for(int i = 0; i < h.getSlots(); i++) {
					ItemStack stack = h.getStackInSlot(i);
					Containers.dropItemStack(level, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), stack);
				}
			});
			level.updateNeighbourForOutputSignal(pos, this);
		}
    }
	
	@Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player)
    {
    	BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity !=null && blockentity instanceof INBTItemDrop nbtTile) {
        	return nbtTile.getNBTDrop(asItem());
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }
    
    @SuppressWarnings("deprecation")
	@Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
    	BlockEntity blockentity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
    	if(blockentity !=null) {
    		if (blockentity instanceof INBTItemDrop nbtTile) {
            	return Lists.newArrayList(nbtTile.getNBTDrop(asItem()));
            }
    	}
    	return super.getDrops(state, builder);
    }
    
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
    	super.setPlacedBy(level, pos, state, entity, stack);
    	if (!level.isClientSide) {
    		BlockEntity be = level.getBlockEntity(pos);
    		if (be instanceof INBTItemDrop) {
    			INBTItemDrop nbtTile = (INBTItemDrop)be;
    			if(!stack.isEmpty()) {
    				nbtTile.readFromItem(stack);
    			}
    		}
    	}
    }

}
