package com.roguesmp.block.impl.generator.part;

import com.roguesmp.block.EnergyStorage;
import com.roguesmp.block.StoredItem;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.block.storage.FixedSlots;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.item.component.impl.GeneratorFuelComponent;
import com.roguesmp.block.impl.generator.stat.GeneratorEffect;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class GeneratorFuel {

    private static final MapCodec<Optional<ActiveBurn>> BURN_CODEC = ActiveBurn.CODEC.optionalFieldOf("burn");

    private final EnergyStorage energy;
    private final FixedSlots fuelSlot;
    private final Runnable onEffectsChanged;
    private final Runnable onStateChanged;

    private @Nullable String slotComponentItem;
    private @Nullable GeneratorFuelComponent slotComponent;
    private @Nullable ActiveBurn burn;
    private @Nullable GeneratorFuelComponent burningComponent;

    public GeneratorFuel(EnergyStorage energy, FixedSlots fuelSlot, Runnable onEffectsChanged, Runnable onStateChanged) {
        this.energy = energy;
        this.fuelSlot = fuelSlot;
        this.onEffectsChanged = onEffectsChanged;
        this.onStateChanged = onStateChanged;
    }

    public @Nullable StoredItem slot() {
        return fuelSlot.get(0);
    }

    public @Nullable ActiveBurn burn() {
        return burn;
    }

    public @Nullable GeneratorFuelComponent burningComponent() {
        return burningComponent;
    }

    public @Nullable String burningItem() {
        return burn == null ? null : burn.item();
    }

    public void resolve() {
        burningComponent = burn == null ? null : GeneratorFuelComponent.of(burn.item());
        if (burningComponent == null) burn = null;
    }

    public void collectEffects(List<GeneratorEffect> out) {
        if (burningComponent != null) out.add(burningComponent.effect());
    }

    public void consume() {
        StoredItem slot = slot();
        if (slot == null) return;

        GeneratorFuelComponent component = slotComponent(slot);
        if (component == null) return;

        int consumed = Math.min(slot.amount(), consumableCount(slot, component));
        if (consumed <= 0) return;

        energy.addEnergy(consumed * component.energy());
        startBurn(slot.item(), component, consumed);
        fuelSlot.extract(0, consumed, false);
        onStateChanged.run();
    }

    public void tick() {
        if (energy.getEnergy() > 0 && burningComponent != null && burningComponent.unit() == BurnUnit.TICKS) advance();
    }

    public void onHarvest() {
        if (burningComponent != null && burningComponent.unit() == BurnUnit.HARVESTS) advance();
    }

    public StateSection<Optional<ActiveBurn>> burnSection() {
        return StateSection.of(BURN_CODEC, () -> Optional.ofNullable(burn), value -> burn = value.orElse(null));
    }

    private void advance() {
        if (burn.advance()) {
            burn = null;
            onEffectsChanged.run();
        }
        onStateChanged.run();
    }

    private @Nullable GeneratorFuelComponent slotComponent(StoredItem slot) {
        if (!slot.item().equals(slotComponentItem)) {
            slotComponentItem = slot.item();
            slotComponent = GeneratorFuelComponent.of(slotComponentItem);
        }
        return slotComponent;
    }

    private int consumableCount(StoredItem slot, GeneratorFuelComponent component) {
        if (component.energy() > 0) return (energy.getMaxEnergy() - energy.getEnergy()) / component.energy();
        return component.hasEffect() && !isBurning(slot.item()) ? 1 : 0;
    }

    private boolean isBurning(String itemId) {
        return burn != null && burn.item().equals(itemId);
    }

    private void startBurn(String item, GeneratorFuelComponent component, int count) {
        if (!component.hasEffect()) return;

        int duration = component.duration() * count;
        if (isBurning(item)) {
            burn.extend(duration);
            return;
        }
        burn = new ActiveBurn(item, duration);
        onEffectsChanged.run();
    }
}
