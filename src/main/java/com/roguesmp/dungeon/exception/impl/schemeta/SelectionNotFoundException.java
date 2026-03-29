package com.roguesmp.dungeon.exception.impl.schemeta;

import com.roguesmp.dungeon.exception.BaseException;

public class SelectionNotFoundException extends BaseException {

    public SelectionNotFoundException(Throwable cause) {
        super("WorldEdit selection was not found",
                "Ban can chon vung truoc khi luu schemeta", cause);
    }
}
