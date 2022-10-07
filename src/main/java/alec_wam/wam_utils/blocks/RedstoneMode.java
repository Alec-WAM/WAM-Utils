package alec_wam.wam_utils.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public enum RedstoneMode {

	ON, OFF, IGNORE, DISABLED;
	
	public boolean isMet(Level level, BlockPos pos) {
		if(this == ON) {
			return level.getBestNeighborSignal(pos) > 0;
		}
		else if(this == OFF) {
			return level.getBestNeighborSignal(pos) <= 0;
		}
		else if(this == IGNORE) {
			return true;
		}
		return false;
	}
	
	public RedstoneMode getNext() {
		return RedstoneMode.values()[(this.ordinal() + 1) % (RedstoneMode.values().length)];
	}
	
	public static RedstoneMode getMode(int index) {
		return RedstoneMode.values()[index % (RedstoneMode.values().length)];
	}
	
}
