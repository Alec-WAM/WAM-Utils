package alec_wam.wam_utils.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.energy.IEnergyStorage;

public class BlockEnergyStorage implements IEnergyStorage, INBTSerializable<CompoundTag> {
    private static final String KEY = "energy";
    private int energy;
    private IEnergyStorageBlockEntity blockEntity;

    public BlockEnergyStorage(IEnergyStorageBlockEntity blockEntity, int energy) {
        this.energy = energy;
        this.blockEntity = blockEntity;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(KEY, this.energy);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
    	//TODO Possibly min/max this with max energy storage to prevent overflow
        this.energy = nbt.getInt(KEY);
    }

    //Force Addition of energy
    public int addEnergy(int maxReceive, boolean simulate) {
    	int energyReceived = Math.min(getMaxEnergyStored() - energy, maxReceive);

        if (!simulate) {
            energy += energyReceived;
            blockEntity.onEnergyChanged();
        }

        return energyReceived;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
    	if(!blockEntity.canInsertEnergy()) {
    		return 0;
    	}
    	
        int energyReceived = Math.min(getMaxEnergyStored() - energy, Math.min(blockEntity.getMaxEnergyInput(), maxReceive));

        if (!simulate) {
            energy += energyReceived;
            blockEntity.onEnergyChanged();
        }

        return energyReceived;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    //Force Removal of energy
    public int consumeEnergy(int maxExtract, boolean simulate) {
        int energyExtracted = Math.min(energy, maxExtract);

        if (!simulate) {
            energy -= energyExtracted;
            blockEntity.onEnergyChanged();
        }

        return energyExtracted;
    }
    
    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
    	if(!blockEntity.canExtractEnergy()) {
    		return 0;
    	}
    	int energyExtracted = Math.min(energy, Math.min(blockEntity.getMaxEnergyOutput(), maxExtract));

        if (!simulate) {
            energy -= energyExtracted;
            blockEntity.onEnergyChanged();
        }

        return energyExtracted;
    }

    @Override
    public int getEnergyStored() {
        return this.energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return blockEntity.getEnergyCapacity();
    }

    @Override
    public boolean canExtract() {
        return blockEntity.canExtractEnergy();
    }

    @Override
    public boolean canReceive() {
        return blockEntity.canInsertEnergy();
    }

    @Override
    public String toString() {
        return "EnergyStorage{" +
                "energy=" + energy +
                ", capacity=" + blockEntity.getEnergyCapacity() +
                ", maxIn=" + blockEntity.getMaxEnergyInput() +
                ", maxOut=" + blockEntity.getMaxEnergyOutput() +
                '}';
    }
}
