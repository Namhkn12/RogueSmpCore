package com.roguesmp.gui.npccreator;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.npc.BaseNpc;
import com.roguesmp.npc.action.NpcAction;
import com.roguesmp.npc.action.OpenGuiInteractAction;
import com.roguesmp.npc.action.RunCommandInteractAction;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * In-game editor for {@link BaseNpc}, built on the same Paper Dialog API pattern as
 * {@code gui.loottablecreator.LootTableGuiCreator} - a main {@code multiAction()} menu
 * (id + entity_type + name + skin + description + one button per action + add-action + save +
 * delete), where each action hands off to its own entry dialog by kind ({@code run_command}/
 * {@code open_gui}), mirroring {@code LootTableGuiCreator}'s entry-kind-picker shape.
 * <p>
 * Scope: {@link BaseNpc#getSkinValue()}/{@link BaseNpc#getSkinSignature()} (a raw Mojang texture
 * blob) aren't editable here, only {@link BaseNpc#getSkinId()} (a {@code SkinRegistry} reference) -
 * editing an existing NPC that has raw texture data set is flagged with a warning, and saving over
 * it drops that raw data in favor of whatever {@code skinId} is set (or none).
 */
public class NpcCreatorGui {

    private @Nullable String id;
    private EntityType entityType = EntityType.MANNEQUIN;
    private String name = "";
    private @Nullable String skinId;
    private String description = "";
    private final List<ActionDraft> actions;
    private boolean droppedSkinData;

    public NpcCreatorGui() {
        this(null, new ArrayList<>());
    }

    private NpcCreatorGui(@Nullable String id, List<ActionDraft> actions) {
        this.id = id;
        this.actions = actions;
    }

    public static NpcCreatorGui editExisting(String id, BaseNpc npc) {
        NpcCreatorGui gui = new NpcCreatorGui(id, new ArrayList<>());
        gui.entityType = npc.getEntityType();
        gui.name = npc.getName() == null ? "" : npc.getName();
        gui.skinId = npc.getSkinId();
        gui.description = npc.getDescription() == null ? "" : npc.getDescription();
        gui.droppedSkinData = npc.getSkinValue() != null || npc.getSkinSignature() != null;

        for (NpcAction action : npc.getActions()) {
            ActionDraft draft = new ActionDraft();
            if (action instanceof RunCommandInteractAction runCommand) {
                draft.kind = ActionKind.RUN_COMMAND;
                draft.command = runCommand.getCommand();
            } else if (action instanceof OpenGuiInteractAction openGui) {
                draft.kind = ActionKind.OPEN_GUI;
                draft.guiId = openGui.getId();
            } else {
                continue; // unknown/future action type - not editable here
            }
            gui.actions.add(draft);
        }

        return gui;
    }

    // ==========================================
    // MAIN DIALOG
    // ==========================================

    public void openMainDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Trình Tạo NPC"))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("ID: " + (id == null ? "(chưa đặt)" : id)))
                .addTextBody(Component.text("Entity Type: " + entityType.name()))
                .addTextBody(Component.text("Tên: " + (name.isBlank() ? "(chưa đặt)" : name)));

        if (droppedSkinData) {
            builder.addTextBody(Component.text("Cảnh báo: NPC này có skin dạng texture thô (value/signature) không chỉnh sửa được ở đây - lưu lại sẽ chuyển sang chỉ dùng skinId (hoặc xoá skin nếu chưa đặt).", NamedTextColor.YELLOW));
        }

        DialogTypeBuilder.MultiAction multi = builder.multiAction();

        multi.addButton(Component.text("id", id == null ? NamedTextColor.RED : NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildIdDialog(player))));
        multi.addButton(Component.text("entity_type", NamedTextColor.AQUA), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildEntityTypeDialog(player))));
        multi.addButton(Component.text("name", name.isBlank() ? NamedTextColor.RED : NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildNameDialog(player))));
        multi.addButton(Component.text("skin", skinId == null ? NamedTextColor.GRAY : NamedTextColor.GREEN), Component.text("Chỉ áp dụng khi entity_type = MANNEQUIN.", NamedTextColor.GRAY),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildSkinDialog(player))));
        multi.addButton(Component.text("description", NamedTextColor.AQUA), Component.text("Chỉ áp dụng khi entity_type = MANNEQUIN.", NamedTextColor.GRAY),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildDescriptionDialog(player))));

        for (int i = 0; i < actions.size(); i++) {
            int index = i;
            multi.addButton(Component.text((index + 1) + ". " + describeAction(actions.get(index))), null,
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildActionDialog(player, index))));
        }
        multi.addButton(Component.text("+ Thêm action", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildActionTypeDialog(player))));

        multi.addButton(Component.text("save", NamedTextColor.GOLD), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildSaveDialog(player))));
        multi.addButton(Component.text("delete", NamedTextColor.RED), null,
                (response, audience) -> audience.showDialog(buildDeleteDialog()));

        multi.columns(3);
        multi.exitButton(Component.text("Đóng"), (response, audience) -> audience.closeDialog());

        player.showDialog(multi.build());
    }

    private Dialog buildDeleteDialog() {
        return DialogBuilder.create(Component.text("Xác nhận xóa?"))
                .addTextBody(Component.text("Xác nhận xóa? Hành động này không thể hoàn tác."))
                .confirmation()
                .yesButton(Component.text("Vẫn xóa"), null, (response, audience) -> {
                    Registries.NPC.removeAndDeleteFiles(RogueSmpCore.getInstance(), id);
                    audience.sendMessage(Component.text("Deleted NPC: " + id, NamedTextColor.RED));
                })
                .noButton(Component.text("Thôi, không xóa nữa"), null, (response, audience) -> openMainDialog((Player) audience))
                .build();
    }

    // ==========================================
    // ID / ENTITY TYPE / NAME / SKIN / DESCRIPTION
    // ==========================================

    private Dialog buildIdDialog(Player player) {
        return DialogBuilder.create(Component.text("Đặt ID NPC"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("id", id == null ? NamedTextColor.RED : NamedTextColor.GREEN))
                .addTextInput("value", Component.text("ID, vd: village_blacksmith"), b -> b.initial(id == null ? "" : id).maxLength(128))
                .confirmation()
                .yesButton(Component.text("Xác nhận"), null, (response, audience) -> {
                    String sanitized = sanitizeId(response.getText("value"));
                    if (sanitized.isEmpty()) player.sendMessage(Utils.text("ID không hợp lệ.", NamedTextColor.RED));
                    else this.id = sanitized;
                    Utils.runLater(() -> openMainDialog(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> openMainDialog(player)))
                .build();
    }

    private Dialog buildEntityTypeDialog(Player player) {
        return DialogBuilder.create(Component.text("Entity Type"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("entity_type", NamedTextColor.AQUA))
                .addTextInput("value", Component.text("Tên entity type (vd: MANNEQUIN, VILLAGER, ZOMBIE)"), b -> b.initial(entityType.name()).maxLength(64))
                .confirmation()
                .yesButton(Component.text("Xác nhận"), null, (response, audience) -> {
                    String raw = response.getText("value");
                    EntityType matched = matchEntityType(raw);
                    if (matched == null) player.sendMessage(Utils.text("Không tìm thấy entity type '" + raw + "'.", NamedTextColor.RED));
                    else this.entityType = matched;
                    Utils.runLater(() -> openMainDialog(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> openMainDialog(player)))
                .build();
    }

    private static @Nullable EntityType matchEntityType(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return EntityType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Dialog buildNameDialog(Player player) {
        return DialogBuilder.create(Component.text("Tên NPC"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("name", name.isBlank() ? NamedTextColor.RED : NamedTextColor.GREEN))
                .addTextInput("value", Component.text("Tên hiển thị"), b -> b.initial(name).maxLength(128))
                .confirmation()
                .yesButton(Component.text("Xác nhận"), null, (response, audience) -> {
                    String text = response.getText("value");
                    this.name = text == null ? "" : text.trim();
                    Utils.runLater(() -> openMainDialog(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> openMainDialog(player)))
                .build();
    }

    private Dialog buildSkinDialog(Player player) {
        return DialogBuilder.create(Component.text("Skin (Mannequin)"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("skin", skinId == null ? NamedTextColor.GRAY : NamedTextColor.GREEN))
                .addTextInput("value", Component.text("Skin ID (trong SkinRegistry, để trống để bỏ)"), b -> b.initial(skinId == null ? "" : skinId).maxLength(64))
                .confirmation()
                .yesButton(Component.text("Xác nhận"), null, (response, audience) -> {
                    String text = response.getText("value");
                    this.skinId = (text == null || text.isBlank()) ? null : text.trim();
                    this.droppedSkinData = false; // explicit save of the editable field supersedes whatever raw data was there
                    Utils.runLater(() -> openMainDialog(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> openMainDialog(player)))
                .build();
    }

    private Dialog buildDescriptionDialog(Player player) {
        return DialogBuilder.create(Component.text("Mô tả (Mannequin)"))
                .canCloseWithEscape(false)
                .addTextInput("value", Component.text("Mô tả hiển thị"), b -> b.initial(description).maxLength(256))
                .confirmation()
                .yesButton(Component.text("Xác nhận"), null, (response, audience) -> {
                    String text = response.getText("value");
                    this.description = text == null ? "" : text.trim();
                    Utils.runLater(() -> openMainDialog(player));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> openMainDialog(player)))
                .build();
    }

    private static String sanitizeId(@Nullable String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    // ==========================================
    // ACTIONS
    // ==========================================

    private Dialog buildActionTypeDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Chọn loại action")).canCloseWithEscape(false);

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        for (ActionKind kind : ActionKind.values()) {
            multi.addButton(Component.text(kind.label), null, (response, audience) -> {
                ActionDraft draft = new ActionDraft();
                draft.kind = kind;
                actions.add(draft);
                int newIndex = actions.size() - 1;
                Utils.runLater(() -> player.showDialog(buildActionDialog(player, newIndex)));
            });
        }
        multi.columns(2);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> openMainDialog(player)));

        return multi.build();
    }

    private Dialog buildActionDialog(Player player, int index) {
        ActionDraft draft = actions.get(index);

        DialogBuilder builder = DialogBuilder.create(Component.text("Action: " + draft.kind.label))
                .canCloseWithEscape(false);

        switch (draft.kind) {
            case RUN_COMMAND -> builder.addTextInput("command", Component.text("command (không cần dấu /, hỗ trợ @p, @p_id)"), b -> b.initial(draft.command).maxLength(256));
            case OPEN_GUI -> builder.addTextInput("gui", Component.text("gui id"), b -> b.initial(draft.guiId).maxLength(128));
        }

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            switch (draft.kind) {
                case RUN_COMMAND -> {
                    String command = response.getText("command");
                    if (command != null) draft.command = command.trim();
                }
                case OPEN_GUI -> {
                    String gui = response.getText("gui");
                    if (gui != null) draft.guiId = gui.trim();
                }
            }
            Utils.runLater(() -> openMainDialog(player));
        });
        multi.addButton(Component.text("Xoá", NamedTextColor.RED), null, (response, audience) -> {
            actions.remove(index);
            Utils.runLater(() -> openMainDialog(player));
        });
        multi.columns(2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> openMainDialog(player)));

        return multi.build();
    }

    private static String describeAction(ActionDraft draft) {
        return switch (draft.kind) {
            case RUN_COMMAND -> "run_command(" + draft.command + ")";
            case OPEN_GUI -> "open_gui(" + draft.guiId + ")";
        };
    }

    // ==========================================
    // SAVE
    // ==========================================

    private Dialog buildSaveDialog(Player player) {
        return DialogBuilder.create(Component.text("Lưu NPC"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("save", NamedTextColor.GOLD))
                .addTextBody(Component.text("ID: " + (id == null ? "(chưa đặt)" : id)))
                .addTextBody(Component.text("Entity Type: " + entityType.name()))
                .addTextBody(Component.text("Tên: " + (name.isBlank() ? "(chưa đặt)" : name)))
                .confirmation()
                .yesButton(Component.text("Lưu"), null, (response, audience) -> Utils.runLater(() -> attemptSave(player)))
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> openMainDialog(player)))
                .build();
    }

    private void attemptSave(Player player) {
        if (id == null || id.isBlank()) {
            player.sendMessage(Utils.text("Bạn cần đặt ID trước khi lưu.", NamedTextColor.RED));
            openMainDialog(player);
            return;
        }
        if (name.isBlank()) {
            player.sendMessage(Utils.text("Bạn cần đặt tên trước khi lưu.", NamedTextColor.RED));
            openMainDialog(player);
            return;
        }

        if (Registries.NPC.get(id) != null) {
            Dialog dialog = DialogBuilder.create(Component.text("Ghi đè NPC?"))
                    .canCloseWithEscape(false)
                    .addTextBody(Component.text("Đã tồn tại một NPC với ID '" + id + "'. Ghi đè?"))
                    .confirmation()
                    .yesButton(Component.text("Ghi đè"), null, (response, audience) -> doSave(player))
                    .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> openMainDialog(player)))
                    .build();

            player.showDialog(dialog);
            return;
        }

        doSave(player);
    }

    private void doSave(Player player) {
        List<NpcAction> realActions = new ArrayList<>();
        for (ActionDraft draft : actions) {
            NpcAction action = switch (draft.kind) {
                case RUN_COMMAND -> draft.command.isBlank() ? null : new RunCommandInteractAction(draft.command);
                case OPEN_GUI -> draft.guiId.isBlank() ? null : new OpenGuiInteractAction(draft.guiId);
            };
            if (action != null) realActions.add(action);
        }

        BaseNpc npc = new BaseNpc(entityType, name, null, null, skinId, description, id, realActions);
        Registries.NPC.registerAndSave(RogueSmpCore.getInstance(), id, npc);

        droppedSkinData = false;
        player.sendMessage(Utils.text("Đã lưu NPC '" + id + "'.", NamedTextColor.GREEN));
        Utils.runLater(() -> openMainDialog(player));
    }

    // ==========================================
    // DRAFT STATE
    // ==========================================

    private static class ActionDraft {
        ActionKind kind = ActionKind.RUN_COMMAND;
        String command = "";
        String guiId = "";
    }

    private enum ActionKind {
        RUN_COMMAND("run_command"), OPEN_GUI("open_gui");

        final String label;

        ActionKind(String label) {
            this.label = label;
        }
    }

    // ==========================================
    // COMMAND
    // ==========================================

    public static void registerCommand() {
        new CommandAPICommand("smpnpccreator")
                .withSubcommand(new CommandAPICommand("new")
                        .executesPlayer((player, args) -> {
                            new NpcCreatorGui().openMainDialog(player);
                        }))
                .withSubcommand(new CommandAPICommand("edit")
                        .withArguments(new StringArgument("npc_id").replaceSuggestions(ArgumentSuggestions.strings(Registries.NPC.getAll().keySet())))
                        .executesPlayer((player, args) -> {
                            String npcId = (String) args.get("npc_id");
                            BaseNpc existing = Registries.NPC.get(npcId);
                            if (existing == null) {
                                player.sendMessage(Utils.text("Không tìm thấy NPC với ID '" + npcId + "'.", NamedTextColor.RED));
                                return;
                            }
                            NpcCreatorGui.editExisting(npcId, existing).openMainDialog(player);
                        }))
                .register();
    }
}
