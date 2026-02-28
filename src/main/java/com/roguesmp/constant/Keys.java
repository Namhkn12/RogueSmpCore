package com.roguesmp.constant;

import org.bukkit.NamespacedKey;

public class Keys {
    public static final String GLOBAL_NAMESPACE = "smp";

    /**
     * Convenient method to create a NamespacedKey under GLOBAL_NAMESPACE
     * @param key The key
     * @return A NamespacedKey instance
     */
    public static NamespacedKey of(String key) {
        return new NamespacedKey(GLOBAL_NAMESPACE, key);
    }

    public static final NamespacedKey ITEM_ID = of("id");

    public static final NamespacedKey APPLIED_GEM = of("gem");

    public static final NamespacedKey MOB_ID = of("id");
}
