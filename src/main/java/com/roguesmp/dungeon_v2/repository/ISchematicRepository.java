package com.roguesmp.dungeon_v2.repository;

import com.sk89q.worldedit.extent.clipboard.Clipboard;

public interface ISchematicRepository {
    void save(String name, Clipboard clipboard);
    Clipboard load(String name);
    void delete(String name);
    boolean exists(String name);
}
