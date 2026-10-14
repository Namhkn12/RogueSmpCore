package com.roguesmp.dungeon.build;

import com.roguesmp.command.CommandFeedback;
import com.roguesmp.constant.Keys;
import com.roguesmp.dungeon.feature.RewardChestsFeature;
import com.roguesmp.dungeon.room.RoomSchematics;
import com.roguesmp.loot.LootTable;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.permission.Permissions;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.stream.Stream;

public final class DungeonBuildCommand {

    private DungeonBuildCommand() {
    }

    public static void register() {
        new CommandAPICommand("dungeonbuild")
                .withAliases("dbuild")
                .withPermission(Permissions.DUNGEON_BUILD.node())
                .withSubcommand(new CommandAPICommand("schematic")
                        .withSubcommand(new CommandAPICommand("save")
                                .withArguments(new StringArgument("name"))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.get("name");
                                    try {
                                        RoomSchematics.save(player, name);
                                    } catch (IllegalStateException e) {
                                        CommandFeedback.error(player, e);
                                        return;
                                    }
                                    player.sendMessage(Utils.fromString("<green>Đã lưu schematic <yellow>" + name + "<green>, gốc dán tại vị trí của bạn."));
                                }))
                        .withSubcommand(new CommandAPICommand("paste")
                                .withArguments(new StringArgument("name").replaceSuggestions(ArgumentSuggestions.strings(info -> RoomSchematics.names().toArray(String[]::new))))
                                .executesPlayer((player, args) -> {
                                    try {
                                        RoomSchematics.paste((String) args.get("name"), player.getLocation());
                                    } catch (IllegalStateException e) {
                                        CommandFeedback.error(player, e);
                                    }
                                })))
                .withSubcommand(new CommandAPICommand("rewardchest")
                        .withArguments(new TextArgument("table").replaceSuggestions(ArgumentSuggestions.strings(info -> Stream.concat(
                                Stream.of(RewardChestsFeature.DUNGEON_TABLE), Registries.LOOT_TABLE.getAll().keySet().stream().sorted()).toArray(String[]::new))))
                        .executesPlayer((player, args) -> {
                            String table = (String) args.get("table");
                            ItemStack item = ItemStack.of(Material.CHEST);
                            item.setData(DataComponentTypes.ITEM_NAME, Utils.fromString("<gold>Rương thưởng: <white>" + table));
                            item.editPersistentDataContainer(pdc -> pdc.set(Keys.REWARD_CID, PersistentDataType.STRING, table));
                            PlayerUtils.giveItem(player, item);
                        }))
                .withSubcommand(new CommandAPICommand("loottable")
                        .withArguments(new TextArgument("table").replaceSuggestions(ArgumentSuggestions.strings(
                                info -> Registries.LOOT_TABLE.getAll().keySet().stream().sorted().toArray(String[]::new))))
                        .executesPlayer((player, args) -> {
                            String tableId = (String) args.get("table");
                            LootTable table = Registries.LOOT_TABLE.get(tableId);
                            SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
                            if (table == null || smpPlayer == null) {
                                player.sendMessage(Utils.fromString("<red>Không tìm thấy bảng loot " + tableId + "."));
                                return;
                            }
                            List<ItemStack> items = table.roll(LootContext.builder(smpPlayer).build());
                            player.sendMessage(Utils.fromString("<green>Đã roll <yellow>" + tableId + "<green>: " + items.size() + " vật phẩm."));
                            PlayerUtils.giveItem(player, items);
                        }))
                .register();
    }
}
