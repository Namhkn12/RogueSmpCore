package com.roguesmp.dungeon.repository;

import com.roguesmp.dungeon.data.Schemeta;
import com.sk89q.worldedit.extent.clipboard.Clipboard;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface ISchemetaRepository {
    /**
     * Load dữ liệu schemeta trong folder
     */
    List<Schemeta> loadAll();

    /**
     * Lưu 1 schemeta xuống file
     */
    void save(Schemeta schemeta) throws IOException;

    /**
     * Xóa 1 file schemeta
     */
    void delete(String id);

    /**
     * Lưu 1 schemeta cùng schematic
     */
    void saveSchem(String name, Clipboard clipboard) throws IOException;

    /**
     * Load 1 schemeta, trả về clipboard, dùng để paste công trình
     */
    Clipboard loadSchem(String name) throws IOException;
}
