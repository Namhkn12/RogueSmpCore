package com.roguesmp.listener;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.BrewedPotionComponent;
import com.roguesmp.item.component.impl.ConsumableComponent;
import org.bukkit.Location;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ThrownPotionListener implements Listener {
    private static final double SPLASH_RADIUS = 4.0;
    private static final double LINGERING_DURATION_SCALE = 0.25;
    private static final long CLOUD_TICK_INTERVAL = 5L;

    private final RogueSmpCore plugin;

    public ThrownPotionListener(RogueSmpCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();
        SmpItem smpItem = SmpItem.wrap(potion.getItem());
        if (!hasCustomEffects(smpItem)) return;

        Location center = potion.getLocation();
        for (LivingEntity entity : center.getNearbyLivingEntities(SPLASH_RADIUS, 2.0, SPLASH_RADIUS)) {
            double distanceSquared = entity.getLocation().distanceSquared(center);
            if (distanceSquared >= SPLASH_RADIUS * SPLASH_RADIUS) continue;
            double intensity = entity.equals(event.getHitEntity()) ? 1.0 : 1.0 - Math.sqrt(distanceSquared) / SPLASH_RADIUS;
            applyEffects(smpItem, entity, intensity);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLingeringPotionSplash(LingeringPotionSplashEvent event) {
        SmpItem smpItem = SmpItem.wrap(event.getEntity().getItem());
        if (!hasCustomEffects(smpItem)) return;

        AreaEffectCloud cloud = event.getAreaEffectCloud();
        Map<UUID, Integer> victims = new HashMap<>();
        cloud.getScheduler().runAtFixedRate(plugin, task -> {
            if (!cloud.isValid()) {
                task.cancel();
                return;
            }
            if (cloud.getTicksLived() < cloud.getWaitTime()) return;

            int now = cloud.getTicksLived();
            victims.values().removeIf(expiry -> expiry <= now);

            float radius = cloud.getRadius();
            Location center = cloud.getLocation();
            for (LivingEntity entity : center.getNearbyLivingEntities(radius, 0.5, radius + 1)) {
                if (victims.containsKey(entity.getUniqueId())) continue;
                double dx = entity.getX() - center.getX();
                double dz = entity.getZ() - center.getZ();
                if (dx * dx + dz * dz > radius * radius) continue;

                victims.put(entity.getUniqueId(), now + cloud.getReapplicationDelay());
                applyEffects(smpItem, entity, LINGERING_DURATION_SCALE);

                if (cloud.getRadiusOnUse() != 0) {
                    radius += cloud.getRadiusOnUse();
                    if (radius < 0.5f) {
                        cloud.remove();
                        return;
                    }
                    cloud.setRadius(radius);
                }
            }
        }, null, 1L, CLOUD_TICK_INTERVAL);
    }

    private boolean hasCustomEffects(SmpItem smpItem) {
        return smpItem != null && (smpItem.hasComponent(ItemComponentKeys.CONSUMABLE) || smpItem.hasComponent(ItemComponentKeys.BREWED_POTION));
    }

    private void applyEffects(SmpItem smpItem, LivingEntity entity, double durationScale) {
        ConsumableComponent consumable = smpItem.getComponent(ItemComponentKeys.CONSUMABLE);
        if (consumable != null) consumable.applyEffects(entity, smpItem.getId(), durationScale);

        BrewedPotionComponent brewedPotion = smpItem.getComponent(ItemComponentKeys.BREWED_POTION);
        if (brewedPotion != null) brewedPotion.drink(entity, durationScale);
    }
}
