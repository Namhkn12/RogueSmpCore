package com.roguesmp.loot.condition.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.condition.LootCondition;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.loot.context.LootOrigin;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Passes only when the roll's {@link LootOrigin} is one of {@link #getOrigins()} — lets a
 * single loot table scope specific entries to a caller (e.g. only from CHEST, never ENTITY)
 * without needing a separate table per origin.
 */
public class OriginCondition implements LootCondition {

    public static final String TYPE_KEY = "origin";

    public static final Codec<OriginCondition> CODEC = Codec.listOf(Codec.enumOf(LootOrigin.class))
            .fieldOf("origins")
            .xmap(OriginCondition::fromList, condition -> List.copyOf(condition.origins))
            .codec();

    // EnumSet.copyOf(Collection) throws on an empty collection - build via noneOf+addAll instead.
    private static OriginCondition fromList(List<LootOrigin> list) {
        Set<LootOrigin> origins = EnumSet.noneOf(LootOrigin.class);
        origins.addAll(list);
        return new OriginCondition(origins);
    }

    private final Set<LootOrigin> origins;

    public OriginCondition(Set<LootOrigin> origins) {
        this.origins = origins;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public Set<LootOrigin> getOrigins() {
        return origins;
    }

    @Override
    public boolean test(LootContext context) {
        return origins.contains(context.getOrigin());
    }
}
