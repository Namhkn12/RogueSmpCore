package com.roguesmp.item.component.impl;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.codec.Codec;
import com.roguesmp.enchant.Enchants;
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

    public static final Codec<EnchantComponent> CODEC = Codec.composite(
            Codec.unboundedMap(Codec.enumOf(Enchants.class), Codec.INT).fieldOf("enchants").forGetter(EnchantComponent::getBaseEnchants),
            EnchantComponent::new
    );

    private final Map<Enchants, Integer> enchants = new EnumMap<>(Enchants.class);

    @GsonIgnore
    private final EnumMap<Enchants, Integer> persistentEnchants = new EnumMap<>(Enchants.class);

    @GsonIgnore
    private final Map<String, Map<Enchants, Integer>> modifierEnchants = new HashMap<>();

    @GsonIgnore
    private final EnumMap<Enchants, Integer> totalEnchants = new EnumMap<>(Enchants.class);

    @GsonIgnore
    private boolean isDirty = true;

    public EnchantComponent(Map<Enchants, Integer> enchants) {
        this.enchants.putAll(enchants);
    }

    /**
     * Get the total level of the enchants that is the sum of BaseItem's enchant level and Modifiers' enchant level
     */
    public int getTotalLevel(Enchants enchant) {
        update();
        return this.totalEnchants.getOrDefault(enchant, 0);
    }

    public int getModifierLevel(Enchants enchant) {
        int total = 0;
        for (Map<Enchants, Integer> modifierMap : modifierEnchants.values()) {
            total += modifierMap.getOrDefault(enchant, 0);
        }
        return total;
    }

    /**
     * Get the level of the enchants that is present on the itemStack's pdc (Added by players is one source)
     */
    public int getPersistentLevel(Enchants enchant) {
        return this.persistentEnchants.getOrDefault(enchant, 0);
    }

    /**
     * Get a map of the fully evaluated total enchant levels, unmodifiable.
     */
    public @Unmodifiable Map<Enchants, Integer> getTotalEnchants() {
        update();
        return Collections.unmodifiableMap(totalEnchants);
    }

    /**
     * Get a map of the base enchant levels, unmodifiable.
     */
    public @Unmodifiable Map<Enchants, Integer> getBaseEnchants() {
        update();
        return Collections.unmodifiableMap(enchants);
    }

    public @Unmodifiable Map<Enchants, Integer> getPersistentEnchants() {
        return Collections.unmodifiableMap(persistentEnchants);
    }

    /**
     * Used to add/replace temporary enchant data (such as a gem that give some enchant extra level). The added modifier will not be saved to ItemStack pdc <br>
     * Can also be used to remove levels by using negative value for input
     */
    public void putModifier(String sourceKey, Modifier modifier) {
        this.modifierEnchants.put(sourceKey, modifier.modifiers());
        this.isDirty = true;
    }

    /**
     * Clear a specific modifier using its sourceKey
     */
    public void removeModifier(String sourceKey) {
        if (this.modifierEnchants.remove(sourceKey) != null) {
            this.isDirty = true;
        }
    }

    /**
     * Add persistent enchants to this EnchantComponent. They will be saved into the ItemStack's pdc <br>
     * Can also be used to remove levels by using negative values for input
     */
    public void addPersistentEnchant(Modifier modifier) {
        for (var entry : modifier.modifiers().entrySet()) {
            this.persistentEnchants.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        this.isDirty = true;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new EnchantComponent(enchants);
    }

    public void contributeLore(ItemLoreContext context) {
        update();
        List<Component> res = new ArrayList<>();
        for (Enchants enchants : totalEnchants.keySet()) {
            int level = getTotalLevel(enchants);
            if (level <= 0) continue; //Skip non-positive enchants
            List<Component> lines = enchants.getEnchant().getDisplayText(level, context.player(), context.data());
            if (lines != null) res.addAll(lines);
        }

        context.builder().putLines(3, res);
    }

    @Override
    public void save(PersistentDataContainer pdc) {
        update();
        totalEnchants.forEach((enchants1, integer) -> {
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
        update();
        Map<Enchantment, Integer> enchantMap = new HashMap<>();
        totalEnchants.forEach((enchants1, integer) -> enchants1.getEnchant().attachVanillaEnchant(enchantMap, integer));
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
            if (enchantPair.length < 2) continue;
            String enchantId = enchantPair[0];
            String level = enchantPair[1];
            Enchants enchants = Enchants.fromId(enchantId);
            if (enchants == null) continue;
            int intLevel = Integer.parseInt(level);

            modifierMap.put(enchants, intLevel);
        }

        if (modifierMap.isEmpty()) return;
        this.addPersistentEnchant(new Modifier(modifierMap));
    }

    private void update() {
        if (!isDirty) return;

        this.totalEnchants.clear();

        // static base item enchants
        this.totalEnchants.putAll(this.enchants);

        //Merge persistent player enchants
        this.persistentEnchants.forEach((enchant, lvl) ->
                this.totalEnchants.merge(enchant, lvl, Integer::sum)
        );

        //Merge all active keyed systems (Gems, buffs,...)
        for (Map<Enchants, Integer> modifierMap : modifierEnchants.values()) {
            modifierMap.forEach((enchant, lvl) ->
                    this.totalEnchants.merge(enchant, lvl, (integer, integer2) -> {
                        int value = integer + integer2;
                        if (value <= 0) return null;
                        return value;
                    })
            );
        }

        this.isDirty = false;
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
