package com.roguesmp.quest;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.quest.data.PlayerQuestData;
import com.roguesmp.quest.data.QuestDataManager.RankEntry;
import org.bukkit.Bukkit;
import com.roguesmp.quest.board.TopRankSettings;
import com.roguesmp.quest.board.AdventureLeaderboard;
import org.bukkit.command.CommandSender;
import dev.jorel.commandapi.arguments.LongArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import com.roguesmp.quest.board.QuestBoardManager;
import com.roguesmp.quest.reward.QuestReward;
import com.roguesmp.gui.quest.QuestGui;
import com.roguesmp.player.inbox.gui.InboxGui;
import com.roguesmp.quest.board.gui.QuestBoardGui;
import com.roguesmp.quest.board.gui.QuestLevelGui;
import com.roguesmp.permission.Permissions;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;

public class QuestCommand {

    private static final int TOP_LINES = 10;

    private final QuestManager questManager;

    public QuestCommand(QuestManager questManager) {
        this.questManager = questManager;
    }

    public void register() {
        CommandAPICommand infoSubcommand = new CommandAPICommand("info")
                .withArguments(new TextArgument("quest_id")
                        .replaceSuggestions(ArgumentSuggestions.strings(info ->
                                Registries.QUEST.getAll().keySet().toArray(new String[0])
                        ))
                )
                .executesPlayer((player, args) -> {
                    String questId = (String) args.get("quest_id");
                    Quest quest = Registries.QUEST.get(questId);

                    if (quest == null) {
                        player.sendMessage(Utils.fromString("<red>Không tìm thấy nhiệm vụ với ID: <yellow>" + questId));
                        return;
                    }

                    SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);

                    // Render Layout Interface Block ra chat
                    player.sendMessage(Utils.fromString("<dark_gray>=======================================</dark_gray>"));
                    player.sendMessage(Utils.fromString("<gold><bold>THÔNG TIN NHIỆM VỤ</bold></gold>"));
                    player.sendMessage(Utils.fromString("<gray>ID: <yellow>" + quest.getId() + "</yellow>"));
                    player.sendMessage(Utils.fromString("<gray>Tên: " + (quest.getName() != null ? quest.getName() : "<italic>Chưa đặt tên</italic>")));

                    if (!quest.getDescription().isEmpty()) {
                        player.sendMessage(Utils.fromString("<gray>Mô tả:"));
                        for (String line : quest.getDescription()) {
                            player.sendMessage(Utils.fromString("  " + line));
                        }
                    }

                    if (!quest.getRequirements().isEmpty()) {
                        player.sendMessage(Utils.fromString("<red>[!] Yêu cầu:</red>"));
                        player.sendMessage(Utils.fromString("<gray>  • Có <yellow>" + quest.getRequirements().size() + "</yellow> điều kiện để nhận."));
                    }

                    player.sendMessage(Utils.fromString("<aqua>[✦] Mục tiêu:</aqua>"));
                    quest.getObjectives().forEach((trackingKey, objective) -> {
                        List<Component> components = objective.getDisplay(smpPlayer);
                        for (Component component : components) {
                            player.sendMessage(Utils.fromString("  <dark_aqua>• Tracker [" + trackingKey + "]: </dark_aqua>").append(component));
                        }
                    });

                    if (!quest.getRewards().isEmpty()) {
                        player.sendMessage(Utils.fromString("<green>[🎁] Phan thưởng:</green>"));
                        for (QuestReward reward : quest.getRewards()) {
                            List<Component> components = reward.getDisplay(smpPlayer);
                            for (Component component : components) {
                                player.sendMessage(Utils.text("  ").append(component));
                            }
                        }
                    }
                    player.sendMessage(Utils.fromString("<dark_gray>=======================================</dark_gray>"));
                });

        CommandAPICommand assignSubcommand = new CommandAPICommand("assign")
                .withPermission(Permissions.QUEST_ASSIGN.node())
                .withArguments(new EntitySelectorArgument.OnePlayer("target"))
                .withArguments(new TextArgument("quest_id")
                        .replaceSuggestions(ArgumentSuggestions.strings(info ->
                                Registries.QUEST.getAll().keySet().toArray(new String[0])
                        ))
                )
                .executes((sender, args) -> {
                    Player target = (Player) args.get("target");
                    String questId = (String) args.get("quest_id");

                    if (target == null) {
                        sender.sendMessage(Utils.fromString("<red>Người chơi không trực tuyến!"));
                        return;
                    }

                    Quest quest = Registries.QUEST.get(questId);
                    if (quest == null) {
                        sender.sendMessage(Utils.fromString("<red>Không tìm thấy nhiệm vụ với ID: <yellow>" + questId));
                        return;
                    }

                    boolean success = questManager.assignQuest(target, quest);

                    Component qName = quest.getName() != null ? Utils.fromString(quest.getName()) : Component.text(quest.getId());

                    if (success) {
                        sender.sendMessage(Utils.text("Đã giao nhiệm vụ thành công cho " + target.getName(), NamedTextColor.GREEN));
                        target.sendMessage(Utils.text("Bạn đã được giao nhiệm vụ mới: ", NamedTextColor.GOLD).append(qName));
                    } else {
                        sender.sendMessage(Utils.text(target.getName() + " đã hoặc đang thực hiện nhiệm vụ này rồi!", NamedTextColor.RED));
                    }
                });

        CommandAPICommand boardSubcommand = new CommandAPICommand("board")
                .executesPlayer((player, args) -> {
                    new QuestBoardGui(player).showInventory(player);
                });

        CommandAPICommand expSubcommand = new CommandAPICommand("exp")
                .withPermission(Permissions.QUEST_EXP.node())
                .withSubcommand(new CommandAPICommand("give")
                        .withArguments(new EntitySelectorArgument.OnePlayer("target"), new LongArgument("amount", 1))
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("target");
                            long amount = (long) args.get("amount");
                            if (questData(sender, target) == null) return;
                            QuestBoardManager.getInstance().giveExp(target, amount);
                            sendExpStatus(sender, target);
                        }))
                .withSubcommand(new CommandAPICommand("take")
                        .withArguments(new EntitySelectorArgument.OnePlayer("target"), new LongArgument("amount", 1))
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("target");
                            PlayerQuestData data = questData(sender, target);
                            if (data == null) return;
                            QuestBoardManager.getInstance().setExp(target, data.getExp() - (long) args.get("amount"));
                            sendExpStatus(sender, target);
                        }))
                .withSubcommand(new CommandAPICommand("set")
                        .withArguments(new EntitySelectorArgument.OnePlayer("target"), new LongArgument("amount", 0))
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("target");
                            PlayerQuestData data = questData(sender, target);
                            if (data == null) return;
                            QuestBoardManager.getInstance().setExp(target, (long) args.get("amount"));
                            sendExpStatus(sender, target);
                        }))
                .withSubcommand(new CommandAPICommand("setlevel")
                        .withArguments(new EntitySelectorArgument.OnePlayer("target"), new IntegerArgument("level", 1))
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("target");
                            PlayerQuestData data = questData(sender, target);
                            if (data == null) return;
                            int level = (int) args.get("level");
                            QuestBoardManager.getInstance().setExp(target, QuestBoardManager.getInstance().getSettings().level().totalExpFor(level));
                            sendExpStatus(sender, target);
                        }))
                .withSubcommand(new CommandAPICommand("check")
                        .withArguments(new EntitySelectorArgument.OnePlayer("target"))
                        .executes((sender, args) -> {
                            Player target = (Player) args.get("target");
                            if (questData(sender, target) == null) return;
                            sendExpStatus(sender, target);
                        }));

        CommandAPICommand topSubcommand = new CommandAPICommand("top")
                .withSubcommand(new CommandAPICommand("refresh")
                        .withPermission(Permissions.QUEST_TOP_REFRESH.node())
                        .executes((sender, args) -> {
                            sender.sendMessage(Utils.fromString("<gray>Đang xếp hạng lại..."));
                            QuestBoardManager.getInstance().refreshTopRank().thenRun(() -> sendTop(sender));
                        }))
                .executes((sender, args) -> {
                    sendTop(sender);
                });

        CommandAPICommand levelSubcommand = new CommandAPICommand("level")
                .executesPlayer((player, args) -> {
                    new QuestLevelGui(player, null).showInventory(player);
                });

        CommandAPICommand inboxSubcommand = new CommandAPICommand("inbox")
                .executesPlayer((player, args) -> {
                    new InboxGui(player, null).showInventory(player);
                });

        // /quest
        new CommandAPICommand("quest")
                .withSubcommand(infoSubcommand)
                .withSubcommand(assignSubcommand)
                .withSubcommand(boardSubcommand)
                .withSubcommand(inboxSubcommand)
                .withSubcommand(levelSubcommand)
                .withSubcommand(expSubcommand)
                .withSubcommand(topSubcommand)
                // no-args open gui
                .executesPlayer((player, args) -> {
                    PlayerQuestData questData = questManager.getQuestDataManager().getCachedData(player.getUniqueId());

                    if (questData == null) {
                        player.sendMessage(Utils.fromString("<red>Không thể tải dữ liệu nhiệm vụ của bạn lúc này."));
                        return;
                    }

                    // Khởi tạo và mở thẳng GUI cho người chơi thao tác
                    QuestGui gui = new QuestGui(player, questData);
                    gui.showInventory(player);
                })
                .register();
    }

    private PlayerQuestData questData(CommandSender sender, Player target) {
        PlayerQuestData data = target == null ? null : questManager.getPlayerQuestData(target.getUniqueId());
        if (data == null) sender.sendMessage(Utils.fromString("<red>Không thể tải dữ liệu nhiệm vụ của người chơi này."));
        return data;
    }

    private void sendExpStatus(CommandSender sender, Player target) {
        PlayerQuestData data = questManager.getPlayerQuestData(target.getUniqueId());
        QuestBoardManager board = QuestBoardManager.getInstance();
        sender.sendMessage(Utils.fromString("<gray>" + target.getName() + ": <aqua>" + data.getExp() + "</aqua> kinh nghiệm, cấp <gold>" + board.getLevel(data) + "</gold>, hạng ")
                .append(Utils.fromString(board.getRank(data).name())));
    }

    private void sendTop(CommandSender sender) {
        QuestBoardManager board = QuestBoardManager.getInstance();
        AdventureLeaderboard leaderboard = board.getLeaderboard();
        leaderboard.top(TOP_LINES).thenAcceptAsync(entries -> {
            sender.sendMessage(Utils.fromString("<gold><bold>Bảng xếp hạng Mạo hiểm</bold> <gray>(cập nhật mỗi ngày)"));
            if (entries.isEmpty()) sender.sendMessage(Utils.fromString("<gray>Chưa có ai trên bảng xếp hạng."));
            TopRankSettings settings = leaderboard.getSettings();
            for (RankEntry entry : entries) {
                Component line = Utils.fromString("<yellow>" + entry.position() + ". <white>" + AdventureLeaderboard.nameOf(entry.uuid()) + " <gray>- <aqua>" + entry.exp() + "</aqua> kinh nghiệm");
                if (settings != null && leaderboard.holdsTopRank(entry)) line = line.append(settings.rank().getDisplayIcon());
                sender.sendMessage(line);
            }
            if (sender instanceof Player player) {
                QuestBoardManager.AdventureStatus status = board.getStatus(player.getUniqueId());
                sender.sendMessage(Utils.fromString(status.position() > 0
                        ? "<gray>Hạng của bạn: <gold>#" + status.position()
                        : "<gray>Bạn chưa có trên bảng xếp hạng (" + (status.level() < AdventureLeaderboard.minRankedLevel() ? "cần cấp " + AdventureLeaderboard.minRankedLevel() : "cập nhật ở lần xếp hạng tới") + ")."));
            }
            if (settings != null) {
                sender.sendMessage(Utils.fromString("<gray>Top <gold>" + settings.slots() + "</gold> từ cấp <gold>" + settings.minLevel() + "</gold> nhận hạng ").append(Utils.fromString(settings.rank().name())));
            }
        }, Bukkit.getScheduler().getMainThreadExecutor(RogueSmpCore.getInstance()));
    }
}
