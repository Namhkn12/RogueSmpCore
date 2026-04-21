package com.roguesmp.dungeon.data.definition.room.roomevent;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BaseRoomEvent implements RoomEvent, PersistableRoomEvent {

    protected transient RoomEventCallback callback;
    protected transient RoomEventContext context;
    protected boolean started;
    protected boolean ended;
    protected long tickCount;

    public RoomEventCallback getCallback() {
        return callback;
    }

    public void setCallback(RoomEventCallback callback) {
        this.callback = callback;
    }

    public RoomEventContext getContext() {
        return context;
    }

    public void setContext(RoomEventContext context) {
        this.context = context;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isEnded() {
        return ended;
    }

    public long getTickCount() {
        return tickCount;
    }

    @Override
    public void onRoomStart() {
        if (started) return;
        started = true;
        onStartInternal();
        fire(RoomEventPhase.START);
    }

    @Override
    public void onRoomPlay() {
        if (!started || ended) return;
        tickCount++;
        onPlayInternal();
        fire(RoomEventPhase.TICK);
    }

    @Override
    public void onRoomEnd() {
        if (ended) return;
        ended = true;
        onEndInternal();
        fire(RoomEventPhase.END);
    }

    protected void fire(RoomEventPhase phase) {
        if (callback != null) {
            callback.callback(this, phase);
        }
    }

    protected abstract String getType();

    protected void onStartInternal() {}
    protected void onPlayInternal() {}
    protected void onEndInternal() {}

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", getType());
        data.put("started", started);
        data.put("ended", ended);
        data.put("tickCount", tickCount);
        return data;
    }

    @Override
    public void deserialize(Map<String, Object> data) {
        this.started = data.get("started") instanceof Boolean value && value;
        this.ended = data.get("ended") instanceof Boolean value && value;
        this.tickCount = data.get("tickCount") instanceof Number number ? number.longValue() : 0L;
    }
}
