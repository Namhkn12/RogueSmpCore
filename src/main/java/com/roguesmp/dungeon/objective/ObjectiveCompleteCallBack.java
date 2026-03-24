package com.roguesmp.dungeon.objective;

@FunctionalInterface
public interface ObjectiveCompleteCallBack {
    void onComplete(IObjective objective);
}
