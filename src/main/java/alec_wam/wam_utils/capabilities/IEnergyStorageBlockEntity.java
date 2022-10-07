package alec_wam.wam_utils.capabilities;

public interface IEnergyStorageBlockEntity {

	public abstract void onEnergyChanged();
	
	public abstract boolean canInsertEnergy();
	
	public abstract boolean canExtractEnergy();
	
	public abstract int getEnergyCapacity();
	
	public abstract int getMaxEnergyInput();
	
	public abstract int getMaxEnergyOutput();
	
}
