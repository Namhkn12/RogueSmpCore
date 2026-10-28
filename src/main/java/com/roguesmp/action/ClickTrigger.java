package com.roguesmp.action;

public enum ClickTrigger {
    RIGHT,
    LEFT,
    ANY;

    public boolean matches(boolean rightClick) {
        return this == ANY || (this == RIGHT) == rightClick;
    }
}
