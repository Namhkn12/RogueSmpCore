package com.roguesmp.entity.component.impl;

import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.component.EntityComponent;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.entity.component.TickingComponent;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.utils.EntityUtils;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Health-threshold phase triggers (e.g. "at 70% HP, run enterPhaseTwo()"), optionally capping
 * damage so a single big hit can't skip past a threshold before its action runs. Code-driven only -
 * a {@link BossHealthAction} is an arbitrary Java lambda, not JSON-declarable - bosses attach one
 * directly: {@code this.setComponent(EntityComponentKeys.PHASE, new PhaseComponent(events, true));}.
 * <p>
 * Split out of {@code SpellComponent} (which used to own this as a nested {@code PhaseManager} and
 * drive it from its own tick) so phase triggers don't require spell casting to exist at all. Ticks
 * itself via {@link TickingComponent} and hooks {@link #onHurt} directly. When a threshold is
 * crossed and {@code capDamage} is on, this forces the entity's real HP down to align with it -
 * {@link BossBarComponent} (or anything else rendering off {@code entity.getHealth()}) just reads
 * that already-correct value on its own tick, no direct dependency on this component needed.
 */
public class PhaseComponent implements TickingComponent {

    @FunctionalInterface
    public interface BossHealthAction {
        void run(LivingEntity boss);
    }

    private final Map<Integer, BossHealthAction> sourceEvents;
    private final boolean capDamage;

    private final PriorityQueue<Map.Entry<Integer, BossHealthAction>> events;

    public PhaseComponent(@Nullable Map<Integer, BossHealthAction> events, boolean capDamage) {
        this.sourceEvents = events == null ? Map.of() : Map.copyOf(events);
        this.capDamage = capDamage;
        this.events = buildQueue(sourceEvents);
    }

    private static PriorityQueue<Map.Entry<Integer, BossHealthAction>> buildQueue(Map<Integer, BossHealthAction> source) {
        PriorityQueue<Map.Entry<Integer, BossHealthAction>> queue =
                new PriorityQueue<>(Math.max(source.size(), 1), Comparator.comparing(entry -> -entry.getKey()));
        queue.addAll(source.entrySet());
        return queue;
    }

    @Override
    public @NotNull EntityComponent copy() {
        // Runtime state (the queue, consumed as thresholds get crossed) must not be shared between
        // spawned entities - rebuild fresh from the still-pristine source map.
        return new PhaseComponent(sourceEvents, capDamage);
    }

    @Override
    public void tick(SmpEntity smpEntity, int interval) {
        if (smpEntity.dead) return;
        checkThresholds(smpEntity);
    }

    /**
     * Runs (and consumes) every threshold the entity's current HP% has crossed since the last
     * check, in descending order. If {@link #capDamage}, stops after the first crossed threshold
     * and forces HP to align exactly with it (matching the old {@code PhaseManager} contract:
     * one threshold crossing "spent" per check when damage capping is on) - since that HP change
     * doesn't go through a {@link DamageEvent}, refresh the nameplate ourselves if one is attached.
     */
    private void checkThresholds(SmpEntity smpEntity) {
        LivingEntity entity = smpEntity.getEntity();
        double maxHealth = EntityUtils.getMaxHealth(entity);
        if (maxHealth <= 0) return;
        double currentPercent = entity.getHealth() / maxHealth * 100;

        while (true) {
            Map.Entry<Integer, BossHealthAction> entry = events.peek();
            if (entry == null || entry.getKey() < currentPercent) return;

            entry.getValue().run(entity);
            events.remove();

            if (capDamage) {
                entity.setHealth(maxHealth * (entry.getKey() / 100.0));
                refreshNameplate(smpEntity);
                return;
            }
        }
    }

    private void refreshNameplate(SmpEntity smpEntity) {
        NameplateComponent nameplate = smpEntity.getComponent(EntityComponentKeys.NAMEPLATE);
        if (nameplate != null) nameplate.update(smpEntity);
    }

    /**
     * Pre-emptively caps a hit so it can't drop HP past the next threshold in one go - the actual
     * phase action still only fires from {@link #tick}/{@link #checkThresholds} once HP has
     * landed on (or below) that threshold, this just prevents overshooting it in a single hit.
     */
    @Override
    public void onHurt(DamageEvent event, SmpEntity smpEntity) {
        if (!capDamage || event.getDamageType() == DamageType.TRUE) return;

        LivingEntity entity = smpEntity.getEntity();
        getNextHealthThreshold().ifPresent(nextHpPercent -> {
            // Min 1 to make sure we actually go below the threshold but don't kill the boss
            double setHealth = Math.max(nextHpPercent * EntityUtils.getMaxHealth(entity) / 100, 1);
            double health = entity.getHealth();
            if (health - event.getFinalDamage() >= setHealth) {
                return;
            }
            entity.setHealth(Math.max(0, health - setHealth + 1));
            event.addDamageModifier(0, DamageOperation.MORE_FINAL);
            refreshNameplate(smpEntity);
        });
    }

    public boolean capsDamage() {
        return capDamage;
    }

    public Optional<Integer> getNextHealthThreshold() {
        return Optional.ofNullable(events.peek()).map(Map.Entry::getKey);
    }

    public boolean removeHealthEvent(int percent) {
        return events.removeIf(entry -> entry.getKey() == percent);
    }
}
