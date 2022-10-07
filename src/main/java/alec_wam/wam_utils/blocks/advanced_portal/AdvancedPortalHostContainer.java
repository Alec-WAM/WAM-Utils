package alec_wam.wam_utils.blocks.advanced_portal;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.init.BlockInit;
import alec_wam.wam_utils.init.ContainerInit;
import alec_wam.wam_utils.server.container.WAMContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = WAMUtilsMod.MODID, bus = Bus.FORGE)
public class AdvancedPortalHostContainer extends WAMContainerMenu {

	public AdvancedPortalHostBE blockEntity;
    private Player playerEntity;
	
	public AdvancedPortalHostContainer(int windowId, BlockPos pos, Inventory playerInventory, Player player) {
		super(ContainerInit.ADVANCED_PORTAL_HOST_CONTAINER.get(), windowId);
		blockEntity = (AdvancedPortalHostBE) player.getCommandSenderWorld().getBlockEntity(pos);
        this.playerEntity = player;
	}

	@Override
    public boolean stillValid(Player playerIn) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), playerEntity, BlockInit.ADVANCED_PORTAL_HOST_BLOCK.get());
    }

	@Override
	public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
		return ItemStack.EMPTY;
	}
	
	@SubscribeEvent
	public static void onContainerOpened(PlayerContainerEvent.Open ev)
	{
		if(ev.getContainer() instanceof AdvancedPortalHostContainer container && ev.getEntity() instanceof ServerPlayer serverPlayer)
		{
			AdvancedPortalDataManager.UI_PLAYERS.add(serverPlayer);
			AdvancedPortalDataManager.sendPlayerPortals(serverPlayer);
		}
	}
	
	@SubscribeEvent
	public static void onContainerClosed(PlayerContainerEvent.Close ev)
	{
		if(ev.getContainer() instanceof WAMContainerMenu container && ev.getEntity() instanceof ServerPlayer serverPlayer)
			AdvancedPortalDataManager.UI_PLAYERS.remove(serverPlayer);
	}
	
	@Override
	public void receiveMessageFromScreen(CompoundTag nbt)
	{
		if(nbt.contains("SyncPortals")) {
			if(this.playerEntity instanceof ServerPlayer serverPlayer)
				AdvancedPortalDataManager.sendPlayerPortals(serverPlayer);
		}
	}

}
