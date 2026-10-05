package com.roguesmp.block.impl.generator.part;

import com.roguesmp.block.EnergyStorage;
import com.roguesmp.block.StoredItem;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.item.component.impl.GeneratorFuelComponent;
import com.roguesmp.block.impl.generator.stat.GeneratorEffect;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class GeneratorFuel {

    private static final MapCodec<Optional<StoredItem>> SLOT_CODEC = StoredItem.CODEC.optionalFieldOf("fuel");
    private static final MapCodec<Optional<ActiveBurn>> BURN_CODEC = ActiveBurn.CODEC.optionalFieldOf("burn");

    private final EnergyStorage energy;
    private final Runnable onEffectsChanged;
    private final Runnable onStateChanged;

    private @Nullable StoredItem slot;
    private @Nullable String slotComponentItem;
    private @Nullable GeneratorFuelComponent slotComponent;
    private @Nullable ActiveBurn burn;
    private @Nullable GeneratorFuelComponent burningComponent;

    public GeneratorFuel(EnergyStorage energy, Runnable onEffectsChanged, Runnable onStateChanged) {
        this.energy = energy;
        this.onEffectsChanged = onEffectsChanged;
        this.onStateChanged = onStateChanged;
    }

    public @Nullable StoredItem slot() {
        return slot;
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

    public void setSlot(@Nullable StoredItem item) {
        slot = item;
        consume();
        onStateChanged.run();
    }

    public @Nullable StoredItem drainSlot() {
        StoredItem drained = slot;
        slot = null;
        return drained;
    }

    public void resolve() {
        burningComponent = burn == null ? null : GeneratorFuelComponent.of(burn.item());
        if (burningComponent == null) burn = null;
    }

    public void collectEffects(List<GeneratorEffect> out) {
        if (burningComponent != null) out.add(burningComponent.effect());
    }

    public void consume() {
        if (slot == null) return;

        GeneratorFuelComponent component = slotComponent();
        if (component == null) return;

        int consumed = Math.min(slot.amount(), consumableCount(component));
        if (consumed <= 0) return;

        energy.addEnergy(consumed * component.energy());
        startBurn(component, consumed);
        int remaining = slot.amount() - consumed;
        slot = remaining > 0 ? slot.withAmount(remaining) : null;
        onStateChanged.run();
    }

    public void tick() {
        if (energy.getEnergy() > 0 && burningComponent != null && burningComponent.unit() == BurnUnit.TICKS) advance();
    }

    public void onHarvest() {
        if (burningComponent != null && burningComponent.unit() == BurnUnit.HARVESTS) advance();
    }

    public StateSection<Optional<StoredItem>> slotSection() {
        return StateSection.of(SLOT_CODEC, () -> Optional.ofNullable(slot), value -> slot = value.orElse(null));
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

    private @Nullable GeneratorFuelComponent slotComponent() {
        if (!slot.item().equals(slotComponentItem)) {
            slotComponentItem = slot.item();
            slotComponent = GeneratorFuelComponent.of(slotComponentItem);
        }
        return slotComponent;
    }

    private int consumableCount(GeneratorFuelComponent component) {
        if (component.energy() > 0) return (energy.getMaxEnergy() - energy.getEnergy()) / component.energy();
        return component.hasEffect() && !isBurning(slot.item()) ? 1 : 0;
    }

    private boolean isBurning(String itemId) {
        return burn != null && burn.item().equals(itemId);
    }

    private void startBurn(GeneratorFuelComponent component, int count) {
        if (!component.hasEffect()) return;

        int duration = component.duration() * count;
        if (isBurning(slot.item())) {
            burn.extend(duration);
            return;
        }
        burn = new ActiveBurn(slot.item(), duration);
        onEffectsChanged.run();
    }
}
