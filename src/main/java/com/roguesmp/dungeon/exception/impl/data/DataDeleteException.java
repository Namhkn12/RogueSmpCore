package com.roguesmp.dungeon.exception.impl.data;

import com.roguesmp.dungeon.exception.BaseException;

public class DataDeleteException extends BaseException {
    public DataDeleteException(String fileName, Throwable cause) {
        super("Fail to delete file: " + fileName, cause);
    }
}
