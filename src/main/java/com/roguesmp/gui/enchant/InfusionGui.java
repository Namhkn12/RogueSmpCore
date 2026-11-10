package com.roguesmp.gui.enchant;

import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.enchant.infusion.InfusionDefinition;
import com.roguesmp.enchant.infusion.InfusionLevel;
import com.roguesmp.enchant.infusion.InfusionType;
import com.roguesmp.gate.cost.Cost;
import com.roguesmp.gate.cost.CostPayment;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.item.SmpItem;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.modification.InfusionUtil;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class InfusionGui extends BaseGui {

    private static final int ROWS = 6;
    private static final int INPUT_ROW = 1, PANEL_COL = 1;
    private static final int BASIC_TAB_ROW = 3, ADVANCED_TAB_ROW = 4;
    private static final int DIVIDER_COL = 2, LAST_COL = 8;
    private static final int GRID_START_ROW = 1, GRID_START_COL = 3;
    private static final int GRID_WIDTH = 5, GRID_HEIGHT = 4;
    private static final int PER_PAGE = GRID_WIDTH * GRID_HEIGHT;
    private static final int HEADER_ROW = 0, FOOTER_ROW = ROWS - 1, CENTER_COL = 5;
    private static final int PREV_COL = 3, NEXT_COL = 7;
    private static final int MESSAGE_ROW = 2;
    private static final int ACTIVE_ROW = 2, UPGRADE_COL = 4, DOWNGRADE_COL = 6, LEVEL_BAR_ROW = 4;

    private final Player player;
    private final SmpPlayer smpPlayer;
    private ItemStack targetItemStack = null;
    private int page = 0;
    private InfusionType tab = InfusionType.BASIC;

    public InfusionGui(Player player) {
        super(Component.text("Cường Hóa Ấn Ký (Infusion)", NamedTextColor.DARK_PURPLE), ROWS);
        this.player = player;
        this.smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
    }

    @Override
    public void setup() {
        clearUi();
        SmpItem smpItem = targetItemStack == null ? null : SmpItem.wrap(targetItemStack);

        drawFrame();
        renderInput();
        renderTab(smpItem, InfusionType.BASIC, BASIC_TAB_ROW);
        renderTab(smpItem, InfusionType.ADVANCED, ADVANCED_TAB_ROW);

        if (smpItem == null) {
            renderHeader(null);
            renderMessage("Hãy bỏ trang bị vào trước!");
        } else if (!InfusionUtil.isEnchantable(smpItem)) {
            renderHeader(null);
            renderMessage("Trang bị này không thể khảm Ấn Ký!");
        } else {
            setupOptions(smpItem);
        }

        fillEmpty(FILLER_BLACK);
    }

    private void drawFrame() {
        ItemStack pane = createDecoration(tab.getFramePane());
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col <= LAST_COL; col++) {
                boolean border = row == HEADER_ROW || row == FOOTER_ROW || col == 0 || col == DIVIDER_COL || col == LAST_COL;
                if (border) addButton(row, col, pane, ClickHandler.noAction());
            }
        }
    }

    private void renderInput() {
        if (targetItemStack != null) {
            addButton(INPUT_ROW, PANEL_COL, targetItemStack, this::handleTakeOut);
            return;
        }
        ItemStack hint = named(Material.ITEM_FRAME, "Ô Trang Bị", NamedTextColor.GREEN);
        hint.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(Utils.text("Click trang bị trong túi để đưa vào.", NamedTextColor.GRAY))));
        addButton(INPUT_ROW, PANEL_COL, hint, ClickHandler.noAction());
    }

    private void renderHeader(@Nullable List<InfusionDefinition> infusions) {
        ItemStack header = ItemStack.of(tab.getIcon());
        header.setData(DataComponentTypes.ITEM_NAME, Component.text(tab.getDisplayName(), tab.getColor(), TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text(tab.getDescription(), NamedTextColor.GRAY));
        lore.add(Utils.text("Mỗi trang bị chỉ được khảm 1 " + tab.getDisplayName() + ".", NamedTextColor.DARK_GRAY));
        header.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        addButton(HEADER_ROW, CENTER_COL, header, ClickHandler.noAction());
    }

    private void renderMessage(String message) {
        addButton(MESSAGE_ROW, CENTER_COL, named(Material.BARRIER, message, NamedTextColor.RED), ClickHandler.noAction());
    }

    private void setupOptions(SmpItem smpItem) {
        InfusionDefinition active = InfusionUtil.getActiveInfusion(smpItem, tab);
        if (active != null) {
            renderHeader(null);
            renderOption(smpItem, active, ACTIVE_ROW, UPGRADE_COL);
            renderDowngradeOption(smpItem, active, ACTIVE_ROW, DOWNGRADE_COL);
            return;
        }

        List<InfusionDefinition> infusions = InfusionDefinition.sorted(tab);
        renderHeader(infusions);
        if (infusions.isEmpty()) {
            renderMessage("Chưa có " + tab.getDisplayName() + " nào!");
            return;
        }

        int maxPage = (infusions.size() - 1) / PER_PAGE;
        page = Math.min(page, maxPage);

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < infusions.size(); i++) {
            renderOption(smpItem, infusions.get(start + i), GRID_START_ROW + i / GRID_WIDTH, GRID_START_COL + i % GRID_WIDTH);
        }

        renderPagination(maxPage);
    }

    private void renderPagination(int maxPage) {
        if (maxPage == 0) return;

        ItemStack pageInfo = named(Material.PAPER, "Trang " + (page + 1) + "/" + (maxPage + 1), NamedTextColor.WHITE);
        pageInfo.setAmount(page + 1);
        addButton(FOOTER_ROW, CENTER_COL, pageInfo, ClickHandler.noAction());

        if (page > 0) {
            addButton(FOOTER_ROW, PREV_COL, PREV_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                page--;
                setup();
            });
        }
        if (page < maxPage) {
            addButton(FOOTER_ROW, NEXT_COL, NEXT_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                page++;
                setup();
            });
        }
    }

    private void renderTab(@Nullable SmpItem smpItem, InfusionType type, int row) {
        boolean selected = type == tab;
        ItemStack button = ItemStack.of(type.getIcon());
        button.setData(DataComponentTypes.ITEM_NAME, Component.text(type.getDisplayName(), selected ? type.getColor() : NamedTextColor.GRAY, TextDecoration.BOLD));
        button.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, selected);

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text(type.getDescription(), NamedTextColor.GRAY));
        if (smpItem != null && InfusionUtil.isEnchantable(smpItem)) {
            InfusionDefinition active = InfusionUtil.getActiveInfusion(smpItem, type);
            lore.add(Component.empty());
            lore.add(Utils.text("Đang khảm: ", NamedTextColor.GRAY).append(active == null
                    ? Component.text("Trống", NamedTextColor.DARK_GRAY)
                    : Component.text(active.smpEnchant().getSimpleName() + " " + Utils.toRoman(InfusionUtil.getLevel(smpItem, active)), NamedTextColor.YELLOW)));
        }
        if (!selected) {
            lore.add(Component.empty());
            lore.add(Utils.text("Click để xem!", NamedTextColor.GREEN));
        }
        button.setData(DataComponentTypes.LORE, ItemLore.lore(lore));

        addButton(row, PANEL_COL, button, event -> {
            event.setCancelled(true);
            if (selected) return;
            tab = type;
            page = 0;
            setup();
        });
    }

    private void renderOption(SmpItem smpItem, InfusionDefinition infusion, int row, int col) {
        SmpEnchant smpEnchant = infusion.smpEnchant();
        int currentLevel = InfusionUtil.getLevel(smpItem, infusion);
        InfusionLevel next = InfusionUtil.getNextLevel(smpItem, infusion);

        ItemStack optionItem = new ItemStack(next == null ? Material.BEDROCK : smpEnchant.getIcon());
        optionItem.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hiddenComponents(Set.of(DataComponentTypes.ATTRIBUTE_MODIFIERS)).build());
        String title = next == null
                ? "Ấn Ký: " + smpEnchant.getSimpleName() + " [ĐẠT CẤP TỐI ĐA]"
                : "Nâng cấp Ấn Ký: " + smpEnchant.getSimpleName();
        optionItem.setData(DataComponentTypes.ITEM_NAME, Component.text(title, NamedTextColor.GOLD, TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.addAll(Utils.text(smpEnchant.getDescription(Math.max(1, currentLevel)), NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Utils.text("Cấp độ hiện tại: ", NamedTextColor.GRAY)
                .append(Component.text(currentLevel == 0 ? "Chưa có" : currentLevel + "/" + infusion.maxLevel(), NamedTextColor.YELLOW)));

        if (next == null) {
            lore.add(Utils.text("Ấn ký này đã đạt giới hạn cấp độ cao nhất (" + infusion.maxLevel() + ").", NamedTextColor.RED));
            optionItem.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
            addButton(getSlot(row, col), optionItem, ClickHandler.noAction());
            return;
        }

        lore.add(Utils.text("Nâng cấp lên Cấp: ", NamedTextColor.GRAY).append(Component.text(currentLevel + 1, NamedTextColor.GREEN)));
        lore.add(Utils.text("Tỉ lệ thành công: ", NamedTextColor.GRAY).append(Component.text(formatChance(next.successChance()), chanceColor(next.successChance()))));

        if (currentLevel == 0) {
            lore.add(Component.empty());
            lore.add(Utils.text("Chú ý: Mỗi trang bị chỉ được khảm 1 " + tab.getDisplayName() + "!", NamedTextColor.RED));
        }

        lore.add(Component.empty());
        lore.add(Utils.text("Chi phí nâng cấp:", NamedTextColor.AQUA));
        addCostLines(lore, next.costs());
        if (next.successChance() < 1) {
            lore.add(Utils.text("Thất bại vẫn sẽ tiêu hao chi phí, cấp độ giữ nguyên.", NamedTextColor.DARK_RED));
        }

        lore.add(Component.empty());
        boolean affordable = CostPayment.canAfford(smpPlayer, next.costs());
        lore.add(affordable ? Utils.text("Click để nâng cấp!", NamedTextColor.GREEN) : Utils.text("Không đủ nguyên liệu!", NamedTextColor.RED));

        optionItem.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        addButton(getSlot(row, col), optionItem, event -> {
            event.setCancelled(true);
            handleUpgrade(smpItem, infusion);
        });
    }

    private void renderDowngradeOption(SmpItem smpItem, InfusionDefinition infusion, int row, int col) {
        InfusionLevel current = InfusionUtil.getCurrentLevel(smpItem, infusion);
        if (current == null) return;

        ItemStack button = ItemStack.of(Material.REDSTONE_ORE);
        button.setData(DataComponentTypes.ITEM_NAME, Component.text("Hạ Cấp Ấn Ký (Giảm 1 Cấp)", NamedTextColor.RED, TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Utils.text("Hành động này sẽ giảm cấp độ Ấn Ký hiện tại xuống 1 bậc.", NamedTextColor.GRAY));
        lore.add(Utils.text("Cấp độ sau khi hạ: ", NamedTextColor.GRAY)
                .append(Component.text(InfusionUtil.getLevel(smpItem, infusion) - 1, NamedTextColor.YELLOW)));
        lore.add(Component.empty());
        lore.add(Utils.text("Hoàn trả 100% chi phí của cấp này:", NamedTextColor.AQUA));
        for (Cost cost : current.costs()) {
            lore.add(Utils.text("- ", NamedTextColor.GRAY).append(cost.getAmountDisplay()));
        }

        button.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        addButton(getSlot(row, col), button, event -> {
            event.setCancelled(true);
            handleDowngrade(smpItem, infusion);
        });
    }

    private void addCostLines(List<Component> lore, List<Cost> costs) {
        if (costs.isEmpty()) {
            lore.add(Utils.text("- Miễn phí", NamedTextColor.GRAY));
            return;
        }
        for (Cost cost : costs) {
            lore.add(Utils.text("- ", NamedTextColor.GRAY).append(cost.getDisplay(smpPlayer)));
        }
    }

    private void handleUpgrade(SmpItem smpItem, InfusionDefinition infusion) {
        InfusionUtil.Result result = InfusionUtil.upgrade(smpPlayer, smpItem, infusion);

        switch (result) {
            case SUCCESS -> {
                regenerateTarget(smpItem);
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
                player.spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            }
            case FAILED_ROLL -> {
                player.sendMessage(Component.text("Cường hóa thất bại! Nguyên liệu đã bị mất.", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1.0f, 1.0f);
                player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.02);
            }
            case NOT_ENCHANTABLE -> deny("Vật phẩm này không hỗ trợ Ấn Ký!");
            case OTHER_INFUSION_ACTIVE -> deny("Vật phẩm đã sở hữu một " + infusion.type().getDisplayName() + " khác!");
            case CANNOT_AFFORD -> deny("Bạn không có đủ nguyên liệu!");
            case MAX_LEVEL -> deny("Ấn Ký đã đạt cấp độ tối đa!");
            case NO_ACTIVE_INFUSION -> deny("Vật phẩm chưa có Ấn Ký!");
        }
        setup();
    }

    private void handleDowngrade(SmpItem smpItem, InfusionDefinition infusion) {
        if (InfusionUtil.downgrade(smpPlayer, smpItem, infusion) == InfusionUtil.Result.SUCCESS) {
            regenerateTarget(smpItem);
            player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.2f);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }
        setup();
    }

    private void regenerateTarget(SmpItem smpItem) {
        targetItemStack = smpItem.generateItemStack(smpPlayer, targetItemStack.getAmount());
    }

    private void deny(String message) {
        player.sendMessage(Component.text(message, NamedTextColor.RED));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
    }

    private static ItemStack named(Material material, String name, NamedTextColor color) {
        ItemStack stack = ItemStack.of(material);
        stack.setData(DataComponentTypes.ITEM_NAME, Component.text(name, color));
        return stack;
    }

    private static String formatChance(double chance) {
        return Utils.formatDecimal(chance * 100) + "%";
    }

    private static NamedTextColor chanceColor(double chance) {
        if (chance >= 1) return NamedTextColor.GREEN;
        if (chance >= 0.5) return NamedTextColor.YELLOW;
        return NamedTextColor.RED;
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (!ItemStackUtils.isValidItem(clicked) || targetItemStack != null) return;

        targetItemStack = clicked.clone();
        targetItemStack.setAmount(1);
        clicked.setAmount(clicked.getAmount() - 1);
        setup();
    }

    private void handleTakeOut(InventoryClickEvent event) {
        event.setCancelled(true);
        if (targetItemStack == null) return;

        PlayerUtils.giveItem(player, targetItemStack);
        targetItemStack = null;
        setup();
    }

    @Override
    public void onCloseInventory(InventoryCloseEvent event) {
        if (targetItemStack != null) {
            PlayerUtils.giveItem(player, targetItemStack);
            targetItemStack = null;
        }
    }
}
