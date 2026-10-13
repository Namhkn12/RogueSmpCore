package com.roguesmp.dungeon_core;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
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
import java.util.List;
import java.util.Map;

/**
 * A dungeon: the template world each run is cloned from, how it is shown to players, and the features that make up
 * its behavior, keyed by an id unique inside the dungeon. The registry id is the dungeon id.
 *
 * @param icon the item model of the dungeon's menu icon
 */
public record DungeonDefinition(PolarWorldKey template, String displayName, Key icon, List<String> lore, Map<String, DungeonFeature<?>> features) {

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

    public static final Codec<DungeonDefinition> CODEC = Codec.composite(
            TEMPLATE_CODEC.fieldOf("template").forGetter(DungeonDefinition::template),
            Codec.STRING.fieldOf("display_name").forGetter(DungeonDefinition::displayName),
            Codec.KEY.optionalFieldOf("icon", Key.key("minecraft:paper")).forGetter(DungeonDefinition::icon),
            Codec.listOf(Codec.STRING).optionalFieldOf("lore", List.of()).forGetter(DungeonDefinition::lore),
            Codec.unboundedMap(DungeonFeature.CODEC).optionalFieldOf("features", Map.of()).forGetter(DungeonDefinition::features),
            DungeonDefinition::new
    );

    public Component name() {
        return Utils.fromString(displayName);
    }

    /**
     * @param extraLore lines shown under the dungeon's own lore, such as the viewer's status
     */
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
