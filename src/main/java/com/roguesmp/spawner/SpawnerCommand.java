package com.roguesmp.spawner;

import com.roguesmp.constant.Keys;
import com.roguesmp.permission.Permissions;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class SpawnerCommand {

    private SpawnerCommand() {
    }

    public static void register() {
        new CommandAPICommand("spawner")
                .withPermission(Permissions.SPAWNER_ADMIN.node())
                .withSubcommand(new CommandAPICommand("give")
                        .withArguments(new TextArgument("template").replaceSuggestions(ArgumentSuggestions.strings(
                                info -> SpawnerSystem.getInstance().templates().getAllIds().toArray(String[]::new))))
                        .executesPlayer((player, args) -> {
                            String templateId = (String) args.get("template");
                            Spawner template = SpawnerSystem.getInstance().templates().getById(templateId);
                            if (template == null) {
                                player.sendMessage(Utils.fromString("<red>Không tìm thấy mẫu spawner " + templateId + "."));
                                return;
                            }
                            ItemStack item = ItemStack.of(Material.SPAWNER);
                            String name = template.getName() != null ? template.getName() : templateId;
                            item.setData(DataComponentTypes.ITEM_NAME, Utils.fromString("<gold>Spawner: " + name));
                            item.editPersistentDataContainer(pdc -> pdc.set(Keys.SPAWNER_TID, PersistentDataType.STRING, templateId));
                            PlayerUtils.giveItem(player, item);
                        }))
                .register();
    }
}
