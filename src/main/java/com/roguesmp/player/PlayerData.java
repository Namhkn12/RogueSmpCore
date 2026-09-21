package com.roguesmp.player;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.codec.Codec;
import com.roguesmp.player.ability.AbilityLoadout;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

/**
 * A class that hold player-related data
 */
public class PlayerData{

    // Sparse slot list: {"0": "fireball", "2": "dash"} instead of a fixed-size list with null
    // holes, since the Codec/DynamicOps framework has no concept of a null leaf value.
    private static final Codec<Map<String, String>> SPARSE_SLOTS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING);

    private static final Codec<List<String>> SLOTS_CODEC = SPARSE_SLOTS_CODEC.xmap(
            sparse -> {
                List<String> slots = new ArrayList<>(Collections.nCopies(AbilityLoadout.SLOT_COUNT, (String) null));
                sparse.forEach((indexStr, abilityId) -> {
                    try {
                        int index = Integer.parseInt(indexStr);
                        if (index >= 0 && index < AbilityLoadout.SLOT_COUNT) {
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

    // Old per-type format ({"ACTIVE": {"0": ...}, "PASSIVE": {...}, "LIFELINE": {...}}), flattened
    // into the single slot list. Decode-only: withAlternative never encodes through it.
    private static final List<String> LEGACY_TYPE_ORDER = List.of("ACTIVE", "LIFELINE", "PASSIVE");
    private static final Codec<List<String>> LEGACY_SLOTS_CODEC = Codec.unboundedMap(Codec.STRING, SPARSE_SLOTS_CODEC).xmap(
            byType -> {
                List<String> slots = new ArrayList<>(Collections.nCopies(AbilityLoadout.SLOT_COUNT, (String) null));
                int next = 0;
                for (String type : LEGACY_TYPE_ORDER) {
                    Map<String, String> sparse = byType.get(type);
                    if (sparse == null) continue;
                    List<Integer> indices = new ArrayList<>();
                    for (String indexStr : sparse.keySet()) {
                        try {
                            indices.add(Integer.parseInt(indexStr));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    Collections.sort(indices);
                    for (int index : indices) {
                        if (next >= AbilityLoadout.SLOT_COUNT) break;
                        slots.set(next++, sparse.get(String.valueOf(index)));
                    }
                }
                return slots;
            },
            slots -> {
                throw new UnsupportedOperationException("legacy equipped-abilities format is decode-only");
            }
    );

    private static final Codec<List<String>> EQUIPPED_ABILITIES_CODEC = Codec.withAlternative(SLOTS_CODEC, LEGACY_SLOTS_CODEC);

    public static final Codec<PlayerData> CODEC = Codec.composite(
            Codec.UUID.fieldOf("uuid").forGetter(PlayerData::getUuid),
            Codec.INT.optionalFieldOf("level", 0).forGetter(PlayerData::getLevel),
            Codec.UUID.optionalFieldOf("islandId", (UUID) null).forGetter(PlayerData::getIslandId),
            Codec.LONG.optionalFieldOf("money", 0L).forGetter(PlayerData::getMoney),
            Codec.lenientUnboundedMap(Codec.INT).optionalFieldOf("unlockedAbilities", new HashMap<>()).forGetter(PlayerData::getUnlockedAbilities),
            EQUIPPED_ABILITIES_CODEC.optionalFieldOf("equippedAbilities", List.<String>of()).forGetter(PlayerData::getEquippedAbilities),
            Codec.STRING.optionalFieldOf("classId", (String) null).forGetter(PlayerData::getClassId),
            PlayerData::new
    );

    @GsonIgnore
    private boolean dirty = true;

    private final UUID uuid;
    private int level;
    private UUID islandId;
    private final Map<String, Integer> unlockedAbilities;
    private long money;
    private @Nullable String classId;

    private final List<String> equippedAbilities;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.islandId = null;
        this.unlockedAbilities = new HashMap<>();
        this.equippedAbilities = new ArrayList<>(Collections.nCopies(AbilityLoadout.SLOT_COUNT, (String) null));
        this.money = 0;
        this.classId = null;
    }

    /**
     * Reconstruction constructor used when decoding from storage (see {@link #CODEC}).
     */
    private PlayerData(UUID uuid, int level, @Nullable UUID islandId, long money,
                        Map<String, Integer> unlockedAbilities, List<String> equippedAbilities,
                        @Nullable String classId) {
        this.uuid = uuid;
        this.level = level;
        this.islandId = islandId;
        this.money = money;
        this.classId = classId;
        this.unlockedAbilities = new HashMap<>();
        // level <= 0 means "not unlocked" (see setAbilityLevel) - keep that invariant even for
        // data that bypassed setAbilityLevel (e.g. a hand-edited or stale DB record).
        unlockedAbilities.forEach((abilityId, abilityLevel) -> {
            if (abilityLevel > 0) this.unlockedAbilities.put(abilityId, abilityLevel);
        });

        this.equippedAbilities = new ArrayList<>(Collections.nCopies(AbilityLoadout.SLOT_COUNT, (String) null));
        for (int i = 0; i < Math.min(AbilityLoadout.SLOT_COUNT, equippedAbilities.size()); i++) {
            this.equippedAbilities.set(i, equippedAbilities.get(i));
        }

        this.dirty = false;
    }

    public void setEquippedAbility(int index, @Nullable String abilityId) {
        if (index < 0 || index >= AbilityLoadout.SLOT_COUNT) return;

        equippedAbilities.set(index, abilityId);
        this.dirty = true;
    }

    public @Unmodifiable List<String> getEquippedAbilities() {
        return Collections.unmodifiableList(equippedAbilities);
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

    public @Nullable String getClassId() {
        return classId;
    }

    public void setClassId(@Nullable String classId) {
        this.classId = classId;
        this.dirty = true;
    }
}
