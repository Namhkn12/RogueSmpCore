package com.roguesmp.gui.ability;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.AbilityType;
import com.roguesmp.player.classes.PlayerClass;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AbilitySelectionGui extends BaseGui {

    private final SmpPlayer smpPlayer;
    private final AbilityType type;
    private final int targetIndex;
    private final List<AbilityInfo<?>> available;

    // Decoration Constants
    private static final ItemStack FILLER_GRAY = createDecoration(Material.GRAY_STAINED_GLASS_PANE);
    private static final ItemStack BORDER_PURPLE = createDecoration(Material.PURPLE_STAINED_GLASS_PANE);

    public AbilitySelectionGui(SmpPlayer smpPlayer, AbilityType type, int index) {
        super(Component.text("Chọn: " + type.getDisplay() + " #" + (index + 1)), 5);
        this.smpPlayer = smpPlayer;
        this.type = type;
        this.targetIndex = index;

        // Every ability in the current class's roster (unlocked or not) - not just unlocked ones -
        // so a not-yet-unlocked ability still shows here (locked, unselectable) rather than being
        // invisible until the player goes and unlocks it elsewhere first.
        PlayerClass playerClass = smpPlayer.getPlayerClass();
        List<AbilityInfo<?>> list = new ArrayList<>();
        if (playerClass != null) {
            for (String id : playerClass.getDefaultAbilities().keySet()) {
                AbilityInfo<?> info = Registries.ABILITY.get(id);
                if (info != null && info.getType() == type) {
                    list.add(info);
                }
            }
        }

        list.sort((a, b) -> {
            boolean aUnlocked = isUnlocked(a.getId());
            boolean bUnlocked = isUnlocked(b.getId());
            if (aUnlocked != bUnlocked) return Boolean.compare(bUnlocked, aUnlocked);

            boolean aEquipped = smpPlayer.getAbilityLoadout().isEquipped(a.getId(), type);
            boolean bEquipped = smpPlayer.getAbilityLoadout().isEquipped(b.getId(), type);
            if (aEquipped != bEquipped) return Boolean.compare(bEquipped, aEquipped);
            return a.getId().compareTo(b.getId());
        });

        this.available = list;
    }

    @Override
    public void setup() {
        clearUi();
        fillEmpty(FILLER_GRAY);

        // --- 1. Draw Border ---
        // Top row (except center for remove button)
        for (int i = 0; i < 9; i++) {
            if (i == 4) continue;
            addItem(0, i, FILLER_BLACK);
        }
        // Bottom row
        for (int i = 0; i < 9; i++) {
            addItem(4, i, FILLER_BLACK);
        }
        // Sides
        for (int i = 1; i < 4; i++) {
            addItem(i, 0, BORDER_PURPLE);
            addItem(i, 8, BORDER_PURPLE);
        }

        // --- 2. Remove Button (Centered top) ---
        addButton(0, 4, createRemoveButton(), event -> {
            event.setCancelled(true);
            smpPlayer.getAbilityLoadout().equip(type, null, targetIndex);
            Utils.runLater(() -> new AbilityLoadoutGui(smpPlayer).showInventory(smpPlayer.getBukkitPlayer()));
        });

        // --- 3. Selection Bank (Starting from index 10 to keep within borders) ---
        for (int i = 0; i < available.size(); i++) {
            AbilityInfo<?> info = available.get(i);
            boolean unlocked = isUnlocked(info.getId());
            int level = smpPlayer.getPlayerData().getAbilityLevel(info.getId());

            // Logic to calculate slot (skipping borders)
            int row = (i / 7) + 1;
            int col = (i % 7) + 1;

            addButton(row, col, createSelectIcon(info, level, unlocked), event -> {
                event.setCancelled(true);

                if (!unlocked) {
                    smpPlayer.getBukkitPlayer().sendMessage(Component.text("⚠ Kĩ năng này chưa được mở khóa! Hãy dùng /ability catalogue để mở khóa.", NamedTextColor.RED));
                    return;
                }

                if (isEquippedElsewhere(info.getId())) {
                    smpPlayer.getBukkitPlayer().sendMessage(Component.text("⚠ Kĩ năng này đã được trang bị ở ô khác!", NamedTextColor.RED));
                    return;
                }

                Ability instance = info.createInstance(smpPlayer, level);
                smpPlayer.getAbilityLoadout().equip(type, instance, targetIndex);
                Utils.runLater(() -> new AbilityLoadoutGui(smpPlayer).showInventory(smpPlayer.getBukkitPlayer()));
            });
        }

        // --- 4. Back Button (Bottom corner) ---
        addButton(4, 0, PREV_PAGE_BUTTON, ClickHandler.openGui(new AbilityLoadoutGui(smpPlayer)));
    }

    private boolean isEquippedElsewhere(String id) {
        Ability[] equipped = smpPlayer.getAbilityLoadout().getAbilities(type);
        for (int i = 0; i < equipped.length; i++) {
            if (i != targetIndex && equipped[i] != null && equipped[i].getId().equals(id)) return true;
        }
        return false;
    }

    private boolean isUnlocked(String abilityId) {
        return smpPlayer.getPlayerData().getAbilityLevel(abilityId) > 0;
    }

    private ItemStack createSelectIcon(AbilityInfo<?> info, int level, boolean unlocked) {
        if (!unlocked) return createLockedIcon(info);

        ItemStack item = ItemStack.of(info.getIcon());
        item.setData(DataComponentTypes.ITEM_NAME, info.getFormattedDisplayName());

        List<Component> lore = new ArrayList<>();

        Ability currentInSlot = smpPlayer.getAbilityLoadout().getAbilities(type)[targetIndex];
        boolean isHere = currentInSlot != null && currentInSlot.getId().equals(info.getId());
        boolean isElsewhere = isEquippedElsewhere(info.getId());

        if (isHere) {
            lore.add(Utils.text("✔ Đang trang bị ở ô này", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));
        } else if (isElsewhere) {
            lore.add(Utils.text("✘ Đang trang bị ở ô khác", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
        } else {
            lore.add(Utils.text("○ Có thể trang bị", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, true));
        }

        lore.add(Utils.text("Phân loại: " + type.getDisplay(), NamedTextColor.DARK_GRAY));
        lore.add(Utils.text("Cấp độ: " + level, NamedTextColor.GOLD));
        lore.add(Component.empty());

        lore.addAll(info.getFormattedDescription(level));

        lore.add(Component.empty());
        if (isElsewhere) {
            lore.add(Utils.text("Hãy gỡ ở vị trí cũ trước", NamedTextColor.RED));
        } else if (!isHere) {
            lore.add(Utils.text("Click để chọn", NamedTextColor.YELLOW));
        }

        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    private ItemStack createLockedIcon(AbilityInfo<?> info) {
        ItemStack item = ItemStack.of(Material.COAL_BLOCK);
        item.setData(DataComponentTypes.ITEM_NAME, info.getFormattedDisplayName().decoration(TextDecoration.STRIKETHROUGH, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("🔒 Chưa mở khóa", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, true));
        lore.add(Utils.text("Phân loại: " + type.getDisplay(), NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());
        lore.add(Utils.text("Hãy dùng /ability catalogue để mở khóa", NamedTextColor.YELLOW));

        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    private ItemStack createRemoveButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Gỡ bỏ kĩ năng hiện tại", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("Click để để trống ô này", NamedTextColor.YELLOW));

        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
