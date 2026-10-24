package com.roguesmp.crafting.gui;

import com.roguesmp.utils.ItemRef;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.crafting.CraftingManager;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecipeSearchGui extends BaseGui {

    private static final int FIRST_RESULT_ROW = 1;
    private static final int FIRST_RESULT_COL = 1;
    private static final int RESULT_COLS = 7;
    private static final int PAGE_SIZE = RESULT_COLS * 4;
    private static final int BOTTOM_ROW = 5;
    private static final int PREV_PAGE_COL = 3;
    private static final int SEARCH_COL = 4;
    private static final int NEXT_PAGE_COL = 5;

    private final Player player;
    private final String query;
    private final List<String> results;
    private int page = 0;

    public RecipeSearchGui(Player player, @Nullable String query) {
        super(Component.text(query == null || query.isBlank() ? "Tìm công thức" : "Tìm công thức: " + query.trim()), BOTTOM_ROW + 1);
        this.player = player;
        this.query = query == null ? "" : query.trim();
        this.results = search(this.query);
    }

    public static void registerCommand() {
        new CommandAPICommand("recipes")
                .withArguments(new GreedyStringArgument("query"))
                .executesPlayer((player, args) -> {
                    new RecipeSearchGui(player, (String) args.get("query")).showInventory(player);
                })
                .register(RogueSmpCore.getInstance());
    }

    private static List<String> search(String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        List<String> found = new ArrayList<>();
        for (String key : CraftingManager.getInstance().getCraftableItemKeys()) {
            if (key.toLowerCase(Locale.ROOT).contains(needle)) found.add(key);
        }
        found.sort(String.CASE_INSENSITIVE_ORDER);
        return found;
    }

    @Override
    public void setup() {
        clearUi();
        for (int col = 0; col < 9; col++) {
            addButton(0, col, FILLER_BLACK, ClickHandler.noAction());
            addButton(BOTTOM_ROW, col, FILLER_BLACK, ClickHandler.noAction());
        }
        for (int row = 1; row < BOTTOM_ROW; row++) {
            addButton(row, 0, FILLER_BLACK, ClickHandler.noAction());
            addButton(row, 8, FILLER_BLACK, ClickHandler.noAction());
        }

        addButton(BOTTOM_ROW, SEARCH_COL, searchButton(), event -> {
            event.setCancelled(true);
            openSearchDialog();
        });

        if (results.isEmpty()) {
            addButton(2, 4, noResultDisplay(), ClickHandler.noAction());
            fillEmpty(FILLER);
            return;
        }

        int pageCount = (results.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int from = page * PAGE_SIZE;
        for (int i = from; i < Math.min(from + PAGE_SIZE, results.size()); i++) {
            String key = results.get(i);
            int index = i - from;
            addButton(FIRST_RESULT_ROW + index / RESULT_COLS, FIRST_RESULT_COL + index % RESULT_COLS, resultDisplay(key), event -> {
                event.setCancelled(true);
                RecipeViewerGui.forItem(player, key, this).showInventory(player);
            });
        }

        if (pageCount > 1) {
            addButton(BOTTOM_ROW, PREV_PAGE_COL, PREV_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                page = (page - 1 + pageCount) % pageCount;
                setup();
            });
            addButton(BOTTOM_ROW, NEXT_PAGE_COL, NEXT_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                page = (page + 1) % pageCount;
                setup();
            });
        }

        fillEmpty(FILLER);
    }

    private ItemStack resultDisplay(String key) {
        ItemStack display = ItemRef.toItemStack(key, 1);
        if (display == null) display = ItemStack.of(Material.BARRIER);

        int recipeCount = CraftingManager.getInstance().getRecipesProducing(key).size();
        ItemLore existing = display.getData(DataComponentTypes.LORE);
        List<Component> lore = new ArrayList<>(existing == null ? List.of() : existing.lines());
        lore.add(Component.empty());
        lore.add(Utils.text(recipeCount + " công thức", NamedTextColor.GRAY));
        lore.add(Utils.text("Click để xem cách chế tạo.", NamedTextColor.GREEN));
        display.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return display;
    }

    private ItemStack searchButton() {
        ItemStack item = ItemStack.of(Material.COMPASS);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Tìm kiếm", NamedTextColor.GOLD));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Từ khóa: " + (query.isEmpty() ? "Trống" : query), NamedTextColor.YELLOW),
                Utils.text(results.size() + " vật phẩm", NamedTextColor.GRAY),
                Utils.text("Click để tìm vật phẩm khác.", NamedTextColor.GREEN)
        )));
        return item;
    }

    private ItemStack noResultDisplay() {
        ItemStack item = ItemStack.of(Material.BARRIER);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Không tìm thấy vật phẩm nào", NamedTextColor.RED));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Thử tìm theo ID khác.", NamedTextColor.GRAY)
        )));
        return item;
    }

    private void openSearchDialog() {
        Dialog dialog = DialogBuilder.create(Component.text("Tìm công thức"))
                .addTextInput("query", Component.text("ID vật phẩm"), b -> b.initial(query))
                .confirmation()
                .yesButton(Component.text("Tìm kiếm"), null, (response, audience) -> {
                    String text = response.getText("query");
                    if (text == null || text.isBlank()) {
                        player.sendMessage(Utils.text("Bạn cần nhập ID vật phẩm.", NamedTextColor.RED));
                        Utils.runLater(() -> showInventory(player));
                        return;
                    }
                    Utils.runLater(() -> new RecipeSearchGui(player, text).showInventory(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> showInventory(player)))
                .build();

        player.closeInventory();
        player.showDialog(dialog);
    }
}
