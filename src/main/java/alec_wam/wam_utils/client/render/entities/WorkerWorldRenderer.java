package alec_wam.wam_utils.client.render.entities;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.client.util.RenderHelper;
import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import alec_wam.wam_utils.common.entities.workers.WorkerInventorySettings;
import alec_wam.wam_utils.common.entities.workers.WorkerInventorySettings.IOType;
import alec_wam.wam_utils.common.entities.workers.jobs.WorkerJob;
import alec_wam.wam_utils.common.items.WorkerStaffItem;
import alec_wam.wam_utils.common.items.WorkerStaffItem.SelectionType;
import alec_wam.wam_utils.common.items.WorkerStaffItem.WorkerBlockSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

// @EventBusSubscriber(modid = WAMUtils.MODID, value = Dist.CLIENT)
public class WorkerWorldRenderer {
	
	private static final Map<UUID, WorkerEntity> WORKERS_TO_RENDER = new HashMap<UUID, WorkerEntity>();
	
	public static boolean toggleJobVisibility(WorkerEntity worker) {
		UUID uuid = worker.getUUID();
		if(!WORKERS_TO_RENDER.containsKey(uuid)) {
			WORKERS_TO_RENDER.put(uuid, worker);
			return true;
		}
		else {
			WORKERS_TO_RENDER.remove(uuid, worker);			
		}
		return false;
	}
	
	@SubscribeEvent
	public static void renderWorldAfterTripWires(RenderLevelStageEvent.AfterTripwireBlocks event) {
		RenderBuffers buffers = Minecraft.getInstance().renderBuffers();
		MultiBufferSource.BufferSource buffersource = buffers.bufferSource();			
		Vec3 projected = event.getCamera().getPosition();		
		PoseStack pose = event.getPoseStack();
		Player player = Minecraft.getInstance().player;

		for(WorkerEntity worker : WORKERS_TO_RENDER.values()) {
			if(worker.isAlive()) {
				WorkerJob job = worker.getJob();
				if(job !=null) {
					//TODO Create Status Enum for Colors.
					boolean errorMode = !job.canKeepRunning();	
					float r = errorMode ? 255.0F : 0.0F;
					float g = !errorMode ? 255.0F : 0.0F;
					float b = 0.0F;
					Color lineColor = new Color(r / 255.0F, g / 255.0F, b / 255.0F, 1.0F);
					if(job.getBoundingBoxes() !=null && !job.getBoundingBoxes().isEmpty()) {
						job.getBoundingBoxes().forEach((bb) -> {
							AABB aabb = bb;
							pose.pushPose();
							pose.translate(-projected.x(), -projected.y(), -projected.z());
							RenderHelper.renderLines(pose, aabb, lineColor, buffersource);
							pose.popPose();
						});
						
					}
				}
			}
		}
		
		ItemStack mainHandStack = player.getMainHandItem();
		ItemStack offHandStack = player.getOffhandItem();
		if(mainHandStack.is(ModInit.WORKER_STAFF_ITEM)) {
			renderWorkerStaffSelection(mainHandStack, buffers, pose, buffersource, projected);			
		}
		else if(offHandStack.is(ModInit.WORKER_STAFF_ITEM)) {
			renderWorkerStaffSelection(offHandStack, buffers, pose, buffersource, projected);
		}
	}

	public static void renderWorkerStaffSelection(ItemStack stack, RenderBuffers buffers, PoseStack pose, MultiBufferSource.BufferSource buffersource, Vec3 projected){
		SelectionType selection = stack.get(ModInit.WORKER_SELECTION_TYPE_COMPONENT);

		WorkerBlockSettings settings = WorkerStaffItem.loadBlockSettings(stack);		
		if(settings !=null) {			
			if(selection == SelectionType.SINGLE) {
				BlockPos pos = settings.getSingleBlockPos().orElse(null);
				if(pos !=null) {
					pose.pushPose();
					pose.translate(-projected.x(), -projected.y(), -projected.z());
					RenderHelper.renderLines(pose, new AABB(pos), Color.GREEN, buffersource);
					pose.popPose();
				}
			}
			if(selection == SelectionType.AREA) {
				BlockPos pos1 = settings.getAreaBlockPos1().orElse(null);
				BlockPos pos2 = settings.getAreaBlockPos2().orElse(null);
				if(pos1 !=null && pos2 !=null) {
					pose.pushPose();
					pose.translate(-projected.x(), -projected.y(), -projected.z());
					RenderHelper.renderLines(pose, AABB.encapsulatingFullBlocks(pos1, pos2), Color.GREEN, buffersource);
					pose.popPose();
				}
			}
			if(selection == SelectionType.LIST) {
				List<BlockPos> posList = settings.getBlockPosList().orElse(null);
				if(posList !=null && !posList.isEmpty()) {
					pose.pushPose();
					pose.translate(-projected.x(), -projected.y(), -projected.z());
					posList.forEach((pos) -> {
						RenderHelper.renderLines(pose, new AABB(pos), Color.GREEN, buffersource);
					});
					pose.popPose();
				}
			}
		}
	}

	@SubscribeEvent
	public static void renderWorldAfterTranslucent(RenderLevelStageEvent.AfterTranslucentBlocks event) {
		RenderBuffers buffers = Minecraft.getInstance().renderBuffers();
		MultiBufferSource.BufferSource buffersource = buffers.bufferSource();			
		Vec3 projected = event.getCamera().getPosition();		
		LocalPlayer player = Minecraft.getInstance().player;
		PoseStack pose = event.getPoseStack();		
		ItemStack mainHandStack = player.getMainHandItem();	
		
		if(mainHandStack.is(ModInit.WORKER_INVENTORY_ITEM.get())) {
			WorkerInventorySettings settings = mainHandStack.get(ModInit.WORKER_INVENTORY_SETTINGS_DATA_COMPONENT);
			if(settings !=null) {
				GlobalPos globalPos = settings.getPos();
				if(globalPos !=null && player.level().dimension().equals(globalPos.dimension())){
					BlockPos pos = globalPos.pos();
					pose.pushPose();
					pose.translate(-projected.x, -projected.y, -projected.z);

					pose.pushPose();
					pose.translate(pos.getX(), pos.getY(), pos.getZ());
					
					for(Direction face : Direction.values()) {
						float a = 0.2F;    				    
						float r = 0.5F;
						float g = 0.5F;
						float b = 0.5F;
						
						IOType faceIO = settings.getIO(face);
						
						if(faceIO == IOType.IN) {
							r = 0.0F;
							g = 0.0F;
							b = 1.0F;
						}
						else if(faceIO == IOType.OUT) {
							r = 1.0F;
							g = 0.0F;
							b = 0.0F;
						}
						else if(faceIO == IOType.BOTH) {
							r = 1.0F;
							g = 0.0F;
							b = 1.0F;
						}
						
						RenderHelper.renderFaceSolid(pose, pose.last().pose(), buffersource, BlockPos.ZERO, face, r, g, b, a);
					}

					pose.popPose();
					
					pose.popPose();
					buffersource.endBatch(RenderType.translucentMovingBlock());
				}
			}
		}
	}
	
}
