package com.roguesmp.command;

import com.roguesmp.teleport.destination.RegionDestination;
import com.roguesmp.teleport.TeleportOptions;
import com.roguesmp.teleport.TeleportService;
import com.roguesmp.permission.Permissions;
import com.roguesmp.region.RegionSelection;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.region.editor.RegionBrowserGui;
import com.roguesmp.region.editor.RegionSelections;
import com.roguesmp.region.editor.RegionWand;
import com.roguesmp.utils.Utils;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class RegionCommand {

    private RegionCommand() {
    }

    public static void register() {
        new CommandAPICommand("smpregion")
                .withPermission(Permissions.REGION_ADMIN.node())
                .executesPlayer((player, args) -> {
                    new RegionBrowserGui(player).showInventory(player);
                })
                .withSubcommand(new CommandAPICommand("wand")
                        .executesPlayer((player, args) -> {
                            player.getInventory().addItem(RegionWand.create());
                        }))
                .withSubcommand(new CommandAPICommand("define")
                        .withArguments(new StringArgument("id"))
                        .executesPlayer((player, args) -> {
                            define(player, (String) args.get("id"));
                        }))
                .withSubcommand(new CommandAPICommand("merge")
                        .withArguments(regionArgument())
                        .executesPlayer((player, args) -> {
                            RegionSelection selection = selection(player);
                            if (selection == null) return;
                            withRegion(player, (String) args.get("id"), id -> {
                                ServerRegionManager.getInstance().merge(player.getWorld(), id, selection);
                                RegionSelections.getInstance().clear(player);
                            });
                        }))
                .withSubcommand(new CommandAPICommand("redefine")
                        .withArguments(regionArgument())
                        .executesPlayer((player, args) -> {
                            RegionSelection selection = selection(player);
                            if (selection == null) return;
                            withRegion(player, (String) args.get("id"), id -> {
                                ServerRegionManager.getInstance().replaceShape(player.getWorld(), id, selection);
                                RegionSelections.getInstance().clear(player);
                            });
                        }))
                .withSubcommand(new CommandAPICommand("delete")
                        .withArguments(regionArgument())
                        .executesPlayer((player, args) -> {
                            withRegion(player, (String) args.get("id"), id -> ServerRegionManager.getInstance().remove(player.getWorld(), id));
                        }))
                .withSubcommand(new CommandAPICommand("list")
                        .executesPlayer((player, args) -> {
                            player.sendMessage(Utils.fromString("<yellow>Regions: <white>"
                                    + String.join(", ", ServerRegionManager.getInstance().regions(player.getWorld()).keySet())));
                        }))
                .withSubcommand(new CommandAPICommand("here")
                        .executesPlayer((player, args) -> {
                            player.sendMessage(Utils.fromString("<yellow>Regions here: <white>"
                                    + String.join(", ", ServerRegionManager.getInstance().sortedRegionIdsAt(player.getLocation()))));
                        }))
                .withSubcommand(new CommandAPICommand("priority")
                        .withArguments(regionArgument(), new IntegerArgument("priority"))
                        .executesPlayer((player, args) -> {
                            withRegion(player, (String) args.get("id"),
                                    id -> ServerRegionManager.getInstance().setPriority(player.getWorld(), id, (int) args.get("priority")));
                        }))
                .withSubcommand(new CommandAPICommand("flag")
                        .withArguments(regionArgument(), flagArgument(), new GreedyStringArgument("value"))
                        .executesPlayer((player, args) -> {
                            String flag = (String) args.get("flag");
                            String value = (String) args.get("value");
                            String error = ServerRegionManager.getInstance().flagError(flag, value);
                            if (error != null) {
                                player.sendMessage(Utils.fromString("<red>" + error));
                                return;
                            }
                            withRegion(player, (String) args.get("id"), id -> {
                                try {
                                    ServerRegionManager.getInstance().setFlag(player.getWorld(), id, flag, value, RegionGroup.ALL);
                                } catch (InvalidFlagFormat e) {
                                    player.sendMessage(Utils.fromString("<red>" + e.getMessage()));
                                }
                            });
                        }))
                .withSubcommand(new CommandAPICommand("unflag")
                        .withArguments(regionArgument(), flagArgument())
                        .executesPlayer((player, args) -> {
                            Flag<?> flag = WorldGuard.getInstance().getFlagRegistry().get((String) args.get("flag"));
                            if (flag == null) {
                                player.sendMessage(Utils.fromString("<red>Unknown flag."));
                                return;
                            }
                            withRegion(player, (String) args.get("id"), id -> ServerRegionManager.getInstance().clearFlag(player.getWorld(), id, flag));
                        }))
                .withSubcommand(new CommandAPICommand("tp")
                        .withArguments(regionArgument())
                        .executesPlayer((player, args) -> {
                            RegionDestination destination = new RegionDestination((String) args.get("id"), null);
                            if (!destination.isAvailable(player)) {
                                player.sendMessage(Utils.fromString("<red>No such region in this world."));
                                return;
                            }
                            TeleportService.getInstance().teleport(player, destination, TeleportOptions.INSTANT);
                        }))
                .register();
    }

    private static Argument<String> regionArgument() {
        return new StringArgument("id")
                .replaceSuggestions(ArgumentSuggestions.strings(info -> info.sender() instanceof Player player
                        ? ServerRegionManager.getInstance().regions(player.getWorld()).keySet().toArray(String[]::new)
                        : new String[0]));
    }

    private static Argument<String> flagArgument() {
        return new StringArgument("flag")
                .replaceSuggestions(ArgumentSuggestions.strings(info -> WorldGuard.getInstance().getFlagRegistry().getAll().stream()
                        .map(Flag::getName).toArray(String[]::new)));
    }

    private static void define(Player player, String id) {
        if (!ProtectedRegion.isValidId(id) || id.startsWith("__")) {
            player.sendMessage(Utils.fromString("<red>Invalid region id."));
            return;
        }
        if (ServerRegionManager.getInstance().get(player.getWorld(), id) != null) {
            player.sendMessage(Utils.fromString("<red>A region named '" + id + "' already exists. Use /smpregion merge to add shapes to it."));
            return;
        }

        RegionSelection selection = selection(player);
        if (selection == null) return;

        ServerRegionManager.getInstance().merge(player.getWorld(), id, selection);
        ServerRegionManager.getInstance().saveChanges(player.getWorld());
        RegionSelections.getInstance().clear(player);
        player.sendMessage(Utils.fromString("<green>Region '" + id + "' defined."));
    }

    private static void withRegion(Player player, String id, Consumer<String> change) {
        if (ServerRegionManager.getInstance().get(player.getWorld(), id) == null) {
            player.sendMessage(Utils.fromString("<red>No region named '" + id + "' in this world."));
            return;
        }

        change.accept(id);
        ServerRegionManager.getInstance().saveChanges(player.getWorld());
        player.sendMessage(Utils.fromString("<green>Region '" + id + "' updated."));
    }

    private static @Nullable RegionSelection selection(Player player) {
        RegionSelection selection = RegionSelections.getInstance().current(player);
        if (selection == null || !selection.isValid()) {
            player.sendMessage(Utils.fromString("<red>Select at least two corners with the region wand (or WorldEdit) first."));
            return null;
        }
        return selection;
    }
}
