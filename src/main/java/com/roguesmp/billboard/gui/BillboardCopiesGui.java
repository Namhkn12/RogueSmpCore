package com.roguesmp.billboard.gui;

import com.roguesmp.billboard.BillboardManager;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Comparator;
import java.util.List;

import static com.roguesmp.gui.GuiItems.hint;
import static com.roguesmp.gui.GuiItems.item;
import static com.roguesmp.gui.GuiItems.line;

public class BillboardCopiesGui extends BaseGui {

    private static final int[] CONTENT_SLOTS = contentSlots();
    private static final int BACK_SLOT = 0;
    private static final int HEADER_SLOT = 4;
    private static final int EMPTY_SLOT = 22;
    private static final int PREV_SLOT = 48;
    private static final int PAGE_SLOT = 49;
    private static final int NEXT_SLOT = 50;

    private final Player player;
    private final String billboardId;
    private final BaseGui parent;
    private int currentPage = 0;

    public BillboardCopiesGui(Player player, String billboardId, BaseGui parent) {
        super(Component.text("Copies: " + billboardId, NamedTextColor.DARK_AQUA), 6);
        this.player = player;
        this.billboardId = billboardId;
        this.parent = parent;
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
            if (i / 9 == 0 || i / 9 == 5 || i % 9 == 0 || i % 9 == 8) addButton(i, FILLER_BLACK, ClickHandler.noAction());
        }

        Location here = player.getLocation();
        List<BillboardManager.Placed> copies = BillboardManager.getInstance().copiesOf(billboardId).stream()
                .sorted(Comparator.<BillboardManager.Placed, Boolean>comparing(copy -> !copy.instance().getAnchor().getWorld().equals(here.getWorld()))
                        .thenComparing(copy -> copy.instance().getAnchor().getWorld().getName())
                        .thenComparingDouble(copy -> distanceSquared(copy.instance().getAnchor(), here)))
                .toList();
        int totalPages = Math.max(1, (int) Math.ceil(copies.size() / (double) CONTENT_SLOTS.length));
        currentPage = Math.min(currentPage, totalPages - 1);

        addButton(BACK_SLOT, item(Material.ARROW, "Back", NamedTextColor.GRAY), event -> {
            event.setCancelled(true);
            Utils.runLater(() -> parent.showInventory(player));
        });
        addButton(HEADER_SLOT, item(Material.OAK_SIGN, billboardId, NamedTextColor.AQUA,
                line(copies.size() + " loaded copy(ies)"),
                line("Copies in unloaded chunks or worlds are not listed")), ClickHandler.noAction());
        if (currentPage > 0) {
            addButton(PREV_SLOT, PREV_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                currentPage--;
                setup();
            });
        }
        if (currentPage < totalPages - 1) {
            addButton(NEXT_SLOT, NEXT_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                currentPage++;
                setup();
            });
        }
        addButton(PAGE_SLOT, item(Material.PAPER, "Page " + (currentPage + 1) + "/" + totalPages, NamedTextColor.YELLOW), ClickHandler.noAction());

        if (copies.isEmpty()) {
            addButton(EMPTY_SLOT, item(Material.STRUCTURE_VOID, "No loaded copies", NamedTextColor.GRAY), ClickHandler.noAction());
        }

        int from = currentPage * CONTENT_SLOTS.length;
        List<BillboardManager.Placed> page = copies.subList(from, Math.min(from + CONTENT_SLOTS.length, copies.size()));
        for (int i = 0; i < page.size(); i++) {
            BillboardManager.Placed copy = page.get(i);
            Location at = copy.instance().getAnchor();
            boolean sameWorld = at.getWorld().equals(here.getWorld());
            addButton(CONTENT_SLOTS[i], item(sameWorld ? Material.ARMOR_STAND : Material.ENDER_EYE,
                    String.format("%d, %d, %d", at.getBlockX(), at.getBlockY(), at.getBlockZ()), NamedTextColor.WHITE,
                    line("World: " + at.getWorld().getName()),
                    line(sameWorld ? String.format("%.1f blocks away", Math.sqrt(distanceSquared(at, here))) : "Other world"),
                    hint("Click: teleport"),
                    hint("Shift + right click: remove this copy")), event -> {
                event.setCancelled(true);
                if (event.getClick() == ClickType.SHIFT_RIGHT) {
                    if (Bukkit.getEntity(copy.markerId()) instanceof Marker marker) {
                        marker.remove();
                        player.sendMessage(Utils.fromString("<yellow>Removed a copy of '" + billboardId + "'."));
                    }
                    Utils.runLater(this::setup);
                    return;
                }
                if (event.getClick().isShiftClick()) return;
                player.closeInventory();
                player.teleport(at);
            });
        }
        fillEmpty(FILLER);
    }

    private static double distanceSquared(Location a, Location b) {
        return a.getWorld().equals(b.getWorld()) ? a.distanceSquared(b) : Double.MAX_VALUE;
    }
}
