package com.roguesmp.effect.impl;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.brewing.BrewResult;
import com.roguesmp.codec.Codec;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * A drunk brewed potion: grants its attribute bonuses and runs its bundled effects as one unit.
 * Always applied under the source {@link #ID}, so drinking another potion replaces this one.
 */
public class BrewedPotionEffect extends SmpEffect {

    public static final String ID = "brewed_potion";

    public static final Codec<BrewedPotionEffect> CODEC = Codec.composite(
            SmpEffect.BASE_CODEC.forGetter(SmpEffect::getBaseProperties),
            Codec.unboundedMap(Codec.enumOf(Attributes.class), Codec.DOUBLE).optionalFieldOf("attributes", Map.of()).forGetter(BrewedPotionEffect::getAttributes),
            Codec.listOf(SmpEffect.CODEC).optionalFieldOf("effects", List.of()).forGetter(BrewedPotionEffect::getEffects),
            BrewedPotionEffect::new
    );

    private final Map<Attributes, Double> attributes;
    private List<SmpEffect> effects;

    public BrewedPotionEffect(BaseProperties base, Map<Attributes, Double> attributes, List<SmpEffect> effects) {
        super(ID, base);
        this.attributes = Map.copyOf(attributes);
        this.effects = List.copyOf(effects);
    }

    public static BrewedPotionEffect of(BrewResult result) {
        BaseProperties base = new BaseProperties(result.durationTicks(), DeathBehavior.REMOVE_ON_DEATH, DisplayMode.WITH_TIME);
        return new BrewedPotionEffect(base, result.attributes(), result.effects());
    }

    public Map<Attributes, Double> getAttributes() {
        return attributes;
    }

    public List<SmpEffect> getEffects() {
        return effects;
    }

    @Override
    public double getMagnitude() {
        return 1;
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    public @Nullable Component getDisplayComponent() {
        return Utils.text("Thuốc pha chế", NamedTextColor.LIGHT_PURPLE);
    }

    @Override
    public void onGainEffect(Entity entity) {
        if (entity instanceof Player player) applyAttributes(player);
        effects.forEach(effect -> effect.onGainEffect(entity));
    }

    @Override
    public void onLoseEffect(Entity entity) {
        if (entity instanceof Player player) {
            SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
            if (smpPlayer != null) smpPlayer.removeAttributeBonus(player, ID);
        }
        effects.forEach(effect -> effect.onLoseEffect(entity));
    }

    private void applyAttributes(Player player) {
        SmpPlayer smpPlayer = PlayerManager.getInstance().getSmpPlayer(player);
        if (smpPlayer != null) {
            smpPlayer.setAttributeBonus(player, ID, attributes);
            return;
        }
        Utils.runLater(() -> {
            SmpPlayer later = PlayerManager.getInstance().getSmpPlayer(player);
            if (later != null && player.isOnline()) later.setAttributeBonus(player, ID, attributes);
        });
    }

    @Override
    public void onTick(Entity entity, boolean oneHz, boolean twoHz) {
        effects.forEach(effect -> effect.onTick(entity, oneHz, twoHz));
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        effects.forEach(effect -> effect.onDeath(event));
    }

    @Override
    public void onDamageEntity(DamageEvent event) {
        effects.forEach(effect -> effect.onDamageEntity(event));
    }

    @Override
    public void onHurt(DamageEvent event) {
        effects.forEach(effect -> effect.onHurt(event));
    }

    @Override
    public BrewedPotionEffect clone() {
        BrewedPotionEffect copy = (BrewedPotionEffect) super.clone();
        copy.effects = effects.stream().map(SmpEffect::clone).toList();
        return copy;
    }
}
