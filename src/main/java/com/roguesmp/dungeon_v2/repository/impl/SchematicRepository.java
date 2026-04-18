package com.roguesmp.dungeon_v2.repository.impl;

import com.roguesmp.dungeon.exception.impl.data.DataLoadException;
import com.roguesmp.dungeon.exception.impl.data.DataSaveException;
import com.roguesmp.dungeon_v2.config.DataFolderConfig;
import com.roguesmp.dungeon_v2.repository.ISchematicRepository;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import org.bukkit.plugin.Plugin;

import java.io.*;

public class SchematicRepository implements ISchematicRepository {

    private final File schematicFolder;

    public SchematicRepository(Plugin plugin) {
        this.schematicFolder = new File(plugin.getDataFolder(), DataFolderConfig.getSchematicFolder());
        if (!schematicFolder.exists()) schematicFolder.mkdirs();
    }

    @Override
    public void save(String name, Clipboard clipboard) {
        File file = toFile(name);
        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC
                .getWriter(new FileOutputStream(file))) {
            writer.write(clipboard);
        } catch (IOException e) {
            throw new DataSaveException(file.getName(), e);
        }
    }

    @Override
    public Clipboard load(String name) {
        File file = toFile(name);
        if (!file.exists()) throw new DataLoadException(file.getName(), null);

        try (ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC
                .getReader(new FileInputStream(file))) {
            return reader.read();
        } catch (IOException e) {
            throw new DataLoadException(file.getName(), e);
        }
    }

    @Override
    public void delete(String name) {
        File file = toFile(name);
        if (file.exists()) file.delete();
    }

    @Override
    public boolean exists(String name) {
        return toFile(name).exists();
    }

    private File toFile(String name) {
        return new File(schematicFolder, name + DataFolderConfig.SCHEM_TYPE);
    }
}