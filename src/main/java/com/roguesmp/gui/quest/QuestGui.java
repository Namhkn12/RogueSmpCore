package com.roguesmp.gui.quest;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.*;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
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
        super(Component.text("Nhiệm vụ"), 6);
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

                Material iconMaterial;
                String statusPrefix;

                if (qp.isRewardClaimed()) {
                    iconMaterial = Material.MINECART;
                    statusPrefix = "<gray><st>";
                } else if (qp.isCompleted()) {
                    iconMaterial = Material.CHEST_MINECART;
                    statusPrefix = "<b><green>";
                } else {
                    iconMaterial = Material.BOOK;
                    statusPrefix = "<gold>";
                }

                ItemStack questItem = ItemStack.of(iconMaterial);
                questItem.setData(DataComponentTypes.ITEM_NAME,
                        Utils.fromString(statusPrefix + (quest.getName() != null ? quest.getName() : "Nhiệm vụ không tên")));

                List<Component> loreLines = new ArrayList<>();

                if (quest.getDescription() != null) {
                    for (String descLine : quest.getDescription()) {
                        loreLines.add(Utils.fromString(descLine).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
                    }
                }

                loreLines.add(Component.empty());

                if (qp.isRewardClaimed()) {
                    loreLines.add(Utils.text("✕ Bạn đã nhận phần thưởng của nhiệm vụ này.", NamedTextColor.RED));
                } else if (qp.isCompleted()) {
                    loreLines.add(Utils.text("✔ Đã hoàn thành", NamedTextColor.GREEN));
                    loreLines.add(Utils.text("Click chuột để nhận thưởng", NamedTextColor.YELLOW));
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

                questItem.setData(DataComponentTypes.LORE, ItemLore.lore(loreLines));

                addButton(r, c, questItem, event -> {
                    event.setCancelled(true);

                    if (qp.isCompleted() && !qp.isRewardClaimed()) {
                        boolean success = questManager.claimQuestReward(player, quest, true, true);

                        if (success) {
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                            setup();
                        }
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
