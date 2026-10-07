package com.roguesmp.island;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.player.PlayerData;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.mvplugins.multiverse.core.teleportation.PassengerModes;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class IslandManager {

    private static IslandManager INSTANCE;

    private final PlayerManager playerManager;
    private final IslandWorldManager islandWorldManager;
    private final IslandDataManager islandDataManager;
    private final IslandRegionManager islandRegionManager;
    private final IslandTeleportService teleportService;
    private final IslandMembershipService membershipService;
    private final IslandInviteManager inviteManager;
    private final IslandPurgeService purgeService;
    private final IslandVisitService visitService;
    private final Set<UUID> creating = new HashSet<>();

    private IslandManager(RogueSmpCore plugin, PlayerManager playerManager) {
        this.playerManager = playerManager;
        this.islandWorldManager = new IslandWorldManager(plugin);
        this.islandDataManager = new IslandDataManager(plugin);
        this.islandRegionManager = new IslandRegionManager();
        this.teleportService = new IslandTeleportService(playerManager, islandDataManager, islandWorldManager);
        this.membershipService = new IslandMembershipService(playerManager, islandDataManager, islandWorldManager, islandRegionManager, teleportService);
        this.inviteManager = new IslandInviteManager(playerManager, islandDataManager, membershipService);
        this.purgeService = new IslandPurgeService(islandDataManager, islandWorldManager, islandRegionManager);
        this.visitService = new IslandVisitService(playerManager, islandDataManager, islandWorldManager, teleportService);
    }

    public void createIsland(Player creator) {
        UUID creatorId = creator.getUniqueId();
        if (!creating.add(creatorId)) {
            creator.sendMessage(Utils.fromString("<red>Đảo của bạn đang được tạo, vui lòng chờ!"));
            return;
        }

        IslandData islandData = new IslandData(creatorId);
        islandWorldManager.createIslandWorld(islandData.getIslandId())
                .thenAccept(world -> Utils.runLater(() -> {
                    creating.remove(creatorId);
                    if (creator.isOnline()) finishIslandCreation(creator, islandData, world);
                    else purgeService.purge(islandData.getIslandId());
                }))
                .exceptionally(error -> {
                    RogueSmpCore.LOGGER.error("Failed to create island world for {}", creatorId, error);
                    Utils.runLater(() -> {
                        creating.remove(creatorId);
                        purgeService.purge(islandData.getIslandId());
                        creator.sendMessage(Utils.fromString("<red>Không thể tạo đảo, vui lòng thử lại sau."));
                    });
                    return null;
                });
    }

    private void finishIslandCreation(Player creator, IslandData islandData, World world) {
        Location spawnPoint = islandWorldManager.getDefaultSpawn(world);
        islandRegionManager.createIslandRegion(world, creator.getUniqueId());

        PlayerData playerData = playerManager.getDataManager().getData(creator.getUniqueId());
        if (playerData != null) playerData.setIslandId(islandData.getIslandId());

        islandDataManager.cache(islandData);
        islandDataManager.saveAsync(islandData);

        islandWorldManager.getMultiverseApi().getSafetyTeleporter()
                .to(spawnPoint)
                .passengerMode(PassengerModes.RETAIN_ALL)
                .teleportSingle(creator)
                .onSuccess(() -> creator.setRespawnLocation(spawnPoint, true));

        creator.sendMessage(Utils.fromString("<green>Tạo đảo thành công!"));
    }

    public boolean setIslandSpawn(Player player, Location targetLocation) {
        IslandData islandData = getIslandByWorld(player.getWorld());
        if (islandData == null || !islandData.isMember(player.getUniqueId())) {
            player.sendMessage(Utils.fromString("<red>Hành động này chỉ hoạt động ở đảo cá nhân."));
            return false;
        }

        boolean hasGround = !targetLocation.clone().subtract(0, 1, 0).getBlock().getType().isAir();
        if (!hasGround || !targetLocation.getWorld().getWorldBorder().isInside(targetLocation)) {
            player.sendMessage(Utils.fromString("<red>Không thể đặt spawn ở vị trí này"));
            return false;
        }

        islandData.setSpawnLocation(targetLocation);
        islandDataManager.saveAsync(islandData);
        return true;
    }

    public @Nullable IslandData getIslandByWorld(World world) {
        UUID islandId = islandWorldManager.getIslandId(world);
        return islandId == null ? null : islandDataManager.getCachedData(islandId);
    }

    /**
     * Unloads the island and evicts its data from the cache when none of its members, other than the given player who is leaving the server, is online.
     */
    public void releaseIslandIfUnoccupied(IslandData islandData, @Nullable UUID leavingPlayerId) {
        for (UUID memberId : islandData.getMembers()) {
            if (memberId.equals(leavingPlayerId)) continue;

            Player onlineMember = Bukkit.getPlayer(memberId);
            if (onlineMember != null && onlineMember.isOnline()) return;
        }

        islandDataManager.removeCache(islandData.getIslandId());
        if (islandData.isDirty()) islandDataManager.saveAsync(islandData);
        Utils.runLater(() -> islandWorldManager.unloadIslandWorld(islandData.getIslandId()));
    }

    public void onDisable() {
        for (World world : Bukkit.getWorlds()) {
            if (islandWorldManager.isIslandWorld(world)) islandRegionManager.saveNow(world);
        }
        islandDataManager.onDisable();
    }

    public IslandWorldManager getIslandWorldManager() {
        return islandWorldManager;
    }

    public IslandDataManager getIslandDataManager() {
        return islandDataManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public IslandTeleportService getTeleportService() {
        return teleportService;
    }

    public IslandMembershipService getMembershipService() {
        return membershipService;
    }

    public IslandInviteManager getInviteManager() {
        return inviteManager;
    }

    public IslandVisitService getVisitService() {
        return visitService;
    }

    public IslandPurgeService getPurgeService() {
        return purgeService;
    }

    public static void init(RogueSmpCore plugin, PlayerManager playerManager) {
        INSTANCE = new IslandManager(plugin, playerManager);
    }

    public static IslandManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("IslandManager is null!");
        }
        return INSTANCE;
    }
}
