package com.roguesmp.dungeon.exception;

import com.roguesmp.dungeon.utils.DungeonEcho;
import com.roguesmp.dungeon.utils.Log4Craft;
import org.bukkit.entity.Player;

public class GlobalException {

    public static void handle(BaseException e) {
        Log4Craft.fire("Dungeon exception: " + e.getMessage(), e);
    }

    public static void handleUnexpected(String context, Throwable e) {
        Log4Craft.fire("Unexpected error at [" + context + "]: " + e.getMessage(), e);
    }

    public static void notify(Player player, BaseException e) {
        if (player == null) return;
        DungeonEcho.error(player, e.getUserMessage());
    }

    public static void handleAndNotify(BaseException e, Player player) {
        handle(e);
        notify(player, e);
    }

    public static void handleAndNotifyUnexpected(String context, Throwable e, Player player, String userMessage) {
        handleUnexpected(context, e);
        if (player != null) {
            DungeonEcho.error(player, userMessage);
        }
    }
}
