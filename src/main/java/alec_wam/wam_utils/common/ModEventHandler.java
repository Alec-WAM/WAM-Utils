package alec_wam.wam_utils.common;

import alec_wam.wam_utils.WAMUtils;
import alec_wam.wam_utils.common.entities.workers.WorkerEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = WAMUtils.MODID)
public class ModEventHandler {
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void mobDrops(LivingDropsEvent event){
        if(event.isCanceled())return;
        DamageSource damageSource = event.getSource();
		
		if(damageSource.getEntity() !=null) {
			Entity causingEntity = damageSource.getEntity();
			if(causingEntity instanceof WorkerEntity worker) {
				
				if(worker.getJob() !=null) {
					if(worker.getJob().pickupMobDrops()) {
						event.getDrops().removeIf((item) -> worker.silentPickupItem(item));
					}
				}
			}
		}
    }
}
