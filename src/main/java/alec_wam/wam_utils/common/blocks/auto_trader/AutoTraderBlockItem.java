package alec_wam.wam_utils.common.blocks.auto_trader;

import java.util.function.Consumer;

import alec_wam.wam_utils.common.ModInit;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class AutoTraderBlockItem extends BlockItem {

    public AutoTraderBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if(!level.isClientSide) {
            if(player.isCrouching()){
                ItemStack handItem = player.getItemInHand(hand);
                handItem.remove(ModInit.AUTO_TRADER_VILLAGER_DATA_COMPONENT.get());
                return InteractionResult.SUCCESS_SERVER;
            }
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(
        ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
        if(stack.has(ModInit.AUTO_TRADER_VILLAGER_DATA_COMPONENT.get())) {
            tooltipAdder.accept(Component.literal("Villager: " + stack.get(ModInit.AUTO_TRADER_VILLAGER_DATA_COMPONENT.get())));
        }
    }
}
