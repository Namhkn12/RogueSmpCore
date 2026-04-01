package com.roguesmp.dungeon_v2.service;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

public interface ISchematicService {
    BoundingBox paste(String schemetaId, Location location);
}
