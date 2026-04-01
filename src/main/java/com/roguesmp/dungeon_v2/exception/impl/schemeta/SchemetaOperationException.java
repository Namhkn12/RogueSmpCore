package com.roguesmp.dungeon_v2.exception.impl.schemeta;

import com.roguesmp.dungeon_v2.exception.BaseException;

public class SchemetaOperationException extends BaseException {

    public SchemetaOperationException(String action, Throwable cause) {
        super("Schemeta operation failed: " + action,
                "Khong the thuc hien thao tac schemeta", cause);
    }
}
