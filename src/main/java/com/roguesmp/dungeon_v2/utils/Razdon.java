package com.roguesmp.dungeon_v2.utils;

import java.util.Random;

public class Razdon {
    private static final Razdon INSTANCE = new Razdon();
    private final Random random;

    private Razdon() {
        this.random = new Random();
    }

    public static Razdon getInstance() {
        return INSTANCE;
    }

    public Random getRandom() {
        return random;
    }

    // Các method tiện ích dùng trực tiếp luôn
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    public int nextIntInRange(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    public double nextDouble() {
        return random.nextDouble();
    }

    public boolean nextBoolean() {
        return random.nextBoolean();
    }
}
