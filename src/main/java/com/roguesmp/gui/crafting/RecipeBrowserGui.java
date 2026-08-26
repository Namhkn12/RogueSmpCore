package com.roguesmp.gui.crafting;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.CraftingManager;
import com.roguesmp.crafting.recipe.CraftingRecipe;
import com.roguesmp.crafting.recipe.FusionRecipe;
import com.roguesmp.crafting.recipe.ShapedCraftingRecipe;
import com.roguesmp.crafting.recipe.ShapelessCraftingRecipe;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.gui.crafting.editor.FusionRecipeEditorGui;
import com.roguesmp.gui.crafting.editor.RecipeCreatorHubGui;
import com.roguesmp.gui.crafting.editor.ShapedShapelessRecipeEditorGui;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Single, unified read-only browser for every registered {@link CraftingRecipe} - replaces what
 * used to be two separate per-kind browsers with one paginated list (same shape as
 * {@code gui.ItemBrowser}), filtered in place by {@link #typeFilter} (cycled via
 * {@link #FILTER_SLOT}) and/or a substring id search (via {@link #SEARCH_SLOT}).
 * <p>
 * Left-click an entry to see its read-only breakdown ({@link ShapedShapelessRecipeEditorGui}/
 * {@link FusionRecipeEditorGui} in their locked {@code preview(...)} mode); right-click to jump
 * straight into editing it (same classes, via their {@code editExisting(...)} factory).
 */
public class RecipeBrowserGui extends BaseGui {

    private static final int PAGE_SIZE = 36;
    private static final int FILTER_SLOT = 7;
    private static final int SEARCH_SLOT = 8;
    private static final int GUIDE_SLOT = 4;

    private enum TypeFilter {
        ALL("Tất cả"), SHAPED_SHAPELESS("Shaped & Shapeless"), FUSION("Fusion");

        final String label;

        TypeFilter(String label) {
            this.label = label;
        }

        TypeFilter next() {
            TypeFilter[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private final Player player;
    private final List<CraftingRecipe> allEntries;
    private List<CraftingRecipe> filteredEntries;

    private TypeFilter typeFilter = TypeFilter.ALL;
    private String searchQuery = null;
    private int currentPage = 0;
    private int totalPages;

    public RecipeBrowserGui(Player player) {
        super(Component.text("Xem Công Thức"), 6);
        this.player = player;

        this.allEntries = new ArrayList<>(Registries.CRAFTING_RECIPE.getAll().values());
        allEntries.sort(Comparator.comparing(CraftingRecipe::getId));
        updateFilters();
    }

    @Override
    public void setup() {
        clearUi();
        for (int i = 0; i < 9; i++) {
            addButton(i, FILLER_BLACK, ClickHandler.noAction());
        }
        for (int i = 45; i < 54; i++) {
            addButton(i, FILLER_BLACK, ClickHandler.noAction());
        }
        fillEmpty(FILLER);

        addButton(FILTER_SLOT, filterButton(), event -> {
            event.setCancelled(true);
            typeFilter = typeFilter.next();
            updateFilters();
            setup();
        });

        addButton(SEARCH_SLOT, searchButton(), event -> {
            event.setCancelled(true);
            if (event.getClick().isShiftClick()) {
                searchQuery = null;
                updateFilters();
                setup();
                return;
            }
            openSearchDialog();
        });

        ItemStack guide = ItemStack.of(Material.BOOK);
        guide.setData(DataComponentTypes.ITEM_NAME, Component.text("Guide", NamedTextColor.GREEN));
        List<Component> guideLore = List.of(
                Utils.text("Chuột trái để xem công thức", NamedTextColor.GREEN),
                Utils.text("Chuột phải để edit công thức", NamedTextColor.GREEN)
        );
        guide.setData(DataComponentTypes.LORE, ItemLore.lore(guideLore));
        addButton(GUIDE_SLOT, guide, ClickHandler.noAction());

        if (totalPages > 1) {
            if (currentPage > 0) {
                addButton(5, 3, PREV_PAGE_BUTTON, event -> {
                    event.setCancelled(true);
                    currentPage--;
                    setup();
                });
            }
            if (currentPage < totalPages - 1) {
                addButton(5, 5, NEXT_PAGE_BUTTON, event -> {
                    event.setCancelled(true);
                    currentPage++;
                    setup();
                });
            }
        }

        int slot = 9;
        for (CraftingRecipe recipe : getPage()) {
            addButton(slot, previewButtonFor(recipe), event -> {
                event.setCancelled(true);
                if (event.getClick() == ClickType.RIGHT) {
                    openEditor(recipe);
                } else {
                    openDetail(recipe);
                }
            });
            slot++;
        }
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    private void updateFilters() {
        filteredEntries = new ArrayList<>();
        String query = (searchQuery == null || searchQuery.isBlank()) ? null : searchQuery.toLowerCase().trim();

        for (CraftingRecipe recipe : allEntries) {
            if (!matchesType(recipe)) continue;
            if (query != null && !recipe.getId().toLowerCase().contains(query)) continue;
            filteredEntries.add(recipe);
        }

        totalPages = Math.max(1, (int) Math.ceil(filteredEntries.size() / (double) PAGE_SIZE));
        currentPage = 0;
    }

    private boolean matchesType(CraftingRecipe recipe) {
        return switch (typeFilter) {
            case ALL -> true;
            case SHAPED_SHAPELESS -> recipe instanceof ShapedCraftingRecipe || recipe instanceof ShapelessCraftingRecipe;
            case FUSION -> recipe instanceof FusionRecipe;
        };
    }

    private List<CraftingRecipe> getPage() {
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, filteredEntries.size());
        if (from >= filteredEntries.size()) return Collections.emptyList();
        return filteredEntries.subList(from, to);
    }

    private void openDetail(CraftingRecipe recipe) {
        if (recipe instanceof FusionRecipe fusion) {
            FusionRecipeEditorGui.preview(player, fusion).showInventory(player);
        } else {
            ShapedShapelessRecipeEditorGui.preview(player, recipe).showInventory(player);
        }
    }

    private void openEditor(CraftingRecipe recipe) {
        if (recipe instanceof FusionRecipe fusion) {
            FusionRecipeEditorGui gui = FusionRecipeEditorGui.editExisting(player, fusion);
            gui.setRefundItem(false);
            gui.showInventory(player);
        } else {
            ShapedShapelessRecipeEditorGui gui = ShapedShapelessRecipeEditorGui.editExisting(player, recipe);
            gui.setRefundItem(false);
            gui.showInventory(player);
        }
    }

    private ItemStack filterButton() {
        ItemStack item = ItemStack.of(Material.HOPPER);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Lọc: " + typeFilter.label, NamedTextColor.GOLD));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Click để chuyển loại công thức hiển thị.", NamedTextColor.GRAY)
        )));
        return item;
    }

    private ItemStack searchButton() {
        ItemStack item = ItemStack.of(Material.COMPASS);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Tìm kiếm theo ID", NamedTextColor.GOLD));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Từ khóa hiện tại: " + (searchQuery == null || searchQuery.isBlank() ? "Trống" : searchQuery), NamedTextColor.YELLOW),
                Utils.text("Click để đổi từ khóa.", NamedTextColor.GRAY),
                Utils.text("Shift-Click để xoá bộ lọc.", NamedTextColor.RED)
        )));
        return item;
    }

    private void openSearchDialog() {
        Dialog dialog = DialogBuilder.create(Component.text("Tìm kiếm công thức"))
                .addTextInput("value", Component.text("Nhập ID hoặc một phần ID..."), b -> b.initial(searchQuery == null ? "" : searchQuery))
                .confirmation()
                .yesButton(Component.text("Tìm kiếm"), null, (response, audience) -> {
                    searchQuery = response.getText("value");
                    updateFilters();
                    Utils.runLater(() -> showInventory(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> showInventory(player)))
                .build();

        player.closeInventory();
        player.showDialog(dialog);
    }

    private ItemStack previewButtonFor(CraftingRecipe recipe) {
        ItemStack result = resolvePreview(recipe.getResult());

        List<Component> lore = new ArrayList<>();
        ItemLore existingLore = result.getData(DataComponentTypes.LORE);
        if (existingLore != null) lore.addAll(existingLore.lines());

        lore.add(Component.empty());
        lore.add(Utils.text("ID: " + recipe.getId(), NamedTextColor.DARK_GRAY));
        lore.add(Utils.text("Loại: " + typeLabel(recipe), NamedTextColor.GRAY));
        lore.add(Utils.text("Trái-click để xem chi tiết.", NamedTextColor.GREEN));
        lore.add(Utils.text("Phải-click để chỉnh sửa.", NamedTextColor.YELLOW));

        result.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return result;
    }

    private static String typeLabel(CraftingRecipe recipe) {
        if (recipe instanceof ShapedCraftingRecipe) return "Shaped";
        if (recipe instanceof ShapelessCraftingRecipe) return "Shapeless";
        if (recipe instanceof FusionRecipe) return "Fusion";
        return "Unknown";
    }

    private static ItemStack resolvePreview(@Nullable CraftingIngredient ingredient) {
        if (ingredient != null) {
            ItemStack resolved = ingredient.toItemStack();
            if (resolved != null) return resolved.clone();
        }

        ItemStack placeholder = ItemStack.of(Material.BARRIER);
        placeholder.setData(DataComponentTypes.ITEM_NAME, Component.text(
                ingredient == null ? "(không có)" : "Không tìm thấy: " + ingredient.key(), NamedTextColor.RED));
        return placeholder;
    }

    public static void registerCommand() {
        new CommandAPICommand("smprecipe")
                .executesPlayer((player, args) -> {
                    new RecipeBrowserGui(player).showInventory(player);
                })
                .withSubcommand(new CommandAPICommand("view")
                        .executesPlayer((sender, args) -> {
                            new RecipeBrowserGui(sender).showInventory(sender);
                        }))
                .withSubcommand(new CommandAPICommand("new")
                        .executesPlayer((sender, args) -> {
                            new RecipeCreatorHubGui(sender).showInventory(sender);
                        }))
                .withSubcommand(new CommandAPICommand("delete")
                        .withArguments(new StringArgument("recipe_id").replaceSuggestions(ArgumentSuggestions.strings(Registries.CRAFTING_RECIPE.getAll().keySet())))
                        .executesPlayer((sender, args) -> {
                            String id = (String) args.get("recipe_id");
                            if (Registries.CRAFTING_RECIPE.get(id) == null) {
                                sender.sendMessage(Component.text("No recipe exist: " + id, NamedTextColor.RED));
                                return;
                            }
                            Registries.CRAFTING_RECIPE.removeAndDeleteFiles(RogueSmpCore.getInstance(), id);
                            CraftingManager.getInstance().rebuild();
                            sender.sendMessage(Component.text("Removed recipe: " + id, NamedTextColor.RED));
                        }))
                .register();
    }
}
