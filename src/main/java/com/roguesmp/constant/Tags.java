package com.roguesmp.constant;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.quest.Quest;
import com.roguesmp.registry.Registries;
import com.roguesmp.tag.SmpTag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;

public class Tags {
    private static final Map<String, SmpTag<?>> REGISTRY = new HashMap<>();

    public static final SmpTag<Enchants> IN_ENCHANTABLE = register("in_enchantable", Enchants::fromId);
    public static final SmpTag<Enchants> TOP_ENCHANT = register("top_enchant", Enchants::fromId);
    public static final SmpTag<Quest> DAILY_EASY_QUEST = register("daily_easy_quest", Registries.QUEST::get);
    public static final SmpTag<Quest> DAILY_MEDIUM_QUEST = register("daily_medium_quest", Registries.QUEST::get);
    public static final SmpTag<Quest> DAILY_HARD_QUEST = register("daily_hard_quest", Registries.QUEST::get);


    private static <T> SmpTag<T> register(String id, Function<String, T> res) {
        SmpTag<T> tag = new SmpTag<>(id, res);
        REGISTRY.put(id, tag);
        return tag;
    }

    public static void loadTagData(RogueSmpCore plugin) {
        // 1. First pass: Load all raw JSON strings into memory
        RogueSmpCore.LOGGER.info("Loading tags data...");
        REGISTRY.values().forEach(tag -> tag.load(plugin));

        // 2. Second pass: calculate every tag's full element set
        for (SmpTag<?> tag : REGISTRY.values()) {
            resolveRecursively(tag);
        }
        RogueSmpCore.LOGGER.info("Loaded {} tags.",REGISTRY.size());
    }

    @SuppressWarnings("unchecked")
    private static <T> void resolveRecursively(SmpTag<T> tag) {
        tag.resolve(id -> (SmpTag<T>) REGISTRY.get(id), new HashSet<>());
    }
}
