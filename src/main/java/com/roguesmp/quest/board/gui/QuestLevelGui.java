package com.roguesmp.quest.board.gui;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.quest.board.AdventureLeaderboard;
import com.roguesmp.quest.board.AdventureRank;
import com.roguesmp.quest.board.QuestBoardManager;
import com.roguesmp.quest.board.QuestLevelCurve;
import com.roguesmp.quest.board.TopRankSettings;
import com.roguesmp.quest.data.PlayerQuestData;
import com.roguesmp.quest.reward.QuestReward;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuestLevelGui extends BaseGui {

    private static final int PATH_ROWS = 5;
    private static final int[] PATH = buildPath();

    private final Player player;
    private final BaseGui parent;
    private final QuestBoardManager boardManager;
    private int page = -1;

    public QuestLevelGui(Player player, @Nullable BaseGui parent) {
        super(Component.text("Cấp Mạo hiểm"), 6);
        this.player = player;
        this.parent = parent;
        this.boardManager = QuestBoardManager.getInstance();
    }

    private static int[] buildPath() {
        List<Integer> slots = new ArrayList<>();
        for (int col = 0; col < 9; col++) {
            if (col % 2 == 0) {
                slots.add((col % 4 == 0 ? PATH_ROWS - 1 : 0) * 9 + col);
                continue;
            }
            boolean upward = col % 4 == 1;
            for (int i = 0; i < PATH_ROWS; i++) slots.add((upward ? PATH_ROWS - 1 - i : i) * 9 + col);
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public void setup() {
        clearUi();
        PlayerQuestData data = QuestManager.getInstance().getPlayerQuestData(player.getUniqueId());
        QuestLevelCurve curve = boardManager.getSettings().level();
        int level = data == null ? 1 : boardManager.getLevel(data);
        int pageCount = (curve.maxLevel() + PATH.length - 1) / PATH.length;
        if (page < 0) page = Math.min((level - 1) / PATH.length, pageCount - 1);

        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        for (int i = 0; i < PATH.length; i++) {
            int shownLevel = page * PATH.length + i + 1;
            if (shownLevel > curve.maxLevel()) break;
            boolean claimable = data != null && boardManager.canClaimReward(data, shownLevel);
            addButton(PATH[i], buildLevelItem(curve, shownLevel, level, data, smpPlayer), claimable ? event -> claim(event, shownLevel) : ClickHandler.noAction());
        }

        for (int c = 0; c < 9; c++) addButton(5, c, FILLER_BLACK, ClickHandler.noAction());
        if (page > 0) addButton(5, 0, PREV_PAGE_BUTTON, event -> turnPage(event, -1));
        if (page < pageCount - 1) addButton(5, 8, NEXT_PAGE_BUTTON, event -> turnPage(event, 1));
        if (data != null) addButton(5, 2, buildRankItem(curve, data, level), ClickHandler.noAction());

        if (parent != null) {
            ItemStack back = ItemStack.of(Material.ARROW);
            back.setData(DataComponentTypes.ITEM_NAME, Component.text("Quay lại", NamedTextColor.YELLOW));
            addButton(5, 4, back, ClickHandler.openGui(parent));
        } else {
            ItemStack close = ItemStack.of(Material.BARRIER);
            close.setData(DataComponentTypes.ITEM_NAME, Component.text("Đóng", NamedTextColor.RED));
            addButton(5, 4, close, event -> {
                event.setCancelled(true);
                event.getWhoClicked().closeInventory();
            });
        }

        fillEmpty(createDecoration(Material.GRAY_STAINED_GLASS_PANE));
    }

    private ItemStack buildRankItem(QuestLevelCurve curve, PlayerQuestData data, int level) {
        AdventureRank rank = boardManager.getRank(data);
        ItemStack item = ItemStack.of(Material.NAME_TAG);
        if (!rank.model().isEmpty()) item.setData(DataComponentTypes.ITEM_MODEL, Key.key(rank.model()));
        item.setData(DataComponentTypes.ITEM_NAME, Utils.fromString("<gold>Hạng hiện tại: </gold>" + (rank.name().isEmpty() ? "<gray>Chưa có" : rank.name())).append(rank.getDisplayIcon()));

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.fromString("<!i><gray>Cấp Mạo hiểm: <gold>" + level));
        if (level < curve.maxLevel()) {
            lore.add(Utils.fromString("<!i><gray>Kinh nghiệm: <aqua>" + curve.expIntoLevel(data.getExp()) + "<gray>/<aqua>" + curve.expToNext(level)));
        }
        Map.Entry<Integer, AdventureRank> nextRank = curve.ranks().higherEntry(level);
        if (nextRank != null) {
            lore.add(Component.empty());
            lore.add(Utils.fromString("<!i><gray>Hạng tiếp theo: </gray>" + nextRank.getValue().name() + " <gray>(cấp <gold>" + nextRank.getKey() + "</gold>)"));
        }
        AdventureLeaderboard leaderboard = boardManager.getLeaderboard();
        QuestBoardManager.AdventureStatus status = boardManager.getStatus(player.getUniqueId());
        lore.add(Component.empty());
        lore.add(Utils.fromString(status.position() > 0
                ? "<!i><gray>Xếp hạng toàn server: <gold>#" + status.position()
                : "<!i><gray>Xếp hạng toàn server: <gray>Chưa có (" + (status.level() < AdventureLeaderboard.minRankedLevel() ? "cần cấp " + AdventureLeaderboard.minRankedLevel() : "cập nhật ở lần xếp hạng tới") + ")"));
        TopRankSettings topRank = leaderboard.getSettings();
        if (topRank != null) {
            lore.add(Component.empty());
            lore.add(Utils.fromString("<!i><gray>Hạng đặc biệt: </gray>" + topRank.rank().name()));
            lore.add(Utils.fromString("<!i><gray>Dành cho <gold>" + topRank.slots() + "</gold> người nhiều kinh nghiệm nhất từ cấp <gold>" + topRank.minLevel() + "</gold>."));
            if (rank.equals(topRank.rank())) lore.add(Utils.fromString("<!i><green>✔ Bạn đang giữ hạng này"));
        }
        lore.add(Utils.fromString("<!i><dark_gray>Bảng xếp hạng cập nhật mỗi ngày."));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    private void turnPage(InventoryClickEvent event, int delta) {
        event.setCancelled(true);
        page += delta;
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        setup();
    }

    private void claim(InventoryClickEvent event, int shownLevel) {
        event.setCancelled(true);
        if (boardManager.claimReward(player, shownLevel)) {
            player.sendMessage(Utils.fromString("<green>Đã nhận phần thưởng cấp <gold>" + shownLevel + "</gold>!"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        }
        setup();
    }

    private ItemStack buildLevelItem(QuestLevelCurve curve, int shownLevel, int level, PlayerQuestData data, SmpPlayer smpPlayer) {
        List<QuestReward> rewards = curve.rewardsAt(shownLevel);
        boolean reached = shownLevel <= level;
        boolean next = shownLevel == level + 1;

        boolean unclaimed = data != null && boardManager.canClaimReward(data, shownLevel);
        boolean claimed = reached && !rewards.isEmpty() && !unclaimed;

        Material material;
        if (unclaimed) material = Material.CHEST;
        else material = reached ? Material.LIME_STAINED_GLASS_PANE : next ? Material.YELLOW_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;

        ItemStack item = ItemStack.of(material);
        String color = reached ? "<green>" : next ? "<yellow>" : "<red>";
        item.setData(DataComponentTypes.ITEM_NAME, Utils.fromString(color + "Cấp " + shownLevel));
        if (shownLevel == level && !claimed) item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        List<Component> lore = new ArrayList<>();
        AdventureRank unlockedRank = curve.ranks().get(shownLevel);
        if (unlockedRank != null) lore.add(Utils.fromString("<!i><gray>Mở khóa hạng: </gray>" + unlockedRank.name()).append(unlockedRank.getDisplayIcon()));
        int limit = curve.acceptLimit(shownLevel);
        if (shownLevel == 1 || curve.acceptLimit(shownLevel - 1) != limit) {
            lore.add(Utils.fromString("<!i><gray>Nhận được <gold>" + limit + "</gold> nhiệm vụ cùng lúc"));
        }
        if (!rewards.isEmpty() && smpPlayer != null) {
            if (!lore.isEmpty()) lore.add(Component.empty());
            lore.add(Utils.text("🎁 Phần thưởng:", NamedTextColor.GREEN));
            for (QuestReward reward : rewards) {
                for (Component line : reward.getDisplay(smpPlayer)) {
                    lore.add(Component.text("  ").append(line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)));
                }
            }
        }
        if (!lore.isEmpty()) lore.add(Component.empty());
        if (unclaimed) lore.add(Utils.fromString("<!i><yellow>▶ Click để nhận phần thưởng"));
        else if (claimed) lore.add(Utils.fromString("<!i><green>✔ Đã nhận phần thưởng"));
        else if (reached) lore.add(Utils.fromString("<!i><green>✔ Đã đạt"));
        else if (shownLevel > 1) lore.add(Utils.fromString("<!i><gray>Cần <aqua>" + curve.expToNext(shownLevel - 1) + "</aqua> kinh nghiệm từ cấp " + (shownLevel - 1)));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
