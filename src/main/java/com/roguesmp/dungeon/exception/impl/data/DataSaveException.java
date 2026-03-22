package com.roguesmp.dungeon.exception.impl.data;

import com.roguesmp.dungeon.exception.BaseException;

public class DataSaveException extends BaseException {
    public DataSaveException(String filename, Throwable cause) {
        super("Fail to save file: " + filename, cause);
    }
}
