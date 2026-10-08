package com.roguesmp.island;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.Utils;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.Set;
import java.util.UUID;

public class IslandPurgeService {

    private final IslandDataManager dataManager;
    private final IslandWorldManager worldManager;
    private final IslandRegionManager regionManager;
    private final IslandTeleportService teleportService;

    public IslandPurgeService(IslandDataManager dataManager, IslandWorldManager worldManager, IslandRegionManager regionManager, IslandTeleportService teleportService) {
        this.dataManager = dataManager;
        this.worldManager = worldManager;
        this.regionManager = regionManager;
        this.teleportService = teleportService;
    }

    public void purgeArchived(CommandSender requester) {
        Utils.runAsync(() -> {
            Set<UUID> archived = dataManager.findArchivedIslandIds();
            Utils.runLater(() -> {
                archived.forEach(this::purge);
                requester.sendMessage(Utils.fromString("<green>Đã dọn dẹp <yellow>" + archived.size() + "<green> đảo bị bỏ hoang."));
            });
        });
    }

    public void purge(UUID islandId) {
        World world = worldManager.getLoadedWorld(islandId);
        String worldName = worldManager.getWorldName(islandId);

        if (world == null) {
            finishPurge(islandId, worldName);
            return;
        }

        regionManager.removeIslandRegion(world);
        teleportService.evacuate(world);
        worldManager.discardWorld(world).thenRun(() -> Utils.runLater(() -> finishPurge(islandId, worldName)));
    }

    private void finishPurge(UUID islandId, String worldName) {
        regionManager.deleteStoredRegionData(worldName);
        Utils.runAsync(() -> {
            worldManager.deleteWorldFile(islandId);
            dataManager.delete(islandId);
            RogueSmpCore.LOGGER.info("Purged abandoned island {}", islandId);
        });
    }
}
