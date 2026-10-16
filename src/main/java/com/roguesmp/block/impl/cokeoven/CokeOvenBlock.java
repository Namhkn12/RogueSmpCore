package com.roguesmp.block.impl.cokeoven;

import com.roguesmp.block.BlockProperties;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.StoredItem;
import com.roguesmp.block.Tickable;
import com.roguesmp.block.event.SmpBlockBreakEvent;
import com.roguesmp.block.gui.CokeOvenGui;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.codec.Codec;
import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.CraftingManager;
import com.roguesmp.crafting.recipe.CokeOvenRecipe;
import com.roguesmp.crafting.recipe.CraftingRecipes;
import org.bukkit.Location;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class CokeOvenBlock extends SmpBlock implements Tickable {

    public static final int INPUT_COUNT = 2;
    public static final int OUTPUT_COUNT = 2;
    private static final int GUI_REFRESH_INTERVAL = 10;

    private final StoredItem[] inputs = new StoredItem[INPUT_COUNT];
    private final StoredItem[] outputs = new StoredItem[OUTPUT_COUNT];
    private @Nullable CokeOvenRecipe activeRecipe;
    private int progress;
    private @Nullable CokeOvenGui gui;
    private int guiRefreshTicks;

    public CokeOvenBlock(BlockProperties properties) {
        super(properties);
    }

    @Override
    public void onHydrated() {
        activeRecipe = matchRecipe();
        progress = activeRecipe == null ? 0 : Math.min(progress, activeRecipe.getDuration());
    }

    @Override
    public void tick() {
        tickProgress();
        tickGuiRefresh();
    }

    private void tickProgress() {
        if (activeRecipe == null) return;

        if (progress < activeRecipe.getDuration()) {
            progress++;
            markDirty();
            return;
        }

        if (!canFitResults(activeRecipe)) return;
        complete(activeRecipe);
    }

    private void complete(CokeOvenRecipe recipe) {
        consumeIngredients(recipe);
        for (CraftingIngredient result : recipe.getResults()) storeOutput(new StoredItem(result.key(), result.count()), outputs);
        progress = 0;
        activeRecipe = matchRecipe();
        markDirty();
        if (gui != null) gui.refresh();
    }

    private void tickGuiRefresh() {
        if (gui == null) return;
        if (++guiRefreshTicks < GUI_REFRESH_INTERVAL) return;

        guiRefreshTicks = 0;
        gui.refresh();
    }

    private @Nullable CokeOvenRecipe matchRecipe() {
        ItemStack[] stacks = new ItemStack[INPUT_COUNT];
        boolean empty = true;
        for (int i = 0; i < INPUT_COUNT; i++) {
            stacks[i] = inputs[i] == null ? null : inputs[i].toItemStack();
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
        markDirty();
        if (gui != null) gui.refresh();
    }

    private void consumeIngredients(CokeOvenRecipe recipe) {
        recipe.getRequiredCounts().forEach((key, required) -> {
            int remaining = required;
            for (int i = 0; i < INPUT_COUNT && remaining > 0; i++) {
                StoredItem stored = inputs[i];
                if (stored == null || !stored.item().equals(key)) continue;

                int taken = Math.min(remaining, stored.amount());
                remaining -= taken;
                inputs[i] = stored.amount() > taken ? stored.withAmount(stored.amount() - taken) : null;
            }
        });
    }

    private boolean canFitResults(CokeOvenRecipe recipe) {
        StoredItem[] simulated = outputs.clone();
        for (CraftingIngredient result : recipe.getResults()) {
            if (storeOutput(new StoredItem(result.key(), result.count()), simulated) > 0) return false;
        }
        return true;
    }

    private static int storeOutput(StoredItem item, StoredItem[] slots) {
        int remaining = item.amount();
        int maxStack = item.maxStackSize();

        for (int i = 0; i < slots.length && remaining > 0; i++) {
            StoredItem existing = slots[i];
            if (existing == null || !existing.item().equals(item.item())) continue;

            int added = Math.min(remaining, maxStack - existing.amount());
            if (added <= 0) continue;
            slots[i] = existing.withAmount(existing.amount() + added);
            remaining -= added;
        }

        for (int i = 0; i < slots.length && remaining > 0; i++) {
            if (slots[i] != null) continue;

            int added = Math.min(remaining, maxStack);
            slots[i] = item.withAmount(added);
            remaining -= added;
        }
        return remaining;
    }

    /** Puts as much of {@code stack} as fits into input {@code index}, returning how many items were accepted. */
    public int insertInput(int index, ItemStack stack) {
        StoredItem incoming = StoredItem.of(stack);
        if (incoming == null) return 0;

        StoredItem existing = inputs[index];
        if (existing != null && !existing.item().equals(incoming.item())) return 0;

        int current = existing == null ? 0 : existing.amount();
        int accepted = Math.min(incoming.amount(), incoming.maxStackSize() - current);
        if (accepted <= 0) return 0;

        inputs[index] = incoming.withAmount(current + accepted);
        onInputsChanged();
        return accepted;
    }

    /** Spreads {@code stack} over every input slot that can take it, returning how many items were accepted. */
    public int insertAnyInput(ItemStack stack) {
        StoredItem incoming = StoredItem.of(stack);
        if (incoming == null) return 0;

        int remaining = incoming.amount();
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int i = 0; i < INPUT_COUNT && remaining > 0; i++) {
                StoredItem existing = inputs[i];
                boolean matching = existing != null && existing.item().equals(incoming.item());
                if (pass == 0 ? !matching : existing != null) continue;

                int current = existing == null ? 0 : existing.amount();
                int accepted = Math.min(remaining, incoming.maxStackSize() - current);
                if (accepted <= 0) continue;
                inputs[i] = incoming.withAmount(current + accepted);
                remaining -= accepted;
            }
        }

        int accepted = incoming.amount() - remaining;
        if (accepted > 0) onInputsChanged();
        return accepted;
    }

    public @Nullable StoredItem takeInput(int index, int amount) {
        StoredItem taken = take(inputs, index, amount);
        if (taken != null) onInputsChanged();
        return taken;
    }

    public @Nullable StoredItem takeOutput(int index, int amount) {
        StoredItem taken = take(outputs, index, amount);
        if (taken != null) {
            markDirty();
            if (gui != null) gui.refresh();
        }
        return taken;
    }

    private static @Nullable StoredItem take(StoredItem[] slots, int index, int amount) {
        StoredItem stored = slots[index];
        if (stored == null || amount <= 0) return null;

        int taken = Math.min(amount, stored.amount());
        slots[index] = stored.amount() > taken ? stored.withAmount(stored.amount() - taken) : null;
        return stored.withAmount(taken);
    }

    public @Nullable StoredItem input(int index) {
        return inputs[index];
    }

    public @Nullable StoredItem output(int index) {
        return outputs[index];
    }

    public @Nullable CokeOvenRecipe activeRecipe() {
        return activeRecipe;
    }

    public int progress() {
        return progress;
    }

    public boolean isStalled() {
        return activeRecipe != null && progress >= activeRecipe.getDuration() && !canFitResults(activeRecipe);
    }

    @Override
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        event.setCancelled(true);
        if (gui == null) gui = new CokeOvenGui(this);
        gui.showInventory(event.getPlayer());
    }

    public void releaseGui(CokeOvenGui closed) {
        if (gui == closed) gui = null;
    }

    @Override
    public void onBlockBreak(SmpBlockBreakEvent event) {
        if (gui != null) gui.closeAll();
        drain(inputs);
        drain(outputs);
        activeRecipe = null;
        progress = 0;
    }

    private void drain(StoredItem[] slots) {
        Location center = getLocation().toCenterLocation();
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = slots[i] == null ? null : slots[i].toItemStack();
            slots[i] = null;
            if (stack != null) center.getWorld().dropItemNaturally(center, stack);
        }
    }

    @Override
    protected void collectSections(List<StateSection<?>> sections) {
        super.collectSections(sections);
        addSlotSections(sections, "input_", inputs);
        addSlotSections(sections, "output_", outputs);
        sections.add(StateSection.of(Codec.INT.optionalFieldOf("progress", 0), () -> progress, value -> progress = value));
    }

    private static void addSlotSections(List<StateSection<?>> sections, String prefix, StoredItem[] slots) {
        for (int i = 0; i < slots.length; i++) {
            int index = i;
            sections.add(StateSection.of(StoredItem.CODEC.optionalFieldOf(prefix + i), () -> Optional.ofNullable(slots[index]), value -> slots[index] = value.orElse(null)));
        }
    }
}
