package com.roguesmp.dungeon.data.runtime.session;

/**
 * Identity and ownership metadata for one dungeon run.
 */
public class DungeonSession {

    private String sessionId;
    private String dungeonId;
    private String partyId;
    private String regionId;
    private long startedAt;

    public DungeonSession() {
    }

    public static DungeonSession create(String dungeonId, String regionId, String partyId) {
        DungeonSession session = new DungeonSession();
        session.sessionId = java.util.UUID.randomUUID().toString();
        session.dungeonId = dungeonId;
        session.regionId = regionId;
        session.partyId = partyId;
        session.startedAt = System.currentTimeMillis();
        return session;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getDungeonId() {
        return dungeonId;
    }

    public void setDungeonId(String dungeonId) {
        this.dungeonId = dungeonId;
    }

    public String getPartyId() {
        return partyId;
    }

    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }
}
