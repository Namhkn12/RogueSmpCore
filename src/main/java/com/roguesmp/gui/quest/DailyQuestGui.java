package com.roguesmp.gui.quest;

import com.roguesmp.constant.Tags;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.*;
import com.roguesmp.quest.daily.DailyQuestManager;
import com.roguesmp.tag.SmpTag;
import com.roguesmp.utils.DateUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DailyQuestGui extends BaseGui {

    private final Player player;
    private final QuestManager questManager;
    private final DailyQuestManager dailyQuestManager;

    public DailyQuestGui(Player player) {
        super(Utils.fromString("<dark_green>Nhiệm Vụ Hàng Ngày <gray>(" + DateUtils.getFormattedTimeUntilReset() + ")"), 5);
        this.player = player;
        this.questManager = QuestManager.getInstance();
        this.dailyQuestManager = this.questManager.getDailyQuestManager();
    }

    @Override
    public void setup() {
        clearUi();

        PlayerQuestData playerQuestData = questManager.getPlayerQuestData(player.getUniqueId());

        setupBorder();

        if (playerQuestData == null) {
            fillEmpty();
            return;
        }

        setupTierRow(1, Tags.DAILY_EASY_QUEST, "<green>Dễ", Material.LIGHT_BLUE_GLAZED_TERRACOTTA, Material.LIME_CONCRETE, playerQuestData);
        setupTierRow(2, Tags.DAILY_MEDIUM_QUEST, "<yellow>Trung Bình", Material.YELLOW_GLAZED_TERRACOTTA, Material.YELLOW_CONCRETE, playerQuestData);
        setupTierRow(3, Tags.DAILY_HARD_QUEST, "<red>Khó", Material.RED_GLAZED_TERRACOTTA, Material.RED_CONCRETE, playerQuestData);

        fillEmpty();
    }

    private void setupBorder() {
        ItemStack borderPane = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
        borderPane.setData(DataComponentTypes.ITEM_NAME, Component.empty());

        for (int col = 0; col < 9; col++) {
            addButton(0, col, borderPane, ClickHandler.noAction());
            addButton(4, col, borderPane, ClickHandler.noAction());
        }

        for (int row = 1; row <= 3; row++) {
            addButton(row, 0, borderPane, ClickHandler.noAction());
            addButton(row, 2, borderPane, ClickHandler.noAction());
            addButton(row, 6, borderPane, ClickHandler.noAction());
            addButton(row, 7, borderPane, ClickHandler.noAction());
            addButton(row, 8, borderPane, ClickHandler.noAction());
        }
    }

    private void setupTierRow(int row, SmpTag<Quest> tag, String tierName, Material labelMaterial, Material questFallbackMat, PlayerQuestData data) {
        List<QuestProgress> activeQuests = dailyQuestManager.getActiveDailyQuestsForTag(data, tag);
        int completedToday = dailyQuestManager.getCompletedCountToday(data, tag);
        int maxLimit = dailyQuestManager.getDailyLimitForTag(tag);
        boolean isLimitReached = completedToday >= maxLimit;

        // --- Column 1: Category Label Banner ---
        ItemStack headerItem = ItemStack.of(labelMaterial);
        String headerTitle = isLimitReached
                ? "<bold>" + tierName + " <green>(Đã hoàn tất " + maxLimit + "/" + maxLimit + ")"
                : "<bold>" + tierName + " <gray>(" + completedToday + "/" + maxLimit + " hoàn thành)";

        headerItem.setData(DataComponentTypes.ITEM_NAME, Utils.fromString(headerTitle));
        List<Component> headerLore = List.of(
                Utils.fromString("<!i><gray>Giới hạn hoàn thành: <white>" + maxLimit + " nhiệm vụ/ngày"),
                Utils.fromString("<!i><gray>Đã hoàn thành hôm nay: <green>" + completedToday + "/" + maxLimit)
        );
        headerItem.setData(DataComponentTypes.LORE, ItemLore.lore(headerLore));
        addButton(row, 1, headerItem, ClickHandler.noAction());

        // --- Columns 3 to 5: 3 Quest Slots ---
        for (int slot = 0; slot < DailyQuestManager.QUESTS_PER_TIER; slot++) {
            int col = slot + 3;

            if (isLimitReached) {
                ItemStack maxLimitItem = ItemStack.of(Material.IRON_BARS);
                maxLimitItem.setData(DataComponentTypes.ITEM_NAME, Utils.fromString("<red><bold>ĐÃ ĐẠT GIỚI HẠN HÔM NAY"));
                List<Component> limitLore = List.of(
                        Utils.fromString("<!i><gray>Bạn đã hoàn thành đủ <green>" + maxLimit + "/" + maxLimit + "<gray> nhiệm vụ cho tier này."),
                        Utils.fromString("<!i><gray>Quay lại sau: <white>" + DateUtils.getFormattedTimeUntilReset())
                );
                maxLimitItem.setData(DataComponentTypes.LORE, ItemLore.lore(limitLore));

                addButton(row, col, maxLimitItem, ClickHandler.noAction());
                continue;
            }

            if (slot < activeQuests.size()) {
                QuestProgress progress = activeQuests.get(slot);
                Quest quest = progress.getQuest();
                ItemStack icon = buildQuestIcon(progress, tierName, questFallbackMat);

                addButton(row, col, icon, event -> {
                    event.setCancelled(true);

                    if (progress.isCompleted()) {
                        if (progress.isRewardClaimed()) {
                            player.sendMessage(Utils.fromString("<yellow>Bạn đã nhận phần thưởng nhiệm vụ này rồi."));
                        } else {
                            boolean claimed = dailyQuestManager.claimAndReplenish(player, quest);
                            if (claimed) {
                                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                                setup();
                            }
                        }
                    }
                });

            } else {
                ItemStack emptyItem = ItemStack.of(Material.BARRIER);
                emptyItem.setData(DataComponentTypes.ITEM_NAME, Utils.fromString("<gray>Hết nhiệm vụ khả dụng"));
                addButton(row, col, emptyItem, ClickHandler.noAction());
            }
        }
    }

    private ItemStack buildQuestIcon(QuestProgress progress, String tierName, Material defaultMaterial) {
        Quest quest = progress.getQuest();
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);

        Material questMaterial = quest.getIcon() != null ? quest.getIcon() : defaultMaterial;
        Material iconMat = progress.isCompleted() ? Material.BOOKSHELF : questMaterial;

        ItemStack item = ItemStack.of(iconMat);
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hiddenComponents(Set.of(DataComponentTypes.ATTRIBUTE_MODIFIERS)).build());

        if (progress.isCompleted() && !progress.isRewardClaimed()) {
            item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        String questName = quest.getName() != null ? quest.getName() : quest.getId();
        item.setData(DataComponentTypes.ITEM_NAME, Utils.fromString("<bold>" + questName + " <gray>(" + tierName + "<gray>)"));

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.fromString("<!i><gray>Thời gian còn lại: <white>" + DateUtils.getFormattedTimeUntilReset()));
        lore.add(Component.empty());

        if (progress.isCompleted()) {
            if (progress.isRewardClaimed()) {
                lore.add(Utils.fromString("<!i><green>✔ Đã nhận thưởng"));
            } else {
                lore.add(Utils.fromString("<!i><gold>★ Đã hoàn thành! Click để nhận thưởng"));
            }
        } else {
            lore.add(Utils.fromString("<!i><yellow>Đang thực hiện"));
        }

        lore.add(Component.empty());

        for (String descLine : quest.getDescription()) {
            lore.add(Utils.fromString("<!i><gray>" + descLine));
        }

        lore.add(Component.empty());
        lore.add(Utils.fromString("<!i><white><bold>Mục tiêu:"));

        quest.getObjectives().forEach((objId, objective) -> {
            ObjectiveProgress objProgress = progress.getProgressMap().get(objId);
            List<Component> objDisplays;

            if (objProgress != null && smpPlayer != null) {
                objDisplays = objective.getDisplay(smpPlayer, objProgress);
            } else if (smpPlayer != null) {
                objDisplays = objective.getDisplay(smpPlayer);
            } else {
                objDisplays = List.of(Utils.fromString("<!i><gray>- " + objId));
            }

            for (Component display : objDisplays) {
                boolean completed = progress.isObjectiveCompleted(objId);
                Component checkMark = completed ? Utils.fromString("<!i><green>✔ ") : Utils.fromString("<!i><red>❌ ");
                lore.add(checkMark.append(display));
            }
        });

        if (!quest.getRewards().isEmpty() && smpPlayer != null) {
            lore.add(Component.empty());
            lore.add(Utils.fromString("<!i><gold><bold>Phần thưởng:"));
            for (QuestReward reward : quest.getRewards()) {
                for (Component rewardDisplay : reward.getDisplay(smpPlayer)) {
                    lore.add(Utils.fromString("<!i><gray>• ").append(rewardDisplay));
                }
            }
        }

        lore.add(Component.empty());
        if (progress.isCompleted() && !progress.isRewardClaimed()) {
            lore.add(Utils.fromString("<!i><yellow>▶ Click để nhận thưởng!"));
        }

        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }
}
