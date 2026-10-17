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

public class Trie<V> {
    private final Node<V> root = new Node<>();

    public void insert(@NotNull List<String> path, @NotNull V value) {
        Node<V> node = root;
        for (String segment : path) {
            node = node.children.computeIfAbsent(segment, key -> new Node<>());
        }
        node.values.add(value);
    }

    public @NotNull @Unmodifiable List<V> find(@NotNull List<String> path) {
        Node<V> node = walk(path);
        return node == null ? List.of() : Collections.unmodifiableList(node.values);
    }

    public boolean contains(@NotNull List<String> path) {
        Node<V> node = walk(path);
        return node != null && !node.values.isEmpty();
    }

    public @NotNull List<V> collect(@NotNull List<String> prefix) {
        Node<V> node = walk(prefix);
        List<V> results = new ArrayList<>();
        if (node != null) collectFrom(node, results);
        return results;
    }

    public @NotNull List<V> collectAll() {
        return collect(List.of());
    }

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
