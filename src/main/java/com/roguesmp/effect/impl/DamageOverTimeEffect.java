package com.roguesmp.effect.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.DamageType;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class DamageOverTimeEffect extends SmpEffect {

    public static final String ID = "damage_over_time";

    public static final Codec<DamageOverTimeEffect> CODEC = Codec.composite(
            SmpEffect.BASE_CODEC.forGetter(SmpEffect::getBaseProperties),
            Codec.DOUBLE.fieldOf("value").forGetter(DamageOverTimeEffect::getMagnitude),
            DamageOverTimeEffect::new
    );

    private final double value;

    public DamageOverTimeEffect(int duration, double value) {
        super(ID, duration);
        this.value = value;
    }

    public DamageOverTimeEffect(BaseProperties base, double value) {
        super(ID, base);
        this.value = value;
    }

    @Override
    public double getMagnitude() {
        return value;
    }

    @Override
    protected boolean isPersistentByDefault() {
        return false;
    }

    @Override
    public @Nullable Component getDisplayComponent() {
        return Component.text("-" + Utils.formatDecimal(value) + " máu/s", NamedTextColor.DARK_GREEN);
    }

    @Override
    public void onTick(Entity entity, boolean oneHz, boolean twoHz) {
        if (!oneHz || !(entity instanceof LivingEntity living) || !living.isValid()) return;

        DamageEvent.Metadata metadata = DamageEvent.Metadata.effect(DamageType.AILMENT);
        metadata.setIgnoreIframe(true);
        metadata.setDoKnockback(false);
        DamageUtils.damage(living, null, value, metadata);
    }
}
