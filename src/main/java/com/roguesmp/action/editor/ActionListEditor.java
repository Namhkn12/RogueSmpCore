package com.roguesmp.action.editor;

import com.roguesmp.action.ActionEntry;
import com.roguesmp.action.ClickTrigger;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import com.roguesmp.utils.dialog.JsonEntryDialog;
import com.roguesmp.utils.dialog.LenientJson;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ActionListEditor {

    private static final String NEW_ENTRY_TEMPLATE = "click: any, action: {type: }";

    private final String title;
    private final List<ActionEntry> entries;
    private final Consumer<List<ActionEntry>> onChange;
    private final Consumer<Player> onBack;

    public ActionListEditor(String title, List<ActionEntry> initialEntries, Consumer<List<ActionEntry>> onChange, Consumer<Player> onBack) {
        this.title = title;
        this.entries = new ArrayList<>(initialEntries);
        this.onChange = onChange;
        this.onBack = onBack;
    }

    public Dialog buildListDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text(title)).canCloseWithEscape(false);
        if (entries.isEmpty()) builder.addTextBody(Component.text("Chưa có action nào.", NamedTextColor.GRAY));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        for (int i = 0; i < entries.size(); i++) {
            int index = i;
            ActionEntry entry = entries.get(index);
            String click = entry.click() == ClickTrigger.ANY ? "" : " [" + entry.click().name().toLowerCase(Locale.ROOT) + "]";
            multi.addButton(Component.text((index + 1) + ". " + entry.action().getTypeId() + click),
                    Component.text(LenientJson.format(ActionEntry.CODEC, entry)),
                    (response, audience) -> Utils.runLater(() -> player.showDialog(entryDialog(player, index).build(player, entry))));
        }
        multi.addButton(Component.text("+ Thêm action", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(entryDialog(player, -1).buildWithText(player, NEW_ENTRY_TEMPLATE))));
        multi.columns(2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> onBack.accept(player)));
        return multi.build();
    }

    private JsonEntryDialog<ActionEntry> entryDialog(Player player, int index) {
        boolean editing = index >= 0;
        return JsonEntryDialog.of(editing ? "Action #" + (index + 1) : "Action mới", ActionEntry.CODEC,
                entry -> {
                    if (editing) entries.set(index, entry);
                    else entries.add(entry);
                    onChange.accept(List.copyOf(entries));
                    player.showDialog(buildListDialog(player));
                },
                editing ? () -> {
                    entries.remove(index);
                    onChange.accept(List.copyOf(entries));
                    player.showDialog(buildListDialog(player));
                } : null,
                () -> player.showDialog(buildListDialog(player)))
                .withHelp(List.of(
                        Component.text("click: ", NamedTextColor.YELLOW).append(Component.text("any, right, left (mặc định any)", NamedTextColor.GRAY)),
                        LenientJson.typesHelp(Registries.PLAYER_ACTION_CODEC.getAll().keySet())));
    }
}
