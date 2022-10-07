package alec_wam.wam_utils.blocks.machine;

public enum BonemealMode {

	OFF, NORMAL, ADVANCED;
	
	public BonemealMode getNext() {
		return BonemealMode.values()[(this.ordinal() + 1) % (BonemealMode.values().length)];
	}
	
	public static BonemealMode getMode(int index) {
		return BonemealMode.values()[index % (BonemealMode.values().length)];
	}
	
}
