package com.roguesmp.gui.quest;

import com.roguesmp.quest.data.PendingQuestReward;
import com.roguesmp.quest.data.PlayerQuestData;
import com.roguesmp.quest.reward.QuestReward;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.quest.Quest;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class RewardInboxGui extends BaseGui {

    private final Player player;
    private final PlayerQuestData questData;
    private final QuestManager questManager;

    public RewardInboxGui(Player player, PlayerQuestData questData) {
        super(Component.text("Phần thưởng nhiệm vụ"), 6);
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

        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        List<PendingQuestReward> rewards = questData.getPendingQuestRewards();
        int index = 0;
        for (int r = 1; r <= 4; r++) {
            for (int c = 1; c <= 7; c++) {
                if (index >= rewards.size()) break;
                PendingQuestReward reward = rewards.get(index++);
                Quest quest = Registries.QUEST.get(reward.questId());
                if (quest == null) continue;

                addButton(r, c, buildRewardItem(reward, quest, smpPlayer), event -> {
                    event.setCancelled(true);
                    if (questManager.claimReward(player, reward.source(), reward.questId())) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                        setup();
                    }
                });
            }
        }

        fillEmpty(FILLER);
    }

    private ItemStack buildRewardItem(PendingQuestReward reward, Quest quest, SmpPlayer smpPlayer) {
        boolean personal = reward.source().equals(player.getUniqueId());
        ItemStack item = ItemStack.of(personal ? Material.CHEST_MINECART : Material.ENDER_CHEST);
        String name = quest.getName() != null ? quest.getName() : quest.getId();
        item.setData(DataComponentTypes.ITEM_NAME, Utils.fromString("<b><green>" + name));

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.fromString(personal ? "<!i><gray>Nhiệm vụ cá nhân" : "<!i><gray>Nhiệm vụ nhóm"));
        lore.add(Utils.fromString("<!i><gray>Hết hạn sau: <white>" + formatRemaining(reward.expiresAt() - System.currentTimeMillis())));

        if (!quest.getRewards().isEmpty() && smpPlayer != null) {
            lore.add(Component.empty());
            lore.add(Utils.fromString("<!i><gold><bold>Phần thưởng:"));
            for (QuestReward questReward : quest.getRewards()) {
                for (Component line : questReward.getDisplay(smpPlayer)) {
                    lore.add(Utils.fromString("<!i><gray>• ").append(line));
                }
            }
        }

        lore.add(Component.empty());
        lore.add(Utils.fromString("<!i><yellow>▶ Click để nhận thưởng!"));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    private static String formatRemaining(long millis) {
        Duration remaining = Duration.ofMillis(Math.max(0, millis));
        if (remaining.toDays() > 0) return remaining.toDays() + " ngày " + remaining.toHoursPart() + " giờ";
        if (remaining.toHours() > 0) return remaining.toHours() + " giờ " + remaining.toMinutesPart() + " phút";
        return remaining.toMinutes() + " phút";
    }

    private void setupActionButtons() {
        ItemStack backItem = ItemStack.of(Material.ARROW);
        backItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Quay lại", NamedTextColor.YELLOW));
        addButton(5, 1, backItem, event -> {
            event.setCancelled(true);
            new QuestGui(player, questData).showInventory(player);
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
