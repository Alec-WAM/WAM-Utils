package alec_wam.wam_utils.events;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import alec_wam.wam_utils.init.ItemInit;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class BlockEventHandler {

	@SubscribeEvent
	public static void anvilChange(final AnvilUpdateEvent event) {
		ItemStack left = event.getLeft().copy();
		ItemStack right = event.getRight();
		boolean rightSideEnchantmentItem = !right.isEmpty() && right.getItem() == ItemInit.SINGLE_ENCHANTMENT_ITEM.get();
		if(!left.isEmpty() && left.getItem() == ItemInit.SINGLE_ENCHANTMENT_ITEM.get()) {
			boolean fail = !rightSideEnchantmentItem;
			
			if(!fail) {
				//Right side is scroll
				Map<Enchantment, Integer> leftEnchantments = EnchantmentHelper.getEnchantments(left);
				Map<Enchantment, Integer> rightEnchantments = EnchantmentHelper.getEnchantments(right);
				
				for(Enchantment enchantment : rightEnchantments.keySet()) {
					//Only allow the same type scroll to enchant
					if(!leftEnchantments.containsKey(enchantment)) {
						fail = true;
						break;
					}
				}
			}
			if(fail) {
				event.setOutput(ItemStack.EMPTY);
				event.setCanceled(true);
				return;
			}
		}
		if(rightSideEnchantmentItem) {
	        int cost = 0;
			int renameCost = 0;
	        Map<Enchantment, Integer> leftEnchantments = EnchantmentHelper.getEnchantments(left);
			Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(right);
			boolean notCompatibleEnchants = false;
			boolean compatibleEnchants = false;
			for(Enchantment enchantment : enchantments.keySet()) {
                if (enchantment != null) {
                	int otherLevel = leftEnchantments.getOrDefault(enchantment, 0);
                	int level = enchantments.get(enchantment);
                	level = otherLevel == level ? level + 1 : Math.max(level, otherLevel);
                	
                	boolean canEnchant = enchantment.canEnchant(left);
                	if (event.getPlayer().getAbilities().instabuild || left.is(Items.ENCHANTED_BOOK)) {
                		canEnchant = true;
                	}
                	
                	for(Enchantment otherEnchantment : leftEnchantments.keySet()) {
                		if (otherEnchantment != enchantment && !enchantment.isCompatibleWith(otherEnchantment)) {
                			canEnchant = false;
                			++cost;
                		}
                	}
                	
                	if(!canEnchant) {
                		notCompatibleEnchants = true;
                	}
                	else {
                		compatibleEnchants = true;
                		
                		if (level > enchantment.getMaxLevel()) {
                			level = enchantment.getMaxLevel();
                		}
                		
                		leftEnchantments.put(enchantment, level);
                		
                		int k3 = 0;
                        switch (enchantment.getRarity()) {
                           case COMMON:
                              k3 = 1;
                              break;
                           case UNCOMMON:
                              k3 = 2;
                              break;
                           case RARE:
                              k3 = 4;
                              break;
                           case VERY_RARE:
                              k3 = 8;
                        }

                        k3 = Math.max(1, k3 / 2);

                        cost += k3 * level;
                        if (left.getCount() > 1) {
                        	cost = 40;
                        }
                	}
                }
			}
            
            if (notCompatibleEnchants && !compatibleEnchants) {
            	event.setOutput(ItemStack.EMPTY);
            	event.setCost(0);
            	return;
            }
            
            if (!left.isBookEnchantable(right)) {
            	left = ItemStack.EMPTY;
            }
            
            if (StringUtils.isBlank(event.getName())) {
                if (event.getLeft().hasCustomHoverName()) {
                   renameCost = 1;
                   cost += renameCost;
                   left.resetHoverName();
                }
             } else if (!event.getName().equals(event.getLeft().getHoverName().getString())) {
            	renameCost = 1;
            	cost += renameCost;
                left.setHoverName(Component.literal(event.getName()));
             }
            
            event.setCost(event.getCost() + cost);

            if (renameCost == cost && renameCost > 0 && event.getCost() >= 40) {
            	event.setCost(39);
            }
            
            if (event.getCost() >= 40 && !event.getPlayer().getAbilities().instabuild) {
            	left = ItemStack.EMPTY;
            }
            
            if (!left.isEmpty()) {
            	int k2 = left.getBaseRepairCost();
            	if (!right.isEmpty() && k2 < right.getBaseRepairCost()) {
            		k2 = right.getBaseRepairCost();
            	}
            	
            	if (renameCost != cost || renameCost == 0) {
            		k2 = AnvilMenu.calculateIncreasedRepairCost(k2);
            	}

            	left.setRepairCost(k2);
            	EnchantmentHelper.setEnchantments(leftEnchantments, left);
            }
            
            event.setOutput(left);
		}
	}
	
}
