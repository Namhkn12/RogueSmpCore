package com.roguesmp.block.impl.generator.part;

import com.roguesmp.block.EnergyStorage;
import com.roguesmp.block.StoredItem;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.block.storage.FixedSlots;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.item.component.impl.GeneratorCatalystComponent;
import com.roguesmp.block.impl.generator.stat.GeneratorEffect;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class GeneratorCatalyst {

    private static final MapCodec<Optional<ActiveCatalyst>> ACTIVE_CODEC = ActiveCatalyst.CODEC.optionalFieldOf("active_catalyst");

    private final EnergyStorage energy;
    private final FixedSlots catalystSlot;
    private final Runnable onEffectsChanged;
    private final Runnable onStateChanged;

    private @Nullable String slotComponentItem;
    private @Nullable GeneratorCatalystComponent slotComponent;
    private @Nullable ActiveCatalyst active;
    private @Nullable GeneratorCatalystComponent activeComponent;

    public GeneratorCatalyst(EnergyStorage energy, FixedSlots catalystSlot, Runnable onEffectsChanged, Runnable onStateChanged) {
        this.energy = energy;
        this.catalystSlot = catalystSlot;
        this.onEffectsChanged = onEffectsChanged;
        this.onStateChanged = onStateChanged;
    }

    public @Nullable StoredItem slot() {
        return catalystSlot.get(0);
    }

    public @Nullable ActiveCatalyst active() {
        return active;
    }

    public @Nullable GeneratorCatalystComponent activeComponent() {
        return activeComponent;
    }

    public @Nullable String activeItem() {
        return active == null ? null : active.item();
    }

    public void resolve() {
        activeComponent = active == null ? null : GeneratorCatalystComponent.of(active.item());
        if (activeComponent == null) active = null;
    }

    public void collectEffects(List<GeneratorEffect> out) {
        if (activeComponent != null) out.add(activeComponent.effect());
    }

    public void consume() {
        if (active != null) return;

        StoredItem slot = slot();
        if (slot == null) return;

        GeneratorCatalystComponent component = slotComponent(slot);
        if (component == null) return;

        active = new ActiveCatalyst(slot.item(), component.duration());
        catalystSlot.extract(0, 1, false);
        onEffectsChanged.run();
        onStateChanged.run();
    }

    public void tick() {
        if (energy.getEnergy() > 0 && activeComponent != null && activeComponent.unit() == DurationUnit.TICKS) advance();
    }

    public void onHarvest() {
        if (activeComponent != null && activeComponent.unit() == DurationUnit.HARVESTS) advance();
    }

    public StateSection<Optional<ActiveCatalyst>> activeSection() {
        return StateSection.of(ACTIVE_CODEC, () -> Optional.ofNullable(active), value -> active = value.orElse(null));
    }

    private void advance() {
        if (active.advance()) {
            active = null;
            onEffectsChanged.run();
        }
        onStateChanged.run();
    }

    private @Nullable GeneratorCatalystComponent slotComponent(StoredItem slot) {
        if (!slot.item().equals(slotComponentItem)) {
            slotComponentItem = slot.item();
            slotComponent = GeneratorCatalystComponent.of(slotComponentItem);
        }
        return slotComponent;
    }
}
