package com.roguesmp.gui.classes;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.gui.ability.AbilityLoadoutGui;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.classes.PlayerClass;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lets a player pick (or switch to) a {@link PlayerClass}. Picking a class the player has never
 * had grants its default abilities permanently; switching away from a class only unequips its
 * abilities, it never un-levels them (see {@link SmpPlayer#setPlayerClass(PlayerClass)}).
 */
public class ClassSelectionGui extends BaseGui {

    private static final int[] CARD_COLUMNS = {1, 3, 5, 7};
    private static final int BORDER_TOP_ROW = 1;
    private static final int ICON_ROW = 2;
    private static final int ABILITY_ROW = 3;
    private static final int BORDER_BOTTOM_ROW = 4;
    private static final int NAV_ROW = 5;

    private final SmpPlayer smpPlayer;
    private final List<PlayerClass> classes;
    private int currentPage = 0;

    public ClassSelectionGui(SmpPlayer smpPlayer) {
        super(Component.text("Chọn Class", NamedTextColor.DARK_AQUA).decoration(TextDecoration.BOLD, true), 6);
        this.smpPlayer = smpPlayer;
        this.classes = new ArrayList<>(Registries.PLAYER_CLASS.getAll().values());
    }

    @Override
    public void setup() {
        clearUi();

        String currentClassId = smpPlayer.getPlayerData().getClassId();
        int totalPages = Math.max(1, (int) Math.ceil(classes.size() / (double) CARD_COLUMNS.length));
        currentPage = Math.min(currentPage, totalPages - 1);

        int from = currentPage * CARD_COLUMNS.length;
        List<PlayerClass> pageClasses = classes.subList(from, Math.min(from + CARD_COLUMNS.length, classes.size()));
        for (int i = 0; i < pageClasses.size(); i++) {
            PlayerClass playerClass = pageClasses.get(i);
            boolean isCurrent = playerClass.getId().equals(currentClassId);
            int col = CARD_COLUMNS[i];

            ItemStack border = createDecoration(isCurrent ? Material.LIME_STAINED_GLASS_PANE : Material.CYAN_STAINED_GLASS_PANE);
            addButton(BORDER_TOP_ROW, col, border, ClickHandler.noAction());
            addButton(BORDER_BOTTOM_ROW, col, border, ClickHandler.noAction());
            addButton(ABILITY_ROW, col, createDefaultAbilitiesIcon(playerClass), ClickHandler.noAction());
            addButton(ICON_ROW, col, createClassIcon(playerClass, isCurrent), event -> {
                event.setCancelled(true);
                if (!isCurrent) {
                    smpPlayer.setPlayerClass(playerClass);
                    smpPlayer.sendMessage(Component.text("Bạn đã chọn lớp nhân vật: ", NamedTextColor.GREEN)
                            .append(playerClass.getFormattedDisplayName()));
                    event.getWhoClicked().playSound(Sound.sound(SoundEventKeys.ENTITY_PLAYER_LEVELUP, Sound.Source.PLAYER, 0.8f, 1.2f));
                }
                BaseGui abilityMenu = new AbilityLoadoutGui(smpPlayer).withBackAction(getBackTitle(), getBackAction());
                Utils.runLater(() -> abilityMenu.showInventory(event.getWhoClicked()));
            });
        }

        if (currentPage > 0) {
            addButton(NAV_ROW, 3, PREV_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                changePage(event.getWhoClicked(), -1);
            });
        }
        if (currentPage < totalPages - 1) {
            addButton(NAV_ROW, 5, NEXT_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                changePage(event.getWhoClicked(), 1);
            });
        }
        addButton(NAV_ROW, 4, createPageIcon(totalPages), ClickHandler.noAction());
        addBackButton(getSlot(NAV_ROW, 0));
        fillEmpty(FILLER_BLACK);
    }

    private void changePage(HumanEntity viewer, int delta) {
        currentPage += delta;
        viewer.playSound(Sound.sound(SoundEventKeys.ITEM_BOOK_PAGE_TURN, Sound.Source.PLAYER, 1f, 1f));
        setup();
    }

    private ItemStack createPageIcon(int totalPages) {
        ItemStack item = ItemStack.of(Material.PAPER);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Trang " + (currentPage + 1) + "/" + totalPages, NamedTextColor.YELLOW));
        return item;
    }

    private ItemStack createClassIcon(PlayerClass playerClass, boolean isCurrent) {
        ItemStack item = ItemStack.of(Material.PAPER);
        item.setData(DataComponentTypes.ITEM_NAME, playerClass.getFormattedDisplayName()
                .decoration(TextDecoration.BOLD, true));
        item.setData(DataComponentTypes.ITEM_MODEL, Key.key(playerClass.getIcon()));
        if (isCurrent) item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        List<Component> lore = new ArrayList<>();
        if (isCurrent) {
            lore.add(Utils.text("✔ Đang sử dụng", NamedTextColor.GREEN));
            lore.add(Component.empty());
        }
        for (String line : playerClass.getDescription()) {
            lore.add(Utils.text(line, NamedTextColor.GRAY));
        }
        lore.add(Component.empty());
        lore.add(Utils.text(isCurrent ? "Click để mở bảng kĩ năng" : "Click để chọn lớp này", NamedTextColor.YELLOW));

        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    private ItemStack createDefaultAbilitiesIcon(PlayerClass playerClass) {
        ItemStack item = ItemStack.of(Material.ENCHANTED_BOOK);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Kĩ Năng Khởi Đầu", NamedTextColor.LIGHT_PURPLE));

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("Nhận được khi chọn lớp lần đầu:", NamedTextColor.GRAY));
        boolean any = false;
        for (Map.Entry<String, Integer> entry : playerClass.getDefaultAbilities().entrySet()) {
            if (entry.getValue() <= 0) continue;
            AbilityInfo<?> info = Registries.ABILITY.get(entry.getKey());
            Component name = info == null ? Component.text(entry.getKey()) : info.getFormattedDisplayName();
            lore.add(Utils.text("• ", NamedTextColor.DARK_GRAY).append(name.colorIfAbsent(NamedTextColor.WHITE))
                    .append(Utils.text(" (Cấp " + entry.getValue() + ")", NamedTextColor.GRAY)));
            any = true;
        }
        if (!any) lore.add(Utils.text("• Không có", NamedTextColor.DARK_GRAY));

        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    public static void register() {
        new CommandAPICommand("class")
                .executesPlayer((player, args) -> {
                    SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player.getUniqueId());
                    if (smpPlayer == null) return;
                    new ClassSelectionGui(smpPlayer).showInventory(player);
                })
                .register(RogueSmpCore.getInstance());
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
