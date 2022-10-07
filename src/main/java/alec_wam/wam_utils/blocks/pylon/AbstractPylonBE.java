package alec_wam.wam_utils.blocks.pylon;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractPylonBE extends WAMUtilsBlockEntity {

	public AbstractPylonBE(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
		super(p_155228_, p_155229_, p_155230_);
	}

	public abstract void tickServer();

	public abstract void tickClient();

	public abstract MobCategory getMobCategory();

}
