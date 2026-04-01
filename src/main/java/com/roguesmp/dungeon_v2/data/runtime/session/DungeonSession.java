package com.roguesmp.dungeon_v2.data.runtime.session;

import java.util.UUID;

/**
 * Identity and ownership metadata for one dungeon run.
 */
public class DungeonSession {

    private UUID sessionId;
    private String dungeonId;
    private UUID partyId;
    private long startedAt;

    public DungeonSession() {
    }

    public static DungeonSession create(String dungeonId, UUID partyId) {
        DungeonSession session = new DungeonSession();
        session.sessionId = UUID.randomUUID();
        session.dungeonId = dungeonId;
        session.partyId = partyId;
        session.startedAt = System.currentTimeMillis();
        return session;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public String getDungeonId() {
        return dungeonId;
    }

    public void setDungeonId(String dungeonId) {
        this.dungeonId = dungeonId;
    }

    public UUID getPartyId() {
        return partyId;
    }

    public void setPartyId(UUID partyId) {
        this.partyId = partyId;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }
}
