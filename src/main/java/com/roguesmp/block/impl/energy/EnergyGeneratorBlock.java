package com.roguesmp.block.impl.energy;

import com.roguesmp.block.BlockProperties;
import com.roguesmp.block.EnergyStorage;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.StoredItem;
import com.roguesmp.block.Tickable;
import com.roguesmp.block.data.EnergyGeneratorData;
import com.roguesmp.block.event.SmpBlockBreakEvent;
import com.roguesmp.block.gui.EnergyGeneratorGui;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.block.storage.FixedSlots;
import com.roguesmp.block.storage.ItemStorage;
import com.roguesmp.block.storage.ItemStorageHolder;
import com.roguesmp.block.storage.SlotAccess;
import com.roguesmp.block.transfer.EnergyTransfer;
import com.roguesmp.codec.Codec;
import com.roguesmp.item.component.impl.EnergySourceComponent;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnergyGeneratorBlock extends SmpBlock implements Tickable, EnergyStorage, ItemStorageHolder {

    private static final int GUI_REFRESH_INTERVAL = 10;

    private final EnergyGeneratorData data;
    private final ItemStorage storage = new ItemStorage(this);
    private final FixedSlots fuelSlot = storage.add(new FixedSlots("fuel", "Nhiên liệu", 1, SlotAccess.INSERT, item -> EnergySourceComponent.of(item.item()) != null));
    private int energy;
    private int burnTicksLeft;
    private int burnTicksTotal;
    private int burnEnergyPerTick;
    private @Nullable EnergyGeneratorGui gui;
    private int guiRefreshTicks;

    public EnergyGeneratorBlock(BlockProperties properties, EnergyGeneratorData data) {
        super(properties);
        this.data = data;
    }

    @Override
    public void tick() {
        if (burnTicksLeft <= 0) ignite();
        if (burnTicksLeft > 0) burn();
        EnergyTransfer.distribute(this, getLocation().getBlock(), data.transferRate());
        tickGuiRefresh();
    }

    private void ignite() {
        if (energy >= data.maxEnergy()) return;

        StoredItem fuel = fuelSlot.get(0);
        EnergySourceComponent source = fuel == null ? null : EnergySourceComponent.of(fuel.item());
        if (source == null || source.burnTicks() <= 0) return;

        fuelSlot.extract(0, 1, false);
        burnTicksLeft = burnTicksTotal = source.burnTicks();
        burnEnergyPerTick = source.energyPerTick();
        markDirty();
    }

    private void burn() {
        if (energy >= data.maxEnergy()) return;

        addEnergy(burnEnergyPerTick);
        burnTicksLeft--;
        markDirty();
    }

    private void tickGuiRefresh() {
        if (gui == null) return;
        if (++guiRefreshTicks < GUI_REFRESH_INTERVAL) return;

        guiRefreshTicks = 0;
        gui.refresh();
    }

    public FixedSlots fuelSlot() {
        return fuelSlot;
    }

    public boolean isBurning() {
        return burnTicksLeft > 0;
    }

    public int burnTicksLeft() {
        return burnTicksLeft;
    }

    public int burnTicksTotal() {
        return burnTicksTotal;
    }

    public int burnEnergyPerTick() {
        return burnEnergyPerTick;
    }

    public int transferRate() {
        return data.transferRate();
    }

    @Override
    public int getEnergy() {
        return energy;
    }

    @Override
    public int getMaxEnergy() {
        return data.maxEnergy();
    }

    @Override
    public int addEnergy(int amount) {
        int accepted = Math.min(amount, data.maxEnergy() - energy);
        energy += accepted;
        if (accepted > 0) markDirty();
        return accepted;
    }

    @Override
    public int removeEnergy(int amount) {
        int removed = Math.min(amount, energy);
        energy -= removed;
        if (removed > 0) markDirty();
        return removed;
    }

    @Override
    public boolean canReceiveEnergy() {
        return false;
    }

    @Override
    public ItemStorage itemStorage() {
        return storage;
    }

    @Override
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        event.setCancelled(true);
        openGui(event.getPlayer());
    }

    public void openGui(HumanEntity viewer) {
        if (gui == null) gui = new EnergyGeneratorGui(this);
        gui.showInventory(viewer);
    }

    public void releaseGui(EnergyGeneratorGui closed) {
        if (gui == closed) gui = null;
    }

    @Override
    public void onBlockBreak(SmpBlockBreakEvent event) {
        if (gui != null) gui.closeAll();
    }

    @Override
    protected void collectSections(List<StateSection<?>> sections) {
        super.collectSections(sections);
        sections.add(StateSection.of(Codec.INT.optionalFieldOf("energy", 0), () -> energy, value -> energy = value));
        sections.add(StateSection.of(Codec.INT.optionalFieldOf("burn_left", 0), () -> burnTicksLeft, value -> burnTicksLeft = value));
        sections.add(StateSection.of(Codec.INT.optionalFieldOf("burn_total", 0), () -> burnTicksTotal, value -> burnTicksTotal = value));
        sections.add(StateSection.of(Codec.INT.optionalFieldOf("burn_rate", 0), () -> burnEnergyPerTick, value -> burnEnergyPerTick = value));
    }
}
