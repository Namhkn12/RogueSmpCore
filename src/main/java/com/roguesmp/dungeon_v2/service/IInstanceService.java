package com.roguesmp.dungeon_v2.service;

import com.roguesmp.dungeon_v2.data.definition.Dungeon;
import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import com.roguesmp.dungeon_v2.data.runtime.Party;
import com.roguesmp.dungeon_v2.data.runtime.session.DungeonPlayer;
import com.roguesmp.dungeon_v2.data.runtime.session.PlayerStatus;
import org.bukkit.entity.Player;

public interface IInstanceService {
    DungeonInstance createDungeonInstance(Dungeon dungeon, Party party);
    void startDungeonInstance(DungeonInstance instance);
    void removeDungeonInstance(DungeonInstance instance);
    void saveDungeonInstance(DungeonInstance instance);
    DungeonPlayer getPlayersInInstance(DungeonInstance instance);
    PlayerStatus getPlayerStatus(Player player);
}
