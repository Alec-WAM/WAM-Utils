package alec_wam.wam_utils.network;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import alec_wam.wam_utils.WAMUtils;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ProblemReporter;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = WAMUtils.MODID)
public class ModPayloadInit {

	public static final Logger LOGGER = LogUtils.getLogger();

	@SubscribeEvent // on the mod event bus
	public static void register(RegisterPayloadHandlersEvent event) {
		System.out.println("Registering WAMUtils Payloads");
	    // Sets the current network version
	    final PayloadRegistrar registrar = event.registrar("1");
		registrar.playToClient(
			SyncWorkerJobPayload.TYPE,
    		SyncWorkerJobPayload.STREAM_CODEC
		);
		registrar.playToClient(
			SyncWorkerFishingPayload.TYPE,
    		SyncWorkerFishingPayload.STREAM_CODEC
		);
		registrar.playToClient(
			SyncWorkerFoodDataPayload.TYPE,
    		SyncWorkerFoodDataPayload.STREAM_CODEC
		);
	    registrar.playBidirectional(
           BaseBEMessagePayload.TYPE,
           BaseBEMessagePayload.STREAM_CODEC,
		   ServerPayloadHandler::handleBaseBEMessageOnMain
       	);
	   	registrar.playToClient(
			SyncClientShelfItemsPayload.TYPE,
    		SyncClientShelfItemsPayload.STREAM_CODEC
		);
	}	

	public static ProblemReporter.PathElement problemPath(CustomPacketPayload payload) {
        return new ModPayloadInit.PayloadElement(payload);
    }

    record PayloadElement(CustomPacketPayload payload) implements ProblemReporter.PathElement {
        @Override
        public String get() {
            return "CustomPacketPayload@" + this.payload.type().id();
        }
    }
	
}
