package com.roguesmp.constant;

import io.papermc.paper.registry.keys.tags.DamageTypeTagKeys;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public enum DamageFlag {
    NO_KNOCKBACK(DamageTypeTagKeys.NO_KNOCKBACK),
    BYPASS_COOLDOWN(DamageTypeTagKeys.create(Key.key(Key.MINECRAFT_NAMESPACE, "bypasses_cooldown"))),
    UNBLOCKABLE(DamageTypeTagKeys.BYPASSES_SHIELD);

    private final TagKey<org.bukkit.damage.@NotNull DamageType> tag;

    DamageFlag(TagKey<org.bukkit.damage.@NotNull DamageType> tag) {
        this.tag = tag;
    }

    public TagKey<org.bukkit.damage.@NotNull DamageType> getTag() {
        return tag;
    }

    public int mask() {
        return 1 << ordinal();
    }

    public static int combinationCount() {
        return 1 << values().length;
    }
}
