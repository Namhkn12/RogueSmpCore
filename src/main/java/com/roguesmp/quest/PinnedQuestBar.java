package com.roguesmp.quest;

import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.objective.ObjectiveProgress;
import com.roguesmp.quest.objective.QuestObjective;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows each player's pinned quest as a boss bar: the objective itself when the quest has one, otherwise the quest
 * name with how many objectives are done.
 */
public class PinnedQuestBar {

    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<>();

    public void refresh(Player player, @Nullable QuestProgress pinned) {
        if (pinned == null) {
            hide(player);
            return;
        }

        BossBar bar = bars.computeIfAbsent(player.getUniqueId(), id -> {
            BossBar created = BossBar.bossBar(Component.empty(), 0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
            player.showBossBar(created);
            return created;
        });
        render(bar, pinned, PlayerManager.getInstance().getSmpPlayer(player));
    }

    public void hide(Player player) {
        BossBar bar = bars.remove(player.getUniqueId());
        if (bar != null) player.hideBossBar(bar);
    }

    public void forget(UUID playerId) {
        bars.remove(playerId);
    }

    private void render(BossBar bar, QuestProgress pinned, @Nullable SmpPlayer smpPlayer) {
        Map<String, QuestObjective> objectives = pinned.getQuest().getObjectives();
        Map<String, ObjectiveProgress> progresses = pinned.getProgressMap();

        if (objectives.size() == 1) {
            Map.Entry<String, QuestObjective> only = objectives.entrySet().iterator().next();
            ObjectiveProgress progress = progresses.get(only.getKey());
            if (progress != null) {
                bar.name(Utils.fromString("<gold>📌</gold> ").append(only.getValue().getDisplay(smpPlayer, progress).getFirst()));
                bar.progress(only.getValue().completion(progress));
                return;
            }
        }

        long done = objectives.keySet().stream().filter(pinned::isObjectiveCompleted).count();
        String name = pinned.getQuest().getName() != null ? pinned.getQuest().getName() : pinned.getQuest().getId();
        bar.name(Utils.fromString("<gold>📌</gold> " + name + " <gray>(</gray><yellow>" + done + "</yellow><gray>/</gray><gold>" + objectives.size() + "</gold><gray>)</gray>"));
        bar.progress(objectives.isEmpty() ? 1f : (float) done / objectives.size());
    }
}
