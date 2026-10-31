package com.roguesmp.permission;

import org.bukkit.permissions.Permissible;

public enum Permissions {
    ISLAND_ADMIN("island.admin"),
    ISLAND_VISIT_BYPASS("island.visit.bypass"),
    DUNGEON_BUILD("dungeon.build"),
    GLYPH_GENERATE("glyph.generate"),
    CONTENT("content"),
    RELOAD("reload"),
    QUEST_ASSIGN("quest.assign"),
    QUEST_EXP("quest.exp"),
    QUEST_TOP_REFRESH("quest.top.refresh"),
    ABILITY_UNLOCK("ability.unlock"),
    FISHING_ADMIN("fishing.admin"),
    REGION_ADMIN("region.admin"),
    DUNGEON_ADMIN("dungeon.admin"),
    SPAWNER_ADMIN("spawner.admin"),
    POLAR_TEMPLATE_ADMIN("polartemplate.admin"),
    WARP_ADMIN("warp.admin"),
    TELEPORT("teleport"),
    MONEY_ADMIN("money.admin");

    private static final String ROOT = "roguesmp.";

    private final String node;

    Permissions(String path) {
        this.node = ROOT + path;
    }

    public String node() {
        return node;
    }

    public boolean has(Permissible permissible) {
        return permissible.hasPermission(node);
    }
}
