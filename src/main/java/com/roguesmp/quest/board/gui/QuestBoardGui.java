package com.roguesmp.quest.board.gui;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.inbox.InboxManager;
import com.roguesmp.player.inbox.gui.InboxGui;
import com.roguesmp.quest.Quest;
import com.roguesmp.quest.QuestProgress;
import com.roguesmp.quest.board.BoardEntry;
import com.roguesmp.quest.board.AdventureRank;
import com.roguesmp.quest.board.QuestBoardManager;
import com.roguesmp.quest.board.QuestLevelCurve;
import com.roguesmp.quest.data.PlayerQuestData;
import com.roguesmp.quest.objective.SubmitItemObjective;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.quest.reward.QuestReward;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.DateUtils;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
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
import java.util.Set;

public class QuestBoardGui extends BaseGui {

    private final Player player;
    private final QuestManager questManager;
    private final QuestBoardManager boardManager;

    public QuestBoardGui(Player player) {
        super(Component.text("Bảng nhiệm vụ"), 6);
        this.player = player;
        this.questManager = QuestManager.getInstance();
        this.boardManager = QuestBoardManager.getInstance();
    }

    @Override
    public void setup() {
        clearUi();

        for (int r = 0; r <= 5; r++) {
            for (int c = 0; c < 9; c++) {
                if (r == 0 || r == 5 || c == 0 || c == 8) addButton(r, c, FILLER_BLACK, ClickHandler.noAction());
            }
        }
        addBackButton(getSlot(5, 0));

        boardManager.refresh(player);
        PlayerQuestData data = questManager.getPlayerQuestData(player.getUniqueId());
        if (data == null) {
            fillEmpty(FILLER);
            return;
        }

        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        long now = System.currentTimeMillis();
        List<BoardEntry> entries = data.getBoard().getEntries();
        int index = 0;
        for (int r = 1; r <= 4; r++) {
            for (int c = 1; c <= 7; c++) {
                if (index >= entries.size()) break;
                BoardEntry entry = entries.get(index++);
                Quest quest = Registries.QUEST.get(entry.questId());
                if (quest == null) continue;
                QuestProgress progress = data.getQuestProgress(quest.getId());
                addButton(r, c, buildQuestItem(quest, progress, smpPlayer, now), event -> {
                    event.setCancelled(true);
                    onQuestClicked(event, quest, progress);
                });
            }
        }

        setupActionButtons(data);
        fillEmpty(FILLER);
    }

    private void onQuestClicked(InventoryClickEvent event, Quest quest, @Nullable QuestProgress progress) {
        if (progress == null) {
            QuestBoardManager.AcceptResult result = boardManager.accept(player, quest);
            player.sendMessage(Utils.fromString(result.getMessage()));
            if (result == QuestBoardManager.AcceptResult.ACCEPTED) player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        } else if (event.isShiftClick() && event.isRightClick()) {
            openAbandonDialog(quest);
            return;
        } else if (event.isLeftClick() && !event.isShiftClick() && quest.hasObjective(SubmitItemObjective.class)) {
            if (questManager.submitItems(player, quest)) {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            } else {
                player.sendMessage(Utils.fromString("<red>Bạn chưa có đủ vật phẩm cần nộp."));
            }
        } else {
            return;
        }
        setup();
    }

    private void openAbandonDialog(Quest quest) {
        String name = quest.getName() != null ? quest.getName() : quest.getId();
        DialogBuilder builder = DialogBuilder.create(Utils.fromString("<red>Hủy nhiệm vụ?"))
                .addTextBody(Utils.fromString(name))
                .addTextBody(Component.text("Toàn bộ tiến độ của nhiệm vụ này sẽ bị mất.", NamedTextColor.GRAY));
        Dialog dialog = builder.confirmation()
                .yesButton(Component.text("Hủy nhiệm vụ", NamedTextColor.RED), null, (response, audience) -> Utils.runLater(() -> {
                    if (boardManager.abandon(player, quest)) {
                        player.sendMessage(Utils.fromString("<yellow>Đã hủy nhiệm vụ. Tiến độ đã được đặt lại."));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                    showInventory(player);
                }))
                .noButton(Component.text("Giữ lại"), null, (response, audience) -> Utils.runLater(() -> showInventory(player)))
                .build();

        player.closeInventory();
        player.showDialog(dialog);
    }

    private ItemStack buildQuestItem(Quest quest, @Nullable QuestProgress progress, @Nullable SmpPlayer smpPlayer, long now) {
        boolean accepted = progress != null;
        ItemStack item = quest.createIcon();
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hiddenComponents(Set.of(DataComponentTypes.ATTRIBUTE_MODIFIERS)).build());
        String name = quest.getName() != null ? quest.getName() : quest.getId();
        item.setData(DataComponentTypes.ITEM_NAME, Utils.fromString(accepted ? "<green><b>✔ </b></green><b>" + name + "</b> <green><b>[ĐANG LÀM]" : "<gold>" + name));
        if (accepted) item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        List<Component> lore = new ArrayList<>();
        for (String line : quest.getDescription()) lore.add(nonItalic(Utils.fromString(line)));
        lore.add(Component.empty());

        QuestProgress shown = accepted ? progress : new QuestProgress(quest);
        lore.add(Utils.text("✦ Mục tiêu:", NamedTextColor.AQUA));
        for (Component line : shown.getDisplay(smpPlayer)) lore.add(Component.text("  ").append(nonItalic(line)));

        if (!quest.getRewards().isEmpty() && smpPlayer != null) {
            lore.add(Component.empty());
            lore.add(Utils.text("🎁 Phần thưởng:", NamedTextColor.GREEN));
            for (QuestReward reward : quest.getRewards()) {
                for (Component line : reward.getDisplay(smpPlayer)) lore.add(Component.text("  ").append(nonItalic(line)));
            }
        }

        lore.add(Component.empty());
        long resetAt = quest.getBoard().reset().cycleEnd(quest.cycleAt(now));
        lore.add(Utils.fromString("<!i><gray>Làm mới sau: <white>" + DateUtils.formatRemaining(resetAt - now)));
        if (accepted) {
            lore.add(Utils.fromString("<!i><green>✔ Đang thực hiện"));
            if (quest.hasObjective(SubmitItemObjective.class)) lore.add(Utils.fromString("<!i><yellow>▶ Chuột trái để nộp vật phẩm"));
            lore.add(Utils.fromString("<!i><red>Shift + Chuột phải để hủy (mất tiến độ)"));
        } else {
            lore.add(Utils.fromString("<!i><yellow>▶ Click để nhận nhiệm vụ"));
        }
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }


    private static Component nonItalic(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private void setupActionButtons(PlayerQuestData data) {
        QuestLevelCurve curve = boardManager.getSettings().level();
        int level = boardManager.getLevel(data);
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Utils.fromString("<!i><gray>Đang nhận: <gold>" + boardManager.getAcceptedCount(data) + "<gray>/<gold>" + boardManager.getAcceptLimit(data)));
        if (level < curve.maxLevel()) {
            infoLore.add(Utils.fromString("<!i><gray>Kinh nghiệm Mạo hiểm: <aqua>" + curve.expIntoLevel(data.getExp()) + "<gray>/<aqua>" + curve.expToNext(level)));
            Map.Entry<Integer, Integer> nextLimit = curve.acceptLimits().higherEntry(level);
            if (nextLimit != null) infoLore.add(Utils.fromString("<!i><gray>Cấp <gold>" + nextLimit.getKey() + "</gold>: nhận được <gold>" + nextLimit.getValue() + "</gold> nhiệm vụ"));
        } else {
            infoLore.add(Utils.fromString("<!i><gold>Đã đạt cấp tối đa"));
        }
        infoLore.add(Utils.fromString("<!i><gray>Hoàn thành nhiệm vụ để nhận nhiệm vụ mới."));
        int claimable = boardManager.countClaimableRewards(data);
        if (claimable > 0) infoLore.add(Utils.fromString("<!i><gold>🎁 " + claimable + " phần thưởng chưa nhận"));
        infoLore.add(Utils.fromString("<!i><yellow>▶ Click để xem phần thưởng cấp"));
        AdventureRank rank = boardManager.getRank(data);
        ItemStack infoItem = ItemStack.of(Material.WRITABLE_BOOK);
        if (!rank.model().isEmpty()) infoItem.setData(DataComponentTypes.ITEM_MODEL, Key.key(rank.model()));
        infoItem.setData(DataComponentTypes.ITEM_NAME, Utils.fromString(rank.name() + " <dark_gray>- <gold>Cấp Mạo hiểm " + level));
        infoItem.setData(DataComponentTypes.LORE, ItemLore.lore(infoLore));
        if (claimable > 0) infoItem.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        addButton(5, 1, infoItem, ClickHandler.openGui(new QuestLevelGui(player, this)));

        ItemStack closeItem = ItemStack.of(Material.BARRIER);
        closeItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Đóng", NamedTextColor.RED));
        addButton(5, 4, closeItem, event -> {
            event.setCancelled(true);
            event.getWhoClicked().closeInventory();
        });

        int pending = InboxManager.getInstance().count(player.getUniqueId());
        ItemStack inboxItem = ItemStack.of(pending > 0 ? Material.CHEST_MINECART : Material.MINECART);
        inboxItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Hộp thư", NamedTextColor.GOLD));
        inboxItem.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.fromString("<!i><gray>Bạn có <gold>" + pending + "</gold> phần thưởng đang chờ."),
                Utils.fromString("<!i><yellow>▶ Click để mở"))));
        if (pending > 0) inboxItem.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        addButton(5, 7, inboxItem, event -> {
            event.setCancelled(true);
            new InboxGui(player, this).showInventory(player);
        });
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
