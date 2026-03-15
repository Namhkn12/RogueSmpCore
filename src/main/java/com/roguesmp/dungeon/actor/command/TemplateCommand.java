package com.roguesmp.dungeon.actor.command;

import com.roguesmp.dungeon.controller.TemplateController;
import com.roguesmp.dungeon.controller.response.ControllerResponse;
import com.roguesmp.dungeon.data.Dungeon;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;

import java.util.Collections;
import java.util.List;

public class TemplateCommand {

    private final TemplateController templateController;

    public TemplateCommand(TemplateController templateController) {
        this.templateController = templateController;
    }

    public void register() {

        new CommandAPICommand("template")
                .withSubcommand(createSub())
                .withSubcommand(deleteSub())
                .withSubcommand(listSub())
                .register();
    }

    private CommandAPICommand createSub() {
        return new CommandAPICommand("create")
                .withArguments(new StringArgument("name"))
                .executesPlayer((player, args) -> {
                    String name = (String) args.get("name");

                    ControllerResponse<Dungeon> result = templateController.createTemplate(name, Collections.emptyList());

                    if (result.isSuccess()) {
                        player.sendMessage("§aDungeon template created! §eID: " + result.getData().getDgId());
                    } else {
                        player.sendMessage("§c" + result.getMessage());
                    }
                });
    }

    private CommandAPICommand deleteSub() {
        return new CommandAPICommand("delete")
                .withArguments(
                        new StringArgument("id")
                                .replaceSuggestions(ArgumentSuggestions.strings(info -> {
                                    ControllerResponse<List<String>> result = templateController.loadTemplateIdList();
                                    if (result.getData() == null) return new String[0];
                                    return result.getData().toArray(new String[0]);
                                }))
                )
                .executesPlayer((player, args) -> {
                    String id = (String) args.get("id");

                    ControllerResponse<Void> result = templateController.deleteTemplate(id);

                    if (result.isSuccess()) {
                        player.sendMessage("§aDungeon template deleted: §e" + id);
                    } else {
                        player.sendMessage("§c" + result.getMessage());
                    }
                });
    }

    private CommandAPICommand listSub() {
        return new CommandAPICommand("list")
                .executesPlayer((player, args) -> {
                    ControllerResponse<java.util.List<String>> result = templateController.loadTemplateIdList();

                    if (result.isSuccess()) {
                        player.sendMessage("§6=== Dungeon Templates ===");
                        result.getData().forEach(id -> player.sendMessage("§7- §e" + id));
                    } else {
                        player.sendMessage("§c" + result.getMessage());
                    }
                });
    }
}