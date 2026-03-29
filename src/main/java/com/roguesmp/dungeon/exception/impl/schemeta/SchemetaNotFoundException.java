package com.roguesmp.dungeon.exception.impl.schemeta;

import com.roguesmp.dungeon.exception.BaseException;

public class SchemetaNotFoundException extends BaseException {

    public SchemetaNotFoundException(String id) {
        super("Schemeta not found: " + id, "Khong tim thay schemeta: " + id);
    }
}
