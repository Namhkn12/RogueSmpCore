package com.roguesmp.entity;

import com.roguesmp.registry.SkinRegistry;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.*;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A class to help decorate our entities <3
 */
public class EntityEquipment {
    private final Material material;
    // Usually only entity weapon need displayName and lore
    private final String displayName;
    private final List<String> lore;
    private final boolean enchantGlint;
    private final String trimMaterial;
    private final String trimPattern;
    private final String dyeColor;
    private final String headSkin;

    public EntityEquipment(Material material, String displayName, List<String> lore, boolean enchantGlint, String trimMaterial, String trimPattern, String dyeColor, String headSkin) {
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.enchantGlint = enchantGlint;
        this.trimMaterial = trimMaterial;
        this.trimPattern = trimPattern;
        this.dyeColor = dyeColor;
        this.headSkin = headSkin;
    }

    public ItemStack createItemStack() {
        ItemStack itemStack = ItemStack.of(material);
        if (displayName != null) itemStack.setData(DataComponentTypes.ITEM_NAME, Utils.fromString(displayName));
        if (lore != null) itemStack.setData(DataComponentTypes.LORE, ItemLore.lore(Utils.fromStrings(lore)));
        itemStack.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, enchantGlint);
        // Unbreakable, just in case
        itemStack.setData(DataComponentTypes.UNBREAKABLE);
        // Wipe attribute modifiers, the attribute is handled using the entity base attribute
        itemStack.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.itemAttributes().build());
        // It's possible to add custom armor trim via datapacks
        TrimMaterial trimMaterial1 = null;
        TrimPattern trimPattern1 = null;
        if (trimMaterial != null) trimMaterial1 = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_MATERIAL).get(NamespacedKey.minecraft(trimMaterial));
        if (trimPattern != null) trimPattern1 = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN).get(NamespacedKey.minecraft(trimPattern));
        if (trimMaterial1 != null && trimPattern1 != null) {
            itemStack.setData(DataComponentTypes.TRIM, ItemArmorTrim.itemArmorTrim(new ArmorTrim(trimMaterial1, trimPattern1)));
        }

        if (dyeColor != null && !dyeColor.isEmpty()) {
            String[] parts = dyeColor.split(",");

            itemStack.setData(DataComponentTypes.DYED_COLOR,
                    DyedItemColor.dyedItemColor(
                            Color.fromARGB(
                                    Integer.parseInt(parts[0]),
                                    Integer.parseInt(parts[1]),
                                    Integer.parseInt(parts[2]),
                                    Integer.parseInt(parts[3]))
                            )
            );

        }

        if (headSkin != null) {
            SkinRegistry.SkinData skinData = SkinRegistry.getInstance().getSkin(headSkin);
            if (skinData != null) {
                itemStack.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(skinData.getProfile()));
            }

        }

        return itemStack;
    }

    public Material getMaterial() {
        return material;
    }

    public @Nullable String getDisplayName() {
        return displayName;
    }

    public @Nullable List<String> getLore() {
        return lore;
    }

    public boolean isEnchantGlint() {
        return enchantGlint;
    }
}
