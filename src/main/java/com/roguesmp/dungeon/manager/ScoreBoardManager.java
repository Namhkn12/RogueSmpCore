package com.roguesmp.dungeon.manager;

import com.roguesmp.dungeon.actor.scoreboard.IScoreBoardRenderer;
import com.roguesmp.dungeon.actor.scoreboard.Impl.TabBoardRenderer;
import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.dto.DungeonScoreBoard;
import com.roguesmp.dungeon.expansion.DungeonExpansion;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.RoomInstance;
import com.roguesmp.dungeon.objective_.IObjective;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScoreBoardManager {

    private final Map<UUID, IScoreBoardRenderer> boards = new HashMap<>();
    private final Map<UUID, Integer> playerSlotIndex = new HashMap<>();
    private final Map<UUID, DungeonInstance> playerInstance = new HashMap<>(); // thêm cái này
    private final DungeonExpansion expansion;

    public ScoreBoardManager(DungeonExpansion expansion) {
        this.expansion = expansion;
    }

    public void bind(DungeonInstance instance) {
        // không còn lưu instance chung nữa
    }

    public void createBoard(Player player, int slotIndex, List<DungeonScoreBoard.PartyMember> members, DungeonInstance instance, Dungeon template) {
        //IScoreBoardRenderer renderer = new DungeonBoardRenderer(player);
        IScoreBoardRenderer renderer = new TabBoardRenderer(player, expansion);
        renderer.init(template ,instance , members);

        boards.put(player.getUniqueId(), renderer);
        playerSlotIndex.put(player.getUniqueId(), slotIndex);
        playerInstance.put(player.getUniqueId(), instance); // mỗi player biết instance của mình
    }

    public void removeBoard(Player player) {
        IScoreBoardRenderer renderer = boards.remove(player.getUniqueId());
        playerSlotIndex.remove(player.getUniqueId());
        playerInstance.remove(player.getUniqueId()); // dọn luôn
        if (renderer != null) renderer.destroy();
    }

    public void tickUpdate() {
        boards.forEach((playerId, renderer) -> {
            DungeonInstance instance = playerInstance.get(playerId);
            if (instance == null) return;

            renderer.updateTime(instance.getRemainingSeconds());
            renderer.updateScore((int) instance.getScore());

            // collect objective lines từ room hiện tại
            RoomInstance room = instance.getActiveRoom();
            if (room != null && room.getObjective() != null) {
                List<List<String>> objectiveLines = room.getObjective().stream()
                        .map(IObjective::getMessageScoreBoard)
                        .toList();
                renderer.updateObjectives(objectiveLines);
            }

            renderer.flush();
        });
    }

    public void onScoreChanged(DungeonInstance instance) { // nhận instance thay vì đọc field chung
        boards.forEach((playerId, renderer) -> {
            if (playerInstance.get(playerId) == instance) { // chỉ update đúng party
                renderer.updateScore((int) instance.getScore());
                renderer.flush();
            }
        });
    }

    public void destroyAll(DungeonInstance instance) { // chỉ dọn boards của instance này
        List<UUID> toRemove = playerInstance.entrySet().stream()
                .filter(e -> e.getValue() == instance)
                .map(Map.Entry::getKey)
                .toList();

        toRemove.forEach(id -> {
            IScoreBoardRenderer renderer = boards.remove(id);
            playerSlotIndex.remove(id);
            playerInstance.remove(id);
            if (renderer != null) renderer.destroy();
        });
    }
}