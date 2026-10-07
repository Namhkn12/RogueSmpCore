package com.roguesmp.island;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.player.PlayerData;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.utils.Utils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.teleportation.PassengerModes;

public class IslandTeleportService {

    private final PlayerManager playerManager;
    private final IslandDataManager dataManager;
    private final IslandWorldManager worldManager;
    private final MultiverseCoreApi multiverseApi = MultiverseCoreApi.get();

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
        multiverseApi.getWorldManager().getDefaultWorld().peek(hub -> teleport(player, hub.getSpawnLocation()));
    }

    /**
     * Sends everyone in the world to the hub, so the world can be unloaded.
     */
    public void evacuate(World world) {
        multiverseApi.getWorldManager().getDefaultWorld().peek(hub -> {
            for (Player player : world.getPlayers()) {
                player.sendMessage(Utils.fromString("<yellow>Đảo đã đóng cửa nên bạn được đưa về hub."));
                teleport(player, hub.getSpawnLocation());
            }
        });
    }

    /**
     * Teleports through Multiverse's safety teleporter, and falls back to a plain teleport if Multiverse refuses.
     */
    public void teleport(Player player, Location location) {
        multiverseApi.getSafetyTeleporter()
                .to(location)
                .passengerMode(PassengerModes.RETAIN_ALL)
                .teleportSingle(player)
                .onFailure(attempts -> {
                    attempts.forEach(attempt -> {
                        RogueSmpCore.LOGGER.warn("Multiverse could not teleport {} ({}), falling back to a plain teleport", player.getName(), attempt.getFailureMessage());
                    });

                    Utils.runLater(() -> player.teleportAsync(location));
                });
    }

    public MultiverseCoreApi getMultiverseApi() {
        return multiverseApi;
    }
}
