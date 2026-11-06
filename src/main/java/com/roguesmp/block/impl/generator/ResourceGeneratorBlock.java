package com.roguesmp.block.impl.generator;

import com.roguesmp.utils.ItemRef;
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
import com.roguesmp.block.upgrade.UpgradeHost;
import com.roguesmp.block.upgrade.UpgradeSet;
import com.roguesmp.block.upgrade.ActiveCatalyst;
import com.roguesmp.block.upgrade.DurationUnit;
import com.roguesmp.block.upgrade.UpgradeInstallResult;
import com.roguesmp.block.upgrade.UpgradeType;
import com.roguesmp.block.upgrade.effect.OutputContext;
import com.roguesmp.item.component.impl.BlockUpgradeComponent;
import com.roguesmp.block.upgrade.stat.BlockStat;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ResourceGeneratorBlock extends SmpBlock implements Tickable, Directional, EnergyStorage, ItemStorageHolder, UpgradeHost {

    private static final int GUI_REFRESH_INTERVAL = 10;

    private static final MapCodec<BlockFace> DIRECTION_CODEC = Directional.CODEC.optionalFieldOf("direction", BlockFace.EAST);
    private static final MapCodec<Optional<Integer>> ENERGY_CODEC = Codec.INT.optionalFieldOf("energy");
    private static final MapCodec<Optional<ActiveCatalyst>> CATALYST_CODEC = ActiveCatalyst.CODEC.optionalFieldOf("active_catalyst");

    private static final Set<BlockStat> STATS = EnumSet.of(BlockStat.ENERGY_PER_TICK, BlockStat.MAX_ENERGY,
            BlockStat.GENERATOR_PLACE_DELAY, BlockStat.GENERATOR_BREAK_DELAY, BlockStat.GENERATOR_LOOT_CAPACITY,
            BlockStat.GENERATOR_DROP_AMOUNT, BlockStat.GENERATOR_RARE_DROP_CHANCE, BlockStat.GENERATOR_COMMON_DROP_AMOUNT);

    private final GeneratorData data;
    private final @Nullable Material placeMaterial;
    private final ItemStorage storage = new ItemStorage(this).lockFaces(() -> Set.of(this.faceDirection));
    private final FixedSlots catalystFeed = storage.add(new FixedSlots("catalyst", "Chất xúc tác", 1, SlotAccess.INSERT, item -> isValidCatalyst(item.item())), this::refreshGui);
    private final PooledSlots loot = storage.add(new PooledSlots("loot", "Kho", this::lootCapacity, SlotAccess.EXTRACT));
    private final CrackDisplay crack = new CrackDisplay();
    private final UpgradeSet upgrades = new UpgradeSet(this);
    private final FixedSlots modules;
    private @Nullable ActiveCatalyst catalyst;
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
        this.placeMaterial = ItemRef.vanillaMaterial(data.place());
        this.modules = storage.add(new FixedSlots("modules", "Mô-đun", data.moduleSlots(), SlotAccess.NONE, item -> checkModule(item.item()) == UpgradeInstallResult.INSTALLED), this::onModulesChanged);
    }

    @Override
    public void onHydrated() {
        upgrades.rebuild();
    }

    @Override
    public void tick() {
        tickCatalyst();
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
            placeDelay = placeDelayTicks();
            return;
        }

        placeGenerated(target);
        beginMining(target);
    }

    private void beginMining(Block target) {
        mining = true;
        breakTicksLeft = breakDelayTicks();
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
        int total = breakDelayTicks();
        int elapsed = total - breakTicksLeft;
        return Math.min((elapsed * BlockVisual.CRACK_STAGES) / total, BlockVisual.CRACK_STAGES - 1);
    }

    private void consumeTickEnergy() {
        energyDebt += energyPerTick();
        int cost = (int) energyDebt;
        energyDebt -= cost;
        removeEnergy(cost);
    }

    private void resetToWaiting() {
        mining = false;
        placeDelay = placeDelayTicks();
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
            StoredItem item = rollDrop(drop);
            if (item != null) drops.add(item);
        }
        drops = upgrades.output(new OutputContext(null, drops, this::rollDrop));
        for (StoredItem drop : drops) {
            int overflow = drop.amount() - loot.insert(drop, false);
            if (overflow > 0) dropAtCenter(drop.withAmount(overflow));
        }
        if (catalystUnit() == DurationUnit.CYCLES) countDownCatalyst();
        markDirty();
    }

    private void dropAtCenter(StoredItem stored) {
        ItemStack item = stored.toItemStack();
        if (item == null) return;

        Location center = getLocation().toCenterLocation();
        center.getWorld().dropItemNaturally(center, item);
    }

    @Override
    public void onUpgradesChanged() {
        energy = Math.min(energy, getMaxEnergy());
        breakTicksLeft = Math.min(breakTicksLeft, breakDelayTicks());
    }

    @Override
    public UpgradeSet upgrades() {
        return upgrades;
    }

    @Override
    public Set<BlockStat> stats() {
        return STATS;
    }

    @Override
    public List<String> upgradeItems() {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < modules.size(); i++) {
            StoredItem stored = modules.get(i);
            if (stored != null && isValidModule(stored.item())) items.add(stored.item());
        }
        if (catalyst != null && isValidCatalyst(catalyst.item())) items.add(catalyst.item());
        return items;
    }

    public UpgradeInstallResult checkModule(String itemId) {
        UpgradeInstallResult result = BlockUpgradeComponent.check(itemId, this, UpgradeType.MODULE);
        return result == UpgradeInstallResult.INSTALLED ? upgrades.canAdd(itemId) : result;
    }

    public boolean isValidModule(String itemId) {
        return BlockUpgradeComponent.check(itemId, this, UpgradeType.MODULE) == UpgradeInstallResult.INSTALLED;
    }

    public boolean isValidCatalyst(String itemId) {
        BlockUpgradeComponent upgrade = BlockUpgradeComponent.of(itemId);
        return upgrade != null && upgrade.duration() > 0 && BlockUpgradeComponent.check(itemId, this, UpgradeType.CATALYST) == UpgradeInstallResult.INSTALLED;
    }

    private void onModulesChanged() {
        upgrades.rebuild();
        refreshGui();
    }

    private void tickCatalyst() {
        if (catalyst != null && !isValidCatalyst(catalyst.item())) setCatalyst(null);
        if (catalyst == null) {
            StoredItem stored = catalystFeed.extract(0, 1, false);
            if (stored != null) setCatalyst(new ActiveCatalyst(stored.item(), BlockUpgradeComponent.of(stored.item()).duration()));
        }
        if (getEnergy() > 0 && catalystUnit() == DurationUnit.TICKS) countDownCatalyst();
    }

    private @Nullable DurationUnit catalystUnit() {
        BlockUpgradeComponent component = catalyst == null ? null : catalyst.component();
        return component == null ? null : component.unit();
    }

    private void countDownCatalyst() {
        if (catalyst == null) return;
        if (catalyst.countDown()) setCatalyst(null);
        markDirty();
    }

    public void clearCatalyst() {
        if (catalyst != null) setCatalyst(null);
    }

    private void setCatalyst(@Nullable ActiveCatalyst value) {
        catalyst = value;
        upgrades.rebuild();
        markDirty();
    }

    public boolean isCatalystPaused() {
        return catalyst != null && catalystUnit() == DurationUnit.TICKS && getEnergy() <= 0;
    }

    private @Nullable StoredItem rollDrop(BlockDrop drop) {
        boolean rare = drop.chance() < 1.0;
        double chance = rare ? upgrades.sheet().applyChance(BlockStat.GENERATOR_RARE_DROP_CHANCE, drop.chance()) : drop.chance();
        double amountMultiplier = upgrades.sheet().multiplier(BlockStat.GENERATOR_DROP_AMOUNT)
                * (rare ? 1.0 : upgrades.sheet().multiplier(BlockStat.GENERATOR_COMMON_DROP_AMOUNT));
        double flatAmount = upgrades.sheet().flatSum(BlockStat.GENERATOR_DROP_AMOUNT)
                + (rare ? 0.0 : upgrades.sheet().flatSum(BlockStat.GENERATOR_COMMON_DROP_AMOUNT));
        return drop.rollStored(chance, amountMultiplier, flatAmount);
    }

    public int placeDelayTicks() {
        return upgrades.sheet().applyInt(BlockStat.GENERATOR_PLACE_DELAY, data.placeDelayTicks());
    }

    public int breakDelayTicks() {
        return upgrades.sheet().applyInt(BlockStat.GENERATOR_BREAK_DELAY, data.breakDelayTicks());
    }

    public int lootCapacity() {
        return upgrades.sheet().applyInt(BlockStat.GENERATOR_LOOT_CAPACITY, data.lootCapacity());
    }

    public double energyPerTick() {
        return upgrades.sheet().apply(BlockStat.ENERGY_PER_TICK, data.energyPerTick());
    }

    public GeneratorData data() {
        return data;
    }

    public FixedSlots modules() {
        return modules;
    }

    public FixedSlots catalystFeed() {
        return catalystFeed;
    }

    private void refreshGui() {
        if (gui != null) gui.refresh();
    }

    @Override
    public ItemStorage itemStorage() {
        return storage;
    }

    public PooledSlots loot() {
        return loot;
    }

    public @Nullable ActiveCatalyst catalyst() {
        return catalyst;
    }

    public boolean isMining() {
        return mining;
    }

    /** Ticks left until the current mine finishes, only meaningful while {@link #isMining}. */
    public int getBreakTicksLeft() {
        return breakTicksLeft;
    }

    @Override
    public int getEnergy() {
        return energy;
    }

    @Override
    public int getMaxEnergy() {
        return upgrades.sheet().applyInt(BlockStat.MAX_ENERGY, data.maxEnergy());
    }

    @Override
    public int addEnergy(int amount) {
        int accepted = Math.min(amount, getMaxEnergy() - energy);
        energy += accepted;
        if (accepted > 0) markDirty();
        return accepted;
    }

    @Override
    public int removeEnergy(int amount) {
        int removed = Math.min(amount, energy);
        energy -= removed;
        if (removed > 0) markDirty();
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
        placeDelay = placeDelayTicks();
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
    }

    @Override
    public void onUnload() {
        crack.remove(targetToMine());
    }

    @Override
    protected void collectSections(List<StateSection<?>> sections) {
        super.collectSections(sections);
        sections.add(StateSection.of(DIRECTION_CODEC, () -> faceDirection, direction -> this.faceDirection = direction));
        sections.add(StateSection.of(ENERGY_CODEC, () -> Optional.of(energy), value -> energy = value.orElse(0)));
        sections.add(StateSection.of(CATALYST_CODEC, () -> Optional.ofNullable(catalyst), value -> catalyst = value.orElse(null)));
    }

    @Override
    public BlockFace getFaceDirection() {
        return faceDirection;
    }
}
