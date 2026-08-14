package com.roguesmp.item.component.impl;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.codec.Codec;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.constant.Keys;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.UniqueTrackingComponent;
import com.roguesmp.registry.ItemRegistry;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.persistence.ListPersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class GemSocketComponent implements UniqueTrackingComponent {

    public static final Codec<GemSocketComponent> CODEC = Codec.INT
            .xmap(GemSocketComponent::new, GemSocketComponent::getSocketCount);

    private final int amount;

    @GsonIgnore
    private final List<String> appliedItem = new ArrayList<>(); //For old gem data (for removal in case we change socket count, or the applied gem is no longer compatible)

    public GemSocketComponent(int amount) {
        this.amount = amount;
    }

    public int getSocketCount() {
        return amount;
    }

    public void addGem(@NotNull String gemId) {
        appliedItem.add(gemId);
    }

    public boolean canFitGem() {
        return appliedItem.size() < amount;
    }

    /**
     * Have to use string to handle removing gems that are no longer fit
     */
    public void removeGem(@NotNull String baseItemId) {
        Iterator<String> iterator = appliedItem.iterator();
        while (iterator.hasNext()) {
            String gem = iterator.next();
            if (gem.equals(baseItemId)) {
                iterator.remove();
                break;
            }
        }
    }

    /**
     * Return a view of all the item that count as gem currently working/active on this itemStack
     */
    public @Unmodifiable List<BaseItem> getActiveGem() {
        List<BaseItem> activeGem = new ArrayList<>();
        for (String s : appliedItem) {
            if (activeGem.size() >= this.amount) break;
            BaseItem baseItem = ItemRegistry.getInstance().getBaseItem(s);
            if (baseItem == null) continue;
            GemDataComponent gemDataComponent = baseItem.getComponent(ItemComponentKeys.GEM_DATA);
            if (gemDataComponent == null) continue;
            activeGem.add(baseItem);
        }
        return Collections.unmodifiableList(activeGem);
    }

    /**
     * Return a view of all items applied onto his itemStack (including non-gems/legacy itemStack/gems that are no longer compatible)
     */
    public @Unmodifiable List<String> getAppliedItem() {
        return Collections.unmodifiableList(appliedItem);
    }

    @Override
    public void load(PersistentDataContainerView pdc) {
        List<String> appliedGemIds = pdc.get(Keys.APPLIED_GEM, ListPersistentDataType.LIST.strings());
        if (appliedGemIds == null) return;
        appliedGemIds.forEach(this::addGem);
    }

    @Override
    public void save(PersistentDataContainer pdc) {
        if (appliedItem.isEmpty()) {
            pdc.remove(Keys.APPLIED_GEM);
            return;
        }
        pdc.set(Keys.APPLIED_GEM, ListPersistentDataType.LIST.strings(), appliedItem);

    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        List<Component> res = new ArrayList<>();
        res.add(Component.empty());
        List<BaseItem> activeGem = getActiveGem();
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

            NameComponent name = baseItem.getComponent(ItemComponentKeys.ITEM_NAME);
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
