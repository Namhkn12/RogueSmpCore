package com.roguesmp.tag;

import com.roguesmp.entity.BaseEntity;
import com.roguesmp.item.BaseItem;
import com.roguesmp.quest.Quest;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Every tag the code depends on, as a direct {@link SmpTag}. "Built-in" means the tag is
 * guaranteed to exist on its registry: with no {@code <locationKey>/tags/<id>.json} it's an empty
 * tag, otherwise that file supplies its members like any other tag. The registry keeps these exact
 * instances across reloads, so holding {@code Tags.WEAPONS} directly is always safe.
 * <p>
 * To add one, declare it here - {@link #bootstrap()} picks it up automatically.
 */
public final class Tags {

    private static final List<Runnable> DECLARATIONS = new ArrayList<>();

    // Items
    public static final SmpTag<BaseItem> WEAPONS = builtIn("weapons", () -> Registries.ITEM);

    // Entities
    public static final SmpTag<BaseEntity> ELITE = builtIn("elite", () -> Registries.ENTITY);
    public static final SmpTag<BaseEntity> BOSS = builtIn("boss", () -> Registries.ENTITY);
    public static final SmpTag<BaseEntity> ANGELIC = builtIn("angelic", () -> Registries.ENTITY);
    public static final SmpTag<BaseEntity> FRIENDLY = builtIn("friendly", () -> Registries.ENTITY);

    // Quests
    public static final SmpTag<Quest> DAILY_EASY_QUEST = builtIn("daily_easy_quest", () -> Registries.QUEST);
    public static final SmpTag<Quest> DAILY_MEDIUM_QUEST = builtIn("daily_medium_quest", () -> Registries.QUEST);
    public static final SmpTag<Quest> DAILY_HARD_QUEST = builtIn("daily_hard_quest", () -> Registries.QUEST);

    private Tags() {
    }

    /**
     * Declares every built-in tag on its registry so it exists even without a tag file. Must run
     * before {@link Registry#loadTagsFrom} - see {@code Registries#boostrap}.
     */
    public static void bootstrap() {
        DECLARATIONS.forEach(Runnable::run);
    }

    // The registry is a Supplier because this runs during static init, when Registries may not
    // have finished loading - it's only dereferenced later, in bootstrap() and on resolve.
    private static <T> SmpTag<T> builtIn(String id, Supplier<Registry<T>> registry) {
        SmpTag<T> tag = new SmpTag<>(id, List.of(), key -> registry.get().get(key));
        DECLARATIONS.add(() -> registry.get().registerBuiltInTag(tag));
        return tag;
    }
}
