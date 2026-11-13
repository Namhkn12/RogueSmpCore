package com.roguesmp.island;

import com.roguesmp.block.BlockData;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.impl.energy.network.EnergyNetworkData;
import com.roguesmp.codec.Codec;
import com.roguesmp.island.setting.IslandSettings;
import com.roguesmp.island.setting.Setting;
import com.roguesmp.registry.Registries;
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
            Codec.unboundedMap(Codec.INT).optionalFieldOf("blockIdCounts", Map.of()).forGetter(data -> data.blockIdCounts),
            Codec.unboundedMap(Codec.INT).optionalFieldOf("levelContributions", Map.of()).forGetter(data -> data.levelContributions),
            Codec.LONG.optionalFieldOf("moneyContribution", 0L).forGetter(IslandData::getMoneyContribution),
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
    private final Map<String, Integer> blockIdCounts;
    private final Map<String, Integer> levelContributions;
    private long moneyContribution;

    private IslandData(UUID islandId, Set<UUID> members, @Nullable WorldPos spawnLocation, boolean archived, int level, Map<String, Object> settings, List<EnergyNetworkData> energyNetworks, Map<String, Integer> blockIdCounts, Map<String, Integer> levelContributions, long moneyContribution) {
        this.islandId = islandId;
        this.members = new HashSet<>(members);
        this.spawnLocation = spawnLocation != null ? spawnLocation : defaultSpawn(islandId);
        this.archived = archived;
        this.level = level;
        this.settings = new LinkedHashMap<>(settings);
        this.energyNetworks = new ArrayList<>(energyNetworks);
        this.blockIdCounts = new HashMap<>(blockIdCounts);
        this.levelContributions = new HashMap<>(levelContributions);
        this.moneyContribution = moneyContribution;
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
        this.blockIdCounts = new HashMap<>();
        this.levelContributions = new HashMap<>();
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
        this.blockIdCounts = new HashMap<>(source.blockIdCounts);
        this.levelContributions = new HashMap<>(source.levelContributions);
        this.moneyContribution = source.moneyContribution;
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

    public int getContribution(String itemId) {
        return levelContributions.getOrDefault(itemId, 0);
    }

    public void addContribution(String itemId, int amount) {
        levelContributions.merge(itemId, amount, Integer::sum);
        this.dirty = true;
    }

    public long getMoneyContribution() {
        return moneyContribution;
    }

    public void addMoneyContribution(long amount) {
        moneyContribution += amount;
        this.dirty = true;
    }

    public void clearContributions() {
        levelContributions.clear();
        moneyContribution = 0;
        this.dirty = true;
    }

    public void changeBlockCount(SmpBlock block, int delta) {
        if (block.getId() != null) adjust(blockIdCounts, block.getId(), delta);
        this.dirty = true;
    }

    public int getUsedCapacity() {
        int used = 0;
        for (Map.Entry<String, Integer> entry : blockIdCounts.entrySet()) {
            BlockData data = Registries.BLOCK.get(entry.getKey());
            if (data != null) used += data.properties().capacityCost() * entry.getValue();
        }
        return used;
    }

    private static void adjust(Map<String, Integer> counts, String key, int delta) {
        int count = counts.getOrDefault(key, 0) + delta;
        if (count > 0) counts.put(key, count);
        else counts.remove(key);
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return dirty;
    }
}
