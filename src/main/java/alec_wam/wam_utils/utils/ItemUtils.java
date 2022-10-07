package alec_wam.wam_utils.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class ItemUtils {
	
	public static Map<Enchantment, Integer> getAllNonCurseEnchantments(ItemStack stack){
		return EnchantmentHelper.getEnchantments(stack).entrySet().stream().filter((entry) -> {
			return !entry.getKey().isCurse();
		}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
	
	public static Map<Enchantment, Integer> getAllCurseEnchantments(ItemStack stack){
		return EnchantmentHelper.getEnchantments(stack).entrySet().stream().filter((entry) -> {
			return entry.getKey().isCurse();
		}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
	
	
	public static int getEnchantmentLevel(ItemStack stack, Enchantment enchantement) {
		if(!stack.isEmpty()) {
			Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
			return enchantments.getOrDefault(enchantement, 0);
		}
		return 0;
	}
	
	public static EnchantmentCategory getMainEnchantmentCategory(Set<Enchantment> enchantments) {
		Map<EnchantmentCategory, Integer> categories = new HashMap<>();
		for(Enchantment e : enchantments) {
			int count = categories.getOrDefault(e.category, 0);
			categories.put(e.category, count + 1);
		}
		
		return categories.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
	}
	
	public static EnchantmentCategory getMainEnchantmentCategory(ItemStack stack) {
		if(!stack.isEmpty()) {
			Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
			
			Map<EnchantmentCategory, Integer> categories = new HashMap<>();
			
			for(Enchantment e : enchantments.keySet()) {
				int count = categories.getOrDefault(e.category, 0);
				categories.put(e.category, count + 1);
			}
			
			return categories.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
		}
		return null;
	}
	
	public static List<Item> getTagItems(ResourceLocation tag){
		Registry<Item> itemRegistry = RegistryAccess.BUILTIN.get().registryOrThrow(Registry.ITEM_REGISTRY);
		Optional<HolderSet.Named<Item>> holderSet = itemRegistry.getTag(ItemTags.create(tag));
		if(holderSet.isPresent()) {
			HolderSet.Named<Item> holder = holderSet.get();
			return holder.stream().map(Holder::get).collect(Collectors.toList());
		}
		return Lists.newArrayList();
	}
	
	//TOOLS
	public static boolean isShield(ItemStack stack) {
		return stack.canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK);
	}
	
	public static boolean isMeleeWeapon(ItemStack stack) {
		Item item = stack.getItem();
		if(item instanceof SwordItem) {
			return true;
		}
		if(item instanceof AxeItem) {
			return true;
		}
		if(item.canDisableShield(stack, ItemStack.EMPTY, null, null)) {
			return true;
		}
		return false;
	}
	
	public static boolean isSword(ItemStack stack) {
		Item item = stack.getItem();
		if(item instanceof SwordItem) {
			return true;
		}
		return false;
	}
	
}
