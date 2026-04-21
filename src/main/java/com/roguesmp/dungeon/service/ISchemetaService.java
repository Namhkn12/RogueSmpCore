package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.data.definition.Schemeta;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public interface ISchemetaService {
    // Data management
    Schemeta create(Player player, String name);
    void delete(String id);
    Optional<Schemeta> getById(String id);
    List<Schemeta> getAll();

    // Validation helper (service nên expose)
    boolean exists(String id);
}