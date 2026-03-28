package com.roguesmp.dungeon.exception.impl.data;

import com.roguesmp.dungeon.exception.BaseException;

public class DataLoadException extends BaseException {
    public DataLoadException(String dataScr, Throwable cause) {
        super("Fail to load data from source: " + dataScr, "Khong the tai du lieu: " + dataScr, cause);
    }
}
