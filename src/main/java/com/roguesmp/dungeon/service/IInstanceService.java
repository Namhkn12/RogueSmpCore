package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.data.definition.Dungeon;
import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import com.roguesmp.dungeon.data.runtime.Party;
import com.roguesmp.dungeon.data.runtime.session.DungeonPlayer;
import com.roguesmp.dungeon.data.runtime.session.PlayerStatus;
import org.bukkit.entity.Player;

public interface IInstanceService {
    DungeonInstance createDungeonInstance(Dungeon dungeon, Party party);
    void startDungeonInstance(DungeonInstance instance);
    void removeDungeonInstance(DungeonInstance instance);
    void saveDungeonInstance(DungeonInstance instance);
    DungeonPlayer getPlayersInInstance(DungeonInstance instance);
    PlayerStatus getPlayerStatus(Player player);
}
