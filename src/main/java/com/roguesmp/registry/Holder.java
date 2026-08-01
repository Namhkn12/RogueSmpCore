package com.roguesmp.registry;

import org.jetbrains.annotations.Nullable;

/**
 * A stable, identity-preserving reference to a {@link Registry} entry by id - mirrors Minecraft's
 * own {@code Holder<T>}. Obtained via {@link Registry#getHolder(String)}, which may hand back an
 * <b>unbound</b> holder if nothing is registered under that id yet; whenever something later
 * calls {@link Registry#register(String, Object)} for that same id, this exact object gets bound
 * to the real value - every earlier caller who already holds this Holder transparently starts
 * seeing it too, no matter what order registration happened in.
 * <p>
 * This also makes references survive {@link Registry#clear()}/reload: a raw cached {@code T}
 * value goes stale the moment the registry reloads under it, but a Holder is the same object
 * before and after - it just gets unbound then rebound as the registry repopulates.
 *
 * @param <T> the type of value held
 */
public class Holder<T> {

    private final String id;
    private @Nullable T value;

    Holder(String id) {
        this.id = id;
    }

    Holder(String id, T value) {
        this.id = id;
        this.value = value;
    }

    void bind(T value) {
        this.value = value;
    }

    void unbind() {
        this.value = null;
    }

    public boolean isBound() {
        return value != null;
    }

    /**
     * @return the referenced value.
     * @throws IllegalStateException if nothing has registered under this id (yet, or ever) -
     * check {@link #isBound()} first if that's expected (e.g. an optional reference).
     */
    public T value() {
        if (value == null) {
            throw new IllegalStateException("Unbound holder: '" + id + "'");
        }
        return value;
    }

    public String getId() {
        return id;
    }
}
