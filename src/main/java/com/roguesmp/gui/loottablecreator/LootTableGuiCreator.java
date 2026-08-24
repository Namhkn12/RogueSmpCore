package com.roguesmp.gui.loottablecreator;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.loot.LootEntry;
import com.roguesmp.loot.LootPool;
import com.roguesmp.loot.LootTable;
import com.roguesmp.loot.condition.LootCondition;
import com.roguesmp.loot.condition.impl.AndCondition;
import com.roguesmp.loot.condition.impl.ChanceCondition;
import com.roguesmp.loot.condition.impl.NotCondition;
import com.roguesmp.loot.condition.impl.OrCondition;
import com.roguesmp.loot.condition.impl.OriginCondition;
import com.roguesmp.loot.context.LootOrigin;
import com.roguesmp.loot.entry.EmptyEntry;
import com.roguesmp.loot.entry.ItemEntry;
import com.roguesmp.loot.entry.NestedTableEntry;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * In-game editor for {@link LootTable}s, built on the same Paper Dialog API pattern as
 * {@code gui.entitycreator.EntityCreatorGui} - a main {@code multiAction()} menu (id + one button
 * per pool + add-pool + save), where each pool hands off to its own hub dialog (rolls + one
 * button per entry + add-entry + conditions), mirroring {@code EntitySpellListEditorGui}'s
 * hub/list/add/entry shape. Entry-level conditions get the same treatment inside each entry's
 * own dialog. Condition editing itself ({@link #buildConditionListDialog}) reuses that same
 * shape recursively - {@code and}/{@code or} hand off to another condition list for their
 * children, {@code not} does too but only its first child is used (Paper's Dialog API has no
 * clean single-child list UI, so this keeps the editor to one recursive shape instead of two).
 * <p>
 * Scope: pool/entry {@code conditions} are editable; entry {@code functions} are not (a JSON-only
 * extension point, see {@code LootTableGuide}) - editing an existing table that uses any is
 * flagged with a warning, and saving over it silently drops just the functions.
 */
public class LootTableGuiCreator {

    private @Nullable String id;
    private final List<PoolDraft> pools;
    private boolean droppedUneditableData;

    public LootTableGuiCreator() {
        this(null, new ArrayList<>());
    }

    private LootTableGuiCreator(@Nullable String id, List<PoolDraft> pools) {
        this.id = id;
        this.pools = pools;
    }

    public static LootTableGuiCreator editExisting(String id, LootTable table) {
        List<PoolDraft> poolDrafts = new ArrayList<>();
        boolean droppedAny = false;

        for (LootPool pool : table.getPools()) {
            PoolDraft poolDraft = new PoolDraft();
            poolDraft.rolls = pool.getRolls();
            poolDraft.conditions = fromRealConditions(pool.getConditions());
            if (poolDraft.conditions.size() < pool.getConditions().size()) droppedAny = true;

            for (LootEntry entry : pool.getEntries()) {
                EntryDraft entryDraft = new EntryDraft();
                entryDraft.weight = entry.getWeight();
                entryDraft.conditions = fromRealConditions(entry.getConditions());
                if (entryDraft.conditions.size() < entry.getConditions().size()) droppedAny = true;
                if (!entry.getFunctions().isEmpty()) droppedAny = true;

                if (entry instanceof ItemEntry item) {
                    entryDraft.kind = EntryKind.ITEM;
                    entryDraft.itemId = item.getItemId();
                    entryDraft.minAmount = item.getMinAmount();
                    entryDraft.maxAmount = item.getMaxAmount();
                } else if (entry instanceof NestedTableEntry nested) {
                    entryDraft.kind = EntryKind.LOOT_TABLE;
                    entryDraft.nestedTableId = nested.getNestedTableId();
                } else {
                    entryDraft.kind = EntryKind.EMPTY;
                }

                poolDraft.entries.add(entryDraft);
            }

            poolDrafts.add(poolDraft);
        }

        LootTableGuiCreator gui = new LootTableGuiCreator(id, poolDrafts);
        gui.droppedUneditableData = droppedAny;
        return gui;
    }

    // ==========================================
    // MAIN DIALOG
    // ==========================================

    public void openMainDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Trình Tạo Loot Table"))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("ID: " + (id == null ? "(chưa đặt)" : id)))
                .addTextBody(Component.text(pools.size() + " pool"));

        if (droppedUneditableData) {
            builder.addTextBody(Component.text("Cảnh báo: table này có functions không chỉnh sửa được ở đây - lưu lại sẽ xoá chúng.", NamedTextColor.YELLOW));
        }

        DialogTypeBuilder.MultiAction multi = builder.multiAction();

        multi.addButton(Component.text("id", id == null ? NamedTextColor.RED : NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildIdDialog(player))));

        for (int i = 0; i < pools.size(); i++) {
            int index = i;
            multi.addButton(Component.text("pool[" + index + "]", NamedTextColor.AQUA), describePoolTooltip(pools.get(index)),
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildPoolHubDialog(player, index))));
        }

        multi.addButton(Component.text("+ Thêm pool", NamedTextColor.GREEN), null, (response, audience) -> {
            pools.add(new PoolDraft());
            Utils.runLater(() -> player.showDialog(buildPoolHubDialog(player, pools.size() - 1)));
        });

        multi.addButton(Component.text("save", NamedTextColor.GOLD), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildSaveDialog(player))));

        multi.columns(3);
        multi.exitButton(Component.text("Đóng"), (response, audience) -> audience.closeDialog());

        player.showDialog(multi.build());
    }

    private static Component describePoolTooltip(PoolDraft pool) {
        return Component.text("rolls=" + pool.rolls + ", " + pool.entries.size() + " entrie(s), "
                + pool.conditions.size() + " condition(s)", NamedTextColor.GRAY);
    }

    // ==========================================
    // ID
    // ==========================================

    private Dialog buildIdDialog(Player player) {
        return DialogBuilder.create(Component.text("Đặt ID loot table"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("id", id == null ? NamedTextColor.RED : NamedTextColor.GREEN))
                .addTextInput("value", Component.text("ID = đường dẫn tương đối, vd dungeons/goblin_drops"), b -> b.initial(id == null ? "" : id).maxLength(128))
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

    private static String sanitizeId(@Nullable String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    // ==========================================
    // SAVE
    // ==========================================

    private Dialog buildSaveDialog(Player player) {
        return DialogBuilder.create(Component.text("Lưu loot table"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("save", NamedTextColor.GOLD))
                .addTextBody(Component.text("ID: " + (id == null ? "(chưa đặt)" : id)))
                .addTextBody(Component.text(pools.size() + " pool" + (pools.isEmpty() ? " (bảng sẽ không roll ra gì cả)" : "")))
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

        if (Registries.LOOT_TABLE.get(id) != null) {
            Dialog dialog = DialogBuilder.create(Component.text("Ghi đè loot table?"))
                    .canCloseWithEscape(false)
                    .addTextBody(Component.text("Đã tồn tại một loot table với ID '" + id + "'. Ghi đè?"))
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
        List<LootPool> realPools = new ArrayList<>();
        for (PoolDraft poolDraft : pools) {
            realPools.add(new LootPool(poolDraft.rolls, toRealEntries(poolDraft.entries), toRealConditions(poolDraft.conditions)));
        }

        LootTable table = new LootTable(realPools);
        Registries.LOOT_TABLE.registerAndSave(RogueSmpCore.getInstance(), id, table);

        droppedUneditableData = false;
        player.sendMessage(Utils.text("Đã lưu loot table '" + id + "'.", NamedTextColor.GREEN));
        Utils.runLater(() -> openMainDialog(player));
    }

    private static List<LootEntry> toRealEntries(List<EntryDraft> drafts) {
        List<LootEntry> entries = new ArrayList<>();
        for (EntryDraft draft : drafts) {
            LootEntry.BaseProperties base = new LootEntry.BaseProperties(draft.weight, toRealConditions(draft.conditions), List.of());
            entries.add(switch (draft.kind) {
                case ITEM -> new ItemEntry(base, draft.itemId, draft.minAmount, draft.maxAmount);
                case LOOT_TABLE -> new NestedTableEntry(base, draft.nestedTableId);
                case EMPTY -> new EmptyEntry(base);
            });
        }
        return entries;
    }

    // ==========================================
    // POOL HUB
    // ==========================================

    private Dialog buildPoolHubDialog(Player player, int poolIndex) {
        PoolDraft pool = pools.get(poolIndex);

        DialogBuilder builder = DialogBuilder.create(Component.text("Pool[" + poolIndex + "]"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("pool[" + poolIndex + "]", NamedTextColor.AQUA))
                .addTextBody(Component.text("rolls=" + pool.rolls + ", " + pool.entries.size() + " entrie(s)"));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("rolls", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildRollsDialog(player, poolIndex))));
        multi.addButton(Component.text("conditions (" + pool.conditions.size() + ")", pool.conditions.isEmpty() ? NamedTextColor.GRAY : NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(
                        buildConditionListDialog(player, pool.conditions, () -> player.showDialog(buildPoolHubDialog(player, poolIndex))))));

        for (int i = 0; i < pool.entries.size(); i++) {
            int entryIndex = i;
            EntryDraft entry = pool.entries.get(i);
            multi.addButton(Component.text((entryIndex + 1) + ". " + describeEntryLabel(entry)), null,
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildEntryDialog(player, poolIndex, entryIndex))));
        }

        multi.addButton(Component.text("+ Thêm entry", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildEntryTypeDialog(player, poolIndex))));
        multi.addButton(Component.text("Xoá pool này", NamedTextColor.RED), null, (response, audience) -> {
            pools.remove(poolIndex);
            Utils.runLater(() -> openMainDialog(player));
        });

        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> openMainDialog(player)));

        return multi.build();
    }

    private Dialog buildRollsDialog(Player player, int poolIndex) {
        PoolDraft pool = pools.get(poolIndex);

        return DialogBuilder.create(Component.text("Số lần quay"))
                .canCloseWithEscape(false)
                .addTextInput("value", Component.text("rolls (số lần quay pool này, tối thiểu 1)"), b -> b.initial(String.valueOf(pool.rolls)).maxLength(8))
                .confirmation()
                .yesButton(Component.text("Xác nhận"), null, (response, audience) -> {
                    Integer parsed = parseInt(response.getText("value"), 1, 100);
                    if (parsed != null) pool.rolls = parsed;
                    Utils.runLater(() -> player.showDialog(buildPoolHubDialog(player, poolIndex)));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> player.showDialog(buildPoolHubDialog(player, poolIndex))))
                .build();
    }

    // ==========================================
    // ENTRY
    // ==========================================

    private Dialog buildEntryTypeDialog(Player player, int poolIndex) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Chọn loại entry"))
                .canCloseWithEscape(false);

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        for (EntryKind kind : EntryKind.values()) {
            multi.addButton(Component.text(kind.label), null, (response, audience) -> {
                EntryDraft draft = new EntryDraft();
                draft.kind = kind;
                pools.get(poolIndex).entries.add(draft);
                int newIndex = pools.get(poolIndex).entries.size() - 1;
                Utils.runLater(() -> player.showDialog(buildEntryDialog(player, poolIndex, newIndex)));
            });
        }
        multi.columns(3);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildPoolHubDialog(player, poolIndex))));

        return multi.build();
    }

    private Dialog buildEntryDialog(Player player, int poolIndex, int entryIndex) {
        EntryDraft entry = pools.get(poolIndex).entries.get(entryIndex);

        DialogBuilder builder = DialogBuilder.create(Component.text("Entry: " + entry.kind.label))
                .canCloseWithEscape(false)
                .addTextInput("weight", Component.text("weight"), b -> b.initial(String.valueOf(entry.weight)).maxLength(8));

        switch (entry.kind) {
            case ITEM -> builder
                    .addTextInput("item_id", Component.text("item_id (\"minecraft:...\" = vanilla)"), b -> b.initial(entry.itemId).maxLength(128))
                    .addTextInput("min_amount", Component.text("min_amount"), b -> b.initial(String.valueOf(entry.minAmount)).maxLength(8))
                    .addTextInput("max_amount", Component.text("max_amount"), b -> b.initial(String.valueOf(entry.maxAmount)).maxLength(8));
            case LOOT_TABLE -> builder.addTextInput("nested_id", Component.text("id bảng lồng"), b -> b.initial(entry.nestedTableId).maxLength(128));
            case EMPTY -> {
                // no extra fields
            }
        }

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
            Integer weight = parseInt(response.getText("weight"), 0, 100000);
            if (weight != null) entry.weight = weight;

            switch (entry.kind) {
                case ITEM -> {
                    String itemId = response.getText("item_id");
                    if (itemId != null && !itemId.isBlank()) entry.itemId = itemId.trim();
                    Integer min = parseInt(response.getText("min_amount"), 1, 6400);
                    Integer max = parseInt(response.getText("max_amount"), 1, 6400);
                    if (min != null) entry.minAmount = min;
                    if (max != null) entry.maxAmount = max;
                }
                case LOOT_TABLE -> {
                    String nestedId = response.getText("nested_id");
                    if (nestedId != null && !nestedId.isBlank()) entry.nestedTableId = nestedId.trim();
                }
                case EMPTY -> {
                }
            }
            Utils.runLater(() -> player.showDialog(buildPoolHubDialog(player, poolIndex)));
        });
        multi.addButton(Component.text("conditions (" + entry.conditions.size() + ")", entry.conditions.isEmpty() ? NamedTextColor.GRAY : NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(
                        buildConditionListDialog(player, entry.conditions, () -> player.showDialog(buildEntryDialog(player, poolIndex, entryIndex))))));
        multi.addButton(Component.text("Xoá", NamedTextColor.RED), null, (response, audience) -> {
            pools.get(poolIndex).entries.remove(entryIndex);
            Utils.runLater(() -> player.showDialog(buildPoolHubDialog(player, poolIndex)));
        });
        multi.columns(3);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildPoolHubDialog(player, poolIndex))));

        return multi.build();
    }

    private static String describeEntryLabel(EntryDraft entry) {
        String base = switch (entry.kind) {
            case ITEM -> "item(" + entry.itemId + ", w=" + entry.weight + ")";
            case LOOT_TABLE -> "loot_table(" + entry.nestedTableId + ", w=" + entry.weight + ")";
            case EMPTY -> "empty(w=" + entry.weight + ")";
        };
        return entry.conditions.isEmpty() ? base : base + " [" + entry.conditions.size() + " cond]";
    }

    // ==========================================
    // CONDITIONS (recursive - and/or/not hand back into this same hub)
    // ==========================================

    private Dialog buildConditionListDialog(Player player, List<ConditionDraft> conditions, Runnable onBack) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Conditions"))
                .canCloseWithEscape(false)
                .addTextBody(Component.text(conditions.size() + " condition"));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("+ Thêm condition", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildConditionTypeDialog(player, conditions, onBack))));

        for (int i = 0; i < conditions.size(); i++) {
            int index = i;
            multi.addButton(Component.text((index + 1) + ". " + describeConditionLabel(conditions.get(index))), null,
                    (response, audience) -> Utils.runLater(() -> player.showDialog(buildConditionEntryDialog(player, conditions, index, onBack))));
        }

        multi.columns(2);
        multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(onBack));

        return multi.build();
    }

    private Dialog buildConditionTypeDialog(Player player, List<ConditionDraft> conditions, Runnable onBack) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Chọn loại condition"))
                .canCloseWithEscape(false);

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        for (ConditionKind kind : ConditionKind.values()) {
            multi.addButton(Component.text(kind.label), null, (response, audience) -> {
                ConditionDraft draft = new ConditionDraft();
                draft.kind = kind;
                conditions.add(draft);
                int newIndex = conditions.size() - 1;
                Utils.runLater(() -> player.showDialog(buildConditionEntryDialog(player, conditions, newIndex, onBack)));
            });
        }
        multi.columns(3);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> player.showDialog(buildConditionListDialog(player, conditions, onBack))));

        return multi.build();
    }

    private Dialog buildConditionEntryDialog(Player player, List<ConditionDraft> conditions, int index, Runnable onBack) {
        ConditionDraft draft = conditions.get(index);
        Runnable backToList = () -> player.showDialog(buildConditionListDialog(player, conditions, onBack));

        switch (draft.kind) {
            case CHANCE -> {
                DialogBuilder builder = DialogBuilder.create(Component.text("Condition: chance"))
                        .canCloseWithEscape(false)
                        .addTextInput("chance", Component.text("chance (0.0 - 1.0)"), b -> b.initial(String.valueOf(draft.chance)).maxLength(8));

                DialogTypeBuilder.MultiAction multi = builder.multiAction();
                multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
                    Double parsed = parseDouble(response.getText("chance"), 0.0, 1.0);
                    if (parsed != null) draft.chance = parsed;
                    Utils.runLater(backToList);
                });
                multi.addButton(Component.text("Xoá", NamedTextColor.RED), null, (response, audience) -> {
                    conditions.remove(index);
                    Utils.runLater(backToList);
                });
                multi.columns(3);
                multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(backToList));
                return multi.build();
            }
            case ORIGIN -> {
                DialogBuilder builder = DialogBuilder.create(Component.text("Condition: origin"))
                        .canCloseWithEscape(false);
                for (LootOrigin origin : LootOrigin.values()) {
                    builder.addCheckboxInput(origin.name(), Component.text(origin.name()), draft.origins.contains(origin), "true", "false");
                }

                DialogTypeBuilder.MultiAction multi = builder.multiAction();
                multi.addButton(Component.text("Lưu"), null, (response, audience) -> {
                    Set<LootOrigin> selected = EnumSet.noneOf(LootOrigin.class);
                    for (LootOrigin origin : LootOrigin.values()) {
                        Boolean checked = response.getBoolean(origin.name());
                        if (checked != null && checked) selected.add(origin);
                    }
                    draft.origins = selected;
                    Utils.runLater(backToList);
                });
                multi.addButton(Component.text("Xoá", NamedTextColor.RED), null, (response, audience) -> {
                    conditions.remove(index);
                    Utils.runLater(backToList);
                });
                multi.columns(3);
                multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(backToList));
                return multi.build();
            }
            case AND, OR -> {
                DialogBuilder builder = DialogBuilder.create(Component.text("Condition: " + draft.kind.label))
                        .canCloseWithEscape(false)
                        .addTextBody(Component.text(draft.children.size() + " condition con"));

                DialogTypeBuilder.MultiAction multi = builder.multiAction();
                multi.addButton(Component.text("Sửa danh sách con"), null,
                        (response, audience) -> Utils.runLater(() -> player.showDialog(buildConditionListDialog(player, draft.children, backToList))));
                multi.addButton(Component.text("Xoá", NamedTextColor.RED), null, (response, audience) -> {
                    conditions.remove(index);
                    Utils.runLater(backToList);
                });
                multi.columns(2);
                multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(backToList));
                return multi.build();
            }
            case NOT -> {
                DialogBuilder builder = DialogBuilder.create(Component.text("Condition: not"))
                        .canCloseWithEscape(false)
                        .addTextBody(Component.text(draft.children.isEmpty() ? "Chưa có condition con" : "Đảo ngược: " + describeConditionLabel(draft.children.get(0))))
                        .addTextBody(Component.text("Chỉ condition đầu tiên trong danh sách con được dùng.", NamedTextColor.GRAY));

                DialogTypeBuilder.MultiAction multi = builder.multiAction();
                multi.addButton(Component.text("Sửa condition con"), null,
                        (response, audience) -> Utils.runLater(() -> player.showDialog(buildConditionListDialog(player, draft.children, backToList))));
                multi.addButton(Component.text("Xoá", NamedTextColor.RED), null, (response, audience) -> {
                    conditions.remove(index);
                    Utils.runLater(backToList);
                });
                multi.columns(2);
                multi.exitButton(Component.text("« Quay lại"), (response, audience) -> Utils.runLater(backToList));
                return multi.build();
            }
        }

        throw new IllegalStateException("Unknown condition kind: " + draft.kind);
    }

    private static String describeConditionLabel(ConditionDraft draft) {
        return switch (draft.kind) {
            case CHANCE -> "chance(" + draft.chance + ")";
            case ORIGIN -> "origin(" + draft.origins + ")";
            case AND -> "and(" + draft.children.size() + ")";
            case OR -> "or(" + draft.children.size() + ")";
            case NOT -> "not(" + (draft.children.isEmpty() ? "?" : describeConditionLabel(draft.children.get(0))) + ")";
        };
    }

    private static List<LootCondition> toRealConditions(List<ConditionDraft> drafts) {
        List<LootCondition> result = new ArrayList<>();
        for (ConditionDraft draft : drafts) {
            LootCondition condition = toRealCondition(draft);
            if (condition != null) result.add(condition);
        }
        return result;
    }

    private static @Nullable LootCondition toRealCondition(ConditionDraft draft) {
        return switch (draft.kind) {
            case CHANCE -> new ChanceCondition(draft.chance);
            case ORIGIN -> draft.origins.isEmpty() ? null : new OriginCondition(EnumSet.copyOf(draft.origins));
            case AND -> new AndCondition(toRealConditions(draft.children));
            case OR -> new OrCondition(toRealConditions(draft.children));
            case NOT -> {
                List<LootCondition> children = toRealConditions(draft.children);
                yield children.isEmpty() ? null : new NotCondition(children.get(0));
            }
        };
    }

    private static List<ConditionDraft> fromRealConditions(List<LootCondition> conditions) {
        List<ConditionDraft> drafts = new ArrayList<>();
        for (LootCondition condition : conditions) {
            ConditionDraft draft = fromRealCondition(condition);
            if (draft != null) drafts.add(draft);
        }
        return drafts;
    }

    private static @Nullable ConditionDraft fromRealCondition(LootCondition condition) {
        ConditionDraft draft = new ConditionDraft();
        if (condition instanceof ChanceCondition chance) {
            draft.kind = ConditionKind.CHANCE;
            draft.chance = chance.getChance();
        } else if (condition instanceof OriginCondition origin) {
            draft.kind = ConditionKind.ORIGIN;
            draft.origins = EnumSet.copyOf(origin.getOrigins());
        } else if (condition instanceof AndCondition and) {
            draft.kind = ConditionKind.AND;
            draft.children.addAll(fromRealConditions(and.getConditions()));
        } else if (condition instanceof OrCondition or) {
            draft.kind = ConditionKind.OR;
            draft.children.addAll(fromRealConditions(or.getConditions()));
        } else if (condition instanceof NotCondition not) {
            draft.kind = ConditionKind.NOT;
            ConditionDraft child = fromRealCondition(not.getCondition());
            if (child != null) draft.children.add(child);
        } else {
            return null; // unknown/future condition type - not editable here
        }
        return draft;
    }

    // ==========================================
    // SHARED PARSING
    // ==========================================

    private static @Nullable Integer parseInt(@Nullable String text, int min, int max) {
        if (text == null || text.isBlank()) return null;
        try {
            int value = Integer.parseInt(text.trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static @Nullable Double parseDouble(@Nullable String text, double min, double max) {
        if (text == null || text.isBlank()) return null;
        try {
            double value = Double.parseDouble(text.trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==========================================
    // DRAFT STATE
    // ==========================================

    private static class PoolDraft {
        int rolls = 1;
        List<EntryDraft> entries = new ArrayList<>();
        List<ConditionDraft> conditions = new ArrayList<>();
    }

    private static class EntryDraft {
        EntryKind kind = EntryKind.ITEM;
        int weight = 1;
        String itemId = "";
        int minAmount = 1;
        int maxAmount = 1;
        String nestedTableId = "";
        List<ConditionDraft> conditions = new ArrayList<>();
    }

    private static class ConditionDraft {
        ConditionKind kind = ConditionKind.CHANCE;
        double chance = 0.5;
        Set<LootOrigin> origins = EnumSet.noneOf(LootOrigin.class);
        List<ConditionDraft> children = new ArrayList<>(); // AND/OR: all children. NOT: first child only.
    }

    private enum EntryKind {
        ITEM("item"), LOOT_TABLE("loot_table"), EMPTY("empty");

        final String label;

        EntryKind(String label) {
            this.label = label;
        }
    }

    private enum ConditionKind {
        CHANCE("chance"), ORIGIN("origin"), AND("and"), OR("or"), NOT("not");

        final String label;

        ConditionKind(String label) {
            this.label = label;
        }
    }

    // ==========================================
    // COMMAND
    // ==========================================

    public static void registerCommand() {
        new CommandAPICommand("smplootcreator")
                .withSubcommand(new CommandAPICommand("new")
                        .executesPlayer((player, args) -> {
                            new LootTableGuiCreator().openMainDialog(player);
                        }))
                .withSubcommand(new CommandAPICommand("edit")
                        .withArguments(new StringArgument("table_id"))
                        .executesPlayer((player, args) -> {
                            String tableId = (String) args.get("table_id");
                            LootTable existing = Registries.LOOT_TABLE.get(tableId);
                            if (existing == null) {
                                player.sendMessage(Utils.text("Không tìm thấy loot table với ID '" + tableId + "'.", NamedTextColor.RED));
                                return;
                            }
                            LootTableGuiCreator.editExisting(tableId, existing).openMainDialog(player);
                        }))
                .register();
    }
}
