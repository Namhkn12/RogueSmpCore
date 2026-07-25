package com.roguesmp.registry;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.impl.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.*;
import java.util.Map;

public class ItemRegistry {

    private static ItemRegistry INSTANCE = null;

    private final RogueSmpCore plugin;
    private final Map<String, BaseItem> dataMap = Registries.ITEM.getAll();

    private ItemRegistry(RogueSmpCore plugin) {
        this.plugin = plugin;
    }

    public @Nullable BaseItem getBaseItem(@NotNull String id) {
        return dataMap.get(id);
    }

    public static void init(RogueSmpCore core) {
        INSTANCE = new ItemRegistry(core);
    }

    /**
     * Deprecated, will be removed soon. Change your code to use {@link Registries#ITEM} instead.
     */
    @Deprecated
    public static ItemRegistry getInstance() {
        return INSTANCE;
    }

}
