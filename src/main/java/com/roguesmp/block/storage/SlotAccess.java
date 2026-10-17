package com.roguesmp.block.storage;

public enum SlotAccess {
    NONE(false, false),
    INSERT(true, false),
    EXTRACT(false, true),
    BOTH(true, true);

    private final boolean insert;
    private final boolean extract;

    SlotAccess(boolean insert, boolean extract) {
        this.insert = insert;
        this.extract = extract;
    }

    public boolean allowsInsert() {
        return insert;
    }

    public boolean allowsExtract() {
        return extract;
    }
}
