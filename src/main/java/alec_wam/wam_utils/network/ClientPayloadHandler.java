package alec_wam.wam_utils.network;

import java.util.Optional;

import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager;
import alec_wam.wam_utils.common.entities.workers.jobs.WorkerJob;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
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
	        			WorkerJob job = JobManager.loadFromTag(worker, jobData.get());
	        			worker.setJob(job);
	        		}
	            	else {
	            		worker.setJob(null);
	            	}
	            }
	    	}			
        });
    }
	
}
