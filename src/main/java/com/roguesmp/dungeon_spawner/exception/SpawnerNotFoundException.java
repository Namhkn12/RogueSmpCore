package com.roguesmp.dungeon_spawner.exception;

import com.roguesmp.dungeon.exception.BaseException;

public class SpawnerNotFoundException extends BaseException {
    public SpawnerNotFoundException(String sid, Throwable cause) {
        super("Can not be found spawner with id: " + sid,
                "Khong tim thay spawner: " + sid, cause);
    }
}
