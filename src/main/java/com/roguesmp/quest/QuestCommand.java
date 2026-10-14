package com.roguesmp.quest;

import com.roguesmp.quest.data.PendingQuestReward;
import com.roguesmp.quest.data.PlayerQuestData;
import com.roguesmp.quest.reward.QuestReward;
import com.roguesmp.gui.quest.QuestGui;
import com.roguesmp.permission.Permissions;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class QuestCommand {

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

        CommandAPICommand rewardsSubcommand = new CommandAPICommand("rewards")
                .executesPlayer((player, args) -> {
                    List<PendingQuestReward> rewards = questManager.getPendingRewards(player);
                    for (PendingQuestReward reward : rewards) {
                        Quest quest = Registries.QUEST.get(reward.questId());
                        if (quest != null) questManager.sendClaimPrompt(player, reward.source(), quest);
                    }
                    if (rewards.isEmpty()) player.sendMessage(Utils.fromString("<gray>Bạn không có phần thưởng nào chưa nhận."));
                });

        CommandAPICommand claimSubcommand = new CommandAPICommand("claim")
                .withArguments(new StringArgument("source"), new TextArgument("quest_id"))
                .executesPlayer((player, args) -> {
                    UUID source;
                    try {
                        source = UUID.fromString((String) args.get("source"));
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Utils.fromString("<red>Mã phần thưởng không hợp lệ!"));
                        return;
                    }
                    questManager.claimReward(player, source, (String) args.get("quest_id"));
                });

        // /quest
        new CommandAPICommand("quest")
                .withSubcommand(infoSubcommand)
                .withSubcommand(assignSubcommand)
                .withSubcommand(rewardsSubcommand)
                .withSubcommand(claimSubcommand)
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
}
