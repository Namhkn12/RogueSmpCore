package com.roguesmp.tab;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Holds the state for one scoreboard or tablist view, as key/value pairs. Setting a value runs every
 * listener registered for that key, so only the elements that actually depend on that key re-render.
 */
public final class TabContext {

    private final Map<String, Object> values = new ConcurrentHashMap<>();
    private final Map<String, Set<Consumer<TabContext>>> listeners = new ConcurrentHashMap<>();

    /** Stores the value under {@code key}. Runs listeners registered for {@code key}, but only if the value changed. */
    public void set(String key, Object value) {
        Object previous = value == null ? values.remove(key) : values.put(key, value);
        if (!java.util.Objects.equals(previous, value)) {
            notifyChange(key);
        }
    }

    /** Same as {@link #set}, but runs listeners even if the value didn't change. Use this for a one-off trigger. */
    public void touch(String key, Object value) {
        if (value == null) {
            values.remove(key);
        } else {
            values.put(key, value);
        }
        notifyChange(key);
    }

    /** Returns the value stored under {@code key}, or {@code null} if nothing is stored there. */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) values.get(key);
    }

    /** Returns the value stored under {@code key}, or {@code defaultValue} if nothing is stored there. */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        return (T) values.getOrDefault(key, defaultValue);
    }

    /** Returns {@code true} if a value is currently stored under {@code key}. */
    public boolean has(String key) {
        return values.containsKey(key);
    }

    /** Registers {@code listener} to run every time {@link #set} or {@link #touch} updates {@code key}. */
    public void onChange(String key, Consumer<TabContext> listener) {
        listeners.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(listener);
    }

    /** Stops {@code listener} from running for {@code key}. Call this when whatever registered the listener is torn down. */
    public void removeListener(String key, Consumer<TabContext> listener) {
        Set<Consumer<TabContext>> set = listeners.get(key);
        if (set != null) {
            set.remove(listener);
        }
    }

    private void notifyChange(String key) {
        Set<Consumer<TabContext>> set = listeners.get(key);
        if (set == null) return;
        for (Consumer<TabContext> listener : set) {
            listener.accept(this);
        }
    }
}
