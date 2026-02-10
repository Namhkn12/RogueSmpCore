package com.roguesmp.player;

import com.roguesmp.RogueSmpCore;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {
    private static PlayerManager INSTANCE = null;

    private static final int PERIOD = 5;

    private final RogueSmpCore plugin;
    private final Map<UUID, SmpPlayer> players;

    private PlayerManager(RogueSmpCore plugin) {
        this.plugin = plugin;
        players = new HashMap<>();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks += PERIOD;
                boolean twoHz = ticks % 10 == 0;
                boolean oneHz = ticks % 20 == 0;

                for (Map.Entry<UUID, SmpPlayer> entry : players.entrySet()) {
                    SmpPlayer player = entry.getValue();

                    player.getActiveEnchants().forEach((enchants, integer) -> enchants.getEnchant().tick(player, integer, twoHz, oneHz));
                    player.getActiveAttributes().forEach((attributes, aDouble) -> attributes.getAttribute().tick(player, aDouble, twoHz, oneHz));
                }
            }
        }.runTaskTimer(this.plugin, 0, PERIOD);
    }

    public @Nullable SmpPlayer getSmpPlayer(UUID uuid) {
        return players.getOrDefault(uuid, null);
    }

    public void addPlayer(UUID uuid) {
        players.put(uuid, new SmpPlayer(uuid));
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
    }

    public static PlayerManager getInstance() {
        if (INSTANCE == null) {
            throw new RuntimeException(PlayerManager.class.getSimpleName() + "is null when getInstance() is called.");
        }
        return INSTANCE;
    }

    public static void init(RogueSmpCore core) {
        INSTANCE = new PlayerManager(core);
    }
}
