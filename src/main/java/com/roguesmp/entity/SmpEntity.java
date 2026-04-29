package com.roguesmp.entity;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.DamageOperation;
import com.roguesmp.constant.DamageType;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellManager;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.event.SpellCastEvent;
import com.roguesmp.utils.EntityUtils;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.*;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represent a BaseEntity that is spawned in the world
 */
public class SmpEntity {
    public static final int PASSIVE_RUN_INTERVAL_DEFAULT = 5;
    public static final int ACTIVE_RUN_INTERVAL_DEFAULT = 2;

    protected final LivingEntity entity;
    protected final BaseEntity base;
    private final RogueSmpCore plugin;

    protected boolean initialized;

    private int detectionRange;
    public SpellManager activeSpells;
    private List<Spell> passiveSpells;
    private boolean preventSameSpellTwiceInARow;
    private @Nullable ScheduledTask taskPassive = null;
    private @Nullable ScheduledTask taskActive = null;
    private boolean unloaded = false;
    private int nextActiveTimer = 0;
    public boolean dead = false;
    private int passiveIntervalTicks;
    private @Nullable BossBarManager bossBar;

    public SmpEntity(BaseEntity base, LivingEntity entity) {
        this.plugin = RogueSmpCore.getInstance();
        this.entity = entity;
        this.base = base;
        activeSpells = SpellManager.EMPTY;
        passiveSpells = Collections.emptyList();
    }

    public void initialize() {
        if (initialized) return; // Safety check

        // This triggers the logic inside BaseEntity to call startSpell()
        base.processEntity(this.entity);
        base.processSpell(this);

        this.initialized = true;
    }

    public void changePhase(SpellManager activeSpells,
                            List<Spell> passiveSpells, @Nullable Consumer<LivingEntity> phaseAction) {

        changePhase(activeSpells, passiveSpells, phaseAction, 0);
    }

    public void changePhase(SpellManager activeSpells,
                            List<Spell> passiveSpells, @Nullable Consumer<LivingEntity> phaseAction, int spellDelay) {
        if (phaseAction != null) {
            phaseAction.accept(entity);
        }
        if (spellDelay > 0) {
            if (taskActive != null) {
                taskActive.cancel();
            }
            taskActive = entity.getScheduler().runAtFixedRate(
                    plugin,
                    task -> runActiveSpellTask(ACTIVE_RUN_INTERVAL_DEFAULT),
                    null,
                    spellDelay,
                    ACTIVE_RUN_INTERVAL_DEFAULT
            );
            if (taskPassive != null) {
                taskPassive.cancel();
            }
            taskPassive = entity.getScheduler().runAtFixedRate(
                    plugin,
                    task -> runPassiveSpellTask(passiveIntervalTicks),
                    null,
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
        startSpell(activeSpell, detectionRange, null);
    }

    public void startSpell(Spell activeSpell, int detectionRange,
                              @Nullable BossBarManager bossBar) {
        startSpell(activeSpell, detectionRange, bossBar, 100);
    }

    public void startSpell(Spell activeSpell, int detectionRange,
                              @Nullable BossBarManager bossBar, int spellDelay) {
        startSpell(List.of(activeSpell), Collections.emptyList(), detectionRange, bossBar, spellDelay);
    }

    public void startSpell(List<Spell> activeSpells, List<Spell> passiveSpells,
                              int detectionRange, @Nullable BossBarManager bossBar, int spellDelay) {
        startSpell(new SpellManager(activeSpells), passiveSpells, detectionRange, bossBar, spellDelay);
    }

    public void startSpell(SpellManager activeSpells, List<Spell> passiveSpells, int detectionRange,
                              @Nullable BossBarManager bossBar) {
        startSpell(activeSpells, passiveSpells, detectionRange, bossBar, 100);
    }

    public void startSpell(SpellManager activeSpells, List<Spell> passiveSpells, int detectionRange,
                              @Nullable BossBarManager bossBar, int spellDelay) {
        startSpell(activeSpells, passiveSpells, detectionRange, bossBar, spellDelay, PASSIVE_RUN_INTERVAL_DEFAULT);
    }

    public void startSpell(SpellManager activeSpells, List<Spell> passiveSpells, int detectionRange,
                              @Nullable BossBarManager bossBar, int spellDelay, int passiveIntervalTicks) {
        startSpell(activeSpells, passiveSpells, detectionRange, bossBar, spellDelay, passiveIntervalTicks, false);
    }

    /* If detectionRange <= 0, will always run regardless of whether players are nearby */
    public void startSpell(SpellManager activeSpells, List<Spell> passiveSpells,
                           int detectionRange, @Nullable BossBarManager bossBar, int spellDelay,
                           int passiveIntervalTicks, boolean preventSameSpellTwiceInARow) {
        this.detectionRange = detectionRange;
        this.bossBar = bossBar;
        this.activeSpells = activeSpells;
        this.passiveSpells = passiveSpells;
        this.preventSameSpellTwiceInARow = preventSameSpellTwiceInARow;

        this.passiveIntervalTicks = passiveIntervalTicks;
        if (passiveSpells != null && !passiveSpells.isEmpty()) {
            if (taskPassive != null) taskPassive.cancel();
            taskPassive = entity.getScheduler().runAtFixedRate(
                    plugin,
                    task -> runPassiveSpellTask(passiveIntervalTicks),
                    this::unload,
                    1L,
                    passiveIntervalTicks
            );
        }

        if (activeSpells != null && !activeSpells.isEmpty()) {
            if (taskActive != null) taskActive.cancel();
            taskActive = entity.getScheduler().runAtFixedRate(
                    plugin,
                    task -> runActiveSpellTask(ACTIVE_RUN_INTERVAL_DEFAULT),
                    this::unload,
                    spellDelay,
                    ACTIVE_RUN_INTERVAL_DEFAULT
            );
        }
    }

    private void runPassiveSpellTask(int passiveIntervalTicks) {
        if (bossBar != null && !dead) {
            bossBar.update();
        }
        if (detectionRange > 0 && PlayerUtils.playersInRange(entity.getLocation(), detectionRange, true).isEmpty()) {
            return;
        }
        if (passiveSpells != null) {
            for (Spell spell : passiveSpells) {
                spell.run(passiveIntervalTicks);
            }
        }
    }

    private boolean activeDisabled = true;

    private void runActiveSpellTask(int activeInterval) {
        if (bossBar != null && !dead) {
            bossBar.update();
        }
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
            SpellCastEvent event = new SpellCastEvent(entity, this, spell);
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
            SpellCastEvent event = new SpellCastEvent(entity, this, sp);
            Bukkit.getPluginManager().callEvent(event);
        } else {
            RogueSmpCore.LOGGER.warn("Warning: Entity '{}' attempted to force cast '{}' but entity does not have this spell!", base.getId(), spell.toString());
        }
    }

    public String getId() {
        return base.getId();
    }

    public int getDetectionRange() {
        return detectionRange;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void unload() {
        if (taskPassive != null) {
            taskPassive.cancel();
        }

        if (taskActive != null) {
            taskActive.cancel();
        }

        /* Make sure we don't accidentally call the main unload sequence twice */
        if (!unloaded) {
            unloaded = true;

            activeSpells.cancelAll();

            if (bossBar != null) {
                bossBar.remove();
            }
        }
    }

    public void onDamage(DamageEvent event) {
        activeSpells.getSpells().forEach(spell -> {
            spell.onDamage(event);
        });
        passiveSpells.forEach(spell -> {
            spell.onDamage(event);
        });
    }

    /*
     * Entity was hurt
     */
    public void onHurt(DamageEvent event) {
        activeSpells.getSpells().forEach(spell -> {
            spell.onHurt(event);
        });
        passiveSpells.forEach(spell -> {
            spell.onHurt(event);
        });

        if (entity != null && event.getDamageType() != DamageType.TRUE) {
            if (bossBar == null || !bossBar.capsDamage()) {
                return;
            }
            bossBar.getNextHealthThreshold().ifPresent(nextHpPercent -> {
                // Min 1 to make sure we actually go below the threshold but don't kill the boss
                double setHealth = Math.max(nextHpPercent * EntityUtils.getMaxHealth(entity) / 100, 1);
                double health = entity.getHealth();
                if (health - event.getFinalDamage() >= setHealth) {
                    return;
                }
                entity.setHealth((health - setHealth + 1));
                event.addDamageModifier(0, DamageOperation.MORE_FINAL);
            });
        }
    }

    public void onDeath(EntityDeathEvent event) {
        activeSpells.getSpells().forEach(spell -> {
            spell.onDeath(event);
        });
        passiveSpells.forEach(spell -> {
            spell.onDeath(event);
        });
    }

    /*
     * Entity shot a projectile
     */
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        activeSpells.getSpells().forEach(spell -> {
            spell.onProjectileLaunch(event);
        });
        passiveSpells.forEach(spell -> {
            spell.onProjectileLaunch(event);
        });
    }

    /*
     * Entity-shot projectile hit something
     */
    public void onProjectileHit(ProjectileHitEvent event) {
        activeSpells.getSpells().forEach(spell -> {
            spell.onProjectileHit(event);
        });
        passiveSpells.forEach(spell -> {
            spell.onProjectileHit(event);
        });
    }

    public void onCastSpell(SpellCastEvent event) {
        activeSpells.getSpells().forEach(spell -> {
            spell.onCastSpell(event);
        });
        passiveSpells.forEach(spell -> {
            spell.onCastSpell(event);
        });
    }

    public void onTargetEntity(EntityTargetLivingEntityEvent event) {
        activeSpells.getSpells().forEach(spell -> {
            spell.onTargetEntity(event);
        });
        passiveSpells.forEach(spell -> {
            spell.onTargetEntity(event);
        });
    }

    public boolean hasPlayerDeathTrigger() {
        return false;
    }

    public void onNearbyPlayerDeath(PlayerDeathEvent event) {

    }
}
