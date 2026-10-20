package com.roguesmp.island;

import com.roguesmp.gate.cost.Cost;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class IslandLevelService {

    public enum Result {
        SUCCESS,
        MAX_LEVEL,
        REQUIREMENTS_NOT_MET,
        CANNOT_AFFORD
    }

    private final IslandDataManager dataManager;

    public IslandLevelService(IslandDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public Result check(SmpPlayer player, IslandData island) {
        Map.Entry<Integer, IslandLevel> next = IslandLevel.next(island.getLevel());
        if (next == null) return Result.MAX_LEVEL;
        if (!next.getValue().requirements().stream().allMatch(requirement -> requirement.test(player))) return Result.REQUIREMENTS_NOT_MET;
        if (!next.getValue().costs().stream().allMatch(cost -> cost.canAfford(player))) return Result.CANNOT_AFFORD;
        return Result.SUCCESS;
    }

    public Result upgrade(SmpPlayer player, IslandData island) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null) return Result.CANNOT_AFFORD;

        Result result = check(player, island);
        if (result != Result.SUCCESS) return result;

        Map.Entry<Integer, IslandLevel> next = IslandLevel.next(island.getLevel());
        for (Cost cost : next.getValue().costs()) cost.takeFrom(bukkitPlayer.getInventory());
        for (Cost cost : next.getValue().costs()) cost.take(player);

        setLevel(island, next.getKey());
        broadcast(island, "<green>" + bukkitPlayer.getName() + " đã nâng cấp đảo lên <yellow>cấp " + next.getKey() + "<green>!");
        return Result.SUCCESS;
    }

    public void setLevel(IslandData island, int level) {
        island.setLevel(level);
        dataManager.saveAsync(island);
    }

    private void broadcast(IslandData island, String message) {
        for (UUID memberId : island.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) member.sendMessage(Utils.fromString(message));
        }
    }
}
