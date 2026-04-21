package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.config.DataFolderConfig;
import com.roguesmp.dungeon.data.definition.Schemeta;
import com.roguesmp.dungeon.exception.impl.schemeta.SchemetaOperationException;
import com.roguesmp.dungeon.exception.impl.schemeta.SelectionNotFoundException;
import com.roguesmp.dungeon.manager.SchemetaManager;
import com.roguesmp.dungeon.service.ISchemetaService;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public class SchemetaService implements ISchemetaService {

    private final SchemetaManager schemetaManager;

    public SchemetaService(SchemetaManager schemetaManager) {
        this.schemetaManager = schemetaManager;
    }

    @Override
    public Schemeta create(Player player, String name) {
        Region region = getSelection(player);
        Clipboard clipboard = copyRegion(region, player);

        Schemeta schemeta = buildSchemeta(name);

        // atomic: cả metadata lẫn schematic file lưu cùng 1 lần qua manager
        schemetaManager.register(schemeta, clipboard);
        return schemeta;
    }

    @Override
    public void delete(String id) {
        // manager lo xóa cả metadata + schematic file + evict cache
        schemetaManager.delete(id);
    }

    @Override
    public Optional<Schemeta> getById(String id) {
        return schemetaManager.get(id);
    }

    @Override
    public List<Schemeta> getAll() {
        return schemetaManager.getAll();
    }

    @Override
    public boolean exists(String id) {
        return schemetaManager.exists(id);
    }

    private Schemeta buildSchemeta(String name) {
        Schemeta schemeta = new Schemeta();
        schemeta.setId(String.join("_", DataFolderConfig.SCHEMETA_FILE, name));
        schemeta.setName(name);
        return schemeta;
    }

    private Clipboard copyRegion(Region region, Player player) {
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

        } catch (Exception e) {
            throw new SchemetaOperationException("copy region", e);
        }
        return clipboard;
    }

    private Region getSelection(Player player) {
        try {
            LocalSession session = WorldEdit.getInstance()
                    .getSessionManager()
                    .get(BukkitAdapter.adapt(player));
            return session.getSelection(session.getSelectionWorld());
        } catch (Exception e) {
            throw new SelectionNotFoundException(e);
        }
    }
}