package alec_wam.wam_utils.events;

import java.util.stream.Stream;

import com.mojang.blaze3d.platform.InputConstants;

import alec_wam.wam_utils.blocks.generator.furnace.FurnaceGeneratorScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ClientEventHandler {
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onItemTooltip(final ItemTooltipEvent event) {
    	Screen screen = Minecraft.getInstance().screen;
    	if(screen !=null) {
    		ItemStack stack = event.getItemStack();
			if(screen instanceof FurnaceGeneratorScreen) {
    			if(!stack.isEmpty()) {
    				int burnTime = ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
    		    	if(burnTime > 0) {
    		    		//TODO Make this match the generators real fuel per tick
    		    		int value = burnTime * 60;
    		    		String energyValue = String.format("%,d", value);
    		    		event.getToolTip().add(Component.translatable("gui.wam_utils.tooltip.burn.energyValue", energyValue));
    		    	}
    			}
    		}
    		//Tags
			if(event.getFlags().isAdvanced()) {
				boolean ctrlDown = false;
				try {
					long windowHandle = Minecraft.getInstance().getWindow().getWindow();
					ctrlDown = InputConstants.isKeyDown(windowHandle, InputConstants.KEY_LCONTROL);
				} catch(Exception e) {
	
				}
				if(!stack.isEmpty()) {
	    			Item item = stack.getItem();
	    			Stream<TagKey<Item>> tags = item.builtInRegistryHolder().tags();
	    			if(tags.count() > 0) {
		    			if(!ctrlDown) {
		    				event.getToolTip().add(Component.literal("Hold L-Ctrl for Tags").withStyle(ChatFormatting.BLUE));
		    			}
		    			else {
			    			event.getToolTip().add(Component.literal("Tags:").withStyle(ChatFormatting.GRAY));
			    			item.builtInRegistryHolder().tags().forEach((key) -> {
			    				String value = key.location().toString();
			    				event.getToolTip().add(Component.literal(value).withStyle(ChatFormatting.GRAY));
			    			});
		    			}
	    			}
	    		}
			}
    	}
    }
}
