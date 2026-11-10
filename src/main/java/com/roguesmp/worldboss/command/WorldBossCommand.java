package com.roguesmp.worldboss.command;

import com.roguesmp.GlobalConfig;
import com.roguesmp.permission.Permissions;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.DateUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.worldboss.WorldBossManager;
import com.roguesmp.worldboss.definition.PeriodicSpawn;
import com.roguesmp.worldboss.definition.WorldBossDefinition;
import com.roguesmp.worldboss.gui.WorldBossSummonGui;
import com.roguesmp.worldboss.runtime.OpenResult;
import com.roguesmp.worldboss.runtime.WorldBossInstance;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;
import org.bukkit.command.CommandSender;

import java.util.stream.Stream;

public final class WorldBossCommand {

    private static final String ALL = "all";

    private WorldBossCommand() {
    }

    public static void register() {
        new CommandAPICommand("worldboss")
                .withPermission(Permissions.WORLD_BOSS_ADMIN.node())
                .withSubcommand(new CommandAPICommand("start")
                        .withArguments(bossArgument())
                        .executes((sender, args) -> {
                            WorldBossDefinition definition = Registries.WORLD_BOSS.get((String) args.get("boss"));
                            if (definition == null) {
                                unknown(sender, (String) args.get("boss"));
                                return;
                            }
                            OpenResult result = WorldBossManager.getInstance().open(definition, null, true);
                            sender.sendMessage(Utils.fromString((result == OpenResult.OPENED ? "<green>" : "<red>") + result.name()));
                        }))
                .withSubcommand(new CommandAPICommand("cancel")
                        .withArguments(bossArgument())
                        .executes((sender, args) -> {
                            boolean canceled = WorldBossManager.getInstance().cancel((String) args.get("boss"));
                            sender.sendMessage(Utils.fromString(canceled ? "<green>Canceled." : "<red>That boss is not active."));
                        }))
                .withSubcommand(new CommandAPICommand("gui")
                        .withArguments(bossArgument())
                        .executesPlayer((player, args) -> {
                            WorldBossSummonGui.open(player, (String) args.get("boss"));
                        }))
                .withSubcommand(toggleCommand("disable", true))
                .withSubcommand(toggleCommand("enable", false))
                .withSubcommand(new CommandAPICommand("list")
                        .executes((sender, args) -> {
                            Registries.WORLD_BOSS.getAll().values().forEach(definition -> sender.sendMessage(Utils.fromString(describe(definition))));
                        }))
                .register();
    }

    private static CommandAPICommand toggleCommand(String name, boolean disabled) {
        return new CommandAPICommand(name)
                .withArguments(new TextArgument("boss").replaceSuggestions(ArgumentSuggestions.strings(info ->
                        Stream.concat(Stream.of(ALL), Registries.WORLD_BOSS.getAll().keySet().stream()).toArray(String[]::new))))
                .executes((sender, args) -> {
                    String id = (String) args.get("boss");
                    if (!id.equals(ALL) && Registries.WORLD_BOSS.get(id) == null) {
                        unknown(sender, id);
                        return;
                    }
                    WorldBossManager.getInstance().setDisabled(id.equals(ALL) ? GlobalConfig.ALL_WORLD_BOSSES : id, disabled);
                    sender.sendMessage(Utils.fromString((disabled ? "<red>Disabled " : "<green>Enabled ")
                            + (id.equals(ALL) ? "all world bosses" : "'" + Utils.MINI_MESSAGE.escapeTags(id) + "'") + "."));
                });
    }

    private static String describe(WorldBossDefinition definition) {
        WorldBossManager manager = WorldBossManager.getInstance();
        WorldBossInstance active = manager.getActive(definition.id());
        String status;
        if (active != null) {
            status = "<red>" + active.getState().name() + " (" + active.getSecondsLeft() + "s, "
                    + active.getAliveParticipants().size() + " alive)";
        } else if (definition.spawn() instanceof PeriodicSpawn) {
            Long next = manager.getNextSpawnAt(definition.id());
            status = next == null ? "<gray>not scheduled" : "<yellow>next in " + DateUtils.formatRemaining(next - System.currentTimeMillis());
        } else {
            long cooldown = manager.getCooldownRemaining(definition.id());
            status = cooldown > 0 ? "<yellow>cooldown " + DateUtils.formatRemaining(cooldown) : "<green>ready";
        }
        String disabled = manager.isDisabled(definition.id()) ? " <dark_red>[disabled]" : "";
        return "<gold>" + definition.id() + " <gray>[" + definition.spawn().getTypeId() + "] " + status + disabled;
    }

    private static void unknown(CommandSender sender, String id) {
        sender.sendMessage(Utils.fromString("<red>Unknown world boss '" + Utils.MINI_MESSAGE.escapeTags(id) + "'."));
    }

    private static Argument<String> bossArgument() {
        return new TextArgument("boss").replaceSuggestions(ArgumentSuggestions.strings(
                info -> Registries.WORLD_BOSS.getAll().keySet().toArray(String[]::new)));
    }
}
