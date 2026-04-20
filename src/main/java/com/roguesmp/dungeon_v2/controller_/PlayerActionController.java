package com.roguesmp.dungeon_v2.controller_;

import com.roguesmp.constant.Keys;
import com.roguesmp.dungeon_v2.data.definition.objective.BaseObjective;
import com.roguesmp.dungeon_v2.data.definition.objective.IObjective;
import com.roguesmp.dungeon_v2.data.definition.objective.event.IBlockBreakAware;
import com.roguesmp.dungeon_v2.data.definition.objective.event.IEntityKillAware;
import com.roguesmp.dungeon_v2.data.definition.objective.event.IItemCollectAware;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.Party;
import com.roguesmp.dungeon_v2.data.runtime.RoomInstance;
import com.roguesmp.dungeon_v2.data.runtime.session.PlayerStatus;
import com.roguesmp.dungeon_v2.helper.SerializableLocation;
import com.roguesmp.dungeon_v2.manager.DungeonManager;
import com.roguesmp.dungeon_v2.manager.InstanceManager;
import com.roguesmp.dungeon_v2.manager.ReviveManager;
import com.roguesmp.dungeon_v2.manager.ScoreBoardManager;
import com.roguesmp.dungeon_v2.presentation.presenter.DungeonPresenter;
import com.roguesmp.dungeon_v2.service.IInstanceService;
import com.roguesmp.dungeon_v2.service.IPartyService;
import com.roguesmp.dungeon_v2.service.IReviveService;
import com.roguesmp.dungeon_v2.service.impl.ObjectiveDispatchService;
import com.roguesmp.dungeon_v2.service.impl.PlayerDeathService;
import com.roguesmp.dungeon_v2.service.impl.PlayerSessionService;
import com.roguesmp.dungeon_v2.service.impl.SpectatorBoundaryService;
import com.roguesmp.dungeon_v2.task.TaskScheduler;
import com.roguesmp.dungeon_v2.utils.DungeonEcho;
import com.roguesmp.dungeon_v2.utils.PdcUtil;
import com.roguesmp.dungeon_v2.utils.Teleporter;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class PlayerActionController {

    private final ObjectiveDispatchService objectiveDispatchService;
    private final PlayerDeathService playerDeathService;
    private final SpectatorBoundaryService spectatorBoundaryService;
    private final PlayerSessionService playerSessionService;

    public PlayerActionController(ObjectiveDispatchService objectiveDispatchService,
                                  PlayerDeathService playerDeathService,
                                  SpectatorBoundaryService spectatorBoundaryService,
                                  PlayerSessionService playerSessionService) {
        this.objectiveDispatchService = objectiveDispatchService;
        this.playerDeathService = playerDeathService;
        this.spectatorBoundaryService = spectatorBoundaryService;
        this.playerSessionService = playerSessionService;
    }

    public void handlePlayerKillMob(LivingEntity entity, Player player) {
        objectiveDispatchService.handlePlayerKillMob(entity, player);
    }

    public void handlePlayerBreakSpawner(Block spawner, Player player) {
        objectiveDispatchService.handlePlayerBreakSpawner(spawner, player);
    }

    public void handlePlayerCollectItem(ItemStack item, Player player) {
        objectiveDispatchService.handlePlayerCollectItem(item, player);
    }

    public void handlePlayerDead(Player player) {
        playerDeathService.handlePlayerDead(player);
    }

    public void handlePlayerMoveInDeadMode(Player player) {
        spectatorBoundaryService.handlePlayerMoveInDeadMode(player);
    }

    public void handlePlayerReconnect(Player player) {
        playerSessionService.handlePlayerReconnect(player);
    }

    public void handlePlayerDisconnect(Player player) {
        playerSessionService.handlePlayerDisconnect(player);
    }
}
