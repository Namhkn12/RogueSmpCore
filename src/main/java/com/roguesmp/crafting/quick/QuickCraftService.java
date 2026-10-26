package com.roguesmp.crafting.quick;

import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.CraftingManager;
import com.roguesmp.crafting.recipe.CraftingRecipe;
import com.roguesmp.crafting.recipe.ShapedCraftingRecipe;
import com.roguesmp.crafting.recipe.ShapelessCraftingRecipe;
import com.roguesmp.utils.ItemRef;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class QuickCraftService {

    private QuickCraftService() {}

    public static @NotNull List<QuickCraftCandidate> findFirst(@NotNull Player player, int limit) {
        return candidates(player).limit(limit).toList();
    }

    public static @NotNull List<QuickCraftCandidate> findAll(@NotNull Player player) {
        return candidates(player).toList();
    }

    private static Stream<QuickCraftCandidate> candidates(Player player) {
        Map<String, Integer> have = countInventory(player);
        Set<String> seen = new HashSet<>();
        return have.keySet().stream()
                .flatMap(key -> CraftingManager.getInstance().getRecipesUsing(key).stream())
                .filter(QuickCraftService::isQuickCraftable)
                .filter(recipe -> seen.add(recipe.getId()))
                .map(recipe -> evaluate(recipe, have))
                .filter(candidate -> candidate.maxCrafts() > 0);
    }

    private static boolean isQuickCraftable(CraftingRecipe recipe) {
        return recipe instanceof ShapedCraftingRecipe || recipe instanceof ShapelessCraftingRecipe;
    }

    private static QuickCraftCandidate evaluate(CraftingRecipe recipe, Map<String, Integer> have) {
        Map<String, Integer> need = recipe.getInputTotals();
        List<QuickCraftCandidate.IngredientStatus> ingredients = new ArrayList<>(need.size());
        int crafts = need.isEmpty() ? 0 : Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> entry : need.entrySet()) {
            int owned = have.getOrDefault(entry.getKey(), 0);
            ingredients.add(new QuickCraftCandidate.IngredientStatus(entry.getKey(), owned, entry.getValue()));
            crafts = Math.min(crafts, owned / entry.getValue());
        }
        return new QuickCraftCandidate(recipe, ingredients, crafts);
    }

    private static Map<String, Integer> countInventory(Player player) {
        return CraftingIngredient.aggregateCounts(player.getInventory().getStorageContents());
    }

    public static void craft(@NotNull Player player, @NotNull CraftingRecipe recipe, boolean craftAll) {
        int available = evaluate(recipe, countInventory(player)).maxCrafts();
        if (available <= 0) {
            deny(player, "Không đủ nguyên liệu!");
            return;
        }

        ItemStack output = recipe.getPrimaryOutputStack();
        if (output == null) {
            deny(player, "Công thức không hợp lệ!");
            return;
        }

        int times = craftAll ? Math.min(available, craftsThatFit(player, output)) : 1;
        if (times <= 0) {
            deny(player, "Túi đồ của bạn đã đầy!");
            return;
        }

        ItemStack[] contents = player.getInventory().getStorageContents();
        recipe.getInputTotals().forEach((key, amount) -> take(contents, key, amount * times));
        player.getInventory().setStorageContents(contents);

        for (int i = 0; i < times; i++) {
            PlayerUtils.giveItem(player, recipe.getOutputStacks());
        }
    }

    private static int craftsThatFit(Player player, ItemStack output) {
        int maxStackSize = output.getMaxStackSize();
        int capacity = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType().isAir()) capacity += maxStackSize;
            else if (stack.isSimilar(output)) capacity += Math.max(0, maxStackSize - stack.getAmount());
        }
        return capacity / output.getAmount();
    }

    private static void deny(Player player, String reason) {
        player.sendMessage(Utils.text(reason, NamedTextColor.RED));
        player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    private static void take(ItemStack[] contents, String key, int amount) {
        for (int i = 0; i < contents.length && amount > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || !key.equals(ItemRef.idOf(stack))) continue;
            int taken = Math.min(amount, stack.getAmount());
            stack.setAmount(stack.getAmount() - taken);
            amount -= taken;
        }
    }
}
