package com.roguesmp.effect.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.effect.EffectManager;
import com.roguesmp.effect.SmpEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

/**
 * Marks an entity as unable to cast - a pure status marker (duration, display) for now. Nothing in
 * the mob spell system ({@link com.roguesmp.entity.component.impl.SpellComponent}) currently checks
 * for it before letting a spell fire; a caster would need to query
 * {@code EffectManager.getInstance().getActiveEffects(entity).containsKey(SilenceEffect.ID)} itself
 * to actually be blocked by this.
 */
public class SilenceEffect extends SmpEffect {

    public static final String ID = "silence";

    public static final Codec<SilenceEffect> CODEC = Codec.composite(
            SmpEffect.BASE_CODEC.forGetter(SmpEffect::getBaseProperties),
            SilenceEffect::new
    );

    public SilenceEffect(int duration) {
        super(ID, duration);
    }

    public SilenceEffect(BaseProperties base) {
        super(ID, base);
    }

    @Override
    public double getMagnitude() {
        return 1;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public @Nullable Component getDisplayComponent() {
        return Component.text("Câm lặng", NamedTextColor.DARK_GRAY);
    }
}
