package com.roguesmp.dungeon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.actor.command.*;
import com.roguesmp.dungeon.actor.command.LootTableCommand;
import com.roguesmp.dungeon.actor.listener.LootTableListener;
import com.roguesmp.dungeon.actor.listener.NextDoorListener;
import com.roguesmp.dungeon.actor.listener.ObjectiveListener;
import com.roguesmp.dungeon.actor.listener.SpawnerListener;
import com.roguesmp.dungeon.adapter.LocationAdapter;
import com.roguesmp.dungeon.adapter.ObjectiveAdapter_;
import com.roguesmp.dungeon.adapter.UUIDTypeAdapter;
import com.roguesmp.dungeon.controller.*;
import com.roguesmp.dungeon.expansion.DungeonExpansion;
import com.roguesmp.dungeon.manager.*;
import com.roguesmp.dungeon.objective_.IObjective;
import com.roguesmp.dungeon.presentation.EffectManager;
import com.roguesmp.dungeon.presentation.PresentationManager;
import com.roguesmp.dungeon.presentation.ScreenMessManager;
import com.roguesmp.dungeon.presentation.SoundManager;
import com.roguesmp.dungeon.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon.repository.*;
import com.roguesmp.dungeon.repository.impl.*;
import com.roguesmp.dungeon.schedule.DungeonTickTask;
import com.roguesmp.dungeon.service.*;
import com.roguesmp.dungeon.service.impl.*;
import com.roguesmp.dungeon.task.PartyInviteTask;
import com.roguesmp.registry.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class DungeonRegistry {

    private static IInstanceService instanceService;
    private static IRegionService regionService;
    private static IPartyService partyService;

    public static void onEnable(Plugin plugin, ItemRegistry itemRegistry) {
        // --- Infrastructure ---
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Location.class, new LocationAdapter())
                .registerTypeAdapter(IObjective.class, new ObjectiveAdapter_())
                .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
                .setPrettyPrinting()
                .create();

        // --- ScoreBoard ---
        DungeonExpansion papiExpansion = new DungeonExpansion();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiExpansion.register();
        }
        ScoreBoardManager scoreBoardManager = new ScoreBoardManager(papiExpansion);


        // --- Present ---
        SoundManager soundManager = new SoundManager(plugin);
        EffectManager effectManager = new EffectManager(plugin);
        ScreenMessManager screenMessManager = new ScreenMessManager(plugin);
        PresentationManager presentationManager = new PresentationManager(soundManager, effectManager, screenMessManager);

        DungeonPresenter dungeonPresenter = new DungeonPresenter(presentationManager);

        // --- Repository ---
        ISchemetaRepository schemetaRepository = new SchemetaRepository(gson);
        IDungeonRepository dungeonRepository = new DungeonRepository(gson);
        IRegionRepository regionRepository = new RegionRepository(gson);
        IPartyRepository partyRepository = new PartyRepository(gson);
        IInstanceRepository instanceRepository = new InstanceRepository(gson);
        ISpawnerRepository spawnerRepository = new SpawnerRepository(plugin, gson);
        ILootTableRepository lootTableRepository = new LootTableRepository(plugin, gson);

        // --- Manager ---
        SchemetaManager schemetaManager = new SchemetaManager(schemetaRepository);
        DungeonManager dungeonManager = new DungeonManager(dungeonRepository);
        RegionManager regionManager = new RegionManager(regionRepository);
        PartyManager partyManager = new PartyManager(partyRepository);
        InstanceManager instanceManager = new InstanceManager(instanceRepository, scoreBoardManager);
        SpawnerManager spawnerManager = new SpawnerManager(spawnerRepository);
        SpawnerInstanceManager spawnerInstanceManager = new SpawnerInstanceManager();
        LootTableManager lootTableManager = new LootTableManager(lootTableRepository);
        lootTableManager.reload();

        // --- Service ---
        ISchemetaService schemetaService = new SchemetaService(schemetaManager);
        IDungeonService dungeonService = new DungeonService(dungeonManager);
        regionService = new RegionService(regionManager);
        partyService = new PartyService(partyManager);
        instanceService = new InstanceService(dungeonManager, instanceManager);
        PartyInviteTask partyInviteTask = new PartyInviteTask(partyService);
        ISpawnerService spawnerService = new SpawnerService(spawnerManager, spawnerInstanceManager);
        ILootService lootService = new LootService(lootTableManager, itemRegistry);
        IDungeonRewardService rewardService = new DungeonRewardService(lootService, instanceService, partyService, dungeonService);
        DungeonFlowService dungeonFlowService = new DungeonFlowService(partyService, dungeonPresenter, instanceService);

        // --- Controller ---
        BuildingController buildingController = new BuildingController(schemetaService);
        TemplateController templateController = new TemplateController(dungeonService);
        PartyController partyController = new PartyController(partyService,partyInviteTask);
        DungeonController dungeonController = new DungeonController(partyService, dungeonService, schemetaService, instanceService, regionService, scoreBoardManager, dungeonPresenter, dungeonFlowService);
        SpawnerController spawnerController = new SpawnerController(spawnerService, RogueSmpCore.getInstance());
        DungeonTreasureController treasureController = new DungeonTreasureController(rewardService);

        //Task
        new DungeonTickTask(scoreBoardManager, instanceService)
                .runTaskTimer(RogueSmpCore.getInstance(), 0L, 20L);

        // --- Command ---
        new SchemetaCommand(buildingController).register();
        new TemplateCommand(templateController).register();
        new PartyCommand(partyController).register();
        new DungeonCommand(dungeonController, partyController).register();
        new SpawnerCommand().register();
        new LootTableCommand(lootService).register();

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

        Bukkit.getPluginManager().registerEvents(
                new LootTableListener(treasureController),
                RogueSmpCore.getInstance()
        );

    }

    public static void onDisable(){
        partyService.savePartyToFile();
        instanceService.onServerStop();
    }

}