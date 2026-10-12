package com.roguesmp.command;

import com.roguesmp.permission.Permissions;
import com.roguesmp.utils.Utils;
import com.roguesmp.world.PolarTemplateManager;
import com.roguesmp.world.PolarWorldKey;
import com.roguesmp.world.PolarWorldManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import static com.roguesmp.command.CommandFeedback.report;

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
                    worlds.storedKeys().thenAccept(keys -> Utils.runLater(() -> {
                        if (keys.isEmpty()) {
                            sender.sendMessage(Utils.fromString("<yellow>No template worlds exist yet."));
                            return;
                        }
                        for (PolarWorldKey key : keys) {
                            World world = worlds.getLoaded(key);
                            String state = world != null ? "<green>loaded (" + world.getPlayers().size() + " players)" : "<gray>unloaded";
                            sender.sendMessage(Utils.fromString("<yellow>" + key + " " + state));
                        }
                    }));
                });
    }

    private static CommandAPICommand createSub() {
        return new CommandAPICommand("create")
                .withArguments(new StringArgument("name"))
                .withOptionalArguments(worldArgument("template"))
                .executes((sender, args) -> {
                    PolarWorldKey key = key(sender, (String) args.get("name"));
                    if (key == null) return;

                    PolarWorldKey template = null;
                    if (args.getOptional("template").isPresent()) {
                        template = key(sender, (String) args.get("template"));
                        if (template == null) return;
                    }
                    report(sender, PolarTemplateManager.getInstance().create(key, template), "<green>Created template world <yellow>" + key);
                });
    }

    private static CommandAPICommand copySub() {
        return new CommandAPICommand("copy")
                .withArguments(worldArgument("source"), new StringArgument("target"))
                .executes((sender, args) -> {
                    PolarWorldKey source = key(sender, (String) args.get("source"));
                    PolarWorldKey target = key(sender, (String) args.get("target"));
                    if (source == null || target == null) return;

                    sender.sendMessage(Utils.fromString("<yellow>Copying the saved state of " + source + "..."));
                    report(sender, PolarTemplateManager.getInstance().copy(source, target), "<green>Copied <yellow>" + source + "<green> to <yellow>" + target);
                });
    }

    private static CommandAPICommand deleteSub() {
        return new CommandAPICommand("delete")
                .withArguments(worldArgument("world"))
                .withOptionalArguments(new StringArgument(CONFIRM).replaceSuggestions(ArgumentSuggestions.strings(CONFIRM)))
                .executes((sender, args) -> {
                    PolarWorldKey key = key(sender, (String) args.get("world"));
                    if (key == null) return;

                    if (!CONFIRM.equals(args.getOptional(CONFIRM).orElse(null))) {
                        sender.sendMessage(Utils.fromString("<red>This permanently deletes " + key + ". Run <yellow>/polartemplate delete " + key.name() + " confirm<red> to proceed."));
                        return;
                    }
                    report(sender, PolarTemplateManager.getInstance().delete(key), "<green>Deleted template world <yellow>" + key);
                });
    }

    private static CommandAPICommand loadSub() {
        return new CommandAPICommand("load")
                .withArguments(worldArgument("world"))
                .executes((sender, args) -> {
                    PolarWorldKey key = key(sender, (String) args.get("world"));
                    if (key == null) return;

                    report(sender, PolarTemplateManager.getInstance().load(key), "<green>Loaded template world <yellow>" + key);
                });
    }

    private static CommandAPICommand unloadSub() {
        return new CommandAPICommand("unload")
                .withArguments(worldArgument("world"))
                .withOptionalArguments(new BooleanArgument("save"))
                .executes((sender, args) -> {
                    PolarWorldKey key = key(sender, (String) args.get("world"));
                    if (key == null) return;

                    boolean save = (boolean) args.getOptional("save").orElse(false);
                    report(sender, PolarTemplateManager.getInstance().unload(key, save), "<green>Unloaded template world <yellow>" + key
                            + (save ? "<green> (saved)" : "<green> (changes discarded)"));
                });
    }

    private static CommandAPICommand saveSub() {
        return new CommandAPICommand("save")
                .withArguments(worldArgument("world"))
                .executesPlayer((player, args) -> {
                    PolarWorldKey name = key(player, (String) args.get("world"));
                    if (name == null) return;

                    PolarTemplateManager.getInstance().saveWorld(player.getWorld(), name).whenComplete((key, error) -> Utils.runLater(() -> {
                        if (error != null) CommandFeedback.error(player, error);
                        else player.sendMessage(Utils.fromString("<green>Saved template world <yellow>" + key));
                    }));
                });
    }

    private static CommandAPICommand teleportSub() {
        return new CommandAPICommand("tp")
                .withArguments(worldArgument("world"))
                .executesPlayer((player, args) -> {
                    PolarWorldKey key = key(player, (String) args.get("world"));
                    if (key == null) return;

                    World world = PolarTemplateManager.getInstance().worlds().getLoaded(key);
                    if (world == null) {
                        player.sendMessage(Utils.fromString("<red>World " + key + " is not loaded. Use /polartemplate load first."));
                        return;
                    }
                    player.teleportAsync(world.getSpawnLocation());
                });
    }

    private static CommandAPICommand setSpawnSub() {
        return new CommandAPICommand("setspawn")
                .executesPlayer((player, args) -> {
                    PolarWorldKey key = PolarTemplateManager.getInstance().keyOf(player.getWorld());
                    if (key == null) {
                        player.sendMessage(Utils.fromString("<red>This is not a template world."));
                        return;
                    }
                    report(player, PolarTemplateManager.getInstance().setSpawn(key, player.getLocation()),
                            "<green>Spawn of <yellow>" + key + "<green> set to your location.");
                });
    }

    private static CommandAPICommand borderSub() {
        return new CommandAPICommand("border")
                .withArguments(worldArgument("world"), new DoubleArgument("diameter", 0))
                .executes((sender, args) -> {
                    PolarWorldKey key = key(sender, (String) args.get("world"));
                    if (key == null) return;

                    double diameter = (double) args.get("diameter");
                    report(sender, PolarTemplateManager.getInstance().setBorder(key, diameter),
                            "<green>Border of <yellow>" + key + "<green> set to " + (diameter > 0 ? diameter : "none") + ".");
                });
    }

    private static Argument<String> worldArgument(String nodeName) {
        return new StringArgument(nodeName).replaceSuggestions(ArgumentSuggestions.stringsAsync(
                info -> PolarTemplateManager.getInstance().worlds().storedKeys()
                        .thenApply(keys -> keys.stream().map(PolarWorldKey::name).toArray(String[]::new))));
    }

    private static @Nullable PolarWorldKey key(CommandSender sender, String name) {
        try {
            return PolarTemplateManager.getInstance().worlds().key(name);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Utils.fromString("<red>" + Utils.MINI_MESSAGE.escapeTags(e.getMessage())));
            return null;
        }
    }

}
