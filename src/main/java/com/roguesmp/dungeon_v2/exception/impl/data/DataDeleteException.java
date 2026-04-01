package com.roguesmp.dungeon_v2.exception.impl.data;

import com.roguesmp.dungeon_v2.exception.BaseException;

public class DataDeleteException extends BaseException {
    public DataDeleteException(String fileName, Throwable cause) {
        super("Fail to delete file: " + fileName, "Khong the xoa du lieu: " + fileName, cause);
    }
}
