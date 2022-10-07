package alec_wam.wam_utils.blocks.machine.enchantment_remover;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;

import alec_wam.wam_utils.init.ItemInit;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;

public class SingleEnchantmentItem extends Item {

	public SingleEnchantmentItem(Properties props) {
		super(props);
	}

	@Override
	public boolean isFoil(ItemStack p_41166_) {
		return true;
	}

	@Override
	public boolean isEnchantable(ItemStack p_41168_) {
		return false;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void appendHoverText(ItemStack p_41157_, @Nullable Level p_41158_, List<Component> p_41159_, TooltipFlag p_41160_) {
		//super.appendHoverText(p_41157_, p_41158_, p_41159_, p_41160_);
//		EnchantmentHelper.getEnchantments(p_41157_).entrySet().stream().forEach(entry -> {
//			Enchantment enchantment = entry.getKey();
//			Integer level = entry.getValue();
//			Registry.ENCHANTMENT.getOptional(EnchantmentHelper.getEnchantmentId(enchantment)).ifPresent((p_41708_) -> {
//				p_41159_.add(p_41708_.getFullname(level));
//			});
//		});
	}

	public static ItemStack createForEnchantment(EnchantmentInstance enchantmentInstance) {
		ItemStack itemstack = new ItemStack(ItemInit.SINGLE_ENCHANTMENT_ITEM.get());
		Map<Enchantment, Integer> enchantments = Maps.newHashMap();
		enchantments.put(enchantmentInstance.enchantment, enchantmentInstance.level);
		EnchantmentHelper.setEnchantments(enchantments, itemstack);
		return itemstack;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void fillItemCategory(CreativeModeTab p_41151_, NonNullList<ItemStack> p_41152_) {
		if (p_41151_ == CreativeModeTab.TAB_SEARCH) {
			for(Enchantment enchantment : Registry.ENCHANTMENT) {
				if (enchantment.allowedInCreativeTab(this, p_41151_) && !enchantment.isCurse()) {
					for(int i = enchantment.getMinLevel(); i <= enchantment.getMaxLevel(); ++i) {
						p_41152_.add(createForEnchantment(new EnchantmentInstance(enchantment, i)));
					}
				}
			}
		} else if (p_41151_.getEnchantmentCategories().length != 0) {
			for(Enchantment enchantment1 : Registry.ENCHANTMENT) {
				if (enchantment1.allowedInCreativeTab(this, p_41151_) && !enchantment1.isCurse()) {
					p_41152_.add(createForEnchantment(new EnchantmentInstance(enchantment1, enchantment1.getMaxLevel())));
				}
			}
		}
	}
	
	@Override
	public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
		if(book.getItem() != ItemInit.SINGLE_ENCHANTMENT_ITEM.get()) {
			return false;
		}
		Map<Enchantment, Integer> stackEnchantments = EnchantmentHelper.getEnchantments(stack);
		Map<Enchantment, Integer> bookEnchantments = EnchantmentHelper.getEnchantments(book);
		
		for(Enchantment enchantment : bookEnchantments.keySet()) {
			//Only allow the same type scroll to enchant
			if(!stackEnchantments.containsKey(enchantment)) {
				return false;
			}
		}
		return true;
	}

}
