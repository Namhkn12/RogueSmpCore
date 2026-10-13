package com.roguesmp.quest.holder;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

public interface QuestHolder {

    UUID id();

    Collection<Player> audience();
}
