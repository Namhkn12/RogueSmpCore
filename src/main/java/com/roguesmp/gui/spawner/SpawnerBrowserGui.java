package com.roguesmp.gui.spawner;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.spawner.Spawner;
import com.roguesmp.registry.Registries;
import com.roguesmp.spawner.behavior.BehaviorData;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SpawnerBrowserGui extends BaseGui {

    private static final int PAGE_SIZE = 36;
    private static final int CREATE_NEW_SLOT = 0;
    private static final int GUIDE_SLOT = 4;
    private static final int SEARCH_SLOT = 8;

    private final Player player;
    private String searchQuery = null;
    private int currentPage = 0;

    public SpawnerBrowserGui(Player player) {
        super(Component.text("Browse Spawner"), 6);
        this.player = player;
    }

    @Override
    public void setup() {
        clearUi();
        for (int i = 0; i < 9; i++) {
            addButton(i, FILLER_BLACK, ClickHandler.noAction());
            addButton(45 + i, FILLER_BLACK, ClickHandler.noAction());
        }

        addButton(CREATE_NEW_SLOT, button(Material.EMERALD, "Tạo mới", NamedTextColor.GREEN), event -> {
            event.setCancelled(true);
            player.closeInventory();
            new SpawnerCreatorGui().open(player);
        });

        addButton(GUIDE_SLOT, button(Material.BOOK, "Guide", NamedTextColor.GREEN,
                Utils.text("Chuột trái để lấy spawner", NamedTextColor.GREEN),
                Utils.text("Chuột phải để edit spawner", NamedTextColor.GREEN),
                Utils.text("Shift + chuột phải để xoá spawner", NamedTextColor.RED)), ClickHandler.noAction());

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

        List<Spawner> spawners = filteredSpawners();
        int totalPages = Math.max(1, (int) Math.ceil(spawners.size() / (double) PAGE_SIZE));
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
        for (Spawner spawner : spawners.subList(from, Math.min(from + PAGE_SIZE, spawners.size()))) {
            addButton(slot++, spawnerButton(spawner), event -> {
                event.setCancelled(true);
                if (event.getClick() == ClickType.SHIFT_RIGHT) {
                    openDeleteDialog(spawner);
                } else if (event.getClick().isRightClick()) {
                    player.closeInventory();
                    SpawnerCreatorGui.editExisting(spawner).open(player);
                } else {
                    PlayerUtils.giveItem(player, spawner.toItem());
                }
            });
        }
        fillEmpty(FILLER);
    }

    private List<Spawner> filteredSpawners() {
        String query = searchQuery == null || searchQuery.isBlank() ? null : searchQuery.toLowerCase().trim();
        List<Spawner> result = new ArrayList<>();
        for (String id : Registries.SPAWNER.getAll().keySet().stream().sorted().toList()) {
            if (query != null && !id.toLowerCase().contains(query)) continue;
            result.add(Registries.SPAWNER.get(id));
        }
        return result;
    }

    private ItemStack spawnerButton(Spawner spawner) {
        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text(spawner.getId(), NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());
        lore.add(Utils.text("Mobs:", NamedTextColor.AQUA));
        if (spawner.getMobs() != null) {
            spawner.getMobs().forEach((mob, weight) -> lore.add(Utils.text("- " + mob + " (" + weight + ")", NamedTextColor.GRAY)));
        }
        lore.add(Component.empty());
        lore.add(Utils.text("Delay: " + spawner.getMinDelay() + "-" + spawner.getMaxDelay() + "  Count: " + spawner.getSpawnCount(), NamedTextColor.GRAY));
        lore.add(Utils.text("Active range: " + spawner.getActiveRange() + "  Spawn range: " + spawner.getSpawnRange(), NamedTextColor.GRAY));
        lore.add(Utils.text("Max nearby: " + spawner.getMaxNearBy(), NamedTextColor.GRAY));
        if (spawner.getBehaviors() != null && !spawner.getBehaviors().isEmpty()) {
            lore.add(Component.empty());
            lore.add(Utils.text("Behaviors:", NamedTextColor.AQUA));
            for (BehaviorData behavior : spawner.getBehaviors()) {
                lore.add(Utils.text("- " + SpawnerCreatorGui.formatBehavior(behavior), NamedTextColor.GRAY));
            }
        }

        ItemStack item = ItemStack.of(Material.PAPER);
        item.setData(DataComponentTypes.ITEM_MODEL, Material.SPAWNER.key());
        item.setData(DataComponentTypes.ITEM_NAME, Utils.fromString(spawner.getName() != null ? spawner.getName() : spawner.getId()));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    private static ItemStack button(Material material, String name, NamedTextColor color, Component... lore) {
        ItemStack item = ItemStack.of(material);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text(name, color));
        if (lore.length > 0) item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(lore)));
        return item;
    }

    private void openSearchDialog() {
        Dialog dialog = DialogBuilder.create(Component.text("Tìm kiếm spawner"))
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

    private void openDeleteDialog(Spawner spawner) {
        Dialog dialog = DialogBuilder.create(Component.text("Xoá spawner '" + spawner.getId() + "'?"))
                .addTextBody(Component.text("Hành động này không thể hoàn tác.", NamedTextColor.GRAY))
                .confirmation()
                .yesButton(Component.text("Xoá"), null, (response, audience) -> Utils.runLater(() -> {
                    if (!Registries.SPAWNER.removeAndDeleteFiles(RogueSmpCore.getInstance(), spawner.getId())) {
                        player.sendMessage(Utils.fromString("<red>Không thể xoá spawner " + spawner.getId() + "."));
                    }
                    showInventory(player);
                }))
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> showInventory(player)))
                .build();

        player.closeInventory();
        player.showDialog(dialog);
    }
}
