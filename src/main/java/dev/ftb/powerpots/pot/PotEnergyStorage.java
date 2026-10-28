package dev.ftb.powerpots.pot;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.energy.IEnergyStorage;

public class PotEnergyStorage implements IEnergyStorage, INBTSerializable<CompoundTag> {
    private static final String KEY = "energy";
    private int energy;
    private final int capacity;
    private final int maxInOut;
    private final PowerPotTileBase tile;

    public PotEnergyStorage(PowerPotTileBase tile, int capacity) {
        this.energy = 0;
        this.capacity = capacity;
        this.maxInOut = capacity;
        this.tile = tile;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(KEY, this.energy);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.energy = Math.min(nbt.getInt(KEY), this.capacity);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int energyReceived = Math.min(capacity - energy, Math.min(this.maxInOut, maxReceive));

        if (!simulate && energyReceived > 0) {
            energy += energyReceived;
            tile.onEnergyChanged();
        }

        return energyReceived;
    }

    public int consumeEnergy(int maxConsume, boolean simulate) {
        int energyConsumed = Math.min(energy, maxConsume);

        if (!simulate && energyConsumed > 0) {
            energy -= energyConsumed;
            tile.onEnergyChanged();
        }

        return energyConsumed;
    }

    // We don't use this method and thus we don't let other people use it either
    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return this.energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return this.capacity;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    @Override
    public String toString() {
        return "PotEnergyStorage{" +
                "energy=" + energy +
                ", capacity=" + capacity +
                ", maxInOut=" + maxInOut +
                '}';
    }
}
