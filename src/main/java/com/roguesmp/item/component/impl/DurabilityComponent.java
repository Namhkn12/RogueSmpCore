package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.Keys;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.UniqueTrackingComponent;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class DurabilityComponent implements UniqueTrackingComponent {

    public static final NamespacedKey DURABILITY_KEY = new NamespacedKey(Keys.GLOBAL_NAMESPACE, "durability");

    public static final Codec<DurabilityComponent> CODEC = Codec.INT.xmap(DurabilityComponent::new, DurabilityComponent::maxDurability);

    private final int maxDurability;

    private int currentDurability;

    public DurabilityComponent(int maxDurability) {
        this.maxDurability = maxDurability;
        this.currentDurability = maxDurability;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new DurabilityComponent(maxDurability);
    }

    @Override
    public void load(PersistentDataContainerView pdc) {
        Integer currentDurability = pdc.get(DURABILITY_KEY, PersistentDataType.INTEGER);
        if (currentDurability == null) {
            this.currentDurability = maxDurability;
        } else this.currentDurability = currentDurability;
    }

    @Override
    public void save(PersistentDataContainer pdc) {
        pdc.set(DURABILITY_KEY, PersistentDataType.INTEGER, currentDurability);
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        double percent = (double) currentDurability / maxDurability;
        NamedTextColor dynamicColor;

        if (percent >= 0.75) {
            dynamicColor = NamedTextColor.GREEN;
        } else if (percent >= 0.40) {
            dynamicColor = NamedTextColor.YELLOW;
        } else if (percent >= 0.15) {
            dynamicColor = NamedTextColor.GOLD;
        } else {
            dynamicColor = NamedTextColor.RED;
        }

        Component loreComponent = Component.text()
                .content("Độ bền: ")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(currentDurability, dynamicColor))
                .append(Component.text("/", NamedTextColor.DARK_GRAY))
                .append(Component.text(maxDurability, NamedTextColor.GRAY))
                .build();

        context.builder().putLines(110, List.of(Component.empty(), loreComponent));
    }

    public int maxDurability() {
        return maxDurability;
    }

    public int currentDurability() {
        return currentDurability;
    }

    public void setCurrentDurability(int newCurrent) {
        currentDurability = newCurrent;
    }
}
