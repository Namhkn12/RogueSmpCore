package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.utils.ItemRef;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DurabilityRepairComponent implements ItemComponent {

    public record RepairMaterial(String item, double percent) {
        public static final Codec<RepairMaterial> CODEC = Codec.composite(
                Codec.STRING.fieldOf("item").forGetter(RepairMaterial::item),
                Codec.DOUBLE.fieldOf("percent").forGetter(RepairMaterial::percent),
                RepairMaterial::new
        );
    }

    public static final Codec<DurabilityRepairComponent> CODEC = Codec.listOf(RepairMaterial.CODEC)
            .xmap(DurabilityRepairComponent::new, DurabilityRepairComponent::getMaterials);

    private final List<RepairMaterial> materials;

    public DurabilityRepairComponent(List<RepairMaterial> materials) {
        this.materials = List.copyOf(materials);
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    public @Nullable RepairMaterial find(@Nullable String itemId) {
        if (itemId == null) return null;
        for (RepairMaterial material : materials) {
            if (material.item().equals(itemId)) return material;
        }
        return null;
    }

    public List<RepairMaterial> getMaterials() {
        return materials;
    }
}
