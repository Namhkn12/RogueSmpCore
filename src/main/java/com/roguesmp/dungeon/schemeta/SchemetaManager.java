package com.roguesmp.dungeon.schemeta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.dungeon.ultis.TimeId;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.LocalSession;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class SchemetaManager {

    private static SchemetaManager INSTANCE = null;
    private final Map<String, Schemeta> schemetaMap = new HashMap<>();
    private final File schemFolder;
    private final File jsonFolder;
    private final Gson gson;
    private final RogueSmpCore plugin;

    public static void init(RogueSmpCore plugin) {
        if (INSTANCE != null) {
            throw new IllegalStateException("PartyManager already initialized!");
        }
        INSTANCE = new SchemetaManager(plugin);
    }

    public static SchemetaManager getInstance(){
        if (INSTANCE == null) {
            throw new RuntimeException(SchemetaManager.class.getSimpleName() + "is null when getInstance() is called.");
        }
        return INSTANCE;
    }

    public void registerCommands() {
        new SchemetaCommand(this).register();
    }

    public SchemetaManager(RogueSmpCore plugin) {
        this.plugin = plugin;

        this.schemFolder = new File(plugin.getDataFolder(), "schematics");
        this.jsonFolder = new File(plugin.getDataFolder(), "schemeta");

        if (!schemFolder.exists()) schemFolder.mkdirs();
        if (!jsonFolder.exists()) jsonFolder.mkdirs();

        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public Schemeta createFromSelection(Player player, String name) throws Exception {

        Region region = getSelection(player);

        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(region.getMinimumPoint());

        try (var editSession = WorldEdit.getInstance()
                .newEditSession(BukkitAdapter.adapt(player.getWorld()))) {

            ForwardExtentCopy copy = new ForwardExtentCopy(
                    editSession,
                    region,
                    clipboard,
                    region.getMinimumPoint()
            );

            copy.setCopyingEntities(true);
            copy.setCopyingBiomes(true);

            Operations.complete(copy);
        }

        File schemFile = new File(schemFolder, name + ".schem");

        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC
                .getWriter(new FileOutputStream(schemFile))) {

            writer.write(clipboard);
        }

        Schemeta schemeta = new Schemeta();
        schemeta.setSchemId(TimeId.generateTimeId());
        schemeta.setSchemName(name);
        schemeta.setSchematic("schematics/" + name + ".schem");

        saveSchemetaObject(schemeta);

        return schemeta;
    }

    public void saveSchemetaObject(Schemeta schemeta) {

        try {
            File file = new File(jsonFolder, schemeta.getSchemId() + ".json");

            try (Writer writer = new FileWriter(file)) {
                gson.toJson(schemeta, writer);
            }

            schemetaMap.put(schemeta.getSchemId(), schemeta);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete(String id) {
        schemetaMap.remove(id);

        File json = new File(jsonFolder, id + ".json");
        File schem = new File(schemFolder, id + ".schem");

        if (json.exists()) json.delete();
        if (schem.exists()) schem.delete();
    }

    public Schemeta get(String id) {
        return schemetaMap.get(id);
    }

    private Region getSelection(Player player) throws Exception {

        LocalSession session = WorldEdit.getInstance()
                .getSessionManager()
                .get(BukkitAdapter.adapt(player));

        return session.getSelection(session.getSelectionWorld());
    }
}