package com.roguesmp.dungeon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.actor.command.DungeonCommand;
import com.roguesmp.dungeon.actor.command.PartyCommand;
import com.roguesmp.dungeon.actor.command.SchemetaCommand;
import com.roguesmp.dungeon.actor.command.TemplateGenCommand;
import com.roguesmp.dungeon.actor.listener.DoorInteractListener;
import com.roguesmp.dungeon.actor.listener.DungeonListener;
import com.roguesmp.dungeon.actor.listener.LootTableListener;
import com.roguesmp.dungeon.actor.listener.SpawnerEventListener;
import com.roguesmp.dungeon.controller.*;
import com.roguesmp.dungeon.expansion.DungeonExpansion;
import com.roguesmp.dungeon.itemdisplay.impl.ChestOpenAnimation;
import com.roguesmp.dungeon.manager.*;
import com.roguesmp.dungeon.presentation.*;
import com.roguesmp.dungeon.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon.presentation.presenter.RevivePointPresenter;
import com.roguesmp.dungeon.repository.*;
import com.roguesmp.dungeon.repository.impl.*;
import com.roguesmp.dungeon.service.*;
import com.roguesmp.dungeon.service.impl.*;
import com.roguesmp.dungeon.task.PartyInviteTask;
import com.roguesmp.dungeon.task.TaskScheduler;
import com.roguesmp.dungeon.utils.Log4Craft_;
import com.roguesmp.dungeon.utils.adapter.UUIDTypeAdapter;
import com.roguesmp.registry.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.UUID;

/**
 * Placeholder bootstrap for the future dungeon v2 module wiring.
 */
public class DungeonRegistry {

    private static PartyManager partyManager;
    private static InstanceManager instanceManager;
    private static ReviveManager reviveManager;

    public static void onEnable(Plugin plugin, ItemRegistry itemRegistry) {
        Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
                .setPrettyPrinting()
                .create();
        /*Scheduler*/
        TaskScheduler.init(plugin);
        TaskScheduler taskScheduler = TaskScheduler.getInstance();

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
        ParticleManager dungeonParticle = new ParticleManager();
        PresentationManager presentationManager = new PresentationManager(soundManager, effectManager, screenMessManager, dungeonParticle);

        /*Presentation Implement*/
        DungeonPresenter dungeonPresenter = new DungeonPresenter(presentationManager);

        /*Repository*/
        ISchematicRepository schematicRepository = new SchematicRepository(plugin);
        ISchemetaRepository schemetaRepository = new SchemetaRepository(plugin, gson, logger);
        IRegionRepository regionRepository = new RegionRepository(plugin, gson, logger);
        IRoomRepository roomRepository = new RoomRepository(plugin, gson, logger);
        IDungeonRepository dungeonRepository = new DungeonRepository(plugin, gson);
        ISpawnerRepository spawnerRepository = new SpawnerRepository(plugin, gson, logger);
        IPartyRepository partyRepository = new PartyRepository(plugin, gson, logger);
        IInstanceRepository instanceRepository = new InstanceRepository(plugin, gson, logger);
        ILootTableRepository lootTableRepository = new LootTableRepository(plugin, gson);

        /*Manager*/
        SchemetaManager schemetaManager = new SchemetaManager(schemetaRepository, schematicRepository);
        RegionManager regionManager = new RegionManager(regionRepository, logger);
        partyManager = new PartyManager(partyRepository, logger);
        RoomManager roomManager = new RoomManager(roomRepository, logger);
        DungeonManager dungeonManager = new DungeonManager(dungeonRepository, logger);
        SpawnerManager spawnerManager = new SpawnerManager(spawnerRepository, logger);
        LootTableManager lootTableManager = new LootTableManager(lootTableRepository);
        RevivePointPresenter revivePointPresenter = new RevivePointPresenter();
        reviveManager = new ReviveManager(taskScheduler, revivePointPresenter);

        SpawnerInstanceManager spawnerInstanceManager = new SpawnerInstanceManager();
        instanceManager = new InstanceManager(instanceRepository, scoreBoardManager, logger);

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
        IReviveService reviveService = new ReviveService(reviveManager, partyService, instanceManager, dungeonPresenter);
        ObjectiveDispatchService objectiveDispatchService = new ObjectiveDispatchService(partyService, instanceManager);
        SpectatorBoundaryService spectatorBoundaryService = new SpectatorBoundaryService(partyService, instanceManager);
        RoomRuntimeService roomRuntimeService = new RoomRuntimeService(
                roomManager, instanceManager, partyService, regionService, spawnerInstanceManager
        );
        RoomCompletionService roomCompletionService = new RoomCompletionService(
                partyService, roomService, reviveService, dungeonPresenter, roomRuntimeService
        );
        roomRuntimeService.setCompletionService(roomCompletionService);
        DungeonLifecycleService dungeonLifecycleService = new DungeonLifecycleService(
                instanceService,
                instanceManager,
                partyService,
                regionService,
                scoreBoardManager,
                dungeonManager,
                spawnerInstanceManager,
                dungeonPresenter,
                roomRuntimeService
        );
        PlayerDeathService playerDeathService = new PlayerDeathService(partyService, instanceManager, reviveService, dungeonLifecycleService,dungeonPresenter);
        PlayerSessionService playerSessionService = new PlayerSessionService(partyService, instanceManager, scoreBoardManager, dungeonManager, playerDeathService);
        RoomNavigationService roomNavigationService = new RoomNavigationService(
                partyService,
                instanceManager,
                roomManager,
                schematicService,
                dungeonService,
                roomService,
                roomRuntimeService,
                dungeonPresenter,
                taskScheduler
        );
        TreasureService treasureService = new TreasureService(
                partyService,
                instanceManager,
                regionService,
                roomManager,
                schematicService,
                taskScheduler
        );
        DungeonTimerService dungeonTimerService = new DungeonTimerService(
                instanceManager, partyService, roomRuntimeService, dungeonLifecycleService
        );
        PlayerTeleportService playerTeleportService = new PlayerTeleportService(partyService, instanceManager);

        /*Task*/
        PartyInviteTask inviteTask = new PartyInviteTask(plugin, partyService);

        /*Controller*/
        PartyController partyController = new PartyController(partyService, inviteTask);
        DungeonFlowController flowController = new DungeonFlowController(
                dungeonLifecycleService, roomNavigationService, treasureService, dungeonTimerService
        );
        SchemetaController schemetaController = new SchemetaController(schemetaService, schematicService);
        SpawnerEventController spawnerController = new SpawnerEventController(spawnerService, spawnerManager, spawnerInstanceManager, taskScheduler, logger);
        PlayerActionController actionController = new PlayerActionController(
            objectiveDispatchService, playerDeathService, spectatorBoundaryService, playerSessionService, playerTeleportService
        );
        DungeonTreasureController treasureController = new DungeonTreasureController(rewardService);
        BossRoomController bossRoomController = new BossRoomController(instanceManager, partyService, roomManager);

        /*Command*/
        new TemplateGenCommand(
                lootService,
                spawnerManager,
                dungeonManager,
                roomManager,
                lootTableManager,
                schemetaManager
        ).register();
        new SchemetaCommand(schemetaController, schemetaManager).register();
        new PartyCommand(partyController).register();
        new DungeonCommand(flowController, dungeonManager).register();

        /*Listener*/
        PluginManager pluginManager = Bukkit.getPluginManager();

        pluginManager.registerEvents(
                new DoorInteractListener(flowController, partyService),
                RogueSmpCore.getInstance()
        );

        pluginManager.registerEvents(
                new SpawnerEventListener(spawnerController, bossRoomController),
                RogueSmpCore.getInstance()
        );

        pluginManager.registerEvents(
                new DungeonListener(actionController, taskScheduler),
                RogueSmpCore.getInstance()
        );

        pluginManager.registerEvents(
                new LootTableListener(treasureController),
                RogueSmpCore.getInstance()
        );

        /*Runtime tick: refresh scoreboard + monitor dungeon timeout (1s)*/
        taskScheduler.runTimer(20L, 20L, () -> {
            scoreBoardManager.tickUpdate();
            flowController.handleDungeonTimerTick();
        });


    }

    public static void onDisable() {
        if (reviveManager != null) {
            reviveManager.shutdown();
        }
        if (partyManager != null) {
            partyManager.saveAll();
        }
        if (instanceManager != null) {
            instanceManager.saveAll();
        }
    }

}
