package com.roguesmp.block.impl.cokeoven;

import com.roguesmp.block.BlockProperties;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.StoredItem;
import com.roguesmp.block.Tickable;
import com.roguesmp.block.data.CokeOvenData;
import com.roguesmp.block.event.SmpBlockBreakEvent;
import com.roguesmp.block.gui.CokeOvenGui;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.block.storage.FixedSlots;
import com.roguesmp.block.storage.ItemStorage;
import com.roguesmp.block.storage.ItemStorageHolder;
import com.roguesmp.block.storage.SlotAccess;
import com.roguesmp.codec.Codec;
import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.CraftingManager;
import com.roguesmp.crafting.recipe.CokeOvenRecipe;
import com.roguesmp.crafting.recipe.CraftingRecipes;
import com.roguesmp.registry.Registries;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CokeOvenBlock extends SmpBlock implements Tickable, ItemStorageHolder {

    public static final int INPUT_COUNT = 2;
    public static final int OUTPUT_COUNT = 2;
    private static final int GUI_REFRESH_INTERVAL = 10;

    private final CokeOvenData data;
    private final ItemStorage storage = new ItemStorage(this);
    private final FixedSlots inputs = storage.add(new FixedSlots("input", "Nguyên liệu", INPUT_COUNT, SlotAccess.INSERT), this::onInputsChanged);
    private final FixedSlots outputs = storage.add(new FixedSlots("output", "Thành phẩm", OUTPUT_COUNT, SlotAccess.EXTRACT), this::refreshGui);
    private @Nullable CokeOvenRecipe activeRecipe;
    private @Nullable String rememberedRecipeId;
    private int progress;
    private @Nullable CokeOvenGui gui;
    private int guiRefreshTicks;

    public CokeOvenBlock(BlockProperties properties, CokeOvenData data) {
        super(properties);
        this.data = data;
        inputs.withDemand(this::recipeDemand);
    }

    @Override
    public void onHydrated() {
        activeRecipe = matchRecipe();
        progress = activeRecipe == null ? 0 : Math.min(progress, activeRecipe.getDuration());
    }

    @Override
    public void tick() {
        tickProgress();
        syncDisplayModel();
        tickGuiRefresh();
    }

    private void tickProgress() {
        if (activeRecipe == null) return;

        if (progress < activeRecipe.getDuration()) {
            progress++;
            markDirty();
            return;
        }

        if (!outputs.canFitAll(results(activeRecipe))) return;
        complete(activeRecipe);
    }

    private void complete(CokeOvenRecipe recipe) {
        consumeIngredients(recipe);
        for (StoredItem result : results(recipe)) outputs.insert(result, false);
        progress = 0;
        markDirty();
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

    private @Nullable CokeOvenRecipe matchRecipe() {
        ItemStack[] stacks = new ItemStack[INPUT_COUNT];
        boolean empty = true;
        for (int i = 0; i < INPUT_COUNT; i++) {
            StoredItem stored = inputs.get(i);
            stacks[i] = stored == null ? null : stored.toItemStack();
            if (stacks[i] != null) empty = false;
        }
        return empty ? null : CraftingManager.getInstance().match(CraftingRecipes.COKE_OVEN, stacks, 0, 0);
    }

    private void onInputsChanged() {
        CokeOvenRecipe matched = matchRecipe();
        if (matched != activeRecipe) {
            activeRecipe = matched;
            progress = 0;
        }
        if (matched != null) rememberedRecipeId = matched.getId();
        refreshGui();
    }

    private void consumeIngredients(CokeOvenRecipe recipe) {
        recipe.getRequiredCounts().forEach((key, required) -> {
            int remaining = required;
            for (int i = 0; i < INPUT_COUNT && remaining > 0; i++) {
                StoredItem stored = inputs.get(i);
                if (stored == null || !stored.item().equals(key)) continue;

                StoredItem taken = inputs.extract(i, remaining, false);
                if (taken != null) remaining -= taken.amount();
            }
        });
    }

    private static List<StoredItem> results(CokeOvenRecipe recipe) {
        List<StoredItem> results = new ArrayList<>(recipe.getOutputs().size());
        for (CraftingIngredient result : recipe.getOutputs()) results.add(new StoredItem(result.key(), result.count()));
        return results;
    }

    private @Nullable List<StoredItem> recipeDemand() {
        CokeOvenRecipe recipe = rememberedRecipe();
        if (recipe == null) return null;

        Map<String, Integer> required = recipe.getRequiredCounts();
        int slotsPerIngredient = Math.max(1, INPUT_COUNT / required.size());
        int batches = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            int maxStack = new StoredItem(entry.getKey(), 1).maxStackSize();
            batches = Math.min(batches, maxStack * slotsPerIngredient / entry.getValue());
        }

        List<StoredItem> demand = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            int missing = batches * entry.getValue() - heldInputs(entry.getKey());
            if (missing > 0) demand.add(new StoredItem(entry.getKey(), missing));
        }
        return demand;
    }

    private int heldInputs(String item) {
        int held = 0;
        for (int i = 0; i < INPUT_COUNT; i++) {
            StoredItem stored = inputs.get(i);
            if (stored != null && stored.item().equals(item)) held += stored.amount();
        }
        return held;
    }

    public @Nullable CokeOvenRecipe rememberedRecipe() {
        if (rememberedRecipeId == null) return null;
        return Registries.CRAFTING_RECIPE.get(rememberedRecipeId) instanceof CokeOvenRecipe recipe ? recipe : null;
    }

    public void forgetRecipe() {
        rememberedRecipeId = null;
        markDirty();
        refreshGui();
    }

    public FixedSlots inputs() {
        return inputs;
    }

    public FixedSlots outputs() {
        return outputs;
    }

    public @Nullable CokeOvenRecipe activeRecipe() {
        return activeRecipe;
    }

    public int progress() {
        return progress;
    }

    public boolean isStalled() {
        return activeRecipe != null && progress >= activeRecipe.getDuration() && !outputs.canFitAll(results(activeRecipe));
    }

    @Override
    public Key getDisplayModel() {
        return activeRecipe != null && !isStalled() ? data.modelActive() : super.getDisplayModel();
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
        if (gui == null) gui = new CokeOvenGui(this);
        gui.showInventory(viewer);
    }

    public void releaseGui(CokeOvenGui closed) {
        if (gui == closed) gui = null;
    }

    @Override
    public void onBlockBreak(SmpBlockBreakEvent event) {
        if (gui != null) gui.closeAll();
        activeRecipe = null;
        progress = 0;
    }

    @Override
    protected void collectSections(List<StateSection<?>> sections) {
        super.collectSections(sections);
        sections.add(StateSection.of(Codec.INT.optionalFieldOf("progress", 0), () -> progress, value -> progress = value));
        sections.add(StateSection.of(Codec.STRING.optionalFieldOf("remembered_recipe"), () -> Optional.ofNullable(rememberedRecipeId), value -> rememberedRecipeId = value.orElse(null)));
    }
}
