package com.roguesmp.dungeon;

import com.roguesmp.utils.Utils;
import com.roguesmp.world.PolarTemplateManager;
import com.roguesmp.world.DungeonWorldManager;
import com.roguesmp.dungeon_party.PartyManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.actor.command.DungeonCommand;
import com.roguesmp.dungeon.actor.command.SchemetaCommand;
import com.roguesmp.dungeon.actor.command.TemplateGenCommand;
import com.roguesmp.dungeon.actor.listener.DoorInteractListener;
import com.roguesmp.dungeon.actor.listener.DungeonListener;
import com.roguesmp.dungeon.actor.listener.LootTableListener;
import com.roguesmp.dungeon_spawner.listener.SpawnerEventListener;
import com.roguesmp.dungeon_spawner.controller.SpawnerEventController;
import com.roguesmp.dungeon_spawner.manager.SpawnerInstanceManager;
import com.roguesmp.dungeon_spawner.manager.SpawnerManager;
import com.roguesmp.dungeon_spawner.repository.SpawnerRepository;
import com.roguesmp.dungeon.controller.*;
import com.roguesmp.dungeon.presentation.presenter.ChestOpenAnimation;
import com.roguesmp.dungeon.manager.*;
import com.roguesmp.dungeon.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon.presentation.presenter.RevivePointPresenter;
import com.roguesmp.dungeon.repository.*;
import com.roguesmp.dungeon.service.*;
import com.roguesmp.loot.manager.LootTableManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.UUID;

/**
 * Placeholder bootstrap for the future dungeon v2 module wiring.
 */
public class DungeonRegistry {

    private static InstanceManager instanceManager;
    private static ReviveManager reviveManager;
    private static InstanceWorldService worldService;

    public static void onEnable(Plugin plugin) {
        Gson gson = new GsonBuilder().setPrettyPrinting()
                .create();
        DungeonPresenter dungeonPresenter = new DungeonPresenter();

        /*Repository*/
        SchematicRepository schematicRepository = new SchematicRepository(plugin);
        SchemetaRepository schemetaRepository = new SchemetaRepository(plugin, gson);
        RoomRepository roomRepository = new RoomRepository(plugin, gson);
        DungeonRepository dungeonRepository = new DungeonRepository(plugin, gson);
        SpawnerRepository spawnerRepository = new SpawnerRepository(plugin, gson);
        InstanceRepository instanceRepository = new InstanceRepository(plugin, gson);

        /*Manager*/
        SchemetaManager schemetaManager = new SchemetaManager(schemetaRepository, schematicRepository);
        RoomManager roomManager = new RoomManager(roomRepository);
        DungeonManager dungeonManager = new DungeonManager(dungeonRepository);
        SpawnerManager spawnerManager = new SpawnerManager(spawnerRepository);
        LootTableManager lootTableManager = new LootTableManager();
        RevivePointPresenter revivePointPresenter = new RevivePointPresenter();
        reviveManager = new ReviveManager(plugin, revivePointPresenter);

        SpawnerInstanceManager spawnerInstanceManager = new SpawnerInstanceManager();
        instanceManager = new InstanceManager(instanceRepository);
        worldService = new InstanceWorldService(
                new DungeonWorldManager((RogueSmpCore) plugin, PolarTemplateManager.getInstance(), "spawner_dungeon_worlds", "spawner_dungeon"),
                PolarTemplateManager.getInstance().worlds().key("instance"));

        /*Service*/
        SchematicService schematicService = new SchematicService(schemetaManager);
        SchemetaService schemetaService = new SchemetaService(schemetaManager);
        RoomService roomService = new RoomService(roomManager);
        PartyManager partyManager = PartyManager.getInstance();
        DungeonService dungeonService = new DungeonService(roomManager, dungeonManager);
        InstanceService instanceService = new InstanceService(instanceManager,
                dungeonManager, roomManager, worldService, dungeonService, partyManager, roomService, schematicService);
        DungeonRewardService rewardService = new DungeonRewardService(
                partyManager,
                instanceManager,
                dungeonManager,
                new ChestOpenAnimation()
                );
        ReviveService reviveService = new ReviveService(reviveManager, partyManager, instanceManager, dungeonPresenter);
        ObjectiveDispatchService objectiveDispatchService = new ObjectiveDispatchService(partyManager, instanceManager);
        SpectatorBoundaryService spectatorBoundaryService = new SpectatorBoundaryService(partyManager, instanceManager);
        RoomRuntimeService roomRuntimeService = new RoomRuntimeService(
                roomManager, instanceManager, partyManager, worldService, spawnerInstanceManager
        );
        RoomCompletionService roomCompletionService = new RoomCompletionService(
                partyManager, roomService, reviveService, dungeonPresenter, roomRuntimeService
        );
        roomRuntimeService.setCompletionService(roomCompletionService);
        DungeonLifecycleService dungeonLifecycleService = new DungeonLifecycleService(
                instanceService,
                instanceManager,
                partyManager,
                worldService,
                dungeonManager,
                spawnerInstanceManager,
                dungeonPresenter,
                roomRuntimeService,
                reviveService
        );
        PlayerDeathService playerDeathService = new PlayerDeathService(partyManager, instanceManager, reviveService, dungeonLifecycleService,dungeonPresenter);
        PlayerSessionService playerSessionService = new PlayerSessionService(partyManager, instanceManager, dungeonManager, playerDeathService, worldService);
        RoomNavigationService roomNavigationService = new RoomNavigationService(
                partyManager,
                instanceManager,
                roomManager,
                schematicService,
                dungeonService,
                roomService,
                roomRuntimeService,
                dungeonPresenter,
                plugin
        );
        TreasureService treasureService = new TreasureService(
                partyManager,
                instanceManager,
                worldService,
                roomManager,
                schematicService,
                plugin
        );
        DungeonTimerService dungeonTimerService = new DungeonTimerService(
                instanceManager, partyManager, roomRuntimeService, dungeonLifecycleService
        );
        PlayerTeleportService playerTeleportService = new PlayerTeleportService(partyManager, instanceManager);

        /*Task*/

        /*Controller*/
        DungeonFlowController flowController = new DungeonFlowController(
                dungeonLifecycleService, roomNavigationService, treasureService, dungeonTimerService
        );
        SchemetaController schemetaController = new SchemetaController(schemetaService, schematicService);
        SpawnerEventController spawnerController = new SpawnerEventController(spawnerManager, spawnerInstanceManager, plugin);
        PlayerActionController actionController = new PlayerActionController(
            objectiveDispatchService, playerDeathService, spectatorBoundaryService, playerSessionService, playerTeleportService
        );
        DungeonTreasureController treasureController = new DungeonTreasureController(rewardService);
        BossRoomController bossRoomController = new BossRoomController(instanceManager, partyManager, roomManager);

        /*Command*/
        new TemplateGenCommand(
                spawnerManager,
                dungeonManager,
                roomManager,
                lootTableManager,
                schemetaManager
        ).register();
        new SchemetaCommand(schemetaController, schemetaManager).register();
        new DungeonCommand(flowController, dungeonManager).register();

        /*Listener*/
        PluginManager pluginManager = Bukkit.getPluginManager();

        pluginManager.registerEvents(
                new DoorInteractListener(flowController, partyManager),
                RogueSmpCore.getInstance()
        );

        pluginManager.registerEvents(
                new SpawnerEventListener(spawnerController, bossRoomController),
                RogueSmpCore.getInstance()
        );

        pluginManager.registerEvents(
                new DungeonListener(actionController, plugin),
                RogueSmpCore.getInstance()
        );

        pluginManager.registerEvents(
                new LootTableListener(treasureController),
                RogueSmpCore.getInstance()
        );

        /*Runtime tick: monitor dungeon timeout (1s)*/
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            flowController.handleDungeonTimerTick();
        }, 20L, 20L);

        RogueSmpCore.LOGGER.info("Deleted {} orphaned dungeon world(s)", worldService.deleteOrphans(instanceManager.getAll()));
        instanceManager.getAll().forEach(instance -> worldService.load(instance).exceptionally(error -> {
            RogueSmpCore.LOGGER.warn("Dropping dungeon instance {}, its world could not be loaded", instance.getSession().getSessionId(), error);
            Utils.runLater(() -> instanceService.removeDungeonInstance(instance));
            return null;
        }));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            /*Save dungeon instance to file*/
            instanceManager.saveAll();
        }, 6000L);


    }

    public static void onDisable() {
        if (reviveManager != null) {
            reviveManager.shutdown();
        }
        if (instanceManager != null) {
            instanceManager.saveAll();
        }
        if (worldService != null) {
            worldService.saveAllOnStop();
        }
    }

}
