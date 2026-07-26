package com.roguesmp.tag;

import com.roguesmp.RogueSmpCore;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * A named, resolved collection of {@code T} elements, defined by a flat list of raw string entries
 * (element ids, or {@code #otherTagId} to include another tag's elements). Owned by a {@link com.roguesmp.registry.Registry}
 * — see {@link com.roguesmp.registry.Registry#loadTagsFrom} for how tag files get turned into these.
 */
public class SmpTag<T> {
    private final String id;
    private final List<String> rawEntries;
    private final Function<String, T> resolver;
    private final Set<T> elements = new HashSet<>();

    private boolean resolved = false;

    public SmpTag(String id, List<String> rawEntries, Function<String, T> resolver) {
        this.id = id.toLowerCase();
        this.rawEntries = rawEntries;
        this.resolver = resolver;
    }

    /**
     * Resolves every raw entry into an element (or, for {@code #nestedId} entries, recursively
     * resolves and inlines that tag's elements). Safe to call more than once — a no-op after the
     * first successful resolve.
     *
     * @param tagLookup resolves a nested tag id to its {@link SmpTag}, scoped to whatever set of
     *                   tags this one is allowed to reference (normally: tags of the same registry)
     * @param stack tracks tag ids currently being resolved, to detect circular {@code #} references
     */
    public void resolve(Function<String, SmpTag<T>> tagLookup, Set<String> stack) {
        if (resolved) return;

        if (stack.contains(this.id)) {
            RogueSmpCore.LOGGER.error("Circular tag dependency detected at: {}", this.id);
            return;
        }

        stack.add(this.id);

        for (String entry : rawEntries) {
            if (entry.startsWith("#")) {
                String nestedId = entry.substring(1).toLowerCase();
                SmpTag<T> nestedTag = tagLookup.apply(nestedId);

                if (nestedTag != null) {
                    // Recursive call: force the child to calculate its elements first
                    nestedTag.resolve(tagLookup, stack);
                    this.elements.addAll(nestedTag.elements);
                } else {
                    RogueSmpCore.LOGGER.warn("Tag '{}' references unknown nested tag '#{}'", this.id, nestedId);
                }
            } else {
                T obj = resolver.apply(entry);
                if (obj != null) this.elements.add(obj);
                else RogueSmpCore.LOGGER.warn("Tag '{}' references unknown entry '{}'", this.id, entry);
            }
        }

        stack.remove(this.id);
        this.resolved = true;
    }

    public boolean contains(T value) {
        return elements.contains(value);
    }

    public @Unmodifiable Set<T> getElements() {
        return Collections.unmodifiableSet(elements);
    }

    public String getId() {
        return id;
    }
}
