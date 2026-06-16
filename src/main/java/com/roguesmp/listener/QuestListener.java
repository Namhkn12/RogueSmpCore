package com.roguesmp.listener;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.quest.PlayerQuestData;
import com.roguesmp.quest.Quest;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.utils.Utils;
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
//        Quest quest = questManager.getQuestRegistry().getQuest("legendary_hunter_1");
//        if (quest == null) {
//            RogueSmpCore.LOGGER.error("No quest found");
//            return;
//        }
//        questManager.assignQuest(event.getPlayer(), quest);
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKillEntity(EntityDeathEvent event) {
        questManager.onKillEntity(event);
    }
}
