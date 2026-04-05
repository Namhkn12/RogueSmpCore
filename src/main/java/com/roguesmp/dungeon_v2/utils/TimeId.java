package com.roguesmp.dungeon_v2.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeId {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String generateTimeId() {
        return LocalDateTime.now().format(FORMATTER);
    }

}
