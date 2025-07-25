package alec_wam.wam_utils.common.entities.workers.jobs;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.helpers.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public abstract class AreaWorkerJob extends WorkerJob {

	public static final String NBT_DIMENSION = "Dimension";
	public static final String NBT_BLOCK_LIST = "BlockPosList";
	public static final String NBT_CONNECTED = "Connected";
	
	protected final ResourceKey<Level> dimension;
	protected final List<BlockPos> blockPosList;	
	protected final List<AABB> boundingBoxes;
	protected final boolean connected;
	
	public AreaWorkerJob(WorkerEntity worker, ResourceKey<Level> dimension, List<BlockPos> blockPosList) {
		super(worker);
		this.dimension = dimension;
		this.blockPosList = blockPosList;
		this.boundingBoxes = blockPosList.stream().map((pos) -> {
			return new AABB(pos);
		}).toList();
		this.connected = false;
	}
	
	public AreaWorkerJob(WorkerEntity worker, ResourceKey<Level> dimension, BlockPos posA, BlockPos posB) {
		super(worker);
		this.dimension = dimension;
		BoundingBox box = BoundingBox.fromCorners(posA, posB);
		this.blockPosList = new LinkedList<BlockPos>();
		//Add immutable instance of BlockPos because they are normally mutable
		BlockPos.betweenClosed(posA, posB).forEach((pos) -> this.blockPosList.add(pos.immutable()));
		this.boundingBoxes = Collections.singletonList(AABB.of(box));
		this.connected = true;
	}
	
	@Override
	public void saveToTag(CompoundTag tag) {
		if(dimension !=null) {
			tag.storeNullable(NBT_DIMENSION, ResourceKey.codec(Registries.DIMENSION), dimension);
		}
		if(!this.blockPosList.isEmpty()) {
			Tag tagList = BlockHelper.saveBlockPosList(blockPosList);
			tag.put(NBT_BLOCK_LIST, tagList);
		}
		tag.putBoolean(NBT_CONNECTED, connected);
	}
	
	public boolean isConnected() {
		return this.connected;
	}
	
	public List<BlockPos> getBlockPosList() {
		return blockPosList;
	}

	@Override
	public List<AABB> getBoundingBoxes(){
		return boundingBoxes;
	}
	
	@Override
	public boolean canKeepRunning() {
		return worker.level().dimension().equals(dimension) && super.canKeepRunning();
	}

	@Override
	public boolean isSame(WorkerJob job) {
		if(!(job instanceof AreaWorkerJob)) return false;
		AreaWorkerJob areaJob = (AreaWorkerJob)job;
		if(!this.dimension.equals(areaJob.dimension)) return false;
		if(this.connected != areaJob.connected) return false;
		if(this.blockPosList.size() != areaJob.blockPosList.size()) return false;
		return this.blockPosList.containsAll(areaJob.blockPosList);
	}

}
