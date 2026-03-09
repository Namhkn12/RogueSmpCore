package com.roguesmp.dungeon.constraint;
import java.io.File;
import java.util.List;

public final class DungeonConfig {

    public static final String JSON_TYPE = ".json";
    public static final String SCHEM_TYPE = ".schem";

    public static final String MAIN_FOLDER = "dungeon";
    public static final String SCHEMATIC_FOLDER = "schematic";
    public static final String SCHEMETA_FOLDER = "schemeta";
    public static final String DUNGEON_TEMPLATE_FOLDER = "dungeon_template";
    public static final String RUNTIME_FOLDER = "runtime";
    public static final String REGION_FOLDER = "region";

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

    private static String buildFolderPath(List<String> folders) {
        return String.join(File.separator, folders);
    }

    private DungeonConfig() {}
}
