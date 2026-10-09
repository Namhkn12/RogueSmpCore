package com.roguesmp.region.editor;

import com.roguesmp.gui.BaseGui;
import com.roguesmp.region.ServerRegionManager;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.roguesmp.region.editor.RegionGuiItems.displayValue;
import static com.roguesmp.region.editor.RegionGuiItems.hint;
import static com.roguesmp.region.editor.RegionGuiItems.item;
import static com.roguesmp.region.editor.RegionGuiItems.line;
import static com.roguesmp.region.editor.RegionGuiItems.visibleFlags;

public class RegionFlagGui extends BaseGui {

    private static final int BACK_SLOT = 0;
    private static final int ADD_SLOT = 4;
    private static final int FLAGS_START = 9;
    private static final int MAX_FLAGS = 36;

    private final Player player;
    private final World world;
    private final String regionId;

    public RegionFlagGui(Player player, String regionId) {
        super(Component.text("Flags: " + regionId), 5);
        this.player = player;
        this.world = player.getWorld();
        this.regionId = regionId;
    }

    @Override
    public void setup() {
        ProtectedRegion region = ServerRegionManager.getInstance().get(world, regionId);
        if (region == null) {
            player.closeInventory();
            return;
        }

        clearUi();
        for (int i = 0; i < 9; i++) addButton(i, FILLER_BLACK, ClickHandler.noAction());

        addButton(BACK_SLOT, item(Material.ARROW, "Back", NamedTextColor.GRAY), event -> {
            event.setCancelled(true);
            Utils.runLater(() -> new RegionEditorGui(player, regionId).showInventory(player));
        });

        addButton(ADD_SLOT, item(Material.EMERALD, "Add flag", NamedTextColor.GREEN,
                line("Any WorldGuard flag, e.g. pvp = deny"), hint("Click")), event -> {
            event.setCancelled(true);
            openFlagDialog("", "", RegionGroup.ALL);
        });

        List<Map.Entry<Flag<?>, Object>> flags = visibleFlags(region);
        int slot = FLAGS_START;
        for (Map.Entry<Flag<?>, Object> entry : flags.subList(0, Math.min(MAX_FLAGS, flags.size()))) {
            Flag<?> flag = entry.getKey();
            String value = displayValue(flag, entry.getValue());
            RegionGroup group = groupOf(region, flag);
            addButton(slot++, item(Material.PAPER, flag.getName(), NamedTextColor.LIGHT_PURPLE,
                    line("Value: " + value),
                    line("Group: " + group.name().toLowerCase(Locale.ROOT)),
                    hint("Click: edit"),
                    hint("Shift + right click: remove")), event -> {
                event.setCancelled(true);
                if (event.getClick().isShiftClick() && event.getClick().isRightClick()) {
                    ServerRegionManager.getInstance().clearFlag(world, regionId, flag);
                    ServerRegionManager.getInstance().saveChanges(world);
                    setup();
                    return;
                }
                openFlagDialog(flag.getName(), value, group);
            });
        }
        fillEmpty(FILLER);
    }

    private RegionGroup groupOf(ProtectedRegion region, Flag<?> flag) {
        if (flag.getRegionGroupFlag() == null) return RegionGroup.ALL;

        RegionGroup group = region.getFlag(flag.getRegionGroupFlag());
        return group == null ? RegionGroup.ALL : group;
    }

    private void openFlagDialog(String name, String value, RegionGroup group) {
        Dialog dialog = DialogBuilder.create(Component.text("Region flag"))
                .addTextInput("name", Component.text("Flag name"), 300, true, name, 64, null)
                .addTextInput("value", Component.text("Value"), 300, true, value, 256, null)
                .addTextInput("group", Component.text("Group (all, members, owners, non_members, non_owners)"), 300, true,
                        group.name().toLowerCase(Locale.ROOT), 16, null)
                .confirmation()
                .yesButton(Component.text("Save"), null, (response, audience) -> {
                    String newName = clean(response.getText("name"));
                    String newValue = clean(response.getText("value"));
                    String newGroup = clean(response.getText("group"));
                    Utils.runLater(() -> save(newName, newValue, newGroup));
                })
                .noButton(Component.text("Cancel"), null, (response, audience) -> Utils.runLater(() -> showInventory(player)))
                .build();

        player.closeInventory();
        player.showDialog(dialog);
    }

    private void save(String name, String value, String groupText) {
        RegionGroup group = parseGroup(groupText);
        String error = group == null ? "Unknown group '" + groupText + "'." : ServerRegionManager.getInstance().flagError(name, value);
        if (error == null) {
            try {
                ServerRegionManager.getInstance().setFlag(world, regionId, name, value, group);
                ServerRegionManager.getInstance().saveChanges(world);
            } catch (InvalidFlagFormat e) {
                error = e.getMessage();
            }
        }

        if (error != null) player.sendMessage(Utils.fromString("<red>" + error));
        showInventory(player);
    }

    private static String clean(@Nullable String text) {
        return text == null ? "" : text.trim();
    }

    private static @Nullable RegionGroup parseGroup(String text) {
        if (text.isEmpty()) return RegionGroup.ALL;
        try {
            return RegionGroup.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
