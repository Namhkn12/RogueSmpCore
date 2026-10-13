package com.roguesmp.gui.dungeon;

import com.roguesmp.dungeon_core.DungeonDefinition;
import com.roguesmp.dungeon_core.DungeonInstance;
import com.roguesmp.dungeon_core.DungeonManager;
import com.roguesmp.dungeon_core.DungeonPlayerActions;
import com.roguesmp.dungeon_core.FeatureContext;
import com.roguesmp.dungeon_core.feature.ExpiryFeature;
import com.roguesmp.dungeon_core.feature.MembershipFeature;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.DateUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DungeonDetailGui extends BaseGui {

    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final Player player;
    private final String dungeonId;

    public DungeonDetailGui(Player player, String dungeonId) {
        super(Component.text("Phó bản"), 5);
        this.player = player;
        this.dungeonId = dungeonId;
    }

    @Override
    public void setup() {
        clearUi();
        DungeonDefinition definition = Registries.DUNGEON.get(dungeonId);
        if (definition == null) {
            player.closeInventory();
            return;
        }

        DungeonManager manager = DungeonManager.getInstance();
        addButton(0, 4, definition.toIcon(List.of()), ClickHandler.noAction());

        DungeonInstance run = manager.findFor(player.getUniqueId(), dungeonId);
        if (run != null) setupRun(run);
        else setupNoRun(manager);

        addButton(4, 0, button(Material.ARROW, "<yellow>Quay lại", List.of()), event -> {
            event.setCancelled(true);
            new DungeonBrowserGui(player).showInventory(player);
        });
        fillEmpty(FILLER);
    }

    private void setupRun(DungeonInstance run) {
        List<Component> runLore = new ArrayList<>();
        runLore.add(Utils.fromString("<gray>Chủ phó bản: <white>" + name(run.ownerId())));
        runLore.add(Utils.fromString("<gray>Thành viên: <white>" + run.members().size()));
        FeatureContext<ExpiryFeature.State> expiry = DungeonManager.getInstance().feature(run, ExpiryFeature.class);
        if (expiry != null) {
            runLore.add(Utils.fromString("<gray>Hạn: <white>" + DateUtils.toZonedDateTime(expiry.state().expiresAt()).format(DEADLINE_FORMAT)));
        }
        runLore.add(Component.empty());
        runLore.add(Utils.fromString("<yellow>▶ Click để vào phó bản"));
        addButton(2, 2, button(Material.ENDER_PEARL, "<green><b>Vào lại phó bản", runLore), event -> {
            event.setCancelled(true);
            player.closeInventory();
            DungeonPlayerActions.enter(player, run);
        });

        MembershipFeature membership = DungeonPlayerActions.membershipFeature(run);
        boolean owner = run.ownerId().equals(player.getUniqueId());
        if (membership != null && membership.invites() && owner) {
            addButton(2, 4, button(Material.PLAYER_HEAD, "<aqua><b>Mời người chơi", List.of(Utils.fromString("<yellow>▶ Click để chọn người chơi"))), event -> {
                event.setCancelled(true);
                new DungeonInviteGui(player, run).showInventory(player);
            });
        }
        if (membership != null && membership.abandon()) {
            String warning = owner ? "<red>Bạn là chủ: phó bản sẽ bị huỷ cho mọi người!" : "<gray>Bạn sẽ không thể vào lại nếu không được mời.";
            addButton(2, 6, button(Material.BARRIER, "<red><b>Rời bỏ phó bản", List.of(Utils.fromString(warning))), event -> {
                event.setCancelled(true);
                new DungeonAbandonGui(player, run).showInventory(player);
            });
        }
    }

    private void setupNoRun(DungeonManager manager) {
        addButton(2, 4, button(Material.LIME_CONCRETE, "<green><b>Tạo phó bản", List.of(Utils.fromString("<yellow>▶ Click để tạo và vào phó bản mới"))), event -> {
            event.setCancelled(true);
            player.closeInventory();
            DungeonPlayerActions.create(player, dungeonId);
        });

        List<DungeonInstance> invites = MembershipFeature.invitesFor(manager, player.getUniqueId(), dungeonId);
        for (int i = 0; i < Math.min(invites.size(), 7); i++) {
            DungeonInstance invite = invites.get(i);
            ItemStack head = button(Material.PLAYER_HEAD, "<aqua>Lời mời từ <white>" + name(invite.ownerId()),
                    List.of(Utils.fromString("<gray>Thành viên: <white>" + invite.members().size()), Utils.fromString("<yellow>▶ Click để tham gia")));
            OfflinePlayer owner = Bukkit.getOfflinePlayer(invite.ownerId());
            head.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(owner.getPlayerProfile()));
            addButton(3, 1 + i, head, event -> {
                event.setCancelled(true);
                player.closeInventory();
                DungeonPlayerActions.acceptInvite(player, invite);
            });
        }
    }

    static ItemStack button(Material material, String name, List<Component> lore) {
        ItemStack item = ItemStack.of(material);
        item.setData(DataComponentTypes.ITEM_NAME, Utils.fromString(name));
        if (!lore.isEmpty()) item.setData(DataComponentTypes.LORE, ItemLore.lore(lore.stream().map(line -> Utils.fromString("<!i>").append(line)).toList()));
        return item;
    }

    static String name(UUID playerId) {
        String name = Bukkit.getOfflinePlayer(playerId).getName();
        return name != null ? name : playerId.toString();
    }

    @Override
    public void onClickBottomInventory(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
