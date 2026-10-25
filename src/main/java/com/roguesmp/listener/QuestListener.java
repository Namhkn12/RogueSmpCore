package com.roguesmp.listener;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.event.QuestCompletedEvent;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.quest.board.QuestBoardManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuestListener implements Listener {

    private final QuestManager questManager;
    private final QuestBoardManager boardManager;

    public QuestListener(QuestManager questManager, QuestBoardManager boardManager) {
        this.questManager = questManager;
        this.boardManager = boardManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPreJoin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        questManager.getQuestDataManager().stagePendingJoin(questManager.getQuestDataManager().loadData(event.getUniqueId()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!questManager.getQuestDataManager().cachePendingJoin(event.getPlayer().getUniqueId())) {
            RogueSmpCore.LOGGER.error("No quest data was loaded for {} before joining", event.getPlayer().getName());
            return;
        }
        boardManager.refresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLeave(PlayerQuitEvent event) {
        questManager.getQuestDataManager().saveAndUncache(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuestCompleted(QuestCompletedEvent event) {
        boardManager.onQuestCompleted(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKillEntity(EntityDeathEvent event) {
        questManager.onKillEntity(event);
    }
}
