package com.roguesmp.gui.spawner;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.spawner.Spawner;
import com.roguesmp.registry.Registries;
import com.roguesmp.spawner.behavior.BehaviorData;
import com.roguesmp.spawner.behavior.BehaviorFactory;
import com.roguesmp.utils.Utils;
import com.roguesmp.utils.dialog.DialogBuilder;
import com.roguesmp.utils.dialog.DialogTypeBuilder;
import com.roguesmp.utils.dialog.LenientJson;
import io.papermc.paper.dialog.DialogResponseView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class SpawnerCreatorGui {

    private static final Codec<List<BehaviorData>> BEHAVIOR_LIST_CODEC = Codec.listOf(BehaviorData.CODEC);

    private static final String ID = "spawner_id";
    private static final String NAME = "spawner_name";
    private static final String DELAY = "spawner_delay";
    private static final String MIN_DELAY = "spawner_min_delay";
    private static final String MAX_DELAY = "spawner_max_delay";
    private static final String ACTIVE_RANGE = "spawner_active_range";
    private static final String SPAWN_RANGE = "spawner_spawn_range";
    private static final String MAX_NEARBY = "spawner_max_nearby";
    private static final String SPAWN_COUNT = "spawner_spawn_count";
    private static final String BEHAVIORS = "spawner_behaviors";

    private final @Nullable String originalId;
    private final Map<String, String> form = new LinkedHashMap<>();
    private final Map<String, Integer> mobs = new LinkedHashMap<>();

    public SpawnerCreatorGui() {
        this.originalId = null;
        form.put(ID, "");
        form.put(NAME, "");
        form.put(DELAY, "20");
        form.put(MIN_DELAY, "200");
        form.put(MAX_DELAY, "800");
        form.put(ACTIVE_RANGE, "16");
        form.put(SPAWN_RANGE, "4");
        form.put(MAX_NEARBY, "6");
        form.put(SPAWN_COUNT, "4");
        form.put(BEHAVIORS, "");
    }

    private SpawnerCreatorGui(Spawner spawner) {
        this.originalId = spawner.getId();
        form.put(ID, spawner.getId());
        form.put(NAME, spawner.getName() == null ? "" : spawner.getName());
        if (spawner.getMobs() != null) mobs.putAll(spawner.getMobs());
        form.put(DELAY, String.valueOf(spawner.getDelay()));
        form.put(MIN_DELAY, String.valueOf(spawner.getMinDelay()));
        form.put(MAX_DELAY, String.valueOf(spawner.getMaxDelay()));
        form.put(ACTIVE_RANGE, String.valueOf(spawner.getActiveRange()));
        form.put(SPAWN_RANGE, String.valueOf(spawner.getSpawnRange()));
        form.put(MAX_NEARBY, String.valueOf(spawner.getMaxNearBy()));
        form.put(SPAWN_COUNT, String.valueOf(spawner.getSpawnCount()));
        StringJoiner behaviors = new StringJoiner("\n");
        if (spawner.getBehaviors() != null) spawner.getBehaviors().forEach(behavior -> behaviors.add(formatBehavior(behavior)));
        form.put(BEHAVIORS, behaviors.toString());
    }

    public static SpawnerCreatorGui editExisting(Spawner spawner) {
        return new SpawnerCreatorGui(spawner);
    }

    public void open(Player player) {
        open(player, null);
    }

    Map<String, Integer> mobs() {
        return mobs;
    }

    private void open(Player player, @Nullable String error) {
        DialogBuilder builder = DialogBuilder.create(Component.text(originalId == null ? "Tạo Spawner" : "Sửa Spawner: " + originalId))
                .canCloseWithEscape(false);
        if (error != null) builder.addTextBody(Component.text(error, NamedTextColor.RED));

        builder.addTextInput(ID, Component.text("ID"), 300, true, form.get(ID), 64, null)
                .addTextInput(NAME, Component.text("Tên hiển thị (MiniMessage)"), 300, true, form.get(NAME), 256, null)
                .addTextInput(DELAY, Component.text("Delay ban đầu (tick)"), 300, true, form.get(DELAY), 8, null)
                .addTextInput(MIN_DELAY, Component.text("Min delay (tick)"), 300, true, form.get(MIN_DELAY), 8, null)
                .addTextInput(MAX_DELAY, Component.text("Max delay (tick)"), 300, true, form.get(MAX_DELAY), 8, null)
                .addTextInput(ACTIVE_RANGE, Component.text("Active range (block)"), 300, true, form.get(ACTIVE_RANGE), 8, null)
                .addTextInput(SPAWN_RANGE, Component.text("Spawn range (block)"), 300, true, form.get(SPAWN_RANGE), 8, null)
                .addTextInput(MAX_NEARBY, Component.text("Max nearby"), 300, true, form.get(MAX_NEARBY), 8, null)
                .addTextInput(SPAWN_COUNT, Component.text("Spawn count"), 300, true, form.get(SPAWN_COUNT), 8, null)
                .addTextBody(Component.text(LenientJson.HELP, NamedTextColor.DARK_GRAY))
                .addTextBody(Component.text("type: protector, wave, teleport, cursed", NamedTextColor.DARK_GRAY));
        LenientJson.addInput(builder, BEHAVIORS, Component.text("Behaviors"), form.get(BEHAVIORS));

        DialogTypeBuilder.MultiAction multi = builder.multiAction();
        multi.addButton(Component.text("Mobs (" + mobs.size() + ")", mobs.isEmpty() ? NamedTextColor.RED : NamedTextColor.GREEN), describeMobs(),
                (response, audience) -> {
                    readForm(response);
                    Utils.runLater(() -> new SpawnerMobPickerGui(player, this).showInventory(player));
                });
        multi.addButton(Component.text("Lưu", NamedTextColor.GOLD), null, (response, audience) -> {
            readForm(response);
            Utils.runLater(() -> save(player));
        });
        multi.columns(2);
        multi.exitButton(Component.text("Huỷ"), (response, audience) -> Utils.runLater(() -> new SpawnerBrowserGui(player).showInventory(player)));

        player.showDialog(multi.build());
    }

    private Component describeMobs() {
        if (mobs.isEmpty()) return Component.text("Chưa có mob nào", NamedTextColor.GRAY);
        StringJoiner lines = new StringJoiner("\n");
        mobs.forEach((mob, weight) -> lines.add(mob + " (" + weight + ")"));
        return Component.text(lines.toString(), NamedTextColor.GRAY);
    }

    private void readForm(DialogResponseView response) {
        for (String key : form.keySet()) {
            String value = response.getText(key);
            form.put(key, value == null ? "" : value.trim());
        }
    }

    private void save(Player player) {
        String id = form.get(ID);
        if (id.isEmpty() || !id.matches("[a-z0-9_\\-/]+")) {
            open(player, "ID không hợp lệ (chỉ a-z, 0-9, _ - /).");
            return;
        }

        if (!id.equals(originalId) && Registries.SPAWNER.get(id) != null) {
            open(player, "ID '" + id + "' đã tồn tại.");
            return;
        }

        Spawner spawner = new Spawner();
        spawner.setId(id);
        spawner.setName(form.get(NAME).isEmpty() ? null : form.get(NAME));
        try {
            spawner.setMobs(validateMobs());
            spawner.setDelay(parseInt(DELAY));
            spawner.setMinDelay(parseInt(MIN_DELAY));
            spawner.setMaxDelay(parseInt(MAX_DELAY));
            spawner.setActiveRange(parseInt(ACTIVE_RANGE));
            spawner.setSpawnRange(parseInt(SPAWN_RANGE));
            spawner.setMaxNearBy(parseInt(MAX_NEARBY));
            spawner.setSpawnCount(parseInt(SPAWN_COUNT));
            spawner.setBehaviors(parseBehaviors(form.get(BEHAVIORS)));
        } catch (IllegalArgumentException e) {
            open(player, e.getMessage());
            return;
        }
        if (spawner.getMinDelay() > spawner.getMaxDelay()) {
            open(player, "Min delay phải nhỏ hơn hoặc bằng max delay.");
            return;
        }

        if (originalId != null && !originalId.equals(id)) Registries.SPAWNER.removeAndDeleteFiles(RogueSmpCore.getInstance(), originalId);
        Registries.SPAWNER.registerAndSave(RogueSmpCore.getInstance(), id, spawner);

        player.sendMessage(Utils.fromString("<green>Đã lưu spawner " + id + "."));
        new SpawnerBrowserGui(player).showInventory(player);
    }

    private int parseInt(String key) {
        try {
            int value = Integer.parseInt(form.get(key));
            if (value < 0) throw new IllegalArgumentException("'" + key.substring("spawner_".length()) + "' không được âm.");
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + key.substring("spawner_".length()) + "' phải là số nguyên.");
        }
    }

    private Map<String, Integer> validateMobs() {
        if (mobs.isEmpty()) throw new IllegalArgumentException("Cần ít nhất một mob.");
        for (String mob : mobs.keySet()) {
            if (EntityManager.getInstance().getBaseEntity(mob) == null) throw new IllegalArgumentException("Không tìm thấy entity '" + mob + "'.");
        }
        return new LinkedHashMap<>(mobs);
    }

    private static List<BehaviorData> parseBehaviors(String text) {
        if (text.isBlank()) return List.of();
        DataResult<List<BehaviorData>> result = LenientJson.decode(BEHAVIOR_LIST_CODEC, text);
        if (!result.isSuccess()) throw new IllegalArgumentException("Behaviors không hợp lệ: " + result.error());
        for (BehaviorData behavior : result.result()) {
            try {
                BehaviorFactory.create(behavior);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Behavior không tồn tại: '" + behavior.getType() + "'.");
            }
        }
        return result.result();
    }

    private static String formatBehaviors(List<BehaviorData> behaviors) {
        if (behaviors.isEmpty()) return "[]";
        StringJoiner lines = new StringJoiner(",\n", "[\n", "\n]");
        behaviors.forEach(behavior -> lines.add("{" + formatBehavior(behavior) + "}"));
        return lines.toString();
    }

    static String formatBehavior(BehaviorData behavior) {
        return LenientJson.format(BehaviorData.CODEC, behavior);
    }
}
