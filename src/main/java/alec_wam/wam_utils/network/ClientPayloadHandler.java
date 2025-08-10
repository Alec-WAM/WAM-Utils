package alec_wam.wam_utils.network;

import java.util.Optional;

import alec_wam.wam_utils.common.blocks.BaseBE;
import alec_wam.wam_utils.common.blocks.enchantment.indexer.menu.EnchantmentIndexerMenu;
import alec_wam.wam_utils.common.blocks.enchantment.indexer.menu.EnchantmentIndexerScreen;
import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.jobs.JobManager;
import alec_wam.wam_utils.common.entities.workers.jobs.WorkerJob;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
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

	public static void handleSyncWorkerFishingOnMain(final SyncWorkerFishingPayload data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			Level level = Minecraft.getInstance().level;
	    	if (level != null) {
	    		Entity entity = level.getEntity(data.entityId());
	            if (entity !=null && entity instanceof WorkerEntity worker) {
	            	if(data.isFishing() && data.fishingTargetLocation().isPresent()){
						worker.startFishing(new Vec3(data.fishingTargetLocation().get()));
					}
					else {
						worker.stopFishing();
					}
	            }
	    	}			
        });
    }

	public static void handleSyncWorkerFoodDataOnMain(final SyncWorkerFoodDataPayload data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			Level level = Minecraft.getInstance().level;
	    	if (level != null) {
	    		Entity entity = level.getEntity(data.entityId());
	            if (entity !=null && entity instanceof WorkerEntity worker) {
					worker.getFoodData().setFoodLevel(data.foodLevel());
					worker.getFoodData().setSaturation(data.saturationLevel());
	            }
	    	}			
        });
    }
	
	public static void handleBaseBEMessageOnMain(final BaseBEMessagePayload data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			Level level = Minecraft.getInstance().level;
	    	if (level != null) {
				BlockEntity	blockEntity = level.getBlockEntity(data.pos());
				if(blockEntity != null && blockEntity instanceof BaseBE baseBE) {
					baseBE.handleCustomMessage(data.messageType(), data.messageData(), true);
				}
			}
		});
	}

	public static void handleClientShelfItemMessageOnMain(final SyncClientShelfItemsPayload data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if(Minecraft.getInstance().screen != null){
				if (Minecraft.getInstance().screen instanceof EnchantmentIndexerScreen screen) {
					EnchantmentIndexerMenu menu = screen.getMenu();
					menu.setItemList(data.shelfItems());
				}
			}
		});
	}
}
