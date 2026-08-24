package com.roguesmp.gui.itemcreator;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.attribute.Attributes;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.ComponentKey;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.UniqueTrackingComponent;
import com.roguesmp.item.component.impl.*;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.SkinRegistry;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * In-game editor for {@link BaseItem}s, built entirely on the Paper Dialog API
 * ({@link DialogBuilder}). The main menu is a {@code dialogList()}: one nested {@link Dialog}
 * per editable field, each with its {@code externalTitle} set to the raw field/component id
 * (e.g. "name", "durability", "attribute") - that id is both the button label the player sees
 * and the key used against {@link ItemComponentKeys} / {@link Registries#ITEM}, so there's no
 * separate display-name mapping to maintain (players are expected to check the item docs for
 * what each id means). The live preview is an {@code addItemBody(...)} - a hoverable item icon
 * (show_item tooltip), not a real inventory item.
 * <p>
 * Every {@link ItemComponent} kind is editable here except {@code BrokenComponent}, which has no
 * codec and is runtime-only. {@code attribute}, {@code enchant}, {@code consumable},
 * {@code potion_content}, {@code gem_data} and {@code passive_ability} have too many fields/entries
 * for a single dialog and hand off to their own sub-editor ({@link AttributeEditorGui},
 * {@link EnchantEditorGui}, {@link ConsumableEditorGui}, {@link PotionContentEditorGui},
 * {@link GemDataEditorGui}, {@link ItemAbilityListEditorGui}), each of which commits straight back
 * into this draft on every leaf edit rather than requiring a separate top-level apply step.
 */
public class ItemCreatorGui {

    private @Nullable String id;
    private Material base;
    private final Map<String, ItemComponent> components = new HashMap<>();

    public ItemCreatorGui() {
        this(null, Material.PAPER, Map.of());
    }

    private ItemCreatorGui(@Nullable String id, Material base, Map<String, ItemComponent> components) {
        this.id = id;
        this.base = base;
        this.components.putAll(components);
    }

    public static ItemCreatorGui editExisting(BaseItem item) {
        Map<String, ItemComponent> copy = new HashMap<>();
        item.getComponents().forEach((key, value) -> copy.put(key, value.copy()));
        return new ItemCreatorGui(item.getId(), item.getBase(), copy);
    }

    /**
     * Mirrors {@link BaseItem}'s derived uniqueness: true if any current draft component is a
     * {@link UniqueTrackingComponent}.
     */
    private boolean isUnique() {
        return components.values().stream().anyMatch(component -> component instanceof UniqueTrackingComponent);
    }

    public void openMainDialog(Player player) {
        DialogBuilder builder = DialogBuilder.create(Component.text("Trình Tạo Vật Phẩm"))
                .canCloseWithEscape(false)
                .addItemBody(buildPreviewItem())
                .addTextBody(Component.text("ID: " + (id == null ? "(chưa đặt)" : id)
                        + "  |  Material: " + (base == null ? "(chưa đặt)" : base.name())
                        + "  |  Unique: " + (isUnique() ? "Bật (tự động)" : "Tắt")));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();

        multi.addButton(Component.text("id", id == null ? NamedTextColor.RED : NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildIdDialog(player))));
        multi.addButton(Component.text("material", base == null ? NamedTextColor.RED : NamedTextColor.GREEN), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildMaterialDialog(player))));

        multi.addButton(componentLabel(ItemComponentKeys.ITEM_NAME),
                tooltip("Đặt CUSTOM_NAME cho item.", "Mặc định không in nghiêng nên không cần <!i>"),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildNameDialog(player))));
        multi.addButton(componentLabel(ItemComponentKeys.DESCRIPTION),
                tooltip("Đặt description lore cho item.", "Mặc định không in nghiêng nên không cần <!i>"),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildDescriptionDialog(player))));
        multi.addButton(componentLabel(ItemComponentKeys.STACK_SIZE),
                tooltip("Đặt STACK_SIZE cho item.", "Không được nhỏ hơn 0"),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildStackSizeDialog(player))));
        multi.addButton(componentLabel(ItemComponentKeys.DURABILITY),
                tooltip("Đặt MAX_DAMAGE cho item.", null),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildDurabilityDialog(player))));
        multi.addButton(componentLabel(ItemComponentKeys.DURABILITY_REPAIR),
                tooltip("Số độ bền hồi phục khi dùng để sửa trang bị.", null),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildDurabilityRepairDialog(player))));
        multi.addButton(componentLabel(ItemComponentKeys.ITEM_MODEL),
                tooltip("Để thay đổi model item.", null),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildItemModelDialog(player))));
        multi.addButton(componentLabel(ItemComponentKeys.ENCHANT_GLINT),
                tooltip("Buộc bật/tắt hiệu ứng lấp lánh phù phép.", "Ghi đè bất kể item có phù phép hay không"),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildEnchantGlintDialog(player))));
        multi.addButton(componentLabel(ItemComponentKeys.HEAD_SKIN),
                tooltip("Thay đổi head texture.", null),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildHeadSkinDialog(player))));
        multi.addButton(componentLabel(ItemComponentKeys.GEM_SOCKET),
                tooltip("Đặt item có x ô khảm ngọc.", "Khi dùng trong SmpItem cũng sẽ dùng để lưu trữ/xử lí ngọc đã khảm"),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildGemSocketDialog(player))));
        multi.addButton(componentLabel(ItemComponentKeys.MAGIC_POWER),
                tooltip("Đặt ma lực tối đa cho item.", "Giá trị hiện tại (current) được tính bởi RandomStatModifier nếu item có random_stat"),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildMagicPowerDialog(player))));
        multi.addButton(componentLabel(ItemComponentKeys.RANDOM_STAT),
                tooltip("Đánh dấu item có chất lượng ngẫu nhiên (roll khi tạo).", "Chất lượng làm giảm tối đa 20% các chỉ số melee/projectile/defense base khi thấp"),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildRandomStatDialog(player))));
        multi.addButton(componentLabel(ItemComponentKeys.ATTRIBUTE),
                tooltip("Đặt attribute cho item, phải có slot.", "Để trống \"attributes\" để đánh dấu item có thể thay đổi được attribute (từ gem, etc...)"),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildAttributeDialog(player))));
        multi.addButton(componentLabel(ItemComponentKeys.ENCHANT),
                tooltip("Đặt enchants cho item.", "Để trống \"enchants\" để đánh dấu item là \"có thể enchant được\""),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildEnchantDialog(player))));
        multi.addButton(componentLabel(ItemComponentKeys.USAGE_TIMER),
                tooltip("Đặt thời gian sử dụng khi người dùng trang bị, khi hết thời gian vật phẩm sẽ biến mất", null),
                (response, audience) -> player.showDialog(buildUsageTimerDialog(player)));
        multi.addButton(componentLabel(ItemComponentKeys.CONSUMABLE),
                tooltip("Biến một vật phẩm thành vật phẩm có thể ăn uống được.", null),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildConsumableDialog(player))));
        multi.addButton(componentLabel(ItemComponentKeys.POTION_CONTENT),
                tooltip("Điều chỉnh màu potion và thêm vanilla effect.", null),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildPotionContentDialog(player))));
        multi.addButton(componentLabel(ItemComponentKeys.GEM_DATA),
                tooltip("Dùng để chứa dữ liệu ngọc.", "Khi một item có component này, nó có thể dùng để khảm ngọc cho item có cùng equipAttribute slot"),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildGemDataDialog(player))));
        multi.addButton(componentLabel(ItemComponentKeys.PASSIVE_ABILITY),
                tooltip("Đặt các ability bị động cho item.", null),
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildPassiveAbilityDialog(player))));

        multi.addButton(Component.text("save", NamedTextColor.GOLD), null,
                (response, audience) -> Utils.runLater(() -> player.showDialog(buildSaveDialog(player))));

        multi.columns(3);
        multi.exitButton(Component.text("Đóng"), (response, audience) -> player.closeDialog());

        player.showDialog(multi.build());
    }

    private Component componentLabel(ComponentKey<?> key) {
        boolean present = components.containsKey(key.id());
        return Component.text(key.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY);
    }

    /**
     * Builds a hover tooltip for a main-menu component button from the item component docs
     * table - usage line, then an optional note line on its own line.
     */
    private static Component tooltip(String usage, @Nullable String note) {
        Component result = Utils.text(usage, NamedTextColor.GRAY);
        if (note != null) result = result.append(Component.newline()).append(Utils.text(note, NamedTextColor.DARK_GRAY));
        return result;
    }

    // ==========================================
    // PACKAGE-VISIBLE HOOKS FOR SUB-EDITORS
    // ==========================================

    void applyAttribute(EquipAttributeComponent component) {
        components.put(ItemComponentKeys.ATTRIBUTE.id(), component);
    }

    void removeAttribute() {
        components.remove(ItemComponentKeys.ATTRIBUTE.id());
    }

    void applyEnchant(EnchantComponent component) {
        components.put(ItemComponentKeys.ENCHANT.id(), component);
    }

    void removeEnchant() {
        components.remove(ItemComponentKeys.ENCHANT.id());
    }

    void applyConsumable(ConsumableComponent component) {
        components.put(ItemComponentKeys.CONSUMABLE.id(), component);
    }

    void removeConsumable() {
        components.remove(ItemComponentKeys.CONSUMABLE.id());
    }

    void applyPotionContent(PotionContentComponent component) {
        components.put(ItemComponentKeys.POTION_CONTENT.id(), component);
    }

    void removePotionContent() {
        components.remove(ItemComponentKeys.POTION_CONTENT.id());
    }

    void applyGemData(GemDataComponent component) {
        components.put(ItemComponentKeys.GEM_DATA.id(), component);
    }

    void removeGemData() {
        components.remove(ItemComponentKeys.GEM_DATA.id());
    }

    void applyPassiveAbility(PassiveAbilityComponent component) {
        components.put(ItemComponentKeys.PASSIVE_ABILITY.id(), component);
    }

    void removePassiveAbility() {
        components.remove(ItemComponentKeys.PASSIVE_ABILITY.id());
    }

    void reopen(Player player) {
        openMainDialog(player);
    }

    // ==========================================
    // PREVIEW / SAVE
    // ==========================================

    private ItemStack buildPreviewItem() {
        Map<String, ItemComponent> copy = new HashMap<>();
        components.forEach((key, value) -> copy.put(key, value.copy()));
        BaseItem draft = new BaseItem(id == null ? "preview" : id, base, copy);
        return draft.generatePreviewStack(1);
    }

    private Dialog buildSaveDialog(Player player) {
        return DialogBuilder.create(Component.text("Lưu vật phẩm"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("save", NamedTextColor.GOLD))
                .addItemBody(buildPreviewItem())
                .addTextBody(Component.text("ID: " + (id == null ? "(chưa đặt)" : id)))
                .addTextBody(Component.text("Material: " + (base == null ? "(chưa đặt)" : base.name())))
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
        if (base == null) {
            player.sendMessage(Utils.text("Bạn cần chọn material trước khi lưu.", NamedTextColor.RED));
            openMainDialog(player);
            return;
        }

        if (Registries.ITEM.get(id) != null) {
            Dialog dialog = DialogBuilder.create(Component.text("Ghi đè vật phẩm?"))
                    .canCloseWithEscape(false)
                    .addItemBody(buildPreviewItem())
                    .addTextBody(Component.text("Đã tồn tại một vật phẩm với ID '" + id + "'. Ghi đè?"))
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
        Map<String, ItemComponent> toSave = new HashMap<>();
        components.forEach((key, value) -> toSave.put(key, value.copy()));

        BaseItem item = new BaseItem(id, base, toSave);
        Registries.ITEM.registerAndSave(RogueSmpCore.getInstance(), id, item);

        player.sendMessage(Utils.text("Đã lưu vật phẩm '" + id + "'.", NamedTextColor.GREEN));
        Utils.runLater(() -> openMainDialog(player));
    }

    // ==========================================
    // ID / MATERIAL / UNIQUE
    // ==========================================

    private Dialog buildIdDialog(Player player) {
        return DialogBuilder.create(Component.text("Đặt ID vật phẩm"))
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

    private Dialog buildMaterialDialog(Player player) {
        return DialogBuilder.create(Component.text("Đặt Material"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text("material", base == null ? NamedTextColor.RED : NamedTextColor.GREEN))
                .addTextInput("value", Component.text("Tên material (vd: DIAMOND_SWORD)"), b -> b.initial(base == null ? "" : base.name()).maxLength(64))
                .confirmation()
                .yesButton(Component.text("Xác nhận"), null, (response, audience) -> {
                    String raw = response.getText("value");
                    Material matched = raw == null ? null : Material.matchMaterial(raw.trim());
                    if (matched == null) player.sendMessage(Utils.text("Không tìm thấy material '" + raw + "'.", NamedTextColor.RED));
                    else this.base = matched;
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
    // COMPONENT DIALOGS
    // ==========================================

    /**
     * Wraps a component-specific {@link DialogBuilder} (inputs already added) into a 2-or-3
     * button multiAction dialog: "Lưu" always, "Xoá" only if the component is currently set,
     * and an exit button that discards and returns to the main menu.
     */
    private Dialog wrapComponentDialog(Player player, ComponentKey<?> key, DialogBuilder builder, DialogActionCallback onSave, DialogActionCallback onClear) {
        boolean present = components.containsKey(key.id());
        builder.externalTitle(Component.text(key.id(), present ? NamedTextColor.GREEN : NamedTextColor.GRAY));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Lưu"), null, onSave);
        if (present) multi.addButton(Component.text("Xoá"), null, onClear);
        multi.columns(present ? 3 : 2);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> openMainDialog(player)));

        return multi.build();
    }

    private Dialog buildNameDialog(Player player) {
        NameComponent current = (NameComponent) components.get(ItemComponentKeys.ITEM_NAME.id());
        DialogBuilder builder = DialogBuilder.create(Component.text("Tên vật phẩm"))
                .canCloseWithEscape(false)
                .addTextInput("value", Component.text("Tên (hỗ trợ MiniMessage)"), b -> b.initial(current == null ? "" : current.value()).maxLength(256));

        return wrapComponentDialog(player, ItemComponentKeys.ITEM_NAME, builder,
                (response, audience) -> {
                    String text = response.getText("value");
                    if (text == null || text.isBlank()) components.remove(ItemComponentKeys.ITEM_NAME.id());
                    else components.put(ItemComponentKeys.ITEM_NAME.id(), new NameComponent(text));
                    Utils.runLater(() -> openMainDialog(player));
                },
                (response, audience) -> {
                    components.remove(ItemComponentKeys.ITEM_NAME.id());
                    Utils.runLater(() -> openMainDialog(player));
                });
    }

    private Dialog buildDescriptionDialog(Player player) {
        DescriptionComponent current = (DescriptionComponent) components.get(ItemComponentKeys.DESCRIPTION.id());
        String initial = current == null ? "" : String.join("\n", current.description());
        DialogBuilder builder = DialogBuilder.create(Component.text("Mô tả vật phẩm"))
                .canCloseWithEscape(false)
                .addTextInput("value", Component.text("Mỗi dòng là một dòng lore"), b -> b.initial(initial).maxLength(3000).multiline(TextDialogInput.MultilineOptions.create(10, 100)));

        return wrapComponentDialog(player, ItemComponentKeys.DESCRIPTION, builder,
                (response, audience) -> {
                    String text = response.getText("value");
                    if (text == null || text.isBlank()) components.remove(ItemComponentKeys.DESCRIPTION.id());
                    else components.put(ItemComponentKeys.DESCRIPTION.id(), new DescriptionComponent(new ArrayList<>(Arrays.asList(text.split("\n", -1)))));
                    Utils.runLater(() -> openMainDialog(player));
                },
                (response, audience) -> {
                    components.remove(ItemComponentKeys.DESCRIPTION.id());
                    Utils.runLater(() -> openMainDialog(player));
                });
    }

    private Dialog buildStackSizeDialog(Player player) {
        StackSizeComponent current = (StackSizeComponent) components.get(ItemComponentKeys.STACK_SIZE.id());
        int initial = current == null ? 64 : current.size();
        DialogBuilder builder = DialogBuilder.create(Component.text("Kích thước Stack tối đa"))
                .canCloseWithEscape(false)
                .addTextInput("value", Component.text("Số lượng tối đa (1-99)"), b -> b.initial(String.valueOf(initial)).maxLength(16));

        return wrapComponentDialog(player, ItemComponentKeys.STACK_SIZE, builder,
                (response, audience) -> {
                    Float value = DialogInputUtils.parseFloat(response.getText("value"), 1f, 99f);
                    if (value != null) components.put(ItemComponentKeys.STACK_SIZE.id(), new StackSizeComponent(Math.round(value)));
                    Utils.runLater(() -> openMainDialog(player));
                },
                (response, audience) -> {
                    components.remove(ItemComponentKeys.STACK_SIZE.id());
                    Utils.runLater(() -> openMainDialog(player));
                });
    }

    private Dialog buildDurabilityDialog(Player player) {
        DurabilityComponent current = (DurabilityComponent) components.get(ItemComponentKeys.DURABILITY.id());
        int initial = current == null ? 100 : current.maxDurability();
        DialogBuilder builder = DialogBuilder.create(Component.text("Độ bền tối đa"))
                .canCloseWithEscape(false)
                .addTextInput("value", Component.text("Độ bền tối đa (1-5000)"), b -> b.initial(String.valueOf(initial)).maxLength(16));

        return wrapComponentDialog(player, ItemComponentKeys.DURABILITY, builder,
                (response, audience) -> {
                    Float value = DialogInputUtils.parseFloat(response.getText("value"), 1f, 5000f);
                    if (value != null) components.put(ItemComponentKeys.DURABILITY.id(), new DurabilityComponent(Math.round(value)));
                    Utils.runLater(() -> openMainDialog(player));
                },
                (response, audience) -> {
                    components.remove(ItemComponentKeys.DURABILITY.id());
                    Utils.runLater(() -> openMainDialog(player));
                });
    }

    private Dialog buildDurabilityRepairDialog(Player player) {
        DurabilityRepairComponent current = (DurabilityRepairComponent) components.get(ItemComponentKeys.DURABILITY_REPAIR.id());
        int initial = current == null ? 50 : current.getAmount();
        DialogBuilder builder = DialogBuilder.create(Component.text("Lượng độ bền hồi phục"))
                .canCloseWithEscape(false)
                .addTextInput("value", Component.text("Hồi phục (1-5000)"), b -> b.initial(String.valueOf(initial)).maxLength(16));

        return wrapComponentDialog(player, ItemComponentKeys.DURABILITY_REPAIR, builder,
                (response, audience) -> {
                    Float value = DialogInputUtils.parseFloat(response.getText("value"), 1f, 5000f);
                    if (value != null) components.put(ItemComponentKeys.DURABILITY_REPAIR.id(), new DurabilityRepairComponent(Math.round(value)));
                    Utils.runLater(() -> openMainDialog(player));
                },
                (response, audience) -> {
                    components.remove(ItemComponentKeys.DURABILITY_REPAIR.id());
                    Utils.runLater(() -> openMainDialog(player));
                });
    }

    private Dialog buildItemModelDialog(Player player) {
        ItemModelComponent current = (ItemModelComponent) components.get(ItemComponentKeys.ITEM_MODEL.id());
        DialogBuilder builder = DialogBuilder.create(Component.text("Item Model"))
                .canCloseWithEscape(false)
                .addTextInput("value", Component.text("Model key (vd: minecraft:diamond_sword)"), b -> b.initial(current == null ? "" : current.getModelKey().asString()).maxLength(256));

        return wrapComponentDialog(player, ItemComponentKeys.ITEM_MODEL, builder,
                (response, audience) -> {
                    String text = response.getText("value");
                    if (text == null || text.isBlank()) {
                        components.remove(ItemComponentKeys.ITEM_MODEL.id());
                    } else {
                        try {
                            components.put(ItemComponentKeys.ITEM_MODEL.id(), new ItemModelComponent(Key.key(text.trim())));
                        } catch (Exception e) {
                            player.sendMessage(Utils.text("Key không hợp lệ: " + text, NamedTextColor.RED));
                        }
                    }
                    Utils.runLater(() -> openMainDialog(player));
                },
                (response, audience) -> {
                    components.remove(ItemComponentKeys.ITEM_MODEL.id());
                    Utils.runLater(() -> openMainDialog(player));
                });
    }

    private Dialog buildEnchantGlintDialog(Player player) {
        EnchantGlintComponent current = (EnchantGlintComponent) components.get(ItemComponentKeys.ENCHANT_GLINT.id());
        boolean initial = current == null || current.glint();
        DialogBuilder builder = DialogBuilder.create(Component.text("Ghi đè hiệu ứng phù phép (glint)"))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("Buộc bật/tắt hiệu ứng lấp lánh phù phép, bất kể item có phù phép hay không."))
                .addCheckboxInput("value", Component.text("Bật glint"), b -> b.initial(initial));

        return wrapComponentDialog(player, ItemComponentKeys.ENCHANT_GLINT, builder,
                (response, audience) -> {
                    Boolean value = response.getBoolean("value");
                    components.put(ItemComponentKeys.ENCHANT_GLINT.id(), new EnchantGlintComponent(value != null && value));
                    Utils.runLater(() -> openMainDialog(player));
                },
                (response, audience) -> {
                    components.remove(ItemComponentKeys.ENCHANT_GLINT.id());
                    Utils.runLater(() -> openMainDialog(player));
                });
    }

    private Dialog buildHeadSkinDialog(Player player) {
        PlayerHeadSkinComponent current = (PlayerHeadSkinComponent) components.get(ItemComponentKeys.HEAD_SKIN.id());
        DialogBuilder builder = DialogBuilder.create(Component.text("Skin đầu người"))
                .canCloseWithEscape(false)
                .addTextInput("value", Component.text("Skin ID (trong SkinRegistry)"), b -> b.initial(current == null ? "" : current.getSkinId()).maxLength(64));

        return wrapComponentDialog(player, ItemComponentKeys.HEAD_SKIN, builder,
                (response, audience) -> {
                    String text = response.getText("value");
                    if (text == null || text.isBlank()) {
                        components.remove(ItemComponentKeys.HEAD_SKIN.id());
                    } else {
                        String skinId = text.trim();
                        if (SkinRegistry.getInstance().getSkin(skinId) == null) {
                            player.sendMessage(Utils.text("Cảnh báo: Không tìm thấy skin '" + skinId + "' (vẫn được lưu).", NamedTextColor.YELLOW));
                        }
                        components.put(ItemComponentKeys.HEAD_SKIN.id(), new PlayerHeadSkinComponent(skinId));
                    }
                    Utils.runLater(() -> openMainDialog(player));
                },
                (response, audience) -> {
                    components.remove(ItemComponentKeys.HEAD_SKIN.id());
                    Utils.runLater(() -> openMainDialog(player));
                });
    }

    private Dialog buildGemSocketDialog(Player player) {
        GemSocketComponent current = (GemSocketComponent) components.get(ItemComponentKeys.GEM_SOCKET.id());
        int initial = current == null ? 1 : current.getSocketCount();
        DialogBuilder builder = DialogBuilder.create(Component.text("Số ổ khảm ngọc"))
                .canCloseWithEscape(false)
                .addTextInput("value", Component.text("Số ổ (1-9)"), b -> b.initial(String.valueOf(initial)).maxLength(16));

        return wrapComponentDialog(player, ItemComponentKeys.GEM_SOCKET, builder,
                (response, audience) -> {
                    Float value = DialogInputUtils.parseFloat(response.getText("value"), 1f, 9f);
                    if (value != null) components.put(ItemComponentKeys.GEM_SOCKET.id(), new GemSocketComponent(Math.round(value)));
                    Utils.runLater(() -> openMainDialog(player));
                },
                (response, audience) -> {
                    components.remove(ItemComponentKeys.GEM_SOCKET.id());
                    Utils.runLater(() -> openMainDialog(player));
                });
    }

    private Dialog buildMagicPowerDialog(Player player) {
        MagicPowerComponent current = (MagicPowerComponent) components.get(ItemComponentKeys.MAGIC_POWER.id());
        int initial = current == null ? 0 : current.getMax();
        DialogBuilder builder = DialogBuilder.create(Component.text("Ma lực tối đa"))
                .canCloseWithEscape(false)
                .addTextInput("value", Component.text("Ma lực tối đa (>= 0)"), b -> b.initial(String.valueOf(initial)).maxLength(16));

        return wrapComponentDialog(player, ItemComponentKeys.MAGIC_POWER, builder,
                (response, audience) -> {
                    Float value = DialogInputUtils.parseFloat(response.getText("value"), 0f, Float.MAX_VALUE);
                    if (value != null) components.put(ItemComponentKeys.MAGIC_POWER.id(), new MagicPowerComponent(Math.round(value)));
                    Utils.runLater(() -> openMainDialog(player));
                },
                (response, audience) -> {
                    components.remove(ItemComponentKeys.MAGIC_POWER.id());
                    Utils.runLater(() -> openMainDialog(player));
                });
    }

    private Dialog buildRandomStatDialog(Player player) {
        RandomStatComponent current = (RandomStatComponent) components.get(ItemComponentKeys.RANDOM_STAT.id());
        boolean initial = current != null && current.hasRandomQuality();
        DialogBuilder builder = DialogBuilder.create(Component.text("Chất lượng ngẫu nhiên"))
                .canCloseWithEscape(false)
                .addTextBody(Component.text("Mỗi item được tạo ra sẽ roll một chất lượng ngẫu nhiên, làm giảm tối đa 20% một số chỉ số base khi chất lượng thấp."))
                .addCheckboxInput("value", Component.text("Có chất lượng ngẫu nhiên"), b -> b.initial(initial));

        return wrapComponentDialog(player, ItemComponentKeys.RANDOM_STAT, builder,
                (response, audience) -> {
                    Boolean value = response.getBoolean("value");
                    components.put(ItemComponentKeys.RANDOM_STAT.id(), new RandomStatComponent(value != null && value));
                    Utils.runLater(() -> openMainDialog(player));
                },
                (response, audience) -> {
                    components.remove(ItemComponentKeys.RANDOM_STAT.id());
                    Utils.runLater(() -> openMainDialog(player));
                });
    }

    private Dialog buildAttributeDialog(Player player) {
        EquipAttributeComponent current = (EquipAttributeComponent) components.get(ItemComponentKeys.ATTRIBUTE.id());
        EquipSlot currentSlot = current == null ? EquipSlot.MAINHAND : current.getSlot();

        List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();
        for (EquipSlot slot : EquipSlot.values()) {
            entries.add(SingleOptionDialogInput.OptionEntry.create(slot.name(), Component.text(slot.getSimpleName()), slot == currentSlot));
        }

        return DialogBuilder.create(Component.text("Chọn vị trí trang bị"))
                .canCloseWithEscape(false)
                .externalTitle(Component.text(ItemComponentKeys.ATTRIBUTE.id(), current != null ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .addSingleOptionInput("slot", Component.text("Vị trí trang bị"), entries, b -> {})
                .confirmation()
                .yesButton(Component.text("Tiếp tục"), null, (response, audience) -> {
                    String slotId = response.getText("slot");
                    EquipSlot chosen = slotId == null ? currentSlot : EquipSlot.valueOf(slotId);
                    boolean matchesChosenSlot = current != null && current.getSlot() == chosen;
                    Map<Attributes, Double> initialValues = new EnumMap<>(Attributes.class);
                    if (matchesChosenSlot) initialValues.putAll(current.getBaseAttributes());
                    Utils.runLater(() -> player.showDialog(new AttributeEditorGui(this, chosen, initialValues, matchesChosenSlot).buildListDialog(player)));
                })
                .noButton(Component.text("Huỷ"), null, (response, audience) -> Utils.runLater(() -> openMainDialog(player)))
                .build();
    }

    private Dialog buildEnchantDialog(Player player) {
        EnchantComponent current = (EnchantComponent) components.get(ItemComponentKeys.ENCHANT.id());
        Map<Enchants, Integer> initialValues = new EnumMap<>(Enchants.class);
        if (current != null) initialValues.putAll(current.getBaseEnchants());

        return new EnchantEditorGui(this, initialValues, current != null).buildListDialog(player);
    }

    private Dialog buildConsumableDialog(Player player) {
        ConsumableComponent current = (ConsumableComponent) components.get(ItemComponentKeys.CONSUMABLE.id());
        return new ConsumableEditorGui(this, current).buildHubDialog(player);
    }

    private Dialog buildPotionContentDialog(Player player) {
        PotionContentComponent current = (PotionContentComponent) components.get(ItemComponentKeys.POTION_CONTENT.id());
        return new PotionContentEditorGui(this, current).buildHubDialog(player);
    }

    private Dialog buildGemDataDialog(Player player) {
        GemDataComponent current = (GemDataComponent) components.get(ItemComponentKeys.GEM_DATA.id());
        return new GemDataEditorGui(this, current).buildHubDialog(player);
    }

    private Dialog buildPassiveAbilityDialog(Player player) {
        PassiveAbilityComponent current = (PassiveAbilityComponent) components.get(ItemComponentKeys.PASSIVE_ABILITY.id());
        return new ItemAbilityListEditorGui(this, current).buildListDialog(player);
    }

    private Dialog buildUsageTimerDialog(Player player) {
        UsageTimerComponent current = (UsageTimerComponent) components.get(ItemComponentKeys.USAGE_TIMER.id());
        int initial = current == null ? 200 : current.getBaseTickDuration();
        DialogBuilder builder = DialogBuilder.create(Component.text("Thời gian sử dụng tối đa"))
                .canCloseWithEscape(false)
                .addTextInput("value", Component.text("Thời gian sử dụng tối đa, đơn vị tick (>= 20)"), b -> b.initial(String.valueOf(initial)).maxLength(16));

        return wrapComponentDialog(player, ItemComponentKeys.USAGE_TIMER, builder,
                (response, audience) -> {
                    Integer value = DialogInputUtils.parseInt(response.getText("value"), 20, Integer.MAX_VALUE);
                    if (value != null) components.put(ItemComponentKeys.USAGE_TIMER.id(), new UsageTimerComponent(value));
                    Utils.runLater(() -> openMainDialog(player));
                },
                (response, audience) -> {
                    components.remove(ItemComponentKeys.USAGE_TIMER.id());
                    Utils.runLater(() -> openMainDialog(player));
                });
    }

    // ==========================================
    // COMMAND
    // ==========================================

    public static void registerCommand() {
        new CommandAPICommand("smpitemcreator")
                .withSubcommand(new CommandAPICommand("new")
                        .executesPlayer((player, args) -> {
                            new ItemCreatorGui().openMainDialog(player);
                        }))
                .withSubcommand(new CommandAPICommand("edit")
                        .withArguments(new StringArgument("item_id").replaceSuggestions(ArgumentSuggestions.strings(Registries.ITEM.getAll().keySet())))
                        .executesPlayer((player, args) -> {
                            String itemId = (String) args.get("item_id");
                            BaseItem existing = Registries.ITEM.get(itemId);
                            if (existing == null) {
                                player.sendMessage(Utils.text("Không tìm thấy vật phẩm với ID '" + itemId + "'.", NamedTextColor.RED));
                                return;
                            }
                            ItemCreatorGui.editExisting(existing).openMainDialog(player);
                        }))
                .withSubcommand(new CommandAPICommand("delete")
                        .withArguments(new StringArgument("item_id").replaceSuggestions(ArgumentSuggestions.strings(Registries.ITEM.getAll().keySet())))
                        .executesPlayer((player, commandArguments) -> {
                            String itemId = (String) commandArguments.get("item_id");
                            BaseItem existing = Registries.ITEM.get(itemId);
                            if (existing == null) {
                                player.sendMessage(Utils.text("Không tìm thấy vật phẩm với ID '" + itemId + "'.", NamedTextColor.RED));
                                return;
                            }
                            Registries.ITEM.removeAndDeleteFiles(RogueSmpCore.getInstance(), itemId);
                        }))
                .register();
    }
}
