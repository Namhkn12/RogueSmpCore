package com.roguesmp.entity.boss.goblinking;

import java.util.function.IntPredicate;

public interface Hazard {

    boolean tick(int interval);

    default void remove() {

    }

    static Hazard timed(int duration, IntPredicate tick) {
        int[] age = {0};
        return interval -> {
            age[0] += interval;
            return age[0] >= duration || tick.test(age[0]);
        };
    }
}
