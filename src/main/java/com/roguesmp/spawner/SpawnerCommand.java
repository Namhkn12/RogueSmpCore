package com.roguesmp.spawner;

import com.roguesmp.gui.spawner.SpawnerBrowserGui;
import com.roguesmp.permission.Permissions;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;

public final class SpawnerCommand {

    private SpawnerCommand() {
    }

    public static void register() {
        new CommandAPICommand("spawner")
                .withPermission(Permissions.SPAWNER_ADMIN.node())
                .withSubcommand(new CommandAPICommand("give")
                        .withArguments(new TextArgument("template").replaceSuggestions(ArgumentSuggestions.strings(
                                info -> Registries.SPAWNER.getAll().keySet().toArray(String[]::new))))
                        .executesPlayer((player, args) -> {
                            String templateId = (String) args.get("template");
                            Spawner template = Registries.SPAWNER.get(templateId);
                            if (template == null) {
                                player.sendMessage(Utils.fromString("<red>Không tìm thấy mẫu spawner " + templateId + "."));
                                return;
                            }
                            PlayerUtils.giveItem(player, template.toItem());
                        }))
                .withSubcommand(new CommandAPICommand("browse")
                        .executesPlayer((player, args) -> {
                            new SpawnerBrowserGui(player).showInventory(player);
                        }))
                .register();
    }
}
