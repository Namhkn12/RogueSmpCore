package com.roguesmp.dungeon.actor.listener;

import com.roguesmp.dungeon.context.DungeonContext;
import com.roguesmp.dungeon.controller.DungeonController;
import com.roguesmp.dungeon.controller.PartyController;
import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.instance.RoomInstance;
import com.roguesmp.dungeon.objective_.ievento.IBlockBreakObjective;
import com.roguesmp.dungeon.objective_.ievento.IEntityDeadObjective;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class ObjectiveListener implements Listener {

    private final DungeonController dungeonController;
    private final PartyController partyController;

    public ObjectiveListener(DungeonController dungeonController, PartyController partyController) {
        this.dungeonController = dungeonController;
        this.partyController = partyController;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSpawnerBreak(BlockBreakEvent e) {
        //check world
        if (e.isCancelled()) return;
        if (e.getBlock().getType() != Material.SPAWNER) return;

        DungeonContext ctx = resolve(e.getPlayer(), e.getBlock().getLocation());
        if (ctx == null) return;

        ctx.room().getObjective().forEach(obj -> {
            if (obj instanceof IBlockBreakObjective eventObj) {
                eventObj.onBlockBreak(e);
            }
        });
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent e){
        //check world
        DungeonContext ctx = resolve(e.getEntity().getKiller(), e.getEntity().getLocation());
        if (ctx == null) return;

        ctx.room().getObjective().forEach(obj -> {
            if(obj instanceof IEntityDeadObjective eventObj) {
                eventObj.onEntityDead(e);
            }
        });
    }

    private DungeonContext resolve(Player player, Location loc) {
        Party party = partyController.getPartyByPlayer(player).getData();
        DungeonInstance dungeon = dungeonController.getInstanceByParty(party.getPartyId()).getData();
        if (dungeon == null) return null;
        RoomInstance room = dungeon.getActiveRoom();
        if (room == null) return null;
        if (!room.getRoomBounds().contains(loc.toVector())) return null;
        return new DungeonContext(party, dungeon, room);
    }
}