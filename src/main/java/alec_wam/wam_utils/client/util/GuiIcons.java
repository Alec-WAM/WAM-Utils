package alec_wam.wam_utils.client.util;

import alec_wam.wam_utils.WAMUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;

public enum GuiIcons {

	ENCHANTMENT_ALL(0, 0),
	ENCHANTMENT_ARMOR(0, 1),
	ENCHANTMENT_ARMOR_FEET(0, 2),
	ENCHANTMENT_ARMOR_LEGS(0, 3),
	ENCHANTMENT_ARMOR_CHEST(0, 4),
	ENCHANTMENT_ARMOR_HEAD(0, 5),
	ENCHANTMENT_WEAPON(0, 6),
	ENCHANTMENT_DIG(0, 7),
	ENCHANTMENT_FISHING_ROD(0, 8),
	ENCHANTMENT_TRIDENT(0, 9),
	ENCHANTMENT_BREAKABLE(0, 10),
	ENCHANTMENT_BOW(0, 11),
	ENCHANTMENT_WEARABLE(0, 12),
	ENCHANTMENT_CROSSBOW(0, 13),
	ENCHANTMENT_CURSE(0, 14),
	
	BUTTON_BOUNDINGBOX_ON(1, 0),
	BUTTON_BOUNDINGBOX_OFF(1, 1),

	ICON_WARNING(2, 0),
	ICON_CHECKMARK(2, 1);
	
	public static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(WAMUtils.MODID, "textures/gui/gui_icons.png");
	private final int row;
	private final int col;
	GuiIcons(int row, int col) {
		this.row = row; 
		this.col = col;
	}

    public void renderIcon(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        float textX = col * 16.0F;
		float textY = row * 16.0F;
        // guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ICONS, x, y, textX, textY, 16, 16, 256, 256);
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED, 
            ICONS, 
            x, y, 
            textX, textY, 
            width, height,
            16, 16, 
            256, 256,
			color
        );
    }
}
