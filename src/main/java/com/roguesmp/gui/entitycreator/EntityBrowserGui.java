package com.roguesmp.gui.entitycreator;

import com.roguesmp.crafting.recipe.CraftingRecipe;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.entity.component.impl.AttributeComponent;
import com.roguesmp.entity.component.impl.EquipmentComponent;
import com.roguesmp.entity.component.impl.LootTableComponent;
import com.roguesmp.entity.component.impl.SpellComponent;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.gui.loottablecreator.LootTableGuiCreator;
import com.roguesmp.registry.Registries;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EntityBrowserGui extends BaseGui {

    private static final int PAGE_SIZE = 36;
    private static final int SEARCH_SLOT = 8;
    private static final int GUIDE_SLOT = 4;
    private static final int CREATE_NEW_SLOT = 0;

    private final Player player;
    private final List<BaseEntity> allEntries;
    private List<BaseEntity> filteredEntries;
    private String searchQuery = null;
    private int currentPage = 0;
    private int totalPages;

    public EntityBrowserGui(Player player) {
        super(Component.text("Browse Entity"), 6);
        this.player = player;
        this.allEntries = new ArrayList<>(Registries.ENTITY.getAll().values());
        allEntries.sort(Comparator.comparing(BaseEntity::getId));
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

        addButton(CREATE_NEW_SLOT, createNewButton(), event -> {
            event.setCancelled(true);
            player.closeInventory();
            new EntityCreatorGui().openMainDialog(player);
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
                Utils.text("Chuột trái để spawn entity", NamedTextColor.GREEN),
                Utils.text("Chuột phải để edit entity", NamedTextColor.GREEN)
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
        for (BaseEntity base : getPage()) {
            addButton(slot, previewButtonFor(base), event -> {
                event.setCancelled(true);
                if (event.getClick() == ClickType.RIGHT) {
                    openEditor(base);
                } else {
                    spawnEntity(base);
                }
            });
            slot++;
        }
    }

    private void updateFilters() {
        filteredEntries = new ArrayList<>();
        String query = (searchQuery == null || searchQuery.isBlank()) ? null : searchQuery.toLowerCase().trim();

        for (BaseEntity base : allEntries) {
            if (query != null && !base.getId().toLowerCase().contains(query)) continue;
            filteredEntries.add(base);
        }

        totalPages = Math.max(1, (int) Math.ceil(filteredEntries.size() / (double) PAGE_SIZE));
        currentPage = 0;
    }

    private void openSearchDialog() {
        Dialog dialog = DialogBuilder.create(Component.text("Tìm kiếm entity"))
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

    private List<BaseEntity> getPage() {
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, filteredEntries.size());
        if (from >= filteredEntries.size()) return Collections.emptyList();
        return filteredEntries.subList(from, to);
    }

    private void openEditor(BaseEntity base) {
        EntityCreatorGui.editExisting(base).openMainDialog(player);
    }

    private void spawnEntity(BaseEntity base) {
        base.spawn(player.getLocation());
    }

    private ItemStack createNewButton() {
        ItemStack item = ItemStack.of(Material.EMERALD);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Tạo mới", NamedTextColor.GREEN));
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

    private ItemStack previewButtonFor(BaseEntity base) {
        Material egg;
        egg = Bukkit.getItemFactory().getSpawnEgg(base.getEntityType());
        if (egg == null) egg = Material.PAPER;
        ItemStack itemStack = ItemStack.of(egg);

        String baseName = base.getDisplayName();
        if (baseName == null) baseName = base.getId();
        Component name = Utils.fromString(baseName);
        itemStack.setData(DataComponentTypes.ITEM_NAME, name);

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text(base.getId(), NamedTextColor.DARK_GRAY));
        AttributeComponent attributeComponent = base.getComponent(EntityComponentKeys.ATTRIBUTES);
        if (attributeComponent != null) {
            lore.add(Component.empty());
            lore.add(Utils.text("Attribute:", NamedTextColor.AQUA));
            attributeComponent.values().forEach((entityAttribute, aDouble) -> {
                lore.add(Utils.text("- " + entityAttribute + ": " + Utils.formatDecimal(aDouble), NamedTextColor.GRAY));
            });
        }

        EquipmentComponent equipmentComponent = base.getComponent(EntityComponentKeys.EQUIPMENT);
        if (equipmentComponent != null && !equipmentComponent.slots().isEmpty()) {
            lore.add(Component.empty());
            lore.add(Utils.text("Equipment:", NamedTextColor.AQUA));

            equipmentComponent.slots().forEach((slot, entityEquipment) -> {

                String itemDisplayName = entityEquipment.getDisplayName();
                Component display = itemDisplayName != null
                        ? Utils.fromString(itemDisplayName)
                        : Component.text(entityEquipment.getMaterial().toString());

                lore.add(Utils.text("- " + slot + ": ", NamedTextColor.GRAY)
                        .append(display.colorIfAbsent(NamedTextColor.YELLOW)));
            });
        }

        LootTableComponent lootTableComponent = base.getComponent(EntityComponentKeys.LOOT_TABLE);
        if (lootTableComponent != null && !lootTableComponent.lootTableIds().isEmpty()) {
            lore.add(Component.empty());
            lore.add(Utils.text("Loot table:", NamedTextColor.AQUA));

            lootTableComponent.lootTableIds().forEach(s -> {
                lore.add(Utils.text("- " + s, NamedTextColor.GRAY));
            });
        }

        lore.add(Component.empty());
        lore.add(Utils.text("Mở edit lên để xem chi tiết hơn.", NamedTextColor.YELLOW));

        itemStack.setData(DataComponentTypes.LORE, ItemLore.lore(lore));

        return itemStack;
    }
}
