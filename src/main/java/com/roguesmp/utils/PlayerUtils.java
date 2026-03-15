package com.roguesmp.utils;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PlayerUtils {
    /**
     * Given a list of players, a location, and settings, returns the players that are close enough to
     * <code>range</code>.
     *
     * @param ps                   Players we will check
     * @param loc                  The location to check
     * @param range                The valid range
     * @param includeNonTargetable Whether to include non-targetable players
     * @param includeDead          Whether to include players that are currently dead
     * @return The players that fit the criteria
     */
    public static List<Player> playersInRange(Iterable<Player> ps, Location loc, double range,
                                              boolean includeNonTargetable, boolean includeDead) {
        List<Player> players = new ArrayList<>();

        double rangeSquared = range * range;
        for (Player player : ps) {
            if (player.getLocation().distanceSquared(loc) < rangeSquared
                    && player.getGameMode() != GameMode.SPECTATOR) {
                players.add(player);
            }
        }

        return players;
    }

    public static List<Player> playersInRange(Location loc, double range, boolean includeNonTargetable,
                                              boolean includeDead) {
        return playersInRange(loc.getWorld().getPlayers(), loc, range, includeNonTargetable, includeDead);
    }

    public static List<Player> playersInRange(Location loc, double range, boolean includeNonTargetable) {
        return playersInRange(loc, range, includeNonTargetable, false);
    }
}
