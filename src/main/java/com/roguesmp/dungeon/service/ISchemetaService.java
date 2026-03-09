package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.data.Schemeta;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public interface ISchemetaService {
    Schemeta createSchemeta(Player player, String name) throws Exception;
    void deleteSchemeta(String id);
    Schemeta getSchemeta(String id);
    List<Schemeta> getSchemetaList();
    List<String> getSchemetaIdList();
    void pasteSchematic(String id, Location location) throws Exception;
}
