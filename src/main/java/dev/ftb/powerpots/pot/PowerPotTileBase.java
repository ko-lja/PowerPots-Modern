package dev.ftb.powerpots.pot;

import net.darkhax.botanypots.BotanyPotHelper;
import net.darkhax.botanypots.block.BlockEntityBotanyPot;
import net.darkhax.botanypots.block.inv.BotanyPotContainer;
import net.darkhax.botanypots.data.recipes.crop.Crop;
import net.darkhax.botanypots.data.recipes.soil.Soil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * A hopper botany pot that only grows while it is powered.
 * Soil, crop and output storage live in the Botany Pots container.
 */
public class PowerPotTileBase extends BlockEntityBotanyPot {
    /**
     * How long the pot keeps reporting itself as powered after the last growth tick. Smooths out
     * flickering energy input so the client isnt resynced every tick.
     */
    private static final int POWER_SYNC_GRACE = 20;

    private final PotTier tier;
    private final PotEnergyStorage energy;
    private LazyOptional<IEnergyStorage> energyLazy;
    /**
     * Pipes may only pull from the output slots, never insert or take the soil and crop.
     */
    private LazyOptional<IItemHandler> outputLazy;

    private final Random rng = new Random();

    /**
     * Whether the pot is currently growing on the server. Synced so the client can animate the growth.
     */
    private boolean powered;
    private int idleTicks = POWER_SYNC_GRACE;

    public PowerPotTileBase(BlockEntityType<?> type, BlockPos pos, BlockState state, PotTier tier) {
        super(type, pos, state);

        this.tier = tier;
        this.energy = new PotEnergyStorage(this, tier.config.maxEnergy.get());
        this.energyLazy = LazyOptional.of(() -> this.energy);
        this.outputLazy = LazyOptional.of(() -> new SidedInvWrapper(this, Direction.DOWN));
    }

    public PotTier getTier() {
        return this.tier;
    }

    public PotEnergyStorage getEnergy() {
        return this.energy;
    }

    @Override
    public BotanyPotContainer createInventory() {
        // Called from the super constructor, so the tier must only be read lazily.
        return new BotanyPotContainer(this) {
            @Override
            public int getRequiredGrowthTime() {
                int requiredGrowthTicks = super.getRequiredGrowthTime();
                if (requiredGrowthTicks <= 0 || !(this.getPotEntity() instanceof PowerPotTileBase pot)) {
                    return requiredGrowthTicks;
                }

                return Math.max(1, requiredGrowthTicks - (int) (requiredGrowthTicks * pot.tier.config.speedModifier.get()));
            }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PowerPotTileBase pot) {
        if (pot.isRemoved()) {
            return;
        }

        BotanyPotContainer inventory = pot.getInventory();
        inventory.update();

        Soil soil = pot.getSoil();
        Crop crop = pot.getCrop();
        if (soil != null) {
            soil.onTick(level, pos, pot);
        }

        if (crop != null) {
            crop.onTick(level, pos, pot);
        }

        boolean canGrow = soil != null && crop != null && pot.areGrowthConditionsMet();

        if (level.isClientSide) {
            if (canGrow && pot.powered && !pot.doneGrowing && pot.growthTime < inventory.getRequiredGrowthTime()) {
                pot.growthTime++;
            }

            return;
        }

        if (!pot.isStorageEmpty()) {
            pot.exportToBelow();
        }

        boolean grew = false;
        if (canGrow) {
            if (pot.doneGrowing) {
                if (pot.harvestDelay > 0) {
                    pot.harvestDelay--;
                } else if (pot.harvestToStorage()) {
                    pot.resetGrowth();
                } else {
                    // Storage is full, retry in a bit
                    pot.harvestDelay = 5;
                }
            } else {
                int cost = pot.tier.config.perTickEnergy.get();
                if (pot.hasStorageSpace() && pot.energy.consumeEnergy(cost, true) >= cost) {
                    pot.energy.consumeEnergy(cost, false);
                    grew = true;

                    pot.growthTime++;
                    soil.onGrowthTick(level, pos, pot, crop);
                    crop.onGrowthTick(level, pos, pot, soil);

                    int requiredGrowthTicks = inventory.getRequiredGrowthTime();
                    pot.comparatorLevel = Mth.floor(15f * ((float) pot.growthTime / requiredGrowthTicks));

                    if (pot.growthTime >= requiredGrowthTicks) {
                        pot.doneGrowing = true;
                        pot.markDirty();
                    }
                }
            }
        } else if (pot.growthTime != -1 || pot.doneGrowing || pot.comparatorLevel != 0) {
            pot.resetGrowth();
        }

        pot.idleTicks = grew ? 0 : Math.min(pot.idleTicks + 1, POWER_SYNC_GRACE);
        boolean powered = pot.idleTicks < POWER_SYNC_GRACE;
        if (pot.powered != powered) {
            pot.powered = powered;
            pot.markDirty();
        }

        if (pot.comparatorLevel != pot.prevComparatorLevel) {
            pot.prevComparatorLevel = pot.comparatorLevel;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    private boolean isStorageEmpty() {
        for (int slot : BotanyPotContainer.STORAGE_SLOT) {
            if (!this.getInventory().getItem(slot).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private boolean hasStorageSpace() {
        for (int slot : BotanyPotContainer.STORAGE_SLOT) {
            if (this.getInventory().getItem(slot).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Puts the drops of the crop into the output slots. Each drop is replaced by the configured amount of
     * items for the tier, whatever does not fit is voided.
     *
     * @return Whether anything was stored.
     */
    private boolean harvestToStorage() {
        Crop crop = this.getCrop();
        if (crop == null) {
            return false;
        }

        BotanyPotContainer inventory = this.getInventory();
        boolean didCollect = false;

        for (ItemStack drop : BotanyPotHelper.generateDrop(this.rng, this.level, this.worldPosition, this, crop)) {
            if (drop.isEmpty()) {
                continue;
            }

            int remaining = this.tier.config.itemsPerOutput.get();
            int limit = Math.min(drop.getMaxStackSize(), inventory.getMaxStackSize());

            for (int slot : BotanyPotContainer.STORAGE_SLOT) {
                if (remaining <= 0) {
                    break;
                }

                ItemStack existing = inventory.getItem(slot);
                if (existing.isEmpty()) {
                    int moved = Math.min(remaining, limit);
                    inventory.setItem(slot, drop.copyWithCount(moved));
                    remaining -= moved;
                    didCollect = true;
                } else if (existing.getCount() < limit && ItemStack.isSameItemSameTags(existing, drop)) {
                    int moved = Math.min(remaining, limit - existing.getCount());
                    inventory.setItem(slot, existing.copyWithCount(existing.getCount() + moved));
                    remaining -= moved;
                    didCollect = true;
                }
            }
        }

        return didCollect;
    }

    private void exportToBelow() {
        BlockEntity below = this.level.getBlockEntity(this.worldPosition.below());
        if (below == null) {
            return;
        }

        below.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).ifPresent(target -> {
            BotanyPotContainer inventory = this.getInventory();
            for (int slot : BotanyPotContainer.STORAGE_SLOT) {
                ItemStack stack = inventory.getItem(slot);
                if (stack.isEmpty()) {
                    continue;
                }

                ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, stack.copy(), false);
                if (remainder.getCount() != stack.getCount()) {
                    inventory.setItem(slot, remainder);
                }
            }
        });
    }

    void onEnergyChanged() {
        if (this.level != null) {
            this.level.blockEntityChanged(this.worldPosition);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.energy.deserializeNBT(tag.getCompound("Energy"));
        this.powered = tag.getBoolean("Powered");
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Energy", this.energy.serializeNBT());
        tag.putBoolean("Powered", this.powered);
    }

    @Override
    protected Component getDefaultName() {
        return this.getBlockState().getBlock().getName();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (!this.isRemoved()) {
            if (cap == ForgeCapabilities.ENERGY) {
                return this.energyLazy.cast();
            }

            if (cap == ForgeCapabilities.ITEM_HANDLER) {
                return this.outputLazy.cast();
            }
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.energyLazy.invalidate();
        this.outputLazy.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        this.energyLazy = LazyOptional.of(() -> this.energy);
        this.outputLazy = LazyOptional.of(() -> new SidedInvWrapper(this, Direction.DOWN));
    }
}
