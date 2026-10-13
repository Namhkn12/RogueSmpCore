package com.roguesmp.dungeon.repository;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.config.DataFolderConfig;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import org.bukkit.plugin.Plugin;

import java.io.*;

public class SchematicRepository {

    private final File schematicFolder;

    public SchematicRepository(Plugin plugin) {
        this.schematicFolder = new File(plugin.getDataFolder(), DataFolderConfig.getSchematicFolder());
        if (!schematicFolder.exists()) schematicFolder.mkdirs();
    }

    public void save(String name, Clipboard clipboard) {
        File file = toFile(name);
        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC
                .getWriter(new FileOutputStream(file))) {
            writer.write(clipboard);
        } catch (IOException e) {
            RogueSmpCore.LOGGER.error("Failed to save file: {}", file.getName(), e);
        }
    }

    public Clipboard load(String name) {
        File file = toFile(name);
        if (!file.exists()) return null;

        try (ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC
                .getReader(new FileInputStream(file))) {
            return reader.read();
        } catch (IOException e) {
            RogueSmpCore.LOGGER.error("Failed to load file: {}", file.getName(), e);
            return null;
        }
    }

    public void delete(String name) {
        File file = toFile(name);
        if (file.exists()) file.delete();
    }

    public boolean exists(String name) {
        return toFile(name).exists();
    }

    private File toFile(String name) {
        return new File(schematicFolder, name + DataFolderConfig.SCHEM_TYPE);
    }
}