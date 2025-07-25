package alec_wam.wam_utils.network;

import java.util.Optional;

import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager;
import alec_wam.wam_utils.common.entities.workers.jobs.WorkerJob;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {

	public static void handleSyncWorkerJobOnMain(final SyncWorkerJobPayload data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			Level level = Minecraft.getInstance().level;
	    	if (level != null) {
	    		Entity entity = level.getEntity(data.entityId());
	            if (entity !=null && entity instanceof WorkerEntity worker) {
	            	Optional<CompoundTag> jobData = data.jobData();
	            	if(jobData.isPresent()) {
						try (ProblemReporter.ScopedCollector problemreporter = new ProblemReporter.ScopedCollector(ModPayloadInit.problemPath(data), ModPayloadInit.LOGGER)) {
							ValueInput valueInput = TagValueInput.create(problemreporter, level.registryAccess(), jobData.get());
							WorkerJob job = JobManager.load(worker, valueInput);
							worker.setJob(job);
						}
						catch(Exception e){
							ModPayloadInit.LOGGER.error("Failed to sync Worker Job", e);
							worker.setJob(null);
						}
	        		}
	            	else {
	            		worker.setJob(null);
	            	}
	            }
	    	}			
        });
    }
	
}
