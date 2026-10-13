package com.roguesmp.dungeon_static;

import com.roguesmp.codec.Codec;
import com.roguesmp.world.PolarTemplateData;
import com.roguesmp.world.PolarWorldKey;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class StaticDungeonInstance {

    public static final Codec<StaticDungeonInstance> CODEC = Codec.composite(
            Codec.UUID.fieldOf("id").forGetter(StaticDungeonInstance::id),
            Codec.STRING.fieldOf("dungeon").forGetter(StaticDungeonInstance::dungeonId),
            PolarWorldKey.CODEC.fieldOf("template").forGetter(StaticDungeonInstance::template),
            PolarTemplateData.CODEC.optionalFieldOf("world").forGetter(instance -> Optional.ofNullable(instance.worldData)),
            Codec.UUID.fieldOf("owner").forGetter(StaticDungeonInstance::ownerId),
            Codec.listOf(Codec.UUID).<Set<UUID>>xmap(LinkedHashSet::new, ArrayList::new).fieldOf("members").forGetter(instance -> instance.members),
            Codec.LONG.fieldOf("created_at").forGetter(StaticDungeonInstance::createdAt),
            Codec.LONG.fieldOf("expires_at").forGetter(StaticDungeonInstance::expiresAt),
            StaticDungeonInstance::new
    );

    private final UUID id;
    private final String dungeonId;
    private final PolarWorldKey template;
    private final @Nullable PolarTemplateData worldData;
    private final UUID ownerId;
    private final Set<UUID> members;
    private final long createdAt;
    private long expiresAt;

    private StaticDungeonInstance(UUID id, String dungeonId, PolarWorldKey template, Optional<PolarTemplateData> worldData, UUID ownerId, Set<UUID> members, long createdAt, long expiresAt) {
        this.id = id;
        this.dungeonId = dungeonId;
        this.template = template;
        this.worldData = worldData.orElse(null);
        this.ownerId = ownerId;
        this.members = new LinkedHashSet<>(members);
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public StaticDungeonInstance(UUID ownerId, String dungeonId, PolarWorldKey template, PolarTemplateData worldData, long expiresAt) {
        this(UUID.randomUUID(), dungeonId, template, Optional.of(worldData), ownerId, Set.of(ownerId), System.currentTimeMillis(), expiresAt);
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

    public long expiresAt() {
        return expiresAt;
    }

    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }

    public boolean isExpired(long now) {
        return expiresAt <= now;
    }

    public void addMember(UUID playerId) {
        members.add(playerId);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }
}
