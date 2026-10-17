package com.roguesmp.crafting.gui;

import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.CraftingManager;
import com.roguesmp.crafting.gui.category.RecipeCategories;
import com.roguesmp.crafting.gui.category.RecipeCategory;
import com.roguesmp.crafting.gui.category.RecipeLayout;
import com.roguesmp.crafting.recipe.CraftingRecipe;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RecipeViewerGui extends BaseGui {

    private enum Mode {
        RECIPES, USES, CATEGORY
    }

    private record View(Mode mode, @Nullable String focusKey, @Nullable RecipeCategory<?> category, int tab, int page) {}

    private static final int MAX_HISTORY = 50;
    private static final int CONTENT_FIRST_ROW = 1;
    private static final int BOTTOM_ROW = CONTENT_FIRST_ROW + RecipeLayout.ROWS;
    private static final int BACK_SLOT = 0;
    private static final int FIRST_TAB_SLOT = 1;
    private static final int MAX_TABS = 8;
    private static final int MAX_CATALYSTS = 5;
    private static final int PREV_PAGE_COL = 6;
    private static final int PAGE_INFO_COL = 7;
    private static final int NEXT_PAGE_COL = 8;
    private static final ItemStack CONTENT_FILLER = createDecoration(Material.GRAY_STAINED_GLASS_PANE);

    private final Player player;
    private final Mode mode;
    private final @Nullable String focusKey;
    private final @Nullable RecipeCategory<?> category;
    private final List<View> history;
    private final @Nullable BaseGui root;
    private final List<RecipeCategory<?>> tabs = new ArrayList<>();
    private final Map<RecipeCategory<?>, List<CraftingRecipe>> recipesByTab = new LinkedHashMap<>();
    private int tab = 0;
    private int page = 0;

    private RecipeViewerGui(Player player, View view, List<View> history, @Nullable BaseGui root) {
        super(titleOf(view), BOTTOM_ROW + 1);
        this.player = player;
        this.mode = view.mode();
        this.focusKey = view.focusKey();
        this.category = view.category();
        this.history = history;
        this.root = root;

        for (CraftingRecipe recipe : recipesOf(view)) recipesByTab.computeIfAbsent(RecipeCategories.of(recipe), group -> new ArrayList<>()).add(recipe);
        tabs.addAll(recipesByTab.keySet());
        tab = Math.min(view.tab(), Math.max(0, tabs.size() - 1));
        page = tabs.isEmpty() ? 0 : Math.min(view.page(), recipesByTab.get(tabs.get(tab)).size() - 1);
    }

    public static RecipeViewerGui forRecipe(Player player, CraftingRecipe recipe, @Nullable BaseGui root) {
        RecipeViewerGui viewer = new RecipeViewerGui(player, new View(Mode.RECIPES, recipe.getPrimaryOutput().key(), null, 0, 0), List.of(), root);
        viewer.select(recipe);
        return viewer;
    }

    public static RecipeViewerGui forItem(Player player, String itemKey, @Nullable BaseGui root) {
        return new RecipeViewerGui(player, new View(Mode.RECIPES, itemKey, null, 0, 0), List.of(), root);
    }

    private static Component titleOf(View view) {
        return switch (view.mode()) {
            case RECIPES -> Component.text("Cách chế tạo: ").append(resolveDisplay(view.focusKey()).effectiveName());
            case USES -> Component.text("Công thức sử dụng: ").append(resolveDisplay(view.focusKey()).effectiveName());
            case CATEGORY -> Component.text(view.category().displayName());
        };
    }

    private static List<CraftingRecipe> recipesOf(View view) {
        return switch (view.mode()) {
            case RECIPES -> CraftingManager.getInstance().getRecipesProducing(view.focusKey());
            case USES -> CraftingManager.getInstance().getRecipesUsing(view.focusKey());
            case CATEGORY -> RecipeCategories.recipesIn(view.category());
        };
    }

    private void navigate(View next) {
        List<View> nextHistory = new ArrayList<>(history);
        nextHistory.add(new View(mode, focusKey, category, tab, page));
        if (nextHistory.size() > MAX_HISTORY) nextHistory.removeFirst();
        new RecipeViewerGui(player, next, List.copyOf(nextHistory), root).showInventory(player);
    }

    private void goBack() {
        if (history.isEmpty()) {
            if (root != null) root.showInventory(player);
            return;
        }
        new RecipeViewerGui(player, history.getLast(), history.subList(0, history.size() - 1), root).showInventory(player);
    }

    private void select(CraftingRecipe recipe) {
        int tabIndex = tabs.indexOf(RecipeCategories.of(recipe));
        if (tabIndex < 0) return;
        tab = tabIndex;

        page = Math.max(0, recipesByTab.get(tabs.get(tab)).indexOf(recipe));
    }

    @Override
    public void setup() {
        clearUi();
        for (int col = 0; col < 9; col++) {
            addButton(0, col, FILLER_BLACK, ClickHandler.noAction());
            addButton(BOTTOM_ROW, col, FILLER_BLACK, ClickHandler.noAction());
            for (int row = 0; row < RecipeLayout.ROWS; row++) addButton(CONTENT_FIRST_ROW + row, col, CONTENT_FILLER, ClickHandler.noAction());
        }

        if (!history.isEmpty() || root != null) {
            addButton(BACK_SLOT, backButton(), event -> {
                event.setCancelled(true);
                goBack();
            });
        }

        if (tabs.isEmpty()) {
            addButton(CONTENT_FIRST_ROW + 1, 4, emptyDisplay(), ClickHandler.noAction());
            return;
        }

        addTabs();

        RecipeCategory<?> shown = tabs.get(tab);
        List<CraftingRecipe> tabRecipes = recipesByTab.get(shown);
        SlotLayout layout = new SlotLayout();
        RecipeCategories.layout(tabRecipes.get(page), layout);
        layout.flush();

        addCatalysts(shown);
        if (tabRecipes.size() > 1) addPaging(tabRecipes.size());
    }

    private void addTabs() {
        for (int i = 0; i < tabs.size() && i < MAX_TABS; i++) {
            int index = i;
            RecipeCategory<?> category = tabs.get(i);
            int count = recipesByTab.get(category).size();

            boolean selected = index == tab;
            ItemStack item = ItemStack.of(category.icon());
            item.setData(DataComponentTypes.ITEM_NAME, Component.text("Loại công thức: ", NamedTextColor.GRAY)
                    .append(Component.text(category.displayName(), selected ? NamedTextColor.GOLD : NamedTextColor.WHITE)));
            item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                    Utils.text(count + " công thức", NamedTextColor.GRAY),
                    selected ? Utils.text("Đang xem.", NamedTextColor.YELLOW) : Utils.text("Click để xem loại công thức này.", NamedTextColor.GREEN)
            )));
            if (selected) item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

            addButton(FIRST_TAB_SLOT + i, item, event -> {
                event.setCancelled(true);
                tab = index;
                page = 0;
                setup();
            });
        }
    }

    private void addCatalysts(RecipeCategory<?> shown) {
        List<String> catalysts = RecipeCategories.catalystsOf(shown);
        for (int i = 0; i < catalysts.size() && i < MAX_CATALYSTS; i++) {
            ItemStack display = resolveDisplay(catalysts.get(i));
            Component role = Utils.text("Vật phẩm có thể chế tạo sử dụng khối này.", NamedTextColor.GOLD);
            if (mode == Mode.CATEGORY) {
                appendLore(display, List.of(role));
                addButton(BOTTOM_ROW, i, display, ClickHandler.noAction());
                continue;
            }

            appendLore(display, List.of(role, Utils.text("Click để xem mọi công thức của khối này.", NamedTextColor.GREEN)));
            addButton(BOTTOM_ROW, i, display, event -> {
                event.setCancelled(true);
                navigate(new View(Mode.CATEGORY, null, shown, 0, 0));
            });
        }
    }

    private void addPaging(int pageCount) {
        addButton(BOTTOM_ROW, PREV_PAGE_COL, PREV_PAGE_BUTTON, event -> {
            event.setCancelled(true);
            page = (page - 1 + pageCount) % pageCount;
            setup();
        });
        addButton(BOTTOM_ROW, PAGE_INFO_COL, label(Material.PAPER, "Trang " + (page + 1) + "/" + pageCount, NamedTextColor.YELLOW), ClickHandler.noAction());
        addButton(BOTTOM_ROW, NEXT_PAGE_COL, NEXT_PAGE_BUTTON, event -> {
            event.setCancelled(true);
            page = (page + 1) % pageCount;
            setup();
        });
    }

    private final class SlotLayout implements RecipeLayout {

        private final List<PendingSlot> pendingSlots = new ArrayList<>();

        @Override
        public Slot slot(int row, int col, Role role, CraftingIngredient ingredient) {
            PendingSlot slot = new PendingSlot(row, col, role, ingredient);
            if (fits(row, col)) pendingSlots.add(slot);
            return slot;
        }

        private void flush() {
            for (PendingSlot slot : pendingSlots) addIngredient(getSlot(CONTENT_FIRST_ROW + slot.row, slot.col), slot);
        }

        @Override
        public void decoration(int row, int col, ItemStack item) {
            if (fits(row, col)) addButton(CONTENT_FIRST_ROW + row, col, item, ClickHandler.noAction());
        }

        private boolean fits(int row, int col) {
            return row >= 0 && row < ROWS && col >= 0 && col < COLUMNS;
        }
    }

    private static final class PendingSlot implements RecipeLayout.Slot {

        private final int row;
        private final int col;
        private final RecipeLayout.Role role;
        private final CraftingIngredient ingredient;
        private @Nullable ItemStack display;
        private final List<String> lore = new ArrayList<>();

        private PendingSlot(int row, int col, RecipeLayout.Role role, CraftingIngredient ingredient) {
            this.row = row;
            this.col = col;
            this.role = role;
            this.ingredient = ingredient;
        }

        @Override
        public RecipeLayout.Slot display(ItemStack item) {
            this.display = item.clone();
            return this;
        }

        @Override
        public RecipeLayout.Slot lore(String... lines) {
            lore.addAll(List.of(lines));
            return this;
        }
    }

    private void addIngredient(int slot, PendingSlot pending) {
        CraftingIngredient ingredient = pending.ingredient;
        RecipeLayout.Role role = pending.role;
        String key = ingredient.key();
        CraftingManager manager = CraftingManager.getInstance();
        boolean canShowRecipes = !(mode == Mode.RECIPES && key.equals(focusKey)) && !manager.getRecipesProducing(key).isEmpty();
        boolean canShowUses = !(mode == Mode.USES && key.equals(focusKey)) && !manager.getRecipesUsing(key).isEmpty();

        ItemStack display;
        if (pending.display != null) {
            display = pending.display;
        } else {
            display = resolveDisplay(key);
            display.setAmount(Math.clamp(ingredient.count(), 1, display.getMaxStackSize()));
        }

        List<Component> lines = new ArrayList<>();
        for (String line : pending.lore) lines.add(Utils.text(line, NamedTextColor.GRAY));
        if (role == RecipeLayout.Role.OUTPUT) lines.add(Utils.text("Thành phẩm", NamedTextColor.GOLD));
        if (canShowRecipes) lines.add(Utils.text("Chuột trái: Xem cách chế tạo vật phẩm này", NamedTextColor.GREEN));
        if (canShowUses) lines.add(Utils.text("Chuột phải: Xem công thức sử dụng vật phẩm này", NamedTextColor.AQUA));
        if (!lines.isEmpty()) appendLore(display, lines);

        addButton(slot, display, event -> {
            event.setCancelled(true);
            if (event.getClick() == ClickType.RIGHT) {
                if (canShowUses) navigate(new View(Mode.USES, key, null, 0, 0));
            } else if (canShowRecipes) {
                navigate(new View(Mode.RECIPES, key, null, 0, 0));
            }
        });
    }

    private ItemStack emptyDisplay() {
        String text = mode == Mode.USES ? "Không có công thức sử dụng" : "Không có công thức";
        return label(Material.BARRIER, text, NamedTextColor.RED);
    }

    private ItemStack backButton() {
        View previous = history.isEmpty() ? null : history.getLast();
        if (previous == null || previous.focusKey() == null) return label(Material.ARROW, "« Quay lại", NamedTextColor.YELLOW);

        ItemStack item = resolveDisplay(previous.focusKey());
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("« Quay lại: ", NamedTextColor.YELLOW).append(item.effectiveName()));
        item.unsetData(DataComponentTypes.CUSTOM_NAME);
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text(previous.mode() == Mode.USES ? "Về công thức sử dụng." : "Về cách chế tạo.", NamedTextColor.GRAY)
        )));
        return item;
    }

    private static ItemStack label(Material material, String name, NamedTextColor color) {
        ItemStack item = ItemStack.of(material);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text(name, color));
        return item;
    }

    private static ItemStack resolveDisplay(String key) {
        ItemStack resolved = CraftingIngredient.resolveItemStack(key, 1);
        if (resolved != null) return resolved.clone();
        return label(Material.BARRIER, "Không tìm thấy: " + key, NamedTextColor.RED);
    }

    private static void appendLore(ItemStack stack, List<Component> lines) {
        ItemLore existing = stack.getData(DataComponentTypes.LORE);
        List<Component> lore = new ArrayList<>(existing == null ? List.of() : existing.lines());
        lore.add(Component.empty());
        lore.addAll(lines);
        stack.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
    }
}
