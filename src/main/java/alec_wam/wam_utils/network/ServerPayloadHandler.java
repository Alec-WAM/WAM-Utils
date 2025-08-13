package alec_wam.wam_utils.network;

import alec_wam.wam_utils.common.blocks.BaseBE;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
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
                try (ProblemReporter.ScopedCollector problemreporter = new ProblemReporter.ScopedCollector(ModPayloadInit.problemPath(data), ModPayloadInit.LOGGER)) {
                    ValueInput valueInput = TagValueInput.create(problemreporter, level.registryAccess(), data.messageData());
                    baseBE.handleCustomMessage(data.messageType(), valueInput, false);
                }
                catch(Exception e){
                    ModPayloadInit.LOGGER.error("Failed to convert to ValueInput for BaseBE message", e);
                }
            }
        });
    }

    public static void handleFilterSlotOnMain(final FilterSlotPayload data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if(player == null) return;

            AbstractContainerMenu container = player.containerMenu;
            if (container == null)
                return;

            Slot slot = container.slots.get(data.slotNumber());
            ItemStack stack = data.stack();
            stack.setCount(data.count());
            slot.set(stack);
        });
    }
	
}
