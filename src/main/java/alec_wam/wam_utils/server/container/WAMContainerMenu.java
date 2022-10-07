package alec_wam.wam_utils.server.container;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.mojang.datafixers.util.Pair;

import alec_wam.wam_utils.WAMUtilsMod;
import alec_wam.wam_utils.server.container.GenericDataSerializers.DataPair;
import alec_wam.wam_utils.server.network.MessageContainerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.network.NetworkDirection;

@EventBusSubscriber(modid = WAMUtilsMod.MODID, bus = Bus.FORGE)
public abstract class WAMContainerMenu extends AbstractContainerMenu {

	private final List<GenericContainerData<?>> genericData = new ArrayList<>();
	private final List<ServerPlayer> usingPlayers = new ArrayList<>();
	protected WAMContainerMenu(MenuType<?> p_38851_, int p_38852_) {
		super(p_38851_, p_38852_);
	}
	
	public void addGenericData(GenericContainerData<?> newData)
	{
		genericData.add(newData);
	}

	@Override
	public void broadcastChanges()
	{
		super.broadcastChanges();
		List<Pair<Integer, DataPair<?>>> toSync = new ArrayList<>();
		for(int i = 0; i < genericData.size(); i++)
		{
			GenericContainerData<?> data = genericData.get(i);
			if(data.needsUpdate())
				toSync.add(Pair.of(i, data.dataPair()));
		}
		if(!toSync.isEmpty())
			for(ServerPlayer player : usingPlayers)
				WAMUtilsMod.packetHandler.sendTo(
						new MessageContainerData(toSync), player.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT
				);
	}

	public void receiveSync(List<Pair<Integer, DataPair<?>>> synced)
	{
		for(Pair<Integer, DataPair<?>> syncElement : synced)
			genericData.get(syncElement.getFirst()).processSync(syncElement.getSecond().data());
	}
	
	@Override
	public void removed(@Nonnull Player player)
	{
		super.removed(player);
	}
	
	@SubscribeEvent
	public static void onContainerOpened(PlayerContainerEvent.Open ev)
	{
		if(ev.getContainer() instanceof WAMContainerMenu container && ev.getEntity() instanceof ServerPlayer serverPlayer)
		{
			container.usingPlayers.add(serverPlayer);
			List<Pair<Integer, DataPair<?>>> list = new ArrayList<>();
			for(int i = 0; i < container.genericData.size(); i++)
				list.add(Pair.of(i, container.genericData.get(i).dataPair()));
			WAMUtilsMod.packetHandler.sendTo(
					new MessageContainerData(list), serverPlayer.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT
			);
		}
	}

	@SubscribeEvent
	public static void onContainerClosed(PlayerContainerEvent.Close ev)
	{
		if(ev.getContainer() instanceof WAMContainerMenu container && ev.getEntity() instanceof ServerPlayer serverPlayer)
			container.usingPlayers.remove(serverPlayer);
	}

    protected int addSlotRange(IItemHandler handler, int index, int x, int y, int amount, int dx) {
        for (int i = 0 ; i < amount ; i++) {
            addSlot(new SlotItemHandler(handler, index, x, y));
            x += dx;
            index++;
        }
        return index;
    }

    protected int addSlotBox(IItemHandler handler, int index, int x, int y, int horAmount, int dx, int verAmount, int dy) {
        for (int j = 0 ; j < verAmount ; j++) {
            index = addSlotRange(handler, index, x, y, horAmount, dx);
            y += dy;
        }
        return index;
    }

    protected void layoutPlayerInventorySlots(IItemHandler playerInventory, int leftCol, int topRow) {
        // Player inventory
        addSlotBox(playerInventory, 9, leftCol, topRow, 9, 18, 3, 18);

        // Hotbar
        topRow += 58;
        addSlotRange(playerInventory, 0, leftCol, topRow, 9, 18);
    }

    public void receiveMessageFromScreen(CompoundTag nbt)
	{
    	WAMUtilsMod.LOGGER.debug("Message From Screen");
		
	}

}
