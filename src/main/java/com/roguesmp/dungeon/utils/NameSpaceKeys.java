package com.roguesmp.dungeon.utils;

import com.roguesmp.RogueSmpCore;
import org.bukkit.NamespacedKey;

public class NameSpaceKeys {
    public static final NamespacedKey SPAWNER_IID_KEY =
            new NamespacedKey(RogueSmpCore.getInstance(), "spawner_iid");
    public static final NamespacedKey SPAWNER_TID_KEY =
            new NamespacedKey(RogueSmpCore.getInstance(), "spawner_tid");
}
