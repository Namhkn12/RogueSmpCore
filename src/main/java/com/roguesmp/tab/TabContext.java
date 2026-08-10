package com.roguesmp.tab;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Per-view reactive key/value store. {@link #set} notifies listeners bound to that key so only the elements depending on it re-render. */
public final class TabContext {

    private final Map<String, Object> values = new ConcurrentHashMap<>();
    private final Map<String, Set<Consumer<TabContext>>> listeners = new ConcurrentHashMap<>();

    public void set(String key, Object value) {
        Object previous = value == null ? values.remove(key) : values.put(key, value);
        if (!java.util.Objects.equals(previous, value)) {
            notifyChange(key);
        }
    }

    /** Sets the value and fires listeners even if it's unchanged from before (e.g. a one-off pulse/trigger). */
    public void touch(String key, Object value) {
        if (value == null) {
            values.remove(key);
        } else {
            values.put(key, value);
        }
        notifyChange(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) values.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        return (T) values.getOrDefault(key, defaultValue);
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public void onChange(String key, Consumer<TabContext> listener) {
        listeners.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(listener);
    }

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
