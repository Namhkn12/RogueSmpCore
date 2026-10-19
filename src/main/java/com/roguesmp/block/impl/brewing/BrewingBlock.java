package com.roguesmp.block.impl.brewing;

import com.roguesmp.block.BlockProperties;
import com.roguesmp.block.EnergyStorage;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.StoredItem;
import com.roguesmp.block.Tickable;
import com.roguesmp.block.data.BrewingData;
import com.roguesmp.block.event.SmpBlockBreakEvent;
import com.roguesmp.block.gui.BrewingGui;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.block.storage.FixedSlots;
import com.roguesmp.block.storage.ItemStorage;
import com.roguesmp.block.storage.ItemStorageHolder;
import com.roguesmp.block.storage.SlotAccess;
import com.roguesmp.brewing.BrewRecipe;
import com.roguesmp.brewing.Brewing;
import com.roguesmp.codec.Codec;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.BrewIngredientComponent;
import com.roguesmp.item.component.impl.BrewedPotionComponent;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.ItemRef;
import com.roguesmp.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BrewingBlock extends SmpBlock implements Tickable, EnergyStorage, ItemStorageHolder {

    public static final int INGREDIENT_COUNT = 6;
    private static final String GLASS_BOTTLE = ItemRef.vanillaId(Material.GLASS_BOTTLE);
    private static final int GUI_REFRESH_INTERVAL = 10;

    private final BrewingData data;
    private final ItemStorage storage = new ItemStorage(this);
    private final FixedSlots ingredients = storage.add(new FixedSlots("ingredient", "Nguyên liệu", INGREDIENT_COUNT, SlotAccess.INSERT,
            item -> BrewIngredientComponent.of(item.item()) != null), this::onInputsChanged);
    private final FixedSlots bottle = storage.add(new FixedSlots("bottle", "Bình thủy tinh", 1, SlotAccess.INSERT,
            item -> item.item().equals(GLASS_BOTTLE)), this::onInputsChanged);
    private @Nullable BrewRecipe output;
    private boolean brewing;
    private int progress;
    private int energy;
    private List<String> recipe = List.of();
    private @Nullable BrewingGui gui;
    private int guiRefreshTicks;

    public BrewingBlock(BlockProperties properties, BrewingData data) {
        super(properties);
        this.data = data;
    }

    @Override
    public void onHydrated() {
        recipe = currentRecipe();
        if (brewing && readiness() != BrewStatus.READY) stopBrewing();
        progress = brewing ? Math.min(progress, data.brewTicks()) : 0;
    }

    @Override
    public void tick() {
        tickBrewing();
        tickGuiRefresh();
    }

    private void tickBrewing() {
        if (status() != BrewStatus.BREWING) return;

        removeEnergy(data.energyPerTick());
        progress++;
        markDirty();
        if (progress >= data.brewTicks()) complete();
    }

    private void complete() {
        List<String> used = new ArrayList<>(INGREDIENT_COUNT);
        for (int i = 0; i < INGREDIENT_COUNT; i++) {
            StoredItem taken = ingredients.extract(i, 1, false);
            if (taken != null) used.add(taken.item());
        }
        bottle.extract(0, 1, false);

        output = new BrewRecipe(used, Utils.RANDOM.nextLong());
        stopBrewing();
        refreshGui();
    }

    public boolean startBrewing() {
        if (brewing || readiness() != BrewStatus.READY) return false;

        brewing = true;
        progress = 0;
        markDirty();
        refreshGui();
        return true;
    }

    public void cancelBrewing() {
        if (!brewing) return;

        stopBrewing();
        refreshGui();
    }

    private void stopBrewing() {
        brewing = false;
        progress = 0;
        markDirty();
    }

    public boolean isBrewing() {
        return brewing;
    }

    public BrewStatus status() {
        BrewStatus readiness = readiness();
        if (!brewing || readiness != BrewStatus.READY) return readiness;
        return energy < data.energyPerTick() ? BrewStatus.NO_ENERGY : BrewStatus.BREWING;
    }

    private BrewStatus readiness() {
        if (output != null) return BrewStatus.OUTPUT_FULL;
        BrewedPotionComponent potion = ItemRef.component(data.potionItem(), ItemComponentKeys.BREWED_POTION);
        if (potion == null) return BrewStatus.INVALID_POTION;
        if (bottle.get(0) == null) return BrewStatus.NO_BOTTLE;

        List<BrewIngredientComponent> components = ingredientComponents();
        if (components.isEmpty()) return BrewStatus.NO_INGREDIENTS;
        if (Brewing.durationSeconds(components, potion.getBaseDuration()) < potion.getMinDuration()) return BrewStatus.UNSTABLE;
        return BrewStatus.READY;
    }

    private List<BrewIngredientComponent> ingredientComponents() {
        List<BrewIngredientComponent> components = new ArrayList<>(INGREDIENT_COUNT);
        for (int i = 0; i < INGREDIENT_COUNT; i++) {
            StoredItem stored = ingredients.get(i);
            BrewIngredientComponent component = stored == null ? null : BrewIngredientComponent.of(stored.item());
            if (component != null) components.add(component);
        }
        return components;
    }

    private List<String> currentRecipe() {
        List<String> ids = new ArrayList<>(INGREDIENT_COUNT + 1);
        for (int i = 0; i < INGREDIENT_COUNT; i++) {
            StoredItem stored = ingredients.get(i);
            ids.add(stored == null ? "" : stored.item());
        }
        ids.add(bottle.get(0) == null ? "" : GLASS_BOTTLE);
        return ids;
    }

    private void onInputsChanged() {
        List<String> changed = currentRecipe();
        if (!changed.equals(recipe)) {
            recipe = changed;
            if (brewing) stopBrewing();
        }
        refreshGui();
    }

    public @Nullable ItemStack outputStack() {
        if (output == null) return null;

        BaseItem potion = Registries.ITEM.get(data.potionItem());
        if (potion == null) return null;

        SmpItem item = new SmpItem(potion);
        BrewedPotionComponent component = item.getComponent(ItemComponentKeys.BREWED_POTION);
        if (component == null) return null;

        component.setRecipe(output);
        return item.generateItemStack(1);
    }

    public @Nullable ItemStack takeOutput() {
        ItemStack stack = outputStack();
        if (stack == null) return null;

        output = null;
        markDirty();
        refreshGui();
        return stack;
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

    public FixedSlots ingredients() {
        return ingredients;
    }

    public FixedSlots bottle() {
        return bottle;
    }

    public int progress() {
        return progress;
    }

    public BrewingData data() {
        return data;
    }

    @Override
    public int getEnergy() {
        return energy;
    }

    @Override
    public int getMaxEnergy() {
        return data.maxEnergy();
    }

    @Override
    public int addEnergy(int amount) {
        int accepted = Math.min(amount, data.maxEnergy() - energy);
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
        if (gui == null) gui = new BrewingGui(this);
        gui.showInventory(viewer);
    }

    public void releaseGui(BrewingGui closed) {
        if (gui == closed) gui = null;
    }

    @Override
    public void onBlockBreak(SmpBlockBreakEvent event) {
        if (gui != null) gui.closeAll();

        ItemStack potion = outputStack();
        if (potion != null) event.getDrops().add(potion);
        output = null;
        brewing = false;
        progress = 0;
    }

    @Override
    protected void collectSections(List<StateSection<?>> sections) {
        super.collectSections(sections);
        sections.add(StateSection.of(Codec.BOOLEAN.optionalFieldOf("brewing", false), () -> brewing, value -> brewing = value));
        sections.add(StateSection.of(Codec.INT.optionalFieldOf("progress", 0), () -> progress, value -> progress = value));
        sections.add(StateSection.of(Codec.INT.optionalFieldOf("energy", 0), () -> energy, value -> energy = value));
        sections.add(StateSection.of(BrewRecipe.CODEC.optionalFieldOf("output"), () -> Optional.ofNullable(output), value -> output = value.orElse(null)));
    }

    public enum BrewStatus {
        READY,
        BREWING,
        INVALID_POTION,
        NO_BOTTLE,
        NO_INGREDIENTS,
        UNSTABLE,
        NO_ENERGY,
        OUTPUT_FULL
    }
}
