package com.roguesmp.player.trade.gui;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.trade.PlayerTradeSession;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PlayerTradeGui extends BaseGui {

    private static final int ROWS = 6;
    private static final int OFFER_ROWS = 5;
    private static final int OFFER_COLUMNS = 4;
    private static final int PARTNER_COLUMN_OFFSET = 5;
    private static final int DIVIDER_COLUMN = 4;
    private static final int COIN_SLOT = 4 * 9 + 3;
    private static final int COIN_BUTTON_SLOT = 5 * 9;
    private static final int READY_BUTTON_SLOT = 5 * 9 + 2;
    private static final int STATUS_SLOT = 5 * 9 + 4;
    private static final int PARTNER_READY_SLOT = 5 * 9 + 6;
    private static final List<Integer> OFFER_SLOTS = buildOfferSlots();

    private final PlayerTradeSession session;
    private final Player owner;
    private final Player partner;
    private final ItemStack divider = createDecoration(Material.BLACK_STAINED_GLASS_PANE);
    private final ItemStack partnerEmpty = createDecoration(Material.WHITE_STAINED_GLASS_PANE);
    private List<ItemStack> offerSnapshot = snapshotOffer(null);
    private long coins;
    private boolean ready;
    private boolean inDialog;

    public PlayerTradeGui(PlayerTradeSession session, Player owner, Player partner) {
        super(Component.text("Giao dịch với " + partner.getName()), ROWS);
        this.session = session;
        this.owner = owner;
        this.partner = partner;
    }

    private static List<Integer> buildOfferSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int row = 0; row < OFFER_ROWS; row++) {
            for (int col = 0; col < OFFER_COLUMNS; col++) {
                int slot = row * 9 + col;
                if (slot != COIN_SLOT) slots.add(slot);
            }
        }
        return List.copyOf(slots);
    }

    @Override
    public void setup() {
        render();
    }

    public void render() {
        Inventory inventory = getInventory();
        PlayerTradeGui partnerGui = session.getPartnerGui(this);

        for (int row = 0; row < ROWS; row++) {
            addButton(row, DIVIDER_COLUMN, divider, ClickHandler.noAction());
        }
        for (int col = 0; col < 9; col++) {
            addButton(5, col, FILLER_BLACK, ClickHandler.noAction());
        }

        for (int slot : OFFER_SLOTS) {
            ItemStack partnerItem = partnerGui.getInventory().getItem(slot);
            addButton(slot + PARTNER_COLUMN_OFFSET, ItemStackUtils.isValidItem(partnerItem) ? partnerItem.clone() : partnerEmpty, ClickHandler.noAction());
        }
        addButton(COIN_SLOT, coinDisplay(coins), ClickHandler.noAction());
        ItemStack partnerCoins = coinDisplay(partnerGui.coins);
        addButton(COIN_SLOT + PARTNER_COLUMN_OFFSET, partnerCoins == null ? partnerEmpty : partnerCoins, ClickHandler.noAction());

        addButton(COIN_BUTTON_SLOT, coinButton(), event -> {
            event.setCancelled(true);
            openCoinDialog();
        });
        addButton(READY_BUTTON_SLOT, readyButton(), event -> {
            event.setCancelled(true);
            session.toggleReady(this);
        });
        addButton(STATUS_SLOT, statusDisplay(), ClickHandler.noAction());
        addButton(PARTNER_READY_SLOT, partnerReadyDisplay(partnerGui.ready), ClickHandler.noAction());

        inventory.getViewers().forEach(viewer -> {
            if (viewer instanceof Player player) player.updateInventory();
        });
    }

    @Override
    public void onClickTopInventory(InventoryClickEvent event) {
        if (session.isEnded()) {
            event.setCancelled(true);
            return;
        }
        if (OFFER_SLOTS.contains(event.getSlot())) {
            scheduleRefresh();
            return;
        }
        event.setCancelled(true);
        super.onClickTopInventory(event);
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        if (event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) return;
        event.setCancelled(true);
        if (session.isEnded()) return;

        ItemStack moving = event.getCurrentItem();
        if (!ItemStackUtils.isValidItem(moving)) return;
        event.setCurrentItem(insertIntoOffer(moving.clone()));
        scheduleRefresh();
    }

    @Override
    public void onDragInventory(InventoryDragEvent event) {
        int topSize = getInventory().getSize();
        boolean touchesTop = false;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= topSize) continue;
            if (session.isEnded() || !OFFER_SLOTS.contains(rawSlot)) {
                event.setCancelled(true);
                return;
            }
            touchesTop = true;
        }
        if (touchesTop) scheduleRefresh();
    }

    @Override
    protected void onContentsSettled() {
        if (session.isEnded()) return;
        if (syncOfferSnapshot()) session.onOfferChanged(this);
    }

    public boolean syncOfferSnapshot() {
        List<ItemStack> current = snapshotOffer(getInventory());
        if (current.equals(offerSnapshot)) return false;
        offerSnapshot = current;
        return true;
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        if (inDialog) return;
        session.onClosed(this);
    }

    private ItemStack insertIntoOffer(ItemStack item) {
        Inventory inventory = getInventory();
        for (int slot : OFFER_SLOTS) {
            ItemStack existing = inventory.getItem(slot);
            if (existing == null || !existing.isSimilar(item)) continue;
            int transfer = Math.min(existing.getMaxStackSize() - existing.getAmount(), item.getAmount());
            if (transfer <= 0) continue;
            existing.add(transfer);
            item.subtract(transfer);
            if (item.isEmpty()) return null;
        }
        for (int slot : OFFER_SLOTS) {
            if (ItemStackUtils.isValidItem(inventory.getItem(slot))) continue;
            inventory.setItem(slot, item);
            return null;
        }
        return item;
    }

    private void openCoinDialog() {
        if (session.isCountingDown() || ready) session.onOfferChanged(this);

        Dialog dialog = DialogBuilder.create(Component.text("Thêm xu vào giao dịch"))
                .canCloseWithEscape(false)
                .addTextInput("amount", Component.text("Số xu"), b -> b.initial(Long.toString(coins)))
                .confirmation()
                .yesButton(Component.text("Xác nhận"), null, (response, audience) -> {
                    Long amount = parseCoins(response.getText("amount"));
                    Utils.runLater(() -> {
                        if (amount != null) setCoins(amount);
                        reopenAfterDialog();
                    });
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(this::reopenAfterDialog))
                .build();

        inDialog = true;
        owner.closeInventory();
        owner.showDialog(dialog);
    }

    private Long parseCoins(String text) {
        try {
            long amount = Long.parseLong(text == null ? "" : text.trim());
            if (amount >= 0) return amount;
        } catch (NumberFormatException ignored) {
        }
        owner.sendMessage(Component.text("Số xu không hợp lệ.", NamedTextColor.RED));
        return null;
    }

    private void setCoins(long amount) {
        if (session.isEnded() || amount == coins) return;
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(owner);
        if (amount > 0 && (smpPlayer == null || !smpPlayer.hasMoney(amount))) {
            owner.sendMessage(Component.text("Bạn không có đủ xu.", NamedTextColor.RED));
            return;
        }
        coins = amount;
        session.onOfferChanged(this);
    }

    private void reopenAfterDialog() {
        inDialog = false;
        if (session.isEnded()) return;
        showInventory(owner);
    }

    public void closeIfViewing() {
        if (inDialog) {
            inDialog = false;
            owner.closeDialog();
            return;
        }
        if (owner.getOpenInventory().getTopInventory().getHolder(false) == this) owner.closeInventory();
    }

    public List<ItemStack> takeOfferedItems() {
        Inventory inventory = getInventory();
        List<ItemStack> items = new ArrayList<>();
        for (int slot : OFFER_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (ItemStackUtils.isValidItem(item)) items.add(item);
            inventory.setItem(slot, null);
        }
        offerSnapshot = snapshotOffer(inventory);
        return items;
    }

    private static List<ItemStack> snapshotOffer(Inventory inventory) {
        List<ItemStack> snapshot = new ArrayList<>(OFFER_SLOTS.size());
        for (int slot : OFFER_SLOTS) {
            ItemStack item = inventory == null ? null : inventory.getItem(slot);
            snapshot.add(ItemStackUtils.isValidItem(item) ? item.clone() : null);
        }
        return snapshot;
    }

    private ItemStack coinDisplay(long amount) {
        if (amount <= 0) {
            ItemStack filler = ItemStack.of(Material.YELLOW_STAINED_GLASS_PANE);
            filler.setData(DataComponentTypes.ITEM_NAME, Component.text("Xu"));
            filler.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(Utils.text("Khi giao dịch có xu sẽ xuất hiện số lượng ở đây", NamedTextColor.GOLD))));
            return filler;
        }
        ItemStack item = ItemStack.of(Material.GOLD_NUGGET);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text(Utils.formatMoney(amount) + " xu", NamedTextColor.GOLD));
        return item;
    }

    private ItemStack coinButton() {
        ItemStack item = ItemStack.of(Material.SUNFLOWER);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Thêm xu", NamedTextColor.GOLD));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Đang đưa: " + Utils.formatMoney(coins) + " xu", NamedTextColor.GRAY),
                Utils.text("Nhấn để thay đổi số xu.", NamedTextColor.YELLOW)
        )));
        return item;
    }

    private ItemStack readyButton() {
        ItemStack item = ItemStack.of(ready ? Material.LIME_CONCRETE : Material.RED_CONCRETE);
        item.setData(DataComponentTypes.ITEM_NAME, ready
                ? Component.text("Đã xác nhận", NamedTextColor.GREEN)
                : Component.text("Xác nhận giao dịch", NamedTextColor.RED));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text(ready ? "Nhấn để huỷ xác nhận." : "Nhấn để đồng ý giao dịch.", NamedTextColor.GRAY)
        )));
        return item;
    }

    private ItemStack partnerReadyDisplay(boolean partnerReady) {
        ItemStack item = ItemStack.of(partnerReady ? Material.LIME_DYE : Material.GRAY_DYE);
        item.setData(DataComponentTypes.ITEM_NAME, partnerReady
                ? Component.text(partner.getName() + " đã xác nhận", NamedTextColor.GREEN)
                : Component.text(partner.getName() + " chưa xác nhận", NamedTextColor.GRAY));
        return item;
    }

    private ItemStack statusDisplay() {
        if (!session.isCountingDown()) {
            ItemStack item = ItemStack.of(Material.CLOCK);
            item.setData(DataComponentTypes.ITEM_NAME, Component.text("Chờ cả hai bên xác nhận", NamedTextColor.GRAY));
            return item;
        }
        int remaining = session.getCountdownRemaining();
        ItemStack item = ItemStack.of(Material.CLOCK, Math.max(1, remaining));
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Hoàn tất sau " + remaining + " giây", NamedTextColor.YELLOW));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Thay đổi vật phẩm sẽ huỷ đếm ngược.", NamedTextColor.GRAY),
                Utils.text("Đóng giao diện sẽ huỷ giao dịch.", NamedTextColor.GRAY)
        )));
        return item;
    }

    public Player getOwner() {
        return owner;
    }

    public long getCoins() {
        return coins;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }
}
