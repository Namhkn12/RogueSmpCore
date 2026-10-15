package com.roguesmp.gui.spawner;

import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpawnerMobPickerGui extends BaseGui {

    private static final int PAGE_SIZE = 36;
    private static final int DONE_SLOT = 0;
    private static final int GUIDE_SLOT = 4;
    private static final int SELECTED_ONLY_SLOT = 6;
    private static final int SEARCH_SLOT = 8;

    private final Player player;
    private final SpawnerCreatorGui creator;
    private String searchQuery = null;
    private boolean selectedOnly = false;
    private int currentPage = 0;

    public SpawnerMobPickerGui(Player player, SpawnerCreatorGui creator) {
        super(Component.text("Chọn mob cho spawner"), 6);
        this.player = player;
        this.creator = creator;
    }

    @Override
    public void setup() {
        clearUi();
        for (int i = 0; i < 9; i++) {
            addButton(i, FILLER_BLACK, ClickHandler.noAction());
            addButton(45 + i, FILLER_BLACK, ClickHandler.noAction());
        }

        Map<String, Integer> mobs = creator.mobs();

        addButton(DONE_SLOT, button(Material.ARROW, "Xong", NamedTextColor.GREEN,
                Utils.text("Đã chọn " + mobs.size() + " mob", NamedTextColor.GRAY)), event -> {
            event.setCancelled(true);
            player.closeInventory();
            Utils.runLater(() -> creator.open(player));
        });

        addButton(GUIDE_SLOT, button(Material.BOOK, "Guide", NamedTextColor.GREEN,
                Utils.text("Chuột trái: thêm / weight +1", NamedTextColor.GREEN),
                Utils.text("Shift + chuột trái: weight +10", NamedTextColor.GREEN),
                Utils.text("Chuột phải: weight -1", NamedTextColor.YELLOW),
                Utils.text("Shift + chuột phải: bỏ chọn", NamedTextColor.RED)), ClickHandler.noAction());

        addButton(SELECTED_ONLY_SLOT, button(selectedOnly ? Material.LIME_DYE : Material.GRAY_DYE,
                "Chỉ hiện mob đã chọn: " + (selectedOnly ? "BẬT" : "TẮT"), selectedOnly ? NamedTextColor.GREEN : NamedTextColor.GRAY), event -> {
            event.setCancelled(true);
            selectedOnly = !selectedOnly;
            currentPage = 0;
            setup();
        });

        addButton(SEARCH_SLOT, button(Material.COMPASS, "Tìm kiếm theo ID", NamedTextColor.GOLD,
                Utils.text("Từ khóa hiện tại: " + (searchQuery == null || searchQuery.isBlank() ? "Trống" : searchQuery), NamedTextColor.YELLOW),
                Utils.text("Click để đổi từ khóa.", NamedTextColor.GRAY),
                Utils.text("Shift-Click để xoá bộ lọc.", NamedTextColor.RED)), event -> {
            event.setCancelled(true);
            if (event.getClick().isShiftClick()) {
                searchQuery = null;
                currentPage = 0;
                setup();
                return;
            }
            openSearchDialog();
        });

        List<BaseEntity> entities = filteredEntities(mobs);
        int totalPages = Math.max(1, (int) Math.ceil(entities.size() / (double) PAGE_SIZE));
        currentPage = Math.min(currentPage, totalPages - 1);

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

        int from = currentPage * PAGE_SIZE;
        int slot = 9;
        for (BaseEntity entity : entities.subList(from, Math.min(from + PAGE_SIZE, entities.size()))) {
            String id = entity.getId();
            addButton(slot++, entityButton(entity, mobs.get(id)), event -> {
                event.setCancelled(true);
                ClickType click = event.getClick();
                if (click == ClickType.SHIFT_RIGHT) {
                    mobs.remove(id);
                } else if (click.isRightClick()) {
                    mobs.computeIfPresent(id, (key, weight) -> weight > 1 ? weight - 1 : null);
                } else {
                    mobs.merge(id, click.isShiftClick() ? 10 : 1, Integer::sum);
                }
                setup();
            });
        }
        fillEmpty(FILLER);
    }

    private List<BaseEntity> filteredEntities(Map<String, Integer> mobs) {
        String query = searchQuery == null || searchQuery.isBlank() ? null : searchQuery.toLowerCase().trim();
        List<BaseEntity> result = new ArrayList<>();
        for (String id : EntityManager.getInstance().getAllIds().stream().sorted().toList()) {
            if (selectedOnly && !mobs.containsKey(id)) continue;
            if (query != null && !id.toLowerCase().contains(query)) continue;
            BaseEntity entity = EntityManager.getInstance().getBaseEntity(id);
            if (entity != null) result.add(entity);
        }
        return result;
    }

    private ItemStack entityButton(BaseEntity entity, Integer weight) {
        Material egg = Bukkit.getItemFactory().getSpawnEgg(entity.getEntityType());
        ItemStack item = ItemStack.of(egg != null ? egg : Material.PAPER);
        item.setData(DataComponentTypes.ITEM_NAME, Utils.fromString(entity.getDisplayName() != null ? entity.getDisplayName() : entity.getId()));

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text(entity.getId(), NamedTextColor.DARK_GRAY));
        lore.add(weight == null
                ? Utils.text("Chưa chọn", NamedTextColor.GRAY)
                : Utils.text("Weight: " + weight, NamedTextColor.GREEN));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        if (weight != null) item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return item;
    }

    private static ItemStack button(Material material, String name, NamedTextColor color, Component... lore) {
        ItemStack item = ItemStack.of(material);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text(name, color));
        if (lore.length > 0) item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(lore)));
        return item;
    }

    private void openSearchDialog() {
        Dialog dialog = DialogBuilder.create(Component.text("Tìm kiếm mob"))
                .addTextInput("value", Component.text("Nhập ID hoặc một phần ID..."), b -> b.initial(searchQuery == null ? "" : searchQuery))
                .confirmation()
                .yesButton(Component.text("Tìm kiếm"), null, (response, audience) -> {
                    searchQuery = response.getText("value");
                    currentPage = 0;
                    Utils.runLater(() -> showInventory(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> showInventory(player)))
                .build();

        player.closeInventory();
        player.showDialog(dialog);
    }
}
