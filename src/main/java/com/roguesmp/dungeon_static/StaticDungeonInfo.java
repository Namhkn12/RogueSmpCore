package com.roguesmp.dungeon_static;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.registry.Registries;
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
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Definition of a static dungeon. The registry id is the dungeon id players and instances refer to; {@code template}
 * is the template world it is cloned from, written as its bare name (e.g. test_dungeon).
 */
public record StaticDungeonInfo(PolarWorldKey template, DungeonExpiry expiry, String displayName, Key icon, List<String> lore) {

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

    public static final Codec<StaticDungeonInfo> CODEC = Codec.composite(
            TEMPLATE_CODEC.fieldOf("template").forGetter(StaticDungeonInfo::template),
            DungeonExpiry.CODEC.optionalFieldOf("expiry", DungeonExpiry.DEFAULT).forGetter(StaticDungeonInfo::expiry),
            Codec.STRING.fieldOf("display_name").forGetter(StaticDungeonInfo::displayName),
            Codec.KEY.optionalFieldOf("icon", Key.key("minecraft:paper")).forGetter(StaticDungeonInfo::icon),
            Codec.listOf(Codec.STRING).optionalFieldOf("lore", List.of()).forGetter(StaticDungeonInfo::lore),
            StaticDungeonInfo::new
    );

    public static String displayNameOf(String dungeonId) {
        StaticDungeonInfo info = Registries.STATIC_DUNGEON_INFO.get(dungeonId);
        return info != null ? info.displayName() : dungeonId;
    }

    public Component name() {
        return Utils.fromString(displayName);
    }

    public ItemStack toIcon() {
        ItemStack stack = new ItemStack(Material.PAPER);
        stack.setData(DataComponentTypes.ITEM_MODEL, icon());
        stack.setData(DataComponentTypes.CUSTOM_NAME, name().decoration(TextDecoration.ITALIC, false));
        stack.setData(DataComponentTypes.LORE, ItemLore.lore(Utils.fromStrings(lore).stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList()));
        return stack;
    }
}
