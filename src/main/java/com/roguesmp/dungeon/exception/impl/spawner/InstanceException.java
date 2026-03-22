package com.roguesmp.dungeon.exception.impl.spawner;

import com.roguesmp.dungeon.exception.BaseException;

public class InstanceException extends BaseException {
    public InstanceException(String tid) {
        super("Failed to create spawner instance from template: " + tid);
    }
}
