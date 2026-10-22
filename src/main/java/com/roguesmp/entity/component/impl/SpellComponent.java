package com.roguesmp.entity.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.component.EntityComponent;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.entity.component.TickingComponent;
import com.roguesmp.entity.spell.*;
import com.roguesmp.entity.spell.common.SpellTargets;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.event.SpellCastEvent;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class SpellComponent implements TickingComponent {

    public static final Codec<SpellComponent> CODEC = Codec.composite(
            Codec.listOf(SpellConfig.CODEC).optionalFieldOf("spells", List.of()).forGetter(SpellComponent::getSpellConfigs),
            Codec.BOOLEAN.optionalFieldOf("canCastSameSpellTwice", false).forGetter(SpellComponent::isCanCastSameSpellTwice),
            SpellComponent::new
    );

    private static final int JSON_START_DELAY = 1;
    private static final int DEFAULT_DETECTION_RANGE = 20;

    private enum State { IDLE, WINDUP, RECOVERY }

    private final List<SpellConfig> spellConfigs;
    private final boolean canCastSameSpellTwice;

    private List<Spell> spells = List.of();
    private List<ActiveSpell> activeSpells = List.of();
    private final Map<ActiveSpell, Long> readyAt = new HashMap<>();
    private int detectionRange = DEFAULT_DETECTION_RANGE;

    private State state = State.IDLE;
    private @Nullable ActiveSpell current;
    private @Nullable ActiveSpell lastCast;
    private int stateTicks;
    private long tick;
    private long castLockUntil;
    private boolean asleep;

    public SpellComponent(List<SpellConfig> spellConfigs, boolean canCastSameSpellTwice) {
        this.spellConfigs = List.copyOf(spellConfigs);
        this.canCastSameSpellTwice = canCastSameSpellTwice;
    }

    public SpellComponent(List<Spell> spells, int detectionRange, int startDelay, boolean canCastSameSpellTwice) {
        this(List.of(), canCastSameSpellTwice);
        this.detectionRange = detectionRange;
        setSpells(spells, startDelay);
    }

    @Override
    public @NotNull EntityComponent copy() {
        return new SpellComponent(spellConfigs, canCastSameSpellTwice);
    }

    @Override
    public void onSpawn(SmpEntity smpEntity) {
        if (spellConfigs.isEmpty()) return;

        List<Spell> built = spellConfigs.stream().map(config -> config.create(smpEntity.getEntity())).toList();

        BehaviorComponent behavior = smpEntity.getComponent(EntityComponentKeys.BEHAVIOR);
        detectionRange = behavior != null ? behavior.getEffectiveDetectionRange() : DEFAULT_DETECTION_RANGE;
        setSpells(built, JSON_START_DELAY);
    }

    public void setSpells(List<Spell> next, int castDelay) {
        Set<Spell> kept = Collections.newSetFromMap(new IdentityHashMap<>());
        kept.addAll(next);
        if (current != null) interrupt(null);
        spells.stream().filter(spell -> !kept.contains(spell)).forEach(Spell::onRemoved);

        spells = List.copyOf(next);
        activeSpells = spells.stream()
                .filter(ActiveSpell.class::isInstance)
                .map(ActiveSpell.class::cast)
                .toList();
        readyAt.keySet().retainAll(activeSpells);
        activeSpells.forEach(spell -> readyAt.putIfAbsent(spell, tick));
        castLockUntil = tick + castDelay;
    }

    @Override
    public void tick(SmpEntity owner, int interval) {
        tick += interval;
        if (owner.dead) return;

        if (detectionRange > 0 && PlayerUtils.playersInRange(owner.getEntity().getLocation(), detectionRange, true).isEmpty()) {
            if (!asleep) {
                asleep = true;
                if (current != null) interrupt(null);
            }
            return;
        }
        asleep = false;

        SpellContext context = new SpellContext(owner, SpellTargets.playerTarget(owner.getEntity()), tick);
        spells.forEach(spell -> spell.tick(context, interval));

        switch (state) {
            case IDLE -> tryStartCast(context);
            case WINDUP -> {
                if (context.target() == null) {
                    interrupt(context);
                    return;
                }
                stateTicks += interval;
                if (stateTicks >= current.windup()) {
                    cast(context);
                } else {
                    current.onWindupTick(context, stateTicks);
                }
            }
            case RECOVERY -> {
                stateTicks += interval;
                if (stateTicks >= current.recovery()) {
                    current = null;
                    enter(State.IDLE);
                }
            }
        }
    }

    private void tryStartCast(SpellContext context) {
        if (tick < castLockUntil || context.target() == null) return;

        List<ActiveSpell> candidates = new ArrayList<>();
        for (ActiveSpell spell : activeSpells) {
            if (readyAt.get(spell) <= tick) {
                if (canCastSameSpellTwice || activeSpells.size() == 1 || spell != lastCast) {
                    if (spell.canCast(context)) {
                        candidates.add(spell);
                    }
                }
            }
        }

        ActiveSpell picked = pickWeighted(candidates);
        if (picked != null) startCast(picked, context);
    }

    private void startCast(ActiveSpell spell, SpellContext context) {
        current = spell;
        lastCast = spell;
        readyAt.put(spell, tick + spell.cooldown());
        Bukkit.getPluginManager().callEvent(new SpellCastEvent(context.caster().getEntity(), context.caster(), spell));

        enter(State.WINDUP);
        spell.onWindupStart(context);
        if (spell.windup() <= 0) cast(context);
    }

    private void cast(SpellContext context) {
        current.onCast(context);
        enter(State.RECOVERY);
    }

    private static @Nullable ActiveSpell pickWeighted(List<ActiveSpell> candidates) {
        int total = candidates.stream().mapToInt(ActiveSpell::weight).sum();
        if (total <= 0) return null;
        int roll = Utils.RANDOM.nextInt(total);
        for (ActiveSpell spell : candidates) {
            roll -= spell.weight();
            if (roll < 0) return spell;
        }
        return null;
    }

    public void interrupt(@Nullable SpellContext context) {
        if (current != null && state == State.WINDUP) current.onInterrupted(context);
        current = null;
        enter(State.IDLE);
    }

    public void forceCast(Class<? extends ActiveSpell> type, SmpEntity owner) {
        activeSpells.stream().filter(type::isInstance).findFirst().ifPresent(spell -> {
            SpellContext context = new SpellContext(owner, SpellTargets.playerTarget(owner.getEntity()), tick);
            if (current != null) interrupt(context);
            startCast(spell, context);
        });
    }

    public void forceCastRandom(SmpEntity owner) {
        if (activeSpells.isEmpty()) return;
        forceCast(activeSpells.get(Utils.RANDOM.nextInt(activeSpells.size())).getClass(), owner);
    }

    public boolean isCasting() {
        return state == State.WINDUP;
    }

    public @Nullable ActiveSpell getCurrentCast() {
        return current;
    }

    public int getDetectionRange() {
        return detectionRange;
    }

    private void enter(State next) {
        state = next;
        stateTicks = 0;
    }

    @Override
    public void onUnload(SmpEntity entity) {
        if (current != null) interrupt(null);
        spells.forEach(Spell::onRemoved);
    }

    @Override
    public void onDamage(DamageEvent event, SmpEntity entity) {
        spells.forEach(spell -> spell.onDamage(event));
    }

    @Override
    public void onHurt(DamageEvent event, SmpEntity smpEntity) {
        spells.forEach(spell -> spell.onHurt(event));
    }

    @Override
    public void onDeath(EntityDeathEvent event, SmpEntity entity) {
        spells.forEach(spell -> spell.onDeath(event));
    }

    @Override
    public void onProjectileLaunch(ProjectileLaunchEvent event, SmpEntity entity) {
        spells.forEach(spell -> spell.onProjectileLaunch(event));
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent event, SmpEntity entity) {
        spells.forEach(spell -> spell.onProjectileHit(event));
    }

    @Override
    public void onCastSpell(SpellCastEvent event, SmpEntity entity) {
        spells.forEach(spell -> spell.onCastSpell(event));
    }

    @Override
    public void onTargetEntity(EntityTargetLivingEntityEvent event, SmpEntity entity) {
        spells.forEach(spell -> spell.onTargetEntity(event));
    }

    public @Unmodifiable List<SpellConfig> getSpellConfigs() {
        return spellConfigs;
    }

    public boolean isCanCastSameSpellTwice() {
        return canCastSameSpellTwice;
    }
}
