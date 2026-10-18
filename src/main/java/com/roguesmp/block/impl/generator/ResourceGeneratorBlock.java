package com.roguesmp.block.impl.generator;

import com.roguesmp.block.*;
import com.roguesmp.block.data.GeneratorData;
import com.roguesmp.block.event.SmpBlockBreakEvent;
import com.roguesmp.block.gui.ResourceGeneratorGui;
import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.block.storage.FixedSlots;
import com.roguesmp.block.storage.ItemStorage;
import com.roguesmp.block.storage.ItemStorageHolder;
import com.roguesmp.block.storage.PooledSlots;
import com.roguesmp.block.storage.SlotAccess;
import com.roguesmp.item.component.impl.GeneratorCatalystComponent;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.block.impl.generator.stat.GeneratorEffect;
import com.roguesmp.block.impl.generator.part.GeneratorCatalyst;
import com.roguesmp.block.impl.generator.stat.GeneratorStats;
import com.roguesmp.block.impl.generator.part.ModuleInstallResult;
import com.roguesmp.block.impl.generator.part.ModuleSlots;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ResourceGeneratorBlock extends SmpBlock implements Tickable, Directional, EnergyStorage, ItemStorageHolder {

    private static final int GUI_REFRESH_INTERVAL = 10;

    private static final MapCodec<BlockFace> DIRECTION_CODEC = Directional.CODEC.optionalFieldOf("direction", BlockFace.EAST);
    private static final MapCodec<Optional<Integer>> ENERGY_CODEC = Codec.INT.optionalFieldOf("energy");

    private final GeneratorData data;
    private final @Nullable Material placeMaterial;
    private final ModuleSlots modules;
    private final ItemStorage storage = new ItemStorage(this).lockFaces(() -> Set.of(this.faceDirection));
    private final FixedSlots catalystSlot = storage.add(new FixedSlots("catalyst", 1, SlotAccess.INSERT, item -> GeneratorCatalystComponent.of(item.item()) != null), this::onCatalystSlotChanged);
    private final PooledSlots loot = storage.add(new PooledSlots("loot", () -> this.stats.lootCapacity(), SlotAccess.EXTRACT));
    private final GeneratorCatalyst catalyst = new GeneratorCatalyst(this, catalystSlot, this::rebuildStats, this::markDirty);
    private final CrackDisplay crack = new CrackDisplay();
    private GeneratorStats stats;
    private double energyDebt;

    private BlockFace faceDirection = BlockFace.EAST;
    private int placeDelay;
    private int breakTicksLeft;
    private boolean mining;
    private int energy;
    private @Nullable ResourceGeneratorGui gui;
    private int guiRefreshTicks;

    public ResourceGeneratorBlock(BlockProperties properties, GeneratorData data) {
        super(properties);
        this.data = data;
        this.placeMaterial = BlockRef.vanillaMaterial(data.place());
        this.modules = new ModuleSlots(data.moduleSlots());
        this.stats = new GeneratorStats(data, List.of());
    }

    @Override
    public void onHydrated() {
        rebuildStats();
    }

    @Override
    public void tick() {
        catalyst.consume();
        catalyst.tick();
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
            placeDelay = stats.placeDelayTicks();
            return;
        }

        placeGenerated(target);
        beginMining(target);
    }

    private void beginMining(Block target) {
        mining = true;
        breakTicksLeft = stats.breakDelayTicks();
        markDirty();
        crack.show(target);
    }

    private void tickMining() {
        Block target = targetToMine();
        if (!isCorrectBlock(target)) {
            resetToWaiting();
            return;
        }

        if (getEnergy() <= 0) return;

        consumeTickEnergy();
        if (--breakTicksLeft <= 0) {
            harvest(target);
            resetToWaiting();
            return;
        }

        crack.update(target, crackStage());
    }

    private int crackStage() {
        int total = stats.breakDelayTicks();
        int elapsed = total - breakTicksLeft;
        return Math.min((elapsed * BlockVisual.CRACK_STAGES) / total, BlockVisual.CRACK_STAGES - 1);
    }

    private void consumeTickEnergy() {
        energyDebt += stats.energyPerTick();
        int cost = (int) energyDebt;
        energyDebt -= cost;
        removeEnergy(cost);
    }

    private void resetToWaiting() {
        mining = false;
        placeDelay = stats.placeDelayTicks();
        crack.remove(targetToMine());
        markDirty();
    }

    private Block targetToMine() {
        return getLocation().getBlock().getRelative(faceDirection);
    }

    private void placeGenerated(Block target) {
        if (placeMaterial != null) {
            target.setType(placeMaterial, false);
        } else {
            BlockManager.getInstance().place(target, data.place(), null, null, null);
        }
    }

    private boolean isCorrectBlock(Block target) {
        SmpBlock custom = BlockManager.getInstance().get(BlockPos.of(target));
        if (custom != null) return placeMaterial == null && data.place().equals(custom.getId());

        return placeMaterial != null && target.getType() == placeMaterial;
    }

    private void harvest(Block target) {
        BlockManager.getInstance().breakBlock(target, null);

        List<StoredItem> drops = new ArrayList<>();
        for (BlockDrop drop : data.drops()) {
            StoredItem item = stats.rollDrop(drop);
            if (item != null) drops.add(item);
        }
        stats.behaviors().forEach(behavior -> behavior.onHarvest(this, drops));
        for (StoredItem drop : drops) {
            int overflow = drop.amount() - loot.insert(drop, false);
            if (overflow > 0) dropAtCenter(drop.withAmount(overflow));
        }
        catalyst.onHarvest();
        markDirty();
    }

    private void dropAtCenter(StoredItem stored) {
        ItemStack item = stored.toItemStack();
        if (item == null) return;

        Location center = getLocation().toCenterLocation();
        center.getWorld().dropItemNaturally(center, item);
    }

    private void rebuildStats() {
        catalyst.resolve();

        List<GeneratorEffect> effects = new ArrayList<>();
        modules.collectEffects(catalyst.activeItem(), effects);
        catalyst.collectEffects(effects);

        stats = new GeneratorStats(data, effects);
        energy = Math.min(energy, stats.maxEnergy());
        breakTicksLeft = Math.min(breakTicksLeft, stats.breakDelayTicks());
    }

    public GeneratorStats stats() {
        return stats;
    }

    public ModuleSlots modules() {
        return modules;
    }

    private void onCatalystSlotChanged() {
        catalyst.consume();
        if (gui != null) gui.refresh();
    }

    @Override
    public ItemStorage itemStorage() {
        return storage;
    }

    public FixedSlots catalystSlot() {
        return catalystSlot;
    }

    public PooledSlots loot() {
        return loot;
    }

    public GeneratorCatalyst catalyst() {
        return catalyst;
    }

    public boolean isMining() {
        return mining;
    }

    /** Ticks left until the current mine finishes, only meaningful while {@link #isMining}. */
    public int getBreakTicksLeft() {
        return breakTicksLeft;
    }

    public ModuleInstallResult installModule(String itemId) {
        ModuleInstallResult result = modules.install(itemId);
        if (result == ModuleInstallResult.INSTALLED) {
            rebuildStats();
            markDirty();
        }
        return result;
    }

    public @Nullable String removeModule(int index) {
        String removed = modules.remove(index);
        if (removed != null) {
            rebuildStats();
            markDirty();
        }
        return removed;
    }

    @Override
    public int getEnergy() {
        return energy;
    }

    @Override
    public int getMaxEnergy() {
        return stats.maxEnergy();
    }

    @Override
    public int addEnergy(int amount) {
        int accepted = Math.min(amount, stats.maxEnergy() - energy);
        energy += accepted;
        return accepted;
    }

    @Override
    public int removeEnergy(int amount) {
        int removed = Math.min(amount, energy);
        energy -= removed;
        return removed;
    }

    /**
     * Removes up to {@code amount} of {@code item} from loot, or null if that id isn't currently
     * stored. The loot entry shrinks or disappears accordingly.
     */
    public @Nullable StoredItem takeLoot(String item, int amount) {
        return loot.take(item, amount, false);
    }

    @Override
    public void onPlaced(Player player, ItemStack placedFrom) {
        faceDirection = getPlacementFacing(player);
        placeDelay = stats.placeDelayTicks();
        markDirty();
        rotateDisplay(Bukkit.getEntity(getDisplayId()), faceDirection);
    }

    @Override
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        event.setCancelled(true);
        openGui(event.getPlayer());
    }

    public void openGui(HumanEntity viewer) {
        if (gui == null) gui = new ResourceGeneratorGui(this);
        gui.showInventory(viewer);
    }

    public void releaseGui(ResourceGeneratorGui closed) {
        if (gui == closed) gui = null;
    }

    @Override
    public void onBlockBreak(SmpBlockBreakEvent event) {
        if (gui != null) gui.closeAll();
        modules.drain().forEach(moduleId -> dropAtCenter(new StoredItem(moduleId, 1)));
    }

    @Override
    public void onUnload() {
        crack.remove(targetToMine());
    }

    @Override
    protected void collectSections(List<StateSection<?>> sections) {
        super.collectSections(sections);
        sections.add(StateSection.of(DIRECTION_CODEC, () -> faceDirection, direction -> this.faceDirection = direction));
        sections.add(StateSection.of(ENERGY_CODEC, () -> Optional.of(energy), value -> energy = value.orElse(data.maxEnergy())));
        sections.add(modules.section());
        sections.add(catalyst.activeSection());
    }

    @Override
    public BlockFace getFaceDirection() {
        return faceDirection;
    }
}
