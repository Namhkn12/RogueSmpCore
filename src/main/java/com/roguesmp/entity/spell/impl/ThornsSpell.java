package com.roguesmp.entity.spell.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellParams;
import com.roguesmp.entity.spell.common.HitTrigger;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Utils;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.jetbrains.annotations.Nullable;

public class ThornsSpell extends Spell {

    public static final String TYPE_KEY = "thorns_spell";

    public record Params(double reflectRatio, double minDamage, HitTrigger trigger) implements SpellParams {
        public static final Codec<Params> CODEC = Codec.composite(
                Codec.DOUBLE.optionalFieldOf("reflectRatio", 0.2).forGetter(Params::reflectRatio),
                Codec.DOUBLE.optionalFieldOf("minDamage", 1.0).forGetter(Params::minDamage),
                Codec.enumOf(HitTrigger.class).optionalFieldOf("trigger", HitTrigger.MELEE).forGetter(Params::trigger),
                Params::new
        );

        @Override
        public String getTypeId() {
            return TYPE_KEY;
        }
    }

    private final LivingEntity owner;
    private final Params params;

    public ThornsSpell(LivingEntity owner, Params params) {
        this.owner = owner;
        this.params = params;
    }

    @Override
    public void onHurt(DamageEvent event) {
        if (event.isCancelled() || !params.trigger().matches(event.getDamageType())) return;
        LivingEntity attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.equals(owner)) return;

        double reflected = Math.max(params.minDamage(), event.getFinalDamage() * params.reflectRatio());
        Utils.runLater(() -> {
            if (!attacker.isValid()) return;
            DamageEvent.Metadata metadata = new DamageEvent.Metadata(DamageType.THORNS, TYPE_KEY);
            metadata.setDoKnockback(false);
            DamageUtils.damage(attacker, owner, reflected, metadata);
            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENCHANT_THORNS_HIT, 1f, 1f);
        });
    }

    private static @Nullable LivingEntity resolveAttacker(@Nullable Entity damager) {
        if (damager instanceof LivingEntity living) return living;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter) return shooter;
        return null;
    }

    @Override
    public void run(int interval) {
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    public static ThornsSpell create(Params params, LivingEntity owner) {
        return new ThornsSpell(owner, params);
    }
}
