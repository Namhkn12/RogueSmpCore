package com.roguesmp.command;

import com.roguesmp.permission.Permissions;
import com.roguesmp.utils.Utils;
import com.roguesmp.world.PolarTemplateManager;
import com.roguesmp.world.PolarWorldManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class PolarTemplateCommand {

    private static final String CONFIRM = "confirm";

    private PolarTemplateCommand() {
    }

    public static void register() {
        new CommandAPICommand("polartemplate")
                .withAliases("pt")
                .withPermission(Permissions.POLAR_TEMPLATE_ADMIN.node())
                .withSubcommand(listSub())
                .withSubcommand(createSub())
                .withSubcommand(copySub())
                .withSubcommand(deleteSub())
                .withSubcommand(loadSub())
                .withSubcommand(unloadSub())
                .withSubcommand(saveSub())
                .withSubcommand(teleportSub())
                .withSubcommand(setSpawnSub())
                .withSubcommand(borderSub())
                .register();
    }

    private static CommandAPICommand listSub() {
        return new CommandAPICommand("list")
                .executes((sender, args) -> {
                    PolarWorldManager worlds = PolarTemplateManager.getInstance().worlds();
                    worlds.storedNames().thenAccept(names -> Utils.runLater(() -> {
                        if (names.isEmpty()) {
                            sender.sendMessage(Utils.fromString("<yellow>No template worlds exist yet."));
                            return;
                        }
                        for (String name : names) {
                            World world = worlds.getLoaded(name);
                            String state = world != null ? "<green>loaded (" + world.getPlayers().size() + " players)" : "<gray>unloaded";
                            sender.sendMessage(Utils.fromString("<yellow>" + name + " " + state));
                        }
                    }));
                });
    }

    private static CommandAPICommand createSub() {
        return new CommandAPICommand("create")
                .withArguments(new StringArgument("name"))
                .withOptionalArguments(worldArgument("template"))
                .executes((sender, args) -> {
                    String name = (String) args.get("name");
                    String template = (String) args.getOptional("template").orElse(null);
                    report(sender, PolarTemplateManager.getInstance().create(name, template), "<green>Created template world <yellow>" + name);
                });
    }

    private static CommandAPICommand copySub() {
        return new CommandAPICommand("copy")
                .withArguments(worldArgument("source"), new StringArgument("target"))
                .executes((sender, args) -> {
                    String source = (String) args.get("source");
                    String target = (String) args.get("target");
                    sender.sendMessage(Utils.fromString("<yellow>Copying the saved state of " + source + "..."));
                    report(sender, PolarTemplateManager.getInstance().copy(source, target), "<green>Copied <yellow>" + source + "<green> to <yellow>" + target);
                });
    }

    private static CommandAPICommand deleteSub() {
        return new CommandAPICommand("delete")
                .withArguments(worldArgument("world"))
                .withOptionalArguments(new StringArgument(CONFIRM).replaceSuggestions(ArgumentSuggestions.strings(CONFIRM)))
                .executes((sender, args) -> {
                    String name = (String) args.get("world");
                    if (!CONFIRM.equals(args.getOptional(CONFIRM).orElse(null))) {
                        sender.sendMessage(Utils.fromString("<red>This permanently deletes " + name + ". Run <yellow>/polartemplate delete " + name + " confirm<red> to proceed."));
                        return;
                    }
                    report(sender, PolarTemplateManager.getInstance().delete(name), "<green>Deleted template world <yellow>" + name);
                });
    }

    private static CommandAPICommand loadSub() {
        return new CommandAPICommand("load")
                .withArguments(worldArgument("world"))
                .executes((sender, args) -> {
                    String name = (String) args.get("world");
                    report(sender, PolarTemplateManager.getInstance().load(name), "<green>Loaded template world <yellow>" + name);
                });
    }

    private static CommandAPICommand unloadSub() {
        return new CommandAPICommand("unload")
                .withArguments(worldArgument("world"))
                .withOptionalArguments(new BooleanArgument("save"))
                .executes((sender, args) -> {
                    String name = (String) args.get("world");
                    boolean save = (boolean) args.getOptional("save").orElse(false);
                    report(sender, PolarTemplateManager.getInstance().unload(name, save), "<green>Unloaded template world <yellow>" + name
                            + (save ? "<green> (saved)" : "<green> (changes discarded)"));
                });
    }

    private static CommandAPICommand saveSub() {
        return new CommandAPICommand("save")
                .withArguments(worldArgument("world"))
                .executes((sender, args) -> {
                    String name = (String) args.get("world");
                    report(sender, PolarTemplateManager.getInstance().save(name), "<green>Saved template world <yellow>" + name);
                });
    }

    private static CommandAPICommand teleportSub() {
        return new CommandAPICommand("tp")
                .withArguments(worldArgument("world"))
                .executesPlayer((player, args) -> {
                    String name = (String) args.get("world");
                    World world = PolarTemplateManager.getInstance().worlds().getLoaded(name);
                    if (world == null) {
                        player.sendMessage(Utils.fromString("<red>World " + name + " is not loaded. Use /polartemplate load first."));
                        return;
                    }
                    player.teleportAsync(world.getSpawnLocation());
                });
    }

    private static CommandAPICommand setSpawnSub() {
        return new CommandAPICommand("setspawn")
                .executesPlayer((player, args) -> {
                    String name = PolarTemplateManager.getInstance().nameOf(player.getWorld());
                    if (name == null) {
                        player.sendMessage(Utils.fromString("<red>This is not a template world."));
                        return;
                    }
                    report(player, PolarTemplateManager.getInstance().setSpawn(name, player.getLocation()),
                            "<green>Spawn of <yellow>" + name + "<green> set to your location.");
                });
    }

    private static CommandAPICommand borderSub() {
        return new CommandAPICommand("border")
                .withArguments(worldArgument("world"), new DoubleArgument("diameter", 0))
                .executes((sender, args) -> {
                    String name = (String) args.get("world");
                    double diameter = (double) args.get("diameter");
                    report(sender, PolarTemplateManager.getInstance().setBorder(name, diameter),
                            "<green>Border of <yellow>" + name + "<green> set to " + (diameter > 0 ? diameter : "none") + ".");
                });
    }

    private static Argument<String> worldArgument(String nodeName) {
        return new StringArgument(nodeName).replaceSuggestions(ArgumentSuggestions.stringsAsync(
                info -> PolarTemplateManager.getInstance().worlds().storedNames().thenApply(names -> names.toArray(String[]::new))));
    }

    private static <T> void report(CommandSender sender, CompletableFuture<T> future, String success) {
        future.whenComplete((value, error) -> Utils.runLater(() -> {
            if (error == null) {
                sender.sendMessage(Utils.fromString(success));
                return;
            }
            Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            sender.sendMessage(Utils.fromString("<red>" + Utils.MINI_MESSAGE.escapeTags(String.valueOf(cause.getMessage()))));
        }));
    }
}
