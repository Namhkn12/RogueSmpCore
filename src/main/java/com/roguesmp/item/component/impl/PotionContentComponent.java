package com.roguesmp.item.component.impl;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.context.ItemDataContext;
import com.roguesmp.item.component.ItemComponent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PotionContents;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import org.bukkit.Color;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PotionContentComponent implements ItemComponent {

    @GsonIgnore
    private Color bukkitColor;

    private final String color;
    private final List<StoredEffect> effects;

    public PotionContentComponent(String color, List<StoredEffect> effects) {
        this.color = color;
        String[] parts = color.split(",");
        this.bukkitColor = Color.fromARGB(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3])
        );
        this.effects = List.copyOf(effects);
    }

    // A simple internal record to hold the data independent of Bukkit's PotionEffect
    public record StoredEffect(String effectType, int duration, int amplifier) {}

    public Color getBukkitColor() {
        if (bukkitColor == null) {
            String[] parts = color.split(",");
            this.bukkitColor = Color.fromARGB(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3])
            );
            return this.bukkitColor;
        }
        return bukkitColor;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new PotionContentComponent(color, effects);
    }

    @Override
    public void modifyStack(ItemDataContext context) {
        PotionContents.Builder builder = PotionContents.potionContents()
                .customColor(getBukkitColor());
        if (effects != null) {
            for (StoredEffect stored : effects) {
                if (stored.effectType() != null) {
                    PotionEffectType type = getPotionEffectType(stored.effectType);
                    builder.addCustomEffect(new PotionEffect(
                            type,
                            stored.duration(),
                            stored.amplifier(),
                            true,
                            true,
                            true
                    ));
                }
            }
        }


        context.newStack().setData(DataComponentTypes.POTION_CONTENTS, builder.build());
    }

    private static PotionEffectType getPotionEffectType(@NotNull @KeyPattern.Value String key) {
        return Registry.MOB_EFFECT.getOrThrow(Key.key(Key.MINECRAFT_NAMESPACE, key));
    }
}
