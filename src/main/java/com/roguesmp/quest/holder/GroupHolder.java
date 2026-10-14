package com.roguesmp.quest.holder;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * @param owner the system that created the group (e.g. "dungeon"), used to sweep groups whose owner no longer knows them
 */
public record GroupHolder(UUID id, String owner, Supplier<Set<UUID>> members) implements QuestHolder {

    public static GroupHolder unbound(UUID id, String owner) {
        return new GroupHolder(id, owner, Set::of);
    }

    public boolean canProgress(Player player) {
        return members.get().contains(player.getUniqueId());
    }

    @Override
    public Collection<Player> audience() {
        return members.get().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList();
    }
}
