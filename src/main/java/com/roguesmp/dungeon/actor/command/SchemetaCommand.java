package com.roguesmp.dungeon.actor.command;

import com.roguesmp.dungeon.constant.DungeonDoorType;
import com.roguesmp.dungeon.controller.BuildingController;
import com.roguesmp.dungeon.exception.BaseException;
import com.roguesmp.dungeon.exception.GlobalException;
import com.roguesmp.dungeon.exception.impl.schemeta.SchemetaNotFoundException;
import com.roguesmp.dungeon.exception.impl.schemeta.SelectionNotFoundException;
import com.roguesmp.dungeon.utils.NameSpaceKeys;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class SchemetaCommand {

    private final BuildingController buildingController;

    public SchemetaCommand(BuildingController buildingController) {
        this.buildingController = buildingController;
    }

    public void register() {

        new CommandAPICommand("schemeta")
                .withSubcommand(
                        new CommandAPICommand("save")
                                .withArguments(new StringArgument("name"))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.get("name");
                                    try {
                                        buildingController.createNewSchematic(player, name);
                                        player.sendMessage("§aSchemeta saved: §e" + name);
                                    } catch (SelectionNotFoundException e) {
                                        GlobalException.handleAndNotify(e, player);
                                    } catch (BaseException e) {
                                        GlobalException.handleAndNotify(e, player);
                                    } catch (Exception e) {
                                        GlobalException.handleAndNotifyUnexpected(
                                                "schemeta save command", e, player, "Khong the luu schemeta");
                                    }
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("paste")
                                .withArguments(
                                        new StringArgument("id")
                                                .replaceSuggestions(ArgumentSuggestions.strings(
                                                        info -> buildingController.getSchematicIdList().toArray(new String[0])
                                                ))
                                )
                                .executesPlayer((player, args) -> {
                                    String id = (String) args.get("id");
                                    try {
                                        buildingController.buildSchematicById(id, player.getLocation());
                                        player.sendMessage("§aPasted: §e" + id);
                                    } catch (SchemetaNotFoundException e) {
                                        GlobalException.handleAndNotify(e, player);
                                    } catch (BaseException e) {
                                        GlobalException.handleAndNotify(e, player);
                                    } catch (Exception e) {
                                        GlobalException.handleAndNotifyUnexpected(
                                                "schemeta paste command", e, player, "Khong the paste schemeta");
                                    }
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("give")
                                .withSubcommand(
                                        new CommandAPICommand("next_door").executesPlayer((player, args) -> {
                                            ItemStack item = new ItemStack(Material.VAULT);
                                            ItemMeta meta = item.getItemMeta();
                                            meta.displayName(Component.text("Next Door"));
                                            meta.getPersistentDataContainer().set(NameSpaceKeys.NEXT_DOOR_KEY, PersistentDataType.STRING, DungeonDoorType.NEXTDOOR.getType());
                                            item.setItemMeta(meta);
                                            player.getInventory().addItem(item);
                                            player.sendMessage("Give Next Door");
                                        })
                                )
                                .withSubcommand(
                                        new CommandAPICommand("end_door").executesPlayer((player, args) -> {
                                            ItemStack item = new ItemStack(Material.LODESTONE);
                                            ItemMeta meta = item.getItemMeta();
                                            meta.displayName(Component.text("End Door"));
                                            meta.getPersistentDataContainer().set(NameSpaceKeys.END_DOOR_KEY, PersistentDataType.STRING, DungeonDoorType.ENDDOOR.getType());
                                            item.setItemMeta(meta);
                                            player.getInventory().addItem(item);
                                            player.sendMessage("Give End Door");
                                        })
                                )
                )
                .register();
    }
}
