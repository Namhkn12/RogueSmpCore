package com.roguesmp.item.component.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.roguesmp.brewing.BrewRecipe;
import com.roguesmp.brewing.BrewResult;
import com.roguesmp.brewing.BrewStat;
import com.roguesmp.brewing.Brewing;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.JsonOps;
import com.roguesmp.constant.Keys;
import com.roguesmp.context.ItemDataContext;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.impl.BrewedPotionEffect;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PotionContents;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A potion brewed by a brewing block. The stack's PDC only holds its {@link BrewRecipe}; stats are
 * evaluated from the current ingredient definitions and this definition's duration tuning
 * ({@code base_duration} before ingredient changes, {@code min_duration} as the floor, in seconds).
 */
public class BrewedPotionComponent implements ItemComponent {

    public static final Codec<BrewedPotionComponent> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("base_duration", 180).forGetter(BrewedPotionComponent::getBaseDuration),
            Codec.INT.optionalFieldOf("min_duration", 30).forGetter(BrewedPotionComponent::getMinDuration),
            BrewedPotionComponent::new
    );

    private static final NamespacedKey RECIPE_KEY = Keys.of("brew_recipe");
    private static final int LORE_PRIORITY = 5;

    private final int baseDuration;
    private final int minDuration;
    private @Nullable BrewRecipe recipe;
    private @Nullable BrewResult result;

    public BrewedPotionComponent(int baseDuration, int minDuration) {
        this.baseDuration = baseDuration;
        this.minDuration = minDuration;
    }

    public int getBaseDuration() {
        return baseDuration;
    }

    public int getMinDuration() {
        return minDuration;
    }

    public void setRecipe(@Nullable BrewRecipe recipe) {
        this.recipe = recipe;
        this.result = null;
    }

    public @Nullable BrewResult getResult() {
        if (result == null && recipe != null) result = Brewing.evaluate(recipe, baseDuration, minDuration);
        return result;
    }

    public void drink(Player player) {
        BrewResult evaluated = getResult();
        if (evaluated == null) return;

        EffectManager.getInstance().addEffect(player, BrewedPotionEffect.ID, BrewedPotionEffect.of(evaluated));
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new BrewedPotionComponent(baseDuration, minDuration);
    }

    @Override
    public void load(PersistentDataContainerView pdc) {
        String json = pdc.get(RECIPE_KEY, PersistentDataType.STRING);
        DataResult<BrewRecipe> decoded = json == null ? null : BrewRecipe.CODEC.decode(JsonParser.parseString(json), JsonOps.INSTANCE);
        setRecipe(decoded != null && decoded.isSuccess() ? decoded.result() : null);
    }

    @Override
    public void save(PersistentDataContainer pdc) {
        if (recipe == null) {
            pdc.remove(RECIPE_KEY);
            return;
        }
        DataResult<JsonElement> encoded = BrewRecipe.CODEC.encode(recipe, JsonOps.INSTANCE);
        if (encoded.isSuccess()) pdc.set(RECIPE_KEY, PersistentDataType.STRING, encoded.result().toString());
    }

    @Override
    public void modifyStack(ItemDataContext context) {
        BrewResult evaluated = getResult();
        if (evaluated == null) return;
        context.newStack().setData(DataComponentTypes.POTION_CONTENTS, PotionContents.potionContents().customColor(evaluated.bukkitColor()).build());
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        List<Component> lore = new ArrayList<>();
        BrewResult evaluated = getResult();
        if (evaluated == null) {
            lore.add(Utils.text("Chưa được pha chế", NamedTextColor.DARK_GRAY));
            context.builder().putLines(LORE_PRIORITY, lore);
            return;
        }

        lore.add(Utils.text("Khi uống:", NamedTextColor.GRAY));
        evaluated.stats().forEach((stat, value) ->
                stat.display(value, evaluated.durationTicks()).forEach(line -> lore.add(BrewStat.indent(line))));
        lore.add(Utils.text("Thời gian: ", NamedTextColor.GRAY).append(Utils.text(Utils.intToMinuteAndSeconds(evaluated.durationTicks() / 20), NamedTextColor.YELLOW)));
        lore.add(Utils.text("Chỉ một thuốc pha chế có hiệu lực cùng lúc", NamedTextColor.DARK_GRAY));
        context.builder().putLines(LORE_PRIORITY, lore);
    }
}
