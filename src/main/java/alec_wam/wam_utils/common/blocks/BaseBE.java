package alec_wam.wam_utils.common.blocks;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.ItemStackHandler;

public abstract class BaseBE extends BlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

	public BaseBE(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
		super(type, pos, blockState);
	}
	
    @Override
    public void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        saveInventory(valueOutput);
    }
    
    public void saveInventory(ValueOutput valueOutput) {        
		ValueOutput inventoryChild = valueOutput.child("inventory");
		ItemStackHandler inventory = this.getItemHandler(null);
        if(inventory !=null){
            inventory.serialize(inventoryChild);
        }
    }

    @Override
    public void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        loadInventory(valueInput);
    }

	public void loadInventory(ValueInput valueInput) {
        ItemStackHandler inventory = this.getItemHandler(null);
        if(inventory !=null){
            inventory.deserialize(valueInput.childOrEmpty("inventory"));
        }
    }

    public void tickClient() {}
	
	public void tickServer() {}
	
	@Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        // Vanilla uses the type parameter to indicate which type of tile entity (command block, skull, or beacon?) is receiving the packet, but it seems like Forge has overridden this behavior
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        this.loadAdditional(input);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(
            this.problemPath(), LOGGER
        )) {
            TagValueOutput output = TagValueOutput.createWithContext(problemreporter$scopedcollector, provider);
            saveAdditional(output);
            return output.buildResult();
        } catch (Exception exception) {
            LOGGER.error("Failed to load block entity from falling block", (Throwable)exception);
        }
        return new CompoundTag();
    }

    public void markDirtyClient() {
        setChanged();
        if (level != null) {
            BlockState state = level.getBlockState(getBlockPos());
            level.sendBlockUpdated(getBlockPos(), state, state, 3);
        }
    }

	public ItemStackHandler getItemHandler(@Nullable Direction side) {
		return null;
	}
}
