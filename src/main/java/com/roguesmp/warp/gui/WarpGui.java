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
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.roguesmp.gui.GuiItems.hint;
import static com.roguesmp.gui.GuiItems.item;
import static com.roguesmp.gui.GuiItems.line;

public class WarpGui extends BaseGui {

    private static final int PAGE_SIZE = 36;
    private static final int CREATE_SLOT = 4;
    private static final int EMPTY_SLOT = 22;

    private final Player player;
    private final boolean admin;
    private int currentPage = 0;

    public WarpGui(Player player) {
        super(Component.text("Điểm dịch chuyển"), 6);
        this.player = player;
        this.admin = Permissions.WARP_ADMIN.has(player);
    }

    @Override
    public void setup() {
        clearUi();
        for (int i = 0; i < 9; i++) {
            addButton(i, FILLER_BLACK, ClickHandler.noAction());
            addButton(45 + i, FILLER_BLACK, ClickHandler.noAction());
        }

        if (admin) {
            addButton(CREATE_SLOT, item(Material.EMERALD, "Create warp here", NamedTextColor.GREEN,
                    line("Creates a warp at your position"), hint("Click")), event -> {
                event.setCancelled(true);
                openCreateDialog();
            });
        }

        List<Map.Entry<String, Warp>> warps = WarpService.getInstance().usableWarps(player);
        int totalPages = Math.max(1, (int) Math.ceil(warps.size() / (double) PAGE_SIZE));
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

        if (warps.isEmpty()) {
            addButton(EMPTY_SLOT, item(Material.BARRIER, "Chưa có điểm dịch chuyển nào", NamedTextColor.GRAY), ClickHandler.noAction());
        }

        int from = currentPage * PAGE_SIZE;
        int slot = 9;
        for (Map.Entry<String, Warp> entry : warps.subList(from, Math.min(from + PAGE_SIZE, warps.size()))) {
            String id = entry.getKey();
            addButton(slot++, warpButton(id, entry.getValue()), event -> {
                event.setCancelled(true);
                if (admin && event.getClick().isShiftClick()) {
                    Utils.runLater(() -> new WarpEditorGui(player, id).showInventory(player));
                    return;
                }
                player.closeInventory();
                WarpService.getInstance().teleport(player, id);
            });
        }
        fillEmpty(FILLER);
    }

    private org.bukkit.inventory.ItemStack warpButton(String id, Warp warp) {
        List<Component> lore = new ArrayList<>(Utils.fromStrings(warp.lore()));
        if (!lore.isEmpty()) lore.add(Component.empty());
        lore.add(hint("Click để dịch chuyển"));
        if (admin) {
            lore.add(line("Id: " + id + "  Order: " + warp.order()));
            lore.add(line(warp.destination().format()));
            if (!warp.destination().isAvailable(player)) lore.add(Utils.text("⚠ Destination can't be resolved", NamedTextColor.RED));
            lore.add(hint("Shift + click: edit"));
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
