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
 * <p>
 * Tags declared in code (see {@link Tags}) are long-lived: the registry keeps the same instance
 * across reloads and just {@link #reset}s and re-resolves it, so code can hold one directly.
 */
public class SmpTag<T> {
    private final String id;
    private final Function<String, T> resolver;
    private List<String> rawEntries;
    private Set<String> elementIds = Set.of();

    private boolean resolved = false;

    public SmpTag(String id, List<String> rawEntries, Function<String, T> resolver) {
        this.id = id.toLowerCase();
        this.rawEntries = rawEntries;
        this.resolver = resolver;
    }

    /**
     * Discards the resolved elements and swaps in new raw entries, so this same instance can be
     * re-resolved after a reload. Call {@link #resolve} afterwards.
     */
    public void reset(List<String> rawEntries) {
        this.rawEntries = rawEntries;
        this.elementIds = Set.of();
        this.resolved = false;
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

        Set<String> resolvedIds = new HashSet<>();
        for (String entry : rawEntries) {
            if (entry.startsWith("#")) {
                String nestedId = entry.substring(1).toLowerCase();
                SmpTag<T> nestedTag = tagLookup.apply(nestedId);

                if (nestedTag != null) {
                    // Recursive call: force the child to calculate its elements first
                    nestedTag.resolve(tagLookup, stack);
                    resolvedIds.addAll(nestedTag.elementIds);
                } else {
                    RogueSmpCore.LOGGER.warn("Tag '{}' references unknown nested tag '#{}'", this.id, nestedId);
                }
            } else {
                if (resolver.apply(entry) != null) resolvedIds.add(entry);
                else RogueSmpCore.LOGGER.warn("Tag '{}' references unknown entry '{}'", this.id, entry);
            }
        }

        stack.remove(this.id);
        this.elementIds = resolvedIds;
        this.resolved = true;
    }

    public boolean contains(String elementId) {
        return elementIds.contains(elementId);
    }

    /**
     * The tag's raw, unresolved entries - element ids and {@code #otherTagId} references exactly
     * as written in the tag file. Unlike {@link #getElements()} this doesn't require (or reflect)
     * resolution, and is what an editor should read/write back rather than the resolved set.
     */
    public @Unmodifiable List<String> getRawEntries() {
        return Collections.unmodifiableList(rawEntries);
    }

    public @Unmodifiable Set<String> getElementIds() {
        return Collections.unmodifiableSet(elementIds);
    }

    public @Unmodifiable Set<T> getElements() {
        Set<T> elements = new HashSet<>();
        for (String elementId : elementIds) {
            T element = resolver.apply(elementId);
            if (element != null) elements.add(element);
        }
        return Collections.unmodifiableSet(elements);
    }

    public String getId() {
        return id;
    }
}
