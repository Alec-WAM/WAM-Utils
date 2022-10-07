package alec_wam.wam_utils.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.WAMUtilsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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
	BUTTON_PUBLIC(1, 2),
	BUTTON_PRIVATE(1, 3),	

	ICON_WARNING(2, 0),
	ICON_CHECKMARK(2, 1);
	
	public static final ResourceLocation ICONS = new ResourceLocation(WAMUtilsMod.MODID, "textures/gui/gui_icons.png");
	private final int row;
	private final int col;
	GuiIcons(int row, int col) {
		this.row = row; 
		this.col = col;
	}
	
	@OnlyIn(Dist.CLIENT)
	public void renderIcon(PoseStack poseStack, double x, double y, double z, double width, double height) {
		RenderSystem.setShaderTexture(0, ICONS);
		RenderSystem.enableDepthTest();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		float textX = col * 16.0F;
		float textY = row * 16.0F;
		GuiUtils.drawScaledTexture(poseStack, x, y, z, width, height, textX, textX + 16.0F, textY, textY + 16.0F, 256.0F, 256.0F);
	}
}
