package com.roguesmp.dungeon_v2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.adapter.UUIDTypeAdapter;
import com.roguesmp.dungeon_v2.actor.command.DungeonCommand;
import com.roguesmp.dungeon_v2.actor.command.PartyCommand;
import com.roguesmp.dungeon_v2.actor.command.SchemetaCommand;
import com.roguesmp.dungeon_v2.actor.command.TemplateGenCommand;
import com.roguesmp.dungeon_v2.actor.listener.DoorInteractListener;
import com.roguesmp.dungeon_v2.actor.listener.DungeonListener;
import com.roguesmp.dungeon_v2.actor.listener.LootTableListener;
import com.roguesmp.dungeon_v2.actor.listener.SpawnerEventListener;
import com.roguesmp.dungeon_v2.controller_.*;
import com.roguesmp.dungeon_v2.expansion.DungeonExpansion;
import com.roguesmp.dungeon_v2.itemdisplay.impl.ChestOpenAnimation;
import com.roguesmp.dungeon_v2.manager.*;
import com.roguesmp.dungeon_v2.presentation.EffectManager;
import com.roguesmp.dungeon_v2.presentation.PresentationManager;
import com.roguesmp.dungeon_v2.presentation.ScreenMessManager;
import com.roguesmp.dungeon_v2.presentation.SoundManager;
import com.roguesmp.dungeon_v2.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon_v2.repository.*;
import com.roguesmp.dungeon_v2.repository.impl.*;
import com.roguesmp.dungeon_v2.service.*;
import com.roguesmp.dungeon_v2.service.impl.*;
import com.roguesmp.dungeon_v2.task.PartyInviteTask;
import com.roguesmp.dungeon_v2.task.TaskScheduler;
import com.roguesmp.dungeon_v2.utils.Log4Craft_;
import com.roguesmp.registry.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Placeholder bootstrap for the future dungeon v2 module wiring.
 */
public class DungeonRegistry {

    public static void onEnable(Plugin plugin, ItemRegistry itemRegistry) {
        Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
                .setPrettyPrinting()
                .create();
        /*Scheduler*/
        TaskScheduler taskScheduler = new TaskScheduler(plugin);

        /*ScoreBoard*/
        DungeonExpansion papiExpansion = new DungeonExpansion();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiExpansion.register();
        }
        ScoreBoardManager scoreBoardManager = new ScoreBoardManager(papiExpansion);

        /*Logging and Messaging*/
        Log4Craft_ logger = new Log4Craft_(plugin, "[WDA]");

        /*Presentation*/
        SoundManager soundManager = new SoundManager(plugin);
        EffectManager effectManager = new EffectManager(plugin);
        ScreenMessManager screenMessManager = new ScreenMessManager(plugin);
        PresentationManager presentationManager = new PresentationManager(soundManager, effectManager, screenMessManager);

        /*Presentation Implement*/
        DungeonPresenter dungeonPresenter = new DungeonPresenter(presentationManager);

        /*Repository*/
        ISchematicRepository schematicRepository = new SchematicRepository(plugin);
        ISchemetaRepository schemetaRepository = new SchemetaRepository(plugin, gson);
        IRegionRepository regionRepository = new RegionRepository(plugin, gson, logger);
        IRoomRepository roomRepository = new RoomRepository(plugin, gson, logger);
        IDungeonRepository dungeonRepository = new DungeonRepository(plugin, gson);
        ISpawnerRepository spawnerRepository = new SpawnerRepository(plugin, gson);
        IPartyRepository partyRepository = new PartyRepository(plugin, gson, logger);
        IInstanceRepository instanceRepository = new InstanceRepository(plugin, gson, logger);
        ILootTableRepository lootTableRepository = new LootTableRepository(plugin, gson);

        /*Manager*/
        SchemetaManager schemetaManager = new SchemetaManager(schemetaRepository, schematicRepository);
        RegionManager regionManager = new RegionManager(regionRepository, logger);
        PartyManager partyManager = new PartyManager(partyRepository, logger);
        RoomManager roomManager = new RoomManager(roomRepository, logger);
        DungeonManager dungeonManager = new DungeonManager(dungeonRepository, logger);
        SpawnerManager spawnerManager = new SpawnerManager(spawnerRepository, logger);
        LootTableManager lootTableManager = new LootTableManager(lootTableRepository);

        SpawnerInstanceManager spawnerInstanceManager = new SpawnerInstanceManager();
        InstanceManager instanceManager = new InstanceManager(instanceRepository, scoreBoardManager, logger);

        /*Service*/
        ISchematicService schematicService = new SchematicService(schemetaManager);
        ISchemetaService schemetaService = new SchemetaService(schemetaManager);
        IRoomService roomService = new RoomService(roomManager);
        IPartyService partyService = new PartyService(partyManager);
        IRegionService regionService = new RegionService(regionManager, logger);
        IDungeonService dungeonService = new DungeonService(roomManager, dungeonManager,logger);
        ISpawnerService spawnerService = new SpawnerService(spawnerManager, spawnerInstanceManager, logger);
        IInstanceService instanceService = new InstanceService(instanceManager,
                dungeonManager, roomManager, regionService, dungeonService, partyService, roomService, schematicService);
        ILootService lootService = new LootService(lootTableManager, ItemRegistry.getInstance());
        IDungeonRewardService rewardService = new DungeonRewardService(
                lootService,
                partyService,
                instanceManager,
                dungeonManager,
                new ChestOpenAnimation(plugin, taskScheduler)
                );

        /*Task*/
        PartyInviteTask inviteTask = new PartyInviteTask(plugin, partyService);

        /*Controller*/
        PartyController partyController = new PartyController(partyService, inviteTask);
        DungeonFlowController flowController = new DungeonFlowController(
                instanceService,
                instanceManager,
                partyService,
                regionService,
                scoreBoardManager,
                dungeonManager,
                roomManager,
                roomService,
                dungeonPresenter,
                schematicService,
                dungeonService,
                taskScheduler
        );
        SchemetaController schemetaController = new SchemetaController(schemetaService, schematicService);
        SpawnerEventController spawnerController = new SpawnerEventController(spawnerService, spawnerManager, spawnerInstanceManager, taskScheduler);
        PlayerActionController actionController = new PlayerActionController(instanceService, instanceManager, partyService);
        DungeonTreasureController treasureController = new DungeonTreasureController(rewardService);
        BossRoomController bossRoomController = new BossRoomController(instanceManager, partyService, roomManager);

        /*Command*/
        new TemplateGenCommand(lootService).register();
        new SchemetaCommand(schemetaController, schemetaManager).register();
        new PartyCommand(partyController).register();
        new DungeonCommand(flowController).register();

        /*Listener*/
        Bukkit.getPluginManager().registerEvents(
                new DoorInteractListener(flowController, instanceManager),
                RogueSmpCore.getInstance()
        );

        Bukkit.getPluginManager().registerEvents(
                new SpawnerEventListener(spawnerController, bossRoomController),
                RogueSmpCore.getInstance()
        );

        Bukkit.getPluginManager().registerEvents(
                new DungeonListener(actionController),
                RogueSmpCore.getInstance()
        );

        Bukkit.getPluginManager().registerEvents(
                new LootTableListener(treasureController),
                RogueSmpCore.getInstance()
        );

        /*Runtime tick: refresh scoreboard + monitor dungeon timeout (1s)*/
        taskScheduler.runTimer(20L, 20L, () -> {
            scoreBoardManager.tickUpdate();
            flowController.handleDungeonTimerTick();
        });


    }

    public static void onDisable(Plugin plugin, ItemRegistry itemRegistry) {

    }

}
