package com.roguesmp.tab;

public record TabKey<T>(String name) {

    public static <T> TabKey<T> of(String name) {
        return new TabKey<>(name);
    }
}
