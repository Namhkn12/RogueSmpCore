package com.roguesmp.registry;

import com.roguesmp.item.interaction.ItemInteraction;
import com.roguesmp.item.interaction.WrenchInteraction;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The code of this only run in-world so no need for singleton
 */
public class ItemInteractionRegistry {
    private static final Map<String, ItemInteraction> registryMap = new HashMap<>();

    static {
        register("wrench", new WrenchInteraction());
    }

    public static @Nullable ItemInteraction getInteraction(String id) {
        return registryMap.get(id);
    }

    private static void register(String itemId, ItemInteraction interaction) {
        registryMap.put(itemId, interaction);
    }
}
