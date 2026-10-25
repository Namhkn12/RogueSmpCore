package com.roguesmp.player.inbox.gui;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.inbox.Inbox;
import com.roguesmp.player.inbox.InboxEntry;
import com.roguesmp.player.inbox.InboxManager;
import com.roguesmp.quest.reward.QuestReward;
import com.roguesmp.utils.DateUtils;
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
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class InboxGui extends BaseGui {

    private static final int ENTRIES_PER_PAGE = 28;
    private static final long EXPIRING_SOON_MILLIS = Duration.ofDays(1).toMillis();

    private final Player player;
    private final @Nullable BaseGui parent;
    private final InboxManager inboxManager;
    private int page;

    public InboxGui(Player player, @Nullable BaseGui parent) {
        super(Component.text("Hộp thư"), 6);
        this.player = player;
        this.parent = parent;
        this.inboxManager = InboxManager.getInstance();
    }

    @Override
    public void setup() {
        clearUi();

        for (int r = 0; r <= 5; r++) {
            for (int c = 0; c < 9; c++) {
                if (r == 0 || r == 5 || c == 0 || c == 8) addButton(r, c, FILLER_BLACK, ClickHandler.noAction());
            }
        }

        addButton(0, 4, buildWarningItem(), ClickHandler.noAction());

        Inbox inbox = inboxManager.getInbox(player.getUniqueId());
        List<InboxEntry> entries = inbox == null ? List.of() : inbox.getEntries().reversed();
        int pageCount = Math.max(1, (entries.size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE);
        page = Math.min(page, pageCount - 1);

        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        long now = System.currentTimeMillis();
        int index = page * ENTRIES_PER_PAGE;
        for (int r = 1; r <= 4; r++) {
            for (int c = 1; c <= 7; c++) {
                if (index >= entries.size()) break;
                InboxEntry entry = entries.get(index++);
                addButton(r, c, buildEntryItem(entry, smpPlayer, now), event -> {
                    event.setCancelled(true);
                    if (inboxManager.claim(player, entry.id())) player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    setup();
                });
            }
        }

        setupActionButtons(entries.size(), pageCount);
        fillEmpty(FILLER);
    }

    private ItemStack buildWarningItem() {
        ItemStack item = ItemStack.of(Material.CLOCK);
        item.setData(DataComponentTypes.ITEM_NAME, Utils.fromString("<red><b>⚠ Lưu ý"));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.fromString("<!i><gray>Phần thưởng trong hộp thư sẽ <red>bị xóa</red>"),
                Utils.fromString("<!i><gray>sau <yellow>" + InboxEntry.CLAIM_WINDOW.toDays() + " ngày</yellow> nếu không được nhận."))));
        return item;
    }

    private ItemStack buildEntryItem(InboxEntry entry, @Nullable SmpPlayer smpPlayer, long now) {
        ItemStack item = entry.source().createIcon();
        item.setData(DataComponentTypes.ITEM_NAME, Utils.fromString("<green>" + entry.source().name()));

        long remaining = entry.expiresAt() - now;
        String remainingColor = remaining <= EXPIRING_SOON_MILLIS ? "<red>" : "<white>";

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.fromString("<!i><gray>Nguồn: <white>" + entry.source().origin()));
        lore.add(Utils.fromString("<!i><gray>Nhận lúc: <white>" + DateUtils.toZonedDateTime(entry.createdAt()).toLocalDate()));
        lore.add(Utils.fromString("<!i><gray>Hết hạn sau: " + remainingColor + DateUtils.formatRemaining(remaining)));

        if (!entry.rewards().isEmpty() && smpPlayer != null) {
            lore.add(Component.empty());
            lore.add(Utils.fromString("<!i><gold><bold>Phần thưởng:"));
            for (QuestReward reward : entry.rewards()) {
                for (Component line : reward.getDisplay(smpPlayer)) lore.add(Utils.fromString("<!i><gray>• ").append(line));
            }
        }

        lore.add(Component.empty());
        lore.add(Utils.fromString("<!i><yellow>▶ Click để nhận thưởng!"));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }

    private void setupActionButtons(int entryCount, int pageCount) {
        if (parent != null) {
            ItemStack backItem = ItemStack.of(Material.ARROW);
            backItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Quay lại", NamedTextColor.YELLOW));
            addButton(5, 0, backItem, event -> {
                event.setCancelled(true);
                parent.showInventory(player);
            });
        }

        if (page > 0) {
            addButton(5, 2, PREV_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                page--;
                setup();
            });
        }

        ItemStack claimAllItem = ItemStack.of(entryCount > 0 ? Material.CHEST_MINECART : Material.MINECART);
        claimAllItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Nhận tất cả", NamedTextColor.GOLD));
        claimAllItem.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.fromString("<!i><gray>Bạn có <gold>" + entryCount + "</gold> phần thưởng đang chờ."))));
        addButton(5, 3, claimAllItem, event -> {
            event.setCancelled(true);
            if (inboxManager.claimAll(player) > 0) player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            setup();
        });

        ItemStack closeItem = ItemStack.of(Material.BARRIER);
        closeItem.setData(DataComponentTypes.ITEM_NAME, Component.text("Đóng", NamedTextColor.RED));
        addButton(5, 5, closeItem, event -> {
            event.setCancelled(true);
            event.getWhoClicked().closeInventory();
        });

        if (page < pageCount - 1) {
            addButton(5, 6, NEXT_PAGE_BUTTON, event -> {
                event.setCancelled(true);
                page++;
                setup();
            });
        }
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
