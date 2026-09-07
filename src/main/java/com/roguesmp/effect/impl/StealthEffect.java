package com.roguesmp.effect.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.Keys;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.entity.EntityManager;
import com.roguesmp.utils.EntityUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.*;
import org.bukkit.entity.memory.MemoryKey;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class StealthEffect extends SmpEffect {

    public static final String KEY = "stealth_effect";
    public static final Codec<StealthEffect> CODEC = Codec.composite(
            SmpEffect.BASE_CODEC.forGetter(StealthEffect::getBaseProperties),
            StealthEffect::new
    );

    public StealthEffect(BaseProperties base) {
        super(KEY, base);
    }

    @Override
    public double getMagnitude() {
        return 0;
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    public @Nullable Component getDisplayComponent() {
        return Component.text("Ẩn thân", NamedTextColor.GREEN);
    }

    @Override
    public void onGainEffect(Entity entity) {
        EntityManager.getInstance().addMetadata(entity, Keys.IN_STEALTH_META_KEY, true);
        if (entity instanceof Player player) { //Player enter stealth
            for (LivingEntity surround : EntityUtils.getNearbyMobs(entity.getLocation(), 32, 32, 32, living -> !living.getUniqueId().equals(entity.getUniqueId()))) {
                if (surround instanceof PiglinAbstract piglinAbstract) {
                    UUID uuid = piglinAbstract.getMemory(MemoryKey.ANGRY_AT);
                    if (player.getUniqueId().equals(uuid)) {
                        piglinAbstract.setMemory(MemoryKey.UNIVERSAL_ANGER, false);
                        piglinAbstract.setMemory(MemoryKey.ANGRY_AT, null);
                    }
                }
                if (surround instanceof Mob mob) {
                    LivingEntity target = mob.getTarget();
                    if (target != null && target.getUniqueId().equals(player.getUniqueId())) {
                        mob.setTarget(null);
                        mob.getPathfinder().stopPathfinding();
                    }
                }
            }
            return;
        }
        //Entity (not player) enter stealth
        for (LivingEntity surround : EntityUtils.getNearbyMobs(entity.getLocation(), 32, 32, 32, living -> !living.getUniqueId().equals(entity.getUniqueId()))) {
            if (surround instanceof PiglinAbstract piglinAbstract) {
                UUID uuid = piglinAbstract.getMemory(MemoryKey.ANGRY_AT);
                if (entity.getUniqueId().equals(uuid)) {
                    piglinAbstract.setMemory(MemoryKey.ANGRY_AT, null);
                }
            }
            if (surround instanceof Mob mob) {
                LivingEntity target = mob.getTarget();
                if (target != null && target.getUniqueId().equals(entity.getUniqueId())) {
                    mob.setTarget(null);
                    mob.getPathfinder().stopPathfinding();
                }
            }
        }
    }

    @Override
    public void onLoseEffect(Entity entity) {
        EntityManager.getInstance().removeMetadata(entity, Keys.IN_STEALTH_META_KEY);
    }
}
