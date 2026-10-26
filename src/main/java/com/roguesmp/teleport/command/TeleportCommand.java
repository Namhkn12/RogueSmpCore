package com.roguesmp.teleport.command;

import com.roguesmp.codec.DataResult;
import com.roguesmp.command.CommandFeedback;
import com.roguesmp.permission.Permissions;
import com.roguesmp.teleport.TeleportDestination;
import com.roguesmp.teleport.TeleportDestinationParser;
import com.roguesmp.teleport.TeleportDestinationSuggestions;
import com.roguesmp.teleport.TeleportOptions;
import com.roguesmp.teleport.TeleportService;
import com.roguesmp.teleport.effect.NoTeleportEffect;
import com.roguesmp.teleport.effect.TeleportEffect;
import com.roguesmp.teleport.effect.TeleportEffects;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TeleportCommand {

    private static final Map<String, String> EFFECT_OPTIONS = new LinkedHashMap<>();

    static {
        TeleportEffects.PRESETS.keySet().forEach(name -> EFFECT_OPTIONS.put(name, "Teleport effect (instant if omitted)"));
    }

    private TeleportCommand() {
    }

    public static void register() {
        new CommandAPICommand("steleport")
                .withAliases("stp")
                .withPermission(Permissions.TELEPORT.node())
                .withArguments(destinationArgument())
                .executesPlayer((player, args) -> {
                    teleport(player, List.of(player), (String) args.get("destination"));
                })
                .withSubcommand(new CommandAPICommand("send")
                        .withArguments(new EntitySelectorArgument.ManyPlayers("targets"), destinationArgument())
                        .executes((sender, args) -> {
                            @SuppressWarnings("unchecked")
                            Collection<Player> targets = (Collection<Player>) args.get("targets");
                            teleport(sender, targets, (String) args.get("destination"));
                        }))
                .register();
    }

    private static void teleport(CommandSender sender, Collection<Player> targets, String input) {
        String[] tokens = input.trim().split("\\s+");
        String last = tokens[tokens.length - 1].toLowerCase();
        TeleportOptions options = TeleportOptions.INSTANT;
        TeleportEffect effect = TeleportEffects.PRESETS.get(last);
        if (tokens.length > 1 && effect != null) {
            if (!(effect instanceof NoTeleportEffect)) options = TeleportOptions.DEFAULT.withEffect(effect);
            tokens = Arrays.copyOf(tokens, tokens.length - 1);
        }

        DataResult<TeleportDestination> destination = TeleportDestinationParser.parse(tokens);
        if (CommandFeedback.failed(sender, destination)) return;

        String description = Utils.MINI_MESSAGE.escapeTags(destination.result().format());
        for (Player target : targets) {
            TeleportService.getInstance().teleport(target, destination.result(), options).thenAccept(teleported -> {
                if (!teleported) sender.sendMessage(Utils.fromString("<red>Could not teleport " + target.getName() + " to " + description + "."));
            });
        }
    }

    private static Argument<String> destinationArgument() {
        return new GreedyStringArgument("destination")
                .replaceSuggestions(ArgumentSuggestions.stringsWithTooltips(info ->
                        TeleportDestinationSuggestions.suggest(info.sender(), info.currentArg(), EFFECT_OPTIONS)));
    }
}
