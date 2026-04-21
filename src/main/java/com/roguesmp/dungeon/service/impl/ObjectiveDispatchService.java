package com.roguesmp.dungeon.service.impl;

import com.roguesmp.constant.Keys;
import com.roguesmp.dungeon.data.definition.objective.BaseObjective;
import com.roguesmp.dungeon.data.definition.objective.IObjective;
import com.roguesmp.dungeon.data.definition.objective.event.IBlockBreakAware;
import com.roguesmp.dungeon.data.definition.objective.event.IEntityKillAware;
import com.roguesmp.dungeon.data.definition.objective.event.IItemCollectAware;
import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon.data.runtime.Party;
import com.roguesmp.dungeon.data.runtime.RoomInstance;
import com.roguesmp.dungeon.manager.InstanceManager;
import com.roguesmp.dungeon.service.IPartyService;
import com.roguesmp.dungeon.utils.PdcUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ObjectiveDispatchService {

    private final IPartyService partyService;
    private final InstanceManager instanceManager;

    public ObjectiveDispatchService(IPartyService partyService, InstanceManager instanceManager) {
        this.partyService = partyService;
        this.instanceManager = instanceManager;
    }

    public void handlePlayerKillMob(LivingEntity entity, Player player) {
        ActionContext ctx = resolveContext(player, entity == null ? null : entity.getLocation().toVector());
        if (ctx == null || entity == null) return;

        String mobId = PdcUtil.getOrDefault(
                entity, Keys.MOB_ID, PersistentDataType.STRING,
                normalizeEntityType(entity.getType())
        );

        dispatchObjectives(ctx.room(), objective -> {
            if (objective instanceof IEntityKillAware aware) aware.onEntityKilled(mobId);
        });
    }

    public void handlePlayerBreakSpawner(Block spawner, Player player) {
        ActionContext ctx = resolveContext(player, spawner == null ? null : spawner.getLocation().toVector());
        if (ctx == null || spawner == null) return;

        dispatchObjectives(ctx.room(), objective -> {
            if (objective instanceof IBlockBreakAware aware) aware.onBlockBreak(spawner.getType());
        });
    }

    public void handlePlayerCollectItem(ItemStack item, Player player) {
        ActionContext ctx = resolveContext(player, player == null ? null : player.getLocation().toVector());
        if (ctx == null || item == null || item.getAmount() <= 0) return;

        String itemId = PdcUtil.getOrDefault(
                item, Keys.ITEM_ID, PersistentDataType.STRING,
                item.getType().name().toLowerCase(Locale.ROOT)
        );

        dispatchObjectives(ctx.room(), objective -> {
            if (objective instanceof IItemCollectAware aware) aware.onItemCollected(itemId, item.getAmount());
        });
    }

    private ActionContext resolveContext(Player player, org.bukkit.util.Vector actionPosition) {
        if (player == null) return null;
        Party party = partyService.getPartyByPlayer(player);
        if (party == null || party.getInstanceId() == null || party.getInstanceId().isEmpty()) return null;
        DungeonInstance instance = instanceManager.get(party.getInstanceId());
        if (instance == null || instance.getProgress() == null) return null;
        RoomInstance room = instance.getProgress().getCurrentRoom();
        if (room == null || room.isCompleted()) return null;
        List<IObjective> objectives = room.getActiveObjectives();
        if (objectives == null || objectives.isEmpty()) return null;
        if (actionPosition != null && !room.isInside(actionPosition.getX(), actionPosition.getY(), actionPosition.getZ())) return null;
        return new ActionContext(party, instance, room);
    }

    private void dispatchObjectives(RoomInstance room, Consumer<IObjective> action) {
        if (room == null || room.getActiveObjectives() == null) return;
        room.getActiveObjectives().forEach(objective -> {
            if (objective instanceof BaseObjective base && base.isCompleted()) return;
            action.accept(objective);
        });
    }

    private String normalizeEntityType(EntityType type) {
        return type == null ? "" : type.name().toLowerCase(Locale.ROOT);
    }

    private record ActionContext(Party party, DungeonInstance instance, RoomInstance room) {}
}