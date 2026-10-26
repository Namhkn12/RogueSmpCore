package com.roguesmp.teleport;

import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.IStringTooltip;
import dev.jorel.commandapi.StringTooltip;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Map;

public final class TeleportDestinationSuggestions {

    private static final Map<String, String> KEYWORD_TOOLTIPS = Map.of(
            "region", "A region's spawn: region <id> [world]",
            "spawn", "A world's spawn point: spawn [world]",
            "hub", "The hub spawn",
            "warp", "A warp: warp <name>",
            "player", "An online player: player <name>"
    );

    private TeleportDestinationSuggestions() {
    }

    public static IStringTooltip[] suggest(CommandSender sender, String input, Map<String, String> trailingOptions) {
        String[] parts = input.split(" ", -1);
        String last = parts[parts.length - 1].toLowerCase();
        String[] done = Arrays.copyOf(parts, parts.length - 1);
        String prefix = done.length == 0 ? "" : String.join(" ", done) + " ";

        Map<String, String> options = new LinkedHashMap<>();
        addOptions(sender, done, options);
        if (done.length > 0 && TeleportDestinationParser.parse(done).isSuccess()) {
            trailingOptions.forEach(options::putIfAbsent);
        }

        return options.entrySet().stream()
                .filter(option -> option.getKey().toLowerCase().startsWith(last))
                .map(option -> StringTooltip.ofString(prefix + option.getKey(), option.getValue()))
                .toArray(IStringTooltip[]::new);
    }

    private static void addOptions(CommandSender sender, String[] done, Map<String, String> options) {
        Player self = sender instanceof Player player ? player : null;
        if (done.length == 0) {
            KEYWORD_TOOLTIPS.forEach((keyword, tooltip) -> options.putIfAbsent(keyword, tooltip));
            Bukkit.getWorlds().forEach(world -> options.putIfAbsent(world.getKey().asString(), "Coordinates in " + world.getName()));
            addPlayers(options);
            if (self != null) addCoordinates(options, self.getLocation(), 0, self);
            return;
        }

        switch (done[0].toLowerCase()) {
            case "region" -> {
                if (done.length == 1) addRegions(options, self);
                if (done.length == 2) addWorldsWithRegion(options, done[1]);
            }
            case "spawn" -> {
                if (done.length == 1) Bukkit.getWorlds().forEach(world -> options.putIfAbsent(world.getKey().asString(), "Spawn of " + world.getName()));
            }
            case "warp" -> {
                if (done.length == 1) Registries.WARP.getAll().forEach((id, warp) ->
                        options.putIfAbsent(id, Utils.MINI_MESSAGE.stripTags(warp.displayName())));
            }
            case "player" -> {
                if (done.length == 1) addPlayers(options);
            }
            default -> addCoordinateContinuation(options, done, self);
        }
    }

    private static void addRegions(Map<String, String> options, @Nullable Player self) {
        for (World world : Bukkit.getWorlds()) {
            boolean own = self != null && self.getWorld().equals(world);
            for (String id : ServerRegionManager.getInstance().regions(world).keySet()) {
                if (ServerRegionManager.isGlobal(id)) continue;
                if (own) options.putIfAbsent(id, "Region in this world");
                else options.putIfAbsent(id + " " + world.getKey().asString(), "Region in " + world.getName());
            }
        }
    }

    private static void addWorldsWithRegion(Map<String, String> options, String regionId) {
        for (World world : Bukkit.getWorlds()) {
            if (ServerRegionManager.getInstance().get(world, regionId) == null) continue;
            options.putIfAbsent(world.getKey().asString(), "'" + regionId + "' in " + world.getName());
        }
    }

    private static void addPlayers(Map<String, String> options) {
        Bukkit.getOnlinePlayers().forEach(player -> options.putIfAbsent(player.getName(), "Teleport to " + player.getName()));
    }

    private static void addCoordinateContinuation(Map<String, String> options, String[] done, @Nullable Player self) {
        boolean sameWorld = Utils.isNumber(done[0]);
        int offset = sameWorld ? 0 : 1;
        for (int i = offset; i < done.length; i++) {
            if (!Utils.isNumber(done[i])) return;
        }

        Location reference = referenceLocation(sameWorld ? null : done[0], self);
        if (reference != null) addCoordinates(options, reference, done.length - offset, self);
    }

    private static @Nullable Location referenceLocation(@Nullable String worldText, @Nullable Player self) {
        if (worldText == null) return self == null ? null : self.getLocation();

        World world;
        try {
            world = Bukkit.getWorld(Key.key(worldText));
        } catch (RuntimeException e) {
            return null;
        }
        if (world == null) return null;
        return self != null && self.getWorld().equals(world) ? self.getLocation() : world.getSpawnLocation();
    }

    private static void addCoordinates(Map<String, String> options, Location reference, int typed, @Nullable Player self) {
        String source = self != null && self.getWorld().equals(reference.getWorld()) ? "your position" : "world spawn";
        String[] position = {
                String.valueOf(reference.getBlockX()), String.valueOf(reference.getBlockY()), String.valueOf(reference.getBlockZ())
        };
        if (typed < 3) {
            String[] remaining = Arrays.copyOfRange(position, typed, 3);
            options.putIfAbsent(String.join(" ", remaining), "x y z of " + source);
            if (remaining.length > 1) options.putIfAbsent(remaining[0], "xyz".charAt(typed) + " of " + source);
            return;
        }
        if (typed == 3) {
            options.putIfAbsent(Math.round(reference.getYaw()) + " " + Math.round(reference.getPitch()), "yaw pitch of " + source);
        }
    }
}
