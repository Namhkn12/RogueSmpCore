package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.data.Schemeta;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.List;

public interface ISchemetaService {
    Schemeta createSchemeta(Player player, String name);
    void deleteSchemeta(String id);
    Schemeta getSchemeta(String id);
    List<Schemeta> getSchemetaList();
    List<String> getSchemetaIdList();
    BoundingBox pasteSchematic(String id, Location location);
}
