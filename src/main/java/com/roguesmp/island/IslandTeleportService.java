package com.roguesmp.island;

import com.roguesmp.player.PlayerData;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.teleport.TeleportOptions;
import com.roguesmp.teleport.TeleportService;
import com.roguesmp.utils.Utils;
import com.roguesmp.teleport.destination.HubDestination;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class IslandTeleportService {

    private final PlayerManager playerManager;
    private final IslandDataManager dataManager;
    private final IslandWorldManager worldManager;

    public IslandTeleportService(PlayerManager playerManager, IslandDataManager dataManager, IslandWorldManager worldManager) {
        this.playerManager = playerManager;
        this.dataManager = dataManager;
        this.worldManager = worldManager;
    }

    public void teleportHome(Player player) {
        PlayerData playerData = playerManager.getDataManager().getData(player.getUniqueId());
        if (playerData == null) return;

        IslandData islandData = dataManager.getCachedData(playerData.getIslandId());
        if (islandData == null) {
            player.sendMessage(Utils.fromString("<red>Bạn đang vô gia cư, hãy tạo đảo trước."));
            return;
        }

        worldManager.isWorldAvailable(islandData.getIslandId()).thenAccept(available -> Utils.runLater(() -> {
            if (available) {
                teleportToIsland(player, islandData);
                return;
            }

            player.sendMessage(Utils.fromString("<red>Dữ liệu thế giới đảo của bạn đã bị mất, bạn đã được gỡ khỏi đảo này."));
            islandData.removeMember(player.getUniqueId());
            if (islandData.getMembers().isEmpty()) islandData.setArchived(true);
            playerData.setIslandId(null);
            dataManager.saveAsync(islandData);
        }));
    }

    public void teleportToIsland(Player player, IslandData islandData) {
        worldManager.loadIslandWorld(islandData.getIslandId())
                .thenAccept(world -> Utils.runLater(() -> TeleportService.getInstance().teleport(player, world.getSpawnLocation(), TeleportOptions.INSTANT)))
                .exceptionally(error -> {
                    Utils.runLater(() -> player.sendMessage(Utils.fromString("<red>Không thể tải thế giới đảo.")));
                    return null;
                });
    }

    /**
     * Sends everyone in the world to the hub, so the world can be unloaded.
     */
    public void evacuate(World world) {
        for (Player player : world.getPlayers()) {
            player.sendMessage(Utils.fromString("<yellow>Đảo đã đóng cửa nên bạn được đưa về hub."));
            TeleportService.getInstance().teleport(player, HubDestination.INSTANCE, TeleportOptions.INSTANT);
        }
    }
}
