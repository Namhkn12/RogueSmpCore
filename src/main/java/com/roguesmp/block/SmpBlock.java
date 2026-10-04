package com.roguesmp.block;

import com.google.gson.JsonElement;
import com.roguesmp.block.event.SmpBlockBreakEvent;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.codec.DataResult;
import com.roguesmp.player.SmpPlayer;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runtime instance of a placed custom block. Created when its display entity loads and dropped when
 * that entity unloads.
 */
public class SmpBlock {

    private static final List<StateSection<?>> NO_SECTIONS = List.of();

    private final BlockProperties properties;
    private String id;
    private BlockPos pos;
    private Location location;
    private UUID displayId;
    private boolean hydrated;
    private boolean dirty;
    private List<StateSection<?>> sections;

    protected SmpBlock(BlockProperties properties) {
        this.properties = properties;
    }

    public void bind(BlockPos pos, UUID displayId) {
        this.pos = pos;
        this.location = pos.toLocation();
        this.displayId = displayId;
    }

    /**
     * Called when this block is placed from an item by players, before this instance is registered to the manager.
     */
    public void onPlaced(Player player, ItemStack placedFrom) {

    }

    /**
     * Items this block gives back when broken. Defaults to rolling the block's declared
     * {@link BlockProperties#drops()}.
     */
    public List<ItemStack> getDrops(@Nullable SmpPlayer smpPlayer) {
        List<ItemStack> drops = new ArrayList<>();
        for (BlockDrop drop : properties.drops()) {
            ItemStack item = drop.roll(smpPlayer);
            if (item != null) drops.add(item);
        }
        return drops;
    }

    /**
     * Experience orb amount this block gives back when broken. Defaults to rolling the block's
     * declared {@link BlockProperties#experience()}; override for custom amounts.
     */
    public int getExperience(@Nullable SmpPlayer smpPlayer) {
        return properties.experience().roll(smpPlayer);
    }

    /**
     * Called when this block is broken, before {@link #onUnload()} is called.
     */
    public void onBlockBreak(SmpBlockBreakEvent event) {

    }

    public void onBlockInteract(PlayerInteractEvent event) {

    }

    /**
     * Called when this instance is unloaded, via being broken, or the entity for this block is unloaded.
     */
    public void onUnload() {

    }

    /**
     * Called when this block data is fully loaded.
     */
    public void onHydrated() {

    }

    /**
     * Used to load persistent state data into this instance.
     */
    protected void collectSections(List<StateSection<?>> sections) {

    }

    public boolean isStateful() {
        return !sections().isEmpty();
    }

    public DataResult<JsonElement> encodeState() {
        return StateSection.encodeAll(sections());
    }

    public DataResult<Runnable> decodeState(JsonElement json) {
        return StateSection.decodeAll(sections(), json);
    }

    private List<StateSection<?>> sections() {
        if (sections == null) {
            List<StateSection<?>> collected = new ArrayList<>();
            collectSections(collected);
            sections = collected.isEmpty() ? NO_SECTIONS : List.copyOf(collected);
        }
        return sections;
    }

    public BlockProperties getProperties() {
        return properties;
    }

    /** The id of this block's JSON file, e.g. {@code "steel_generator"}. */
    public String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    /**
     * The model key this block's display should currently show, override for a block whose appearance depends on its own state
     * (e.g. an active/idle variant) and call {@link BlockVisual#refreshModel} whenever that state
     * changes to update to the new model.
     */
    public Key getDisplayModel() {
        return properties.model();
    }

    public BlockPos getPos() {
        return pos;
    }

    public Location getLocation() {
        return location;
    }

    public UUID getDisplayId() {
        return displayId;
    }

    public boolean isHydrated() {
        return hydrated;
    }

    public void markHydrated() {
        this.hydrated = true;
    }

    /**
     * Flags this block's persisted state as changed, so the next periodic save (or unload) writes
     * it. Call it whenever a field covered by a {@link StateSection} changes.
     */
    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        this.dirty = false;
    }
}
