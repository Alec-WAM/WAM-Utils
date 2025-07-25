package alec_wam.wam_utils.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {

	public static void handleSyncWorkerJobOnMain(final SyncWorkerJobPayload data, final IPayloadContext context) {
        System.out.println("Server Sync Job");
    }
	
}
