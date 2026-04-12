package com.roguesmp.gui.gem;

import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.GemSocketComponent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.SmpItemUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class GemSocketGui extends BaseGui {

    private final Player player;
    private final SmpPlayer smpPlayer;

    private final int EQUIPMENT_SLOT = 19;
    private final int[] POSSIBLE_GEM_SLOTS = {21, 22, 23, 24, 25, 30, 31, 32, 33, 34}; //Surely 10 is the most we will have

    private ItemStack targetItem;

    private final ItemStack emptySocketItem;
    private final ItemStack inputItem;

    private static final ItemStack BORDER = ItemStackUtils.hideTooltip(ItemStack.of(Material.BLACK_STAINED_GLASS_PANE));

    public GemSocketGui(SmpPlayer smpPlayer) {
        super(Component.text("Khảm ngọc", NamedTextColor.DARK_AQUA), 6);
        this.player = smpPlayer.getBukkitPlayer();
        this.smpPlayer = smpPlayer;

        emptySocketItem = ItemStack.of(Material.YELLOW_STAINED_GLASS_PANE); // Looks like an empty socket
        emptySocketItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Ô ngọc trống", NamedTextColor.GRAY));
        emptySocketItem.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(Utils.text("Click ngọc trong túi đồ để khảm", NamedTextColor.GRAY))));

        inputItem = ItemStack.of(Material.MAGENTA_STAINED_GLASS_PANE);
        inputItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Click đồ cần khảm trong túi đồ", NamedTextColor.GREEN));
    }

    @Override
    public void setup() {
        drawBackground();
        if (!ItemStackUtils.isValidItem(targetItem)) {
            addItem(EQUIPMENT_SLOT, inputItem);
        } else {
            addButton(EQUIPMENT_SLOT, targetItem, event -> {
                event.setCancelled(true);
                Map<Integer, ItemStack> leftOver = event.getWhoClicked().getInventory().addItem(targetItem);
                leftOver.values().forEach(item -> event.getWhoClicked().getWorld().dropItemNaturally(event.getWhoClicked().getLocation(), item));
                this.targetItem = null;
                setup();
            });
        }


        renderGemSlot();
    }

    private void renderGemSlot() {
        if (!ItemStackUtils.isValidItem(targetItem)) return;

        SmpItem smpItem = new SmpItem(targetItem);
        GemSocketComponent socketComp = smpItem.getComponent(ComponentKeys.GEM_SOCKET);

        if (socketComp == null) return;

        int maxSlots = socketComp.getSocketCount();
        List<BaseItem> currentGems = socketComp.getAppliedItem();

        for (int i = 0; i < maxSlots; i++) {
            if (i >= POSSIBLE_GEM_SLOTS.length) break;
            int slot = POSSIBLE_GEM_SLOTS[i];

            if (i < currentGems.size()) {
                // Occupied Slot
                BaseItem gem = currentGems.get(i);
                addButton(slot, gem.generateItemStack(smpPlayer, 1), event -> {
                    event.setCancelled(true);
                    handleGemRemoval(gem);
                });
            } else {
                addButton(slot, emptySocketItem, ClickHandler.noAction());
            }

        }
    }

    private void handleGemRemoval(BaseItem gem) {
        targetItem = SmpItemUtils.removeGem(targetItem, smpPlayer, gem.getId()).itemStack();
        player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1.5f);
        setup();
        PlayerUtils.giveItem(player, gem.generateItemStack(smpPlayer, 1));
    }

    private void handleGemAddition(ItemStack gemStack) {
        if (!ItemStackUtils.isValidItem(targetItem)) return;

        String gemId = ItemStackUtils.getId(gemStack);
        if (gemId == null) return;

        SmpItemUtils.Result<String> result = SmpItemUtils.addGem(targetItem, smpPlayer, gemId);

        if (result.success()) {
            gemStack.setAmount(gemStack.getAmount() - 1);
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f);
            targetItem = result.itemStack();
            setup();
        } else {
            player.sendMessage(Component.text(result.message(), NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (!ItemStackUtils.isValidItem(clicked)) return;
        handleGemAddition(clicked);
        if (!ItemStackUtils.isValidItem(targetItem)) {
            player.getInventory().setItem(event.getSlot(), null);
            this.targetItem = clicked;
            setup();
        }
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        if (ItemStackUtils.isValidItem(targetItem)) {
            PlayerUtils.giveItem(player, targetItem);
        }
    }

    private void drawBackground() {
        // Draw a frame around the 6-row inventory
        for (int i = 0; i < 54; i++) {
            // Frame logic: first/last row or first/last column
            addButton(i, BORDER, ClickHandler.noAction());
        }
    }
}
