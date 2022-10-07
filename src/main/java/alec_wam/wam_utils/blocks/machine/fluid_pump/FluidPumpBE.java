package alec_wam.wam_utils.blocks.machine.fluid_pump;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import alec_wam.wam_utils.blocks.WAMUtilsBlockEntity;
import alec_wam.wam_utils.utils.BlockUtils;
import net.minecraft.core.BlockPos;

//public class FluidPumpBE extends WAMUtilsBlockEntity {
//
//	private Queue<BlockPos> pumpQueue;
//	
//	private void buildPumpQueue() {
//		BlockPos root = this.worldPosition;
//		BlockPos min = root.north(2).east(2);
//		BlockPos max = root.south(2).west(2).below(2);
//		
//		Map<BlockPos, Fluid> fluidPosList = new ArrayList<BlockPos>();
//		Iterable<BlockPos> scanList = BlockPos.betweenClosed(min, max);
//		Iterator<BlockPos> scanIter = scanList.iterator();
//		while(scanIter.hasNext()) {
//			BlockPos pos = scanIter.next();
//			if(!level.isLoaded(pos)) {
//				continue;
//			}
//			if(BlockUtils.isFluidSource(level, pos)) {
//				
//			}
//		}
//	}
//	
//}
