package com.roguesmp.dungeon_v2.service.impl;

import com.roguesmp.dungeon_v2.data.runtime.DeadEntry;
import com.roguesmp.dungeon_v2.data.runtime.Party;
import com.roguesmp.dungeon_v2.data.runtime.session.DungeonPlayer;
import com.roguesmp.dungeon_v2.data.runtime.session.PlayerStatus;
import com.roguesmp.dungeon_v2.manager.InstanceManager;
import com.roguesmp.dungeon_v2.manager.ReviveManager;
import com.roguesmp.dungeon_v2.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon_v2.service.IPartyService;
import com.roguesmp.dungeon_v2.service.IReviveService;
import com.roguesmp.dungeon_v2.utils.DungeonEcho;
import com.roguesmp.dungeon_v2.utils.Teleporter;
import com.roguesmp.dungeon_v2.utils.UuidUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ReviveService implements IReviveService {

    private static final double REVIVE_RADIUS          = 3.0;
    private static final int    PROGRESS_PER_TICK      = 20;  // +1s mỗi lần check
    private static final int    DECAY_PER_TICK         = 10;  // -0.5s khi bị gián đoạn

    private final ReviveManager reviveManager;
    private final IPartyService partyService;
    private final InstanceManager instanceManager;
    private final DungeonPresenter dungeonPresenter;

    public ReviveService(ReviveManager reviveManager, IPartyService partyService, Plugin plugin, InstanceManager instanceManager, DungeonPresenter dungeonPresenter) {
        this.reviveManager = reviveManager;
        this.partyService  = partyService;
        this.instanceManager = instanceManager;
        this.dungeonPresenter = dungeonPresenter;
        // Truyền processTick xuống manager để manager chạy task,
        // nhưng logic thực thi vẫn nằm ở đây.
        reviveManager.startTask(this::processTick);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void registerPlayerDead(Player player) {
        reviveManager.addEntry(player);
        reviveManager.createBossBar(player.getUniqueId(), player);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void processTick() {
        // 1. Lấy manager.getAllEntries() — snapshot để iterate an toàn
        List<DeadEntry> revives = reviveManager.getAllEntries();
        // 2. Với mỗi DeadEntry:
        revives.forEach(deadEntry -> {
            Player player = Bukkit.getPlayer(deadEntry.getDeadID());
            if(player == null) return;
            Party party = partyService.getPartyByPlayer(player);
            if (party == null) return;
            DungeonPlayer dungeonPlayer = instanceManager.get(party.getInstanceId()).getDungeonPlayers();
            if (dungeonPlayer == null) return;
            Location deadLocation = dungeonPlayer.getPlayers().get(player.getUniqueId().toString()).getCheckPoint().toBukkit();
            List<Player> rescuers = findRescuer(dungeonPlayer, player, deadLocation);
            if (rescuers.isEmpty()) {
                deadEntry.decayProgress();
                reviveManager.clearRescuersFromBossBar(player.getUniqueId(), player);
            } else {
                deadEntry.incrementProgress(PROGRESS_PER_TICK);
                syncViewers(deadEntry, rescuers, player);
            }
            if(deadEntry.isReviveComplete()){
                revivePlayer(deadEntry.getDeadID(), dungeonPlayer);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void revivePlayer(UUID deadUUID, DungeonPlayer dungeonPlayer) {
        // 1. Lấy entry từ manager.getEntry(uuid), return nếu empty
        DeadEntry entry = reviveManager.getEntry(deadUUID).orElse(null);
        if(entry == null) return;
        reviveManager.removeEntry(deadUUID);
        reviveManager.removeBossBar(deadUUID);

        // 2. manager.removeEntry(uuid)
        // 3. manager.removeBossBar(uuid)
        // 4. Lấy Player từ Bukkit, return nếu null (đã offline)
        PlayerStatus status = dungeonPlayer.getPlayers().get(UuidUtil.toStringOrNull(deadUUID));
        if (status == null) return;
        Player player = Bukkit.getPlayer(deadUUID);
        if(player != null){
            status.setStatus(PlayerStatus.Status.PLAYING);
            Teleporter.teleport(player, status.getCheckPoint());
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(5);
            dungeonPresenter.onPlayerRevive(player, player.getLocation());
            DungeonEcho.success(player, "Fate grants you another chance!");
        }
        else status.setStatus(PlayerStatus.Status.DISCONNECT);


        // 5. player.setGameMode(SURVIVAL)
        // 6. player.teleport(entry.getDeathLocation())
        // 7. Restore health / food level
        // 8. Gọi presenter.onPlayerRevived(player) — title, sound, effect
    }

    @Override
    public void forceRemoveDeadEntry(UUID uuid) {
        reviveManager.removeEntry(uuid);
        reviveManager.removeBossBar(uuid);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void reviveAll() {
        // 1. Lấy snapshot UUID từ manager.getAllEntries()
        // 2. Gọi revivePlayer(uuid) cho từng UUID
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean isDead(UUID uuid) {
        return reviveManager.hasEntry(uuid);
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tìm rescuer hợp lệ cho dead player.
     *
     * Điều kiện:
     *   - Không phải chính dead player
     *   - Không đang trong trạng thái DEAD (isDead check)
     *   - Cùng world với death location
     *   - Đang sneaking (Shift)
     *   - Trong vòng REVIVE_RADIUS tính từ death location
     *   - Cùng party / cùng dungeon instance (check qua partyService)
     *
     * @return Player rescuer đầu tiên hợp lệ, hoặc null nếu không có
     */
    private List<Player> findRescuer(DungeonPlayer teammates, Player dead, Location deadLocation) {
        Party party = partyService.getPartyByPlayer(dead);
        return partyService.getOnlineMembers(party).stream()
                .filter(player -> !player.equals(dead))
                .filter(player -> {
                    PlayerStatus s = teammates.getPlayers().get(player.getUniqueId().toString());
                    return s != null && s.getStatus().equals(PlayerStatus.Status.PLAYING);
                })
                .filter(Player::isSneaking)
                .filter(player -> player.getLocation().distance(deadLocation) <= REVIVE_RADIUS)
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra 2 player có cùng party/dungeon instance không.
     * Dùng partyService.getPartyByPlayer() rồi so sánh instanceId.
     */
    private boolean sameInstance(Player a, Player b) {
        // 1. Lấy party của a và b
        // 2. Null check cả hai
        // 3. So sánh instanceId
        return false;
    }

    private void syncViewers(DeadEntry entry, List<Player> rescuers, Player dead) {
        UUID deadUUID = dead.getUniqueId();

        Set<Player> currentViewers = reviveManager.getBossBar(deadUUID)
                .map(bar -> new HashSet<>(bar.getPlayers()))
                .orElse(new HashSet<>());

        Set<Player> expectedRescuers = new HashSet<>(rescuers);

        expectedRescuers.forEach(rescuer -> {
            if (!currentViewers.contains(rescuer)) {
                reviveManager.addViewerToBossBar(deadUUID, rescuer);
            }
        });

        currentViewers.forEach(viewer -> {
            if (!viewer.equals(dead) && !expectedRescuers.contains(viewer)) {
                reviveManager.removeRescuerFromBossBar(deadUUID, viewer);
            }
        });

        double progress = entry.getProgressPercent();
        String title = "Reviving " + dead.getName() + " — " + rescuers.size() + " rescuer(s)";
        reviveManager.updateBossBar(deadUUID, title, progress, BarColor.GREEN);
    }
}