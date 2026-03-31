package com.roguesmp.dungeon_v2.data_.runtime_.dungeon_instance;

import java.util.UUID;

/**
 * Thông tin định danh cơ bản của một dungeon run.
 * Không chứa logic gameplay hay thời gian — chỉ là "ai đang chạy dungeon nào".
 */
public class DungeonSession {

    private String sessionId;
    private String dungeonId;   // ref đến Dungeon.id, không giữ cả object
    private String partyId;
    private long   startedAt;   // epoch ms, dùng để hiển thị / log

    public DungeonSession() {}

    public static DungeonSession create(String dungeonId, String partyId) {
        DungeonSession s = new DungeonSession();
        s.sessionId = UUID.randomUUID().toString();
        s.dungeonId = dungeonId;
        s.partyId   = partyId;
        s.startedAt = System.currentTimeMillis();
        return s;
    }

    // Getters / Setters
    public String getSessionId()           { return sessionId; }
    public void   setSessionId(String v)   { this.sessionId = v; }
    public String getDungeonId()           { return dungeonId; }
    public void   setDungeonId(String v)   { this.dungeonId = v; }
    public String getPartyId()             { return partyId; }
    public void   setPartyId(String v)     { this.partyId = v; }
    public long   getStartedAt()           { return startedAt; }
    public void   setStartedAt(long v)     { this.startedAt = v; }
}