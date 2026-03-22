package com.roguesmp.dungeon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.actor.command.*;
import com.roguesmp.dungeon.actor.listener.NextDoorListener;
import com.roguesmp.dungeon.actor.listener.ObjectiveListener;
import com.roguesmp.dungeon.actor.listener.SpawnerListener;
import com.roguesmp.dungeon.controller.*;
import com.roguesmp.dungeon.manager.*;
import com.roguesmp.dungeon.repository.*;
import com.roguesmp.dungeon.repository.impl.*;
import com.roguesmp.dungeon.service.*;
import com.roguesmp.dungeon.service.impl.*;
import com.roguesmp.dungeon.task.PartyInviteTask;
import org.bukkit.Bukkit;

public class DungeonRegistry {

    private static IInstanceService instanceService;
    private static IRegionService regionService;
    private static IPartyService partyService;

    public static void onEnable() {
        // --- Infrastructure ---
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // --- Repository ---
        ISchemetaRepository schemetaRepository = new SchemetaRepository(gson);
        IDungeonRepository dungeonRepository = new DungeonRepository(gson);
        IRegionRepository regionRepository = new RegionRepository();
        IPartyRepository partyRepository = new PartyRepository();
        IInstanceRepository instanceRepository = new InstanceRepository();
        ISpawnerRepository spawnerRepository = new SpawnerRepository(RogueSmpCore.getInstance(), gson);


        // --- Manager ---
        SchemetaManager schemetaManager = new SchemetaManager(schemetaRepository);
        DungeonManager dungeonManager = new DungeonManager(dungeonRepository);
        RegionManager regionManager = new RegionManager(regionRepository);
        PartyManager partyManager = new PartyManager(partyRepository);
        InstanceManager instanceManager = new InstanceManager(instanceRepository);
        SpawnerManager spawnerManager = new SpawnerManager(spawnerRepository);
        SpawnerInstanceManager spawnerInstanceManager = new SpawnerInstanceManager();

        // --- Service ---
        ISchemetaService schemetaService = new SchemetaService(schemetaManager);
        IDungeonService dungeonService = new DungeonService(dungeonManager);
        regionService = new RegionService(regionManager);
        partyService = new PartyService(partyManager);
        instanceService = new InstanceService(dungeonManager, instanceManager);
        PartyInviteTask partyInviteTask = new PartyInviteTask(partyService);
        ISpawnerService spawnerService = new SpawnerService(spawnerManager, spawnerInstanceManager);

        // --- Controller ---
        BuildingController buildingController = new BuildingController(schemetaService);
        TemplateController templateController = new TemplateController(dungeonService);
        PartyController partyController = new PartyController(partyService,partyInviteTask);
        DungeonController dungeonController = new DungeonController(partyService, dungeonService, schemetaService, instanceService, regionService);
        SpawnerController spawnerController = new SpawnerController(spawnerService, RogueSmpCore.getInstance());

        // --- Command ---
        new SchemetaCommand(buildingController).register();
        new TemplateCommand(templateController).register();
        new PartyCommand(partyController).register();
        new DungeonCommand(dungeonController, partyController).register();
        new SpawnerCommand().register();

        // --- Listener ---
        Bukkit.getPluginManager().registerEvents(
                new NextDoorListener(dungeonController, partyController, buildingController),
                RogueSmpCore.getInstance()
        );

        Bukkit.getPluginManager().registerEvents(
                new ObjectiveListener(dungeonController, partyController),
                RogueSmpCore.getInstance()
        );

        Bukkit.getPluginManager().registerEvents(
                new SpawnerListener(spawnerController),
                RogueSmpCore.getInstance()
        );

    }

    public static void onDisable(){
        partyService.savePartyToFile();
        instanceService.onServerStop();
    }

}