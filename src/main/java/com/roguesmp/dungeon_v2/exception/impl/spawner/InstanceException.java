package com.roguesmp.dungeon_v2.exception.impl.spawner;

import com.roguesmp.dungeon_v2.exception.BaseException;

public class InstanceException extends BaseException {
    public InstanceException(String tid) {
        super("Failed to create spawner instance from template: " + tid,
                "Khong the tao spawner instance tu template: " + tid);
    }
}
