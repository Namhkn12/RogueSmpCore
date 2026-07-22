package com.roguesmp.quest;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.gui.quest.QuestGui;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.daily.DailyQuestManager;
import com.roguesmp.registry.quest.QuestRegistry;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class QuestManager {

    private static QuestManager INSTANCE;

    private final RogueSmpCore plugin;
    private final QuestDataManager questDataManager;
    private final QuestRegistry questRegistry;
    private final DailyQuestManager dailyQuestManager;

    public QuestManager(RogueSmpCore plugin, QuestRegistry questRegistry) {
        this.plugin = plugin;
        this.questDataManager = new QuestDataManager(plugin, questRegistry, this);
        this.questRegistry = questRegistry;
        this.dailyQuestManager = new DailyQuestManager(this);
    }

    public @Nullable PlayerQuestData getPlayerQuestData(UUID uuid) {
        return questDataManager.getCachedData(uuid);
    }

    /**
     * Assigns a specific quest to a player if they don't already have progress on it.
     */
    public boolean assignQuest(Player player, Quest quest) {
        PlayerQuestData data = getPlayerQuestData(player.getUniqueId());
        if (data == null) return false;

        // Prevent re-assigning if they already have it active or completed
        if (data.getQuestProgresses().containsKey(quest.getId())) {
            return false;
        }

        // Initialize fresh progress for this quest
        QuestProgress newProgress = new QuestProgress(quest);
        data.setQuestProgress(quest.getId(), newProgress);

        return true;
    }

    /**
     * @param objectiveConsumer Function to handle Objective progress
     */
    public void handleProgress(Player player, java.util.function.BiConsumer<QuestObjective, ObjectiveProgress> objectiveConsumer) {
        PlayerQuestData playerQuestData = getPlayerQuestData(player.getUniqueId());
        if (playerQuestData == null) return;

        for (QuestProgress questProgress : playerQuestData.getQuestProgresses().values()) {
            if (questProgress.isCompleted()) continue;

            Quest quest = questProgress.getQuest();

            quest.getObjectives().forEach((objectiveId, objective) -> {
                if (questProgress.isObjectiveCompleted(objectiveId)) return;

                boolean doneQuest = questProgress.updateObjectiveProgress(objectiveId, objectiveProgress -> {
                    objectiveConsumer.accept(objective, objectiveProgress);
                });

                if (doneQuest) {
                    sendQuestDoneNotification(player, quest);
                }
            });
        }
    }

    public void sendQuestDoneNotification(Player player, Quest quest) {
        Component questName;
        if (quest.getName() != null) {
            questName = Utils.fromString(quest.getName());
        } else questName = Component.text("NO_NAME");
        player.sendMessage(Component.text("Bạn đã hoàn thành nhiệm vụ ", NamedTextColor.GREEN).append(questName));
    }

    /**
     * Instantly set a player quest progress to a completed state. Useful for administrative debugging or skip tokens.
     *
     * @return true if the modification was successfully processed.
     */
    public boolean forceCompleteQuest(Player player, Quest quest) {
        PlayerQuestData data = getPlayerQuestData(player.getUniqueId());
        if (data == null) return false;

        QuestProgress progress = data.getQuestProgress(quest.getId());
        if (progress == null || progress.isCompleted()) {
            return false;
        }

        progress.complete();
        return true;
    }

    /**
     * Completely purges a player's quest progress for a specific quest.
     * Might be needed for seasonal resets or letting a player re-take a repeatable quest.
     *
     * @return true if reset was successful
     */
    public boolean resetQuestProgress(Player player, Quest quest) {
        PlayerQuestData data = getPlayerQuestData(player.getUniqueId());
        if (data == null) return false;

        QuestProgress progress = data.getQuestProgress(quest.getId());
        if (progress == null) return false;

        progress.resetProgresses();

        return true;
    }

    /**
     * Evaluates whether a player satisfies all requirement
     * to unlock or initiate a specific quest.
     *
     * @return true if the player meets all conditions
     */
    public boolean canAcceptQuest(Player player, Quest quest) {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (smpPlayer == null) return false;

        PlayerQuestData data = getPlayerQuestData(player.getUniqueId());
        if (data == null) return false;

        // 1. Is it already active or completed in their file?
        if (data.getQuestProgresses().containsKey(quest.getId())) {
            return false;
        }

         for (QuestRequirement requirement : quest.getRequirements()) {
             if (!requirement.canMeetRequirement(smpPlayer)) return false;
         }

        return true;
    }

    /**
     * Attempt to claim quest reward for player
     * @param checkCompletion If true, player must have completed the quest and have not already claimed the reward
     * @param updateClaimed If true, will save "rewardClaimed" status to player quest progress data
     * @return false if reward can't be given
     */
    public boolean claimQuestReward(Player player, Quest quest, boolean checkCompletion, boolean updateClaimed) {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (smpPlayer == null) return false;

        PlayerQuestData playerQuestData = questDataManager.getCachedData(player.getUniqueId());
        if (playerQuestData == null) return false;

        QuestProgress questProgress = playerQuestData.getQuestProgress(quest.getId());
        if (questProgress == null) return false;

        if (checkCompletion && questProgress.isRewardClaimed()) {
            player.sendMessage(Utils.fromString("<red>Phần thưởng nhiệm vụ này đã được nhận trước đó rồi!"));
            return false;
        }

        if (checkCompletion && !questProgress.isCompleted()) {
            player.sendMessage(Utils.text("<red>Bạn chưa hoàn thành tất cả mục tiêu của nhiệm vụ này!"));
            return false;
        }

        for (QuestReward reward : quest.getRewards()) {
            reward.giveReward(smpPlayer);
        }

        if (updateClaimed) {
            questProgress.setRewardClaimed(true);
        }

        return true;
    }

    /**
     * Retrieves all active, uncompleted quest tracking sessions for a player.
     */
    public @NotNull List<QuestProgress> getActiveQuests(Player player) {
        PlayerQuestData data = getPlayerQuestData(player.getUniqueId());
        if (data == null) return Collections.emptyList();

        return data.getQuestProgresses().values().stream()
                .filter(progress -> !progress.isCompleted())
                .toList();
    }

    /**
     * Retrieves all completed quest profiles for a player.
     */
    public @NotNull List<QuestProgress> getCompletedQuests(Player player) {
        PlayerQuestData data = getPlayerQuestData(player.getUniqueId());
        if (data == null) return Collections.emptyList();

        return data.getQuestProgresses().values().stream()
                .filter(QuestProgress::isCompleted)
                .toList();
    }

    public QuestRegistry getQuestRegistry() {
        return questRegistry;
    }

    public QuestDataManager getQuestDataManager() {
        return questDataManager;
    }

    public DailyQuestManager getDailyQuestManager() {
        return dailyQuestManager;
    }

    public void onKillEntity(EntityDeathEvent event) {
        if (!(event.getDamageSource().getCausingEntity() instanceof Player player)) return;

        handleProgress(player, (questObjective, objectiveProgress) -> {
            questObjective.onKillEntity(event, objectiveProgress);
        });
    }

    public void onDisable() {
        questDataManager.saveAllData();
    }

    public void registerQuestCommand() {
        CommandAPICommand infoSubcommand = new CommandAPICommand("info")
                .withArguments(new StringArgument("quest_id")
                        .replaceSuggestions(ArgumentSuggestions.strings(info ->
                                questRegistry.getRegistry().keySet().toArray(new String[0])
                        ))
                )
                .executesPlayer((player, args) -> {
                    String questId = (String) args.get("quest_id");
                    Quest quest = questRegistry.getQuest(questId);

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
                .withArguments(new EntitySelectorArgument.OnePlayer("target"))
                .withArguments(new StringArgument("quest_id")
                        .replaceSuggestions(ArgumentSuggestions.strings(info ->
                                questRegistry.getRegistry().keySet().toArray(new String[0])
                        ))
                )
                .executes((sender, args) -> {
                    Player target = (Player) args.get("target");
                    String questId = (String) args.get("quest_id");

                    if (target == null) {
                        sender.sendMessage(Utils.fromString("<red>Người chơi không trực tuyến!"));
                        return;
                    }

                    Quest quest = questRegistry.getQuest(questId);
                    if (quest == null) {
                        sender.sendMessage(Utils.fromString("<red>Không tìm thấy nhiệm vụ với ID: <yellow>" + questId));
                        return;
                    }

                    boolean success = assignQuest(target, quest);

                    Component qName = quest.getName() != null ? Utils.fromString(quest.getName()) : Component.text(quest.getId());

                    if (success) {
                        sender.sendMessage(Utils.text("Đã giao nhiệm vụ thành công cho " + target.getName(), NamedTextColor.GREEN));
                        target.sendMessage(Utils.text("Bạn đã được giao nhiệm vụ mới: ", NamedTextColor.GOLD).append(qName));
                    } else {
                        sender.sendMessage(Utils.text(target.getName() + " đã hoặc đang thực hiện nhiệm vụ này rồi!", NamedTextColor.RED));
                    }
                });

        // /quest
        new CommandAPICommand("quest")
                .withSubcommand(infoSubcommand)
                .withSubcommand(assignSubcommand)
                // no-args open gui
                .executesPlayer((player, args) -> {
                    PlayerQuestData questData = questDataManager.getCachedData(player.getUniqueId());

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

    public static void init(RogueSmpCore plugin, QuestRegistry questRegistry) {
        INSTANCE = new QuestManager(plugin, questRegistry);
    }

    public static QuestManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("QuestManager is null!");
        }
        return INSTANCE;
    }
}
