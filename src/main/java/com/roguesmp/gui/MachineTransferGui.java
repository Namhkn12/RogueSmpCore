package com.roguesmp.gui;

import com.roguesmp.block.impl.SmpMachine;
import com.roguesmp.constant.TransferMode;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MachineTransferGui extends BaseGui{

    private final ItemStack SETTING_FILLER = ItemStackUtils.hideTooltip(ItemStack.of(Material.GREEN_STAINED_GLASS_PANE));
    private final ItemStack FILLER = ItemStackUtils.hideTooltip(ItemStack.of(Material.LIGHT_BLUE_STAINED_GLASS_PANE));
    private final ItemStack BACK = ItemStack.of(Material.ARROW);
    private final ItemStack INFO = ItemStack.of(Material.REDSTONE_TORCH);
    private final int[] settingSlots = {19,20,21,23,24,25};
    private final BlockFace[] mappedFaces = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH,
            BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };
    private final BaseGui previous;
    private final SmpMachine machine;
    private final int row;

    public MachineTransferGui(SmpMachine machine, BaseGui previous) {
        super(Utils.fromString("Cài đặt"), 4);
        this.row = 4;
        this.previous = previous;
        this.machine = machine;

        itemDecoration();
        setup();
    }

    @Override
    public void setup() {
        fillGuiSides();

        this.addButton(0, BACK, event -> {
            event.setCancelled(true);

            BaseGui gui = machine.getGui();

            event.getWhoClicked().openInventory(gui.getInventory());
        });
        this.addButton(8, INFO, ClickHandler.noAction());

        for(int i=0; i<settingSlots.length; i++){
            int slot = settingSlots[i];
            BlockFace face = mappedFaces[i];

            updateSettingButton(slot, face);
        }
    }

    private void itemDecoration(){
        BACK.setData(DataComponentTypes.ITEM_NAME, Component.text("Quay lại", NamedTextColor.GREEN));
        ItemStackUtils.setItemName(INFO, Component.text("Lưu ý", NamedTextColor.RED));
        ItemStackUtils.setLore(INFO, List.of(
                Component.text("Mỗi một hướng có cài đặt riêng gồm 4 cài đăt", NamedTextColor.WHITE),
                Component.text("None: Không làm gì cả", NamedTextColor.WHITE),
                Component.text("Auto push: Tự động đẩy item ra inventory ở hướng đó", NamedTextColor.WHITE),
                Component.text("Auto pull: Tự động lấy item từ inventory ở hướng đó", NamedTextColor.WHITE),
                Component.text("Smart pull: Như auto pull nhưng giữ 1 stack của mỗi item", NamedTextColor.WHITE)
        ));
    }

    private void fillGuiSides(){
        for(int i=0; i<9; i++){
            this.addButton(i, SETTING_FILLER, ClickHandler.noAction());
        }
        for(int i=1; i<row; i++){
            for(int j=0; j<9; j++){
                int slot = i * 9 + j;
                this.addButton(slot, FILLER, ClickHandler.noAction());
            }
        }
    }

    private void updateSettingButton(int slot, BlockFace face) {
        TransferMode currentMode = machine.getTransferMode(face);

        // 1. Tạo hình ảnh (ItemStack) đại diện cho Mode hiện tại
        ItemStack displayItem = createDisplayItemForMode(face, currentMode);

        // 2. Gắn nút vào GUI với Event Click
        this.addButton(slot, displayItem, event -> {
            event.setCancelled(true);

            // Lấy mode tiếp theo
            TransferMode nextMode = event.isShiftClick() ? TransferMode.NONE : currentMode.next();

            // Cập nhật vào dữ liệu của máy
            machine.setTransferMode(face, nextMode);

            // Đệ quy gọi lại hàm này để render lại nút hiển thị mới
            updateSettingButton(slot, face);

            // Ép Inventory cập nhật hình ảnh ngay lập tức cho người chơi đang xem
            event.getInventory().setItem(slot, createDisplayItemForMode(face, nextMode));
        });
    }

    private ItemStack createDisplayItemForMode(BlockFace face, TransferMode mode) {
        Material mat;
        Component modeText;
        NamedTextColor color;

        switch (mode) {
            case AUTO_PUSH -> { mat = Material.DISPENSER; color = NamedTextColor.GOLD; modeText = Component.text("Auto Push"); }
            case AUTO_PULL -> { mat = Material.HOPPER; color = NamedTextColor.GOLD; modeText = Component.text("Auto Pull"); }
            case SMART_PULL -> { mat = Material.HOPPER_MINECART; color = NamedTextColor.GOLD; modeText = Component.text("Smart Pull"); }
            default -> { mat = Material.STONE_BUTTON; color = NamedTextColor.GRAY; modeText = Component.text("None"); }
        }

        ItemStack item = new ItemStack(mat);

        // Tên item là Hướng (VD: Hướng BẮC (NORTH))
        item.setData(DataComponentTypes.ITEM_NAME,
                Component.text("Hướng: " + face.name()).color(NamedTextColor.YELLOW)
        );

        item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        // Lore chỉ ra trạng thái hiện tại
        ItemStackUtils.setLore(item, List.of(
                Component.empty(),
                Component.text("Trạng thái: ").color(NamedTextColor.WHITE).append(modeText.color(color)).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("▶ Click để thay đổi").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                Component.text("▶ Shift + Click để về cài đặt gốc").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
        ));

        return item;
    }
}
