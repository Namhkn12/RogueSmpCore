package com.roguesmp.dungeon_v2.exception.impl.spawner;

import com.roguesmp.dungeon_v2.exception.BaseException;

public class SpawnerNotFoundException extends BaseException {
    public SpawnerNotFoundException(String sid, Throwable cause) {
        super("Can not be found spawner with id: " + sid,
                "Khong tim thay spawner: " + sid, cause);
    }
}
