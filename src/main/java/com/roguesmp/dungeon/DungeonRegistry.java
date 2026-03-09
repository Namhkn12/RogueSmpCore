package com.roguesmp.dungeon;

import com.google.gson.Gson;
import com.roguesmp.dungeon.actor.command.SchemetaCommand;
import com.roguesmp.dungeon.controller.BuildingController;
import com.roguesmp.dungeon.manager.SchemetaManager;
import com.roguesmp.dungeon.repository.ISchemetaRepository;
import com.roguesmp.dungeon.repository.impl.SchemetaRepository;
import com.roguesmp.dungeon.service.ISchemetaService;
import com.roguesmp.dungeon.service.impl.SchemetaService;

public class DungeonRegistry {

    public static void startUp() {
        // --- Infrastructure ---
        Gson gson = new Gson();

        // --- Repository ---
        ISchemetaRepository schemetaRepository = new SchemetaRepository(gson);

        // --- Manager ---
        SchemetaManager schemetaManager = new SchemetaManager(schemetaRepository);

        // --- Service ---
        ISchemetaService schemetaService = new SchemetaService(schemetaManager);

        // --- Controller ---
        BuildingController buildingController = new BuildingController(schemetaService);

        // --- Command ---
        new SchemetaCommand(buildingController).register();
    }

}