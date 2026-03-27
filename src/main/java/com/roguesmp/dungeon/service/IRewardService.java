package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.instance.DungeonInstance;

public interface IRewardService {
    void sendToRewardRoom(Party party, DungeonInstance instance);
}
