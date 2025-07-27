package alec_wam.wam_utils.client.screen;

import javax.annotation.Nullable;

import alec_wam.wam_utils.client.util.GuiIcons;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

public class GuiIconButton extends Button {
    
    private final GuiIcons icon;
    private static final int SPRITE_WIDTH = 16;

    public GuiIconButton(
        int x,
        int y,
        int width,
        int height,
        GuiIcons icon,        
        Button.OnPress onPress,
        @Nullable Button.CreateNarration createNarration
    ){
        super(x, y, width, height, Component.empty(), onPress, createNarration == null ? DEFAULT_NARRATION : createNarration);
        this.icon = icon;
    }

    public GuiIcons getIcon() {
        return this.icon;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float p_296191_) {
        super.renderWidget(guiGraphics, mouseX, mouseY, p_296191_);
        int i = this.getX() + this.getWidth() / 2 - SPRITE_WIDTH / 2;
        int j = this.getY() + this.getHeight() / 2 - SPRITE_WIDTH / 2;
        if(this.getIcon() !=null){
            this.getIcon().renderIcon(guiGraphics, i + 1, j + 1, getWidth() - 2, getHeight() - 2, ARGB.color(this.alpha, -1));
        }
        if (this.isHovered) {
			this.renderTooltip(guiGraphics, mouseX, mouseY);
		}
        // guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, i, j, this.spriteWidth, this.spriteHeight, this.alpha);
    }

    public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        
    }

    @Override
    public void renderString(GuiGraphics p_294683_, Font p_295870_, int p_295770_) {
    }

}
