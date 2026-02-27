package com.roguesmp.entity.spell;

import org.jetbrains.annotations.Nullable;

import java.util.*;

/*
 * The SpellManager class is designed to manage active spells for an entity. It
 * provides a simple interface to run a random spell from the provided list of
 * available spells. It automatically checks each spell's canRun() and moves to
 * the next spell seamlessly if conditions are not met.
 *
 * The SpellManager also automatically manages spell cooldowns to make the
 * fight a little less repetitive. The number of steps in between running the
 * same spell again is calculated as floor((#spells - 1) / 2). So if there are
 * 1 or 2 spells there is no cooldown and either spell is equally likely. 3-4
 * spells is a cooldown of 1, meaning a spell can never be chosen again the
 * very next time a spell is invoked. 5-6 spells is a cooldown of 2 (can not be
 * chosen either immediately afterward OR the time after that). Etc.
 */
public class SpellManager {
    public static final SpellManager EMPTY = new SpellManager(Collections.emptyList());

    protected Map<Class<? extends Spell>, Spell> readySpells;
    protected final Queue<Spell> cooldownSpells;
    protected final int cooldown;
    protected final boolean isEmpty;
    protected @Nullable Spell lastCasted = null;

    public boolean isEmpty() {
        return isEmpty;
    }

    public List<Spell> getSpells() {
        List<Spell> spells = new ArrayList<>();
        if (isEmpty) {
            return spells;
        }

        spells.addAll(cooldownSpells);
        spells.addAll(readySpells.values());
        return spells;
    }

    public SpellManager(List<Spell> spells) {
        isEmpty = spells.isEmpty();
        if (isEmpty) {
            readySpells = Collections.emptyMap();
        } else {
            readySpells = new HashMap<>();
            for (Spell spell : spells) {
                readySpells.put(spell.getClass(), spell);
            }
        }

        cooldownSpells = new ArrayDeque<>();
        cooldown = (int) Math.max(0, Math.floor((readySpells.size() - 1.0) / 2.0));
    }

    public int runNextSpell(boolean preventSameSpellTwiceInARow) {
        /* Standard 1s delay with no spells */
        if (isEmpty) {
            return 20;
        }

        /*
         * If a spell has been on cooldown sufficiently long, remove it from
         * the cooldown list and add it to the ready list.
         */
        if (cooldownSpells.size() > cooldown) {
            Spell toAdd = cooldownSpells.remove();
            readySpells.put(toAdd.getClass(), toAdd);
        }

        /* No active spells, exit early */
        if (readySpells.isEmpty()) {
            return 20;
        }

        /*
         * Try the ready spells in random order until can be run or none remain
         */
        List<Spell> spells = new ArrayList<>(readySpells.values());
        Collections.shuffle(spells);
        Spell previousSpell = lastCasted;
        lastCasted = null;
        Iterator<Spell> iterator = spells.iterator();
        while (iterator.hasNext()) {
            Spell spell = iterator.next();
            if (spell.canRun() && !spell.onlyForceCasted()) {
                if (preventSameSpellTwiceInARow && previousSpell != null && spell.getClass().equals(previousSpell.getClass())) {
                    continue;
                }
                spell.run();
                lastCasted = spell;
                cooldownSpells.add(spell);
                iterator.remove();
                /* Return how much time the spell takes */
                return spell.cooldownTicks();
            }
        }

        /* None of these spells can run - wait a second before trying again */
        return 20;
    }

    public int forceCastSpell(Class<? extends Spell> spell) {
        /* Standard 1s delay with no spells */
        if (isEmpty) {
            return 20;
        }

        if (lastCasted != null) {
            lastCasted.cancel();
            lastCasted = null;
        }
        Spell sp = readySpells.get(spell);
        if (sp != null && sp.canRun()) {
            sp.run();
            lastCasted = sp;
            cooldownSpells.add(sp);
            return sp.cooldownTicks();
        }
        /* None of these spells can run - wait a second before trying again */
        return 20;
    }

    public @Nullable Spell getLastCastedSpell() {
        return lastCasted;
    }

    public void cancelAll() {
        cancelAll(false);
    }

    public void cancelAll(boolean onPhaseChange) {
        if (!isEmpty) {
            List<Spell> spells = new ArrayList<>();
            spells.addAll(readySpells.values());
            spells.addAll(cooldownSpells);
            if (onPhaseChange) {
                spells.removeIf(Spell::persistOnPhaseChange);
            }
            spells.forEach(Spell::cancel);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Spell Manager: [");
        List<Spell> spells = getSpells();
        for (int i = 0; i < spells.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(spells.get(i).toString());
        }
        builder.append("]");
        return builder.toString();
    }
}
