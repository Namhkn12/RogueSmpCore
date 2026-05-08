package com.roguesmp.item.component.impl;

import com.roguesmp.context.ItemDataContext;
import com.roguesmp.item.component.ItemComponent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PotionContents;
import org.bukkit.Color;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PotionContentComponent implements ItemComponent {

    private final Color color;
    private final List<StoredEffect> effects;

    public PotionContentComponent(Color color, List<StoredEffect> effects) {
        this.color = color;
        this.effects = List.copyOf(effects);
    }

    // A simple internal record to hold the data independent of Bukkit's PotionEffect
    public record StoredEffect(PotionEffectType effectType, int duration, int amplifier, boolean ambient, boolean particles, boolean icon) {}

    @Override
    public @NotNull ItemComponent copy() {
        return new PotionContentComponent(color, effects);
    }

    @Override
    public void modifyStack(ItemDataContext context) {
        PotionContents.Builder builder = PotionContents.potionContents()
                .customColor(color);

        for (StoredEffect stored : effects) {
            // Map our internal fields back to the Bukkit API only at runtime
            if (stored.effectType() != null) {
                builder.addCustomEffect(new PotionEffect(
                        stored.effectType(),
                        stored.duration(),
                        stored.amplifier(),
                        stored.ambient(),
                        stored.particles(),
                        stored.icon()
                ));
            }
        }

        context.newStack().setData(DataComponentTypes.POTION_CONTENTS, builder.build());
    }
}
