package com.roguesmp.listener;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.event.DailyResetEvent;
import com.roguesmp.quest.PlayerQuestData;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuestListener implements Listener {

    private final QuestManager questManager;

    public QuestListener(QuestManager questManager) {
        this.questManager = questManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPreJoin(AsyncPlayerPreLoginEvent event) {
        PlayerQuestData playerQuestData = questManager.getQuestDataManager().loadData(event.getUniqueId());
        Utils.runLater(() -> {
            questManager.getQuestDataManager().cache(playerQuestData);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        questManager.getDailyQuestManager().onPlayerJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLeave(PlayerQuitEvent event) {
        PlayerQuestData playerQuestData = questManager.getPlayerQuestData(event.getPlayer().getUniqueId());
        if (playerQuestData == null) {
            RogueSmpCore.LOGGER.error("No quest data found");
            return;
        }
        Utils.runAsync(() -> {
            questManager.getQuestDataManager().saveData(playerQuestData);
        });

    }

    @EventHandler
    public void onDailyReset(DailyResetEvent event) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            questManager.getDailyQuestManager().onPlayerJoin(player); // Resets quests & repopulates active slots
            player.sendMessage(Utils.fromString("<green><bold>[!]</bold> Nhiệm vụ hàng ngày đã được làm mới!"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKillEntity(EntityDeathEvent event) {
        questManager.onKillEntity(event);
    }
}
