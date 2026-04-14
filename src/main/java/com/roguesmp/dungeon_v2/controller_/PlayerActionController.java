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
import com.roguesmp.dungeon_v2.task.TaskScheduler;
import com.roguesmp.dungeon_v2.utils.DungeonEcho;
import com.roguesmp.dungeon_v2.utils.PdcUtil;
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

    private final IInstanceService instanceService;
    private final InstanceManager instanceManager;
    private final IPartyService partyService;
    private final ScoreBoardManager scoreBoardManager;
    private final DungeonManager dungeonManager;
    private final TaskScheduler taskScheduler;
    private final DungeonPresenter presenter;
    private final ReviveManager reviveManager;
    private final IReviveService reviveService;

    public PlayerActionController(IInstanceService instanceService, InstanceManager instanceManager, IPartyService partyService, ScoreBoardManager scoreBoardManager, DungeonManager dungeonManager, TaskScheduler taskScheduler, DungeonPresenter presenter, ReviveManager reviveManager, IReviveService reviveService) {
        this.instanceService = instanceService;
        this.instanceManager = instanceManager;
        this.partyService = partyService;
        this.scoreBoardManager = scoreBoardManager;
        this.dungeonManager = dungeonManager;
        this.taskScheduler = taskScheduler;
        this.presenter = presenter;
        this.reviveManager = reviveManager;
        this.reviveService = reviveService;
    }

    public void handlePlayerKillMob(LivingEntity entity, Player player){
        ActionContext context = resolveContext(player, entity == null ? null : entity.getLocation().toVector());
        if (context == null || entity == null) return;

        String mobId = PdcUtil.getOrDefault(
                entity,
                Keys.MOB_ID,
                PersistentDataType.STRING,
                normalizeEntityType(entity.getType())
        );

        dispatchObjectives(context.room(), objective -> {
            if (objective instanceof IEntityKillAware aware) {
                aware.onEntityKilled(mobId);
            }
        });
    }

    public void handlePlayerBreakSpawner(Block spawner, Player player){
        ActionContext context = resolveContext(player, spawner == null ? null : spawner.getLocation().toVector());
        if (context == null || spawner == null) return;

        dispatchObjectives(context.room(), objective -> {
            if (objective instanceof IBlockBreakAware aware) {
                aware.onBlockBreak(spawner.getType());
            }
        });
    }

    public void handlePlayerCollectItem(ItemStack item, Player player){
        ActionContext context = resolveContext(player, player == null ? null : player.getLocation().toVector());
        if (context == null || item == null || item.getAmount() <= 0) return;

        String itemId = PdcUtil.getOrDefault(
                item,
                Keys.ITEM_ID,
                PersistentDataType.STRING,
                item.getType().name().toLowerCase(Locale.ROOT)
        );

        dispatchObjectives(context.room(), objective -> {
            if (objective instanceof IItemCollectAware aware) {
                aware.onItemCollected(itemId, item.getAmount());
            }
        });
    }

    public void handlePlayerDead(Player player){
        Party party = partyService.getPartyByPlayer(player);
        if(party == null) return;
        String iid = party.getInstanceId();
        if(iid == null) return;
        if(iid.isBlank()) return;
        DungeonInstance instance = instanceManager.get(iid);
        if (instance == null) return;

        PlayerStatus playerStatus = instance.getDungeonPlayers().getPlayers().get(player.getUniqueId().toString());
        if (playerStatus == null) return;
        playerStatus.setCheckPoint(SerializableLocation.from(player.getLocation()));
        /*Set player status is DEAD*/
        playerStatus.setStatus(PlayerStatus.Status.DEAD);
        playerStatus.upDead();
        player.setGameMode(GameMode.SPECTATOR);
        /*Revive register player when dead*/
        reviveService.registerPlayerDead(player);
        DungeonEcho.error(player, "You die! Wait for your teammates complete the room, you will be revived");
        presenter.onPlayerDead(player, player.getLocation());
    }

    public void handlePlayerMoveInDeadMode(Player player){
        Party party = partyService.getPartyByPlayer(player);
        if(party == null) return;
        String iid = party.getInstanceId();
        if(iid == null) return;
        if(iid.isBlank()) return;
        DungeonInstance instance = instanceManager.get(iid);
        if (instance == null) return;

        BoundingBox bounder = instance.getProgress().getCurrentRoom().getBounds().toBukkit();

        double shrink = 3;
        double minX = bounder.getMinX() + shrink;
        double maxX = bounder.getMaxX() - shrink;
        double minY = bounder.getMinY() + shrink;
        double maxY = bounder.getMaxY() - shrink;
        double minZ = bounder.getMinZ() + shrink;
        double maxZ = bounder.getMaxZ() - shrink;

        Location loc = player.getLocation();

        double clampedX = Math.max(minX, Math.min(maxX, loc.getX()));
        double clampedZ = Math.max(minZ, Math.min(maxZ, loc.getZ()));
        double clampedY = Math.max(minY, Math.min(maxY, loc.getY()));

        if(clampedX != loc.getX() || clampedZ != loc.getZ() || clampedY != loc.getY()){
            loc.setX(clampedX);
            loc.setY(clampedY);
            loc.setZ(clampedZ);
            player.teleport(loc);
        }
    }

    public void handlePlayerReconnect(Player player){
        Party party = partyService.getPartyByPlayer(player);
        /*Party null -> No dungeon*/
        if(party == null) return;
        String iid = party.getInstanceId();
        if(iid == null) return;
        if(iid.isBlank()) return;
        DungeonInstance instance = instanceManager.get(iid);
        if(instance == null){
            party.setInstanceId("");
            return;
        }
        PlayerStatus playerStatus = instance.getDungeonPlayers().getPlayers().get(player.getUniqueId().toString());
        if (playerStatus == null) return;

        PlayerStatus.Status status = playerStatus.getStatus();

        /*Out state*/
        if (status == PlayerStatus.Status.OUT) return;

        scoreBoardManager.createBoard(player, instance, dungeonManager.get(instance.getSession().getDungeonId()));

        if (status == PlayerStatus.Status.DISCONNECT) {
            playerStatus.setStatus(PlayerStatus.Status.PLAYING);
            player.setGameMode(GameMode.SURVIVAL);
            return;
        }

        if (status == PlayerStatus.Status.DEAD_DISCONNECT) {
            playerStatus.setStatus(PlayerStatus.Status.DEAD);
            reviveService.registerPlayerDead(player);
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    public void handlePlayerDisconnect(Player player){
        Party party = partyService.getPartyByPlayer(player);
        /*Party null -> No dungeon*/
        if(party == null) return;
        String iid = party.getInstanceId();
        if(iid == null) return;
        if(iid.isBlank()) return;
        DungeonInstance instance = instanceManager.get(iid);
        if (instance == null) return;
        /*TODO*/
        PlayerStatus playerStatus = instance.getDungeonPlayers().getPlayers().get(player.getUniqueId().toString());
        if (playerStatus == null) return;
        if(playerStatus.getStatus() == PlayerStatus.Status.DEAD){
            playerStatus.setStatus(PlayerStatus.Status.DEAD_DISCONNECT);
            player.setGameMode(GameMode.SURVIVAL);
            return;
        }
        if(playerStatus.getStatus() == PlayerStatus.Status.PLAYING){
            playerStatus.setStatus(PlayerStatus.Status.DISCONNECT);
        }
    }

    private ActionContext resolveContext(Player player, org.bukkit.util.Vector actionPosition) {
        if (player == null) return null;

        Party party = partyService.getPartyByPlayer(player);
        if (party == null || party.getInstanceId() == null) return null;
        if(party.getInstanceId().isEmpty()) return null;
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        if (instance == null || instance.getProgress() == null) return null;

        RoomInstance room = instance.getProgress().getCurrentRoom();
        if (room == null || room.isCompleted()) return null;

        List<IObjective> objectives = room.getActiveObjectives();
        if (objectives == null || objectives.isEmpty()) return null;

        if (actionPosition != null && !room.isInside(actionPosition.getX(), actionPosition.getY(), actionPosition.getZ())) {
            return null;
        }

        return new ActionContext(party, instance, room);
    }

    private void dispatchObjectives(RoomInstance room, Consumer<IObjective> action) {
        if (room == null || room.getActiveObjectives() == null) return;

        room.getActiveObjectives().forEach(objective -> {
            if (objective instanceof BaseObjective baseObjective && baseObjective.isCompleted()) {
                return;
            }
            action.accept(objective);
        });
    }

    private String normalizeEntityType(EntityType type) {
        return type == null ? "" : type.name().toLowerCase(Locale.ROOT);
    }

    private record ActionContext(Party party, DungeonInstance instance, RoomInstance room) {}
}
