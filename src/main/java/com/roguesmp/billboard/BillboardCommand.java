package com.roguesmp.billboard;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.billboard.gui.BillboardBrowserGui;
import com.roguesmp.billboard.gui.BillboardEditorGui;
import com.roguesmp.billboard.line.BillboardLine;
import com.roguesmp.billboard.line.TextLine;
import com.roguesmp.codec.DataResult;
import com.roguesmp.command.CommandFeedback;
import com.roguesmp.permission.Permissions;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public final class BillboardCommand {

    private static final double NEAR_RADIUS = 16;
    private static final double UNPLACE_RADIUS = 4;

    private BillboardCommand() {
    }

    public static void register() {
        new CommandAPICommand("billboard")
                .withPermission(Permissions.BILLBOARD_ADMIN.node())
                .executesPlayer((player, args) -> {
                    new BillboardBrowserGui(player).showInventory(player);
                })
                .withSubcommand(new CommandAPICommand("browser")
                        .withOptionalArguments(new GreedyStringArgument("query")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> Registries.BILLBOARD.getAll().keySet().toArray(String[]::new))))
                        .executesPlayer((player, args) -> {
                            new BillboardBrowserGui(player, (String) args.getOrDefault("query", "")).showInventory(player);
                        }))
                .withSubcommand(new CommandAPICommand("create")
                        .withArguments(new TextArgument("id"))
                        .executesPlayer((player, args) -> {
                            String id = ((String) args.get("id")).toLowerCase();
                            if (CommandFeedback.failed(player, BillboardManager.getInstance().create(id, player.getLocation()))) return;
                            player.sendMessage(Utils.fromString("<green>Billboard '" + id + "' created at your position."));
                        }))
                .withSubcommand(new CommandAPICommand("delete")
                        .withArguments(billboardArgument())
                        .executes((sender, args) -> {
                            String id = (String) args.get("id");
                            sender.sendMessage(Utils.fromString(BillboardManager.getInstance().delete(id)
                                    ? "<yellow>Billboard '" + id + "' deleted."
                                    : "<red>Unknown billboard '" + Utils.MINI_MESSAGE.escapeTags(id) + "'."));
                        }))
                .withSubcommand(new CommandAPICommand("edit")
                        .withArguments(billboardArgument())
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("id");
                            if (Registries.BILLBOARD.get(id) == null) {
                                player.sendMessage(Utils.fromString("<red>Unknown billboard '" + Utils.MINI_MESSAGE.escapeTags(id) + "'."));
                                return;
                            }
                            new BillboardEditorGui(player, id).showInventory(player);
                        }))
                .withSubcommand(new CommandAPICommand("nudge")
                        .withArguments(new DoubleArgument("x"), new DoubleArgument("y"), new DoubleArgument("z"))
                        .executesPlayer((player, args) -> {
                            Marker nearest = nearestPlaced(player);
                            if (nearest == null) return;
                            BillboardManager.getInstance().nudge(nearest, (double) args.get("x"), (double) args.get("y"), (double) args.get("z"));
                            player.sendMessage(Utils.fromString("<green>Nudged placed billboard '" + BillboardManager.placedId(nearest) + "'."));
                        }))
                .withSubcommand(new CommandAPICommand("addline")
                        .withArguments(billboardArgument(), new GreedyStringArgument("text"))
                        .executes((sender, args) -> {
                            String text = (String) args.get("text");
                            update(sender, (String) args.get("id"), billboard -> editLines(billboard, lines -> lines.add(TextLine.of(text))), "line added");
                        }))
                .withSubcommand(new CommandAPICommand("setline")
                        .withArguments(billboardArgument(), new IntegerArgument("line", 1), new GreedyStringArgument("text"))
                        .executes((sender, args) -> {
                            String id = (String) args.get("id");
                            int index = (int) args.get("line") - 1;
                            if (!checkLineIndex(sender, id, index)) return;
                            String text = (String) args.get("text");
                            update(sender, id, billboard -> editLines(billboard, lines -> lines.set(index, TextLine.of(text))), "line " + (index + 1) + " set");
                        }))
                .withSubcommand(new CommandAPICommand("removeline")
                        .withArguments(billboardArgument(), new IntegerArgument("line", 1))
                        .executes((sender, args) -> {
                            String id = (String) args.get("id");
                            int index = (int) args.get("line") - 1;
                            if (!checkLineIndex(sender, id, index)) return;
                            update(sender, id, billboard -> editLines(billboard, lines -> lines.remove(index)), "line " + (index + 1) + " removed");
                        }))
                .withSubcommand(new CommandAPICommand("place")
                        .withArguments(billboardArgument())
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("id");
                            if (Registries.BILLBOARD.get(id) == null) {
                                player.sendMessage(Utils.fromString("<red>Unknown billboard '" + Utils.MINI_MESSAGE.escapeTags(id) + "'."));
                                return;
                            }
                            BillboardManager.getInstance().place(id, player.getLocation());
                            player.sendMessage(Utils.fromString("<green>Placed billboard '" + id + "' at your position."));
                        }))
                .withSubcommand(new CommandAPICommand("unplace")
                        .executesPlayer((player, args) -> {
                            Marker nearest = nearestPlaced(player);
                            if (nearest == null) return;
                            String id = BillboardManager.placedId(nearest);
                            nearest.remove();
                            player.sendMessage(Utils.fromString("<yellow>Removed placed billboard '" + id + "'."));
                        }))
                .withSubcommand(new CommandAPICommand("near")
                        .executesPlayer((player, args) -> {
                            Location here = player.getLocation();
                            List<String> nearby = BillboardManager.getInstance().getPlaced().values().stream()
                                    .filter(entry -> entry.instance().getAnchor().getWorld().equals(here.getWorld()))
                                    .filter(entry -> entry.instance().getAnchor().distance(here) <= NEAR_RADIUS)
                                    .sorted(Comparator.comparingDouble(entry -> entry.instance().getAnchor().distance(here)))
                                    .map(entry -> String.format("<gray>- <white>%s <dark_gray>(%.1fm)", entry.id(), entry.instance().getAnchor().distance(here)))
                                    .toList();
                            if (nearby.isEmpty()) {
                                player.sendMessage(Utils.fromString("<gray>No billboards within " + (int) NEAR_RADIUS + " blocks."));
                                return;
                            }
                            nearby.forEach(entry -> player.sendMessage(Utils.fromString(entry)));
                        }))
                .withSubcommand(new CommandAPICommand("tp")
                        .withArguments(billboardArgument())
                        .executesPlayer((player, args) -> {
                            String id = (String) args.get("id");
                            Location here = player.getLocation();
                            BillboardManager.getInstance().copiesOf(id).stream()
                                    .map(entry -> entry.instance().getAnchor())
                                    .min(Comparator.comparingDouble(anchor -> anchor.getWorld().equals(here.getWorld()) ? anchor.distanceSquared(here) : Double.MAX_VALUE))
                                    .ifPresentOrElse(player::teleport,
                                            () -> player.sendMessage(Utils.fromString("<red>No loaded copy of '" + Utils.MINI_MESSAGE.escapeTags(id) + "'.")));
                        }))
                .withSubcommand(new CommandAPICommand("list")
                        .executes((sender, args) -> {
                            Registries.BILLBOARD.getAll().keySet().stream().sorted().forEach(id -> sender.sendMessage(Utils.fromString(
                                    "<gray>- <white>" + id + " <dark_gray>(" + BillboardManager.getInstance().copiesOf(id).size() + " loaded copies)")));
                        }))
                .withSubcommand(new CommandAPICommand("reload")
                        .executes((sender, args) -> {
                            RogueSmpCore.getInstance().reloadRegistry(Registries.BILLBOARD.getLocationKey());
                            sender.sendMessage(Utils.fromString("<green>Reloaded " + Registries.BILLBOARD.getAll().size() + " billboard(s)."));
                        }))
                .register();
    }

    private static void update(CommandSender sender, String id, UnaryOperator<Billboard> change, String message) {
        DataResult<Billboard> result = BillboardManager.getInstance().update(id, change);
        if (CommandFeedback.failed(sender, result)) return;
        sender.sendMessage(Utils.fromString("<green>Billboard '" + id + "' " + message + "."));
    }

    private static @org.jetbrains.annotations.Nullable Marker nearestPlaced(Player player) {
        Marker nearest = BillboardManager.nearestPlaced(player.getLocation(), UNPLACE_RADIUS, null);
        if (nearest == null) player.sendMessage(Utils.fromString("<red>No placed billboard within " + (int) UNPLACE_RADIUS + " blocks."));
        return nearest;
    }

    private static boolean checkLineIndex(CommandSender sender, String id, int index) {
        Billboard billboard = Registries.BILLBOARD.get(id);
        if (billboard == null) {
            sender.sendMessage(Utils.fromString("<red>Unknown billboard '" + Utils.MINI_MESSAGE.escapeTags(id) + "'."));
            return false;
        }
        if (index >= billboard.lines().size()) {
            sender.sendMessage(Utils.fromString("<red>Billboard '" + id + "' only has " + billboard.lines().size() + " line(s)."));
            return false;
        }
        return true;
    }

    private static Billboard editLines(Billboard billboard, Consumer<List<BillboardLine>> edit) {
        List<BillboardLine> lines = new ArrayList<>(billboard.lines());
        edit.accept(lines);
        return billboard.withLines(lines);
    }

    private static Argument<String> billboardArgument() {
        return new TextArgument("id")
                .replaceSuggestions(ArgumentSuggestions.strings(info -> Registries.BILLBOARD.getAll().keySet().toArray(String[]::new)));
    }
}
