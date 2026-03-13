package com.roguesmp.dungeon.ultis;

import com.roguesmp.RogueSmpCore;

import java.util.logging.Logger;

public class ConsoleLogger {
    private static final Logger logger = RogueSmpCore.getInstance().getLogger();

    public static void info(String prefix, String info){
        logger.info(String.join(" ", prefix, info));
    }

    public static void warn(String prefix, String warn){
        logger.info(String.join(" ", prefix, warn));
    }

    public static void success(String prefix, String success){
        logger.info(String.join(" ", prefix, success));
    }

    public static void error(String prefix, String error){
        logger.info(String.join(" ", prefix, error));
    }
}
