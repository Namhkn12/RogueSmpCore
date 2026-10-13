package com.roguesmp.dungeon_core;

import com.roguesmp.quest.Quest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public sealed interface DungeonSignal {

    default @Nullable Player player() {
        return null;
    }

    default @Nullable Location location() {
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

    // mobId: custom entity id, or the vanilla type key.
    record MobKilled(Player killer, LivingEntity mob, String mobId) implements DungeonSignal {
        @Override
        public Player player() {
            return killer;
        }

        @Override
        public Location location() {
            return mob.getLocation();
        }
    }

    // Features may cancel the event.
    record BlockInteracted(Player player, Block block, PlayerInteractEvent event) implements DungeonSignal {
        @Override
        public Location location() {
            return block.getLocation();
        }
    }

    // Features may cancel the event.
    record BlockBroken(Player player, Block block, BlockBreakEvent event) implements DungeonSignal {
        @Override
        public Location location() {
            return block.getLocation();
        }
    }

    // itemId: custom item id, or the vanilla material key.
    record ItemPickedUp(Player player, ItemStack item, String itemId, int amount) implements DungeonSignal {
        @Override
        public Location location() {
            return player.getLocation();
        }
    }

    record PortalEntered(Player player, Material portal, Location location) implements DungeonSignal {
    }

    // Features may cancel the event.
    record PlayerDied(Player player, PlayerDeathEvent event) implements DungeonSignal {
        @Override
        public Location location() {
            return player.getLocation();
        }
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
