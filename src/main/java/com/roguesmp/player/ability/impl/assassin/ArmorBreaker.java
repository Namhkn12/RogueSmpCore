package com.roguesmp.player.ability.impl.assassin;

import com.roguesmp.constant.DamageType;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.effect.impl.ArmorBreakerEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ArmorBreaker extends Ability {

    public static final AbilityInfo<ArmorBreaker> INFO = new AbilityInfo<>("armor_breaker", ArmorBreaker.class, ArmorBreaker::new);
    public static final String EFFECT_SOURCE = "armor_breaker_effect_source";

    private final Player player;

    private final int duration;
    private final int maxStack;
    private final double bonusPerStack;

    public ArmorBreaker(SmpPlayer smpPlayer, int level) {
        super(smpPlayer, level);
        player = smpPlayer.getBukkitPlayer();
        duration = (int) getBaseAttributeValue("duration");
        maxStack = (int) getBaseAttributeValue("max_stack");
        bonusPerStack = getBaseAttributeValue("bonus_per_stack");
    }

    @Override
    public void onDamageEntity(DamageEvent event) {
        if (!DamageType.isMeleeDamage(event.getDamageType())) return;
        Entity victim = event.getVictim();
        SmpEffect effect = EffectManager.getInstance().getActiveEffects(victim).get(EFFECT_SOURCE + player.getName());
        if (effect instanceof ArmorBreakerEffect breakerEffect) {
            breakerEffect.addStack();
        } else {
            EffectManager.getInstance().addEffect(victim, EFFECT_SOURCE + player.getName(), new ArmorBreakerEffect(duration, bonusPerStack, maxStack, player));
        }
    }

    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }
}
