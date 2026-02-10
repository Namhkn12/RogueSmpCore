package com.roguesmp.item.component.impl;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.constant.Keys;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.gem.GemData;
import com.roguesmp.registry.GemRegistry;
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
    private final List<GemData> activeGem = new ArrayList<>();
    @GsonIgnore
    private final List<String> appliedGem = new ArrayList<>(); //For old gem data (for removal in case we change socket count)

    public GemSocketComponent(int amount) {
        this.amount = amount;
    }

    public int getSocketCount() {
        return amount;
    }

    public boolean addGem(@NotNull GemData gemData) {
        if (activeGem.size() >= amount) return false;
        activeGem.add(gemData);
        appliedGem.add(gemData.getId());
        return true;
    }

    public boolean removeGem(@NotNull GemData gemData) {
        activeGem.remove(gemData);
        appliedGem.remove(gemData.getId());
        return true;
    }

    public List<GemData> getActiveGem() {
        return activeGem;
    }

    @Override
    public void load(PersistentDataContainerView pdc) {
        List<String> appliedGemIds = pdc.get(Keys.APPLIED_GEM, ListPersistentDataType.LIST.strings());
        if (appliedGemIds == null) return;
        appliedGemIds.forEach(s -> {
             GemData gemData = GemRegistry.getInstance().getGemData(s);
             if (gemData == null) return;
             addGem(gemData);
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

    private static List<Component> buildGemLine(List<GemData> gemDataList) {
        List<Component> res = new ArrayList<>();
        res.add(Component.text("Ngọc đã khảm:", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));

        gemDataList.forEach(gemData1 -> {
            Component gemLine = Component.text("- ", NamedTextColor.DARK_GRAY);

            BaseItem baseItem = ItemRegistry.getInstance().getBaseItem(gemData1.getIconId());
            if (baseItem == null) {
                gemLine = gemLine.append(Component.text(gemData1.getId()));
            } else {
                NameComponent name = baseItem.getComponent(ComponentKeys.ITEM_NAME);
                if (name == null) {
                    gemLine = gemLine.append(Component.text(gemData1.getId()));
                } else {
                    gemLine = gemLine.append(Utils.fromString(name.value()).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
                }
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
