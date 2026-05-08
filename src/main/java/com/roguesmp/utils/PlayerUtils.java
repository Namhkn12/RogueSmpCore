package com.roguesmp.utils;

import com.roguesmp.RogueSmpCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    /**
     * Give items to players and drop items that are not fit on the ground, only the owner can see and pick up the dropped item
     */
    public static void giveItem(Player player, ItemStack... itemStacks) {
        Map<Integer, ItemStack> leftOver = player.getInventory().addItem(itemStacks);
        if (!leftOver.isEmpty()) {
            player.sendMessage(Component.text("Túi đồ của bạn đã đầy nên một số vật phẩm sẽ bị rơi ra", NamedTextColor.RED));
            for (ItemStack item : leftOver.values()) {
                Item itemEntity = player.getWorld().dropItemNaturally(player.getLocation(), item);
                itemEntity.setOwner(player.getUniqueId());
                itemEntity.setVisibleByDefault(false);
                player.showEntity(RogueSmpCore.getInstance(), itemEntity);
            }
        }

    }

    /**
     * Calculate a player's total experience based on level and progress to next.
     *
     * @param player the Player
     * @return the amount of experience the Player has
     *
     * @see <a href=http://minecraft.wiki/Experience#Leveling_up>Experience#Leveling_up</a>
     */
    public static int getExp(Player player) {
        return getExpFromLevel(player.getLevel())
                + Math.round(getExpToNext(player.getLevel()) * player.getExp());
    }

    /**
     * Calculate total experience based on level.
     *
     * @param level the level
     * @return the total experience calculated
     *
     * @see <a href=http://minecraft.wiki/Experience#Leveling_up>Experience#Leveling_up</a>
     */
    public static int getExpFromLevel(int level) {
        if (level > 30) {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
        if (level > 15) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        }
        return level * level + 6 * level;
    }

    /**
     * Calculate level (including progress to next level) based on total experience.
     *
     * @param exp the total experience
     * @return the level calculated
     */
    public static double getLevelFromExp(long exp) {
        int level = getIntLevelFromExp(exp);

        // Get remaining exp progressing towards next level. Cast to float for next bit of math.
        float remainder = exp - (float) getExpFromLevel(level);

        // Get level progress with float precision.
        float progress = remainder / getExpToNext(level);

        // Slap both numbers together and call it a day. While it shouldn't be possible for progress
        // to be an invalid value (value < 0 || 1 <= value)
        return ((double) level) + progress;
    }

    /**
     * Calculate level based on total experience.
     *
     * @param exp the total experience
     * @return the level calculated
     */
    public static int getIntLevelFromExp(long exp) {
        if (exp > 1395) {
            return (int) ((Math.sqrt(72 * exp - 54215D) + 325) / 18);
        }
        if (exp > 315) {
            return (int) (Math.sqrt(40 * exp - 7839D) / 10 + 8.1);
        }
        if (exp > 0) {
            return (int) (Math.sqrt(exp + 9D) - 3);
        }
        return 0;
    }

    /**
     * Get the total amount of experience required to progress to the next level.
     *
     * @param level the current level
     *
     * @see <a href=http://minecraft.wiki/Experience#Leveling_up>Experience#Leveling_up</a>
     */
    private static int getExpToNext(int level) {
        if (level >= 30) {
            // Simplified formula. Internal: 112 + (level - 30) * 9
            return level * 9 - 158;
        }
        if (level >= 15) {
            // Simplified formula. Internal: 37 + (level - 15) * 5
            return level * 5 - 38;
        }
        // Internal: 7 + level * 2
        return level * 2 + 7;
    }

    /**
     * Change a Player's experience.
     *
     * <p>This method is preferred over {@link Player#giveExp(int)}.
     * <br>In older versions the method does not take differences in exp per level into account.
     * This leads to overlevelling when granting players large amounts of experience.
     * <br>In modern versions, while differing amounts of experience per level are accounted for, the
     * approach used is loop-heavy and requires an excessive number of calculations, which makes it
     * quite slow.
     *
     * @param player the Player affected
     * @param exp the amount of experience to add or remove
     */
    public static void changeExp(Player player, int exp) {
        exp += getExp(player);

        if (exp < 0) {
            exp = 0;
        }

        double levelAndExp = getLevelFromExp(exp);
        int level = (int) levelAndExp;
        player.setLevel(level);
        player.setExp((float) (levelAndExp - level));
    }
}
