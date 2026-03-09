package com.roguesmp.dungeon.repository;

import com.roguesmp.dungeon.data.Schemeta;
import com.sk89q.worldedit.extent.clipboard.Clipboard;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface ISchemetaRepository {
    List<Schemeta> loadAll();
    void save(Schemeta schemeta) throws IOException;
    void delete(String id);
    File getSchematicFolder();
    void saveSchem(String name, Clipboard clipboard) throws IOException;
    Clipboard loadSchem(String name) throws IOException;
}
