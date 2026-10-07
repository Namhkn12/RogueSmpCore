package com.roguesmp.island;

import com.roguesmp.island.setting.IslandSettings;
import com.roguesmp.player.PlayerData;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.utils.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class IslandVisitService {

    public static final String BYPASS_PERMISSION = "roguesmp.island.visit.bypass";

    private final PlayerManager playerManager;
    private final IslandDataManager dataManager;
    private final IslandWorldManager worldManager;
    private final IslandTeleportService teleportService;

    public IslandVisitService(PlayerManager playerManager, IslandDataManager dataManager, IslandWorldManager worldManager, IslandTeleportService teleportService) {
        this.playerManager = playerManager;
        this.dataManager = dataManager;
        this.worldManager = worldManager;
        this.teleportService = teleportService;
    }

    /**
     * Visits the island the given player belongs to, whether or not they are online.
     */
    public void visitPlayerIsland(Player visitor, OfflinePlayer owner) {
        Utils.runAsync(() -> {
            PlayerData ownerData = playerManager.getDataManager().getData(owner.getUniqueId());
            UUID islandId = (ownerData != null ? ownerData : playerManager.getDataManager().loadPlayerData(owner.getUniqueId())).getIslandId();
            if (islandId == null) {
                Utils.runLater(() -> visitor.sendMessage(Utils.fromString("<red>Người chơi này chưa có đảo.")));
                return;
            }

            IslandData loaded = dataManager.loadIslandData(islandId);
            Utils.runLater(() -> {
                if (loaded == null || loaded.isArchived()) {
                    visitor.sendMessage(Utils.fromString("<red>Không thể tìm thấy dữ liệu đảo."));
                    return;
                }

                boolean mayVisit = loaded.isMember(visitor.getUniqueId())
                        || visitor.hasPermission(BYPASS_PERMISSION)
                        || loaded.getSettingValue(IslandSettings.ALLOW_GUEST);
                if (!mayVisit) {
                    visitor.sendMessage(Utils.fromString("<red>Đảo này không cho người chơi khác thăm!"));
                    return;
                }

                teleportService.teleportToIsland(visitor, dataManager.cache(loaded));
                visitor.sendMessage(Utils.fromString("<green>Dịch chuyển thành công."));
            });
        });
    }

    /**
     * Flips whether non-members can visit the player's island. When visiting is switched off, visitors currently on the island
     * (except those with the bypass permission) are sent to the hub.
     *
     * @return the new value, or {@code null} if the player is not a member of a loaded island
     */
    public Boolean toggleVisiting(Player member) {
        PlayerData playerData = playerManager.getDataManager().getData(member.getUniqueId());
        IslandData islandData = playerData == null ? null : dataManager.getCachedData(playerData.getIslandId());
        if (islandData == null || !islandData.isMember(member.getUniqueId())) return null;

        boolean allowed = !islandData.getSettingValue(IslandSettings.ALLOW_GUEST);
        islandData.setSettingValue(IslandSettings.ALLOW_GUEST, allowed);
        dataManager.saveAsync(islandData);

        if (!allowed && worldManager.getLoadedWorld(islandData.getIslandId()) != null) {
            for (Player player : worldManager.getLoadedWorld(islandData.getIslandId()).getPlayers()) {
                if (islandData.isMember(player.getUniqueId()) || player.hasPermission(BYPASS_PERMISSION)) continue;

                player.sendMessage(Utils.fromString("<yellow>Chủ đảo đã đóng cửa đảo nên bạn được đưa về hub."));
                teleportService.sendToHub(player);
            }
        }
        return allowed;
    }
}
