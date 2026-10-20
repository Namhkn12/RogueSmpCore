package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemDataContext;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Component for when player consume (eat, drink) the item
 */
public class ConsumableComponent implements ItemComponent {

    public static final Codec<ConsumableComponent> CODEC = Codec.composite(
            Codec.listOf(SmpEffect.CODEC).fieldOf("effects").forGetter(ConsumableComponent::getEffects),
            Codec.INT.fieldOf("hunger").forGetter(ConsumableComponent::getHunger),
            Codec.FLOAT.fieldOf("saturation").forGetter(ConsumableComponent::getSaturation),
            Codec.BOOLEAN.fieldOf("canAlwaysEat").forGetter(ConsumableComponent::canAlwaysEat),
            Codec.FLOAT.fieldOf("consumeSeconds").forGetter(ConsumableComponent::getConsumeSeconds),
            Codec.enumOf(ItemUseAnimation.class).fieldOf("animation").forGetter(ConsumableComponent::getAnimation),
            Codec.KEY.fieldOf("sound").forGetter(ConsumableComponent::getSound),
            Codec.BOOLEAN.fieldOf("hasParticles").forGetter(ConsumableComponent::hasParticles),
            Codec.STRING.optionalFieldOf("stack_group", "").forGetter(ConsumableComponent::getStackGroup),
            ConsumableComponent::new
    );

    private final List<SmpEffect> effects = new ArrayList<>();

    private final int hunger;
    private final float saturation;
    private final boolean canAlwaysEat;

    private final float consumeSeconds;
    private final ItemUseAnimation animation;
    private final Key sound;
    private final boolean hasParticles;
    private final String stackGroup;

    public ConsumableComponent(List<SmpEffect> effects, int hunger, float saturation, boolean canAlwaysEat, float consumeSeconds, ItemUseAnimation animation, Key sound, boolean hasParticles) {
        this(effects, hunger, saturation, canAlwaysEat, consumeSeconds, animation, sound, hasParticles, "");
    }

    /**
     * @param stackGroup effects from consumables sharing a group don't stack with each other;
     *                   empty means the consuming item's own id
     */
    public ConsumableComponent(List<SmpEffect> effects, int hunger, float saturation, boolean canAlwaysEat, float consumeSeconds, ItemUseAnimation animation, Key sound, boolean hasParticles, String stackGroup) {
        this.stackGroup = stackGroup;
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
            Component display = smpEffect.getDisplayComponent();
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

    public String getStackGroup() {
        return stackGroup;
    }

    public void applyEffects(Entity entity, String itemId) {
        applyEffects(entity, itemId, 1.0);
    }

    public void applyEffects(Entity entity, String itemId, double durationScale) {
        String group = stackGroup.isEmpty() ? itemId : stackGroup;
        for (SmpEffect smpEffect : getEffects()) {
            SmpEffect applied = smpEffect.clone();
            if (!applied.isInfinite()) {
                applied.setDuration((int) (applied.getDuration() * durationScale + 0.5));
                if (applied.getDuration() <= 20) continue;
            }
            EffectManager.getInstance().addEffect(entity, group + "/" + applied.getEffectID(), applied);
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
}
