package alec_wam.wam_utils.blocks.bookshelf;

import com.mojang.blaze3d.vertex.PoseStack;

import alec_wam.wam_utils.blocks.bookshelf.EnchantmentBookshelfBE.EnchantmentCategoryBookshelfFilter;
import alec_wam.wam_utils.client.GuiIcons;
import alec_wam.wam_utils.client.widgets.GuiIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EnchantmentFilterButton extends GuiIconButton {
	
	private EnchantmentCategoryBookshelfFilter filter;
	private Screen screen;
	public EnchantmentFilterButton(Screen screen, int x, int y, int width, int height, EnchantmentCategoryBookshelfFilter filter) {
		super(x, y, width, height, Component.empty(), filter.getIcon());
		this.filter = filter;
		this.screen = screen;
	}
	
	@Override
	public void onPress() {
		this.filter = Screen.hasShiftDown() ? filter.getPrev() : filter.getNext();
	}
	
	@Override
	public GuiIcons getIcon() {
		return this.filter.getIcon();
	}
	
	public EnchantmentCategoryBookshelfFilter getFilter() {
		return filter;
	}
	
	protected void renderTooltip(PoseStack poseStack, int mouseX, int mouseY) {
		if(this.filter !=null) {
			this.screen.renderTooltip(poseStack, filter.getTooltip(), mouseX, mouseY);
		}
	}

}
