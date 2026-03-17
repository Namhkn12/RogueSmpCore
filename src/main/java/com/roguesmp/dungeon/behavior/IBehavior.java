package com.roguesmp.dungeon.behavior;

public interface IBehavior {
    boolean onSpawn();
    void onTick();
    boolean onBreak();
}
