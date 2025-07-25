package alec_wam.wam_utils.common.items;

import java.util.function.Consumer;

import alec_wam.wam_utils.common.ModInit;
import alec_wam.wam_utils.common.entities.workers.WorkerInventorySettings;
import alec_wam.wam_utils.common.entities.workers.WorkerInventorySettings.IOType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class WorkerInventoryItem extends Item {
	
	public WorkerInventoryItem(Properties props) {
		super(props);
	}
	
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack itemstack = player.getItemInHand(hand);

		if (player.isCrouching()) {
			if (!(level instanceof ServerLevel)) {
				return InteractionResult.SUCCESS;
			}
			if (hand == InteractionHand.OFF_HAND) {
				clearSettings(itemstack);
				player.displayClientMessage(Component.translatable("wamutils.message.workers.staff.clear_selection"), true);
				return InteractionResult.SUCCESS;
			}
		}
		return super.use(level, player, hand);
	}
	
	public static void clearSettings(ItemStack stack) {
		stack.remove(ModInit.WORKER_INVENTORY_SETTINGS_DATA_COMPONENT);
	}
	
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		if(player.isCrouching()) {
			if (!(level instanceof ServerLevel)) {
				return InteractionResult.PASS;
			} else {
				BlockPos pos = context.getClickedPos();
				Direction face = context.getClickedFace();
				ItemStack stack = context.getItemInHand();
				
				WorkerInventorySettings settings = stack.getOrDefault(ModInit.WORKER_INVENTORY_SETTINGS_DATA_COMPONENT, new WorkerInventorySettings());
				
				GlobalPos savedPos = settings.getPos();				
				if(savedPos == null || !level.dimension().equals(savedPos.dimension()) || !pos.equals(savedPos.pos())) {
					final GlobalPos newGlobalPos = GlobalPos.of(level.dimension(), pos);
					settings.setPos(newGlobalPos);
					stack.set(ModInit.WORKER_INVENTORY_SETTINGS_DATA_COMPONENT, settings);
					return InteractionResult.SUCCESS_SERVER;
				}
				
				IOType ioType = settings.getIO(face);					
				final IOType nextType = ioType.getNextType();
				stack.update(ModInit.WORKER_INVENTORY_SETTINGS_DATA_COMPONENT, new WorkerInventorySettings(), (set) -> {
					WorkerInventorySettings newSettings = set.setIO(face, nextType);
					return newSettings;
				});
				
				player.displayClientMessage(Component.translatable("wamutils.message.workers.io_type." + nextType.getSerializedName().toLowerCase()), true);				
				return InteractionResult.SUCCESS_SERVER;
			}
		}
		
		return InteractionResult.PASS;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void appendHoverText(
	        ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag
    ) {
		super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
		WorkerInventorySettings settings = stack.get(ModInit.WORKER_INVENTORY_SETTINGS_DATA_COMPONENT);		
		if(settings !=null) {			
			GlobalPos globalPos = settings.getPos();
			if(globalPos !=null) {
				tooltipAdder.accept(Component.literal(globalPos.dimension().location().toString()));
				tooltipAdder.accept(Component.literal(globalPos.pos().toShortString()));
			}
			tooltipAdder.accept(Component.literal("Input: " + settings.getInputFaces().size()));
			tooltipAdder.accept(Component.literal("Output: " + settings.getOutputFaces().size()));
		}
	}

}
