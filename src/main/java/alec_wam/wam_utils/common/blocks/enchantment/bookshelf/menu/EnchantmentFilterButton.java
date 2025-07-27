package alec_wam.wam_utils.common.blocks.enchantment.bookshelf.menu;

import java.util.EnumMap;

import alec_wam.wam_utils.client.screen.GuiIconButton;
import alec_wam.wam_utils.client.util.GuiIcons;
import alec_wam.wam_utils.common.blocks.enchantment.bookshelf.EnchantmentCategoryFilter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

public class EnchantmentFilterButton extends GuiIconButton {

    public static final EnumMap<EnchantmentCategoryFilter, GuiIcons> ICONS = new EnumMap<>(EnchantmentCategoryFilter.class);

    static {
        ICONS.put(EnchantmentCategoryFilter.ALL, GuiIcons.ENCHANTMENT_ALL);
        ICONS.put(EnchantmentCategoryFilter.ARMOR, GuiIcons.ENCHANTMENT_ARMOR);
        ICONS.put(EnchantmentCategoryFilter.ARMOR_FEET, GuiIcons.ENCHANTMENT_ARMOR_FEET);
        ICONS.put(EnchantmentCategoryFilter.ARMOR_LEGS, GuiIcons.ENCHANTMENT_ARMOR_LEGS);
        ICONS.put(EnchantmentCategoryFilter.ARMOR_CHEST, GuiIcons.ENCHANTMENT_ARMOR_CHEST);
        ICONS.put(EnchantmentCategoryFilter.ARMOR_HEAD, GuiIcons.ENCHANTMENT_ARMOR_HEAD);
        ICONS.put(EnchantmentCategoryFilter.WEAPON, GuiIcons.ENCHANTMENT_WEAPON);
        ICONS.put(EnchantmentCategoryFilter.BOW, GuiIcons.ENCHANTMENT_BOW);
        ICONS.put(EnchantmentCategoryFilter.CROSSBOW, GuiIcons.ENCHANTMENT_CROSSBOW);
        ICONS.put(EnchantmentCategoryFilter.MINING, GuiIcons.ENCHANTMENT_DIG);
        ICONS.put(EnchantmentCategoryFilter.FISHING_ROD, GuiIcons.ENCHANTMENT_FISHING_ROD);
        ICONS.put(EnchantmentCategoryFilter.TRIDENT, GuiIcons.ENCHANTMENT_TRIDENT);
        ICONS.put(EnchantmentCategoryFilter.DURABILITY, GuiIcons.ENCHANTMENT_BREAKABLE);
        ICONS.put(EnchantmentCategoryFilter.CURSE, GuiIcons.ENCHANTMENT_CURSE);
    }

    private EnchantmentCategoryFilter filter;
	public EnchantmentFilterButton(int x, int y, int width, int height, EnchantmentCategoryFilter filter, Button.OnPress onPress) {
		super(x, y, width, height, ICONS.get(filter), onPress, null);
		this.filter = filter;
	}

    @Override
    public GuiIcons getIcon() {
        return ICONS.get(filter);
    }
	
	@Override
	public void onPress() {
		this.filter = Screen.hasShiftDown() ? filter.getPrev() : filter.getNext();
        this.onPress.onPress(this);
	}
	
	public EnchantmentCategoryFilter getFilter() {
		return filter;
	}
	
    @Override
	public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		if(this.filter !=null) {
			guiGraphics.setTooltipForNextFrame(this.filter.getTooltip(), mouseX, mouseY);
		}
	}

}
