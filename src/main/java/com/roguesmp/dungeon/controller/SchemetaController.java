package com.roguesmp.dungeon.controller;

import com.roguesmp.dungeon.service.SchemetaService;
import com.roguesmp.dungeon.service.SchematicService;
import org.bukkit.entity.Player;

public class SchemetaController {

    private final SchemetaService schemetaService;
    private final SchematicService schematicService;

    public SchemetaController(SchemetaService schemetaService, SchematicService schematicService) {
        this.schemetaService = schemetaService;
        this.schematicService = schematicService;
    }

    public void handleCreateSchemeta(Player player, String schemame){
        schemetaService.create(player, schemame);
    }

    public void handleBuildSchema(Player player, String schemId){
        schematicService.paste(schemId, player.getLocation());
    }
}
