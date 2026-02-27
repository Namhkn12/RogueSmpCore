package com.roguesmp.entity;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.entity.spell.Spell;
import com.roguesmp.entity.spell.SpellManager;
import com.roguesmp.event.SpellCastEvent;
import com.roguesmp.utils.PlayerUtils;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
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
    private final RogueSmpCore mPlugin;

    private int mDetectionRange;
    public SpellManager mActiveSpells;
    private List<Spell> mPassiveSpells;
    private boolean mPreventSameSpellTwiceInARow;
    private @Nullable BukkitRunnable mTaskPassive = null;
    private @Nullable BukkitRunnable mTaskActive = null;
    private boolean mUnloaded = false;
    private int mNextActiveTimer = 0;
    public boolean mDead = false;
    private long mPassiveIntervalTicks;
    private @Nullable BossBarManager mBossBar;

    protected SmpEntity(BaseEntity base, LivingEntity boss) {
        this.mPlugin = RogueSmpCore.getInstance();
        entity = boss;
        id = base.getId();
        mActiveSpells = SpellManager.EMPTY;
        mPassiveSpells = Collections.emptyList();
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
            if (mTaskActive != null) {
                mTaskActive.cancel();
                mTaskActive = getSpellCastRunnable();
                mTaskActive.runTaskTimer(mPlugin, spellDelay, 2L);
            }
            if (mTaskPassive != null) {
                mTaskPassive.cancel();
                mTaskPassive = getPassiveSpellCastRunnable();
                mTaskPassive.runTaskTimer(mPlugin, spellDelay, mPassiveIntervalTicks);
            }
        }
        mActiveSpells.cancelAll(true);
        mActiveSpells = activeSpells;
        mPassiveSpells = passiveSpells;
    }

    public void changePassivePhase(List<Spell> passiveSpells) {
        mPassiveSpells = passiveSpells;
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
        mDetectionRange = detectionRange;
        mBossBar = bossBar;
        mActiveSpells = activeSpells;
        mPassiveSpells = passiveSpells;
        mPreventSameSpellTwiceInARow = preventSameSpellTwiceInARow;

        mPassiveIntervalTicks = passiveIntervalTicks;
        mTaskPassive = getPassiveSpellCastRunnable();
        mTaskPassive.runTaskTimer(mPlugin, 1, mPassiveIntervalTicks);

        mTaskActive = getSpellCastRunnable();
        mTaskActive.runTaskTimer(mPlugin, spellDelay, 2L);
    }

    private BukkitRunnable getPassiveSpellCastRunnable() {
        return new BukkitRunnable() {
            private long mMissingTicks = 0;

            @Override
            public void run() {
                if (mBossBar != null && !mDead) {
                    mBossBar.update();
                }

                mMissingTicks += mPassiveIntervalTicks;
                if (mMissingTicks > 100) {
                    mMissingTicks = 0;
                    /* Check if somehow the boss entity is missing even though this is still running */
                    if (isEntityMissing()) {
                        handleMissingBoss();
                        cancel();
                        return;
                    }
                }

                /* Don't run abilities if players aren't present */
                if (mDetectionRange > 0 && PlayerUtils.playersInRange(entity.getLocation(), mDetectionRange, true).isEmpty()) {
                    return;
                }

                if (mPassiveSpells != null) {
                    for (Spell spell : mPassiveSpells) {
                            spell.run();
                    }
                }
            }
        };
    }

    private BukkitRunnable getSpellCastRunnable() {
        return new BukkitRunnable() {
            private boolean mDisabled = true;
            private int mMissingTicks = 0;

            @Override
            public void run() {
                mNextActiveTimer -= 2;
                mMissingTicks += 2;

                if (mNextActiveTimer > 0) {
                    // Still waiting for the current spell to finish
                    return;
                }

                if (mMissingTicks > 100) {
                    mMissingTicks = 0;
                    /* Check if somehow the boss entity is missing even though this is still running */
                    if (isEntityMissing()) {
                        handleMissingBoss();
                        cancel();
                        return;
                    }
                }

                /* Don't progress if players aren't present */
                if (mDetectionRange > 0 && PlayerUtils.playersInRange(entity.getLocation(), mDetectionRange, true).isEmpty()) {
                    if (!mDisabled) {
                        /* Cancel all the spells just in case they were activated */
                        mDisabled = true;

                        mActiveSpells.cancelAll();
                    }
                    return;
                }

                /* Some spells might have been run - so when this next deactivates they need to be cancelled */
                mDisabled = false;

                // Run the next spell and store how long before the next spell can run
                mNextActiveTimer = mActiveSpells.runNextSpell(mPreventSameSpellTwiceInARow);

                // The event goes after the spell casts.
                Spell spell = mActiveSpells.getLastCastedSpell();
                if (spell != null) {
                    SpellCastEvent event = new SpellCastEvent(entity, SmpEntity.this, spell);
                    Bukkit.getPluginManager().callEvent(event);
                }

            }
        };
    }

    private void handleMissingBoss() {
        RogueSmpCore.LOGGER.warn("Boss {} is missing{} but still registered as an active boss. It has been removed and untracked via the fallback system.", id, entity.isValid() ? " (but valid)" : "");
        EntityManager.getInstance().unload(entity);
        // usb: this is triggering when it shouldn't, don't remove bosses if they might be persistant
        // mBoss.remove();
    }

    public void forceCastRandomSpell() {
        List<Spell> spells = mActiveSpells.getSpells();
        if (!spells.isEmpty()) {
            Spell spell = spells.get(Utils.RANDOM.nextInt(spells.size()));
            forceCastSpell(spell.getClass());
        }
    }

    public void forceCastSpell(Class<? extends Spell> spell) {
        mNextActiveTimer = mActiveSpells.forceCastSpell(spell);
        Spell sp = mActiveSpells.getLastCastedSpell();
        if (sp != null) {
            SpellCastEvent event = new SpellCastEvent(entity, this, sp);
            Bukkit.getPluginManager().callEvent(event);
        } else {
            RogueSmpCore.LOGGER.warn("Warning: Boss '{}' attempted to force cast '{}' but boss does not have this spell!", id, spell.toString());
        }
    }

    public String getId() {
        return id;
    }

    /* Check if somehow the boss entity is missing even though this is still running */
    private boolean isEntityMissing() {
        Location bossLoc = entity.getLocation();
        if (!bossLoc.isWorldLoaded() || !bossLoc.getChunk().isLoaded()) {
            return true;
        }
        return false;
    }

    public void unload() {
        /* Even if we unload twice, really cancel these tasks */
        if (mTaskPassive != null && !mTaskPassive.isCancelled()) {
            mTaskPassive.cancel();
        }
        if (mTaskActive != null && !mTaskActive.isCancelled()) {
            mTaskActive.cancel();
        }

        /* Make sure we don't accidentally call the main unload sequence twice */
        if (!mUnloaded) {
            mUnloaded = true;

            mActiveSpells.cancelAll();

            if (mBossBar != null) {
                mBossBar.remove();
            }
        }
    }
}
