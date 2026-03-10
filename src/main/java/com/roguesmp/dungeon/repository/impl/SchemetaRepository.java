package com.roguesmp.dungeon.repository.impl;

import com.google.gson.Gson;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.constraint.DungeonConfig;
import com.roguesmp.dungeon.data.Schemeta;
import com.roguesmp.dungeon.repository.ISchemetaRepository;
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
        this.schematicFolder = new File(RogueSmpCore.getInstance().getDataFolder(), DungeonConfig.getSchematicFolder());
        this.schemetaFolder = new File(RogueSmpCore.getInstance().getDataFolder(), DungeonConfig.getSchemetaFolder());
        this.gson = gson;

        if (!schematicFolder.exists()) schematicFolder.mkdirs();
        if (!schemetaFolder.exists()) schemetaFolder.mkdirs();
    }

    @Override
    public List<Schemeta> loadAll() {
        List<Schemeta> schemetas = new ArrayList<>();

        File[] files = schemetaFolder.listFiles((dir, name) -> name.endsWith(DungeonConfig.JSON_TYPE));
        if (files == null) return schemetas;

        for (File file : files) {
            try (Reader reader = Files.newBufferedReader(file.toPath())) {
                Schemeta schemeta = gson.fromJson(reader, Schemeta.class);
                if (schemeta != null && schemeta.getSchemId() != null) {
                    schemetas.add(schemeta);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return schemetas;
    }

    @Override
    public void save(Schemeta schemeta) throws IOException {
        File file = new File(schemetaFolder, schemeta.getSchemId() + DungeonConfig.JSON_TYPE);
        try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(schemeta, writer);
        }
    }

    @Override
    public void delete(String id) {
        new File(schemetaFolder, id + DungeonConfig.JSON_TYPE).delete();
        //new File(schemFolder, id + ".schem").delete();
    }

    @Override
    public File getSchematicFolder() {
        return schematicFolder;
    }

    @Override
    public void saveSchem(String name, Clipboard clipboard) throws IOException {
        File schemFile = new File(schematicFolder, name + DungeonConfig.SCHEM_TYPE);
        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC
                .getWriter(new FileOutputStream(schemFile))) {
            writer.write(clipboard);
        }
    }

    @Override
    public Clipboard loadSchem(String name) throws IOException {
        File schemFile = new File(schematicFolder, name + DungeonConfig.SCHEM_TYPE);
        try (ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC
                .getReader(new FileInputStream(schemFile))) {
            return reader.read();
        }
    }

}