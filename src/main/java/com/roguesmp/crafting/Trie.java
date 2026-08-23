package com.roguesmp.crafting;

import com.roguesmp.crafting.recipe.CraftingRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A prefix tree keyed by a sequence of string segments, used to index {@link CraftingRecipe}s for
 * fast lookup by shape/ingredient signature instead of a linear scan over every registered recipe.
 * <p>
 * Each node may hold more than one value (see {@link #insert}) - two recipes can legitimately share
 * the exact same path (e.g. two shaped recipes with the same layout but different per-slot amounts),
 * so callers that need a single match must disambiguate among {@link #find}'s results themselves.
 *
 * @param <V> the value type stored at path endpoints (a recipe)
 */
public class Trie<V> {

    private final Node<V> root = new Node<>();

    /** Inserts {@code value} under {@code path}, creating intermediate nodes as needed. */
    public void insert(@NotNull List<String> path, @NotNull V value) {
        Node<V> node = root;
        for (String segment : path) {
            node = node.children.computeIfAbsent(segment, key -> new Node<>());
        }
        node.values.add(value);
    }

    /** Every value stored at the exact {@code path} - empty if nothing was inserted there. */
    public @NotNull @Unmodifiable List<V> find(@NotNull List<String> path) {
        Node<V> node = walk(path);
        return node == null ? List.of() : Collections.unmodifiableList(node.values);
    }

    /** Whether any value is stored at the exact {@code path}. */
    public boolean contains(@NotNull List<String> path) {
        Node<V> node = walk(path);
        return node != null && !node.values.isEmpty();
    }

    /**
     * Every value stored anywhere in the subtree rooted at {@code prefix} (depth-first), including
     * {@code prefix} itself - an empty prefix collects the whole trie. Useful for browsing/searching
     * (e.g. "every shaped recipe that starts with this width/height") rather than exact matching.
     */
    public @NotNull List<V> collect(@NotNull List<String> prefix) {
        Node<V> node = walk(prefix);
        List<V> results = new ArrayList<>();
        if (node != null) collectFrom(node, results);
        return results;
    }

    /** Every value in the trie, regardless of path. */
    public @NotNull List<V> collectAll() {
        return collect(List.of());
    }

    /** Discards every entry, leaving the trie empty. */
    public void clear() {
        root.children.clear();
        root.values.clear();
    }

    private @Nullable Node<V> walk(List<String> path) {
        Node<V> node = root;
        for (String segment : path) {
            node = node.children.get(segment);
            if (node == null) return null;
        }
        return node;
    }

    private void collectFrom(Node<V> node, List<V> out) {
        out.addAll(node.values);
        for (Node<V> child : node.children.values()) {
            collectFrom(child, out);
        }
    }

    private static class Node<V> {
        final Map<String, Node<V>> children = new HashMap<>();
        final List<V> values = new ArrayList<>();
    }
}
