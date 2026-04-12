package com.roguesmp.dungeon_v2.actor.command;


import com.roguesmp.dungeon_v2.controller_.SchemetaController;
import com.roguesmp.dungeon_v2.data.definition.Schemeta;
import com.roguesmp.dungeon_v2.manager.SchemetaManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;

public class SchemetaCommand {

    private final SchemetaController schemetaController;
    private final SchemetaManager schemetaManager;

    public SchemetaCommand(SchemetaController schemetaController, SchemetaManager schemetaManager) {
        this.schemetaController = schemetaController;
        this.schemetaManager = schemetaManager;
    }


    public void register() {

        new CommandAPICommand("schemeta")
                .withSubcommand(
                        new CommandAPICommand("save")
                                .withArguments(new StringArgument("name"))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.get("name");
                                    schemetaController.handleCreateSchemeta(player, name);
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("paste")
                                .withArguments(
                                        new StringArgument("id")
                                                .replaceSuggestions(ArgumentSuggestions.strings(
                                                        info -> schemetaManager.getAll().stream()
                                                                .map(Schemeta::getId)
                                                                .toArray(String[]::new)
                                                ))
                                )
                                .executesPlayer((player, args) -> {
                                        String id = (String) args.get("id");
                                        schemetaController.handleBuildSchema(player, id);
                                })
                )
//                .withSubcommand(
//                        new CommandAPICommand("give")
//                                .withSubcommand(
//                                        new CommandAPICommand("next_door").executesPlayer((player, args) -> {
//                                            ItemStack item = new ItemStack(Material.VAULT);
//                                            ItemMeta meta = item.getItemMeta();
//                                            meta.displayName(Component.text("Next Door"));
//                                            meta.getPersistentDataContainer().set(NameSpaceKeys.NEXT_DOOR_KEY, PersistentDataType.STRING, DungeonDoorType.NEXTDOOR.getType());
//                                            item.setItemMeta(meta);
//                                            player.getInventory().addItem(item);
//                                            player.sendMessage("Give Next Door");
//                                        })
//                                )
//                                .withSubcommand(
//                                        new CommandAPICommand("end_door").executesPlayer((player, args) -> {
//                                            ItemStack item = new ItemStack(Material.LODESTONE);
//                                            ItemMeta meta = item.getItemMeta();
//                                            meta.displayName(Component.text("End Door"));
//                                            meta.getPersistentDataContainer().set(NameSpaceKeys.END_DOOR_KEY, PersistentDataType.STRING, DungeonDoorType.ENDDOOR.getType());
//                                            item.setItemMeta(meta);
//                                            player.getInventory().addItem(item);
//                                            player.sendMessage("Give End Door");
//                                        })
//                                )
//                )
                .register();
    }
}
