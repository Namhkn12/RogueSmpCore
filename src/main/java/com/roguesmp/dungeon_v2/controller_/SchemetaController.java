package com.roguesmp.dungeon_v2.controller_;

import com.roguesmp.dungeon_v2.service.ISchematicService;
import com.roguesmp.dungeon_v2.service.ISchemetaService;
import org.bukkit.entity.Player;

public class SchemetaController {

    private final ISchemetaService schemetaService;
    private final ISchematicService schematicService;

    public SchemetaController(ISchemetaService schemetaService, ISchematicService schematicService) {
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
