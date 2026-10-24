package com.roguesmp.entity.boss.hollowheart;

public interface CrystalChallenge {

    enum Outcome { RUNNING, SUCCESS, FAILURE }

    void start(long now);

    Outcome tick(long now, int interval);

    BarState status(long now);

    default void punish() {

    }

    default int empowerOnFailure() {
        return 0;
    }

    void cleanup();
}
