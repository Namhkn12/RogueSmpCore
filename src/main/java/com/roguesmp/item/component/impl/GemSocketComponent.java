package com.roguesmp.item.component.impl;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.constant.Keys;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.registry.ItemRegistry;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.persistence.ListPersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GemSocketComponent implements ItemComponent {

    private final int amount;

    @GsonIgnore
    private final List<BaseItem> activeGem = new ArrayList<>();
    @GsonIgnore
    private final List<String> appliedGem = new ArrayList<>(); //For old gem data (for removal in case we change socket count)

    public GemSocketComponent(int amount) {
        this.amount = amount;
    }

    public int getSocketCount() {
        return amount;
    }

    public boolean addGem(@NotNull BaseItem baseItem) {
        if (activeGem.size() >= amount) return false;
        if (baseItem.getComponent(ComponentKeys.GEM_DATA) == null) return false;
        activeGem.add(baseItem);
        appliedGem.add(baseItem.getId());
        return true;
    }

    public boolean removeGem(@NotNull BaseItem baseItem) {
        activeGem.remove(baseItem);
        appliedGem.remove(baseItem.getId());
        return true;
    }

    public List<BaseItem> getActiveGem() {
        return activeGem;
    }

    @Override
    public void load(PersistentDataContainerView pdc) {
        List<String> appliedGemIds = pdc.get(Keys.APPLIED_GEM, ListPersistentDataType.LIST.strings());
        if (appliedGemIds == null) return;
        appliedGemIds.forEach(s -> {
             BaseItem baseItem = ItemRegistry.getInstance().getBaseItem(s);
             if (baseItem == null) return;
             addGem(baseItem);
        });
    }

    @Override
    public void save(PersistentDataContainer pdc) {
        pdc.set(Keys.APPLIED_GEM, ListPersistentDataType.LIST.strings(), appliedGem);

    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        List<Component> res = new ArrayList<>();
        res.add(Component.empty());
        res.add(buildSlotDisplay(amount, activeGem.size()));
        if (!activeGem.isEmpty()) res.addAll(buildGemLine(activeGem));
        context.builder().putLines(95, res);
    }

    private static Component buildSlotDisplay(int totalSlots, int occupiedSlots) {

        return Component.text("Còn "+ (totalSlots - occupiedSlots) + " ô ngọc trống", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private static List<Component> buildGemLine(List<BaseItem> baseItemList) {
        List<Component> res = new ArrayList<>();
        res.add(Component.text("Ngọc đã khảm:", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));

        baseItemList.forEach(baseItem -> {
            Component gemLine = Component.text("- ", NamedTextColor.DARK_GRAY);

            NameComponent name = baseItem.getComponent(ComponentKeys.ITEM_NAME);
            if (name == null) {
                gemLine = gemLine.append(Component.text(baseItem.getId()));
            } else {
                gemLine = gemLine.append(Utils.fromString(name.value()).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            }

            res.add(gemLine);
        });

        return res;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new GemSocketComponent(amount);
    }
}
