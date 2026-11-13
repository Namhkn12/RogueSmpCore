package com.roguesmp.billboard.gui;

import com.roguesmp.billboard.Billboard;
import com.roguesmp.billboard.BillboardManager;
import com.roguesmp.billboard.DisplaySettings;
import com.roguesmp.command.CommandFeedback;
import com.roguesmp.gui.BaseGui;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.JsonEntryDialog;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

import static com.roguesmp.gui.GuiItems.hint;
import static com.roguesmp.gui.GuiItems.item;
import static com.roguesmp.gui.GuiItems.line;

public class DisplaySettingsGui extends BaseGui {

    private static final List<Color> BACKGROUNDS = List.of(
            Color.fromARGB(0x00000000),
            Color.fromARGB(0x40000000),
            Color.fromARGB(0x80000000),
            Color.fromARGB(0xC0000000),
            Color.fromARGB(0x40FFFFFF)
    );

    private final Player player;
    private final String billboardId;
    private final BaseGui parent;

    public DisplaySettingsGui(Player player, String billboardId, BaseGui parent) {
        super(Component.text("Display: " + billboardId), 3);
        this.player = player;
        this.billboardId = billboardId;
        this.parent = parent;
    }

    @Override
    public void setup() {
        Billboard billboard = Registries.BILLBOARD.get(billboardId);
        if (billboard == null) {
            player.closeInventory();
            return;
        }
        clearUi();
        DisplaySettings s = billboard.display();

        addButton(0, item(Material.ARROW, "Back", NamedTextColor.GRAY), event -> {
            event.setCancelled(true);
            Utils.runLater(() -> parent.showInventory(player));
        });
        addButton(4, item(Material.PAINTING, "Display defaults", NamedTextColor.LIGHT_PURPLE,
                line("Every line uses these unless it sets its own"),
                line("Unset values fall back to the built-in default")), ClickHandler.noAction());
        addButton(8, item(Material.WRITABLE_BOOK, "Edit as JSON", NamedTextColor.LIGHT_PURPLE, hint("Click")), event -> {
            event.setCancelled(true);
            Dialog dialog = JsonEntryDialog.of("Display: " + billboardId, DisplaySettings.CODEC,
                    updated -> change(current -> updated), null, () -> showInventory(player)).build(player, s);
            player.closeInventory();
            player.showDialog(dialog);
        });

        addButton(10, setting(Material.COMPASS, "Billboard mode", s.billboard(), DisplaySettings.DEFAULT.billboard(), "Click: cycle"), event -> {
            Display.Billboard next = cycle(Display.Billboard.values(), s.billboard(), event);
            change(event, current -> withBillboard(current, next));
        });
        addButton(11, setting(Material.GLOWSTONE_DUST, "Scale", s.scale(), DisplaySettings.DEFAULT.scale(),
                "Left / right: +0.1 / -0.1", "Shift: +0.5 / -0.5"), event -> {
            float base = s.scale() != null ? s.scale() : DisplaySettings.DEFAULT.scale();
            float next = Math.max(0.1f, Math.round((base + (float) step(event, 0.1, 0.5)) * 100f) / 100f);
            change(event, current -> withScale(current, next));
        });
        addButton(12, setting(Material.INK_SAC, "Shadow", s.shadow(), DisplaySettings.DEFAULT.shadow(), "Click: toggle"), event -> {
            boolean next = !(s.shadow() != null ? s.shadow() : DisplaySettings.DEFAULT.shadow());
            change(event, current -> withShadow(current, next));
        });
        addButton(13, setting(Material.GLASS, "See through walls", s.seeThrough(), DisplaySettings.DEFAULT.seeThrough(), "Click: toggle"), event -> {
            boolean next = !(s.seeThrough() != null ? s.seeThrough() : DisplaySettings.DEFAULT.seeThrough());
            change(event, current -> withSeeThrough(current, next));
        });
        addButton(14, setting(Material.BLACK_STAINED_GLASS_PANE, "Background",
                s.background() == null ? null : String.format("#%08X", s.background().asARGB()), "vanilla", "Click: cycle presets",
                "Custom colors: edit as JSON"), event -> {
            Color next = cycle(BACKGROUNDS.toArray(Color[]::new), s.background(), event);
            change(event, current -> withBackground(current, next));
        });
        addButton(15, setting(Material.PAPER, "Alignment", s.alignment(), DisplaySettings.DEFAULT.alignment(), "Click: cycle"), event -> {
            TextDisplay.TextAlignment next = cycle(TextDisplay.TextAlignment.values(), s.alignment(), event);
            change(event, current -> withAlignment(current, next));
        });
        addButton(16, setting(Material.LEAD, "Line width", s.lineWidth(), DisplaySettings.DEFAULT.lineWidth(),
                "Left / right: +10 / -10", "Shift: +50 / -50"), event -> {
            int base = s.lineWidth() != null ? s.lineWidth() : DisplaySettings.DEFAULT.lineWidth();
            int next = Math.max(10, base + (int) step(event, 10, 50));
            change(event, current -> withLineWidth(current, next));
        });

        fillEmpty(FILLER);
    }

    private static org.bukkit.inventory.ItemStack setting(Material material, String name, @Nullable Object value, @Nullable Object fallback, String... hints) {
        List<Component> lore = new ArrayList<>();
        lore.add(line(value != null ? "Value: " + value : "Not set (uses " + fallback + ")"));
        Arrays.stream(hints).map(text -> hint(text)).forEach(lore::add);
        lore.add(hint("Q: unset"));
        return item(material, name, value != null ? NamedTextColor.AQUA : NamedTextColor.GRAY, lore.toArray(Component[]::new));
    }

    private static <T> T cycle(T[] values, @Nullable T current, InventoryClickEvent event) {
        int index = current == null ? -1 : Arrays.asList(values).indexOf(current);
        int direction = event.getClick().isRightClick() ? -1 : 1;
        return values[Math.floorMod(index + direction, values.length)];
    }

    private static double step(InventoryClickEvent event, double small, double large) {
        double amount = event.getClick().isShiftClick() ? large : small;
        return event.getClick().isRightClick() ? -amount : amount;
    }

    private void change(InventoryClickEvent event, UnaryOperator<DisplaySettings> set) {
        event.setCancelled(true);
        if (event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) {
            change(current -> unset(current, event.getSlot()));
            return;
        }
        change(set);
    }

    private void change(UnaryOperator<DisplaySettings> set) {
        CommandFeedback.failed(player, BillboardManager.getInstance().update(billboardId, billboard -> billboard.withDisplay(set.apply(billboard.display()))));
        showInventory(player);
    }

    private static DisplaySettings unset(DisplaySettings s, int slot) {
        return new DisplaySettings(
                slot == 10 ? null : s.billboard(),
                slot == 11 ? null : s.scale(),
                slot == 12 ? null : s.shadow(),
                slot == 13 ? null : s.seeThrough(),
                slot == 14 ? null : s.background(),
                slot == 15 ? null : s.alignment(),
                slot == 16 ? null : s.lineWidth());
    }

    private static DisplaySettings withBillboard(DisplaySettings s, Display.Billboard value) {
        return new DisplaySettings(value, s.scale(), s.shadow(), s.seeThrough(), s.background(), s.alignment(), s.lineWidth());
    }

    private static DisplaySettings withScale(DisplaySettings s, float value) {
        return new DisplaySettings(s.billboard(), value, s.shadow(), s.seeThrough(), s.background(), s.alignment(), s.lineWidth());
    }

    private static DisplaySettings withShadow(DisplaySettings s, boolean value) {
        return new DisplaySettings(s.billboard(), s.scale(), value, s.seeThrough(), s.background(), s.alignment(), s.lineWidth());
    }

    private static DisplaySettings withSeeThrough(DisplaySettings s, boolean value) {
        return new DisplaySettings(s.billboard(), s.scale(), s.shadow(), value, s.background(), s.alignment(), s.lineWidth());
    }

    private static DisplaySettings withBackground(DisplaySettings s, Color value) {
        return new DisplaySettings(s.billboard(), s.scale(), s.shadow(), s.seeThrough(), value, s.alignment(), s.lineWidth());
    }

    private static DisplaySettings withAlignment(DisplaySettings s, TextDisplay.TextAlignment value) {
        return new DisplaySettings(s.billboard(), s.scale(), s.shadow(), s.seeThrough(), s.background(), value, s.lineWidth());
    }

    private static DisplaySettings withLineWidth(DisplaySettings s, int value) {
        return new DisplaySettings(s.billboard(), s.scale(), s.shadow(), s.seeThrough(), s.background(), s.alignment(), value);
    }
}
