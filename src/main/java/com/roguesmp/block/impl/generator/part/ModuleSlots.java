package com.roguesmp.block.impl.generator.part;

import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.item.component.impl.GeneratorModuleComponent;
import com.roguesmp.block.impl.generator.stat.GeneratorEffect;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ModuleSlots {

    private static final MapCodec<List<String>> CODEC = Codec.listOf(Codec.STRING).optionalFieldOf("modules", List.of());

    private final List<String> items = new ArrayList<>();
    private final int capacity;

    public ModuleSlots(int capacity) {
        this.capacity = capacity;
    }

    public int capacity() {
        return capacity;
    }

    public List<String> items() {
        return List.copyOf(items);
    }

    public ModuleInstallResult install(String itemId) {
        GeneratorModuleComponent module = GeneratorModuleComponent.of(itemId);
        if (module == null) return ModuleInstallResult.NOT_A_MODULE;
        if (items.size() >= capacity) return ModuleInstallResult.NO_FREE_SLOT;

        int duplicates = 0;
        for (String installedId : items) {
            if (module.incompatible().contains(installedId)) return ModuleInstallResult.INCOMPATIBLE;

            GeneratorModuleComponent installed = GeneratorModuleComponent.of(installedId);
            if (installed != null && installed.incompatible().contains(itemId)) return ModuleInstallResult.INCOMPATIBLE;
            if (installedId.equals(itemId)) duplicates++;
        }
        if (module.maxPerMachine() > 0 && duplicates >= module.maxPerMachine()) return ModuleInstallResult.LIMIT_REACHED;

        items.add(itemId);
        return ModuleInstallResult.INSTALLED;
    }

    public @Nullable String remove(int index) {
        return index < 0 || index >= items.size() ? null : items.remove(index);
    }

    public List<String> drain() {
        List<String> drained = List.copyOf(items);
        items.clear();
        return drained;
    }

    public void collectEffects(@Nullable String burningItemId, List<GeneratorEffect> out) {
        for (String itemId : items) {
            GeneratorModuleComponent module = GeneratorModuleComponent.of(itemId);
            if (module == null) continue;

            out.add(module.effect());
            if (burningItemId == null) continue;
            module.synergies().stream()
                    .filter(synergy -> synergy.appliesTo(burningItemId))
                    .forEach(synergy -> out.add(synergy.effect()));
        }
    }

    public StateSection<List<String>> section() {
        return StateSection.of(CODEC, () -> items, value -> {
            items.clear();
            items.addAll(value);
        });
    }
}
