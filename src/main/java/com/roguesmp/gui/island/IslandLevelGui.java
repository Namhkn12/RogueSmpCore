package com.roguesmp.gui.island;

import com.roguesmp.gate.GateDisplay;
import com.roguesmp.gate.GateInventoryUtils;
import com.roguesmp.gate.cost.Cost;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.island.IslandData;
import com.roguesmp.island.IslandLevel;
import com.roguesmp.island.IslandLevelService;
import com.roguesmp.island.IslandManager;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.ItemRef;
import com.roguesmp.utils.dialog.DialogBuilder;
import io.papermc.paper.dialog.Dialog;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IslandLevelGui extends BaseGui {

    private enum View { OVERVIEW, CONTRIBUTIONS }

    private static final int ROWS = 5;
    private static final int HEADER_ROW = 0, HEADER_COL = 4;
    private static final int LEVEL_ROW = 2, CURRENT_COL = 3, NEXT_COL = 5;
    private static final int GRID_FIRST_ROW = 1, GRID_LAST_ROW = 3, GRID_FIRST_COL = 1, GRID_LAST_COL = 7;
    private static final int GRID_WIDTH = GRID_LAST_COL - GRID_FIRST_COL + 1;
    private static final int PAGE_SIZE = GRID_WIDTH * (GRID_LAST_ROW - GRID_FIRST_ROW + 1);
    private static final int NAV_ROW = 4, PREV_COL = 3, NEXT_PAGE_COL = 5;
    private static final int STACK_SIZE = 64;
    private static final String MONEY_INPUT = "amount";
    private static final ItemStack BORDER = createDecoration(Material.CYAN_STAINED_GLASS_PANE);

    private final Player player;
    private final IslandData island;
    private View view = View.OVERVIEW;
    private int page;

    public IslandLevelGui(Player player, IslandData island) {
        super(Component.text("Cấp Độ Đảo", NamedTextColor.DARK_AQUA).decoration(TextDecoration.BOLD, true), ROWS);
        this.player = player;
        this.island = island;
    }

    @Override
    public void setup() {
        clearUi();

        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        IslandLevelService levelService = IslandManager.getInstance().getLevelService();
        Map.Entry<Integer, IslandLevel> next = IslandLevel.next(island.getLevel());

        if (view == View.OVERVIEW) setupOverview(smpPlayer, levelService, next);
        else setupContributions(smpPlayer, levelService, next);

        addButton(NAV_ROW, 0, PREV_PAGE_BUTTON, event -> {
            event.setCancelled(true);
            if (view == View.CONTRIBUTIONS) switchView(View.OVERVIEW);
            else new IslandMainGui(player).showInventory(player);
        });
        fillBorder();
        fillEmpty(FILLER_BLACK);
    }

    private void fillBorder() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                boolean edge = row == 0 || row == ROWS - 1 || col == 0 || col == 8;
                if (edge && getInventory().getItem(getSlot(row, col)) == null) addButton(row, col, BORDER, event -> event.setCancelled(true));
            }
        }
    }

    public static void refreshViewers(IslandData island) {
        Map.Entry<Integer, IslandLevel> next = IslandLevel.next(island.getLevel());
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            switch (viewer.getOpenInventory().getTopInventory().getHolder(false)) {
                case IslandLevelGui gui when gui.island == island -> gui.setup();
                case IslandUpgradeConfirmGui confirm when confirm.isFor(island) && (next == null || next.getKey() != confirm.getTargetLevel()) -> confirm.returnToParent();
                case null, default -> {
                }
            }
        }
    }

    private void switchView(View target) {
        view = target;
        page = 0;
        setup();
    }

    private void setupOverview(SmpPlayer smpPlayer, IslandLevelService levelService, Map.Entry<Integer, IslandLevel> next) {
        IslandLevel current = IslandLevel.of(island.getLevel());
        addItem(LEVEL_ROW, CURRENT_COL, namedItem(Material.GRASS_BLOCK, "Cấp hiện tại: " + island.getLevel(), NamedTextColor.GREEN, statLore(island, current)));

        if (next == null) {
            addItem(LEVEL_ROW, NEXT_COL, namedItem(Material.NETHER_STAR, "Đã đạt cấp tối đa", NamedTextColor.GOLD, List.of()));
            return;
        }

        IslandLevelService.Result result = levelService.check(smpPlayer, island);
        boolean ready = result == IslandLevelService.Result.SUCCESS;
        ItemStack nextItem = namedItem(Material.EXPERIENCE_BOTTLE, "Nâng lên cấp " + next.getKey(), ready ? NamedTextColor.GREEN : NamedTextColor.AQUA,
                nextLore(smpPlayer, current, next.getValue(), result));
        if (ready) nextItem.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        addButton(LEVEL_ROW, NEXT_COL, nextItem, event -> {
            event.setCancelled(true);
            switch (levelService.check(smpPlayer, island)) {
                case CONTRIBUTIONS_MISSING -> switchView(View.CONTRIBUTIONS);
                case SUCCESS -> new IslandUpgradeConfirmGui(player, island, next.getKey(), this).showInventory(player);
                default -> {
                    player.sendMessage(Utils.fromString("<red>Không thể nâng cấp đảo!"));
                    setup();
                }
            }
        });
    }

    private record Slot(ItemStack icon, ClickHandler handler) {}

    private void setupContributions(SmpPlayer smpPlayer, IslandLevelService levelService, Map.Entry<Integer, IslandLevel> next) {
        List<Slot> slots = next == null ? List.of() : contributionSlots(smpPlayer, levelService, next);
        if (slots.isEmpty()) {
            addItem(LEVEL_ROW, 4, namedItem(Material.BARRIER, "Không có nguyên liệu cần góp", NamedTextColor.GRAY, List.of()));
            return;
        }
        addItem(HEADER_ROW, HEADER_COL, namedItem(Material.CHEST, "Đóng góp cho cấp " + next.getKey(), NamedTextColor.AQUA,
                List.of(line(Utils.text("Tiến độ: ", NamedTextColor.GRAY).append(Component.text(contributionPercent(next.getValue()) + "%", NamedTextColor.WHITE))))));

        int totalPages = (slots.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        page = Math.min(page, totalPages - 1);
        int from = page * PAGE_SIZE;
        for (int i = from; i < Math.min(from + PAGE_SIZE, slots.size()); i++) {
            int index = i - from;
            Slot slot = slots.get(i);
            addButton(GRID_FIRST_ROW + index / GRID_WIDTH, GRID_FIRST_COL + index % GRID_WIDTH, slot.icon(), slot.handler());
        }

        if (page > 0) addButton(NAV_ROW, PREV_COL, PREV_PAGE_BUTTON, event -> {
            event.setCancelled(true);
            page--;
            setup();
        });
        if (page < totalPages - 1) addButton(NAV_ROW, NEXT_PAGE_COL, NEXT_PAGE_BUTTON, event -> {
            event.setCancelled(true);
            page++;
            setup();
        });
        addItem(NAV_ROW, 4, namedItem(Material.PAPER, "Trang " + (page + 1) + "/" + totalPages, NamedTextColor.WHITE, List.of()));
    }

    private List<Slot> contributionSlots(SmpPlayer smpPlayer, IslandLevelService levelService, Map.Entry<Integer, IslandLevel> next) {
        List<Slot> slots = new ArrayList<>();
        long moneyGoal = IslandLevelService.moneyGoal(next.getValue());
        if (moneyGoal > 0) slots.add(new Slot(moneyItem(smpPlayer, moneyGoal), event -> {
            event.setCancelled(true);
            if (event.isShiftClick()) contributeMoney(smpPlayer, levelService, next.getKey(), Long.MAX_VALUE);
            else askMoneyAmount(smpPlayer, levelService, next.getKey());
        }));

        for (Map.Entry<String, Integer> goal : IslandLevelService.contributionGoals(next.getValue()).entrySet()) {
            String itemId = goal.getKey();
            slots.add(new Slot(contributionItem(smpPlayer, itemId, goal.getValue()), event -> {
                event.setCancelled(true);
                int amount = levelService.contribute(smpPlayer, island, next.getKey(), itemId, event.isShiftClick() ? Integer.MAX_VALUE : STACK_SIZE);
                if (amount > 0) {
                    player.sendActionBar(Utils.fromString("<green>Đã góp <yellow>" + amount + " ").append(ItemRef.displayName(itemId)));
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
                    refreshViewers(island);
                }
                setup();
            }));
        }
        return slots;
    }

    private void askMoneyAmount(SmpPlayer smpPlayer, IslandLevelService levelService, int targetLevel) {
        Dialog dialog = DialogBuilder.create(Component.text("Đóng góp xu cho đảo", NamedTextColor.GOLD))
                .addTextBody(Utils.text("Số dư: ", NamedTextColor.GRAY).append(Component.text(Utils.formatMoney(smpPlayer.getPlayerData().getMoney()), NamedTextColor.YELLOW)))
                .addTextInput(MONEY_INPUT, Component.text("Số xu muốn góp"))
                .confirmation()
                .yesButton(Component.text("Góp", NamedTextColor.GREEN), null, (response, audience) -> Utils.runLater(() -> {
                    String input = response.getText(MONEY_INPUT);
                    String digits = input == null ? "" : input.replaceAll("[^0-9]", "");
                    long amount = digits.isEmpty() || digits.length() > 18 ? 0 : Long.parseLong(digits);
                    if (amount <= 0) player.sendMessage(Utils.fromString("<red>Số xu không hợp lệ."));
                    else contributeMoney(smpPlayer, levelService, targetLevel, amount);
                    showInventory(player);
                }))
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> showInventory(player)))
                .build();
        player.showDialog(dialog);
    }

    private void contributeMoney(SmpPlayer smpPlayer, IslandLevelService levelService, int targetLevel, long maxAmount) {
        long amount = levelService.contributeMoney(smpPlayer, island, targetLevel, maxAmount);
        if (amount > 0) {
            player.sendActionBar(Utils.fromString("<green>Đã góp <yellow>" + Utils.formatMoney(amount) + " xu"));
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
            refreshViewers(island);
        }
        setup();
    }

    private ItemStack moneyItem(SmpPlayer smpPlayer, long goal) {
        long contributed = Math.min(goal, island.getMoneyContribution());
        boolean done = contributed >= goal;
        ItemStack icon = ItemStack.of(done ? Material.LIME_STAINED_GLASS_PANE : Material.GOLD_INGOT);

        List<Component> lore = new ArrayList<>(List.of(
                line(GateDisplay.check(moneyProgress(contributed, goal), done)),
                line(Utils.text("Số dư: ", NamedTextColor.GRAY).append(Component.text(Utils.formatMoney(smpPlayer.getPlayerData().getMoney()), NamedTextColor.WHITE)))
        ));
        if (!done) {
            lore.add(Component.empty());
            lore.add(Utils.text("Click: Nhập số xu", NamedTextColor.YELLOW));
            lore.add(Utils.text("Shift + Click: Góp tối đa", NamedTextColor.YELLOW));
        }
        icon.setData(DataComponentTypes.ITEM_NAME, Utils.text(done ? "✔ Xu" : "Góp xu", done ? NamedTextColor.GREEN : NamedTextColor.GOLD));
        icon.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return icon;
    }

    private static Component moneyProgress(long contributed, long goal) {
        return Component.text(Utils.formatMoney(goal) + " xu", NamedTextColor.GOLD)
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(Component.text(Utils.formatMoney(contributed), contributed >= goal ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(Component.text("/" + Utils.formatMoney(goal) + ")", NamedTextColor.GRAY));
    }

    private ItemStack contributionItem(SmpPlayer smpPlayer, String itemId, int goal) {
        int contributed = Math.min(goal, island.getContribution(itemId));
        boolean done = contributed >= goal;
        ItemStack icon = ItemRef.toItemStack(itemId, 1);
        if (done || icon == null) icon = ItemStack.of(done ? Material.LIME_STAINED_GLASS_PANE : Material.BARRIER);

        List<Component> lore = new ArrayList<>(List.of(
                line(GateDisplay.check(GateDisplay.itemProgress(itemId, goal, contributed), done)),
                line(Utils.text("Trong túi: ", NamedTextColor.GRAY).append(Component.text(GateInventoryUtils.count(smpPlayer, itemId), NamedTextColor.WHITE)))
        ));
        if (!done) {
            lore.add(Component.empty());
            lore.add(Utils.text("Click: Nộp " + STACK_SIZE, NamedTextColor.YELLOW));
            lore.add(Utils.text("Shift + Click: Nộp cả túi đồ", NamedTextColor.YELLOW));
        }
        icon.setData(DataComponentTypes.ITEM_NAME, Utils.text(done ? "✔ " : "Nộp ", done ? NamedTextColor.GREEN : NamedTextColor.AQUA).append(ItemRef.displayName(itemId)));
        icon.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return icon;
    }

    public static List<Component> statLore(IslandData island, IslandLevel level) {
        List<Component> lore = new ArrayList<>();
        List<IslandLevel.Perk> perks = level.perks();
        for (int i = 0; i < perks.size(); i++) {
            IslandLevel.Perk perk = perks.get(i);
            if (!perk.unlocked()) continue;
            Component value = i == 0 && level.hasBlockCapacity() ? usedCapacity(island, level) : Component.text(perk.value(), NamedTextColor.WHITE);
            lore.add(line(Utils.text(perk.label() + ": ", NamedTextColor.GRAY).append(value)));
        }
        return lore;
    }

    private static Component usedCapacity(IslandData island, IslandLevel level) {
        int used = island.getUsedCapacity();
        return Component.text(used, used >= level.blockCapacity() ? NamedTextColor.RED : NamedTextColor.GREEN)
                .append(Component.text("/" + level.blockCapacity(), NamedTextColor.GRAY));
    }

    private List<Component> nextLore(SmpPlayer smpPlayer, IslandLevel current, IslandLevel next, IslandLevelService.Result result) {
        List<Component> lore = new ArrayList<>();
        List<IslandLevel.Perk> from = current.perks();
        List<IslandLevel.Perk> to = next.perks();
        for (int i = 0; i < to.size(); i++) {
            if (to.get(i).unlocked()) lore.add(line(compare(from.get(i), to.get(i))));
        }

        Map<String, Integer> goals = IslandLevelService.contributionGoals(next);
        long moneyGoal = IslandLevelService.moneyGoal(next);
        if (!goals.isEmpty() || moneyGoal > 0) {
            lore.add(Component.empty());
            lore.add(Utils.text("Đóng góp: ", NamedTextColor.YELLOW).append(Component.text(contributionPercent(next) + "%", NamedTextColor.WHITE)));
            if (moneyGoal > 0) {
                long contributed = Math.min(moneyGoal, island.getMoneyContribution());
                lore.add(line(GateDisplay.check(moneyProgress(contributed, moneyGoal), contributed >= moneyGoal)));
            }
            for (Map.Entry<String, Integer> goal : goals.entrySet()) {
                int contributed = Math.min(goal.getValue(), island.getContribution(goal.getKey()));
                lore.add(line(GateDisplay.check(GateDisplay.itemProgress(goal.getKey(), goal.getValue(), contributed), contributed >= goal.getValue())));
            }
        }

        List<Cost> directCosts = IslandLevelService.directCosts(next);
        if (!directCosts.isEmpty()) {
            lore.add(Component.empty());
            lore.add(Utils.text("Chi phí khi nâng cấp:", NamedTextColor.YELLOW));
            for (Cost cost : directCosts) lore.add(line(GateDisplay.check(cost.getDisplay(smpPlayer), cost.canAfford(smpPlayer))));
        }

        if (!next.requirements().isEmpty()) {
            lore.add(Component.empty());
            lore.add(Utils.text("Yêu cầu:", NamedTextColor.YELLOW));
            for (Requirement requirement : next.requirements()) lore.add(line(GateDisplay.check(requirement.getDisplay(smpPlayer), requirement.test(smpPlayer))));
        }

        lore.add(Component.empty());
        lore.add(switch (result) {
            case SUCCESS -> Utils.text("Click để nâng cấp!", NamedTextColor.GREEN);
            case CONTRIBUTIONS_MISSING -> Utils.text("Click để góp nguyên liệu", NamedTextColor.YELLOW);
            case CANNOT_AFFORD -> Utils.text("Bạn không đủ chi phí nâng cấp", NamedTextColor.RED);
            case REQUIREMENTS_NOT_MET -> Utils.text("Chưa đạt yêu cầu", NamedTextColor.RED);
            case MAX_LEVEL -> Utils.text("Đã đạt cấp tối đa", NamedTextColor.GOLD);
            case LEVEL_CHANGED -> Utils.text("Cấp đảo vừa thay đổi", NamedTextColor.RED);
        });
        return lore;
    }

    private int contributionPercent(IslandLevel level) {
        double sum = 0;
        int count = 0;
        for (Map.Entry<String, Integer> goal : IslandLevelService.contributionGoals(level).entrySet()) {
            sum += Math.min(1.0, (double) island.getContribution(goal.getKey()) / goal.getValue());
            count++;
        }
        long moneyGoal = IslandLevelService.moneyGoal(level);
        if (moneyGoal > 0) {
            sum += Math.min(1.0, (double) island.getMoneyContribution() / moneyGoal);
            count++;
        }
        return count == 0 ? 100 : (int) (sum * 100 / count);
    }

    private static Component compare(IslandLevel.Perk from, IslandLevel.Perk to) {
        Component label = Utils.text(to.label() + ": ", NamedTextColor.GRAY);
        if (!from.unlocked()) return label.append(Component.text(to.value(), NamedTextColor.GREEN)).append(Component.text(" (Mới!)", NamedTextColor.GOLD));
        Component line = label.append(Component.text(from.value(), NamedTextColor.WHITE));
        return from.value().equals(to.value()) ? line : line.append(Component.text(" → ", NamedTextColor.DARK_GRAY)).append(Component.text(to.value(), NamedTextColor.GREEN));
    }

    private static Component line(Component content) {
        return Component.text(" ").append(content).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private static ItemStack namedItem(Material material, String name, NamedTextColor color, List<Component> lore) {
        ItemStack item = ItemStack.of(material);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text(name, color).decoration(TextDecoration.BOLD, true));
        if (!lore.isEmpty()) item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    @Override
    public void onClickTopInventory(InventoryClickEvent event) {
        event.setCancelled(true);
        super.onClickTopInventory(event);
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
