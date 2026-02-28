package com.roguesmp.integration;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.player.PlayerLoadEvent;
import me.neznamy.tab.api.tablist.layout.Layout;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Unused for now
public class TABIntegration implements Listener {
    public static TABIntegration INSTANCE;

    private final TabAPI tabAPI;

    public record Slot(int id, String text, @Nullable String skin, @Nullable Integer ping) {
    }

    public static class CachedLayout {
        public ConcurrentHashMap.KeySetView<Slot, Boolean> layout = ConcurrentHashMap.newKeySet();

        public void addFixedSlot(int id, String text) {
            layout.add(new Slot(id, text, null, null));
        }

        public void addFixedSlot(int id, String text, @Nullable String skin) {
            layout.add(new Slot(id, text, skin, null));
        }

        public void addFixedSlot(int id, String text, @Nullable String skin, int ping) {
            layout.add(new Slot(id, text, skin, ping));
        }

        public void addFixedSlot(int id, String text, int ping) {
            layout.add(new Slot(id, text, null, ping));
        }

        public Layout toLayout(UUID uuid) {
            Layout layout = Objects.requireNonNull(TabAPI.getInstance().getLayoutManager()).createNewLayout(uuid.toString());
            for (Slot slot : this.layout) {
                if (slot.skin != null && !slot.skin.isEmpty() && slot.ping != null) {
                    layout.addFixedSlot(slot.id, slot.text, slot.skin, slot.ping);
                } else if (slot.skin != null && !slot.skin.isEmpty()) {
                    layout.addFixedSlot(slot.id, slot.text, slot.skin);
                } else if (slot.ping != null) {
                    layout.addFixedSlot(slot.id, slot.text, slot.ping);
                } else {
                    layout.addFixedSlot(slot.id, slot.text);
                }
            }
            return layout;
        }

        public boolean isSameLayout(@Nullable CachedLayout other) {
            if (other == null) {
                return false;
            }
            return layout.equals(other.layout);
        }
    }

    public static class ServerPlayer {
        public final UUID uuid;
        public final String name;
        public int ping;
        public String skin;
        public @Nullable CachedLayout lastLayout;

        public ServerPlayer(Player player) {
            this.uuid = player.getUniqueId();
            this.name = player.getName();
            this.skin = "player:"+player.getName();
        }

        public static void setPing(UUID uuid, int ping) {
            players.computeIfPresent(uuid, (key, oldValue) -> {
                oldValue.ping = ping;
                return oldValue;
            });
        }
    }

    public static final Map<UUID, ServerPlayer> players = new ConcurrentHashMap<>();

    public TABIntegration() {
        INSTANCE = this;
        tabAPI = TabAPI.getInstance();
        tabAPI.getEventBus().register(PlayerLoadEvent.class, this::playerLoadEvent);
    }

    public void playerLoadEvent(PlayerLoadEvent event) {
        loadPlayer((Player) event.getPlayer().getPlayer());
    }

    @EventHandler
    public void playerQuitEvent(PlayerQuitEvent event) {
        unloadPlayer(event.getPlayer());
    }

    public static void loadPlayer(Player player) {
        if (INSTANCE == null) {
            return;
        }
        players.put(player.getUniqueId(), new ServerPlayer(player));

    }

    public static void unloadPlayer(Player player) {
        if (INSTANCE == null) {
            return;
        }
        players.remove(player.getUniqueId());
    }

    private CachedLayout createBaseLayout() {
        CachedLayout layout = new CachedLayout();

        layout.addFixedSlot(1, "<blue>Custom Effects", "texture:9a50157c3a83e0ff64a18ec14c5da6d44efc7dcfe62d320e7f6e59e4da342691");
        for (int effectIndex = 2; effectIndex <= 11; effectIndex++) {
            layout.addFixedSlot(effectIndex, "%roguesmp_effect_" + effectIndex + "%");
        }
        layout.addFixedSlot(12, "%roguesmp_effect_more%");
        return layout;
    }

    private CachedLayout calculateLayout(CachedLayout layout) {
        // not optimized at all, store permission/meta somewhere when initially fetching the player
        int layoutIndex = 21;

        // player list
        for (ServerPlayer serverPlayer : players.values()) {

            layout.addFixedSlot(layoutIndex, "");
            layoutIndex++;
            if (layoutIndex >= 81) {
                return layout;
            }

            layout.addFixedSlot(layoutIndex, "&f&lPlayers", "texture:9a50157c3a83e0ff64a18ec14c5da6d44efc7dcfe62d320e7f6e59e4da342691");
            layoutIndex++;
            if (layoutIndex >= 81) {
                return layout;
            }

            layout.addFixedSlot(layoutIndex, formatPlayer(serverPlayer), serverPlayer.skin, serverPlayer.ping);
            layoutIndex++;
            if (layoutIndex >= 81) {
                return layout;
            }
        }

        return layout;
    }

    private void setHeaderAndFooter(TabPlayer viewer, ServerPlayer player) {
        String header = "<blue><b>Welcome to RogueSMP";
        String footer = "&7 Total players:&f %online%";
        Objects.requireNonNull(tabAPI.getHeaderFooterManager()).setHeaderAndFooter(viewer, header, footer);
    }

    private String formatPlayer(ServerPlayer player) {
        // &7<${player server}>${vanish_suffix}${adminOrNoGuild}${hexColorTag}${player luckpermsbungee_prefix}${isAdmin}&r${player luckpermsbungee_suffix}${player display_name}
        return MessageFormat.format("{0}", player.name);
    }

    public static TABIntegration getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TABIntegration();
        }
        return INSTANCE;
    }
}
