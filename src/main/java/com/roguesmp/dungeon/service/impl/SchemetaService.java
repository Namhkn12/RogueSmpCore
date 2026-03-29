package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.constant.DataConfig;
import com.roguesmp.dungeon.data.Schemeta;
import com.roguesmp.dungeon.exception.impl.schemeta.SchemetaNotFoundException;
import com.roguesmp.dungeon.exception.impl.schemeta.SchemetaOperationException;
import com.roguesmp.dungeon.exception.impl.schemeta.SelectionNotFoundException;
import com.roguesmp.dungeon.manager.SchemetaManager;
import com.roguesmp.dungeon.service.ISchemetaService;
import com.roguesmp.dungeon.utils.TimeId;
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
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.util.List;

public class SchemetaService implements ISchemetaService {

    private static final String PREFIX = "schemeta";
    private final SchemetaManager schemetaManager;

    public SchemetaService(SchemetaManager schemetaManager) {
        this.schemetaManager = schemetaManager;
    }

    @Override
    public Schemeta createSchemeta(Player player, String name) {
        Region region = getSelection(player);
        Clipboard clipboard = copyRegion(region, player);

        schemetaManager.saveSchem(name, clipboard);

        Schemeta schemeta = new Schemeta();
        schemeta.setSchemId(String.join("_", PREFIX, TimeId.generateTimeId()));
        schemeta.setSchemName(name);
        schemeta.setSchematic(String.join(File.separator, List.of(DataConfig.getSchematicFolder(), name)) + DataConfig.SCHEM_TYPE);

        schemetaManager.register(schemeta);
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
    public BoundingBox pasteSchematic(String id, Location location) {
        Schemeta schemeta = schemetaManager.get(id);
        if (schemeta == null) throw new SchemetaNotFoundException(id);

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

            // Tính BoundingBox từ clipboard dimensions + paste location
            Region region = clipboard.getRegion();
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            BlockVector3 origin = clipboard.getOrigin();

            // offset so với origin của schematic
            double offsetX = location.getX() - origin.x();
            double offsetY = location.getY() - origin.y();
            double offsetZ = location.getZ() - origin.z();

            return new BoundingBox(
                    min.x() + offsetX, min.y() + offsetY, min.z() + offsetZ,
                    max.x() + offsetX, max.y() + offsetY, max.z() + offsetZ
            );
        } catch (Exception e) {
            throw new SchemetaOperationException("paste schematic: " + id, e);
        }
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
