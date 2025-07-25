package alec_wam.wam_utils.common.entities.workers.jobs;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.jobs.impl.BeeHiveJob;
import alec_wam.wam_utils.common.entities.workers.jobs.impl.BreedAnimalsJob;
import alec_wam.wam_utils.common.entities.workers.jobs.impl.ButcherAnimalsJob;
import alec_wam.wam_utils.common.entities.workers.jobs.impl.CollectItemsJob;
import alec_wam.wam_utils.common.entities.workers.jobs.impl.ConcreteJob;
import alec_wam.wam_utils.common.entities.workers.jobs.impl.HarvestCropJob;
import alec_wam.wam_utils.common.entities.workers.jobs.impl.ShearAnimalsJob;
import alec_wam.wam_utils.common.entities.workers.jobs.impl.TreeChopJob;
import alec_wam.wam_utils.common.helpers.BlockHelper;
import alec_wam.wam_utils.common.items.WorkerStaffItem.SelectionType;
import alec_wam.wam_utils.common.items.WorkerStaffItem.WorkerBlockSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class JobManager {

	@FunctionalInterface
	public interface WorkerJobFactory {
		WorkerJob create(WorkerEntity worker, ItemStack stack, Player player);
	}

	/**
	 * 
	 * Ideas
	 * - Fishing
	 * - Breaking
	 *
	 */
	public static enum JobType implements StringRepresentable {		
//		BONEMEAL(BonemealJob.class),
//		DEFEND_MELEE(MeleeAttackJob.class), 
//		DEFEND_RANGED(RangeAttackJob.class);
		CROP(HarvestCropJob.class, HarvestCropJob::createJob),
		TREE(TreeChopJob.class, TreeChopJob::createJob),
		SHEAR(ShearAnimalsJob.class, ShearAnimalsJob::createJob),
		BREED(BreedAnimalsJob.class, BreedAnimalsJob::createJob),
		BUTCHER(ButcherAnimalsJob.class, ButcherAnimalsJob::createJob),
		BEEHIVE(BeeHiveJob.class, BeeHiveJob::createJob),
		COLLECT(CollectItemsJob.class, CollectItemsJob::createJob),
		CONCRETE(ConcreteJob.class, ConcreteJob::createJob);

		
		public static final StringRepresentable.EnumCodec<JobType> CODEC = StringRepresentable.fromEnum(JobType::values);
        public static final StreamCodec<ByteBuf, JobType> STREAM_CODEC = ByteBufCodecs.stringUtf8(24)
        																	.map(JobType::valueOf, JobType::name);
		
		private final Class<? extends WorkerJob> jobClass;
		private final WorkerJobFactory jobFactory;
		JobType(Class<? extends WorkerJob> clazz, WorkerJobFactory jobFactory){
			this.jobClass = clazz;
			this.jobFactory = jobFactory;
		}
		
		public Class<? extends WorkerJob> getJobClass(){
			return this.jobClass;
		}
		
		public WorkerJobFactory getJobFactory() {
			return this.jobFactory;
		}

		public JobType getNextType() {
			return JobType.values()[(this.ordinal() + 1) % JobType.values().length];
		}
		
		public void saveToTag(String key, CompoundTag tag) {
			tag.putString(key, name());
		}
		
		public static JobType loadFromTag(String key, CompoundTag tag) {
			return valueOf(tag.getStringOr(key, ""));
		}

		@Override
		public String getSerializedName() {
			return name();
		}
		
		public Component getDisplayName() {
			return Component.literal(name());
		}
	}

	public static <T extends WorkerJob> T createJobFromSelection(
            WorkerEntity worker,
            ItemStack stack,
            Player player,
            Function<List<BlockPos>, T> listConstructor,
            BiFunction<BlockPos, BlockPos, T> areaConstructor
    ) {
        SelectionType selectionType = stack.get(ModInit.WORKER_SELECTION_TYPE_COMPONENT);
        WorkerBlockSettings blockSettings = stack.get(ModInit.WORKER_BLOCK_SETTINGS_DATA_COMPONENT);

        if (selectionType != null && blockSettings != null) {
            if (selectionType == SelectionType.SINGLE) {
                BlockPos pos = blockSettings.getSingleBlockPos().orElse(null);
                if (pos != null) {
                    return listConstructor.apply(Collections.singletonList(pos));
                }
            } else if (selectionType == SelectionType.AREA) {
                BlockPos areaBlockPos1 = blockSettings.getAreaBlockPos1().orElse(null);
                BlockPos areaBlockPos2 = blockSettings.getAreaBlockPos2().orElse(null);
                if (areaBlockPos1 != null && areaBlockPos2 != null) {
                    return areaConstructor.apply(areaBlockPos1, areaBlockPos2);
                }
            } else if (selectionType == SelectionType.LIST) {
				List<BlockPos> posList = blockSettings.getBlockPosList().orElse(null);
				if (posList !=null && !posList.isEmpty()) {
					return listConstructor.apply(posList);
				}
			}            
        }
        return null;
    }
	
	public static CompoundTag saveToTag(WorkerJob job) {
		CompoundTag tag = new CompoundTag();
		job.getJobType().saveToTag("JobType", tag);
		CompoundTag jobData = new CompoundTag();
		job.saveToTag(jobData);
		tag.put("JobData", jobData);
		return tag;
	}
	
	public static WorkerJob loadFromTag(WorkerEntity worker, CompoundTag tag) {
		JobType type = JobType.loadFromTag("JobType", tag);
		if(type == null) {
			System.err.println("Unable to load JobType from tag, returning null job");
			return null;
		}
		CompoundTag jobData = tag.getCompoundOrEmpty("JobData");
		WorkerJob job = null;
		if(isSubclassOf(type.getJobClass(), AreaWorkerJob.class)) {
			Optional<ResourceKey<Level>> dimension = jobData.read(AreaWorkerJob.NBT_DIMENSION, ResourceKey.codec(Registries.DIMENSION));
			if(dimension.isPresent()) {
				List<BlockPos> posList = BlockHelper.loadBlockPosList(jobData, AreaWorkerJob.NBT_BLOCK_LIST);
				boolean isConnected = jobData.getBooleanOr(AreaWorkerJob.NBT_CONNECTED, false);
				
				if(isConnected) {
					try {
						Constructor<? extends WorkerJob> con = type.getJobClass().getConstructor(WorkerEntity.class, ResourceKey.class, BlockPos.class, BlockPos.class);
						if (con != null) {
							Pair<BlockPos, BlockPos> minMax = BlockHelper.getMinAndMaxPos(posList);
							if(minMax !=null) {
								job = con.newInstance(worker, dimension.get(), minMax.getFirst(), minMax.getSecond());
							}
						} else {
							System.err.println("Unable to find AreaWorkerJob BlockList Constructor");
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				else {
					try {
						Constructor<? extends WorkerJob> con = type.getJobClass().getConstructor(WorkerEntity.class, ResourceKey.class, List.class);
						if (con != null) {
							job = con.newInstance(worker, dimension.get(), posList);
						} else {
							System.err.println("Unable to find AreaWorkerJob BlockList Constructor");
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		else {
			try {
				Constructor<? extends WorkerJob> con = type.getJobClass().getConstructor(WorkerEntity.class);
				if (con != null) {
					job = con.newInstance(worker);
				} else {
					System.err.println("Unable to find Basic WorkerJob Constructor");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if(job !=null) {
			job.loadAdditionalData(jobData);
		}
		return job;
	}
	
	protected static boolean isSubclassOf(Class<?> clazz, Class<?> superClass) {
	    if (superClass.equals(Object.class)) {
	        // Every class is an Object.
	        return true;
	    }
	    if (clazz.equals(superClass)) {
	        return true;
	    } else {
	        clazz = clazz.getSuperclass();
	        // every class is Object, but superClass is below Object
	        if (clazz.equals(Object.class)) {
	            // we've reached the top of the hierarchy, but superClass couldn't be found.
	            return false;
	        }
	        // try the next level up the hierarchy.
	        return isSubclassOf(clazz, superClass);
	    }
	}
	
}
