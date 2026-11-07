package com.roguesmp.block.impl.crusher;

import com.roguesmp.block.BlockDrop;
import com.roguesmp.block.BlockProperties;
import com.roguesmp.block.EnergyStorage;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.StoredItem;
import com.roguesmp.block.Tickable;
import com.roguesmp.block.data.CrusherData;
import com.roguesmp.block.event.SmpBlockBreakEvent;
import com.roguesmp.block.gui.CrusherGui;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.block.storage.FixedSlots;
import com.roguesmp.block.storage.ItemStorage;
import com.roguesmp.block.storage.ItemStorageHolder;
import com.roguesmp.block.storage.SlotAccess;
import com.roguesmp.block.upgrade.CatalystBurner;
import com.roguesmp.block.upgrade.UpgradeHost;
import com.roguesmp.block.upgrade.UpgradeInstallResult;
import com.roguesmp.block.upgrade.UpgradeSet;
import com.roguesmp.block.upgrade.UpgradeType;
import com.roguesmp.block.upgrade.effect.OutputContext;
import com.roguesmp.block.upgrade.stat.BlockStat;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.CraftingManager;
import com.roguesmp.crafting.recipe.Recipe;
import com.roguesmp.crafting.recipe.CraftingRecipes;
import com.roguesmp.crafting.recipe.CrusherRecipe;
import com.roguesmp.item.component.impl.BlockUpgradeComponent;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CrusherBlock extends SmpBlock implements Tickable, EnergyStorage, ItemStorageHolder, UpgradeHost {

    public static final int OUTPUT_COUNT = 6;
    private static final int GUI_REFRESH_INTERVAL = 10;
    private static final double MIN_SPEED = 0.01;
    private static final Set<BlockStat> STATS = EnumSet.of(BlockStat.ENERGY_PER_TICK, BlockStat.MAX_ENERGY,
            BlockStat.CRUSHER_SPEED, BlockStat.CRUSHER_OUTPUT_AMOUNT, BlockStat.CRUSHER_EXTRA_CHANCE);
    private static final MapCodec<Optional<Integer>> ENERGY_CODEC = Codec.INT.optionalFieldOf("energy");
    private static final MapCodec<Double> ENERGY_DEBT_CODEC = Codec.DOUBLE.optionalFieldOf("energy_debt", 0.0);
    private static final MapCodec<List<StoredItem>> PENDING_CODEC = Codec.listOf(StoredItem.CODEC).optionalFieldOf("pending_output", List.of());

    private final CrusherData data;

    private final ItemStorage storage = new ItemStorage(this);
    private final FixedSlots input = storage.add(new FixedSlots("input", "Nguyên liệu", 1, SlotAccess.INSERT, item -> isRecipeInput(item.item())), this::onInputChanged);
    private final FixedSlots outputs = storage.add(new FixedSlots("output", "Thành phẩm", OUTPUT_COUNT, SlotAccess.EXTRACT), this::refreshGui);
    private final FixedSlots catalystFeed = storage.add(new FixedSlots("catalyst", "Chất xúc tác", 1, SlotAccess.INSERT, item -> CatalystBurner.isValid(this, item.item())), this::refreshGui);
    private final CatalystBurner catalyst = new CatalystBurner(this, catalystFeed, this::markDirty);
    private final UpgradeSet upgrades = new UpgradeSet(this);
    private final FixedSlots modules;
    private @Nullable CrusherRecipe activeRecipe;
    private int progress;
    private int duration = 1;
    private List<StoredItem> pending = List.of();
    private int energy;
    private double energyDebt;
    private @Nullable CrusherGui gui;
    private int guiRefreshTicks;

    public CrusherBlock(BlockProperties properties, CrusherData data) {
        super(properties);
        this.data = data;
        this.modules = storage.add(new FixedSlots("modules", "Mô-đun", data.moduleSlots(), SlotAccess.NONE, item -> checkModule(item.item()) == UpgradeInstallResult.INSTALLED), this::onModulesChanged);
    }

    @Override
    public void onHydrated() {
        upgrades.rebuild();
        activeRecipe = matchRecipe();
        recalculateDuration();
        progress = activeRecipe == null ? 0 : Math.min(progress, duration);
    }

    @Override
    public void tick() {
        catalyst.tick(isRunning());
        tickProgress();
        tickGuiRefresh();
    }

    private void tickProgress() {
        if (!flushPending()) return;
        if (activeRecipe == null) return;

        if (progress < duration) {
            if (getEnergy() <= 0) return;

            consumeTickEnergy();
            progress++;
            markDirty();
            return;
        }

        complete(activeRecipe);
    }

    private boolean flushPending() {
        if (pending.isEmpty()) return true;
        if (!outputs.canFitAll(pending)) return false;

        for (StoredItem result : pending) outputs.insert(result, false);
        pending = List.of();
        markDirty();
        return true;
    }

    private void consumeTickEnergy() {
        energyDebt += energyPerTick();
        int cost = (int) energyDebt;
        energyDebt -= cost;
        removeEnergy(cost);
    }

    private void complete(CrusherRecipe recipe) {
        StoredItem consumed = input.extract(0, recipe.getInput().count(), false);

        List<StoredItem> produced = new ArrayList<>();
        for (CraftingIngredient result : recipe.getOutputs()) {
            StoredItem rolled = roll(new BlockDrop(result.key(), result.count(), result.count(), 1.0));
            if (rolled != null) produced.add(rolled);
        }
        for (BlockDrop drop : recipe.getExtra()) {
            StoredItem rolled = roll(drop);
            if (rolled != null) produced.add(rolled);
        }
        pending = List.copyOf(upgrades.output(new OutputContext(consumed, produced, this::roll)));
        catalyst.cycle();
        progress = 0;
        markDirty();
        flushPending();
    }

    private @Nullable StoredItem roll(BlockDrop drop) {
        double chance = drop.chance() < 1.0 ? upgrades.sheet().applyChance(BlockStat.CRUSHER_EXTRA_CHANCE, drop.chance()) : drop.chance();
        return drop.rollStored(chance, upgrades.sheet().multiplier(BlockStat.CRUSHER_OUTPUT_AMOUNT), upgrades.sheet().flatSum(BlockStat.CRUSHER_OUTPUT_AMOUNT));
    }

    private void dropAtCenter(StoredItem stored) {
        ItemStack item = stored.toItemStack();
        if (item == null) return;

        Location center = getLocation().toCenterLocation();
        center.getWorld().dropItemNaturally(center, item);
    }

    private @Nullable CrusherRecipe matchRecipe() {
        StoredItem stored = input.get(0);
        ItemStack stack = stored == null ? null : stored.toItemStack();
        if (stack == null) return null;

        return CraftingManager.getInstance().match(CraftingRecipes.CRUSHER, new ItemStack[]{stack},
                recipe -> recipe.getMinTier() <= data.tier() && recipe.matches(stack));
    }

    private boolean isRecipeInput(String itemId) {
        for (Recipe recipe : CraftingManager.getInstance().getRecipesUsing(itemId)) {
            if (recipe instanceof CrusherRecipe crusherRecipe && crusherRecipe.getMinTier() <= data.tier()) return true;
        }
        return false;
    }

    private void onInputChanged() {
        CrusherRecipe matched = matchRecipe();
        if (matched != activeRecipe) {
            activeRecipe = matched;
            progress = 0;
            recalculateDuration();
        }
        refreshGui();
    }

    private void tickGuiRefresh() {
        if (gui == null) return;
        if (++guiRefreshTicks < GUI_REFRESH_INTERVAL) return;

        guiRefreshTicks = 0;
        gui.refresh();
    }

    private void refreshGui() {
        if (gui != null) gui.refresh();
    }

    private void recalculateDuration() {
        duration = activeRecipe == null ? 1 : Math.max(1, (int) Math.ceil(activeRecipe.getDuration() / Math.max(MIN_SPEED, speed())));
    }

    public double speed() {
        return upgrades.sheet().apply(BlockStat.CRUSHER_SPEED, data.speed());
    }

    public int duration() {
        return duration;
    }

    public double energyPerTick() {
        return upgrades.sheet().apply(BlockStat.ENERGY_PER_TICK, data.energyPerTick());
    }

    public boolean isRunning() {
        return activeRecipe != null && pending.isEmpty() && getEnergy() > 0;
    }

    public CrusherData data() {
        return data;
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

    public boolean isStalled() {
        return !pending.isEmpty();
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
        catalyst.collectItems(items);
        return items;
    }

    @Override
    public void onUpgradesChanged() {
        energy = Math.min(energy, getMaxEnergy());
        catalyst.revalidate();
        recalculateDuration();
        if (activeRecipe != null) progress = Math.min(progress, duration);
        refreshGui();
    }

    public UpgradeInstallResult checkModule(String itemId) {
        UpgradeInstallResult result = BlockUpgradeComponent.check(itemId, this, UpgradeType.MODULE);
        return result == UpgradeInstallResult.INSTALLED ? upgrades.canAdd(itemId) : result;
    }

    public boolean isValidModule(String itemId) {
        return BlockUpgradeComponent.check(itemId, this, UpgradeType.MODULE) == UpgradeInstallResult.INSTALLED;
    }

    private void onModulesChanged() {
        upgrades.rebuild();
        refreshGui();
    }

    public FixedSlots input() {
        return input;
    }

    public FixedSlots outputs() {
        return outputs;
    }

    public FixedSlots modules() {
        return modules;
    }

    public CatalystBurner catalyst() {
        return catalyst;
    }

    public @Nullable CrusherRecipe activeRecipe() {
        return activeRecipe;
    }

    public int progress() {
        return progress;
    }

    @Override
    public ItemStorage itemStorage() {
        return storage;
    }

    @Override
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        event.setCancelled(true);
        openGui(event.getPlayer());
    }

    public void openGui(HumanEntity viewer) {
        if (gui == null) gui = new CrusherGui(this);
        gui.showInventory(viewer);
    }

    public void releaseGui(CrusherGui closed) {
        if (gui == closed) gui = null;
    }

    @Override
    public void onBlockBreak(SmpBlockBreakEvent event) {
        if (gui != null) gui.closeAll();
        pending.forEach(this::dropAtCenter);
        pending = List.of();
        activeRecipe = null;
        progress = 0;
    }

    @Override
    protected void collectSections(List<StateSection<?>> sections) {
        super.collectSections(sections);
        sections.add(StateSection.of(Codec.INT.optionalFieldOf("progress", 0), () -> progress, value -> progress = value));
        sections.add(StateSection.of(ENERGY_CODEC, () -> Optional.of(energy), value -> energy = value.orElse(0)));
        sections.add(StateSection.of(ENERGY_DEBT_CODEC, () -> energyDebt, value -> energyDebt = value));
        sections.add(StateSection.of(PENDING_CODEC, () -> pending, value -> pending = List.copyOf(value)));
        sections.add(catalyst.section());
    }
}
