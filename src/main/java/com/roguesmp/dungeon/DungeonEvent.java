package com.roguesmp.dungeon;

import com.roguesmp.dungeon.script.DataValue;
import com.roguesmp.quest.Quest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public sealed interface DungeonEvent {

    // Data keys each built-in event carries.
    Map<String, Set<String>> BUILT_IN = Map.ofEntries(
            Map.entry("started", Set.of()),
            Map.entry("player_entered", Set.of()),
            Map.entry("player_left", Set.of()),
            Map.entry("region_entered", Set.of()),
            Map.entry("region_left", Set.of()),
            Map.entry("mob_killed", Set.of()),
            Map.entry("mob_damaged", Set.of("damage", "health_percent")),
            Map.entry("block_interacted", Set.of()),
            Map.entry("block_broken", Set.of()),
            Map.entry("item_picked_up", Set.of("amount")),
            Map.entry("portal_entered", Set.of()),
            Map.entry("player_died", Set.of()),
            Map.entry("quest_completed", Set.of())
    );

    String id();

    default String subject() {
        return "";
    }

    default Map<String, DataValue> data() {
        return Map.of();
    }

    default int amount() {
        return 1;
    }

    default boolean cancelled() {
        return false;
    }

    default @Nullable Player player() {
        return null;
    }

    default @Nullable Location location() {
        return null;
    }

    record Started() implements DungeonEvent {
        @Override
        public String id() {
            return "started";
        }
    }

    record PlayerEntered(Player player) implements DungeonEvent {
        @Override
        public String id() {
            return "player_entered";
        }
    }

    record PlayerLeft(Player player) implements DungeonEvent {
        @Override
        public String id() {
            return "player_left";
        }
    }

    record RegionEntered(Player player, String region) implements DungeonEvent {
        @Override
        public String id() {
            return "region_entered";
        }

        @Override
        public String subject() {
            return region;
        }
    }

    record RegionLeft(Player player, String region) implements DungeonEvent {
        @Override
        public String id() {
            return "region_left";
        }

        @Override
        public String subject() {
            return region;
        }
    }

    // mobId: custom entity id, or the vanilla type key.
    record MobKilled(Player killer, LivingEntity mob, String mobId) implements DungeonEvent {
        @Override
        public String id() {
            return "mob_killed";
        }

        @Override
        public String subject() {
            return mobId;
        }

        @Override
        public Player player() {
            return killer;
        }

        @Override
        public Location location() {
            return mob.getLocation();
        }
    }

    record MobDamaged(@Nullable Player attacker, LivingEntity mob, String mobId, double damage, double healthPercent) implements DungeonEvent {
        @Override
        public String id() {
            return "mob_damaged";
        }

        @Override
        public String subject() {
            return mobId;
        }

        @Override
        public Map<String, DataValue> data() {
            return Map.of("damage", DataValue.of(damage), "health_percent", DataValue.of(healthPercent));
        }

        @Override
        public @Nullable Player player() {
            return attacker;
        }

        @Override
        public Location location() {
            return mob.getLocation();
        }
    }

    // Features may cancel the event.
    record BlockInteracted(Player player, Block block, PlayerInteractEvent event) implements DungeonEvent {
        @Override
        public String id() {
            return "block_interacted";
        }

        @Override
        public String subject() {
            return block.getType().getKey().getKey();
        }

        @Override
        public boolean cancelled() {
            return event.useInteractedBlock() == Event.Result.DENY;
        }

        @Override
        public Location location() {
            return block.getLocation();
        }
    }

    // Features may cancel the event.
    record BlockBroken(Player player, Block block, BlockBreakEvent event) implements DungeonEvent {
        @Override
        public String id() {
            return "block_broken";
        }

        @Override
        public String subject() {
            return block.getType().getKey().getKey();
        }

        @Override
        public boolean cancelled() {
            return event.isCancelled();
        }

        @Override
        public Location location() {
            return block.getLocation();
        }
    }

    // itemId: custom item id, or the vanilla material key.
    record ItemPickedUp(Player player, ItemStack item, String itemId, int amount) implements DungeonEvent {
        @Override
        public String id() {
            return "item_picked_up";
        }

        @Override
        public Map<String, DataValue> data() {
            return Map.of("amount", DataValue.of(amount));
        }

        @Override
        public String subject() {
            return itemId;
        }

        @Override
        public Location location() {
            return player.getLocation();
        }
    }

    record PortalEntered(Player player, Material portal, Location location) implements DungeonEvent {
        @Override
        public String id() {
            return "portal_entered";
        }

        @Override
        public String subject() {
            return portal.getKey().getKey();
        }
    }

    // Features may cancel the event; a cancelled death is still a death.
    record PlayerDied(Player player, PlayerDeathEvent event) implements DungeonEvent {
        @Override
        public String id() {
            return "player_died";
        }

        @Override
        public Location location() {
            return player.getLocation();
        }
    }

    record QuestCompleted(Quest quest) implements DungeonEvent {
        @Override
        public String id() {
            return "quest_completed";
        }

        @Override
        public String subject() {
            return quest.getId();
        }
    }

    record Custom(String id, int amount, @Nullable Player source, Map<String, DataValue> data) implements DungeonEvent {
        public Custom(String id, int amount, @Nullable Player source) {
            this(id, amount, source, Map.of());
        }

        @Override
        public @Nullable Player player() {
            return source;
        }
    }
}
