package com.roguesmp.event;

import com.roguesmp.entity.SmpEntity;
import com.roguesmp.entity.spell.Spell;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * An entity (not player) cast a spell
 */
public class SpellCastEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity mBoss;
    private final SmpEntity smpEntity;
    private final Spell mSpell;

    public SpellCastEvent(LivingEntity boss, SmpEntity smpEntity, Spell spell) {
        mBoss = boss;
        this.smpEntity = smpEntity;
        mSpell = spell;
    }

    public LivingEntity getBoss() {
        return mBoss;
    }

    public SmpEntity getSmpEntity() {
        return smpEntity;
    }

    public Spell getSpell() {
        return mSpell;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
