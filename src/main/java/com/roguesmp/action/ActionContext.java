package com.roguesmp.action;

import com.roguesmp.item.SmpItem;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.Nullable;

public record ActionContext(Player player, boolean rightClick, @Nullable SmpItem item, @Nullable Entity target,
                            @Nullable Location location, @Nullable Cancellable event) {

    public static ActionContext ofEntity(Player player, boolean rightClick, @Nullable SmpItem item, Entity target, Cancellable event) {
        return new ActionContext(player, rightClick, item, target, target.getLocation(), event);
    }
}
