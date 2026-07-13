package com.roguesmp.gui.gem;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.EquipAttributeComponent;
import com.roguesmp.item.component.impl.GemDataComponent;
import com.roguesmp.item.component.impl.GemSocketComponent;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
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

    private final Player player;
    private final SmpPlayer smpPlayer;

    // Track items currently placed into the UI slots
    private ItemStack targetItemStack = null;
    private ItemStack gemItemStack = null;

    private boolean isProcessing = false;

    public GemSocketingGui(Player player) {
        super(Component.text("Khảm Ngọc", NamedTextColor.DARK_PURPLE), 3);
        this.player = player;
        this.smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
    }

    @Override
    public void setup() {
        clearUi();

        ItemStack actionButton = determineActionButton();
        addButton(ACTION_ROW, ACTION_COL, actionButton, this::handleActionClick);

        if (targetItemStack != null) {
            addButton(ITEM_ROW, ITEM_COL, targetItemStack, event -> handleTakeOut(event, true));
        } else {
            ItemStack equipStandIn = ItemStack.of(Material.ARMOR_STAND);
            equipStandIn.setData(DataComponentTypes.ITEM_NAME, Component.text("Click trang bị cần khảm trong túi", NamedTextColor.GREEN));
            addButton(ITEM_ROW, ITEM_COL, equipStandIn, ClickHandler.noAction());
        }

        if (gemItemStack != null) {
            addButton(GEM_ROW, GEM_COL, gemItemStack, event -> handleTakeOut(event, false));
        } else {
            ItemStack gemStandIn = ItemStack.of(Material.PAPER);
            gemStandIn.setData(DataComponentTypes.ITEM_NAME, Component.text("Click ngọc cần khảm trong túi", NamedTextColor.GREEN));
            addButton(GEM_ROW, GEM_COL, gemStandIn, ClickHandler.noAction());
        }

        fillEmpty(FILLER_BLACK);
    }

    /**
     * Determines what item to display in the confirm slot depending on the current inputs.
     */
    private ItemStack determineActionButton() {
        if (targetItemStack == null || gemItemStack == null) {
            ItemStack missing = new ItemStack(Material.BARRIER);
            missing.setData(DataComponentTypes.ITEM_NAME, Component.text("Hãy bỏ Trang Bị hoặc Ngọc!", NamedTextColor.RED));
            return missing;
        }

        SmpItem targetItem = SmpItem.wrap(targetItemStack);
        SmpItem gemItem = SmpItem.wrap(gemItemStack);

        GemSocketComponent socket = targetItem.getComponent(ComponentKeys.GEM_SOCKET);
        GemDataComponent gemData = gemItem.getComponent(ComponentKeys.GEM_DATA);
        if (socket == null || gemData == null) {
            ItemStack invalid = new ItemStack(Material.BARRIER);
            invalid.setData(DataComponentTypes.ITEM_NAME, Component.text("Trang bị không có lỗ hoặc Vật khảm không phải ngọc!", NamedTextColor.RED));
            return invalid;
        }

        EquipAttributeComponent equipAttribute = targetItem.getComponent(ComponentKeys.ATTRIBUTE);
        if (equipAttribute == null || !gemData.getAttributes().containsKey(equipAttribute.getSlot())) {
            ItemStack invalid = new ItemStack(Material.BARRIER);
            invalid.setData(DataComponentTypes.ITEM_NAME, Component.text("Ngọc này không khảm cho trang bị này được!", NamedTextColor.RED));
            return invalid;
        }

        if (!socket.canFitGem()) {
            ItemStack full = new ItemStack(Material.BARRIER);
            full.setData(DataComponentTypes.ITEM_NAME, Component.text("Trang bị đã hết lỗ khảm!", NamedTextColor.RED));
            return full;
        }

        ItemStack confirm = new ItemStack(Material.ANVIL);
        confirm.setData(DataComponentTypes.ITEM_NAME, Component.text(" Tiến Hành Khảm Ngọc", NamedTextColor.GREEN));
        confirm.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(Utils.text("Tỉ lệ thành công: ", NamedTextColor.GRAY).append(Component.text(Utils.formatDecimal(gemData.getSuccessChance() * 100) + "%", NamedTextColor.GOLD, TextDecoration.BOLD)))));
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

    /**
     * Handles taking items back out of the top inventory slots
     */
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

        GemSocketComponent socket = targetItem.getComponent(ComponentKeys.GEM_SOCKET);
        GemDataComponent gemData = gemItem.getComponent(ComponentKeys.GEM_DATA);

        if (socket == null || !socket.canFitGem() || gemData == null) {
            player.sendMessage(Component.text("Không thể thực hiện khảm ngọc!", NamedTextColor.RED));
            return;
        }

        EquipAttributeComponent equipAttribute = targetItem.getComponent(ComponentKeys.ATTRIBUTE);
        if (equipAttribute == null || !gemData.getAttributes().containsKey(equipAttribute.getSlot())) {
            player.sendMessage(Component.text("Ngọc này không khảm cho trang bị này được!", NamedTextColor.RED));
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
                    executeSocketResult(targetItem, gemItem, socket, gemData);
                    isProcessing = false;
                }
            }
        }.runTaskTimer(RogueSmpCore.getInstance(), 0L, 2L);
    }

    /**
     * Handles the calculation of the success chance, consuming the gem, and playing final effects.
     */
    private void executeSocketResult(SmpItem targetItem, SmpItem gemItem, GemSocketComponent socket, GemDataComponent gemData) {
        double roll = Math.random();
        double chance = gemData.getSuccessChance();

        boolean isSuccess = roll <= chance;

        gemItemStack = null;

        if (isSuccess) {
            socket.addGem(gemItem.getBaseItem());

            targetItemStack = targetItem.generateItemStack(smpPlayer, targetItemStack.getAmount());

            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            player.sendMessage(Component.text("Khảm ngọc thành công!", NamedTextColor.GREEN));
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
            player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);
            player.sendMessage(Component.text("Khảm ngọc thất bại! Ngọc đã bị vỡ vụn.", NamedTextColor.RED));
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
