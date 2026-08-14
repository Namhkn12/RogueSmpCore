package com.roguesmp.entity.component.impl;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.component.EntityComponent;
import com.roguesmp.entity.spell.*;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.event.SpellCastEvent;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * An entity's active/passive spell casting - both the JSON-declared config (spawns real
 * {@link Spell} instances from {@link SpellParams} on {@link #onSpawn}) and the live runtime
 * (scheduling, detection range, event dispatch). Code-driven bosses that don't declare spells in
 * JSON attach their own instance directly via {@link #SpellComponent(SmpEntity)} in their own
 * constructor (see {@code HellKnight}/{@code PrimordialSlime}/{@code HellKnightCompanion}).
 * {@link #copy()} produces a fresh, unbound, not-yet-started instance so runtime state is never
 * shared between spawned entities.
 * <p>
 * Boss bar rendering ({@link BossBarComponent}) and health-threshold phase triggers
 * ({@link PhaseComponent}) are both fully independent components now - a boss attaches them
 * directly onto itself (it's an {@code SmpEntity}) via {@code setComponent(...)}, no wiring
 * through this component needed.
 */
public class SpellComponent implements EntityComponent {

    public static final Codec<SpellComponent> CODEC = Codec.composite(
            Codec.listOf(SpellParams.CODEC).optionalFieldOf("activeSpell", List.of()).forGetter(SpellComponent::getActiveSpellParams),
            Codec.listOf(SpellParams.CODEC).optionalFieldOf("passiveSpell", List.of()).forGetter(SpellComponent::getPassiveSpellParams),
            Codec.INT.optionalFieldOf("passiveInterval", 0).forGetter(SpellComponent::getPassiveIntervalConfig),
            Codec.BOOLEAN.optionalFieldOf("canCastSameSpellTwice", false).forGetter(SpellComponent::isCanCastSameSpellTwiceConfig),
            SpellComponent::new
    );

    // JSON-declared config, shared by every copy
    private final List<SpellParams> activeSpellParams;
    private final List<SpellParams> passiveSpellParams;
    private final int passiveIntervalConfig;
    private final boolean canCastSameSpellTwiceConfig;

    // Per-instance runtime state, only ever touched on this component's own copy.
    // owner/entity are null only until bind()/onSpawn() runs - every other method here is only
    // ever called after that (JSON path: onSpawn always runs first; code-driven bosses bind
    // immediately in their constructor via SpellComponent(SmpEntity)).
    private SmpEntity owner;
    private LivingEntity entity;
    private int detectionRange;
    private SpellManager activeSpells = SpellManager.EMPTY;
    private List<Spell> passiveSpells = Collections.emptyList();
    private boolean preventSameSpellTwiceInARow;
    private @Nullable ScheduledTask taskPassive;
    private @Nullable ScheduledTask taskActive;
    private int nextActiveTimer = 0;
    private int passiveIntervalTicks;
    private boolean activeDisabled = true;

    public SpellComponent(List<SpellParams> activeSpellParams, List<SpellParams> passiveSpellParams,
                           int passiveIntervalConfig, boolean canCastSameSpellTwiceConfig) {
        this.activeSpellParams = activeSpellParams;
        this.passiveSpellParams = passiveSpellParams;
        this.passiveIntervalConfig = passiveIntervalConfig;
        this.canCastSameSpellTwiceConfig = canCastSameSpellTwiceConfig;
    }

    /**
     * For code-driven bosses with no JSON-declared spells - binds immediately so
     * {@link #startSpell}/{@link #changePhase} can be called right away from the owner's own constructor.
     */
    public SpellComponent(SmpEntity owner) {
        this(List.of(), List.of(), 0, false);
        bind(owner);
    }

    @Override
    public @NotNull EntityComponent copy() {
        return new SpellComponent(activeSpellParams, passiveSpellParams, passiveIntervalConfig, canCastSameSpellTwiceConfig);
    }

    /**
     * Binds this component to its owning entity without building/starting any declared spells.
     */
    public void bind(SmpEntity owner) {
        if (this.owner != null) return;
        this.owner = owner;
        this.entity = owner.getEntity();
    }

    @Override
    public void onSpawn(SmpEntity smpEntity) {
        bind(smpEntity);
        if (activeSpellParams.isEmpty() && passiveSpellParams.isEmpty()) return;

        List<Spell> activeSpellList = new ArrayList<>();
        activeSpellParams.forEach(params -> {
            Spell spell = EntitySpells.createSpell(params, owner.getEntity());
            if (spell != null) activeSpellList.add(spell);
        });
        SpellManager spellManager = new SpellManager(activeSpellList);

        List<Spell> passiveSpellList = new ArrayList<>();
        passiveSpellParams.forEach(params -> {
            Spell spell = EntitySpells.createSpell(params, entity);
            if (spell != null) passiveSpellList.add(spell);
        });

        BehaviorComponent behavior = smpEntity.getComponent(EntityComponentKeys.BEHAVIOR);
        int detectionRange = behavior != null ? behavior.getEffectiveDetectionRange() : 20;

        int passiveIntervalTick = passiveIntervalConfig <= 0 ? SmpEntity.PASSIVE_RUN_INTERVAL_DEFAULT : passiveIntervalConfig;

        startSpell(spellManager, passiveSpellList, detectionRange, 1, passiveIntervalTick, canCastSameSpellTwiceConfig);
    }

    public void changePhase(SpellManager activeSpells, List<Spell> passiveSpells, @Nullable Consumer<LivingEntity> phaseAction) {
        changePhase(activeSpells, passiveSpells, phaseAction, 0);
    }

    public void changePhase(SpellManager activeSpells, List<Spell> passiveSpells,
                             @Nullable Consumer<LivingEntity> phaseAction, int spellDelay) {
        if (phaseAction != null) {
            phaseAction.accept(entity);
        }
        if (spellDelay > 0) {
            if (taskActive != null) taskActive.cancel();
            taskActive = entity.getScheduler().runAtFixedRate(
                    RogueSmpCore.getInstance(),
                    task -> runActiveSpellTask(SmpEntity.ACTIVE_RUN_INTERVAL_DEFAULT),
                    owner::unload,
                    spellDelay,
                    SmpEntity.ACTIVE_RUN_INTERVAL_DEFAULT
            );
            if (taskPassive != null) taskPassive.cancel();
            taskPassive = entity.getScheduler().runAtFixedRate(
                    RogueSmpCore.getInstance(),
                    task -> runPassiveSpellTask(passiveIntervalTicks),
                    owner::unload,
                    1L,
                    passiveIntervalTicks
            );
        }
        this.activeSpells.cancelAll(true);
        this.activeSpells = activeSpells;
        this.passiveSpells = passiveSpells;
    }

    public void changePassivePhase(List<Spell> passiveSpells) {
        this.passiveSpells = passiveSpells;
    }

    public void startSpell(Spell activeSpell, int detectionRange) {
        startSpell(activeSpell, detectionRange, 100);
    }

    public void startSpell(Spell activeSpell, int detectionRange, int spellDelay) {
        startSpell(List.of(activeSpell), Collections.emptyList(), detectionRange, spellDelay);
    }

    public void startSpell(List<Spell> activeSpells, List<Spell> passiveSpells, int detectionRange, int spellDelay) {
        startSpell(new SpellManager(activeSpells), passiveSpells, detectionRange, spellDelay);
    }

    public void startSpell(SpellManager activeSpells, List<Spell> passiveSpells, int detectionRange) {
        startSpell(activeSpells, passiveSpells, detectionRange, 100);
    }

    public void startSpell(SpellManager activeSpells, List<Spell> passiveSpells, int detectionRange, int spellDelay) {
        startSpell(activeSpells, passiveSpells, detectionRange, spellDelay, SmpEntity.PASSIVE_RUN_INTERVAL_DEFAULT);
    }

    public void startSpell(SpellManager activeSpells, List<Spell> passiveSpells, int detectionRange,
                            int spellDelay, int passiveIntervalTicks) {
        startSpell(activeSpells, passiveSpells, detectionRange, spellDelay, passiveIntervalTicks, false);
    }

    /* If detectionRange <= 0, will always run regardless of whether players are nearby */
    public void startSpell(SpellManager activeSpells, List<Spell> passiveSpells,
                            int detectionRange, int spellDelay,
                            int passiveIntervalTicks, boolean preventSameSpellTwiceInARow) {
        this.detectionRange = detectionRange;
        this.activeSpells = activeSpells;
        this.passiveSpells = passiveSpells;
        this.preventSameSpellTwiceInARow = preventSameSpellTwiceInARow;

        this.passiveIntervalTicks = passiveIntervalTicks;
        if (passiveSpells != null && !passiveSpells.isEmpty()) {
            if (taskPassive != null) taskPassive.cancel();
            taskPassive = entity.getScheduler().runAtFixedRate(
                    RogueSmpCore.getInstance(),
                    task -> runPassiveSpellTask(passiveIntervalTicks),
                    owner::unload,
                    1L,
                    passiveIntervalTicks
            );
        }

        if (activeSpells != null && !activeSpells.isEmpty()) {
            if (taskActive != null) taskActive.cancel();
            taskActive = entity.getScheduler().runAtFixedRate(
                    RogueSmpCore.getInstance(),
                    task -> runActiveSpellTask(SmpEntity.ACTIVE_RUN_INTERVAL_DEFAULT),
                    owner::unload,
                    spellDelay,
                    SmpEntity.ACTIVE_RUN_INTERVAL_DEFAULT
            );
        }
    }

    private void runPassiveSpellTask(int passiveIntervalTicks) {
        if (detectionRange > 0 && PlayerUtils.playersInRange(entity.getLocation(), detectionRange, true).isEmpty()) {
            return;
        }
        if (passiveSpells != null) {
            for (Spell spell : passiveSpells) {
                spell.run(passiveIntervalTicks);
            }
        }
    }

    private void runActiveSpellTask(int activeInterval) {
        nextActiveTimer -= activeInterval;

        if (nextActiveTimer > 0) {
            return;
        }
        if (detectionRange > 0 && PlayerUtils.playersInRange(entity.getLocation(), detectionRange, true).isEmpty()) {
            if (!activeDisabled) {
                activeDisabled = true;
                activeSpells.cancelAll();
            }

            return;
        }

        activeDisabled = false;
        nextActiveTimer = activeSpells.runNextSpell(preventSameSpellTwiceInARow);
        Spell spell = activeSpells.getLastCastedSpell();
        if (spell != null) {
            SpellCastEvent event = new SpellCastEvent(entity, owner, spell);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    public void forceCastRandomSpell() {
        List<Spell> spells = activeSpells.getSpells();
        if (!spells.isEmpty()) {
            Spell spell = spells.get(Utils.RANDOM.nextInt(spells.size()));
            forceCastSpell(spell.getClass());
        }
    }

    public void forceCastSpell(Class<? extends Spell> spell) {
        nextActiveTimer = activeSpells.forceCastSpell(spell);
        Spell sp = activeSpells.getLastCastedSpell();
        if (sp != null) {
            SpellCastEvent event = new SpellCastEvent(entity, owner, sp);
            Bukkit.getPluginManager().callEvent(event);
        } else {
            RogueSmpCore.LOGGER.warn("Warning: Entity '{}' attempted to force cast '{}' but entity does not have this spell!", owner.getId(), spell.toString());
        }
    }

    public int getDetectionRange() {
        return detectionRange;
    }

    @Override
    public void onUnload(SmpEntity entity) {
        if (taskPassive != null) taskPassive.cancel();
        if (taskActive != null) taskActive.cancel();

        activeSpells.cancelAll();
    }

    @Override
    public void onDamage(DamageEvent event, SmpEntity entity) {
        activeSpells.getSpells().forEach(spell -> spell.onDamage(event));
        passiveSpells.forEach(spell -> spell.onDamage(event));
    }

    @Override
    public void onHurt(DamageEvent event, SmpEntity smpEntity) {
        activeSpells.getSpells().forEach(spell -> spell.onHurt(event));
        passiveSpells.forEach(spell -> spell.onHurt(event));
    }

    @Override
    public void onDeath(EntityDeathEvent event, SmpEntity entity) {
        activeSpells.getSpells().forEach(spell -> spell.onDeath(event));
        passiveSpells.forEach(spell -> spell.onDeath(event));
    }

    @Override
    public void onProjectileLaunch(ProjectileLaunchEvent event, SmpEntity entity) {
        activeSpells.getSpells().forEach(spell -> spell.onProjectileLaunch(event));
        passiveSpells.forEach(spell -> spell.onProjectileLaunch(event));
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent event, SmpEntity entity) {
        activeSpells.getSpells().forEach(spell -> spell.onProjectileHit(event));
        passiveSpells.forEach(spell -> spell.onProjectileHit(event));
    }

    @Override
    public void onCastSpell(SpellCastEvent event, SmpEntity entity) {
        activeSpells.getSpells().forEach(spell -> spell.onCastSpell(event));
        passiveSpells.forEach(spell -> spell.onCastSpell(event));
    }

    @Override
    public void onTargetEntity(EntityTargetLivingEntityEvent event, SmpEntity entity) {
        activeSpells.getSpells().forEach(spell -> spell.onTargetEntity(event));
        passiveSpells.forEach(spell -> spell.onTargetEntity(event));
    }

    public List<SpellParams> getActiveSpellParams() {
        return activeSpellParams;
    }

    public List<SpellParams> getPassiveSpellParams() {
        return passiveSpellParams;
    }

    public int getPassiveIntervalConfig() {
        return passiveIntervalConfig;
    }

    public boolean isCanCastSameSpellTwiceConfig() {
        return canCastSameSpellTwiceConfig;
    }
}
