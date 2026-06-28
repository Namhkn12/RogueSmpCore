package com.roguesmp.island.setting;

/**
 * A list of setting instances can be found at {@link IslandSettings}
 * @param <T> the type of value that the setting will store
 */
public record Setting<T>(String id, T defaultValue) {
}
