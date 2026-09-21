package com.roguesmp.player.ability.impl.warrior;

import com.roguesmp.constant.Keys;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.effect.impl.ResistanceEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Indomitable extends Ability {

    public static final AbilityInfo<Indomitable> INFO = new AbilityInfo<>("indomitable", Indomitable.class, Indomitable::new);

    private final Player player;

    private final double damageResistance;
    private final double healthIncrease;
    private final double knockbackResistance;

    public Indomitable(SmpPlayer smpPlayer, int level) {
        super(smpPlayer, level);
        player = smpPlayer.getBukkitPlayer();
        damageResistance = getBaseAttributeValue("damage_resistance");
        healthIncrease = getBaseAttributeValue("health_increase");
        knockbackResistance = getBaseAttributeValue("knockback_resistance");
    }

    @Override
    public @NotNull AbilityInfo<? extends Ability> getAbilityInfo() {
        return INFO;
    }

    @Override
    public void onEquip() {
        AttributeInstance kb = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (kb != null) {
            kb.removeModifier(Keys.of("indomitable_kb"));
            kb.addTransientModifier(new AttributeModifier(Keys.of("indomitable_kb"), knockbackResistance, AttributeModifier.Operation.ADD_NUMBER));
        }
        AttributeInstance kbExplosion = player.getAttribute(Attribute.EXPLOSION_KNOCKBACK_RESISTANCE);
        if (kbExplosion != null) {
            kbExplosion.removeModifier(Keys.of("indomitable_kb_exp"));
            kbExplosion.addTransientModifier(new AttributeModifier(Keys.of("indomitable_kb_exp"), knockbackResistance, AttributeModifier.Operation.ADD_NUMBER));
        }
        AttributeInstance hp = player.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null) {
            hp.removeModifier(Keys.of("indomitable_hp"));
            hp.addTransientModifier(new AttributeModifier(Keys.of("indomitable_hp"), healthIncrease, AttributeModifier.Operation.ADD_SCALAR));
        }
        EffectManager.getInstance().addEffect(player, "indomitable_dmg_resist", new ResistanceEffect(SmpEffect.INFINITE, damageResistance, SmpEffect.DeathBehavior.KEEP_ON_DEATH));
    }

    @Override
    public void onUnequip() {
        AttributeInstance kb = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (kb != null) {
            kb.removeModifier(Keys.of("indomitable_kb"));
        }
        AttributeInstance kbExplosion = player.getAttribute(Attribute.EXPLOSION_KNOCKBACK_RESISTANCE);
        if (kbExplosion != null) {
            kbExplosion.removeModifier(Keys.of("indomitable_kb_exp"));
        }
        AttributeInstance hp = player.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null) {
            hp.removeModifier(Keys.of("indomitable_hp"));
        }
        EffectManager.getInstance().clearEffects(player, "indomitable_dmg_resist");
    }
}
