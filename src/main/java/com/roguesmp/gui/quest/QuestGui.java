package com.roguesmp.gui.quest;

import com.roguesmp.quest.data.PlayerQuestData;
import com.roguesmp.quest.objective.ObjectiveProgress;
import com.roguesmp.quest.objective.SubmitItemObjective;
import com.roguesmp.quest.reward.QuestReward;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.inbox.InboxManager;
import com.roguesmp.player.inbox.gui.InboxGui;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.*;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class QuestGui extends BaseGui {

    private enum QuestFilter {
        ALL("Tất cả nhiệm vụ", Material.NETHER_STAR),
        ACTIVE("Nhiệm vụ đang làm", Material.COMPASS),
        COMPLETED("Nhiệm vụ đã xong", Material.CHEST);

        final String displayName;
        final Material icon;

        QuestFilter(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public QuestFilter next() {
            QuestFilter[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    private final Player player;
    private final PlayerQuestData questData;
    private final QuestManager questManager;
    private QuestFilter currentFilter = QuestFilter.ALL; // Đặt mặc định là hiển thị Tất cả

    public QuestGui(Player player, PlayerQuestData questData) {
        super(Component.text("Danh sách nhiệm vụ"), 6);
        this.player = player;
        this.questData = questData;
        this.questManager = QuestManager.getInstance();
    }

    @Override
    public void setup() {
        clearUi();

        for (int r = 0; r <= 5; r++) {
            for (int c = 0; c < 9; c++) {
                if (r == 0 || r == 5 || c == 0 || c == 8) {
                    addButton(r, c, FILLER_BLACK, ClickHandler.noAction());
                }
            }
        }

        setupActionButtons();

        Collection<QuestProgress> allProgress = questData.getQuestProgresses().values();
        List<QuestProgress> filteredProgress = allProgress.stream()
                .filter(qp -> {
                    // Xử lý bộ lọc logic mới bao gồm trường hợp ALL
                    if (currentFilter == QuestFilter.ALL) {
                        return true;
                    } else if (currentFilter == QuestFilter.COMPLETED) {
                        return qp.isCompleted();
                    } else {
                        return !qp.isCompleted();
                    }
                })
                .toList();

        int currentQuestIndex = 0;
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);

        for (int r = 1; r <= 4; r++) {
            for (int c = 1; c <= 7; c++) {
                if (currentQuestIndex >= filteredProgress.size()) break;

                QuestProgress qp = filteredProgress.get(currentQuestIndex);
                Quest quest = qp.getQuest();
                boolean completed = qp.isCompleted();

                String statusPrefix = completed ? "<gray><st>" : "<gold>";
                ItemStack questItem = completed ? ItemStack.of(Material.MINECART) : quest.createIcon();
                questItem.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hiddenComponents(Set.of(DataComponentTypes.ATTRIBUTE_MODIFIERS)).build());
                questItem.setData(DataComponentTypes.ITEM_NAME,
                        Utils.fromString(statusPrefix + (quest.getName() != null ? quest.getName() : "Nhiệm vụ không tên")));

                List<Component> loreLines = new ArrayList<>();

                if (quest.getDescription() != null) {
                    for (String descLine : quest.getDescription()) {
                        loreLines.add(Utils.fromString(descLine).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
                    }
                }

                loreLines.add(Component.empty());

                if (completed) {
                    loreLines.add(Utils.text("✔ Đã hoàn thành", NamedTextColor.GREEN));
                } else {
                    loreLines.add(Utils.text("✦ Mục tiêu:", NamedTextColor.AQUA));
                    quest.getObjectives().forEach((objectiveId, objective) -> {
                        ObjectiveProgress objProgress = qp.getProgressMap().get(objectiveId);
                        if (objProgress != null) {
                            List<Component> displayLines = objective.getDisplay(smpPlayer, objProgress);
                            for (Component line : displayLines) {
                                loreLines.add(Component.text("  ").append(line.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)));
                            }
                        }
                    });
                }

                if (!quest.getRewards().isEmpty()) {
                    loreLines.add(Component.empty());
                    loreLines.add(Utils.text("🎁 Phần thưởng:", NamedTextColor.GREEN));
                    for (QuestReward reward : quest.getRewards()) {
                        List<Component> rewardDisplays = reward.getDisplay(smpPlayer);
                        for (Component displayLine : rewardDisplays) {
                            loreLines.add(Component.text("  ").append(displayLine.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)));
                        }
                    }
                }

                boolean canSubmit = !completed && quest.hasObjective(SubmitItemObjective.class);
                if (canSubmit) {
                    loreLines.add(Component.empty());
                    loreLines.add(Utils.fromString("<!i><yellow>▶ Click để nộp vật phẩm"));
                }

                questItem.setData(DataComponentTypes.LORE, ItemLore.lore(loreLines));

                addButton(r, c, questItem, event -> {
                    event.setCancelled(true);
                    if (!canSubmit) return;
                    if (questManager.submitItems(player, quest)) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                        setup();
                    } else {
                        player.sendMessage(Utils.fromString("<red>Bạn chưa có đủ vật phẩm cần nộp."));
                    }
                });

                currentQuestIndex++;
            }
        }

        fillEmpty(FILLER);
    }

    private void setupActionButtons() {
        ItemStack filterButton = ItemStack.of(currentFilter.icon);

        List<Component> filterLore = new ArrayList<>();
        filterLore.add(Utils.text("Nhấn để chuyển chế độ lọc.", NamedTextColor.GRAY));
        filterLore.add(Component.empty());

        for (QuestFilter filter : QuestFilter.values()) {
            if (filter == currentFilter) {
                filterLore.add(Utils.fromString("<!i><green>▶ " + filter.displayName + " <bold>(Đang chọn)"));
            } else {
                filterLore.add(Utils.fromString("<!i><gray>▶ " + filter.displayName));
            }
        }

        filterButton.setData(DataComponentTypes.ITEM_NAME, Component.text("Lọc nhiệm vụ", NamedTextColor.GOLD));
        filterButton.setData(DataComponentTypes.LORE, ItemLore.lore(filterLore));

        addButton(5, 7, filterButton, event -> {
            event.setCancelled(true);
            this.currentFilter = this.currentFilter.next();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            setup();
        });

        int pending = InboxManager.getInstance().count(player.getUniqueId());
        ItemStack inboxItem = ItemStack.of(pending > 0 ? Material.CHEST_MINECART : Material.MINECART);
        inboxItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Hộp thư", NamedTextColor.GOLD));
        inboxItem.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.fromString("<!i><gray>Bạn có <gold>" + pending + "</gold> phần thưởng đang chờ."),
                Utils.fromString("<!i><yellow>▶ Click để mở"))));
        if (pending > 0) inboxItem.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        addButton(5, 1, inboxItem, event -> {
            event.setCancelled(true);
            new InboxGui(player, this).showInventory(player);
        });

        ItemStack closeItem = ItemStack.of(Material.BARRIER);
        closeItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Đóng", NamedTextColor.RED));
        addButton(5, 4, closeItem, event -> {
            event.setCancelled(true);
            event.getWhoClicked().closeInventory();
        });
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
