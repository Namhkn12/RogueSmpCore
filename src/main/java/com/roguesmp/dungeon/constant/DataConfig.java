package com.roguesmp.dungeon.constant;
import java.io.File;
import java.util.List;

public final class DataConfig {

    //file type
    public static final String JSON_TYPE = ".json";
    public static final String SCHEM_TYPE = ".schem";

    //folder name constant
    public static final String MAIN_FOLDER = "dungeon";
    public static final String SCHEMATIC_FOLDER = "schematic";
    public static final String SCHEMETA_FOLDER = "schemeta";
    public static final String DUNGEON_TEMPLATE_FOLDER = "dungeon_template";
    public static final String RUNTIME_FOLDER = "runtime";
    public static final String PARTY_FOLDER = "party";
    public static final String REGION_FOLDER = "region";
    public static final String SPAWNER_TEMPLATE_FOLDER = "spawner_template";

    //file name prefix constant
    public static final String DUNGEON_TEMPLATE_FILE = "dungeon_";
    public static final String DUNGEON_INSTANCE_FILE = "di_";
    public static final String REGION_INSTANCE_FILE = "region_";
    public static final String SCHEMETA_FILE = "schemeta_";
    public static final String SPAWNER_TEMPLATE_FILE = "spawner_";
    public static final String PARTY_FILE = "party_";

    public static String getSpawnerTemplateFolder(){
        return buildFolderPath(List.of(
                MAIN_FOLDER,
                SPAWNER_TEMPLATE_FOLDER
        ));
    }

    public static String getRegionFolder() {
        return buildFolderPath(List.of(
                MAIN_FOLDER,
                REGION_FOLDER
        ));
    }

    public static String getPartyFolder() {
        return buildFolderPath(List.of(
                MAIN_FOLDER,
                PARTY_FOLDER
        ));
    }

    public static String getSchematicFolder() {
        return buildFolderPath(List.of(
                MAIN_FOLDER,
                SCHEMATIC_FOLDER
        ));
    }

    public static String getSchemetaFolder() {
        return buildFolderPath(List.of(
                MAIN_FOLDER,
                SCHEMETA_FOLDER
        ));
    }

    public static String getDungeonTemplateFolder(){
        return buildFolderPath(List.of(
                MAIN_FOLDER,
                DUNGEON_TEMPLATE_FOLDER
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

    private DataConfig() {}
}
