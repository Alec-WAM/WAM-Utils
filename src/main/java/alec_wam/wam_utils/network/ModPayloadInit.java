package alec_wam.wam_utils.network;

import alec_wam.wam_utils.WAMUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = WAMUtils.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModPayloadInit {

	@SubscribeEvent // on the mod event bus
	public static void register(RegisterPayloadHandlersEvent event) {
		System.out.println("Registering WAMUtils Payloads");
	    // Sets the current network version
	    final PayloadRegistrar registrar = event.registrar("1");
	    registrar.playToClient(
    		SyncWorkerJobPayload.TYPE,
    		SyncWorkerJobPayload.STREAM_CODEC,
    		ClientPayloadHandler::handleSyncWorkerJobOnMain
	    );
//	    registrar.playBidirectional(
//            SyncWorkerJobPayload.TYPE,
//            SyncWorkerJobPayload.STREAM_CODEC,
//            new DirectionalPayloadHandler<>(
//                ClientPayloadHandler::handleSyncWorkerJobOnMain,
//                ServerPayloadHandler::handleSyncWorkerJobOnMain
//            )
//        );
	}
	
}
