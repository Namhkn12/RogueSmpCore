package com.roguesmp.item.component.impl;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.constant.Keys;
import com.roguesmp.item.component.ItemComponent;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class DurabilityComponent implements ItemComponent {

    public static final NamespacedKey DURABILITY_KEY = new NamespacedKey(Keys.GLOBAL_NAMESPACE, "durability");

    private final int maxDurability;

    @GsonIgnore
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
            this.currentDurability = 1;
        } else this.currentDurability = currentDurability;
    }

    @Override
    public void save(PersistentDataContainer pdc) {
        pdc.set(DURABILITY_KEY, PersistentDataType.INTEGER, currentDurability);
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

    public boolean isAboutToBreak() {
        return currentDurability < (maxDurability / 100) * 2 && currentDurability > (maxDurability / 100);
    }
}
