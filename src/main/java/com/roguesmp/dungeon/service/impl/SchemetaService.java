package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.constraint.FolderConfig;
import com.roguesmp.dungeon.data.Schemeta;
import com.roguesmp.dungeon.manager.SchemetaManager;
import com.roguesmp.dungeon.service.ISchemetaService;
import com.roguesmp.dungeon.ultis.TimeId;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.List;

public class SchemetaService implements ISchemetaService {

    private static final String PREFIX = "schemeta";
    private final SchemetaManager schemetaManager;

    public SchemetaService(SchemetaManager schemetaManager) {
        this.schemetaManager = schemetaManager;
    }

    @Override
    public Schemeta createSchemeta(Player player, String name) throws Exception {
        Region region = getSelection(player);
        Clipboard clipboard = copyRegion(region, player);

        schemetaManager.saveSchem(name, clipboard);

        Schemeta schemeta = new Schemeta();
        schemeta.setSchemId(String.join("_", PREFIX, TimeId.generateTimeId()));
        schemeta.setSchemName(name);
        schemeta.setSchematic(String.join(File.separator, List.of(FolderConfig.getSchematicFolder(), name)) + FolderConfig.SCHEM_TYPE);

        schemetaManager.register(schemeta);
        return schemeta;
    }

    private Clipboard copyRegion(Region region, Player player) throws Exception {
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(BukkitAdapter.asBlockVector(player.getLocation()));

        try (var editSession = WorldEdit.getInstance()
                .newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
            ForwardExtentCopy copy = new ForwardExtentCopy(
                    editSession, region, clipboard, region.getMinimumPoint()
            );
            copy.setCopyingEntities(true);
            copy.setCopyingBiomes(true);
            Operations.complete(copy);
        }
        return clipboard;
    }

    @Override
    public void deleteSchemeta(String id) {
        schemetaManager.delete(id);
    }

    @Override
    public Schemeta getSchemeta(String id) {
        return schemetaManager.get(id);
    }

    @Override
    public List<Schemeta> getSchemetaList() {
        return schemetaManager.getSchemetaList();
    }

    @Override
    public List<String> getSchemetaIdList() {
        return schemetaManager.getSchematicIdList();
    }

    @Override
    public void pasteSchematic(String id, Location location) throws Exception {
        Schemeta schemeta = schemetaManager.get(id);
        if (schemeta == null) throw new IllegalArgumentException("Schemeta not found: " + id);

        Clipboard clipboard = schemetaManager.loadSchem(schemeta.getSchemName());

        try (var editSession = WorldEdit.getInstance()
                .newEditSession(BukkitAdapter.adapt(location.getWorld()))) {

            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                    .copyEntities(true)
                    .copyBiomes(true)
                    .build();

            Operations.complete(operation);
        }
    }

    private Region getSelection(Player player) throws Exception {
        LocalSession session = WorldEdit.getInstance()
                .getSessionManager()
                .get(BukkitAdapter.adapt(player));
        return session.getSelection(session.getSelectionWorld());
    }
}