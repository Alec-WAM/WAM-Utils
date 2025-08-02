package alec_wam.wam_utils.common.helpers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.items.IItemHandler;

public class ItemHelper {


	
	public static int getEnchantmentLevel(LivingEntity entity, ResourceKey<Enchantment> enchantment) {
//		var lookup = CommonHooks.resolveLookup(net.minecraft.core.registries.Registries.ENCHANTMENT);
		Holder<Enchantment> holder = entity.level().registryAccess().get(enchantment).orElse(null);
		return holder == null ? 0 : EnchantmentHelper.getEnchantmentLevel(holder, entity);
	}
	
	public static int getEnchantmentLevel(Level level, ItemStack stack, ResourceKey<Enchantment> enchantment) {
		Holder<Enchantment> holder = level.registryAccess().get(enchantment).orElse(null);
		return holder == null ? 0 : stack.getEnchantmentLevel(holder);
	}
	
	public static boolean hasEnchantment(Level level, ItemStack stack, ResourceKey<Enchantment> enchantment, int requiredLevel) {
		return getEnchantmentLevel(level, stack, enchantment) >= requiredLevel;
	}

	public static Set<Holder<Enchantment>> getEnchantments(ItemStack stack){
		return stack.getOrDefault(EnchantmentHelper.getComponentType(stack), ItemEnchantments.EMPTY).keySet();
	}

	public static ItemEnchantments getItemEnchantments(ItemStack stack){
		
		ItemEnchantments itemenchantments = stack.getOrDefault(EnchantmentHelper.getComponentType(stack), ItemEnchantments.EMPTY);
		
		if(stack.is(Items.ENCHANTED_BOOK)){
			return itemenchantments;
		}
        // Neo: Respect gameplay-only enchantments when enchantment effect tag checks
        var lookup = net.neoforged.neoforge.common.CommonHooks.resolveLookup(net.minecraft.core.registries.Registries.ENCHANTMENT);
        if (lookup != null) {
            itemenchantments = stack.getAllEnchantments(lookup);
        }
		return itemenchantments;
	}

	public static final Comparator<ItemStack> SORT_STACK_SIZE = (stack1, stack2) -> {
		return Integer.compare(stack2.getCount(), stack1.getCount());
	};
	
	public static ItemStack copy(ItemStack stack, int size) {
		ItemStack copy = stack.copy();
		copy.setCount(size);
		return copy;
	}
	
	public static List<ItemStack> getEntityInventoryItems(LivingEntity entity){
		List<ItemStack> items = new ArrayList<>();

		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = entity.getItemBySlot(slot);
			if (!stack.isEmpty()) {
				items.add(stack);
			}
		}

		if (entity instanceof InventoryCarrier) {
			SimpleContainer container = ((InventoryCarrier) entity).getInventory();
			if (container != null) {
				for (int i = 0; i < container.getContainerSize(); i++) {
					ItemStack stack = container.getItem(i);
					if (!stack.isEmpty()) {
						items.add(stack);
					}
				}
			}
		}
		return items;
	}
	
	private static final Comparator<ItemStack> BEST_ITEM_SORTER_BASE = Comparator.comparingInt((ItemStack stack) -> {
		Set<Entry<Holder<Enchantment>>> enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet();
			return enchantments.size();
		})
		.thenComparingInt((ItemStack stack) -> {
			return stack.getMaxDamage() - stack.getDamageValue();
		})
		.thenComparing((ItemStack stack) -> {
			return stack.has(DataComponents.CUSTOM_NAME);
		});
	/**
	 * This sorts items 
	 * 1. How many enchantments it has
	 * 2. Damage on the item
	 * 3. If it has a custom name
	 */
	public static final Comparator<ItemStack> BEST_ITEM_SORTER = BEST_ITEM_SORTER_BASE.reversed();
	
	public static ItemStack findBestTool(LivingEntity entity, BlockState state) {		
		return findBestTool(getEntityInventoryItems(entity), state);
	}
	
	public static ItemStack findBestMelee(LivingEntity entity) {		
		return findBestMelee(getEntityInventoryItems(entity));
	}
	
	public static ItemStack findBestFood(LivingEntity entity) {		
		return findBestFood(getEntityInventoryItems(entity), entity);
	}
	
	public static ItemStack findBestArmor(LivingEntity entity, EquipmentSlot slot) {		
		return findBestArmor(getEntityInventoryItems(entity), slot);
	}
	
	public static ItemStack findItem(LivingEntity entity, Predicate<ItemStack> searchStack) {	
		return findItem(entity, searchStack, SORT_STACK_SIZE);
	}
	
	public static ItemStack findItem(LivingEntity entity, Predicate<ItemStack> searchStack, @Nullable Comparator<ItemStack> sort) {		
		if(sort == null) {
			return findItem(entity, searchStack);
		}
		return findItem(getEntityInventoryItems(entity), searchStack, sort);
	}
	
	public static ItemStack findBestTool(IItemHandler handler, BlockState state) {
		List<ItemStack> items = new ArrayList<>();
		if(handler !=null) {
			for(int i = 0; i < handler.getSlots(); i++) {
				ItemStack stack = handler.getStackInSlot(i);
				if(!stack.isEmpty()) {
					items.add(stack);
				}
			}
		}
		return findBestTool(items, state);
	}
	
	public static ItemStack findBestMelee(IItemHandler handler) {
		List<ItemStack> items = new ArrayList<>();
		if(handler !=null) {
			for(int i = 0; i < handler.getSlots(); i++) {
				ItemStack stack = handler.getStackInSlot(i);
				if(!stack.isEmpty()) {
					items.add(stack);
				}
			}
		}
		return findBestMelee(items);
	}
	
	public static ItemStack findBestFood(IItemHandler handler, LivingEntity entity) {
		List<ItemStack> items = new ArrayList<>();
		if(handler !=null) {
			for(int i = 0; i < handler.getSlots(); i++) {
				ItemStack stack = handler.getStackInSlot(i);
				if(!stack.isEmpty()) {
					items.add(stack);
				}
			}
		}
		return findBestFood(items, entity);
	}
	
	public static ItemStack findBestArmor(IItemHandler handler, EquipmentSlot slot) {
		List<ItemStack> items = new ArrayList<>();
		if(handler !=null) {
			for(int i = 0; i < handler.getSlots(); i++) {
				ItemStack stack = handler.getStackInSlot(i);
				if(!stack.isEmpty()) {
					items.add(stack);
				}
			}
		}
		return findBestArmor(items, slot);
	}
	
	public static ItemStack findItem(IItemHandler handler, Predicate<ItemStack> searchStack) {	
		return findItem(handler, searchStack, SORT_STACK_SIZE);
	}
	
	public static ItemStack findItem(IItemHandler handler, Predicate<ItemStack> searchStack, Comparator<ItemStack> sort) {
		List<ItemStack> items = new ArrayList<>();
		if(handler !=null) {
			for(int i = 0; i < handler.getSlots(); i++) {
				ItemStack stack = handler.getStackInSlot(i);
				if(!stack.isEmpty()) {
					items.add(stack);
				}
			}
		}
		return findItem(items, searchStack, sort);
	}
	
	/**
	 * Find the best tool for digging the specified blockstate
	 * @param stacks
	 * @param state
	 * @return best tool for block state
	 */
	public static ItemStack findBestTool(List<ItemStack> stacks, BlockState state) {
		Comparator<ItemStack> toolSorter = Comparator.comparingDouble((ItemStack stack) -> {
			return stack.getDestroySpeed(state);
		})
		.thenComparing(BEST_ITEM_SORTER_BASE)
		.reversed();
		Optional<ItemStack> tool = stacks.stream().filter((stack) -> isCorrectToolForState(stack, state)).sorted(toolSorter).findFirst();
		
		return tool.orElse(ItemStack.EMPTY);
	}
	
	/**
	 * Find the best tool for melee attack
	 * @param stacks
	 * @param state
	 * @return best tool for melee attack
	 */
	public static ItemStack findBestMelee(List<ItemStack> stacks) {		
		Comparator<ItemStack> weaponSorter = Comparator.comparingDouble((ItemStack stack) -> {
			return getMeleeDamage(stack);
		})
		.thenComparing(BEST_ITEM_SORTER_BASE)
		.reversed();
		Optional<ItemStack> tool = stacks.stream().filter((stack) -> isMeleeWeapon(stack)).sorted(weaponSorter).findFirst();
		
		return tool.orElse(ItemStack.EMPTY);
	}
	
	/**
	 * Find the best food
	 * @param stacks
	 * @param state
	 * @return best tool for block state
	 */
	public static ItemStack findBestFood(List<ItemStack> stacks, LivingEntity entity) {		
		Optional<ItemStack> tool = stacks.stream().filter((stack) -> stack.has(DataComponents.FOOD) && !isBadFood(stack, entity)).sorted((stack1, stack2) -> {
			FoodProperties foodProp1 = stack1.get(DataComponents.FOOD);
			FoodProperties foodProp2 = stack2.get(DataComponents.FOOD);
			float foodValue1 = foodProp1 == null ? 0.0F : (float)foodProp1.nutrition() + foodProp1.saturation();
			float foodValue2 = foodProp2 == null ? 0.0F : (float)foodProp2.nutrition() + foodProp2.saturation();
			return Float.compare(foodValue2, foodValue1);
		}).findFirst();
		
		return tool.orElse(ItemStack.EMPTY);
	}
	
	/**
	 * Find the best food
	 * @param stacks
	 * @param state
	 * @return best tool for block state
	 */
	public static ItemStack findBestArmor(List<ItemStack> stacks, EquipmentSlot slot) {		
		// Sort armor based on
		// 1. Armor Value + Toughness
		// 2. How many enchantments it has
		// 3. Damage on the item
		// 4. If it has a custom name
		Comparator<ItemStack> armorSorter = Comparator.comparingDouble((ItemStack stack) -> {
			double armorValue = getApproximateAttributeWith(stack, Attributes.ARMOR, slot);
            double armorToughness = getApproximateAttributeWith(stack, Attributes.ARMOR_TOUGHNESS, slot);
            double totalValue = armorValue + armorToughness;
			return totalValue;
		})
		.thenComparing(BEST_ITEM_SORTER_BASE)
		.reversed();
		
		Optional<ItemStack> tool = stacks.stream().filter((stack) -> stack.has(DataComponents.EQUIPPABLE) && stack.get(DataComponents.EQUIPPABLE).slot() == slot).sorted(armorSorter).findFirst();
		
		return tool.orElse(ItemStack.EMPTY);
	}
	
	public static ItemStack findItem(List<ItemStack> stacks, Predicate<ItemStack> searchStack, Comparator<ItemStack> sort) {		
		Comparator<ItemStack> sorter = sort == null ? SORT_STACK_SIZE : sort;
		Optional<ItemStack> item = stacks.stream().filter(searchStack).sorted(sorter).findFirst();
		
		return item.orElse(ItemStack.EMPTY);
	}
	
	public static List<ItemStack> findItems(List<ItemStack> stacks, Predicate<ItemStack> searchStack, Comparator<ItemStack> sort) {		
		return stacks.stream().filter(searchStack).sorted(sort).toList();
	}
	
	public static int findSlot(SimpleContainer container, ItemStack stack) {
		for(int i = 0; i < container.getContainerSize(); ++i) {
	         ItemStack itemstack = container.getItem(i);
	         if (ItemStack.isSameItemSameComponents(itemstack, stack)) {
	        	 return i;
	         }
		}
		return -1;
	}
	
	public static ItemStack removeItem(Container container, int index, int amount) {
		return index >= 0 && index < container.getContainerSize() && !container.getItem(index).isEmpty() && amount > 0
				? container.getItem(index).split(amount)
				: ItemStack.EMPTY;
	}

	public static ItemStack takeItem(Container container, int slot) {
		if(slot >= 0 && slot < container.getContainerSize()) {
			ItemStack old = container.getItem(slot);
			container.setItem(slot, ItemStack.EMPTY);
			return old;
		}
		return ItemStack.EMPTY;
	}
	
	public static ItemStack findFirstWithTag(Container container, TagKey<Item> tag) {
		for(int i = 0; i < container.getContainerSize(); ++i) {
	         ItemStack itemstack = container.getItem(i);
	         if (itemstack.is(tag)) {
	        	 return itemstack;
	         }
		}
		return ItemStack.EMPTY;
	}
	
	/**
	 * Test if item has food properties and if it has a harmful effect
	 * @param stack
	 * @param entity
	 * @return If item FoodProperties have a harmful effect
	 */
	public static boolean isBadFood(ItemStack stack, @Nullable LivingEntity entity) {
		if(stack.has(DataComponents.CONSUMABLE)) {
			Consumable consumeableProps = stack.get(DataComponents.CONSUMABLE);
			if(consumeableProps !=null) {
				List<ConsumeEffect> effects = consumeableProps.onConsumeEffects();
				for(ConsumeEffect effect : effects) {
					if(effect instanceof ApplyStatusEffectsConsumeEffect applyEffect) {
						for(MobEffectInstance mobEffect : applyEffect.effects()) {
							if(mobEffect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	public static boolean isTool(ItemStack stack) {
		if(stack.has(DataComponents.TOOL) && !isSword(stack)) {
			return true;
		}
		if(isShears(stack)) {
			return true;
		}
		if(stack.getEquipmentSlot() == EquipmentSlot.OFFHAND) {
			return true;
		}
		return false;
	}
	
	public static boolean isCorrectToolForState(ItemStack stack, BlockState state) {
//		if(!state.requiresCorrectToolForDrops()) {
//			return true;
//		}
		if(isShears(stack)) {
			if(isShearEfficientBlock(state)) {
				return true;
			}
		}
		return stack.isCorrectToolForDrops(state);
	}
	
	public static boolean canPerformAny(ItemStack stack, Set<ItemAbility> abilities){
		return abilities.stream().anyMatch(stack::canPerformAction);
	}

	public static boolean isShears(ItemStack stack) {
		return stack.getItem() instanceof ShearsItem || canPerformAny(stack, ItemAbilities.DEFAULT_SHEARS_ACTIONS);
	}
	
	public static boolean isShearEfficientBlock(BlockState state) {
		return state.is(BlockTags.LEAVES) || state.is(Blocks.COBWEB) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.TALL_DRY_GRASS)|| state.is(Blocks.FERN) || state.is(Blocks.DEAD_BUSH) || state.is(Blocks.HANGING_ROOTS) || state.is(Blocks.VINE) || state.is(Blocks.TRIPWIRE) || state.is(BlockTags.WOOL);
	}
	
	public static boolean isSword(ItemStack stack) {
		return stack.is(ItemTags.SWORDS);
	}
	
	public static boolean isAxe(ItemStack stack) {
		return stack.getItem() instanceof AxeItem || canPerformAny(stack, ItemAbilities.DEFAULT_AXE_ACTIONS) || stack.is(ItemTags.AXES);
	}
	
	public static boolean isMeleeWeapon(ItemStack stack) {
		return isSword(stack) || isAxe(stack) || isGenericWeapon(stack);
	}

	public static boolean isGenericWeapon(ItemStack stack) {
		Weapon weapon = stack.get(DataComponents.WEAPON);
		Tool tool = stack.get(DataComponents.TOOL);
		return weapon != null && weapon.itemDamagePerAttack() == 1 && (tool == null || tool.damagePerBlock() > 1);
	}
	
	public static boolean isRangedWeapon(ItemStack stack) {
		return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
	}

	public static boolean isShield(ItemStack stack) {
		return stack.has(DataComponents.BLOCKS_ATTACKS);
	}

	public static boolean isArmor(ItemStack stack, @Nullable LivingEntity entity) {
		EquipmentSlot equipmentslot = entity !=null ? entity.getEquipmentSlotForItem(stack) : stack.getEquipmentSlot();
		return getApproximateAttributeWith(stack, Attributes.ARMOR, equipmentslot) > 0.0D;
	}
	
	public static double getMeleeDamage(ItemStack stack) {
		double damage = getApproximateAttributeWith(stack, Attributes.ATTACK_DAMAGE, EquipmentSlot.MAINHAND);
//			damage += EnchantmentHelper.getDamageBonus(stack, MobType.UNDEFINED);
		return damage;
	}

    private static double getApproximateAttributeWith(ItemStack item, Holder<Attribute> attribute, EquipmentSlot slot) {
        double d0 =  0.0;
        ItemAttributeModifiers itemattributemodifiers = item.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        // Neo: Respect gameplay modifiers
        itemattributemodifiers = item.getAttributeModifiers();
        return itemattributemodifiers.compute(d0, slot);
    }

}
