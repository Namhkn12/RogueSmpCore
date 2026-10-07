package com.roguesmp.island;

import com.roguesmp.player.PlayerData;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.utils.Utils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.mvplugins.multiverse.core.teleportation.PassengerModes;

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

        if (!worldManager.isWorldAvailable(islandData.getIslandId())) {
            player.sendMessage(Utils.fromString("<red>Dữ liệu thế giới đảo của bạn đã bị mất, bạn đã được gỡ khỏi đảo này."));
            islandData.removeMember(player.getUniqueId());
            if (islandData.getMembers().isEmpty()) islandData.setArchived(true);
            playerData.setIslandId(null);
            dataManager.saveAsync(islandData);
            return;
        }

        teleportToIsland(player, islandData);
    }

    public void teleportToIsland(Player player, IslandData islandData) {
        worldManager.loadIslandWorld(islandData.getIslandId())
                .thenAccept(world -> Utils.runLater(() -> teleport(player, islandData.getSpawnLocationWorld(world))))
                .exceptionally(error -> {
                    Utils.runLater(() -> player.sendMessage(Utils.fromString("<red>Không thể tải thế giới đảo.")));
                    return null;
                });
    }

    public void sendToHub(Player player) {
        worldManager.getMultiverseApi().getWorldManager().getDefaultWorld()
                .peek(hub -> teleport(player, hub.getSpawnLocation()));
    }

    public void teleport(Player player, Location location) {
        worldManager.getMultiverseApi().getSafetyTeleporter()
                .to(location)
                .passengerMode(PassengerModes.RETAIN_ALL)
                .teleportSingle(player);
    }
}
