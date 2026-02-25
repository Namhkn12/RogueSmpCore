package com.roguesmp.constant;

public enum TransferMode {
    NONE,
    AUTO_PUSH,
    AUTO_PULL,
    SMART_PULL;

    public TransferMode next(){
        TransferMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
