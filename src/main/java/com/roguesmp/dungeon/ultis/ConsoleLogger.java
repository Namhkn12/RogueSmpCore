package com.roguesmp.dungeon.ultis;

import com.roguesmp.RogueSmpCore;

import java.util.logging.Logger;

public class ConsoleLogger {
    private static final Logger logger = RogueSmpCore.getInstance().getLogger();

    public static void info(String info){
        logger.info(info);
    }

    public static void warn(String warn){

    }

    public static void success(String success){

    }

    public static void error(String error){

    }
}
