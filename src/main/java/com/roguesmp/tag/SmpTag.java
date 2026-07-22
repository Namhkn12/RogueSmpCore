package com.roguesmp.tag;

import com.google.gson.reflect.TypeToken;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.utils.Utils;
import org.jetbrains.annotations.Unmodifiable;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import java.io.File;
import java.io.FileReader;

public class SmpTag<T> {
    private final String id;
    private final Set<T> elements = new HashSet<>(); // The "Final" pre-calculated set
    private final List<String> rawEntries = new ArrayList<>();
    private final Function<String, T> resolver;

    public SmpTag(String id, Function<String, T> resolver) {
        this.id = id.toLowerCase();
        this.resolver = resolver;
    }

    public void load(RogueSmpCore plugin) {
        File file = new File(plugin.getDataFolder(), "tags/" + id + ".json");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                if (file.createNewFile()) {
                    try (FileWriter writer = new FileWriter(file)) {
                        Utils.GSON.toJson(Collections.emptyList(), writer);
                        RogueSmpCore.LOGGER.info("Created default empty tag file for {}", id);
                    }
                }
            } catch (Exception e) {
                RogueSmpCore.LOGGER.error("Failed to create default tag file for {}", id, e);
                return;
            }
        }

        try (FileReader reader = new FileReader(file)) {
            List<String> data = Utils.GSON.fromJson(reader, new TypeToken<List<String>>(){}.getType());
            rawEntries.clear();
            elements.clear();
            if (data != null) rawEntries.addAll(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resolve(Function<String, SmpTag<T>> tagLookup, Set<String> stack) {
        // If elements isn't empty, we treat it as "already resolved"
        // (Note: This assumes tags aren't intentionally empty)
        if (!elements.isEmpty()) return;

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
                }
            } else {
                T obj = resolver.apply(entry);
                if (obj != null) this.elements.add(obj);
            }
        }

        stack.remove(this.id);
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
