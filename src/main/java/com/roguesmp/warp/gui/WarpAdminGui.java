package com.roguesmp.warp.gui;

import com.roguesmp.command.CommandFeedback;
import com.roguesmp.codec.DataResult;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.permission.Permissions;
import com.roguesmp.teleport.destination.LocationDestination;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.warp.Warp;
import com.roguesmp.warp.WarpService;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.sound.Sound;
import io.papermc.paper.registry.keys.SoundEventKeys;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.roguesmp.gui.GuiItems.hint;
import static com.roguesmp.gui.GuiItems.item;
import static com.roguesmp.gui.GuiItems.line;

public class WarpAdminGui extends BaseGui {

    private static final int[] CONTENT_SLOTS = contentSlots();
    private static final int HEADER_SLOT = 4;
    private static final int CREATE_SLOT = 8;
    private static final int BACK_SLOT = 45;
    private static final int PREV_SLOT = 48;
    private static final int PAGE_SLOT = 49;
    private static final int NEXT_SLOT = 50;
    private static final int CLOSE_SLOT = 53;
    private static final int EMPTY_SLOT = 22;

    private final Player player;
    private final boolean admin;
    private int currentPage = 0;

    public WarpAdminGui(Player player) {
        super(Component.text("Quản lý điểm dịch chuyển", NamedTextColor.DARK_RED), 6);
        this.player = player;
        this.admin = Permissions.WARP_ADMIN.has(player);
    }

    private static int[] contentSlots() {
        int[] slots = new int[28];
        int index = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                slots[index++] = row * 9 + col;
            }
        }
        return slots;
    }

    @Override
    public void setup() {
        clearUi();
        for (int i = 0; i < 54; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) addButton(i, FILLER_BLACK, ClickHandler.noAction());
        }
        addButton(1, createDecoration(Material.PURPLE_STAINED_GLASS_PANE), ClickHandler.noAction());
        addButton(7, createDecoration(Material.PURPLE_STAINED_GLASS_PANE), ClickHandler.noAction());

        List<Map.Entry<String, Warp>> warps = WarpService.getInstance().usableWarps(player);
        int totalPages = Math.max(1, (int) Math.ceil(warps.size() / (double) CONTENT_SLOTS.length));
        currentPage = Math.min(currentPage, totalPages - 1);

        addButton(HEADER_SLOT, item(Material.RECOVERY_COMPASS, "Điểm Dịch Chuyển", NamedTextColor.LIGHT_PURPLE,
                line("Chọn một địa điểm để dịch chuyển tới."),
                Component.empty(),
                Utils.text("Số điểm khả dụng: ", NamedTextColor.GRAY).append(Utils.text(String.valueOf(warps.size()), NamedTextColor.AQUA))),
                ClickHandler.noAction());

        if (admin) {
            addButton(CREATE_SLOT, item(Material.EMERALD, "Tạo điểm dịch chuyển", NamedTextColor.GREEN,
                    line("Tạo điểm dịch chuyển tại vị trí của bạn"), hint("Click")), event -> {
                event.setCancelled(true);
                openCreateDialog();
            });
        }

        addBackButton(BACK_SLOT);
        addButton(CLOSE_SLOT, item(Material.BARRIER, "Đóng", NamedTextColor.RED), event -> {
            event.setCancelled(true);
            Utils.runLater(player::closeInventory);
        });

        if (currentPage > 0) {
            addButton(PREV_SLOT, PREV_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                changePage(-1);
            });
        }
        if (currentPage < totalPages - 1) {
            addButton(NEXT_SLOT, NEXT_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                changePage(1);
            });
        }
        addButton(PAGE_SLOT, item(Material.PAPER, "Trang " + (currentPage + 1) + "/" + totalPages, NamedTextColor.YELLOW),
                ClickHandler.noAction());

        if (warps.isEmpty()) {
            addButton(EMPTY_SLOT, item(Material.STRUCTURE_VOID, "Chưa có điểm dịch chuyển nào", NamedTextColor.GRAY), ClickHandler.noAction());
        }

        int from = currentPage * CONTENT_SLOTS.length;
        List<Map.Entry<String, Warp>> pageWarps = warps.subList(from, Math.min(from + CONTENT_SLOTS.length, warps.size()));
        for (int i = 0; i < pageWarps.size(); i++) {
            String id = pageWarps.get(i).getKey();
            addButton(CONTENT_SLOTS[i], warpButton(id, pageWarps.get(i).getValue()), event -> {
                event.setCancelled(true);
                if (admin && event.getClick().isShiftClick()) {
                    Utils.runLater(() -> new WarpEditorGui(player, id).showInventory(player));
                    return;
                }
                player.closeInventory();
                player.playSound(Sound.sound(SoundEventKeys.ENTITY_ENDERMAN_TELEPORT, Sound.Source.PLAYER, 0.8f, 1.2f));
                WarpService.getInstance().teleport(player, id);
            });
        }
        fillEmpty(FILLER);
    }

    private void changePage(int delta) {
        currentPage += delta;
        player.playSound(Sound.sound(SoundEventKeys.ITEM_BOOK_PAGE_TURN, Sound.Source.PLAYER, 1f, 1f));
        setup();
    }

    private ItemStack warpButton(String id, Warp warp) {
        List<Component> lore = new ArrayList<>(Utils.fromStrings(warp.lore()));
        if (!lore.isEmpty()) lore.add(Component.empty());
        lore.add(hint("Click để dịch chuyển"));
        if (admin) {
            lore.add(Component.empty());
            lore.add(line("Id: " + id + "  Thứ tự: " + warp.order()));
            lore.add(line(warp.destination().format()));
            if (!warp.destination().isAvailable(player)) lore.add(Utils.text("⚠ Không xác định được điểm đến", NamedTextColor.RED));
            lore.add(hint("Shift + click: chỉnh sửa"));
        }
        return item(warp.createIcon(), Utils.fromString(warp.displayName()), lore);
    }

    private void openCreateDialog() {
        Dialog dialog = DialogBuilder.create(Component.text("Create warp"))
                .addTextInput("warp_id", Component.text("Warp name"))
                .confirmation()
                .yesButton(Component.text("Create"), null, (response, audience) -> {
                    String id = response.getText("warp_id") == null ? "" : response.getText("warp_id").trim().toLowerCase();
                    Utils.runLater(() -> create(id));
                })
                .noButton(Component.text("Cancel"), null, (response, audience) -> Utils.runLater(() -> showInventory(player)))
                .build();

        player.closeInventory();
        player.showDialog(dialog);
    }

    private void create(String id) {
        DataResult<Warp> result = WarpService.getInstance().create(id, LocationDestination.of(player.getLocation()));
        if (CommandFeedback.failed(player, result)) {
            showInventory(player);
            return;
        }
        player.sendMessage(Utils.fromString("<green>Warp '" + id + "' created."));
        new WarpEditorGui(player, id).showInventory(player);
    }
}
