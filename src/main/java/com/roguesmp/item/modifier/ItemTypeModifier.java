package com.roguesmp.item.modifier;

import com.roguesmp.item.BaseItem;
import com.roguesmp.item.ItemType;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.ItemTypeComponent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Holder;
import com.roguesmp.registry.Registries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Derives {@link ItemTypeComponent} (classification lore, e.g. "Kiếm"/"Rìu") from the item's own
 * tags - every registered {@link ItemType} whose {@link ItemType#tagId()} the item is tagged under
 * gets attached, so an item automatically picks up new categories as tags change, with no
 * per-item authoring needed.
 */
public class ItemTypeModifier implements ItemModifier {
    @Override
    public void collectAndApply(SmpItem smpItem, @Nullable SmpPlayer player) {
        Holder<BaseItem> baseItemHolder = Registries.ITEM.getHolder(smpItem.getId());
        Set<String> itemTagIds = baseItemHolder.getTagIds();

        List<Holder<ItemType>> matchingTypes = new ArrayList<>();
        for (Map.Entry<String, ItemType> entry : Registries.ITEM_TYPE.getAll().entrySet()) {
            if (itemTagIds.contains(entry.getValue().tagId())) {
                matchingTypes.add(Registries.ITEM_TYPE.getHolder(entry.getKey()));
            }
        }

        if (matchingTypes.isEmpty()) {
            smpItem.unsetComponent(ItemComponentKeys.ITEM_TYPE);
        } else {
            smpItem.setComponent(ItemComponentKeys.ITEM_TYPE, new ItemTypeComponent(matchingTypes));
        }
    }
}
