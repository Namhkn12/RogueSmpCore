package com.roguesmp.dungeon_v2.exception.impl.data;

import com.roguesmp.dungeon_v2.exception.BaseException;

public class DataLoadException extends BaseException {
    public DataLoadException(String dataScr, Throwable cause) {
        super("Fail to load data from source: " + dataScr, "Khong the tai du lieu: " + dataScr, cause);
    }
}
