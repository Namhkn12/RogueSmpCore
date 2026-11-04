package com.roguesmp.dungeon;

import com.roguesmp.dungeon.listener.BukkitEventAdapter;
import com.roguesmp.dungeon.script.DataValue;
import com.roguesmp.quest.Quest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.Cancellable;
import com.roguesmp.block.BlockPos;
import com.roguesmp.block.BlockRef;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.manager.BlockManager;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public sealed interface DungeonEvent {

    // Data keys of events not fired from a Bukkit event; those are declared on their BukkitEventAdapter.
    Map<String, Set<String>> BUILT_IN = Map.of(
            "started", Set.of(),
            "var_changed", Set.of("value", "old"),
            "countdown_ended", Set.of(),
            "timer", Set.of("ticks")
    );

    Set<String> NO_PLAYER = Set.of("started", "countdown_ended", "timer", "quest_completed");

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

    default void cancel() {
    }

    default @Nullable Player player() {
        return null;
    }

    default @Nullable Location location() {
        return null;
    }

    static String blockId(Block block) {
        SmpBlock custom = BlockManager.getInstance().get(BlockPos.of(block));
        return custom != null ? custom.getId() : BlockRef.VANILLA_PREFIX + block.getType().getKey().getKey();
    }

    static Map<String, DataValue> blockPosition(Block block) {
        return Map.of("x", DataValue.of(block.getX()), "y", DataValue.of(block.getY()), "z", DataValue.of(block.getZ()));
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

    record MobDied(@Nullable Player killer, LivingEntity mob, String mobId, @Nullable String tag) implements DungeonEvent {
        @Override
        public String id() {
            return "mob_died";
        }

        @Override
        public String subject() {
            return mobId;
        }

        @Override
        public Map<String, DataValue> data() {
            return tag == null ? Map.of() : Map.of("tag", DataValue.of(tag));
        }

        @Override
        public @Nullable Player player() {
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
            return blockId(block);
        }

        @Override
        public boolean cancelled() {
            return event.useInteractedBlock() == Event.Result.DENY;
        }

        @Override
        public void cancel() {
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
        }

        @Override
        public Map<String, DataValue> data() {
            return blockPosition(block);
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
            return blockId(block);
        }

        @Override
        public boolean cancelled() {
            return event.isCancelled();
        }

        @Override
        public void cancel() {
            event.setCancelled(true);
        }

        @Override
        public Map<String, DataValue> data() {
            return blockPosition(block);
        }

        @Override
        public Location location() {
            return block.getLocation();
        }
    }

    // Features may cancel the event.
    // blockId: the custom block id, or minecraft:<block> for vanilla.
    record BlockPlaced(Player player, Block block, String blockId, Cancellable event) implements DungeonEvent {
        @Override
        public String id() {
            return "block_placed";
        }

        @Override
        public String subject() {
            return blockId;
        }

        @Override
        public boolean cancelled() {
            return event.isCancelled();
        }

        @Override
        public void cancel() {
            event.setCancelled(true);
        }

        @Override
        public Map<String, DataValue> data() {
            return blockPosition(block);
        }

        @Override
        public Location location() {
            return block.getLocation();
        }
    }

    // itemId: custom item id, or minecraft:<material> for vanilla.
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

    // powered: whether the block is powered after this activation.
    record RedstoneActivated(Player player, Block block, boolean powered) implements DungeonEvent {
        @Override
        public String id() {
            return "redstone_activated";
        }

        @Override
        public String subject() {
            return blockId(block);
        }

        @Override
        public Map<String, DataValue> data() {
            Map<String, DataValue> data = new HashMap<>(blockPosition(block));
            data.put("powered", DataValue.of(powered));
            return data;
        }

        @Override
        public Location location() {
            return block.getLocation();
        }
    }

    record VarChanged(String var, @Nullable DataValue oldValue, DataValue newValue, @Nullable Player player) implements DungeonEvent {
        @Override
        public String id() {
            return "var_changed";
        }

        @Override
        public String subject() {
            return var;
        }

        @Override
        public Map<String, DataValue> data() {
            return oldValue == null ? Map.of("value", newValue) : Map.of("value", newValue, "old", oldValue);
        }
    }

    record CountdownEnded(String countdown) implements DungeonEvent {
        @Override
        public String id() {
            return "countdown_ended";
        }

        @Override
        public String subject() {
            return countdown;
        }
    }

    // ticks: the run world's game time.
    record Timer(long ticks) implements DungeonEvent {
        @Override
        public String id() {
            return "timer";
        }

        @Override
        public Map<String, DataValue> data() {
            return Map.of("ticks", DataValue.of(ticks));
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

    record External(BukkitEventAdapter.Simple<?> adapter, Event event) implements DungeonEvent {
        @Override
        public String id() {
            return adapter.id();
        }

        @Override
        public String subject() {
            return adapter.subjectOf(event);
        }

        @Override
        public Map<String, DataValue> data() {
            return adapter.dataOf(event);
        }

        @Override
        public @Nullable Player player() {
            return adapter.playerOf(event);
        }

        @Override
        public @Nullable Location location() {
            return adapter.locationOf(event);
        }

        @Override
        public boolean cancelled() {
            return event instanceof Cancellable cancellable && cancellable.isCancelled();
        }

        @Override
        public void cancel() {
            if (event instanceof Cancellable cancellable) cancellable.setCancelled(true);
        }
    }
}
