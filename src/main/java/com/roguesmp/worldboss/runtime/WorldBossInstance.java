package com.roguesmp.worldboss.runtime;

import com.roguesmp.entity.SmpEntity;
import com.roguesmp.region.ServerRegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.roguesmp.worldboss.definition.WorldBossDefinition;
import com.roguesmp.worldboss.encounter.WorldBossEncounter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class WorldBossInstance {

    private final WorldBossDefinition definition;
    private final WorldBossEncounter encounter;
    private final Location center;
    private final List<ProtectedRegion> arena;
    private final String siteName;
    private final @Nullable UUID summoner;
    private final Map<UUID, ParticipantState> participants = new LinkedHashMap<>();

    private ArenaState state = ArenaState.OPEN;
    private int secondsLeft;
    private @Nullable SmpEntity boss;
    private @Nullable BukkitTask task;
    private boolean rewarded;

    public WorldBossInstance(WorldBossDefinition definition, WorldBossEncounter encounter, Location center, List<ProtectedRegion> arena,
                             String siteName, @Nullable UUID summoner, int secondsLeft) {
        this.definition = definition;
        this.encounter = encounter;
        this.center = center;
        this.arena = arena;
        this.siteName = siteName;
        this.summoner = summoner;
        this.secondsLeft = secondsLeft;
    }

    public boolean contains(Location location) {
        return location.getWorld() == center.getWorld()
                && ServerRegionManager.contains(arena, location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public boolean isAlive(UUID uuid) {
        return participants.get(uuid) == ParticipantState.ALIVE;
    }

    public boolean isParticipant(UUID uuid) {
        return participants.containsKey(uuid);
    }

    public void addParticipant(UUID uuid) {
        participants.put(uuid, ParticipantState.ALIVE);
    }

    public boolean eliminate(UUID uuid) {
        return participants.replace(uuid, ParticipantState.ALIVE, ParticipantState.ELIMINATED);
    }

    public List<UUID> getAliveParticipants() {
        List<UUID> list = new ArrayList<>();
        for (Map.Entry<UUID, ParticipantState> entry : participants.entrySet()) {
            if (entry.getValue() == ParticipantState.ALIVE) {
                UUID key = entry.getKey();
                list.add(key);
            }
        }
        return list;
    }

    public List<Player> getAlivePlayers() {
        List<Player> list = new ArrayList<>();
        for (UUID uuid : getAliveParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                list.add(player);
            }
        }
        return list;
    }

    public Collection<UUID> getParticipants() {
        return participants.keySet();
    }

    public boolean isBoss(UUID entityUuid) {
        return boss != null && boss.getEntity().getUniqueId().equals(entityUuid);
    }

    public WorldBossDefinition getDefinition() {
        return definition;
    }

    public WorldBossEncounter getEncounter() {
        return encounter;
    }

    public Location getCenter() {
        return center.clone();
    }

    public String getSiteName() {
        return siteName;
    }

    public @Nullable UUID getSummoner() {
        return summoner;
    }

    public ArenaState getState() {
        return state;
    }

    public void setState(ArenaState state) {
        this.state = state;
    }

    public int getSecondsLeft() {
        return secondsLeft;
    }

    public void setSecondsLeft(int secondsLeft) {
        this.secondsLeft = secondsLeft;
    }

    public @Nullable SmpEntity getBoss() {
        return boss;
    }

    public void setBoss(@Nullable SmpEntity boss) {
        this.boss = boss;
    }

    public @Nullable BukkitTask getTask() {
        return task;
    }

    public void setTask(@Nullable BukkitTask task) {
        this.task = task;
    }

    public boolean markRewarded() {
        if (rewarded) return false;
        rewarded = true;
        return true;
    }
}
