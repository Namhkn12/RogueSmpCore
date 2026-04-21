package com.roguesmp.dungeon.data.definition.room.roomevent.impl;

import com.roguesmp.dungeon.data.definition.room.roomevent.BaseRoomEvent;

public class NoopRoomEvent extends BaseRoomEvent {

    public static final String TYPE = "noop";

    @Override
    protected String getType() {
        return TYPE;
    }
}
