package com.roguesmp.dungeon.exception;

import com.roguesmp.dungeon.utils.DungeonEcho;
import com.roguesmp.dungeon.utils.Log4Craft;
import org.bukkit.entity.Player;

public class GlobalException {

    public static void handle(BaseException e) {
        Log4Craft.fire("WDA - Exception: " + e.getMessage(), e);
        if (e.getCause() != null) {
            Log4Craft.error("Caused by: " + e.getCause().getMessage());
        }
    }

    public static void handleUnexpected(String context, Exception e) {
        Log4Craft.error("Unexpected error at [" + context + "]: " + e.getMessage());
        e.printStackTrace();
    }

    public static void handleAndNotify(BaseException e, Player player) {
        handle(e);
        player.sendMessage(": " + e.getMessage());
        DungeonEcho.error(player, "System error: " + e.getMessage());
    }
}