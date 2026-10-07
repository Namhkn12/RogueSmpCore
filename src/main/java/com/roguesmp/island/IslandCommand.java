package com.roguesmp.island;

import com.roguesmp.gui.island.IslandMainGui;
import com.roguesmp.player.PlayerData;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class IslandCommand {

    private static final String ADMIN_PERMISSION = "roguesmp.island.admin";

    private final IslandManager islandManager;

    public IslandCommand(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    public void register() {
        new CommandAPICommand("island")
                .withAliases("is")
                .withSubcommand(createSub())
                .withSubcommand(teleportHomeSub("go"))
                .withSubcommand(teleportHomeSub("home"))
                .withSubcommand(inviteSub())
                .withSubcommand(acceptSub())
                .withSubcommand(denySub())
                .withSubcommand(visitSub())
                .withSubcommand(purgeSub())
                .executesPlayer((player, args) -> {
                    new IslandMainGui(player).showInventory(player);
                })
                .register();
    }

    private CommandAPICommand createSub() {
        return new CommandAPICommand("create")
                .executesPlayer((player, args) -> {
                    PlayerData playerData = islandManager.getPlayerManager().getDataManager().getData(player.getUniqueId());
                    if (playerData == null) return;

                    if (islandManager.getIslandDataManager().getCachedData(playerData.getIslandId()) != null) {
                        player.sendMessage(Utils.fromString("<red>Bạn đã có đảo rồi! Dùng <yellow>/is go <red>để dịch chuyển"));
                        return;
                    }

                    player.sendMessage(Utils.fromString("<green>Đang sắp xếp vị trí đảo cho bạn. Vui lòng chờ..."));
                    islandManager.createIsland(player);
                });
    }

    private CommandAPICommand teleportHomeSub(String name) {
        return new CommandAPICommand(name)
                .executesPlayer((player, args) -> {
                    player.sendMessage(Utils.fromString("<green>Đang dịch chuyển về đảo..."));
                    islandManager.getTeleportService().teleportHome(player);
                });
    }

    private CommandAPICommand inviteSub() {
        return new CommandAPICommand("invite")
                .withArguments(new EntitySelectorArgument.OnePlayer("target"))
                .executesPlayer((player, args) -> {
                    Player target = (Player) args.get(0);
                    if (target != null) islandManager.getInviteManager().sendInvitation(player, target);
                });
    }

    private CommandAPICommand acceptSub() {
        return new CommandAPICommand("accept")
                .withArguments(new EntitySelectorArgument.OnePlayer("sender"))
                .executesPlayer((player, args) -> {
                    Player sender = (Player) args.get(0);
                    if (sender != null) islandManager.getInviteManager().accept(player, sender);
                });
    }

    private CommandAPICommand denySub() {
        return new CommandAPICommand("deny")
                .withArguments(new EntitySelectorArgument.OnePlayer("sender"))
                .executesPlayer((player, args) -> {
                    Player sender = (Player) args.get(0);
                    if (sender != null) islandManager.getInviteManager().deny(player, sender);
                });
    }

    private CommandAPICommand visitSub() {
        return new CommandAPICommand("visit")
                .withArguments(new StringArgument("owner").replaceSuggestions(ArgumentSuggestions.strings(
                        info -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toArray(String[]::new))))
                .executesPlayer((player, args) -> {
                    OfflinePlayer owner = Bukkit.getOfflinePlayerIfCached((String) args.get(0));
                    if (owner == null) {
                        player.sendMessage(Utils.fromString("<red>Không tìm thấy người chơi này."));
                        return;
                    }
                    islandManager.getVisitService().visitPlayerIsland(player, owner);
                });
    }

    private CommandAPICommand purgeSub() {
        return new CommandAPICommand("purge")
                .withPermission(ADMIN_PERMISSION)
                .executes((sender, args) -> {
                    sender.sendMessage(Utils.fromString("<yellow>Đang dọn dẹp các đảo bị bỏ hoang..."));
                    islandManager.getPurgeService().purgeArchived(sender);
                });
    }
}
