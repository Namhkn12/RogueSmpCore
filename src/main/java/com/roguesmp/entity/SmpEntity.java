package com.roguesmp.entity;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellManager;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.event.SpellCastEvent;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represent a BaseEntity that is spawned in the world
 */
public class SmpEntity {
    public static final int PASSIVE_RUN_INTERVAL_DEFAULT = 5;

    public final LivingEntity entity;
    private final String id;
    private final RogueSmpCore plugin;

    private int detectionRange;
    public SpellManager activeSpells;
    private List<Spell> passiveSpells;
    private boolean preventSameSpellTwiceInARow;
    private @Nullable BukkitRunnable taskPassive = null;
    private @Nullable BukkitRunnable taskActive = null;
    private boolean unloaded = false;
    private int nextActiveTimer = 0;
    public boolean dead = false;
    private long passiveIntervalTicks;
    private @Nullable BossBarManager bossBar;

    public SmpEntity(BaseEntity base, LivingEntity entity) {
        this.plugin = RogueSmpCore.getInstance();
        this.entity = entity;
        id = base.getId();
        activeSpells = SpellManager.EMPTY;
        passiveSpells = Collections.emptyList();
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
            taskActive = getSpellCastRunnable();
            taskActive.runTaskTimer(plugin, spellDelay, 2L);
            if (taskPassive != null) {
                taskPassive.cancel();
            }
            taskPassive = getPassiveSpellCastRunnable();
            taskPassive.runTaskTimer(plugin, spellDelay, passiveIntervalTicks);
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
                              @Nullable BossBarManager bossBar, long spellDelay) {
        startSpell(List.of(activeSpell), Collections.emptyList(), detectionRange, bossBar, spellDelay);
    }

    public void startSpell(List<Spell> activeSpells, List<Spell> passiveSpells,
                              int detectionRange, @Nullable BossBarManager bossBar, long spellDelay) {
        startSpell(new SpellManager(activeSpells), passiveSpells, detectionRange, bossBar, spellDelay);
    }

    public void startSpell(SpellManager activeSpells, List<Spell> passiveSpells, int detectionRange,
                              @Nullable BossBarManager bossBar) {
        startSpell(activeSpells, passiveSpells, detectionRange, bossBar, 100);
    }

    public void startSpell(SpellManager activeSpells, List<Spell> passiveSpells, int detectionRange,
                              @Nullable BossBarManager bossBar, long spellDelay) {
        startSpell(activeSpells, passiveSpells, detectionRange, bossBar, spellDelay, PASSIVE_RUN_INTERVAL_DEFAULT);
    }

    public void startSpell(SpellManager activeSpells, List<Spell> passiveSpells, int detectionRange,
                              @Nullable BossBarManager bossBar, long spellDelay, long passiveIntervalTicks) {
        startSpell(activeSpells, passiveSpells, detectionRange, bossBar, spellDelay, passiveIntervalTicks, false);
    }

    /* If detectionRange <= 0, will always run regardless of whether players are nearby */
    public void startSpell(SpellManager activeSpells, List<Spell> passiveSpells,
                           int detectionRange, @Nullable BossBarManager bossBar, long spellDelay,
                           long passiveIntervalTicks, boolean preventSameSpellTwiceInARow) {
        this.detectionRange = detectionRange;
        this.bossBar = bossBar;
        this.activeSpells = activeSpells;
        this.passiveSpells = passiveSpells;
        this.preventSameSpellTwiceInARow = preventSameSpellTwiceInARow;

        this.passiveIntervalTicks = passiveIntervalTicks;
        if (passiveSpells != null && !passiveSpells.isEmpty()) {
            taskPassive = getPassiveSpellCastRunnable();
            taskPassive.runTaskTimer(plugin, 1, this.passiveIntervalTicks);
        }

        if (activeSpells != null && !activeSpells.isEmpty()) {
            taskActive = getSpellCastRunnable();
            taskActive.runTaskTimer(plugin, spellDelay, 2L);
        }
    }

    private BukkitRunnable getPassiveSpellCastRunnable() {
        return new BukkitRunnable() {
            private long mMissingTicks = 0;

            @Override
            public void run() {
                if (bossBar != null && !dead) {
                    bossBar.update();
                }

                mMissingTicks += passiveIntervalTicks;
                if (mMissingTicks > 100) {
                    mMissingTicks = 0;
                    /* Check if somehow the entity is missing even though this is still running */
                    if (isEntityMissing()) {
                        handleMissingEntity();
                        cancel();
                        return;
                    }
                }

                /* Don't run abilities if players aren't present */
                if (detectionRange > 0 && PlayerUtils.playersInRange(entity.getLocation(), detectionRange, true).isEmpty()) {
                    return;
                }

                if (passiveSpells != null) {
                    for (Spell spell : passiveSpells) {
                        spell.run();
                    }
                }
            }
        };
    }

    private BukkitRunnable getSpellCastRunnable() {
        return new BukkitRunnable() {
            private boolean disabled = true;
            private int missingTicks = 0;

            @Override
            public void run() {
                nextActiveTimer -= 2;
                missingTicks += 2;

                if (nextActiveTimer > 0) {
                    // Still waiting for the current spell to finish
                    return;
                }

                if (missingTicks > 100) {
                    missingTicks = 0;
                    /* Check if somehow the entity is missing even though this is still running */
                    if (isEntityMissing()) {
                        handleMissingEntity();
                        cancel();
                        return;
                    }
                }

                /* Don't progress if players aren't present */
                if (detectionRange > 0 && PlayerUtils.playersInRange(entity.getLocation(), detectionRange, true).isEmpty()) {
                    if (!disabled) {
                        /* Cancel all the spells just in case they were activated */
                        disabled = true;

                        activeSpells.cancelAll();
                    }
                    return;
                }

                /* Some spells might have been run - so when this next deactivates they need to be cancelled */
                disabled = false;

                // Run the next spell and store how long before the next spell can run
                nextActiveTimer = activeSpells.runNextSpell(preventSameSpellTwiceInARow);

                // The event goes after the spell casts.
                Spell spell = activeSpells.getLastCastedSpell();
                if (spell != null) {
                    SpellCastEvent event = new SpellCastEvent(entity, SmpEntity.this, spell);
                    Bukkit.getPluginManager().callEvent(event);
                }

            }
        };
    }

    private void handleMissingEntity() {
        RogueSmpCore.LOGGER.warn("Entity {} is missing{} but still registered as an active SmpEntity. It has been removed and untracked via the fallback system.", id, entity.isValid() ? " (but valid)" : "");
        EntityManager.getInstance().unload(entity);
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
            RogueSmpCore.LOGGER.warn("Warning: Entity '{}' attempted to force cast '{}' but entity does not have this spell!", id, spell.toString());
        }
    }

    public String getId() {
        return id;
    }

    /* Check if somehow the entity is missing even though this is still running */
    private boolean isEntityMissing() {
        Location entityLocation = entity.getLocation();
        if (!entityLocation.isWorldLoaded() || !entityLocation.getChunk().isLoaded() || !entity.isValid()) {
            return true;
        }
        return false;
    }

    public void unload() {
        /* Even if we unload twice, really cancel these tasks */
        if (taskPassive != null && !taskPassive.isCancelled()) {
            taskPassive.cancel();
        }
        if (taskActive != null && !taskActive.isCancelled()) {
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

//    public void nearbyPlayerDeath(PlayerDeathEvent event) {
//
//    }
}
