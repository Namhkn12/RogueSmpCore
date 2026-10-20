package com.roguesmp.island;

import com.roguesmp.block.impl.energy.network.EnergyNetworkData;
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
            Codec.INT.optionalFieldOf("level", 0).forGetter(IslandData::getLevel),
            IslandSettings.VALUES_CODEC.optionalFieldOf("settings", new LinkedHashMap<String, Object>()).forGetter(data -> data.settings),
            Codec.listOf(EnergyNetworkData.CODEC).optionalFieldOf("energyNetworks", List.of()).forGetter(data -> data.energyNetworks),
            Codec.unboundedMap(Codec.INT).optionalFieldOf("blockCounts", Map.of()).forGetter(data -> data.blockCounts),
            IslandData::new
    );

    private boolean dirty = true;

    private boolean archived;
    private int level;
    private final UUID islandId;
    private final Set<UUID> members;
    private WorldPos spawnLocation;
    private final Map<String, Object> settings;
    private final List<EnergyNetworkData> energyNetworks;
    private final Map<String, Integer> blockCounts;

    private IslandData(UUID islandId, Set<UUID> members, @Nullable WorldPos spawnLocation, boolean archived, int level, Map<String, Object> settings, List<EnergyNetworkData> energyNetworks, Map<String, Integer> blockCounts) {
        this.islandId = islandId;
        this.members = new HashSet<>(members);
        this.spawnLocation = spawnLocation != null ? spawnLocation : defaultSpawn(islandId);
        this.archived = archived;
        this.level = level;
        this.settings = new LinkedHashMap<>(settings);
        this.energyNetworks = new ArrayList<>(energyNetworks);
        this.blockCounts = new HashMap<>(blockCounts);
        this.dirty = false;
    }

    public IslandData(UUID creatorId) {
        this.islandId = UUID.randomUUID();
        this.members = new HashSet<>();
        this.members.add(creatorId);
        this.spawnLocation = defaultSpawn(islandId);
        this.archived = false;
        this.settings = new LinkedHashMap<>();
        this.energyNetworks = new ArrayList<>();
        this.blockCounts = new HashMap<>();
    }

    public IslandData(IslandData source) {
        this.islandId = source.islandId;
        this.members = new HashSet<>(source.members);
        this.spawnLocation = source.spawnLocation;
        this.archived = source.archived;
        this.level = source.level;
        this.dirty = source.dirty;
        this.settings = new LinkedHashMap<>(source.settings);
        this.energyNetworks = new ArrayList<>(source.energyNetworks.stream().map(EnergyNetworkData::copy).toList());
        this.blockCounts = new HashMap<>(source.blockCounts);
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

    public @Unmodifiable List<EnergyNetworkData> getEnergyNetworks() {
        return Collections.unmodifiableList(energyNetworks);
    }

    public @Nullable EnergyNetworkData getEnergyNetwork(String id) {
        for (EnergyNetworkData network : energyNetworks) {
            if (network.id().equals(id)) return network;
        }
        return null;
    }

    public void addEnergyNetwork(EnergyNetworkData network) {
        energyNetworks.add(network);
        this.dirty = true;
    }

    public void removeEnergyNetwork(EnergyNetworkData network) {
        if (energyNetworks.remove(network)) this.dirty = true;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
        this.dirty = true;
    }

    public int getBlockCount(String blockType) {
        return blockCounts.getOrDefault(blockType, 0);
    }

    public void changeBlockCount(String blockType, int delta) {
        int count = getBlockCount(blockType) + delta;
        if (count > 0) blockCounts.put(blockType, count);
        else blockCounts.remove(blockType);
        this.dirty = true;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return dirty;
    }
}
