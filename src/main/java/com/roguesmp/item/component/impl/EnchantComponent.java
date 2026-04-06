package com.roguesmp.item.component.impl;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.Keys;
import com.roguesmp.context.ItemDataContext;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class EnchantComponent implements ItemComponent {

    public static final NamespacedKey PLAYER_ENCHANT_KEY = Keys.of("p_enchant");

    private final Map<Enchants, Integer> enchants = new EnumMap<>(Enchants.class);

    @GsonIgnore
    private final EnumMap<Enchants, Integer> persistentEnchants = new EnumMap<>(Enchants.class);

    @GsonIgnore
    private final EnumMap<Enchants, Integer> modifierEnchants = new EnumMap<>(Enchants.class);

    public EnchantComponent(Map<Enchants, Integer> enchants) {
        this.enchants.putAll(enchants);
    }

    /**
     * Get the total level of the enchants that is the sum of BaseItem's enchant level and Modifiers' enchant level
     */
    public int getTotalLevel(Enchants enchants) {
        return this.enchants.getOrDefault(enchants, 0);
    }

    /**
     * Get the level of the enchants that is the sum of all added modifiers
     */
    public int getModifierLevel(Enchants enchants) {
        return this.modifierEnchants.getOrDefault(enchants, 0);
    }

    /**
     * Get the level of the enchants that is present on the itemStack's pdc (Added by players is one source)
     */
    public int getPersistentLevel(Enchants enchants) {
        return this.persistentEnchants.getOrDefault(enchants, 0);
    }

    /**
     * Get a map of total enchants level, unmodifiable
     */
    public @Unmodifiable Map<Enchants, Integer> getEnchants() {
        return Collections.unmodifiableMap(enchants);
    }

    /**
     * Used to add temporary enchant data (such as a gem that give some enchant extra level). The added modifier will not be saved to ItemStack pdc
     */
    public void addModifier(Modifier modifier) {

        for (var entry : modifier.modifiers().entrySet()) {
            modifierEnchants.merge(entry.getKey(), entry.getValue(), Integer::sum);
            enchants.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    /**
     * Add persistent enchants to this EnchantComponent. They will be saved into the ItemStack's pdc
     */
    public void addPersistentEnchant(Modifier modifier) {
        for (var entry : modifier.modifiers().entrySet()) {
            Enchants key = entry.getKey();
            Integer level = entry.getValue();
            modifierEnchants.merge(key, level, Integer::sum);
            enchants.merge(key, level, Integer::sum);
            persistentEnchants.merge(key, level, Integer::sum);
        }
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new EnchantComponent(enchants);
    }

    public void contributeLore(ItemLoreContext context) {

        List<Component> res = new ArrayList<>();
        for (Enchants enchants : enchants.keySet()) {
            int level = getTotalLevel(enchants);
            if (level <= 0) continue; //Skip non-positive enchants
            List<Component> lines = enchants.getEnchant().getDisplayText(level, context.player(), context.data());
            if (lines != null) res.addAll(lines);
        }

        context.builder().putLines(3, res);
    }

    @Override
    public void save(PersistentDataContainer pdc) {
        enchants.forEach((enchants1, integer) -> {
            enchants1.getEnchant().attachData(pdc);
        });
        savePersistentEnchant(pdc);
    }

    @Override
    public void load(PersistentDataContainerView pdc) {
        loadPersistentEnchant(pdc);
    }

    @Override
    public void modifyStack(ItemDataContext context) {
        Map<Enchantment, Integer> enchantMap = new HashMap<>();
        enchants.forEach((enchants1, integer) -> enchants1.getEnchant().attachVanillaEnchant(enchantMap, integer));
        if (!enchantMap.isEmpty()) {
            context.newStack().setData(DataComponentTypes.ENCHANTMENTS, ItemEnchantments.itemEnchantments(enchantMap));
        }
    }

    public record Modifier(Map<Enchants, Integer> modifiers){}

    private void loadPersistentEnchant(PersistentDataContainerView pdc) {
        String enchantData = pdc.get(PLAYER_ENCHANT_KEY, PersistentDataType.STRING);
        if (enchantData == null) return;
        // Format: {id1}:{lvl},{id2}:{lvl}
        String[] pairs = enchantData.split(",");
        Map<Enchants, Integer> modifierMap = new EnumMap<>(Enchants.class);
        for (String pairData : pairs) {
            String[] enchantPair = pairData.split(":");
            if (enchantPair.length < 2) return;
            String enchantId = enchantPair[0];
            String level = enchantPair[1];
            Enchants enchants = Enchants.fromId(enchantId);
            if (enchants == null) return;
            int intLevel = Integer.parseInt(level);

            modifierMap.put(enchants, intLevel);
        }

        if (modifierMap.isEmpty()) return;
        this.addPersistentEnchant(new Modifier(modifierMap));
    }

    private void savePersistentEnchant(PersistentDataContainer pdc) {
        if (persistentEnchants.isEmpty()) {
            pdc.remove(PLAYER_ENCHANT_KEY);
            return;
        }

        StringBuilder enchantData = new StringBuilder();

        for (Map.Entry<Enchants, Integer> entry : persistentEnchants.entrySet()) {
            Enchants enchant = entry.getKey();
            int level = entry.getValue();

            if (!enchantData.isEmpty()) {
                enchantData.append(",");
            }

            enchantData.append(enchant.getEnchant().getId())
                    .append(":")
                    .append(level);
        }

        pdc.set(PLAYER_ENCHANT_KEY, PersistentDataType.STRING, enchantData.toString());
    }
}
