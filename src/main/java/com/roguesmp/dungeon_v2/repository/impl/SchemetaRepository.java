package com.roguesmp.dungeon_v2.repository.impl;

import com.google.gson.Gson;
import com.roguesmp.dungeon.exception.impl.data.DataDeleteException;
import com.roguesmp.dungeon.exception.impl.data.DataSaveException;
import com.roguesmp.dungeon.utils.Log4Craft;
import com.roguesmp.dungeon_v2.config.DataFolderConfig;
import com.roguesmp.dungeon_v2.data.definition.Schemeta;
import com.roguesmp.dungeon_v2.repository.ISchemetaRepository;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SchemetaRepository implements ISchemetaRepository {

    private final File schemetaFolder;
    private final Gson gson;

    public SchemetaRepository(Plugin plugin, Gson gson) {
        this.schemetaFolder = new File(plugin.getDataFolder(), DataFolderConfig.getSchemetaFolder());
        this.gson = gson;
        if (!schemetaFolder.exists()) schemetaFolder.mkdirs();
    }

    @Override
    public List<Schemeta> loadAll() {
        List<Schemeta> result = new ArrayList<>();
        File[] files = schemetaFolder.listFiles((dir, name) -> name.endsWith(DataFolderConfig.JSON_TYPE));
        if (files == null) return result;

        for (File file : files) {
            try (Reader reader = Files.newBufferedReader(file.toPath())) {
                Schemeta schemeta = gson.fromJson(reader, Schemeta.class);
                if (schemeta != null && schemeta.getId() != null) {
                    result.add(schemeta);
                }
            } catch (Exception e) {
                Log4Craft.fire("Failed to load schemeta file: " + file.getName(), e);
            }
        }
        return result;
    }

    @Override
    public Optional<Schemeta> findById(String id) {
        File file = toFile(id);
        if (!file.exists()) return Optional.empty();

        try (Reader reader = Files.newBufferedReader(file.toPath())) {
            return Optional.ofNullable(gson.fromJson(reader, Schemeta.class));
        } catch (Exception e) {
            Log4Craft.fire("Failed to load schemeta: " + id, e);
            return Optional.empty();
        }
    }

    @Override
    public void save(Schemeta schemeta) {
        File file = toFile(schemeta.getId());
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(schemeta, writer);
        } catch (IOException e) {
            throw new DataSaveException(file.getName(), e);
        }
    }

    @Override
    public void delete(String id) {
        File file = toFile(id);
        if (file.exists() && !file.delete()) {
            throw new DataDeleteException(file.getName(), null);
        }
    }

    @Override
    public boolean exists(String id) {
        return toFile(id).exists();
    }

    // ── helper ──────────────────────────────────────────────
    private File toFile(String id) {
        return new File(schemetaFolder, id + DataFolderConfig.JSON_TYPE);
    }
}