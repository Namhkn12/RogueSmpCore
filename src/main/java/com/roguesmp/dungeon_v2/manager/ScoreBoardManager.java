package com.roguesmp.dungeon_v2.manager;

import com.roguesmp.dungeon_v2.actor.scoreboard.TabBoardRenderer;
import com.roguesmp.dungeon_v2.data.definition.Dungeon;
import com.roguesmp.dungeon_v2.data.definition.objective.IDisplayable;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.RoomInstance;
import com.roguesmp.dungeon_v2.expansion.DungeonExpansion;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScoreBoardManager {

    private final Map<UUID, TabBoardRenderer> boards = new HashMap<>();
    private final Map<UUID, DungeonInstance> playerInstance = new HashMap<>();
    private final DungeonExpansion expansion;

    public ScoreBoardManager(DungeonExpansion expansion) {
        this.expansion = expansion;
    }

    public void createBoard(Player player, DungeonInstance instance, Dungeon template) {
        TabBoardRenderer renderer = new TabBoardRenderer(player, expansion);
        renderer.init(template, instance);
        boards.put(player.getUniqueId(), renderer);
        playerInstance.put(player.getUniqueId(), instance);
    }

    public void removeBoard(Player player) {
        TabBoardRenderer renderer = boards.remove(player.getUniqueId());
        playerInstance.remove(player.getUniqueId());
        if (renderer != null) renderer.destroy();
    }

    public void tickUpdate() {
        boards.forEach((playerId, renderer) -> {
            DungeonInstance instance = playerInstance.get(playerId);
            if (instance == null) return;

            renderer.updateTime(instance.getTimer().getRemainingSeconds());
            renderer.updateScore(instance.getProgress().getScore());

            RoomInstance room = instance.getProgress().getCurrentRoom();
            if (room != null && room.getActiveObjectives() != null) {
                List<List<String>> objectiveLines = room.getActiveObjectives().stream()
                        .filter(obj -> obj instanceof IDisplayable)
                        .map(obj -> ((IDisplayable) obj).getScoreBoardLine())
                        .toList();
                renderer.updateObjectives(objectiveLines);
            }
        });
    }

    public void onScoreChanged(DungeonInstance instance) {
        boards.forEach((playerId, renderer) -> {
            if (playerInstance.get(playerId) == instance) {
                renderer.updateScore(instance.getProgress().getScore());
            }
        });
    }

    public void destroyAll(DungeonInstance instance) {
        List<UUID> toRemove = playerInstance.entrySet().stream()
                .filter(e -> e.getValue() == instance)
                .map(Map.Entry::getKey)
                .toList();

        toRemove.forEach(id -> {
            TabBoardRenderer renderer = boards.remove(id);
            playerInstance.remove(id);
            if (renderer != null) renderer.destroy();
        });
    }
}