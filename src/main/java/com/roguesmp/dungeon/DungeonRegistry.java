package com.roguesmp.dungeon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguesmp.dungeon.actor.command.SchemetaCommand;
import com.roguesmp.dungeon.actor.command.TemplateCommand;
import com.roguesmp.dungeon.controller.BuildingController;
import com.roguesmp.dungeon.controller.TemplateController;
import com.roguesmp.dungeon.manager.DungeonManager;
import com.roguesmp.dungeon.manager.SchemetaManager;
import com.roguesmp.dungeon.repository.IDungeonRepository;
import com.roguesmp.dungeon.repository.ISchemetaRepository;
import com.roguesmp.dungeon.repository.impl.DungeonRepository;
import com.roguesmp.dungeon.repository.impl.SchemetaRepository;
import com.roguesmp.dungeon.service.IDungeonService;
import com.roguesmp.dungeon.service.ISchemetaService;
import com.roguesmp.dungeon.service.impl.DungeonService;
import com.roguesmp.dungeon.service.impl.SchemetaService;

public class DungeonRegistry {

    public static void startUp() {
        // --- Infrastructure ---
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // --- Repository ---
        ISchemetaRepository schemetaRepository = new SchemetaRepository(gson);
        IDungeonRepository dungeonRepository = new DungeonRepository(gson);

        // --- Manager ---
        SchemetaManager schemetaManager = new SchemetaManager(schemetaRepository);
        DungeonManager dungeonManager = new DungeonManager(dungeonRepository);

        // --- Service ---
        ISchemetaService schemetaService = new SchemetaService(schemetaManager);
        IDungeonService dungeonService = new DungeonService(dungeonManager);

        // --- Controller ---
        BuildingController buildingController = new BuildingController(schemetaService);
        TemplateController templateController = new TemplateController(dungeonService);

        // --- Command ---
        new SchemetaCommand(buildingController).register();
        new TemplateCommand(templateController).register();
    }

}