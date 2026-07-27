package com.roguesmp.item.component.impl;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.codec.Codec;
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
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PotionContentComponent implements ItemComponent {

    public static final Codec<PotionContentComponent> CODEC = Codec.composite(
            Codec.STRING.fieldOf("color").forGetter(PotionContentComponent::getColor),
            Codec.listOf(StoredEffect.CODEC).fieldOf("effects").forGetter(PotionContentComponent::getEffects),
            PotionContentComponent::new
    );

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
        this.effects = new ArrayList<>();
        this.effects.addAll(effects);
    }

    // A simple internal record to hold the data independent of Bukkit's PotionEffect
    public record StoredEffect(String effectType, int duration, int amplifier) {
        public static final Codec<StoredEffect> CODEC = Codec.composite(
                Codec.STRING.fieldOf("type").forGetter(StoredEffect::effectType),
                Codec.INT.fieldOf("duration").forGetter(StoredEffect::duration),
                Codec.INT.fieldOf("amplifier").forGetter(StoredEffect::amplifier),
                StoredEffect::new
        );
    }

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

    public String getColor() {
        return color;
    }

    public @Unmodifiable List<StoredEffect> getEffects() {
        return Collections.unmodifiableList(effects);
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
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
