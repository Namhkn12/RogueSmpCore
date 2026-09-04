package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MagicPowerComponent implements ItemComponent {

    public static final Codec<MagicPowerComponent> CODEC = Codec.INT.xmap(MagicPowerComponent::new, MagicPowerComponent::getMax);

    private final int max;

    //Current is determined by QualityComponent in ItemModifier
    private int current;
    private boolean shouldProvideLore = true; //If RandomStat exist, this will be false.

    public MagicPowerComponent(int max) {
        this.max = max;
        this.current = max;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new MagicPowerComponent(max);
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        if (!shouldProvideLore) return;
        Component mpLine = Component.text("Ma lực: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE).append(Component.text(current, NamedTextColor.AQUA));
        context.builder().putLines(1, List.of(mpLine));
    }

    public int getMax() {
        return max;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public void setShouldProvideLore(boolean shouldProvideLore) {
        this.shouldProvideLore = shouldProvideLore;
    }
}
