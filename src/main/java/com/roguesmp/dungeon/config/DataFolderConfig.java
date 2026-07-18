package com.roguesmp.dungeon.config;

import java.io.File;
import java.util.List;

public class DataFolderConfig {
    //file type
    public static final String JSON_TYPE = ".json";
    public static final String SCHEM_TYPE = ".schem";

    //folder name constant
    public static final String MAIN_FOLDER = "dungeon_v2";
    public static final String TEMPLATE_FOLDER = "template";
    public static final String RUNTIME_FOLDER = "runtime";

    public static final String DUNGEON_TEMPLATE_FOLDER = "dungeon_template";
    public static final String ROOM_FOLDER = "room_template";
    public static final String SCHEMETA_FOLDER = "schemeta";
    public static final String SCHEMATIC_FOLDER = "schematic";
    public static final String SPAWNER_TEMPLATE_FOLDER = "spawner_template";

    public static final String PARTY_FOLDER = "party";
    public static final String REGION_FOLDER = "region";
    public static final String DUNGEON_SESSION_FOLDER = "session";


    //file name prefix constant
    public static final String DUNGEON_TEMPLATE_FILE = "dungeon";
    public static final String ROOM_TEMPLATE_FILE = "room";
    public static final String DUNGEON_INSTANCE_FILE = "di";
    public static final String SCHEMETA_FILE = "schemeta";
    public static final String SPAWNER_TEMPLATE_FILE = "spawner";
    public static final String PARTY_FILE = "party";

    public static String getRoomTemplateFolder(){
        return buildFolderPath(List.of(
                MAIN_FOLDER,
                TEMPLATE_FOLDER,
                ROOM_FOLDER
        ));
    }

    public static String getSpawnerTemplateFolder(){
        return buildFolderPath(List.of(
                MAIN_FOLDER,
                TEMPLATE_FOLDER,
                SPAWNER_TEMPLATE_FOLDER
        ));
    }

    public static String getRegionFolder() {
        return buildFolderPath(List.of(
                MAIN_FOLDER,
                RUNTIME_FOLDER,
                REGION_FOLDER
        ));
    }

    public static String getPartyFolder() {
        return buildFolderPath(List.of(
                MAIN_FOLDER,
                RUNTIME_FOLDER,
                PARTY_FOLDER
        ));
    }

    public static String getSchematicFolder() {
        return buildFolderPath(List.of(
                MAIN_FOLDER,
                TEMPLATE_FOLDER,
                SCHEMATIC_FOLDER
        ));
    }

    public static String getSchemetaFolder() {
        return buildFolderPath(List.of(
                MAIN_FOLDER,
                TEMPLATE_FOLDER,
                SCHEMETA_FOLDER
        ));
    }

    public static String getDungeonTemplateFolder(){
        return buildFolderPath(List.of(
                MAIN_FOLDER,
                TEMPLATE_FOLDER,
                DUNGEON_TEMPLATE_FOLDER
        ));
    }

    public static String getDungeonSessionFolder(){
        return buildFolderPath(List.of(
                MAIN_FOLDER,
                RUNTIME_FOLDER,
                DUNGEON_SESSION_FOLDER
        ));
    }

    public static String getRuntimeFolder(){
        return buildFolderPath(List.of(
                MAIN_FOLDER,
                RUNTIME_FOLDER
        ));
    }

    private static String buildFolderPath(List<String> folders) {
        return String.join(File.separator, folders);
    }

    private DataFolderConfig() {}
}
