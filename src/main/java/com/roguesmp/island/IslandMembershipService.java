package com.roguesmp.island;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.player.PlayerData;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class IslandMembershipService {

    public static final int TEAM_SIZE = 4;

    private final PlayerManager playerManager;
    private final IslandDataManager dataManager;
    private final IslandWorldManager worldManager;
    private final IslandRegionManager regionManager;
    private final IslandTeleportService teleportService;

    public IslandMembershipService(PlayerManager playerManager, IslandDataManager dataManager, IslandWorldManager worldManager,
                                   IslandRegionManager regionManager, IslandTeleportService teleportService) {
        this.playerManager = playerManager;
        this.dataManager = dataManager;
        this.worldManager = worldManager;
        this.regionManager = regionManager;
        this.teleportService = teleportService;
    }

    public boolean addMember(UUID islandId, Player recruit) {
        IslandData islandData = dataManager.getCachedData(islandId);
        if (islandData == null || islandData.isArchived()) {
            RogueSmpCore.LOGGER.warn("Attempted to add member to island {}, but it isn't available!", islandId);
            return false;
        }

        if (islandData.getMembers().size() >= TEAM_SIZE) {
            recruit.sendMessage(Utils.fromString("<red>Đảo này đã đạt giới hạn tối đa thành viên <yellow>(" + TEAM_SIZE + "/" + TEAM_SIZE + ")!"));
            return false;
        }

        PlayerData recruitData = playerManager.getDataManager().getData(recruit.getUniqueId());
        if (recruitData == null) return false;

        if (recruitData.getIslandId() != null) {
            recruit.sendMessage(Utils.fromString("<red>Bạn đã là thành viên một hòn đảo rồi! Bạn phải rời đảo cũ trước."));
            return false;
        }

        UUID recruitId = recruit.getUniqueId();
        islandData.addMember(recruitId);
        recruitData.setIslandId(islandId);

        worldManager.loadIslandWorld(islandId)
                .thenAccept(world -> Utils.runLater(() -> regionManager.addTeammate(world, recruitId)));

        dataManager.saveAsync(islandData);
        return true;
    }

    public boolean removeMember(UUID islandId, UUID targetId) {
        IslandData islandData = dataManager.getCachedData(islandId);
        if (islandData == null || !islandData.isMember(targetId)) return false;

        islandData.removeMember(targetId);
        boolean abandoned = islandData.getMembers().isEmpty();
        if (abandoned) islandData.setArchived(true);

        PlayerData playerData = playerManager.getDataManager().getData(targetId);
        if (playerData != null) playerData.setIslandId(null);

        if (!abandoned) {
            worldManager.loadIslandWorld(islandId)
                    .thenAccept(world -> Utils.runLater(() -> regionManager.removeTeammate(world, targetId)));
        }

        Player onlineTarget = Bukkit.getPlayer(targetId);
        if (onlineTarget != null && worldManager.isIslandWorld(onlineTarget.getWorld())) {
            onlineTarget.sendMessage(Utils.fromString("<yellow>Bạn đã trở thành người vô gia cư nên sẽ được hộ tống về hub."));
            teleportService.sendToHub(onlineTarget);
        }

        if (abandoned) worldManager.unloadIslandWorld(islandId);

        dataManager.saveAsync(islandData);
        return true;
    }
}
