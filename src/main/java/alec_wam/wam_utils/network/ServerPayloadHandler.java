package alec_wam.wam_utils.network;

import alec_wam.wam_utils.common.blocks.BaseBE;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {

	public static void handleSyncWorkerJobOnMain(final SyncWorkerJobPayload data, final IPayloadContext context) {
        System.out.println("Server Sync Job");
    }

    public static void handleBaseBEMessageOnMain(final BaseBEMessagePayload data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if(context.player() == null) return;
            Level level = context.player().level();
            if(level == null) return;
            BlockEntity blockEntity = level.getBlockEntity(data.pos());
            if(blockEntity != null && blockEntity instanceof BaseBE baseBE) {
                baseBE.handleCustomMessage(data.messageType(), data.messageData(), false);
            }
        });
    }
	
}
