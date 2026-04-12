package com.roguesmp.dungeon_v2.exception.impl.schemeta;

import com.roguesmp.dungeon_v2.exception.BaseException;

public class SchemetaNotFoundException extends BaseException {

    public SchemetaNotFoundException(String id) {
        super("Schemeta not found: " + id, "Khong tim thay schemeta: " + id);
    }
}
