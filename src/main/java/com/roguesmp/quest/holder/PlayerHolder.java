package com.roguesmp.quest.holder;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record PlayerHolder(UUID id) implements QuestHolder {

    @Override
    public Collection<Player> audience() {
        Player player = Bukkit.getPlayer(id);
        return player == null ? List.of() : List.of(player);
    }
}
