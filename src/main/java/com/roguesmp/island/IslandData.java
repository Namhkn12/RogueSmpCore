package com.roguesmp.island;

import com.roguesmp.codec.Codec;
import com.roguesmp.island.setting.IslandSettings;
import com.roguesmp.island.setting.Setting;
import com.roguesmp.utils.WorldPos;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class IslandData {

    public static final Codec<IslandData> CODEC = Codec.composite(
            Codec.UUID.fieldOf("islandId").forGetter(IslandData::getIslandId),
            Codec.listOf(Codec.UUID).<Set<UUID>>xmap(HashSet::new, ArrayList::new).optionalFieldOf("members", new HashSet<UUID>()).forGetter(data -> data.members),
            Codec.WORLD_POS.lenientOptionalFieldOf("spawnLocation", null).forGetter(data -> data.spawnLocation),
            Codec.BOOLEAN.optionalFieldOf("archived", false).forGetter(IslandData::isArchived),
            IslandSettings.VALUES_CODEC.optionalFieldOf("settings", new LinkedHashMap<String, Object>()).forGetter(data -> data.settings),
            IslandData::new
    );

    private boolean dirty = true;

    private boolean archived;
    private final UUID islandId;
    private final Set<UUID> members;
    private WorldPos spawnLocation;
    private final Map<String, Object> settings;

    private IslandData(UUID islandId, Set<UUID> members, @Nullable WorldPos spawnLocation, boolean archived, Map<String, Object> settings) {
        this.islandId = islandId;
        this.members = new HashSet<>(members);
        this.spawnLocation = spawnLocation != null ? spawnLocation : defaultSpawn(islandId);
        this.archived = archived;
        this.settings = new LinkedHashMap<>(settings);
        this.dirty = false;
    }

    public IslandData(UUID creatorId) {
        this.islandId = UUID.randomUUID();
        this.members = new HashSet<>();
        this.members.add(creatorId);
        this.spawnLocation = defaultSpawn(islandId);
        this.archived = false;
        this.settings = new LinkedHashMap<>();
    }

    public IslandData(IslandData source) {
        this.islandId = source.islandId;
        this.members = new HashSet<>(source.members);
        this.spawnLocation = source.spawnLocation;
        this.archived = source.archived;
        this.dirty = source.dirty;
        this.settings = new LinkedHashMap<>(source.settings);
    }

    private static WorldPos defaultSpawn(UUID islandId) {
        return new WorldPos(Key.key("polarpaper", IslandWorldManager.WORLD_PREFIX + islandId), 0.5, IslandWorldManager.CENTER_Y + 2, 0.5, 0, 0);
    }

    @SuppressWarnings("unchecked")
    public <T> T getSettingValue(Setting<T> setting) {
        Object value = this.settings.get(setting.id());
        return value != null ? (T) value : setting.defaultValue();
    }

    public <T> void setSettingValue(Setting<T> setting, T value) {
        this.settings.put(setting.id(), value);
        this.dirty = true;
    }

    public UUID getIslandId() {
        return islandId;
    }

    public @Unmodifiable Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public void addMember(UUID playerUuid) {
        members.add(playerUuid);
        this.dirty = true;
    }

    public void removeMember(UUID playerUuid) {
        members.remove(playerUuid);
        this.dirty = true;
    }

    public void setSpawnLocation(Location location) {
        this.spawnLocation = new WorldPos(WorldPos.of(location).world(), location.getBlockX() + 0.5, location.getBlockY(), location.getBlockZ() + 0.5, (int) location.getYaw(), (int) location.getPitch());
        this.dirty = true;
    }

    public Location getSpawnLocationWorld(World world) {
        return spawnLocation.toLocation(world);
    }

    public boolean isMember(UUID playerUuid) {
        return members.contains(playerUuid);
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
        this.dirty = true;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return dirty;
    }
}
