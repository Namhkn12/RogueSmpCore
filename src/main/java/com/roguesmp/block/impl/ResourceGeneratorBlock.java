package com.roguesmp.block.impl;

import com.roguesmp.block.*;
import com.roguesmp.block.event.SmpBlockBreakEvent;
import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.codec.Codec;
import com.roguesmp.block.gui.BlockGeneratorGui;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ResourceGeneratorBlock extends SmpBlock implements Tickable, Directional, EnergyStorage {

    public record Data(String place, int placeDelayTicks, int breakDelayTicks, List<BlockDrop> drops, int maxEnergy, int energyPerTick, int energyPerFuel, int lootCapacity) {

        public static final Data DEFAULT = new Data(
                "minecraft:stone",
                100,
                100,
                List.of(),
                5000,
                1,
                200,
                1000
        );

        public static final Codec<Data> CODEC = Codec.composite(
                Codec.STRING.optionalFieldOf("place", DEFAULT.place()).forGetter(Data::place),
                Codec.INT.optionalFieldOf("place_delay", DEFAULT.placeDelayTicks()).forGetter(Data::placeDelayTicks),
                Codec.INT.optionalFieldOf("break_delay", DEFAULT.breakDelayTicks()).forGetter(Data::breakDelayTicks),
                Codec.listOf(BlockDrop.CODEC).optionalFieldOf("drops", DEFAULT.drops()).forGetter(Data::drops),
                Codec.INT.optionalFieldOf("max_energy", DEFAULT.maxEnergy()).forGetter(Data::maxEnergy),
                Codec.INT.optionalFieldOf("energy_per_tick", DEFAULT.energyPerTick()).forGetter(Data::energyPerTick),
                Codec.INT.optionalFieldOf("energy_per_fuel", DEFAULT.energyPerFuel()).forGetter(Data::energyPerFuel),
                Codec.INT.optionalFieldOf("capacity", DEFAULT.lootCapacity()).forGetter(Data::lootCapacity),
                Data::new
        );
    }

    private static final int GUI_REFRESH_INTERVAL = 10;

    private BlockFace faceDirection;
    private int placeDelay;
    private int breakTicksLeft;
    private boolean mining;
    private int energy;
    private final List<StoredItem> loot = new ArrayList<>();
    private @Nullable StoredItem fuel;
    private @Nullable BlockGeneratorGui gui;
    private int guiRefreshTicks;

    private UUID crackOverlayId;
    private int shownStage = -1;

    private Data data() {
        return getType().data(Data.CODEC, Data.DEFAULT);
    }

    @Override
    public void tick() {
        tryConsumeFuel();
        if (mining) tickMining();
        else if (getEnergy() > 0) tickWaiting();
        tickGuiRefresh();
    }

    private void tickGuiRefresh() {
        if (gui == null) return;
        if (++guiRefreshTicks < GUI_REFRESH_INTERVAL) return;

        guiRefreshTicks = 0;
        gui.refresh();
    }

    private void tickWaiting() {
        Block target = targetToMine();
        if (isCorrectBlock(target)) {
            beginMining(target);
            return;
        }

        if (--placeDelay > 0) return;

        if (!target.getType().isAir()) {
            placeDelay = data().placeDelayTicks();
            return;
        }

        placeGenerated(target);
        beginMining(target);
    }

    private void beginMining(Block target) {
        mining = true;
        breakTicksLeft = data().breakDelayTicks();
        markDirty();
        spawnCrack(target);
    }

    private void tickMining() {
        Block target = targetToMine();
        if (!isCorrectBlock(target)) {
            resetToWaiting();
            return;
        }

        if (getEnergy() <= 0) return;

        removeEnergy(data().energyPerTick());
        if (--breakTicksLeft <= 0) {
            harvest(target);
            resetToWaiting();
            return;
        }

        updateCrack();
    }

    private void resetToWaiting() {
        mining = false;
        placeDelay = data().placeDelayTicks();
        removeCrack();
        markDirty();
    }

    private Block targetToMine() {
        return getLocation().getBlock().getRelative(faceDirection);
    }

    private void placeGenerated(Block target) {
        String place = data().place();
        Material vanilla = BlockRef.vanillaMaterial(place);
        if (vanilla != null) {
            target.setType(vanilla, false);
        } else {
            BlockManager.getInstance().place(target, place, null, null, null);
        }
    }

    private boolean isCorrectBlock(Block target) {
        if (BlockManager.getInstance().get(BlockPos.of(target)) != null) return true;

        Material vanilla = BlockRef.vanillaMaterial(data().place());
        return vanilla != null && target.getType() == vanilla;
    }

    private void harvest(Block target) {
        BlockManager.getInstance().breakBlock(target, null);

        for (BlockDrop drop : data().drops()) {
            StoredItem item = drop.rollStored();
            if (item != null) storeLoot(item);
        }
        markDirty();
    }

    private void storeLoot(StoredItem rolled) {
        int spaceLeft = data().lootCapacity() - getOccupiedCapacity();
        int toStore = Math.max(0, Math.min(rolled.amount(), spaceLeft));

        if (toStore > 0) {
            boolean merged = false;
            for (int i = 0; i < loot.size(); i++) {
                StoredItem existing = loot.get(i);
                if (!existing.item().equals(rolled.item())) continue;

                loot.set(i, existing.withAmount(existing.amount() + toStore));
                merged = true;
                break;
            }
            if (!merged) loot.add(rolled.withAmount(toStore));
        }

        int overflow = rolled.amount() - toStore;
        if (overflow <= 0) return;

        ItemStack dropped = rolled.withAmount(overflow).toItemStack();
        if (dropped != null) {
            Location center = getLocation().toCenterLocation();
            center.getWorld().dropItemNaturally(center, dropped);
        }
    }

    /** The generator's total stored item count, across every distinct id. */
    public int getOccupiedCapacity() {
        int total = 0;
        for (StoredItem stored : loot) total += stored.amount();
        return total;
    }

    public int getLootCapacity() {
        return data().lootCapacity();
    }

    public boolean isMining() {
        return mining;
    }

    /** Ticks left until the current mine finishes, only meaningful while {@link #isMining}. */
    public int getBreakTicksLeft() {
        return breakTicksLeft;
    }

    /** The block id this generator places and mines - vanilla as {@code "minecraft:<material>"}, else a custom block id. */
    public String getPlaceTarget() {
        return data().place();
    }

    public int getPlaceDelayTicks() {
        return data().placeDelayTicks();
    }

    public int getBreakDelayTicks() {
        return data().breakDelayTicks();
    }

    public int getEnergyPerTick() {
        return data().energyPerTick();
    }

    public int getEnergyPerFuel() {
        return data().energyPerFuel();
    }

    private void spawnCrack(Block target) {
        if (isCustom(target)) {
            crackOverlayId = BlockVisual.spawnCrackOverlay(BlockPos.of(target), 0).getUniqueId();
        } else {
            BlockVisual.sendBlockDamage(BlockPos.of(target), 0);
        }
        shownStage = 0;
    }

    private void updateCrack() {
        int total = data().breakDelayTicks();
        int elapsed = total - breakTicksLeft;
        int stage = Math.min((elapsed * BlockVisual.CRACK_STAGES) / total, BlockVisual.CRACK_STAGES - 1);
        if (stage == shownStage) return;
        shownStage = stage;

        if (crackOverlayId != null) {
            Entity overlay = Bukkit.getEntity(crackOverlayId);
            if (overlay != null) BlockVisual.updateCrackOverlay(overlay, stage);
        } else {
            BlockVisual.sendBlockDamage(BlockPos.of(targetToMine()), stage);
        }
    }

    private void removeCrack() {
        if (shownStage == -1) return;

        if (crackOverlayId != null) {
            Entity overlay = Bukkit.getEntity(crackOverlayId);
            if (overlay != null) overlay.remove();
            crackOverlayId = null;
        } else {
            BlockVisual.sendBlockDamage(BlockPos.of(targetToMine()), -1);
        }
        shownStage = -1;
    }

    private boolean isCustom(Block target) {
        return BlockManager.getInstance().isSmpBlock(target.getLocation());
    }

    @Override
    public int getEnergy() {
        return energy;
    }

    @Override
    public int getMaxEnergy() {
        return data().maxEnergy();
    }

    @Override
    public int addEnergy(int amount) {
        energy = Math.min(energy + amount, data().maxEnergy());
        return energy;
    }

    @Override
    public int removeEnergy(int amount) {
        energy = Math.max(0, energy - amount);
        return energy;
    }

    /**
     * Every currently-held loot entry, by id and raw (possibly larger-than-one-real-stack) amount -
     * its size is how many distinct item ids are stored; their amounts sum to at most
     * {@link #getOccupiedCapacity}. Returned as {@link StoredItem}, not resolved to {@link ItemStack}:
     * an entry's amount can exceed its real max stack size, which isn't safe to hand to the client as
     * one stack - the caller (the gui) has to clamp via {@link StoredItem#maxStackSize()} before ever
     * resolving one.
     */
    public List<StoredItem> getLoot() {
        return List.copyOf(loot);
    }

    /**
     * Removes up to {@code amount} of {@code item} from loot, or null if that id isn't currently
     * stored. The loot entry shrinks or disappears accordingly.
     */
    public @Nullable StoredItem takeLoot(String item, int amount) {
        for (int i = 0; i < loot.size(); i++) {
            StoredItem stored = loot.get(i);
            if (!stored.item().equals(item)) continue;

            int taken = Math.min(amount, stored.amount());
            int remaining = stored.amount() - taken;
            if (remaining > 0) loot.set(i, stored.withAmount(remaining));
            else loot.remove(i);

            markDirty();
            return new StoredItem(item, taken);
        }
        return null;
    }

    public @Nullable StoredItem getFuel() {
        return fuel;
    }

    /** Whatever's placed is consumed into energy immediately. */
    public void setFuel(@Nullable StoredItem item) {
        fuel = item;
        tryConsumeFuel();
        markDirty();
    }

    /**
     * Converts as much of the fuel as the remaining energy capacity allows into energy, leaving any
     * leftover sitting in {@link #fuel} (e.g. a stack bigger than what currently fits) for next time.
     */
    private void tryConsumeFuel() {
        if (fuel == null) return;

        int energyPerItem = data().energyPerFuel();
        int capacity = getMaxEnergy() - energy;
        int consumed = energyPerItem > 0 ? Math.min(fuel.amount(), capacity / energyPerItem) : 0;
        if (consumed <= 0) return;

        addEnergy(consumed * energyPerItem);
        int remaining = fuel.amount() - consumed;
        fuel = remaining > 0 ? fuel.withAmount(remaining) : null;
        markDirty();
    }

    @Override
    public void onPlaced(Player player, ItemStack placedFrom) {
        faceDirection = getPlacementFacing(player);
        placeDelay = data().placeDelayTicks();
        markDirty();
        rotateDisplay(Bukkit.getEntity(getDisplayId()), faceDirection);
    }

    @Override
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        event.setCancelled(true);
        if (gui == null) gui = new BlockGeneratorGui(this);
        gui.showInventory(event.getPlayer());
    }

    public void releaseGui(BlockGeneratorGui closed) {
        if (gui == closed) gui = null;
    }

    @Override
    public void onBlockBreak(SmpBlockBreakEvent event) {
        Location center = getLocation().toCenterLocation();
        World world = center.getWorld();
        for (StoredItem stored : loot) {
            ItemStack item = stored.toItemStack();
            if (item != null) world.dropItemNaturally(center, item);
        }
        loot.clear();

        if (fuel != null) {
            ItemStack item = fuel.toItemStack();
            if (item != null) world.dropItemNaturally(center, item);
            fuel = null;
        }
    }

    @Override
    public void onUnload() {
        removeCrack();
    }

    @Override
    protected void collectSections(List<StateSection<?>> sections) {
        super.collectSections(sections);
        sections.add(StateSection.of(Directional.CODEC.optionalFieldOf("direction", BlockFace.EAST), () -> faceDirection, direction -> this.faceDirection = direction));
        sections.add(StateSection.of(Codec.INT.optionalFieldOf("energy", Data.DEFAULT.maxEnergy()), () -> energy, integer -> energy = integer));
        sections.add(StateSection.of(Codec.listOf(StoredItem.CODEC).optionalFieldOf("loot", List.of()), () -> loot, value -> { loot.clear(); loot.addAll(value); }));
        sections.add(StateSection.of(StoredItem.CODEC.optionalFieldOf("fuel"), () -> Optional.ofNullable(fuel), value -> fuel = value.orElse(null)));
    }

    @Override
    public BlockFace getFaceDirection() {
        return faceDirection;
    }
}
