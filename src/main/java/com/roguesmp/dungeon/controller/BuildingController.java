package com.roguesmp.dungeon.controller;

import com.roguesmp.dungeon.service.ISchemetaService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.List;

public class BuildingController {

    private final ISchemetaService schemetaService;

    public BuildingController(ISchemetaService schemetaService) {
        this.schemetaService = schemetaService;
    }

    public void createNewSchematic(Player executor, String schemName) throws Exception {
        schemetaService.createSchemeta(executor, schemName);
    }

    public BoundingBox buildSchematicById(String schemetaId, Location location) throws Exception {
        return schemetaService.pasteSchematic(schemetaId, location);
    }

    public List<String> getSchematicIdList(){
        return schemetaService.getSchemetaIdList();
    }
}
