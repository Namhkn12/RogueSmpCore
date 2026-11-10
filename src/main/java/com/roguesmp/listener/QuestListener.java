package com.roguesmp.listener;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.event.AdventureRankChangeEvent;
import com.roguesmp.event.DailyResetEvent;
import com.roguesmp.event.DungeonClearEvent;
import com.roguesmp.registry.Registries;
import com.roguesmp.event.HandMachineProcessEvent;
import com.roguesmp.event.QuestCompletedEvent;
import com.roguesmp.event.QuestProgressEvent;
import com.roguesmp.quest.holder.QuestHolder;
import com.roguesmp.event.QuestStartedEvent;
import com.roguesmp.event.SmpCraftEvent;
import com.roguesmp.quest.Quest;
import com.roguesmp.quest.holder.PlayerHolder;
import org.bukkit.entity.Player;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.quest.board.AdventureRank;
import com.roguesmp.quest.board.QuestBoardManager;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import com.roguesmp.quest.data.PlayerQuestData;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuestListener implements Listener {

    private static final String INTRO_QUEST = "intro/choose_class";
    private static final NamespacedKey LOBBY_WORLD = NamespacedKey.minecraft("lobby");
    private static final double LOBBY_X = -30.5, LOBBY_Y = 62.0, LOBBY_Z = 0.5;
    private static final float LOBBY_YAW = -90f, LOBBY_PITCH = 0f;

    private final QuestManager questManager;
    private final QuestBoardManager boardManager;

    public QuestListener(QuestManager questManager, QuestBoardManager boardManager) {
        this.questManager = questManager;
        this.boardManager = boardManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPreJoin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        questManager.getQuestDataManager().cache(questManager.getQuestDataManager().loadData(event.getUniqueId()));
        boardManager.loadRanking(event.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpawnLocation(AsyncPlayerSpawnLocationEvent event) {
        PlayerQuestData data = questManager.getPlayerQuestData(event.getConnection().getProfile().getId());
        if (data == null || data.getQuestProgress(INTRO_QUEST) != null) return;

        World lobby = Bukkit.getWorld(LOBBY_WORLD);
        if (lobby != null) event.setSpawnLocation(new Location(lobby, LOBBY_X, LOBBY_Y, LOBBY_Z, LOBBY_YAW, LOBBY_PITCH));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (questManager.getQuestDataManager().getCachedData(event.getPlayer().getUniqueId()) == null) {
            RogueSmpCore.LOGGER.error("No quest data was loaded for {} before joining", event.getPlayer().getName());
            return;
        }
        Player player = event.getPlayer();
        boardManager.refresh(player);
        boardManager.syncStatus(player);
        questManager.refreshPinnedBar(player);
        suggestIntro(player);
        questManager.refresh(player);
    }

    private void suggestIntro(Player player) {
        Quest intro = Registries.QUEST.get(INTRO_QUEST);
        if (intro != null) questManager.assignQuest(player, intro);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLeave(PlayerQuitEvent event) {
        questManager.getQuestDataManager().saveAndUncache(event.getPlayer().getUniqueId());
        questManager.forgetPinnedBar(event.getPlayer().getUniqueId());
        boardManager.clearStatus(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onConnectionClose(PlayerConnectionCloseEvent event) {
        questManager.getQuestDataManager().uncache(event.getPlayerUniqueId());
        boardManager.clearStatus(event.getPlayerUniqueId());
    }

    @EventHandler
    public void onDailyReset(DailyResetEvent event) {
        boardManager.refreshTopRank();
    }

    @EventHandler
    public void onRankChange(AdventureRankChangeEvent event) {
        AdventureRank rank = event.getNewRank();
        event.getPlayer().sendMessage(Utils.fromString("<gold><bold>HẠNG MẠO HIỂM</bold></gold> <yellow>Hạng mới: </yellow>" + rank.name()).append(rank.getDisplayIcon()));
        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
    }

    @EventHandler
    public void onQuestCompleted(QuestCompletedEvent event) {
        boardManager.onQuestCompleted(event);
        refreshPinnedBar(event.getHolder());
    }

    @EventHandler
    public void onQuestProgress(QuestProgressEvent event) {
        refreshPinnedBar(event.getHolder());
    }

    private void refreshPinnedBar(QuestHolder holder) {
        if (!(holder instanceof PlayerHolder playerHolder)) return;
        Player player = Bukkit.getPlayer(playerHolder.id());
        if (player != null) questManager.refreshPinnedBar(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKillEntity(EntityDeathEvent event) {
        questManager.onKillEntity(event);
    }

    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent event) {
        questManager.refresh(event.getPlayer());
    }

    @EventHandler
    public void onCraft(SmpCraftEvent event) {
        questManager.onCraft(event);
    }

    @EventHandler
    public void onHandMachineProcess(HandMachineProcessEvent event) {
        questManager.onHandMachineProcess(event);
    }

    @EventHandler
    public void onDungeonClear(DungeonClearEvent event) {
        questManager.onDungeonClear(event);
    }

    @EventHandler
    public void onQuestStarted(QuestStartedEvent event) {
        refreshPinnedBar(event.getHolder());
        if (!(event.getHolder() instanceof PlayerHolder holder)) return;
        Player player = Bukkit.getPlayer(holder.id());
        if (player == null) return;
        Utils.runLater(() -> {
            if (player.isOnline()) questManager.refresh(player);
        });

        Quest quest = event.getProgress().getQuest();
        if (!quest.isAutoPin()) return;
        String name = quest.getName() != null ? quest.getName() : quest.getId();
        player.sendMessage(Utils.fromString("<aqua><bold>NHIỆM VỤ MỚI</bold></aqua> <gray>»</gray> " + name + " <dark_gray>(/quest để xem chi tiết)</dark_gray>"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);
    }
}
