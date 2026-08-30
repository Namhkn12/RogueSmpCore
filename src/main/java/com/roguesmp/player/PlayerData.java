package com.roguesmp.player;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.codec.Codec;
import com.roguesmp.player.ability.AbilityType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

/**
 * A class that hold player-related data
 */
public class PlayerData{

    // Sparse per-type slot lists: {"0": "fireball", "2": "dash"} instead of a fixed-size list with
    // null holes, since the Codec/DynamicOps framework has no concept of a null leaf value.
    private static final Codec<Map<String, String>> SPARSE_SLOTS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING);

    private static Codec<List<String>> slotListCodecFor(AbilityType type) {
        int maxSlots = type.getMaxSlots();
        return SPARSE_SLOTS_CODEC.xmap(
                sparse -> {
                    List<String> slots = new ArrayList<>(Collections.nCopies(maxSlots, (String) null));
                    sparse.forEach((indexStr, abilityId) -> {
                        try {
                            int index = Integer.parseInt(indexStr);
                            if (index >= 0 && index < maxSlots) {
                                slots.set(index, abilityId);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    });
                    return slots;
                },
                slots -> {
                    Map<String, String> sparse = new LinkedHashMap<>();
                    for (int i = 0; i < slots.size(); i++) {
                        String abilityId = slots.get(i);
                        if (abilityId != null) {
                            sparse.put(String.valueOf(i), abilityId);
                        }
                    }
                    return sparse;
                }
        );
    }

    private static final Codec<Map<AbilityType, List<String>>> EQUIPPED_ABILITIES_CODEC =
            Codec.dispatchedMap(Codec.enumOf(AbilityType.class), PlayerData::slotListCodecFor);

    public static final Codec<PlayerData> CODEC = Codec.composite(
            Codec.UUID.fieldOf("uuid").forGetter(PlayerData::getUuid),
            Codec.INT.optionalFieldOf("level", 0).forGetter(PlayerData::getLevel),
            Codec.UUID.optionalFieldOf("islandId").forGetter(data -> Optional.ofNullable(data.islandId)),
            Codec.LONG.optionalFieldOf("money", 0L).forGetter(PlayerData::getMoney),
            Codec.lenientUnboundedMap(Codec.INT).optionalFieldOf("unlockedAbilities", new HashMap<>()).forGetter(PlayerData::getUnlockedAbilities),
            EQUIPPED_ABILITIES_CODEC.optionalFieldOf("equippedAbilities", new EnumMap<>(AbilityType.class)).forGetter(PlayerData::getEquippedAbilitiesRaw),
            PlayerData::fromCodec
    );

    private static PlayerData fromCodec(UUID uuid, int level, Optional<UUID> islandId, long money,
                                         Map<String, Integer> unlockedAbilities, Map<AbilityType, List<String>> equippedAbilities) {
        return new PlayerData(uuid, level, islandId.orElse(null), money, unlockedAbilities, equippedAbilities);
    }

    @GsonIgnore
    private boolean dirty = true;

    private final UUID uuid;
    private int level;
    private UUID islandId;
    private final Map<String, Integer> unlockedAbilities;
    private long money;

    private final Map<AbilityType, List<String>> equippedAbilities;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.islandId = null;
        this.unlockedAbilities = new HashMap<>();
        this.equippedAbilities = new EnumMap<>(AbilityType.class);

        // Initialize lists for each type to avoid null checks later
        for (AbilityType type : AbilityType.values()) {
            equippedAbilities.put(type, new ArrayList<>(Collections.nCopies(type.getMaxSlots(), null)));
        }
        this.money = 0;
    }

    /**
     * Reconstruction constructor used when decoding from storage (see {@link #CODEC}).
     */
    private PlayerData(UUID uuid, int level, @Nullable UUID islandId, long money,
                        Map<String, Integer> unlockedAbilities, Map<AbilityType, List<String>> equippedAbilities) {
        this.uuid = uuid;
        this.level = level;
        this.islandId = islandId;
        this.money = money;
        this.unlockedAbilities = new HashMap<>(unlockedAbilities);

        this.equippedAbilities = new EnumMap<>(AbilityType.class);
        for (AbilityType type : AbilityType.values()) {
            List<String> slots = new ArrayList<>(Collections.nCopies(type.getMaxSlots(), (String) null));
            List<String> incoming = equippedAbilities.get(type);
            if (incoming != null) {
                for (int i = 0; i < Math.min(slots.size(), incoming.size()); i++) {
                    slots.set(i, incoming.get(i));
                }
            }
            this.equippedAbilities.put(type, slots);
        }

        this.dirty = false;
    }

    public void setEquippedAbility(AbilityType type, int index, @Nullable String abilityId) {
        List<String> list = equippedAbilities.get(type);
        if (list == null || index < 0 || index >= type.getMaxSlots()) return;

        list.set(index, abilityId);
        this.dirty = true;
    }

    public @Unmodifiable List<String> getEquippedByType(AbilityType type) {
        return Collections.unmodifiableList(equippedAbilities.getOrDefault(type, Collections.emptyList()));
    }

    private Map<AbilityType, List<String>> getEquippedAbilitiesRaw() {
        Map<AbilityType, List<String>> map = new EnumMap<>(AbilityType.class);
        for (AbilityType type : AbilityType.values()) {
            map.put(type, getEquippedByType(type));
        }
        return map;
    }

    public void setAbilityLevel(String abilityId, int level) {
        if (level <= 0) {
            unlockedAbilities.remove(abilityId);
        } else {
            unlockedAbilities.put(abilityId, level);
        }
        this.dirty = true;
    }

    public int getAbilityLevel(String abilityId) {
        return unlockedAbilities.getOrDefault(abilityId, 0);
    }

    public @Unmodifiable Map<String, Integer> getUnlockedAbilities() {
        return Collections.unmodifiableMap(unlockedAbilities);
    }

    public UUID getUuid() { return uuid; }
    public @Nullable UUID getIslandId() {
        return islandId;
    }
    public void setIslandId(@Nullable UUID islandId) {
        this.islandId = islandId;
        this.dirty = true;
    }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; this.dirty = true; }
    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }

    public void setMoney(long money) {
        this.money = money;
        this.dirty = true;
    }

    public long getMoney() {
        return money;
    }
}
