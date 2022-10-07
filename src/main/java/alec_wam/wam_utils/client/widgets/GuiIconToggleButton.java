package alec_wam.wam_utils.client.widgets;

import alec_wam.wam_utils.client.GuiIcons;
import net.minecraft.network.chat.Component;

public class GuiIconToggleButton extends GuiIconButton {

	private boolean isSelected = false;
	private GuiIcons iconOff, iconOn;
	
	public GuiIconToggleButton(int x, int y, int width, int height, GuiIcons iconOff, GuiIcons iconOn, boolean selected) {
		this(x, y, width, height, Component.empty(), iconOff, iconOn, selected);
	}
	
	public GuiIconToggleButton(int x, int y, int width, int height, Component component, GuiIcons iconOff, GuiIcons iconOn, boolean selected) {
		super(x, y, width, height, component, iconOff);
		this.isSelected = selected;
		this.iconOff = iconOff;
		this.iconOn = iconOn;
	}
	
	@Override
	public void onPress() {
		final boolean oldSelected = this.isSelected;
		this.isSelected = !oldSelected;
	}
	
	public boolean isSelected() {
		return this.isSelected;
	}
	
	@Override
	public GuiIcons getIcon() {
		return isSelected ? iconOn : iconOff;
	}

}
