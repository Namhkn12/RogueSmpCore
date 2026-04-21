package com.roguesmp.dungeon.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class UuidUtil {

    private UuidUtil() {}

    public static UUID parseOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static String toStringOrNull(UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }

    public static Player getPlayerById(String rawUuid) {
        UUID uuid = parseOrNull(rawUuid);
        if (uuid == null) return null;
        return Bukkit.getPlayer(uuid);
    }
}
