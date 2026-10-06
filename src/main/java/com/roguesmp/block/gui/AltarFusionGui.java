package com.roguesmp.block.gui;

import com.roguesmp.block.impl.altar.AltarMainBlock;
import com.roguesmp.block.impl.altar.AltarSideBlock;
import com.roguesmp.block.impl.altar.AltarSideSlot;
import com.roguesmp.block.impl.altar.FusionAssessment;
import com.roguesmp.block.impl.altar.FusionIssue;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AltarFusionGui extends BaseGui {

    private static final int INPUT_SLOT = 22;
    private static final int RESULT_SLOT = 50;
    private static final int FUSE_BUTTON_SLOT = 49;
    private static final int CLOSE_SLOT = 53;

    private static final Map<AltarSideSlot, Integer> PEDESTAL_SLOTS = new EnumMap<>(Map.of(
            AltarSideSlot.NORTH_WEST, 2,
            AltarSideSlot.NORTH, 4,
            AltarSideSlot.NORTH_EAST, 6,
            AltarSideSlot.WEST, 19,
            AltarSideSlot.EAST, 25,
            AltarSideSlot.SOUTH_WEST, 38,
            AltarSideSlot.SOUTH, 40,
            AltarSideSlot.SOUTH_EAST, 42
    ));

    private static final Map<AltarSideSlot, int[]> TRAIL_SLOTS = new EnumMap<>(Map.of(
            AltarSideSlot.NORTH_WEST, new int[]{12},
            AltarSideSlot.NORTH, new int[]{13},
            AltarSideSlot.NORTH_EAST, new int[]{14},
            AltarSideSlot.WEST, new int[]{20, 21},
            AltarSideSlot.EAST, new int[]{23, 24},
            AltarSideSlot.SOUTH_WEST, new int[]{30},
            AltarSideSlot.SOUTH, new int[]{31},
            AltarSideSlot.SOUTH_EAST, new int[]{32}
    ));

    private static final Set<Integer> RESERVED_SLOTS = computeReservedSlots();

    private static Set<Integer> computeReservedSlots() {
        Set<Integer> reserved = new HashSet<>(PEDESTAL_SLOTS.values());
        TRAIL_SLOTS.values().forEach(trail -> {
            for (int slot : trail) reserved.add(slot);
        });
        reserved.addAll(List.of(INPUT_SLOT, RESULT_SLOT, FUSE_BUTTON_SLOT, CLOSE_SLOT));
        return Set.copyOf(reserved);
    }

    private final AltarMainBlock altar;

    public AltarFusionGui(AltarMainBlock altar) {
        super(Utils.text("Altar Hợp Nhất", NamedTextColor.DARK_GRAY), 6);
        this.altar = altar;
    }

    @Override
    public void setup() {
        for (int slot = 0; slot < getInventory().getSize(); slot++) {
            if (!RESERVED_SLOTS.contains(slot)) addButton(slot, FILLER_BLACK, ClickHandler.noAction());
        }

        FusionAssessment assessment = altar.assess();
        renderInput();
        renderPedestals(assessment.isReady());
        renderResult(assessment);
        renderFuseButton(assessment);
        addButton(CLOSE_SLOT, closeButton(), event -> event.getWhoClicked().closeInventory());
    }

    private void renderInput() {
        ItemStack input = altar.getItem();
        addButton(INPUT_SLOT, input != null ? input.clone() : placeholder(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "Vật phẩm chính", "Đặt lên altar trung tâm"), ClickHandler.noAction());
    }

    private void renderPedestals(boolean ready) {
        for (AltarSideSlot slot : AltarSideSlot.values()) {
            AltarSideBlock side = altar.getSide(slot);
            ItemStack item = side == null ? null : side.getItem();

            addButton(PEDESTAL_SLOTS.get(slot), pedestalDisplay(side, item), ClickHandler.noAction());
            for (int trailSlot : TRAIL_SLOTS.get(slot)) {
                addButton(trailSlot, createDecoration(trailColor(side, item, ready)), ClickHandler.noAction());
            }
        }
    }

    private ItemStack pedestalDisplay(AltarSideBlock side, ItemStack item) {
        if (side == null) return placeholder(Material.RED_STAINED_GLASS_PANE, "Thiếu altar phụ", "Hãy đặt altar phụ tại vị trí này");
        if (item == null) return placeholder(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "Altar trống", "Đặt vật phẩm lên altar này");
        return item.clone();
    }

    private Material trailColor(AltarSideBlock side, ItemStack item, boolean ready) {
        if (side == null) return Material.RED_STAINED_GLASS_PANE;
        if (item == null) return Material.BLACK_STAINED_GLASS_PANE;
        return ready ? Material.LIME_STAINED_GLASS_PANE : Material.YELLOW_STAINED_GLASS_PANE;
    }

    private void renderResult(FusionAssessment assessment) {
        ItemStack result = assessment.result();
        addButton(RESULT_SLOT, result != null ? result : placeholder(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "Thành phẩm", "Chưa có công thức phù hợp"), ClickHandler.noAction());
    }

    private void renderFuseButton(FusionAssessment assessment) {
        ItemStack button = ItemStack.of(assessment.isReady() ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE);
        if (assessment.isReady()) {
            ItemStackUtils.setItemName(button, Utils.text("Bắt đầu hợp nhất", NamedTextColor.GREEN));
            ItemStackUtils.setLore(button, List.of(Utils.text("Nhấn để bắt đầu nghi thức", NamedTextColor.GRAY)));
        } else {
            ItemStackUtils.setItemName(button, Utils.text("Chưa sẵn sàng", NamedTextColor.RED));
            ItemStackUtils.setLore(button, List.of(Utils.text(assessment.issue().message(), NamedTextColor.GRAY)));
        }
        addButton(FUSE_BUTTON_SLOT, button, this::onFuseClick);
    }

    private void onFuseClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        FusionIssue issue = altar.startFusion(player);
        if (issue == FusionIssue.NONE) {
            player.closeInventory();
            return;
        }

        player.sendMessage(Utils.text(issue.message(), NamedTextColor.RED));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    private ItemStack closeButton() {
        ItemStack item = ItemStack.of(Material.BARRIER);
        ItemStackUtils.setItemName(item, Utils.text("Đóng", NamedTextColor.RED));
        return item;
    }

    private ItemStack placeholder(Material material, String name, String hint) {
        ItemStack item = ItemStack.of(material);
        ItemStackUtils.setItemName(item, Utils.text(name, NamedTextColor.GRAY));
        ItemStackUtils.setLore(item, List.of(Utils.text(hint, NamedTextColor.DARK_GRAY)));
        return item;
    }
}
