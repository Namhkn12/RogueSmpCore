package com.roguesmp.dungeon_core;

import com.google.gson.JsonElement;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.DynamicOps;
import com.roguesmp.codec.JsonOps;
import com.roguesmp.world.PolarTemplateData;
import com.roguesmp.world.PolarWorldKey;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * One run of a dungeon: its world, its members and the state of each feature. Feature states are kept as saved JSON
 * until the run is bound to its definition, so a run whose dungeon was removed keeps its data untouched.
 */
public class DungeonInstance {

    @SuppressWarnings("unchecked")
    private static final Codec<JsonElement> JSON = new Codec<>() {
        @Override
        public <O> DataResult<O> encode(JsonElement input, DynamicOps<O> ops) {
            return ops == JsonOps.INSTANCE ? DataResult.success((O) input) : DataResult.error("Feature state can only be stored as JSON");
        }

        @Override
        public <O> DataResult<JsonElement> decode(O input, DynamicOps<O> ops) {
            return ops == JsonOps.INSTANCE ? DataResult.success((JsonElement) input) : DataResult.error("Feature state can only be read from JSON");
        }
    };

    public static final Codec<DungeonInstance> CODEC = Codec.composite(
            Codec.UUID.fieldOf("id").forGetter(DungeonInstance::id),
            Codec.STRING.fieldOf("dungeon").forGetter(DungeonInstance::dungeonId),
            PolarWorldKey.CODEC.fieldOf("template").forGetter(DungeonInstance::template),
            PolarTemplateData.CODEC.optionalFieldOf("world").forGetter(instance -> Optional.ofNullable(instance.worldData)),
            Codec.UUID.fieldOf("owner").forGetter(DungeonInstance::ownerId),
            Codec.listOf(Codec.UUID).<Set<UUID>>xmap(LinkedHashSet::new, ArrayList::new).fieldOf("members").forGetter(instance -> instance.members),
            Codec.LONG.fieldOf("created_at").forGetter(DungeonInstance::createdAt),
            Codec.unboundedMap(JSON).optionalFieldOf("features", Map.of()).forGetter(DungeonInstance::encodeStates),
            DungeonInstance::new
    );

    private final UUID id;
    private final String dungeonId;
    private final PolarWorldKey template;
    private final @Nullable PolarTemplateData worldData;
    private final UUID ownerId;
    private final Set<UUID> members;
    private final long createdAt;
    private final Map<String, JsonElement> savedStates = new HashMap<>();
    private final Map<String, Object> states = new HashMap<>();
    private @Nullable DungeonDefinition definition;

    private DungeonInstance(UUID id, String dungeonId, PolarWorldKey template, Optional<PolarTemplateData> worldData, UUID ownerId, Set<UUID> members, long createdAt, Map<String, JsonElement> savedStates) {
        this.id = id;
        this.dungeonId = dungeonId;
        this.template = template;
        this.worldData = worldData.orElse(null);
        this.ownerId = ownerId;
        this.members = new LinkedHashSet<>(members);
        this.createdAt = createdAt;
        this.savedStates.putAll(savedStates);
    }

    public DungeonInstance(String dungeonId, DungeonDefinition definition, PolarTemplateData worldData, UUID ownerId, Set<UUID> members) {
        this(UUID.randomUUID(), dungeonId, definition.template(), Optional.of(worldData), ownerId, members, System.currentTimeMillis(), Map.of());
        this.members.add(ownerId);
        bind(definition);
    }

    /**
     * Attaches the definition and decodes each feature's saved state. A feature without a readable state starts fresh.
     */
    void bind(DungeonDefinition definition) {
        this.definition = definition;
        definition.features().forEach((featureId, feature) -> states.put(featureId, decodeState(featureId, feature)));
        savedStates.clear();
    }

    private <S> S decodeState(String featureId, DungeonFeature<S> feature) {
        JsonElement saved = savedStates.get(featureId);
        if (saved == null) return feature.createState();

        DataResult<S> decoded = feature.stateCodec().decode(saved, JsonOps.INSTANCE);
        if (decoded.isSuccess()) return decoded.result();
        RogueSmpCore.LOGGER.warn("Resetting state of feature {} in dungeon run {}: {}", featureId, id, decoded.error());
        return feature.createState();
    }

    private Map<String, JsonElement> encodeStates() {
        if (definition == null) return savedStates;

        Map<String, JsonElement> encoded = new HashMap<>();
        definition.features().forEach((featureId, feature) -> {
            JsonElement json = encodeState(featureId, feature);
            if (json != null) encoded.put(featureId, json);
        });
        return encoded;
    }

    @SuppressWarnings("unchecked")
    private <S> @Nullable JsonElement encodeState(String featureId, DungeonFeature<S> feature) {
        DataResult<JsonElement> encoded = feature.stateCodec().encode((S) states.get(featureId), JsonOps.INSTANCE);
        if (encoded.isSuccess()) return encoded.result();
        RogueSmpCore.LOGGER.error("Failed to encode state of feature {} in dungeon run {}: {}", featureId, id, encoded.error());
        return null;
    }

    @SuppressWarnings("unchecked")
    <S> S state(String featureId) {
        return (S) states.get(featureId);
    }

    public @Nullable DungeonDefinition definition() {
        return definition;
    }

    public UUID id() {
        return id;
    }

    public String dungeonId() {
        return dungeonId;
    }

    public PolarWorldKey template() {
        return template;
    }

    public @Nullable PolarTemplateData worldData() {
        return worldData;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public Set<UUID> members() {
        return Collections.unmodifiableSet(members);
    }

    public long createdAt() {
        return createdAt;
    }

    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }

    void addMember(UUID playerId) {
        members.add(playerId);
    }

    void removeMember(UUID playerId) {
        members.remove(playerId);
    }
}
