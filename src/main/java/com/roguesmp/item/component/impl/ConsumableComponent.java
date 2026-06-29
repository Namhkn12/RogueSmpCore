package com.roguesmp.item.component.impl;

import com.google.gson.*;
import com.roguesmp.context.ItemDataContext;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.serialize.ComponentCodec;
import com.roguesmp.registry.EffectCodecRegistry;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import io.papermc.paper.registry.keys.SoundEventKeys;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Component for when player consume (eat, drink) the item
 */
public class ConsumableComponent implements ItemComponent {

    private final List<SmpEffect> effects = new ArrayList<>();

    private final int hunger;
    private final float saturation;
    private final boolean canAlwaysEat;

    private final float consumeSeconds;
    private final ItemUseAnimation animation;
    private final Key sound;
    private final boolean hasParticles;

    public ConsumableComponent(List<SmpEffect> effects, int hunger, float saturation, boolean canAlwaysEat, float consumeSeconds, ItemUseAnimation animation, Key sound, boolean hasParticles) {
        this.hunger = hunger;
        this.saturation = saturation;
        this.canAlwaysEat = canAlwaysEat;
        this.consumeSeconds = consumeSeconds;
        this.animation = animation;
        this.sound = sound;
        this.hasParticles = hasParticles;
        List<SmpEffect> cloned = new ArrayList<>();
        effects.forEach(smpEffect -> cloned.add(smpEffect.clone()));
        this.effects.addAll(cloned);
    }

    @Override
    public void modifyStack(ItemDataContext context) {
        context.newStack().setData(DataComponentTypes.FOOD,
                FoodProperties.food()
                        .nutrition(hunger)
                        .saturation(saturation)
                        .canAlwaysEat(canAlwaysEat)
                        .build());

        context.newStack().setData(DataComponentTypes.CONSUMABLE,
                Consumable.consumable()
                        .consumeSeconds(consumeSeconds)
                        .animation(animation)
                        .sound(sound)
                        .hasConsumeParticles(hasParticles)
                        .build());
    }

    @Override
    public void contributeLore(ItemLoreContext context) {

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("Khi sử dụng:", NamedTextColor.GRAY));

        for (SmpEffect smpEffect : effects) {
            Component display = smpEffect.getDisplay();
            if (display == null) continue;
            lore.add(Component.space().append(display.decoration(TextDecoration.ITALIC, false)).append(Utils.text(" (" + Utils.intToMinuteAndSeconds(smpEffect.getDuration() / 20) +")", NamedTextColor.GRAY)));
        }

        context.builder().putLines(5, lore);
    }

    @Override
    public @NotNull ItemComponent copy() {
//        return new ConsumableComponent(effects, hunger, saturation, canAlwaysEat, consumeSeconds, animation, sound, hasParticles);
        return this;
    }

    /**
     * Return a view of all effects of this component
     */
    public @Unmodifiable List<SmpEffect> getEffects() {
        return Collections.unmodifiableList(effects);
    }

    public void applyEffects(Player player) {
        List<SmpEffect> effectList = getEffects();
        for (SmpEffect smpEffect : effectList) {
            String source = "consumable_" + smpEffect.getEffectID();
            EffectManager.getInstance().addEffect(player, source, smpEffect); //Use clone
        }

    }

    public int getHunger() {
        return hunger;
    }

    public float getSaturation() {
        return saturation;
    }

    public boolean canAlwaysEat() {
        return canAlwaysEat;
    }

    public float getConsumeSeconds() {
        return consumeSeconds;
    }

    public ItemUseAnimation getAnimation() {
        return animation;
    }

    public Key getSound() {
        return sound;
    }

    public boolean hasParticles() {
        return hasParticles;
    }

    public static class Codec implements ComponentCodec<ConsumableComponent> {

        @Override
        public ConsumableComponent deserialize(JsonElement json, JsonDeserializationContext ctx) {
            List<SmpEffect> effectList = new ArrayList<>();
            JsonObject obj = json.getAsJsonObject();

            if (obj.has("effects")) {
                JsonArray array = obj.getAsJsonArray("effects");
                for (JsonElement element : array) {
                    JsonObject effectJson = element.getAsJsonObject();

                    // 1. Get the ID to determine the type
                    String id = effectJson.get("id").getAsString();

                    // 2. Route to the correct deserialization logic
                    // You can use your EffectCodecRegistry here if you prefer
                    EffectCodecRegistry.EffectDeserializer deserializer = EffectCodecRegistry.get(id);
                    if (deserializer == null) continue;
                    SmpEffect effect = deserializer.deserialize(effectJson);

                    effectList.add(effect);
                }
            }

            // Deserialize new fields with defaults if missing
            return new ConsumableComponent(
                    effectList,
                    obj.has("hunger") ? obj.get("hunger").getAsInt() : 0,
                    obj.has("saturation") ? obj.get("saturation").getAsFloat() : 0.0f,
                    obj.has("canAlwaysEat") && obj.get("canAlwaysEat").getAsBoolean(),
                    obj.has("consumeSeconds") ? obj.get("consumeSeconds").getAsFloat() : 1.6f,
                    obj.has("animation") ? ItemUseAnimation.valueOf(obj.get("animation").getAsString().toUpperCase()) : ItemUseAnimation.EAT,
                    obj.has("sound") ? Key.key(obj.get("sound").getAsString()) : SoundEventKeys.ENTITY_GENERIC_EAT,
                    !obj.has("hasParticles") || obj.get("hasParticles").getAsBoolean()
            );
        }

        @Override
        public JsonElement serialize(ConsumableComponent component, JsonSerializationContext ctx) {
            JsonObject json = new JsonObject();
            json.addProperty("hunger", component.hunger);
            json.addProperty("saturation", component.saturation);
            json.addProperty("canAlwaysEat", component.canAlwaysEat);
            json.addProperty("consumeSeconds", component.consumeSeconds);
            json.addProperty("animation", component.animation.name());
            json.addProperty("sound", component.sound.asString());
            json.addProperty("hasParticles", component.hasParticles);

            JsonArray effects = new JsonArray();
            for (SmpEffect effect : component.getEffects()) {
                JsonObject effJson = effect.serialize();
                effJson.addProperty("id", effect.getEffectID());
                effects.add(effJson);
            }
            json.add("effects", effects);
            return json;
        }
    }
}
