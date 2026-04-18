package com.roguesmp.dungeon_v2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguesmp.RogueSmpCore;
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
import com.roguesmp.dungeon_v2.presentation.*;
import com.roguesmp.dungeon_v2.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon_v2.presentation.presenter.RevivePointPresenter;
import com.roguesmp.dungeon_v2.repository.*;
import com.roguesmp.dungeon_v2.repository.impl.*;
import com.roguesmp.dungeon_v2.service.*;
import com.roguesmp.dungeon_v2.service.impl.*;
import com.roguesmp.dungeon_v2.task.PartyInviteTask;
import com.roguesmp.dungeon_v2.task.TaskScheduler;
import com.roguesmp.dungeon_v2.utils.Log4Craft_;
import com.roguesmp.dungeon_v2.utils.adapter.UUIDTypeAdapter;
import com.roguesmp.registry.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

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
                taskScheduler,
                reviveService
        );
        SchemetaController schemetaController = new SchemetaController(schemetaService, schematicService);
        SpawnerEventController spawnerController = new SpawnerEventController(spawnerService, spawnerManager, spawnerInstanceManager, taskScheduler, logger);
        PlayerActionController actionController = new PlayerActionController(
                instanceService,
                instanceManager,
                partyService,
                scoreBoardManager,
                dungeonManager,
                taskScheduler,
                dungeonPresenter,
                reviveManager,
                reviveService
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
        Bukkit.getPluginManager().registerEvents(
                new DoorInteractListener(flowController, instanceManager),
                RogueSmpCore.getInstance()
        );

        Bukkit.getPluginManager().registerEvents(
                new SpawnerEventListener(spawnerController, bossRoomController),
                RogueSmpCore.getInstance()
        );

        Bukkit.getPluginManager().registerEvents(
                new DungeonListener(actionController, taskScheduler),
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
