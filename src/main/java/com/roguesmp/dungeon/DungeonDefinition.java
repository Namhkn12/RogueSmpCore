package com.roguesmp.dungeon;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.DynamicOps;
import com.roguesmp.dungeon.script.DataValue;
import com.roguesmp.utils.Utils;
import com.roguesmp.world.PolarTemplateManager;
import com.roguesmp.world.PolarWorldKey;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DungeonDefinition(PolarWorldKey template, String displayName, Key icon, List<String> lore, Map<String, DataValue> variables, Map<String, DungeonFeature<?>> features) {

    private static final String TYPE_FIELD = "type";

    private static final Codec<PolarWorldKey> TEMPLATE_CODEC = Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return DataResult.success(new PolarWorldKey(PolarTemplateManager.NAMESPACE, name));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(e.getMessage());
                }
            },
            PolarWorldKey::name
    );

    // A feature without "type" uses its key as its type.
    private static final Codec<Map<String, DungeonFeature<?>>> FEATURES = new Codec<>() {
        @Override
        public <O> DataResult<O> encode(Map<String, DungeonFeature<?>> input, DynamicOps<O> ops) {
            O target = ops.emptyMap();
            for (Map.Entry<String, DungeonFeature<?>> entry : input.entrySet()) {
                DataResult<O> encoded = DungeonFeature.CODEC.encode(entry.getValue(), ops);
                if (!encoded.isSuccess()) return DataResult.error("['" + entry.getKey() + "']: " + encoded.error());

                DataResult<Map<String, O>> fields = ops.getMap(encoded.result());
                if (!fields.isSuccess()) return DataResult.error("['" + entry.getKey() + "']: " + fields.error());
                boolean typeIsKey = entry.getKey().equals(entry.getValue().getTypeId());
                O feature = ops.emptyMap();
                for (Map.Entry<String, O> field : fields.result().entrySet()) {
                    if (typeIsKey && field.getKey().equals(TYPE_FIELD)) continue;
                    feature = ops.setMapEntry(feature, field.getKey(), field.getValue());
                }
                target = ops.setMapEntry(target, entry.getKey(), feature);
            }
            return DataResult.success(target);
        }

        @Override
        public <O> DataResult<Map<String, DungeonFeature<?>>> decode(O input, DynamicOps<O> ops) {
            DataResult<Map<String, O>> raw = ops.getMap(input);
            if (!raw.isSuccess()) return DataResult.error(raw.error());

            Map<String, DungeonFeature<?>> features = new LinkedHashMap<>();
            for (Map.Entry<String, O> entry : raw.result().entrySet()) {
                DataResult<Map<String, O>> fields = ops.getMap(entry.getValue());
                if (!fields.isSuccess()) return DataResult.error("['" + entry.getKey() + "']: " + fields.error());

                O feature = ops.emptyMap();
                for (Map.Entry<String, O> field : fields.result().entrySet()) feature = ops.setMapEntry(feature, field.getKey(), field.getValue());
                if (!fields.result().containsKey(TYPE_FIELD)) feature = ops.setMapEntry(feature, TYPE_FIELD, ops.createString(entry.getKey()));

                DataResult<DungeonFeature<?>> decoded = DungeonFeature.CODEC.decode(feature, ops);
                if (!decoded.isSuccess()) return DataResult.error("['" + entry.getKey() + "']: " + decoded.error());
                features.put(entry.getKey(), decoded.result());
            }
            return DataResult.success(features);
        }
    };

    public static final Codec<DungeonDefinition> CODEC = Codec.composite(
            TEMPLATE_CODEC.fieldOf("template").forGetter(DungeonDefinition::template),
            Codec.STRING.fieldOf("display_name").forGetter(DungeonDefinition::displayName),
            Codec.KEY.optionalFieldOf("icon", Key.key("minecraft:paper")).forGetter(DungeonDefinition::icon),
            Codec.listOf(Codec.STRING).optionalFieldOf("lore", List.of()).forGetter(DungeonDefinition::lore),
            Codec.unboundedMap(DataValue.CODEC).optionalFieldOf("variables", Map.of()).forGetter(DungeonDefinition::variables),
            FEATURES.optionalFieldOf("features", Map.of()).forGetter(DungeonDefinition::features),
            DungeonDefinition::new
    );

    public Component name() {
        return Utils.fromString(displayName);
    }

    public ItemStack toIcon(List<Component> extraLore) {
        ItemStack stack = new ItemStack(Material.PAPER);
        stack.setData(DataComponentTypes.ITEM_MODEL, icon);
        stack.setData(DataComponentTypes.CUSTOM_NAME, name().decoration(TextDecoration.ITALIC, false));

        List<Component> lines = new ArrayList<>(Utils.fromStrings(lore));
        if (!extraLore.isEmpty()) {
            if (!lines.isEmpty()) lines.add(Component.empty());
            lines.addAll(extraLore);
        }
        stack.setData(DataComponentTypes.LORE, ItemLore.lore(lines.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList()));
        return stack;
    }
}
