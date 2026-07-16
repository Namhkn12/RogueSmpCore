package com.roguesmp.gui.gem;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.EquipAttributeComponent;
import com.roguesmp.item.component.impl.GemDataComponent;
import com.roguesmp.item.component.impl.GemSocketComponent;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.ItemRegistry;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.SmpItemUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.modification.GemSocketUtil;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class GemSocketingGui extends BaseGui {

    private static final int ITEM_ROW = 1, ITEM_COL = 2;
    private static final int GEM_ROW = 1, GEM_COL = 4;
    private static final int ACTION_ROW = 1, ACTION_COL = 6;

    // Slots to display currently socketed gems
    private static final int REMOVE_ROW = 2;

    private final Player player;
    private final SmpPlayer smpPlayer;

    private ItemStack targetItemStack = null;
    private ItemStack gemItemStack = null;

    private boolean isProcessing = false;

    public GemSocketingGui(Player player) {
        super(Component.text("Khảm / Tháo Ngọc", NamedTextColor.DARK_PURPLE), 3);
        this.player = player;
        this.smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
    }

    @Override
    public void setup() {
        clearUi();

        // 1. Confirm Button (Socketing Action)
        ItemStack actionButton = determineActionButton();
        addButton(ACTION_ROW, ACTION_COL, actionButton, this::handleActionClick);

        // 2. Target Item Slot
        if (targetItemStack != null) {
            addButton(ITEM_ROW, ITEM_COL, targetItemStack, event -> handleTakeOut(event, true));

            // Render dynamically calculated gem removal slots ONLY when an item is placed
            renderSocketedGems();
        } else {
            ItemStack equipStandIn = ItemStack.of(Material.ARMOR_STAND);
            equipStandIn.setData(DataComponentTypes.ITEM_NAME, Component.text("Click trang bị cần khảm/tháo trong túi", NamedTextColor.GREEN));
            addButton(ITEM_ROW, ITEM_COL, equipStandIn, ClickHandler.noAction());

            // Row 2 is left entirely empty here, meaning it automatically fills with FILLER_BLACK below!
        }

        // 3. Gem Slot
        if (gemItemStack != null) {
            addButton(GEM_ROW, GEM_COL, gemItemStack, event -> handleTakeOut(event, false));
        } else {
            ItemStack gemStandIn = ItemStack.of(Material.PAPER);
            gemStandIn.setData(DataComponentTypes.ITEM_NAME, Component.text("Click ngọc cần khảm trong túi", NamedTextColor.GREEN));
            addButton(GEM_ROW, GEM_COL, gemStandIn, ClickHandler.noAction());
        }

        // Fills all unused slots (including Row 2 if no item is present) with your background pane
        fillEmpty(FILLER_BLACK);
    }

    /**
     * Renders the active socketed gems centering them dynamically on Row 2
     * according to the item's maximum socket capacity.
     */
    private void renderSocketedGems() {
        SmpItem targetItem = SmpItem.wrap(targetItemStack);
        List<String> activeGems = GemSocketUtil.getSocketedGems(targetItem);
        int maxSlots = GemSocketUtil.getMaxSlots(targetItem);

        if (maxSlots <= 0) return;

        // Ensure we don't exceed the chest row width (9 slots maximum)
        int slotsToDraw = Math.min(maxSlots, 9);

        // Calculate the starting column to keep them perfectly centered
        int startCol = (9 - slotsToDraw) / 2;

        for (int i = 0; i < slotsToDraw; i++) {
            int currentCol = startCol + i;

            if (i < activeGems.size()) {
                // There is a gem active in this slot
                String gemId = activeGems.get(i);
                BaseItem baseItem = ItemRegistry.getInstance().getBaseItem(gemId);

                if (baseItem != null) {
                    ItemStack gemDisplay = baseItem.generateItemStack(smpPlayer, 1);
                    gemDisplay.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                            Component.empty(),
                            Utils.text("Click vào đây để tháo ngọc này!", NamedTextColor.RED, TextDecoration.BOLD)
                    )));

                    addButton(REMOVE_ROW, currentCol, gemDisplay, event -> handleRemoveGemClick(event, gemId));
                }
            } else {
                // Empty but unlocked socket slot on the active item
                ItemStack openSlot = new ItemStack(Material.MINECART);
                openSlot.setData(DataComponentTypes.ITEM_NAME, Component.text("Lỗ khảm còn trống", NamedTextColor.GRAY));
                addButton(REMOVE_ROW, currentCol, openSlot, ClickHandler.noAction());
            }
        }
    }

    /**
     * Handles clicking on a socketed gem to remove it from the gear.
     */
    private void handleRemoveGemClick(InventoryClickEvent event, String gemId) {
        event.setCancelled(true);
        if (isProcessing || targetItemStack == null) return;

        SmpItem targetItem = SmpItem.wrap(targetItemStack);

        // Attempt removing gem
        boolean success = GemSocketUtil.removeGem(targetItem, gemId);
        if (success) {
            // Update the source itemstack references
            int originalAmount = targetItemStack.getAmount();
            this.targetItemStack = targetItem.generateItemStack(smpPlayer, originalAmount);

            // Refund the removed gem back to the player
            BaseItem gemBase = ItemRegistry.getInstance().getBaseItem(gemId);
            if (gemBase != null) {
                PlayerUtils.giveItem(player, gemBase.generateItemStack(smpPlayer, 1));
            }

            player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.2f);
            player.sendMessage(Component.text("Tháo ngọc thành công!", NamedTextColor.GREEN));

            setup();
        } else {
            player.sendMessage(Component.text("Có lỗi xảy ra khi tháo ngọc!", NamedTextColor.RED));
        }
    }

    private ItemStack determineActionButton() {
        if (targetItemStack == null || gemItemStack == null) {
            ItemStack missing = new ItemStack(Material.BARRIER);
            missing.setData(DataComponentTypes.ITEM_NAME, Component.text("Hãy bỏ Trang Bị và Ngọc!", NamedTextColor.RED));
            return missing;
        }

        SmpItem targetItem = SmpItem.wrap(targetItemStack);
        SmpItem gemItem = SmpItem.wrap(gemItemStack);
        String gemId = gemItem.getBaseItem().getId();

        if (!GemSocketUtil.isSocketable(targetItem)) {
            ItemStack invalid = new ItemStack(Material.BARRIER);
            invalid.setData(DataComponentTypes.ITEM_NAME, Component.text("Trang bị này không có lỗ khảm!", NamedTextColor.RED));
            return invalid;
        }

        if (!GemSocketUtil.isValidGem(gemId)) {
            ItemStack invalid = new ItemStack(Material.BARRIER);
            invalid.setData(DataComponentTypes.ITEM_NAME, Component.text("Vật phẩm khảm không phải là ngọc hợp lệ!", NamedTextColor.RED));
            return invalid;
        }

        if (!GemSocketUtil.isGemCompatible(targetItem, gemId)) {
            ItemStack invalid = new ItemStack(Material.BARRIER);
            invalid.setData(DataComponentTypes.ITEM_NAME, Component.text("Ngọc này không tương thích với trang bị!", NamedTextColor.RED));
            return invalid;
        }

        if (!GemSocketUtil.hasAvailableSlots(targetItem)) {
            ItemStack full = new ItemStack(Material.BARRIER);
            full.setData(DataComponentTypes.ITEM_NAME, Component.text("Trang bị đã hết lỗ khảm trống!", NamedTextColor.RED));
            return full;
        }

        GemDataComponent gemData = gemItem.getComponent(ComponentKeys.GEM_DATA);
        double successChance = (gemData != null) ? gemData.getSuccessChance() : 1.0;

        ItemStack confirm = new ItemStack(Material.ANVIL);
        confirm.setData(DataComponentTypes.ITEM_NAME, Component.text("Tiến Hành Khảm Ngọc", NamedTextColor.GREEN));
        confirm.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Tỉ lệ thành công: ", NamedTextColor.GRAY)
                        .append(Component.text(Utils.formatDecimal(successChance * 100) + "%", NamedTextColor.GOLD, TextDecoration.BOLD))
        )));
        return confirm;
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (!ItemStackUtils.isValidItem(clicked)) return;

        SmpItem smpItem = SmpItem.wrap(clicked);

        if (smpItem.hasComponent(ComponentKeys.GEM_DATA) && gemItemStack == null) {
            gemItemStack = clicked.clone();
            gemItemStack.setAmount(1);
            clicked.setAmount(clicked.getAmount() - 1);
        } else if (targetItemStack == null) {
            targetItemStack = clicked.clone();
            targetItemStack.setAmount(1);
            clicked.setAmount(clicked.getAmount() - 1);
        }

        setup();
    }

    private void handleTakeOut(InventoryClickEvent event, boolean isTargetItem) {
        event.setCancelled(true);
        if (isProcessing) return;
        ItemStack itemToReturn = isTargetItem ? targetItemStack : gemItemStack;

        if (itemToReturn == null) return;

        PlayerUtils.giveItem(player, itemToReturn);

        if (isTargetItem) targetItemStack = null;
        else gemItemStack = null;

        setup();
    }

    private void handleActionClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (isProcessing || targetItemStack == null || gemItemStack == null) return;

        SmpItem targetItem = SmpItem.wrap(targetItemStack);
        SmpItem gemItem = SmpItem.wrap(gemItemStack);
        String gemId = gemItem.getBaseItem().getId();

        if (!GemSocketUtil.canSocketGem(targetItem, gemId, false)) {
            player.sendMessage(Component.text("Không thể thực hiện khảm ngọc trên trang bị này!", NamedTextColor.RED));
            return;
        }

        isProcessing = true;

        Material[] animationFrames = {
                Material.ORANGE_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE,
                Material.WHITE_STAINED_GLASS_PANE
        };

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (player.getOpenInventory().getTopInventory().getHolder() != GemSocketingGui.this) {
                    cancel();
                    isProcessing = false;
                    return;
                }

                if (ticks < 16) {
                    ItemStack loadingItem = new ItemStack(animationFrames[ticks % animationFrames.length]);
                    loadingItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Đang khảm ngọc...", NamedTextColor.GOLD));

                    GemSocketingGui.this.addItem(ACTION_ROW, ACTION_COL, loadingItem);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
                    ticks++;
                } else {
                    cancel();
                    executeSocketResult(targetItem, gemItem);
                    isProcessing = false;
                }
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 2L);
    }

    private void executeSocketResult(SmpItem targetItem, SmpItem gemItem) {
        String gemId = gemItem.getBaseItem().getId();
        GemSocketUtil.SocketResult result = GemSocketUtil.addGem(targetItem, gemId, true, false);

        int originalAmount = targetItemStack.getAmount();
        this.gemItemStack = null; // Consume the gem

        switch (result) {
            case SUCCESS -> {
                this.targetItemStack = targetItem.generateItemStack(smpPlayer, originalAmount);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
                player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                player.sendMessage(Component.text("Khảm ngọc thành công!", NamedTextColor.GREEN));
            }
            case ERROR_FAILED_ROLL -> {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
                player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);
                player.sendMessage(Component.text("Khảm ngọc thất bại! Ngọc đã bị vỡ vụn.", NamedTextColor.RED));
            }
            case ERROR_NOT_SOCKETABLE -> {
                player.sendMessage(Component.text("Vật phẩm này không hỗ trợ khảm ngọc!", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
            case ERROR_NOT_A_GEM -> {
                player.sendMessage(Component.text("Vật phẩm khảm không phải là ngọc!", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
            case ERROR_SLOT_INCOMPATIBLE -> {
                player.sendMessage(Component.text("Ngọc này không tương thích với loại trang bị này!", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
            case ERROR_NO_SLOTS_AVAILABLE -> {
                player.sendMessage(Component.text("Trang bị đã hết lỗ khảm trống!", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
        }

        setup();
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        isProcessing = false;

        if (targetItemStack != null) {
            PlayerUtils.giveItem(player, targetItemStack);
            targetItemStack = null;
        }
        if (gemItemStack != null) {
            PlayerUtils.giveItem(player, gemItemStack);
            gemItemStack = null;
        }
    }
}
