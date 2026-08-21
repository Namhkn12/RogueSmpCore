package com.roguesmp.gui.entitycreator;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.EntityAttribute;
import com.roguesmp.entity.EntityEquipment;
import com.roguesmp.entity.component.EntityComponent;
import com.roguesmp.entity.component.EntityComponentKey;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.entity.component.impl.*;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * In-game editor for {@link BaseEntity}s, built on the same Paper Dialog API pattern as
 * {@code gui.itemcreator.ItemCreatorGui} - a main {@code multiAction()} menu with one button per
 * field/component (the id doubling as its label, colored green/gray by presence), a preview icon,
 * and a save flow that writes to {@link Registries#ENTITY}. {@code attributes}, {@code equipment}
 * and {@code spells} have too many entries for a single dialog and hand off to their own
 * sub-editor ({@link EntityAttributeEditorGui}, {@link EntityEquipmentEditorGui},
 * {@link EntitySpellListEditorGui}), each of which commits straight back into this draft on every
 * leaf edit rather than requiring a separate top-level apply step.
 * <p>
 * Unlike items, {@link Registries#ENTITY} (data-driven entities) and {@link Registries#ENTITY_FACTORY}
 * (hardcoded boss ids) are separate registries with separate id spaces - saving here can never
 * clobber a boss, but {@link #attemptSave} still warns if the chosen id collides with one, since
 * that id would silently never spawn as that boss.
 */
public class EntityCreatorGui {

    private @Nullable String id;
    private EntityType entityType;
    private final Map<String, EntityComponent> components = new HashMap<>();

    public EntityCreatorGui() {
        this(null, EntityType.ZOMBIE, Map.of());
    }

    private EntityCreatorGui(@Nullable String id, EntityType entityType, Map<String, EntityComponent> components) {
        this.id = id;
        this.entityType = entityType;
        this.components.putAll(components);
    }

    public static EntityCreatorGui editExisting(BaseEntity entity) {
        Map<String, EntityComponent> copy = new HashMap<>();
        entity.getComponents().forEach((key, value) -> copy.put(key, value.copy()));
        return new EntityCreatorGui(entity.getId(), entity.getEntityType(), copy);
    }

    public void openMainDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Trình Tạo Entity"))
                .canCloseWithEscape(false)
                .addItemBody(buildPreviewItem())
                .addTextBody(Component.text("ID: " + (id == null ? "(chưa đặt)" : id)
                        + "  |  EntityType: " + entityType.name()));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();

        multi.addButton(Component.text("id", id == null ? NamedTextColor.RED : NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildIdDialog(player))));
        multi.addButton(Component.text("entity_type", NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildEntityTypeDialog(player))));

        multi.addButton(componentLabel(EntityComponentKeys.DISPLAY_NAME),
                tooltip("Đặt tên hiển thị (custom name) cho entity.", null),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildDisplayNameDialog(player))));
        multi.addButton(componentLabel(EntityComponentKeys.BEHAVIOR),
                tooltip("Cờ hành vi vanilla (AI, bất tử, persistent) và phạm vi phát hiện.", null),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildBehaviorDialog(player))));
        multi.addButton(componentLabel(EntityComponentKeys.ATTRIBUTES),
                tooltip("Đặt attribute (máu tối đa, tốc độ, sát thương, ...) cho entity.", null),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildAttributesDialog(player))));
        multi.addButton(componentLabel(EntityComponentKeys.EQUIPMENT),
                tooltip("Đặt trang bị (vũ khí, giáp) cho từng slot của entity.", null),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildEquipmentDialog(player))));
        multi.addButton(componentLabel(EntityComponentKeys.SPELLS),
                tooltip("Đặt spell active/passive cho entity.", "Màu trong danh sách chọn spell là gợi ý ACTIVE/PASSIVE/EITHER"),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildSpellsDialog(player))));
        multi.addButton(componentLabel(EntityComponentKeys.BOSS_BAR),
                tooltip("Hiện thanh máu (boss bar) cho entity.", null),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildBossBarDialog(player))));
        multi.addButton(componentLabel(EntityComponentKeys.NAMEPLATE),
                tooltip("Hiện bảng tên/máu/hiệu ứng nổi phía trên đầu entity.", "Xám = chưa ghi đè, entity vẫn có bảng tên mặc định lúc spawn"),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildNameplateDialog(player))));

        multi.addButton(Component.text("save", NamedTextColor.GOLD), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildSaveDialog(player))));

        multi.columns(3);
        multi.exitButton(Component.text("Đóng"), null);

        player.showDialog(multi.build());
    }

    private Component componentLabel(EntityComponentKey<?> key) {
        boolean present = components.containsKey(key.id());
        return Component.text(key.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY);
    }

    private static Component tooltip(String usage, @Nullable String note) {
        Component result = Utils.text(usage, NamedTextColor.GRAY);
        if (note != null) result = result.append(Component.newline()).append(Utils.text(note, NamedTextColor.DARK_GRAY));
        return result;
    }

    // ==========================================
    // PACKAGE-VISIBLE HOOKS FOR SUB-EDITORS
    // ==========================================

    void applyAttributes(AttributeComponent component) {
        components.put(EntityComponentKeys.ATTRIBUTES.id(), component);
    }

    void removeAttributes() {
        components.remove(EntityComponentKeys.ATTRIBUTES.id());
    }

    void applyEquipment(EquipmentComponent component) {
        components.put(EntityComponentKeys.EQUIPMENT.id(), component);
    }

    void removeEquipment() {
        components.remove(EntityComponentKeys.EQUIPMENT.id());
    }

    void applySpells(SpellComponent component) {
        components.put(EntityComponentKeys.SPELLS.id(), component);
    }

    void removeSpells() {
        components.remove(EntityComponentKeys.SPELLS.id());
    }

    void reopen(Player player) {
        openMainDialog(player);
    }

    // ==========================================
    // PREVIEW / SAVE
    // ==========================================

    private ItemStack buildPreviewItem() {
        Material spawnEgg = Material.matchMaterial(entityType.name() + "_SPAWN_EGG");
        return new ItemStack(spawnEgg != null ? spawnEgg : Material.ZOMBIE_HEAD);
    }

    private Dialog buildSaveDialog(Player player) {
        return DialogBuilder.create(Component.text("Lưu entity"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("save", NamedTextColor.GOLD))
                .addItemBody(buildPreviewItem())
                .addTextBody(Component.text("ID: " + (id == null ? "(chưa đặt)" : id)))
                .addTextBody(Component.text("EntityType: " + entityType.name()))
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

        if (Registries.ENTITY_FACTORY.get(id) != null) {
            player.sendMessage(Utils.text("Cảnh báo: ID '" + id + "' trùng với một boss đã đăng ký (registerSpecial) - entity này sẽ không dùng logic boss đó.", NamedTextColor.YELLOW));
        }

        if (Registries.ENTITY.get(id) != null) {
            Dialog dialog = DialogBuilder.create(Component.text("Ghi đè entity?"))
                    .canCloseWithEscape(false)
                    .addItemBody(buildPreviewItem())
                    .addTextBody(Component.text("Đã tồn tại một entity với ID '" + id + "'. Ghi đè?"))
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
        Map<String, EntityComponent> toSave = new HashMap<>();
        components.forEach((key, value) -> toSave.put(key, value.copy()));

        BaseEntity entity = new BaseEntity(id, entityType, toSave);
        Registries.ENTITY.registerAndSave(RogueSmpCore.getInstance(), id, entity);

        player.sendMessage(Utils.text("Đã lưu entity '" + id + "'.", NamedTextColor.GREEN));
        Utils.runLater(() -> openMainDialog(player));
    }

    // ==========================================
    // ID / ENTITY_TYPE
    // ==========================================

    private Dialog buildIdDialog(Player player) {
        return DialogBuilder.create(Component.text("Đặt ID entity"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("id", id == null ? NamedTextColor.RED : NamedTextColor.GREEN))
                .addTextInput("value", Component.text("ID (chữ thường, dùng _ thay khoảng trắng)"), b -> b.initial(id == null ? "" : id).maxLength(64))
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
        return DialogBuilder.create(Component.text("Đặt loại entity"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("entity_type", NamedTextColor.GREEN))
                .addTextInput("value", Component.text("Tên EntityType (vd: ZOMBIE, SKELETON, HELL_KNIGHT)"), b -> b.initial(entityType.name()).maxLength(64))
                .confirmation()
                .yesButton(Component.text("Xác nhận"), null, (response, audience) -> {
                    EntityType matched = matchEntityType(response.getText("value"));
                    if (matched == null) player.sendMessage(Utils.text("Không tìm thấy EntityType '" + response.getText("value") + "'.", NamedTextColor.RED));
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

    private static String sanitizeId(@Nullable String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    // ==========================================
    // COMPONENT DIALOGS
    // ==========================================

    /**
     * Wraps a component-specific {@link DialogBuilder} (inputs already added) into a 2-or-3
     * button multiAction dialog: "Lưu" always, "Xoá" only if the component is currently set, and
     * an exit button that discards and returns to the main menu. Mirrors
     * {@code gui.itemcreator.ItemCreatorGui}'s private helper of the same shape.
     */
    private Dialog wrapComponentDialog(Player player, EntityComponentKey<?> key, DialogBuilder builder, DialogActionCallback onSave, DialogActionCallback onClear) {
        boolean present = components.containsKey(key.id());
        builder.externalTitle(Component.text(key.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, onSave);
        if (present) multi.addButton(Component.text("Xoá"), null, onClear);
        multi.columns(present ? 3 : 2);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> openMainDialog(player)));

        return multi.build();
    }

    private Dialog buildDisplayNameDialog(Player player) {
        DisplayNameComponent current = (DisplayNameComponent) components.get(EntityComponentKeys.DISPLAY_NAME.id());
        DialogBuilder builder = DialogBuilder.create(Component.text("Tên hiển thị"))
                .canCloseWithEscape(false)
                .addTextInput("value", Component.text("Tên (hỗ trợ MiniMessage)"), b -> b.initial(current == null ? "" : current.name()).maxLength(256));

        return wrapComponentDialog(player, EntityComponentKeys.DISPLAY_NAME, builder,
                (response, audience) -> {
                    String text = response.getText("value");
                    if (text == null || text.isBlank()) components.remove(EntityComponentKeys.DISPLAY_NAME.id());
                    else components.put(EntityComponentKeys.DISPLAY_NAME.id(), new DisplayNameComponent(text));
                    Utils.runLater(() -> openMainDialog(player));
                },
                (response, audience) -> {
                    components.remove(EntityComponentKeys.DISPLAY_NAME.id());
                    Utils.runLater(() -> openMainDialog(player));
                });
    }

    private Dialog buildBehaviorDialog(Player player) {
        BehaviorComponent current = (BehaviorComponent) components.get(EntityComponentKeys.BEHAVIOR.id());
        DialogBuilder builder = DialogBuilder.create(Component.text("Hành vi entity"))
                .canCloseWithEscape(false)
                .addCheckboxInput("no_ai", Component.text("Tắt AI"), current != null && current.noAi(), "true", "false")
                .addCheckboxInput("invulnerable", Component.text("Bất tử"), current != null && current.invulnerable(), "true", "false")
                .addCheckboxInput("persistent", Component.text("Không despawn tự nhiên"), current != null && current.persistent(), "true", "false")
                .addTextInput("detection_range", Component.text("Phạm vi phát hiện người chơi (block)"), b -> b.initial(String.valueOf(current == null ? 20 : current.detectionRange())).maxLength(16));

        return wrapComponentDialog(player, EntityComponentKeys.BEHAVIOR, builder,
                (response, audience) -> {
                    Boolean noAi = response.getBoolean("no_ai");
                    Boolean invulnerable = response.getBoolean("invulnerable");
                    Boolean persistent = response.getBoolean("persistent");
                    Float detectionRange = DialogInputUtils.parseFloat(response.getText("detection_range"), 0f, 1000f);
                    components.put(EntityComponentKeys.BEHAVIOR.id(), new BehaviorComponent(
                            noAi != null && noAi,
                            invulnerable != null && invulnerable,
                            persistent != null && persistent,
                            detectionRange == null ? 20 : Math.round(detectionRange)));
                    Utils.runLater(() -> openMainDialog(player));
                },
                (response, audience) -> {
                    components.remove(EntityComponentKeys.BEHAVIOR.id());
                    Utils.runLater(() -> openMainDialog(player));
                });
    }

    private Dialog buildBossBarDialog(Player player) {
        BossBarComponent current = (BossBarComponent) components.get(EntityComponentKeys.BOSS_BAR.id());
        int initialRange = current == null ? 30 : current.getRange();
        BossBar.Color initialColor = current == null ? BossBar.Color.WHITE : current.getColor();
        BossBar.Overlay initialStyle = current == null ? BossBar.Overlay.PROGRESS : current.getStyle();

        List<SingleOptionDialogInput.OptionEntry> colorOptions = new ArrayList<>();
        for (BossBar.Color color : BossBar.Color.values()) {
            colorOptions.add(SingleOptionDialogInput.OptionEntry.create(color.name(), Component.text(color.name()), color == initialColor));
        }
        List<SingleOptionDialogInput.OptionEntry> styleOptions = new ArrayList<>();
        for (BossBar.Overlay style : BossBar.Overlay.values()) {
            styleOptions.add(SingleOptionDialogInput.OptionEntry.create(style.name(), Component.text(style.name()), style == initialStyle));
        }

        DialogBuilder builder = DialogBuilder.create(Component.text("Boss bar"))
                .canCloseWithEscape(false)
                .addTextInput("range", Component.text("Phạm vi hiển thị (block)"), b -> b.initial(String.valueOf(initialRange)).maxLength(16))
                .addSingleOptionInput("color", Component.text("Màu"), colorOptions, b -> {})
                .addSingleOptionInput("style", Component.text("Kiểu"), styleOptions, b -> {})
                .addCheckboxInput("boss_fog", Component.text("Hiệu ứng sương mù boss"), current == null || current.isBossFog(), "true", "false");

        return wrapComponentDialog(player, EntityComponentKeys.BOSS_BAR, builder,
                (response, audience) -> {
                    Float range = DialogInputUtils.parseFloat(response.getText("range"), 1f, 500f);
                    String colorRaw = response.getText("color");
                    String styleRaw = response.getText("style");
                    Boolean bossFog = response.getBoolean("boss_fog");

                    BossBar.Color color = colorRaw == null ? initialColor : BossBar.Color.valueOf(colorRaw);
                    BossBar.Overlay style = styleRaw == null ? initialStyle : BossBar.Overlay.valueOf(styleRaw);

                    components.put(EntityComponentKeys.BOSS_BAR.id(), new BossBarComponent(
                            range == null ? 30 : Math.round(range), color, style, bossFog == null || bossFog));
                    Utils.runLater(() -> openMainDialog(player));
                },
                (response, audience) -> {
                    components.remove(EntityComponentKeys.BOSS_BAR.id());
                    Utils.runLater(() -> openMainDialog(player));
                });
    }

    private Dialog buildNameplateDialog(Player player) {
        NameplateComponent current = (NameplateComponent) components.get(EntityComponentKeys.NAMEPLATE.id());

        DialogBuilder builder = DialogBuilder.create(Component.text("Bảng tên nổi"))
                .canCloseWithEscape(false)
                .addCheckboxInput("show_name", Component.text("Hiện tên"), current == null || current.showName(), "true", "false")
                .addCheckboxInput("show_health", Component.text("Hiện máu"), current == null || current.showHealth(), "true", "false")
                .addTextInput("height_offset", Component.text("Độ cao thêm phía trên đầu (block)"), b -> b.initial(String.valueOf(current == null ? 0.3 : current.heightOffset())).maxLength(16));

        return wrapComponentDialog(player, EntityComponentKeys.NAMEPLATE, builder,
                (response, audience) -> {
                    Boolean showName = response.getBoolean("show_name");
                    Boolean showHealth = response.getBoolean("show_health");
                    Float heightOffset = DialogInputUtils.parseFloat(response.getText("height_offset"), -5f, 10f);

                    components.put(EntityComponentKeys.NAMEPLATE.id(), new NameplateComponent(
                            showHealth == null || showHealth,
                            showName == null || showName,
                            heightOffset == null ? 0.3 : heightOffset));
                    Utils.runLater(() -> openMainDialog(player));
                },
                (response, audience) -> {
                    components.remove(EntityComponentKeys.NAMEPLATE.id());
                    Utils.runLater(() -> openMainDialog(player));
                });
    }

    private Dialog buildAttributesDialog(Player player) {
        AttributeComponent current = (AttributeComponent) components.get(EntityComponentKeys.ATTRIBUTES.id());
        Map<EntityAttribute, Double> initialValues = current == null ? Map.of() : current.values();
        return new EntityAttributeEditorGui(this, initialValues, current != null).buildListDialog(player);
    }

    private Dialog buildEquipmentDialog(Player player) {
        EquipmentComponent current = (EquipmentComponent) components.get(EntityComponentKeys.EQUIPMENT.id());
        Map<EquipmentSlot, EntityEquipment> initialSlots = current == null ? Map.of() : current.slots();
        return new EntityEquipmentEditorGui(this, initialSlots, current != null).buildListDialog(player);
    }

    private Dialog buildSpellsDialog(Player player) {
        SpellComponent current = (SpellComponent) components.get(EntityComponentKeys.SPELLS.id());
        return new EntitySpellListEditorGui(this, current).buildHubDialog(player);
    }

    // ==========================================
    // COMMAND
    // ==========================================

    public static void registerCommand() {
        new CommandAPICommand("smpentitycreator")
                .withSubcommand(new CommandAPICommand("new")
                        .executesPlayer((player, args) -> {
                            new EntityCreatorGui().openMainDialog(player);
                        }))
                .withSubcommand(new CommandAPICommand("edit")
                        .withArguments(new StringArgument("entity_id"))
                        .executesPlayer((player, args) -> {
                            String entityId = (String) args.get("entity_id");
                            BaseEntity existing = Registries.ENTITY.get(entityId);
                            if (existing == null) {
                                player.sendMessage(Utils.text("Không tìm thấy entity với ID '" + entityId + "'.", NamedTextColor.RED));
                                return;
                            }
                            EntityCreatorGui.editExisting(existing).openMainDialog(player);
                        }))
                .register();
    }
}
