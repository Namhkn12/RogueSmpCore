package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.context.ItemDataContext;
import com.roguesmp.item.component.ItemComponent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemArmorTrim;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Keyed;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jetbrains.annotations.NotNull;

public record ArmorTrimComponent(Key material, Key pattern) implements ItemComponent {

    public static final Codec<ArmorTrimComponent> CODEC = Codec.composite(
            registryKey(RegistryKey.TRIM_MATERIAL, "trim material").fieldOf("material").forGetter(ArmorTrimComponent::material),
            registryKey(RegistryKey.TRIM_PATTERN, "trim pattern").fieldOf("pattern").forGetter(ArmorTrimComponent::pattern),
            ArmorTrimComponent::new
    );

    private static <T extends Keyed> Codec<Key> registryKey(RegistryKey<@NotNull T> registry, String label) {
        return Codec.KEY.comapFlatMap(
                key -> RegistryAccess.registryAccess().getRegistry(registry).get(key) != null
                        ? DataResult.success(key)
                        : DataResult.error("Unknown " + label + ": " + key.asString()),
                key -> key
        );
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void modifyStack(ItemDataContext context) {
        TrimMaterial trimMaterial = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_MATERIAL).get(material);
        TrimPattern trimPattern = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN).get(pattern);
        if (trimMaterial == null || trimPattern == null) return;

        context.newStack().setData(DataComponentTypes.TRIM, ItemArmorTrim.itemArmorTrim(new ArmorTrim(trimMaterial, trimPattern)));
    }
}
