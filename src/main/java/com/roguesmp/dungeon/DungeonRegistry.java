package com.roguesmp.dungeon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguesmp.dungeon.actor.command.DungeonCommand;
import com.roguesmp.dungeon.actor.command.PartyCommand;
import com.roguesmp.dungeon.actor.command.SchemetaCommand;
import com.roguesmp.dungeon.actor.command.TemplateCommand;
import com.roguesmp.dungeon.controller.BuildingController;
import com.roguesmp.dungeon.controller.DungeonController;
import com.roguesmp.dungeon.controller.PartyController;
import com.roguesmp.dungeon.controller.TemplateController;
import com.roguesmp.dungeon.manager.*;
import com.roguesmp.dungeon.repository.*;
import com.roguesmp.dungeon.repository.impl.*;
import com.roguesmp.dungeon.service.*;
import com.roguesmp.dungeon.service.impl.*;
import com.roguesmp.dungeon.task.PartyInviteTask;

public class DungeonRegistry {

    public static void onEnable() {
        // --- Infrastructure ---
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // --- Repository ---
        ISchemetaRepository schemetaRepository = new SchemetaRepository(gson);
        IDungeonRepository dungeonRepository = new DungeonRepository(gson);
        IRegionRepository regionRepository = new RegionRepository();
        IPartyRepository partyRepository = new PartyRepository();
        IInstanceRepository instanceRepository = new InstanceRepository();


        // --- Manager ---
        SchemetaManager schemetaManager = new SchemetaManager(schemetaRepository);
        DungeonManager dungeonManager = new DungeonManager(dungeonRepository);
        RegionManager regionManager = new RegionManager(regionRepository);
        PartyManager partyManager = new PartyManager(partyRepository);
        InstanceManager instanceManager = new InstanceManager(instanceRepository);

        // --- Service ---
        ISchemetaService schemetaService = new SchemetaService(schemetaManager);
        IDungeonService dungeonService = new DungeonService(dungeonManager);
        IRegionService regionService = new RegionService(regionManager);
        IPartyService partyService = new PartyService(partyManager);
        IInstanceService instanceService = new InstanceService(dungeonManager, instanceManager);
        PartyInviteTask partyInviteTask = new PartyInviteTask(partyService);

        // --- Controller ---
        BuildingController buildingController = new BuildingController(schemetaService);
        TemplateController templateController = new TemplateController(dungeonService);
        PartyController partyController = new PartyController(partyService,partyInviteTask);
        DungeonController dungeonController = new DungeonController(partyService, dungeonService, schemetaService, instanceService, regionService);

        // --- Command ---
        new SchemetaCommand(buildingController).register();
        new TemplateCommand(templateController).register();
        new PartyCommand(partyController).register();
        new DungeonCommand(dungeonController).register();

        regionService.onServerStart();
    }

    public static void onDisable(){

    }

}