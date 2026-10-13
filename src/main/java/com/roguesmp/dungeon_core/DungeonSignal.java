package com.roguesmp.dungeon_core;

import com.roguesmp.quest.Quest;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Everything that happens inside one run, delivered to every feature of that run through
 * {@link DungeonFeature#onSignal}. Gameplay is turned into signals by {@link DungeonListener}; features send
 * {@link Custom} signals of their own.
 */
public sealed interface DungeonSignal {

    /**
     * @return the player who caused it, if any
     */
    default @Nullable Player player() {
        return null;
    }

    record Started() implements DungeonSignal {
    }

    record PlayerEntered(Player player) implements DungeonSignal {
    }

    record PlayerLeft(Player player) implements DungeonSignal {
    }

    record RegionEntered(Player player, String region) implements DungeonSignal {
    }

    record RegionLeft(Player player, String region) implements DungeonSignal {
    }

    /**
     * @param mobId the custom entity id, or the vanilla entity type key for vanilla mobs
     */
    record MobKilled(Player killer, LivingEntity mob, String mobId) implements DungeonSignal {
        @Override
        public Player player() {
            return killer;
        }
    }

    record PlayerDied(Player player) implements DungeonSignal {
    }

    record QuestCompleted(Quest quest) implements DungeonSignal {
    }

    record Custom(String id, int amount, @Nullable Player source, Map<String, String> data) implements DungeonSignal {
        public Custom(String id, int amount, @Nullable Player source) {
            this(id, amount, source, Map.of());
        }

        @Override
        public @Nullable Player player() {
            return source;
        }
    }
}
