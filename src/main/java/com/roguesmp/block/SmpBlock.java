package com.roguesmp.block;

import com.google.gson.JsonElement;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.codec.DataResult;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runtime instance of a placed custom block. Created when its display entity loads and dropped when
 * that entity unloads; the static tuning data lives in its {@link BlockType}.
 */
public class SmpBlock {

    private static final List<StateSection<?>> NO_SECTIONS = List.of();

    private BlockType<?> type;
    private BlockPos pos;
    private Location location;
    private UUID displayId;
    private boolean hydrated;
    private boolean dirty;
    private List<StateSection<?>> sections;

    public void bind(BlockPos pos, UUID displayId) {
        this.pos = pos;
        this.location = pos.toLocation();
        this.displayId = displayId;
    }

    /**
     * Called when this block is placed from an item, before this instance is registered to the manager.
     */
    public void onPlaced(Player player, ItemStack placedFrom) {

    }

    /**
     * Items this block gives back when broken. Defaults to rolling the block's declared
     * {@link BlockProperties#drops()}; override to build custom stacks (e.g. writing this block's
     * own data onto the item) instead of, or alongside, the JSON list.
     */
    public List<ItemStack> getDrops() {
        List<ItemStack> drops = new ArrayList<>();
        for (BlockDrop drop : getType().drops()) {
            ItemStack item = drop.roll();
            if (item != null) drops.add(item);
        }
        return drops;
    }

    /**
     * Experience orb amount this block gives back when broken. Defaults to rolling the block's
     * declared {@link BlockProperties#experience()}; override for custom amounts.
     */
    public int getExperience() {
        return getType().experience().roll();
    }

    /**
     * Called when this block is broken, after {@link #onUnload()} is called.
     */
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.isDropItems() || event.getPlayer().getGameMode() == GameMode.CREATIVE) return;

        for (ItemStack drop : getDrops()) {
            location.getWorld().dropItemNaturally(location.toCenterLocation(), drop);
        }

        int experience = getExperience();
        if (experience > 0) {
            location.getWorld().spawn(location.toCenterLocation(), ExperienceOrb.class, orb -> orb.setExperience(experience));
        }
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

    public BlockType<?> getType() {
        return type;
    }

    void setType(BlockType<?> type) {
        this.type = type;
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
     * it. Call it whenever a field covered by a {@link StateSection} changes; a block that changes
     * without calling it is not saved until it is marked or the server shuts down.
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
