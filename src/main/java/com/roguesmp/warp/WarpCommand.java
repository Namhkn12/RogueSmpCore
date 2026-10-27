package com.roguesmp.warp;

import com.roguesmp.codec.DataResult;
import com.roguesmp.command.CommandFeedback;
import com.roguesmp.permission.Permissions;
import com.roguesmp.registry.Registries;
import com.roguesmp.teleport.TeleportDestination;
import com.roguesmp.teleport.TeleportDestinationParser;
import com.roguesmp.teleport.TeleportDestinationSuggestions;
import com.roguesmp.teleport.destination.LocationDestination;
import com.roguesmp.utils.Utils;
import com.roguesmp.warp.gui.WarpAdminGui;
import com.roguesmp.warp.gui.WarpEditorGui;
import com.roguesmp.warp.gui.WarpGui;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.IStringTooltip;
import dev.jorel.commandapi.StringTooltip;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.stream.Stream;

public final class WarpCommand {

    private static final String HERE = "here";

    private WarpCommand() {
    }

    public static void register() {
        new CommandAPICommand("warp")
                .withOptionalArguments(usableWarpArgument())
                .executesPlayer((player, args) -> {
                    String id = (String) args.get("name");
                    if (id == null) new WarpGui(player).showInventory(player);
                    else WarpService.getInstance().teleport(player, id);
                })
                .withSubcommand(new CommandAPICommand("admin")
                        .withPermission(Permissions.WARP_ADMIN.node())
                        .executesPlayer((player, args) -> {
                            new WarpAdminGui(player).showInventory(player);
                        }))
                .withSubcommand(new CommandAPICommand("create")
                        .withPermission(Permissions.WARP_ADMIN.node())
                        .withArguments(new StringArgument("name"))
                        .executesPlayer((player, args) -> {
                            String id = ((String) args.get("name")).toLowerCase();
                            DataResult<Warp> result = WarpService.getInstance().create(id, LocationDestination.of(player.getLocation()));
                            if (CommandFeedback.failed(player, result)) return;
                            player.sendMessage(Utils.fromString("<green>Warp '" + id + "' created at your position."));
                        }))
                .withSubcommand(new CommandAPICommand("set")
                        .withPermission(Permissions.WARP_ADMIN.node())
                        .withArguments(adminWarpArgument(), destinationArgument())
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("name");
                            DataResult<TeleportDestination> destination = parseDestination(player, (String) args.get("destination"));
                            if (CommandFeedback.failed(player, destination)) return;
                            DataResult<Warp> result = WarpService.getInstance().update(id, warp -> warp.withDestination(destination.result()));
                            if (CommandFeedback.failed(player, result)) return;
                            player.sendMessage(Utils.fromString("<green>Warp '" + id + "' now goes to " + Utils.MINI_MESSAGE.escapeTags(destination.result().format()) + "."));
                        }))
                .withSubcommand(new CommandAPICommand("edit")
                        .withPermission(Permissions.WARP_ADMIN.node())
                        .withArguments(adminWarpArgument())
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("name");
                            if (Registries.WARP.get(id) == null) {
                                player.sendMessage(Utils.fromString("<red>Unknown warp '" + Utils.MINI_MESSAGE.escapeTags(id) + "'."));
                                return;
                            }
                            new WarpEditorGui(player, id).showInventory(player);
                        }))
                .withSubcommand(new CommandAPICommand("delete")
                        .withPermission(Permissions.WARP_ADMIN.node())
                        .withArguments(adminWarpArgument())
                        .executes((sender, args) -> {
                            String id = (String) args.get("name");
                            sender.sendMessage(Utils.fromString(WarpService.getInstance().delete(id)
                                    ? "<yellow>Warp '" + id + "' deleted."
                                    : "<red>Unknown warp '" + Utils.MINI_MESSAGE.escapeTags(id) + "'."));
                        }))
                .register();
    }

    private static DataResult<TeleportDestination> parseDestination(Player player, String text) {
        if (text.trim().equalsIgnoreCase(HERE)) return DataResult.success(LocationDestination.of(player.getLocation()));
        return TeleportDestinationParser.parse(text);
    }

    private static Argument<String> usableWarpArgument() {
        return new StringArgument("name")
                .replaceSuggestions(ArgumentSuggestions.strings(info -> info.sender() instanceof Player player
                        ? WarpService.getInstance().usableWarps(player).stream().map(entry -> entry.getKey()).toArray(String[]::new)
                        : Registries.WARP.getAll().keySet().toArray(String[]::new)));
    }

    private static Argument<String> adminWarpArgument() {
        return new StringArgument("name")
                .replaceSuggestions(ArgumentSuggestions.strings(info -> Registries.WARP.getAll().keySet().toArray(String[]::new)));
    }

    private static Argument<String> destinationArgument() {
        return new GreedyStringArgument("destination")
                .replaceSuggestions(ArgumentSuggestions.stringsWithTooltips(info -> {
                    String input = info.currentArg();
                    IStringTooltip[] suggestions = TeleportDestinationSuggestions.suggest(info.sender(), input, Map.of());
                    if (input.contains(" ") || !HERE.startsWith(input.toLowerCase())) return suggestions;
                    return Stream.concat(Stream.of(StringTooltip.ofString(HERE, "Your current position")), Stream.of(suggestions))
                            .toArray(IStringTooltip[]::new);
                }));
    }
}
