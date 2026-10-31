package com.roguesmp.block.gui;

import com.google.gson.JsonElement;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.block.BlockData;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import com.roguesmp.utils.dialog.LenientJson;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class BlockJsonDialog {

    private static final String JSON = "block_json";

    private final String id;
    private final int returnPage;

    public BlockJsonDialog(String id, int returnPage) {
        this.id = id;
        this.returnPage = returnPage;
    }

    public void open(Player player) {
        BlockData block = Registries.BLOCK.get(id);
        if (block == null) {
            player.sendMessage(Utils.fromString("<red>Block '" + id + "' không còn tồn tại."));
            back(player);
            return;
        }
        DataResult<JsonElement> encoded = BlockData.CODEC.encode(block, JsonOps.INSTANCE);
        if (!encoded.isSuccess()) {
            player.sendMessage(Utils.fromString("<red>Không thể encode block '" + id + "': " + encoded.error()));
            back(player);
            return;
        }
        open(player, Utils.GSON.toJson(encoded.result()), null);
    }

    private void open(Player player, String text, @Nullable String error) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Block: " + id)).canCloseWithEscape(false);
        if (error != null) builder.addTextBody(Component.text(error, NamedTextColor.RED));
        builder.addTextBody(Component.text(LenientJson.HELP, NamedTextColor.DARK_GRAY))
                .addTextBody(LenientJson.typesHelp(Registries.BLOCK_TYPE.getAll().keySet()));
        LenientJson.addInput(builder, JSON, Component.text("JSON"), text);

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu", NamedTextColor.GOLD), null, (response, audience) -> {
            String edited = response.getText(JSON);
            Utils.runLater(() -> save(player, edited == null ? "" : edited));
        });
        multi.columns(1);
        multi.exitButton(Component.text("Quay lại"), (response, audience) -> Utils.runLater(() -> back(player)));
        player.showDialog(multi.build());
    }

    private void save(Player player, String text) {
        DataResult<BlockData> decoded = LenientJson.decode(BlockData.CODEC, text);
        if (!decoded.isSuccess()) {
            open(player, text, "JSON không hợp lệ: " + decoded.error());
            return;
        }

        Registries.BLOCK.registerAndSaveAsync(RogueSmpCore.getInstance(), id, decoded.result()).whenComplete((saved, error) -> Utils.runLater(() -> {
            if (error != null) {
                RogueSmpCore.LOGGER.error("Failed to save block {}", id, error);
                player.sendMessage(Utils.fromString("<red>Lưu block '" + id + "' thất bại, xem console."));
            } else {
                player.sendMessage(Utils.fromString("<green>Đã lưu block " + id + ". Block đã đặt sẽ cập nhật khi được load lại."));
            }
            back(player);
        }));
    }

    private void back(Player player) {
        new BlockBrowserGui(player, returnPage).showInventory(player);
    }
}
