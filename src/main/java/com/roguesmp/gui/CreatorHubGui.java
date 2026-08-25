package com.roguesmp.gui;

import com.roguesmp.gui.entitycreator.EntityCreatorGui;
import com.roguesmp.gui.itemcreator.ItemCreatorGui;
import com.roguesmp.gui.loottablecreator.LootTableGuiCreator;
import com.roguesmp.gui.recipecreator.RecipeCreatorGui;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * A launcher hub for every {@code smp*creator} tool ({@link ItemCreatorGui}, {@link EntityCreatorGui},
 * {@link LootTableGuiCreator}, {@link RecipeCreatorGui}) - one button each, straight into that
 * tool's own "new" flow. Built on the same Paper Dialog API {@code multiAction()} pattern as those
 * tools themselves (see e.g. {@code LootTableGuiCreator#openMainDialog}).
 * <p>
 * Doesn't replace the per-tool {@code /smp<domain>creator edit <id>} commands - editing an existing
 * entry needs typing an id, which a button grid can't do, so this hub only ever launches a fresh one.
 */
public final class CreatorHubGui {

    private CreatorHubGui() {
    }

    public static void open(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Trình Tạo - Menu Chính"))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("Chọn loại nội dung muốn tạo.", NamedTextColor.GRAY))
                .addTextBody(Component.text("Để chỉnh sửa nội dung đã có, dùng /smp<loại>creator edit <id>.", NamedTextColor.DARK_GRAY));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();

        multi.addButton(Component.text("Vật phẩm (item)", NamedTextColor.AQUA), Component.text("/smpitemcreator new"),
                (response, audience) -> Utils.runLater(() -> new ItemCreatorGui().openMainDialog(player)));

        multi.addButton(Component.text("Entity", NamedTextColor.AQUA), Component.text("/smpentitycreator new"),
                (response, audience) -> Utils.runLater(() -> new EntityCreatorGui().openMainDialog(player)));

        multi.addButton(Component.text("Loot Table", NamedTextColor.AQUA), Component.text("/smplootcreator new"),
                (response, audience) -> Utils.runLater(() -> new LootTableGuiCreator().openMainDialog(player)));

        multi.addButton(Component.text("Công thức chế tạo (recipe)", NamedTextColor.AQUA), Component.text("/smprecipecreator new"),
                (response, audience) -> Utils.runLater(() -> new RecipeCreatorGui().openKindPickerDialog(player)));

        multi.columns(2);
        multi.exitButton(Component.text("Đóng"), (response, audience) -> audience.closeDialog());

        player.showDialog(multi.build());
    }

    public static void registerCommand() {
        new CommandAPICommand("smpcreator")
                .executesPlayer((player, args) -> {
                    open(player);
                })
                .register();
    }
}
