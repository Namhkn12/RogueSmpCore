package com.roguesmp.island;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.island.setting.IslandSettings;
import com.roguesmp.island.setting.Setting;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class IslandData {

    @GsonIgnore
    private boolean dirty;

    private boolean archived;
    private final UUID islandId;
    private final Set<UUID> members;
    private final int gridIndex;
    private String spawnLocation; //x,y,z,yaw,pitch (int)
    private final Map<String, Object> settings;

    public IslandData(UUID creatorId, int gridIndex) {
        this.gridIndex = gridIndex;
        this.islandId = UUID.randomUUID();
        this.members = new HashSet<>();
        this.members.add(creatorId);
        this.archived = false;
        this.settings = getDefaultSettings();
    }

    public IslandData(UUID islandId, Set<UUID> members, int gridIndex) {
        this.islandId = islandId;
        this.members = new HashSet<>(members);
        this.gridIndex = gridIndex;
        this.archived = false;
        this.settings = getDefaultSettings();
    }

    //no-args constructor because gson love it
    public IslandData() {
        this.archived = false;
        this.islandId = UUID.randomUUID();
        this.members = new HashSet<>();
        this.gridIndex = -1;
        this.settings = getDefaultSettings();
    }

    public IslandData(IslandData source) {
        this.islandId = source.islandId;
        this.gridIndex = source.gridIndex;
        this.members = new HashSet<>(source.members);
        this.spawnLocation = source.spawnLocation;
        this.archived = source.archived;
        this.dirty = source.dirty;
        this.settings = new LinkedHashMap<>(source.settings);
    }

    private Map<String, Object> getDefaultSettings() {
        Map<String, Object> defaultMap = new LinkedHashMap<>();
        defaultMap.put(IslandSettings.ALLOW_GUEST.id(), IslandSettings.ALLOW_GUEST.defaultValue());
        return defaultMap;
    }

    @SuppressWarnings("unchecked")
    public @Nullable <T> T getSettingValue(Setting<T> setting) {
        return (T) this.settings.get(setting.id());
    }

    public <T> void setSettingValue(Setting<T> setting, T value) {
        this.settings.put(setting.id(), value);
    }

    public UUID getIslandId() {
        return islandId;
    }

    public @Unmodifiable Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public void addMember(UUID playerUuid) {
        members.add(playerUuid);
    }

    public void removeMember(UUID playerUuid) {
        members.remove(playerUuid);
    }

    public int getGridIndex() {
        return gridIndex;
    }

    public void setSpawnLocation(Location location) {
        float x = location.getBlockX() + 0.5f;
        float y = location.getBlockY();
        float z = location.getBlockZ() + 0.5f;
        int yaw = (int) location.getYaw();
        int pitch = (int) location.getPitch();
        this.spawnLocation = x + "," + y + "," + z + "," + yaw + "," + pitch;
    }

    public String getSpawnLocation() {
        return spawnLocation;
    }

    public Location getSpawnLocationWorld(World world) {
        if (this.spawnLocation == null || this.spawnLocation.isEmpty()) {
            return null;
        }

        String[] parts = this.spawnLocation.split(",");

        double x = Double.parseDouble(parts[0]);
        double y = Double.parseDouble(parts[1]);
        double z = Double.parseDouble(parts[2]);
        float yaw = Float.parseFloat(parts[3]);
        float pitch = Float.parseFloat(parts[4]);

        return new Location(world, x, y, z, yaw, pitch);
    }

    public boolean isMember(UUID playerUuid) {
        return members.contains(playerUuid);
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return dirty;
    }

    /**
     * Checks if the island is completely abandoned.
     * Use this to determine if the Slime files can be safely deleted or archived.
     */
    public boolean isEmpty() {
        return members.isEmpty();
    }
}
