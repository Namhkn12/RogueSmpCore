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
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PotionContents;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class BrewingBlock extends SmpBlock implements Tickable, EnergyStorage, ItemStorageHolder {

    public static final int INGREDIENT_COUNT = 6;
    public static final int BOTTLE_COUNT = 3;
    private static final Codec<Map<String, Bottle>> BOTTLES_CODEC = Codec.unboundedMap(Bottle.CODEC);
    private static final int GUI_REFRESH_INTERVAL = 10;

    private final BrewingData data;
    private final ItemStorage storage = new ItemStorage(this);
    private final FixedSlots ingredients = storage.add(new FixedSlots("ingredient", "Nguyên liệu", INGREDIENT_COUNT, SlotAccess.INSERT,
            item -> BrewIngredientComponent.of(item.item()) != null), this::onInputsChanged);
    private final Bottle[] bottles = new Bottle[BOTTLE_COUNT];
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
        BrewRecipe brewed = new BrewRecipe(used, Utils.RANDOM.nextLong());
        for (int i = 0; i < BOTTLE_COUNT; i++) {
            if (bottles[i] != null && bottles[i].isWater()) bottles[i] = new Bottle(brewed);
        }
        recipe = currentRecipe();
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
        BrewedPotionComponent potion = ItemRef.component(data.potionItem(), ItemComponentKeys.BREWED_POTION);
        if (potion == null) return BrewStatus.INVALID_POTION;
        if (Arrays.stream(bottles).noneMatch(bottle -> bottle != null && bottle.isWater())) {
            return Arrays.stream(bottles).anyMatch(Objects::nonNull) ? BrewStatus.OUTPUT_FULL : BrewStatus.NO_BOTTLE;
        }

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
        for (Bottle bottle : bottles) ids.add(bottle != null && bottle.isWater() ? "water" : "");
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

    public @Nullable ItemStack bottleStack(int index) {
        Bottle bottle = bottles[index];
        if (bottle == null) return null;
        if (bottle.isWater()) return waterBottle();

        BaseItem potion = Registries.ITEM.get(data.potionItem());
        if (potion == null) return null;

        SmpItem item = new SmpItem(potion);
        BrewedPotionComponent component = item.getComponent(ItemComponentKeys.BREWED_POTION);
        if (component == null) return null;

        component.setRecipe(bottle.potion());
        return item.generateItemStack(1);
    }

    public @Nullable ItemStack takeBottle(int index) {
        ItemStack stack = bottleStack(index);
        if (stack == null) return null;

        bottles[index] = null;
        onInputsChanged();
        markDirty();
        return stack;
    }

    public boolean insertWater(int index) {
        if (bottles[index] != null) return false;

        bottles[index] = Bottle.WATER;
        onInputsChanged();
        markDirty();
        return true;
    }

    public static boolean isWaterBottle(@Nullable ItemStack stack) {
        if (stack == null || stack.getType() != Material.POTION || ItemRef.baseItem(ItemRef.idOf(stack)) != null) return false;
        PotionContents contents = stack.getData(DataComponentTypes.POTION_CONTENTS);
        return contents != null && contents.potion() == PotionType.WATER && contents.customEffects().isEmpty();
    }

    private static ItemStack waterBottle() {
        ItemStack stack = ItemStack.of(Material.POTION);
        stack.setData(DataComponentTypes.POTION_CONTENTS, PotionContents.potionContents().potion(PotionType.WATER).build());
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

        for (int i = 0; i < BOTTLE_COUNT; i++) {
            ItemStack stack = bottleStack(i);
            if (stack != null) event.getDrops().add(stack);
            bottles[i] = null;
        }
        brewing = false;
        progress = 0;
    }

    @Override
    protected void collectSections(List<StateSection<?>> sections) {
        super.collectSections(sections);
        sections.add(StateSection.of(Codec.BOOLEAN.optionalFieldOf("brewing", false), () -> brewing, value -> brewing = value));
        sections.add(StateSection.of(Codec.INT.optionalFieldOf("progress", 0), () -> progress, value -> progress = value));
        sections.add(StateSection.of(Codec.INT.optionalFieldOf("energy", 0), () -> energy, value -> energy = value));
        sections.add(StateSection.of(BOTTLES_CODEC.optionalFieldOf("bottles", Map.of()), this::encodeBottles, this::decodeBottles));
    }

    private Map<String, Bottle> encodeBottles() {
        Map<String, Bottle> encoded = new LinkedHashMap<>();
        for (int i = 0; i < BOTTLE_COUNT; i++) {
            if (bottles[i] != null) encoded.put(String.valueOf(i), bottles[i]);
        }
        return encoded;
    }

    private void decodeBottles(Map<String, Bottle> encoded) {
        Arrays.fill(bottles, null);
        encoded.forEach((key, bottle) -> {
            int index = Integer.parseInt(key);
            if (index >= 0 && index < BOTTLE_COUNT) bottles[index] = bottle;
        });
    }

    public record Bottle(@Nullable BrewRecipe potion) {

        public static final Bottle WATER = new Bottle(null);
        public static final Codec<Bottle> CODEC = Codec.composite(
                BrewRecipe.CODEC.optionalFieldOf("potion").forGetter(bottle -> Optional.ofNullable(bottle.potion())),
                potion -> new Bottle(potion.orElse(null))
        );

        public boolean isWater() {
            return potion == null;
        }
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
