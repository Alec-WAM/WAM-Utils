package alec_wam.wam_utils.common.entities.workers.jobs;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.jobs.impl.BeeHiveJob;
import alec_wam.wam_utils.common.entities.workers.jobs.impl.BreedAnimalsJob;
import alec_wam.wam_utils.common.entities.workers.jobs.impl.ButcherAnimalsJob;
import alec_wam.wam_utils.common.entities.workers.jobs.impl.CollectItemsJob;
import alec_wam.wam_utils.common.entities.workers.jobs.impl.ConcreteJob;
import alec_wam.wam_utils.common.entities.workers.jobs.impl.FishermanJob;
import alec_wam.wam_utils.common.entities.workers.jobs.impl.FlowerHarvestJob;
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
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class JobManager {

	private static final Logger LOGGER = LogUtils.getLogger();

	@FunctionalInterface
	public interface WorkerJobFactory {
		WorkerJob create(WorkerEntity worker, ItemStack stack, Player player);
	}

	/**
	 * 
	 * Ideas
	 * - Automatic Smelting
	 * - XP Collector
	 *
	 */
	public static enum JobType implements StringRepresentable {		
//		BONEMEAL(BonemealJob.class),
//		DEFEND_MELEE(MeleeAttackJob.class), 
//		DEFEND_RANGED(RangeAttackJob.class);
		CROP(HarvestCropJob.class, HarvestCropJob::createJob),
		TREE(TreeChopJob.class, TreeChopJob::createJob),
		FLOWERS(FlowerHarvestJob.class, FlowerHarvestJob::createJob),
		SHEAR(ShearAnimalsJob.class, ShearAnimalsJob::createJob),
		BREED(BreedAnimalsJob.class, BreedAnimalsJob::createJob),
		BUTCHER(ButcherAnimalsJob.class, ButcherAnimalsJob::createJob),
		BEEHIVE(BeeHiveJob.class, BeeHiveJob::createJob),
		FISHING(FishermanJob.class, FishermanJob::createJob),
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
		if(job == null || job.worker == null || job.worker.level() == null){
			return new CompoundTag();
		}
		try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(
            JobManager.problemPath(job), LOGGER
        )) {
            TagValueOutput output = TagValueOutput.createWithContext(problemreporter$scopedcollector, job.worker.level().registryAccess());
            saveToOutput(output, job);
			return output.buildResult();
        } catch (Exception exception) {
            LOGGER.error("Failed to save Worker Job to CompoundTag", (Throwable)exception);
        }
        return new CompoundTag();
	}

	

	public static void saveToOutput(ValueOutput valueOutput, WorkerJob job){
		valueOutput.store("JobType", JobType.CODEC, job.getJobType());
		ValueOutput jobChild = valueOutput.child("JobData");
		job.save(jobChild);
	}
	
	public static WorkerJob load(WorkerEntity worker, ValueInput valueInput) {
		JobType type = valueInput.read("JobType", JobType.CODEC).orElse(null);
		if(type == null) {
			System.err.println("Unable to load JobType from tag, returning null job");
			return null;
		}
		ValueInput jobData = valueInput.childOrEmpty("JobData");
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
			job.load(jobData);
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

	public static ProblemReporter.PathElement problemPath(WorkerJob job) {
        return new WorkerJobElement(job);
    }

    record WorkerJobElement(WorkerJob job) implements ProblemReporter.PathElement {
        @Override
        public String get() {
            return "WorkerJob@" + job.getJobType().getSerializedName();
        }
    }
	
}
