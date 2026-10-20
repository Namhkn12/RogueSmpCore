package com.roguesmp.gui.island;

import com.roguesmp.block.BlockType;
import com.roguesmp.gate.GateDisplay;
import com.roguesmp.gate.cost.Cost;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.island.IslandData;
import com.roguesmp.island.IslandLevel;
import com.roguesmp.island.IslandLevelService;
import com.roguesmp.island.IslandManager;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IslandLevelGui extends BaseGui {

    private final Player player;
    private final IslandData island;

    public IslandLevelGui(Player player, IslandData island) {
        super(Component.text("Cấp Độ Đảo", NamedTextColor.DARK_AQUA).decoration(TextDecoration.BOLD, true), 3);
        this.player = player;
        this.island = island;
    }

    @Override
    public void setup() {
        clearUi();
        fillEmpty(FILLER_BLACK);

        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        IslandLevelService levelService = IslandManager.getInstance().getLevelService();
        int level = island.getLevel();

        addItem(1, 2, levelItem(Material.GRASS_BLOCK, "Cấp hiện tại: " + level, NamedTextColor.GREEN, perkLore(IslandLevel.of(level))));

        Map.Entry<Integer, IslandLevel> next = IslandLevel.next(level);
        if (next == null) {
            addItem(1, 4, levelItem(Material.NETHER_STAR, "Đã đạt cấp tối đa", NamedTextColor.GOLD, List.of()));
        } else {
            addItem(1, 4, levelItem(Material.EXPERIENCE_BOTTLE, "Cấp tiếp theo: " + next.getKey(), NamedTextColor.AQUA, nextLore(smpPlayer, next.getValue())));

            boolean available = levelService.check(smpPlayer, island) == IslandLevelService.Result.SUCCESS;
            ItemStack upgrade = levelItem(available ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
                    available ? "NÂNG CẤP" : "KHÔNG ĐỦ YÊU CẦU", available ? NamedTextColor.GREEN : NamedTextColor.RED, List.of());
            addButton(1, 6, upgrade, event -> {
                event.setCancelled(true);
                IslandLevelService.Result result = levelService.upgrade(smpPlayer, island);
                if (result != IslandLevelService.Result.SUCCESS) player.sendMessage(Utils.fromString("<red>Không thể nâng cấp đảo!"));
                setup();
            });
        }

        addButton(2, 0, PREV_PAGE_BUTTON, event -> {
            event.setCancelled(true);
            new IslandMainGui(player).showInventory(player);
        });
    }

    private List<Component> perkLore(IslandLevel islandLevel) {
        List<Component> lore = new ArrayList<>(List.of(
                Utils.text("Mạng năng lượng tối đa: ", NamedTextColor.GRAY).append(Component.text(islandLevel.maxEnergyNetworks(), NamedTextColor.WHITE)),
                Utils.text("Dung lượng mỗi mạng: ", NamedTextColor.GRAY).append(Component.text(Utils.formatMoney(islandLevel.energyNetworkCapacity()), NamedTextColor.WHITE))
        ));
        if (!islandLevel.blockLimits().isEmpty()) {
            lore.add(Component.empty());
            lore.add(Utils.text("Giới hạn khối:", NamedTextColor.YELLOW));
            islandLevel.blockLimits().forEach((blockType, limit) -> {
                int count = island.getBlockCount(blockType);
                BlockType<?, ?> type = Registries.BLOCK_TYPE.get(blockType);
                lore.add(line(Component.text((type != null ? type.displayName() : blockType) + ": ", NamedTextColor.GRAY)
                        .append(Component.text(count, count >= limit ? NamedTextColor.RED : NamedTextColor.GREEN))
                        .append(Component.text("/" + limit, NamedTextColor.GRAY))));
            });
        }
        return lore;
    }

    private List<Component> nextLore(SmpPlayer smpPlayer, IslandLevel next) {
        List<Component> lore = new ArrayList<>(perkLore(next));
        if (!next.requirements().isEmpty()) {
            lore.add(Component.empty());
            lore.add(Utils.text("Yêu cầu:", NamedTextColor.YELLOW));
            for (Requirement requirement : next.requirements()) {
                lore.add(line(GateDisplay.check(requirement.getDisplay(smpPlayer), requirement.test(smpPlayer))));
            }
        }
        if (!next.costs().isEmpty()) {
            lore.add(Component.empty());
            lore.add(Utils.text("Chi phí:", NamedTextColor.YELLOW));
            for (Cost cost : next.costs()) {
                lore.add(line(GateDisplay.check(cost.getDisplay(smpPlayer), cost.canAfford(smpPlayer))));
            }
        }
        return lore;
    }

    private Component line(Component content) {
        return Component.text(" ").append(content).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private ItemStack levelItem(Material material, String name, NamedTextColor color, List<Component> lore) {
        ItemStack item = ItemStack.of(material);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text(name, color).decoration(TextDecoration.BOLD, true));
        if (!lore.isEmpty()) item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
