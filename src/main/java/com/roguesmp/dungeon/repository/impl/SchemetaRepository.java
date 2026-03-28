package com.roguesmp.dungeon.repository.impl;

import com.google.gson.Gson;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.constant.DataConfig;
import com.roguesmp.dungeon.data.Schemeta;
import com.roguesmp.dungeon.exception.impl.data.DataDeleteException;
import com.roguesmp.dungeon.exception.impl.data.DataLoadException;
import com.roguesmp.dungeon.exception.impl.data.DataSaveException;
import com.roguesmp.dungeon.repository.ISchemetaRepository;
import com.roguesmp.dungeon.utils.Log4Craft;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class SchemetaRepository implements ISchemetaRepository {

    private final File schematicFolder;
    private final File schemetaFolder;
    private final Gson gson;

    public SchemetaRepository(Gson gson) {
        this.schematicFolder = new File(RogueSmpCore.getInstance().getDataFolder(), DataConfig.getSchematicFolder());
        this.schemetaFolder = new File(RogueSmpCore.getInstance().getDataFolder(), DataConfig.getSchemetaFolder());
        this.gson = gson;

        if (!schematicFolder.exists()) schematicFolder.mkdirs();
        if (!schemetaFolder.exists()) schemetaFolder.mkdirs();
    }

    @Override
    public List<Schemeta> loadAll() {
        List<Schemeta> schemetas = new ArrayList<>();

        File[] files = schemetaFolder.listFiles((dir, name) -> name.endsWith(DataConfig.JSON_TYPE));
        if (files == null) return schemetas;

        for (File file : files) {
            try (Reader reader = Files.newBufferedReader(file.toPath())) {
                Schemeta schemeta = gson.fromJson(reader, Schemeta.class);
                if (schemeta != null && schemeta.getSchemId() != null) {
                    schemetas.add(schemeta);
                }
            } catch (Exception e) {
                Log4Craft.fire("Failed to load schemeta file: " + file.getName(), e);
            }
        }
        return schemetas;
    }

    @Override
    public void save(Schemeta schemeta) {
        File file = new File(schemetaFolder,  schemeta.getSchemId() + DataConfig.JSON_TYPE);
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(schemeta, writer);
        } catch (IOException e) {
            throw new DataSaveException(file.getName(), e);
        }
    }

    @Override
    public void delete(String id) {
        File file = new File(schemetaFolder, id + DataConfig.JSON_TYPE);
        if (file.exists() && !file.delete()) {
            throw new DataDeleteException(file.getName(), null);
        }
    }

    @Override
    public void saveSchem(String name, Clipboard clipboard) {
        File schemFile = new File(schematicFolder, name + DataConfig.SCHEM_TYPE);
        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC
                .getWriter(new FileOutputStream(schemFile))) {
            writer.write(clipboard);
        } catch (IOException e) {
            throw new DataSaveException(schemFile.getName(), e);
        }
    }

    @Override
    public Clipboard loadSchem(String name) {
        File schemFile = new File(schematicFolder, name + DataConfig.SCHEM_TYPE);
        if (!schemFile.exists()) {
            throw new DataLoadException(schemFile.getName(), null);
        }
        try (ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC
                .getReader(new FileInputStream(schemFile))) {
            return reader.read();
        } catch (IOException e) {
            throw new DataLoadException(schemFile.getName(), e);
        }
    }

}
