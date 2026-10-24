package com.roguesmp.gui;

import com.roguesmp.enchant.Enchants;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.DurabilityComponent;
import com.roguesmp.item.component.impl.DurabilityRepairComponent;
import com.roguesmp.item.component.impl.EnchantComponent;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.ItemRef;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemRepairGui extends BaseGui {

    private static final int ITEM_SLOT = 11;
    private static final int MATERIAL_SLOT = 13;
    private static final int BUTTON_SLOT = 15;

    private ItemStack itemStack;
    private ItemStack repairMaterialItemStack;

    private ItemStack inputInfoItem;
    private ItemStack repairMaterialInfoItem;
    private ItemStack buttonItem;

    private final Player player;
    private final SmpPlayer smpPlayer;

    public ItemRepairGui(Player player) {
        super(Component.text("Sửa chữa vật phẩm", NamedTextColor.DARK_GRAY), 3);
        this.player = player;
        this.smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);

        inputInfoItem = ItemStack.of(Material.MINECART);
        inputInfoItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Click vật phẩm cần sửa trong túi đồ", NamedTextColor.GREEN));

        repairMaterialInfoItem = ItemStack.of(Material.PAPER);
        repairMaterialInfoItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Click nguyên liệu sửa trong túi đồ", NamedTextColor.GREEN));

        buttonItem = ItemStack.of(Material.ANVIL);
        buttonItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Click để sửa!", NamedTextColor.GREEN));
        buttonItem.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(Utils.text("<!> Nguyên liệu sửa sẽ bị tiêu hao! <!>", NamedTextColor.RED))));
    }

    @Override
    public void setup() {
        clearUi();

        if (itemStack != null) {
            addButton(ITEM_SLOT, itemStack, event -> {
                event.setCancelled(true);
                returnItem(ITEM_SLOT, itemStack, player);
            });
        } else {
            addButton(ITEM_SLOT, inputInfoItem, ClickHandler.noAction());
        }

        if (repairMaterialItemStack != null) {
            addButton(MATERIAL_SLOT, repairMaterialItemStack, event -> {
                event.setCancelled(true);
                returnItem(MATERIAL_SLOT, repairMaterialItemStack, player);
            });
        } else {
            addButton(MATERIAL_SLOT, buildMaterialInfoItem(), ClickHandler.noAction());
        }

        addButton(BUTTON_SLOT, buttonItem, event -> {
            event.setCancelled(true);
            repairItem();
        });

        fillEmpty(FILLER_BLACK);
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (!ItemStackUtils.isValidItem(clicked)) return;

        SmpItem clickedSmp = SmpItem.wrap(clicked, smpPlayer);

        if (isRepairable(clickedSmp)) {
            if (isIrreparable(clickedSmp)) {
                event.getWhoClicked().sendMessage(Component.text("Vật phẩm này bị nguyền, không thể sửa chữa!", NamedTextColor.RED));
                event.getWhoClicked().playSound(Sound.sound(SoundEventKeys.ENTITY_SHULKER_HURT_CLOSED, Sound.Source.PLAYER, 1f, 1f));
                return;
            }

            if (itemStack != null) {
                PlayerUtils.giveItem(player, itemStack);
            }
            event.getClickedInventory().setItem(event.getSlot(), null);
            itemStack = clicked.clone();
            setup();
            return;
        }

        if (itemStack != null && findRepairMaterial(ItemRef.idOf(clicked)) != null) {
            if (repairMaterialItemStack != null) {
                PlayerUtils.giveItem(player, repairMaterialItemStack);
            }
            event.getClickedInventory().setItem(event.getSlot(), null);
            repairMaterialItemStack = clicked.clone();
            setup();
            return;
        }

        event.getWhoClicked().playSound(Sound.sound(SoundEventKeys.ENTITY_SHULKER_HURT_CLOSED, Sound.Source.PLAYER, 1f, 1f));
        event.getWhoClicked().sendMessage(Component.text("Vật phẩm này không cần sửa!", NamedTextColor.RED));
    }

    private void repairItem() {
        if (repairMaterialItemStack == null || itemStack == null) {
            player.sendMessage(Component.text("Vui lòng bỏ đầy đủ vật phẩm", NamedTextColor.RED));
            player.playSound(Sound.sound(SoundEventKeys.ENTITY_SHULKER_HURT_CLOSED, Sound.Source.PLAYER, 1f, 1f));
            return;
        }

        SmpItem targetItem = SmpItem.wrap(itemStack, smpPlayer);
        DurabilityComponent durabilityComponent = targetItem.getComponent(ItemComponentKeys.DURABILITY);
        DurabilityRepairComponent.RepairMaterial material = findRepairMaterial(ItemRef.idOf(repairMaterialItemStack));

        if (durabilityComponent == null) return;
        if (material == null) {
            player.sendMessage(Component.text("Nguyên liệu này không thể sửa vật phẩm này", NamedTextColor.RED));
            player.playSound(Sound.sound(SoundEventKeys.ENTITY_SHULKER_HURT_CLOSED, Sound.Source.PLAYER, 1f, 1f));
            return;
        }

        int current = durabilityComponent.currentDurability();
        int max = durabilityComponent.maxDurability();
        int repairAmount = Math.max(1, (int) Math.round(max * material.percent() / 100.0));

        if (current == max) {
            player.sendMessage(Component.text("Vật phẩm này không cần sửa thêm nữa", NamedTextColor.RED));
            player.playSound(Sound.sound(SoundEventKeys.ENTITY_SHULKER_HURT_CLOSED, Sound.Source.PLAYER, 1f, 1f));
            return;
        }

        durabilityComponent.setCurrentDurability(Math.min(max, current + repairAmount));

        itemStack = targetItem.generateItemStack(smpPlayer, 1);

        if (repairMaterialItemStack.getAmount() > 1) {
            repairMaterialItemStack.setAmount(repairMaterialItemStack.getAmount() - 1);
        } else {
            repairMaterialItemStack = null;
        }

        player.playSound(Sound.sound(SoundEventKeys.BLOCK_ANVIL_USE, Sound.Source.PLAYER, 1f, 1f));
        setup();
    }

    private void returnItem(int slot, ItemStack stack, Player player) {
        PlayerUtils.giveItem(player, stack);
        switch (slot) {
            case ITEM_SLOT -> itemStack = null;
            case MATERIAL_SLOT -> repairMaterialItemStack = null;
        }
        setup();
    }

    private ItemStack buildMaterialInfoItem() {
        if (itemStack == null) return repairMaterialInfoItem;
        DurabilityRepairComponent repairComponent = SmpItem.wrap(itemStack, smpPlayer).getComponent(ItemComponentKeys.DURABILITY_REPAIR);

        List<Component> lore = new ArrayList<>();
        if (repairComponent == null || repairComponent.getMaterials().isEmpty()) {
            lore.add(Utils.text("Vật phẩm này không có nguyên liệu sửa chữa", NamedTextColor.RED));
        } else {
            lore.add(Utils.text("Nguyên liệu có thể dùng:", NamedTextColor.GRAY));
            for (DurabilityRepairComponent.RepairMaterial material : repairComponent.getMaterials()) {
                lore.add(Utils.text(" - ", NamedTextColor.DARK_GRAY)
                        .append(ItemRef.displayName(material.item()).colorIfAbsent(NamedTextColor.WHITE))
                        .append(Utils.text(" (Hồi " + Utils.formatDecimal(material.percent()) + "% độ bền)", NamedTextColor.GRAY)));
            }
        }

        ItemStack info = repairMaterialInfoItem.clone();
        info.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return info;
    }

    private DurabilityRepairComponent.RepairMaterial findRepairMaterial(String materialId) {
        if (itemStack == null) return null;
        DurabilityRepairComponent repairComponent = SmpItem.wrap(itemStack, smpPlayer).getComponent(ItemComponentKeys.DURABILITY_REPAIR);
        return repairComponent == null ? null : repairComponent.find(materialId);
    }

    private boolean isRepairable(SmpItem smpItem) {
        if (smpItem.getBaseItem() == null) return false;
        DurabilityComponent durabilityComponent = smpItem.getComponent(ItemComponentKeys.DURABILITY);
        if (durabilityComponent == null) return false;
        return durabilityComponent.currentDurability() < durabilityComponent.maxDurability();
    }

    private boolean isIrreparable(SmpItem smpItem) {
        EnchantComponent enchantComponent = smpItem.getComponent(ItemComponentKeys.ENCHANT);
        return enchantComponent != null && enchantComponent.getTotalLevel(Enchants.IRREPARABLE) > 0;
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        if (itemStack != null) {
            PlayerUtils.giveItem(player, itemStack);
        }
        if (repairMaterialItemStack != null) {
            PlayerUtils.giveItem(player, repairMaterialItemStack);
        }
    }

    public static void registerCommand() {
        new CommandAPICommand("repairgui")
                .executesPlayer((player, args) -> {
                    new ItemRepairGui(player).showInventory(player);
                })
                .register();
    }
}
