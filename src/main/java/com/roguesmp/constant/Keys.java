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

    /**
     * ItemStack BaseItem's id
     */
    public static final NamespacedKey ITEM_ID = of("id");

    /**
     * ItemStack UUID, used to track and cache custom SmpItem
     */
    public static final NamespacedKey ITEM_UUID = of("uuid");

    public static final NamespacedKey APPLIED_GEM = of("gem");

    public static final NamespacedKey MOB_ID = of("id");

    public static final NamespacedKey NPC_ID = of("npc");

    public static final NamespacedKey REGION_WAND = of("region_wand");

    public static final NamespacedKey BLOCK_ID = of("block_id");

    public static final NamespacedKey BLOCK_POS = of("block_pos");

    public static final NamespacedKey FRENZY_MODIFIER = of("frenzy");

    public static final String DUNGEON_NAMESPACE = "roguesmpcore";

    public static final NamespacedKey SPAWNER_IID = new NamespacedKey(DUNGEON_NAMESPACE, "spawner_iid");

    public static final NamespacedKey SPAWNER_TID = new NamespacedKey(DUNGEON_NAMESPACE, "spawner_tid");

    public static final NamespacedKey REWARD_CID = new NamespacedKey(DUNGEON_NAMESPACE, "reward_cid");

    public static final NamespacedKey REWARD_CLAIMED = new NamespacedKey(DUNGEON_NAMESPACE, "reward_claimed");

    public static final NamespacedKey NEXT_DOOR = new NamespacedKey(DUNGEON_NAMESPACE, "next_door");

    public static final NamespacedKey END_DOOR = new NamespacedKey(DUNGEON_NAMESPACE, "end_door");

    public static final NamespacedKey REVIVE_POINT = new NamespacedKey(DUNGEON_NAMESPACE, "revive_point");

    public static final NamespacedKey PIGZOMBIE_ATTACK = of("pigzombie_attack");

    //Metadata key
    public static final String IN_STEALTH_META_KEY = "in_stealth";
}
