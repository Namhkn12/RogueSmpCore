package com.roguesmp.block.gui;

import com.roguesmp.block.storage.ItemStorage;
import com.roguesmp.block.transfer.SideConfig;
import com.roguesmp.block.transfer.SideMode;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Consumer;

public class SideConfigGui extends BaseGui {

    private static final int FIRST_FACE_SLOT = 10;
    private static final int BACK_SLOT = 18;

    private final ItemStorage storage;
    private final Consumer<HumanEntity> back;

    public SideConfigGui(ItemStorage storage, Consumer<HumanEntity> back) {
        super(Component.text("Cấu hình các mặt"), 3);
        this.storage = storage;
        this.back = back;
    }

    @Override
    public void setup() {
        fillEmpty(FILLER_BLACK);
        List<BlockFace> faces = SideConfig.FACES;
        for (int i = 0; i < faces.size(); i++) renderFace(FIRST_FACE_SLOT + i, faces.get(i));
        renderBack();
    }

    private void renderFace(int slot, BlockFace face) {
        if (storage.isLocked(face)) {
            ItemStack item = ItemStack.of(Material.BARRIER);
            ItemStackUtils.setItemName(item, Utils.text(faceName(face), NamedTextColor.RED));
            ItemStackUtils.setLore(item, List.of(Utils.text("Mặt này không thể cấu hình", NamedTextColor.DARK_GRAY)));
            addButton(slot, item, ClickHandler.noAction());
            return;
        }

        SideMode mode = storage.side(face);
        ItemStack item = ItemStack.of(modeMaterial(mode));
        ItemStackUtils.setItemName(item, Utils.text(faceName(face), NamedTextColor.YELLOW));
        ItemStackUtils.setLore(item, List.of(
                Utils.text("Chế độ: ", NamedTextColor.GRAY).append(Component.text(modeName(mode), NamedTextColor.WHITE)),
                Utils.fromString("<!i><gray><key:key.attack> chế độ tiếp theo"),
                Utils.fromString("<!i><gray><key:key.use> chế độ trước")
        ));
        addButton(slot, item, event -> onFaceClick(event, slot, face));
    }

    private void onFaceClick(InventoryClickEvent event, int slot, BlockFace face) {
        event.setCancelled(true);
        SideMode current = storage.side(face);
        storage.setSide(face, event.isRightClick() ? current.previous() : current.next());

        Player player = (Player) event.getWhoClicked();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        renderFace(slot, face);
    }

    private void renderBack() {
        ItemStack item = ItemStack.of(Material.ARROW);
        ItemStackUtils.setItemName(item, Utils.text("« Quay lại", NamedTextColor.GRAY));
        addButton(BACK_SLOT, item, event -> {
            event.setCancelled(true);
            HumanEntity viewer = event.getWhoClicked();
            Utils.runLater(() -> back.accept(viewer));
        });
    }

    private static String faceName(BlockFace face) {
        return switch (face) {
            case NORTH -> "Mặt Bắc";
            case EAST -> "Mặt Đông";
            case SOUTH -> "Mặt Nam";
            case WEST -> "Mặt Tây";
            case UP -> "Mặt trên";
            case DOWN -> "Mặt dưới";
            default -> face.name();
        };
    }

    private static String modeName(SideMode mode) {
        return switch (mode) {
            case NONE -> "Tắt";
            case PULL -> "Kéo vào";
            case SMART_PULL -> "Kéo thông minh";
            case PUSH -> "Đẩy ra";
            case PULL_PUSH -> "Kéo vào + Đẩy ra";
            case SMART_PULL_PUSH -> "Kéo thông minh + Đẩy ra";
        };
    }

    private static Material modeMaterial(SideMode mode) {
        return switch (mode) {
            case NONE -> Material.GRAY_WOOL;
            case PULL -> Material.LIGHT_BLUE_WOOL;
            case SMART_PULL -> Material.CYAN_WOOL;
            case PUSH -> Material.ORANGE_WOOL;
            case PULL_PUSH -> Material.LIME_WOOL;
            case SMART_PULL_PUSH -> Material.GREEN_WOOL;
        };
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        if (event.isShiftClick()) event.setCancelled(true);
    }

    @Override
    public void onDragInventory(InventoryDragEvent event) {
        event.setCancelled(true);
    }
}
