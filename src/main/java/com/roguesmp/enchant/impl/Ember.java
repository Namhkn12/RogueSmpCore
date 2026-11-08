package com.roguesmp.enchant.impl;

import com.roguesmp.constant.DamageType;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.DamageUtils;
import com.roguesmp.utils.Hitbox;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Ember implements SmpEnchant {

    private static final int STACKS_TO_DETONATE = 5;
    private static final long STACK_DURATION_MILLIS = 4000;
    private static final double RADIUS = 3d;
    private static final double DAMAGE_PER_LEVEL = 3d;
    private static final int PRUNE_THRESHOLD = 256;

    private final Map<UUID, Stack> stacks = new ConcurrentHashMap<>();

    @Override
    public @NotNull String getId() {
        return "ember";
    }

    @Override
    public @NotNull Enchants getEnumConstant() {
        return Enchants.EMBER;
    }

    @Override
    public @NotNull String getSimpleName() {
        return "Hỏa Ngục";
    }

    @Override
    public @NotNull String getSimpleDescription() {
        return "Đòn đánh tích tầng Hỏa Ngục lên mục tiêu. Đủ 5 tầng sẽ phát nổ, gây 3 sát thương mỗi cấp cho kẻ địch xung quanh";
    }

    @Override
    public Material getIcon() {
        return Material.FIRE_CHARGE;
    }

    @Override
    public @Nullable List<Component> getDisplayText(int level, @Nullable SmpPlayer player, PersistentDataContainerView pdc) {
        return defaultLoreProvider(level);
    }

    @Override
    public @NotNull Set<EquipSlot> getActiveSlots() {
        return Set.of(EquipSlot.MAINHAND, EquipSlot.PROJECTILE);
    }

    @Override
    public void onDamageEntityFinal(DamageEvent event, int level, @NotNull SmpPlayer player) {
        if (!event.isBasic(DamageType.MELEE) && !event.isBasic(DamageType.PROJECTILE)) return;
        if (!(event.getVictim() instanceof LivingEntity victim)) return;

        long now = System.currentTimeMillis();
        if (stacks.size() > PRUNE_THRESHOLD) stacks.values().removeIf(stack -> now > stack.expiry());

        Stack previous = stacks.get(victim.getUniqueId());
        boolean continues = previous != null && previous.owner().equals(player.getUuid()) && now <= previous.expiry();
        int count = continues ? previous.count() + 1 : 1;

        if (count < STACKS_TO_DETONATE) {
            stacks.put(victim.getUniqueId(), new Stack(player.getUuid(), count, now + STACK_DURATION_MILLIS));
            victim.getWorld().spawnParticle(Particle.FLAME, victim.getLocation().add(0, victim.getHeight() + 0.3, 0), count * 2, 0.2, 0.1, 0.2, 0.01);
            return;
        }

        stacks.remove(victim.getUniqueId());
        detonate(victim, player.getBukkitPlayer(), level);
    }

    private void detonate(LivingEntity victim, Player attacker, int level) {
        Location center = victim.getLocation().add(0, victim.getHeight() / 2, 0);
        center.getWorld().spawnParticle(Particle.FLAME, center, 40, 1.2, 0.6, 1.2, 0.08);
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 1);
        center.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.6f);

        for (LivingEntity entity : new Hitbox.SphereHitbox(center, RADIUS).getHitMobs()) {
            if (entity.equals(attacker) || entity instanceof Player) continue;
            DamageUtils.damage(entity, attacker, DAMAGE_PER_LEVEL * level, DamageEvent.Metadata.enchant(DamageType.FIRE).setScalable(false));
        }
    }

    @Override
    public void onEquipmentChange(@NotNull SmpPlayer player, int level) {
        if (level == 0) stacks.values().removeIf(stack -> stack.owner().equals(player.getUuid()));
    }

    private record Stack(UUID owner, int count, long expiry) {
    }
}
