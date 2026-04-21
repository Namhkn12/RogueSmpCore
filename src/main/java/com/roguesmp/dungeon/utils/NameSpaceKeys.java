package com.roguesmp.dungeon.utils;

import com.roguesmp.RogueSmpCore;
import org.bukkit.NamespacedKey;

public class NameSpaceKeys {
    public static final NamespacedKey SPAWNER_IID_KEY =
            new NamespacedKey(RogueSmpCore.getInstance(), "spawner_iid");
    public static final NamespacedKey SPAWNER_TID_KEY =
            new NamespacedKey(RogueSmpCore.getInstance(), "spawner_tid");
    public static final NamespacedKey REWARD_CID_KEY =
            new NamespacedKey(RogueSmpCore.getInstance(), "reward_cid");
    public static final NamespacedKey NEXT_DOOR_KEY =
            new NamespacedKey(RogueSmpCore.getInstance(), "next_door");
    public static final NamespacedKey END_DOOR_KEY =
            new NamespacedKey(RogueSmpCore.getInstance(), "end_door");
    public static final NamespacedKey REVIVE_POINT_KEY =
            new NamespacedKey(RogueSmpCore.getInstance(), "revive_point");
}
