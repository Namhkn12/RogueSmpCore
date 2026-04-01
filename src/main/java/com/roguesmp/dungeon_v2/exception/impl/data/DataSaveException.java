package com.roguesmp.dungeon_v2.exception.impl.data;

import com.roguesmp.dungeon_v2.exception.BaseException;

public class DataSaveException extends BaseException {
    public DataSaveException(String filename, Throwable cause) {
        super("Fail to save file: " + filename, "Khong the luu du lieu: " + filename, cause);
    }
}
