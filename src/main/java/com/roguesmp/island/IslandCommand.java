package com.roguesmp.island;

import com.roguesmp.gui.island.IslandLevelGui;
import com.roguesmp.gui.island.IslandMainGui;
import com.roguesmp.permission.Permissions;
import com.roguesmp.player.PlayerData;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class IslandCommand {

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
                .withSubcommand(levelSub())
                .withSubcommand(setLevelSub())
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

    private CommandAPICommand levelSub() {
        return new CommandAPICommand("level")
                .executesPlayer((player, args) -> {
                    IslandData island = getIsland(player);
                    if (island == null) {
                        player.sendMessage(Utils.fromString("<red>Bạn chưa có đảo."));
                        return;
                    }
                    new IslandLevelGui(player, island).showInventory(player);
                });
    }

    private CommandAPICommand setLevelSub() {
        return new CommandAPICommand("setlevel")
                .withPermission(Permissions.ISLAND_ADMIN.node())
                .withArguments(new EntitySelectorArgument.OnePlayer("target"), new IntegerArgument("level", 0))
                .executes((sender, args) -> {
                    Player target = (Player) args.get(0);
                    int level = (int) args.get(1);
                    IslandData island = target == null ? null : getIsland(target);
                    if (island == null) {
                        sender.sendMessage(Utils.fromString("<red>Người chơi này chưa có đảo."));
                        return;
                    }
                    islandManager.getLevelService().setLevel(island, level);
                    sender.sendMessage(Utils.fromString("<green>Đã đặt cấp đảo của <yellow>" + target.getName() + " <green>thành <yellow>" + level + "<green>."));
                });
    }

    private @Nullable IslandData getIsland(Player player) {
        PlayerData playerData = islandManager.getPlayerManager().getDataManager().getData(player.getUniqueId());
        return playerData == null ? null : islandManager.getIslandDataManager().getCachedData(playerData.getIslandId());
    }

    private CommandAPICommand purgeSub() {
        return new CommandAPICommand("purge")
                .withPermission(Permissions.ISLAND_ADMIN.node())
                .executes((sender, args) -> {
                    sender.sendMessage(Utils.fromString("<yellow>Đang dọn dẹp các đảo bị bỏ hoang..."));
                    islandManager.getPurgeService().purgeArchived(sender);
                });
    }
}
