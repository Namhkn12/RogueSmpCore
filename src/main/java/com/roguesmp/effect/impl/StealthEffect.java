package com.roguesmp.effect.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.Keys;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.utils.EntityUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.*;
import org.jetbrains.annotations.Nullable;


public class StealthEffect extends SmpEffect {

    public static final String KEY = "stealth_effect";
    public static final Codec<StealthEffect> CODEC = Codec.composite(
            SmpEffect.BASE_CODEC.forGetter(StealthEffect::getBaseProperties),
            StealthEffect::new
    );

    public StealthEffect(int duration) {
        this(new BaseProperties(duration, DeathBehavior.REMOVE_ON_DEATH, DisplayMode.WITH_TIME));
    }

    public StealthEffect(BaseProperties base) {
        super(KEY, base);
    }

    @Override
    public double getMagnitude() {
        return 0;
    }

    @Override
    protected boolean isPersistentByDefault() {
        return true;
    }

    @Override
    public @Nullable Component getDisplayComponent() {
        return Component.text("Ẩn thân", NamedTextColor.GREEN);
    }

    @Override
    public void onGainEffect(Entity entity) {
        EntityManager.getInstance().addMetadata(entity, Keys.IN_STEALTH_META_KEY, true);
        for (LivingEntity surround : EntityUtils.getNearbyMobs(entity.getLocation(), 32, 32, 32, living -> !living.getUniqueId().equals(entity.getUniqueId()))) {
            if (surround instanceof Mob mob) EntityUtils.forgetTarget(mob, entity);
        }
    }

    @Override
    public void onLoseEffect(Entity entity) {
        EntityManager.getInstance().removeMetadata(entity, Keys.IN_STEALTH_META_KEY);
    }
}
